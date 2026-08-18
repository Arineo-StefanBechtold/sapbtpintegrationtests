# Konzept: Testsuite mit JUnit 5

## Überblick

Dieses Dokument beschreibt den JUnit-5-seitigen Teil des Test-Frameworks: wie Testfälle strukturiert werden, wie das Framework die CPI-Monitoring-API zur Korrelation nutzt und wie die Dokumente vom Collector abgerufen und gegen Erwartungen geprüft werden.

---

## 1. Architekturprinzipien des Frameworks

1. **Framework ist für Korrelation und Wartelogik zuständig** – Der Testfall selbst beschreibt nur Eingabe, erwartete Ausgabe und Assertions.
2. **Correlation ID bleibt im Framework** – Sie wird verwendet, um Message IDs über die CPI-Monitoring-API zu ermitteln, erscheint aber nicht im Collector.
3. **Dokumente werden über Message IDs abgeholt** – Das Framework baut die Anfragen an den Collector aus der ermittelten Message-ID-Liste auf.
4. **Keine harte Kopplung an CPI-Interna** – Schnittstellen sind über konfigurierbare Clients abstrahiert.

---

## 2. Ablauf eines Testfalls

```
 ┌────────────────────────────────────────────────────────┐
 │  JUnit-Testfall                                        │
 │                                                        │
 │  1. triggerMessage(input, correlationId)               │
 │  2. waitForCompletion(correlationId, timeout)          │
 │     → intern: poll CPI Monitoring API                  │
 │  3. messageIds = resolveMessageIds(correlationId)      │
 │  4. documents  = fetchDocuments(messageIds)            │
 │  5. assertions on documents                            │
 └────────────────────────────────────────────────────────┘
```

### Schritt 1 – Trigger

Das Framework sendet die Eingabenachricht an den konfigurierten CPI-Endpunkt. Die Correlation ID wird als HTTP-Header (`SAP-Message-Id` oder ein benutzerdefinierter Header) mitgesendet oder vorab im iFlow konfiguriert.

### Schritt 2 – Warten

```java
framework.waitForCompletion(correlationId, Duration.ofSeconds(30));
```

Intern: wiederholtes `GET /MessageProcessingLogs?correlationId={id}&$top=1` bis Status `COMPLETED` oder `FAILED`, oder Timeout.

### Schritt 3 – Message IDs ermitteln

```java
List<String> messageIds = framework.resolveMessageIds(correlationId);
```

Intern:
```
GET /MessageProcessingLogs?correlationId={id}&$select=messageId
→ [ "MSG-001", "MSG-002" ]
```

### Schritt 4 – Dokumente abrufen

```java
Map<String, List<Document>> docs = framework.fetchDocuments(testRunId, messageIds);
```

Intern: für jede `messageId` ein `GET /collector/messages/{messageId}?testRunId={testRunId}`.

### Schritt 5 – Assertions

Der Testfall greift auf die Dokument-Map zu und prüft mit Standard-JUnit-5-Assertions oder einem DSL-Wrapper:

```java
assertThat(docs.get("MSG-001"))
    .hasSize(1)
    .first()
    .satisfies(doc -> assertXmlEquals(expected, doc.getBody()));
```

---

## 3. Framework-Klassen (Überblick)

```
com.example.cpitest
├── CpiTestFramework          // Haupteinstiegspunkt; orchestriert alle Schritte
├── CpiMonitoringClient       // Zugriff auf CPI Monitoring REST API
├── CollectorClient           // Zugriff auf Collector REST API
├── Document                  // Wertobjekt: sequenceNumber, contentType, body
├── CpiTestExtension          // JUnit-5-Extension; injiziert CpiTestFramework
└── CpiTestConfig             // Konfigurationsobjekt (URLs, Credentials, Timeouts)
```

---

## 4. Testfall-Beispiel

```java
@ExtendWith(CpiTestExtension.class)
class OrderProcessingTest {

    @Inject
    CpiTestFramework framework;

    @Test
    void singleOrderProducesPostedStatus() throws Exception {
        // Arrange
        String correlationId = UUID.randomUUID().toString();
        String input = loadResource("order-4711.xml");

        // Act
        framework.trigger("https://cpi.example.com/http/orders", input, correlationId);
        framework.waitForCompletion(correlationId, Duration.ofSeconds(30));

        List<String> messageIds = framework.resolveMessageIds(correlationId);
        Map<String, List<Document>> docs = framework.fetchDocuments(testRunId, messageIds);

        // Assert
        List<Document> orderDocs = docs.get(messageIds.get(0));
        assertThat(orderDocs).hasSize(1);
        assertXmlContains(orderDocs.get(0).getBody(), "//Status", "POSTED");
    }
}
```

---

## 5. Nachlaufprüfung (residual check)

Nach dem Abruf kann der Testfall sicherstellen, dass **keine unerwarteten Dokumente** gesendet wurden:

```java
framework.assertNoUnexpectedDocuments(testRunId, messageIds, expectedMessageIds);
```

Diese Methode prüft, ob die vom Collector zurückgegebene Message-ID-Liste des Testlaufs mit der erwarteten Liste übereinstimmt.

---

## 6. Parallelausführung und Isolation

Jeder Testlauf erhält eine eigene `testRunId` (UUID), die:
- beim Einliefern als Header `X-Test-Run-Id` mitgesendet wird,
- beim Abrufen als Query-Parameter übergeben wird.

Dadurch können mehrere Tests gleichzeitig laufen, ohne sich gegenseitig zu beeinflussen.

---

## 7. Fehlerbehandlung im Framework

| Fehlerfall | Framework-Verhalten |
|---|---|
| CPI-Status `FAILED` | `CpiProcessingException` mit Fehlerdetails aus Monitoring-API |
| Timeout | `CpiTimeoutException` |
| Collector gibt leere Liste | Test-Assertion schlägt fehl (kein Framework-Fehler) |
| HTTP-Fehler am Collector | `CollectorException` mit HTTP-Statuscode |

---

## 8. Konfigurationsbeispiel

```yaml
cpi:
  baseUrl: https://<tenant>.it-cpi.cfapps.eu10.hana.ondemand.com
  oauthTokenUrl: https://<tenant>.authentication.eu10.hana.ondemand.com/oauth/token
  clientId: ${CPI_CLIENT_ID}
  clientSecret: ${CPI_CLIENT_SECRET}

collector:
  baseUrl: https://<collector-host>/collector
  apiKey: ${COLLECTOR_API_KEY}

test:
  defaultTimeoutSeconds: 30
  pollingIntervalSeconds: 2
```

---

## 9. Offene Punkte

| Nr. | Thema | Stand |
|---|---|---|
| 1 | Paralleler iFlow-Start – mehrere Correlation IDs in einem Testfall | Designvorschlag offen |
| 2 | Assertion-DSL für XML vs. JSON | Bibliothek noch auszuwählen |
| 3 | Integration in Maven Surefire / Failsafe | Konfigurationsvorlage ausstehend |

---

## 10. Bewusst verworfene Alternativen

| Alternative | Verwurfsgrund |
|---|---|
| Framework fragt Collector direkt nach Correlation ID | Collector kennt keine Correlation IDs; Abhängigkeit würde Collector verkomplizieren |
| Eigener Test-Listener in CPI (Custom Adapter) | Nicht supportet; zu hohe Invasivität |
| Synchrones Request/Reply statt asynchroner Prüfung | Nicht alle iFlows sind synchron; Modell wäre nicht universell |

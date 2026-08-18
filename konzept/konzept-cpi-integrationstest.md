# Konzept: CPI-Integrationstest-Framework

## Überblick

Dieses Dokument beschreibt das Gesamtkonzept für automatisierte Integrationstests auf der SAP BTP Integration Suite (Cloud Platform Integration, CPI). Ziel ist es, iFlow-Verarbeitungsschritte end-to-end zu testen, ohne die Produktivlandschaft zu berühren.

---

## 1. Ziele und Abgrenzung

| Ziel | Beschreibung |
|---|---|
| Fachliche Korrektheit | Prüfen, ob ein iFlow die richtigen Ausgabedokumente erzeugt. |
| Technische Korrektheit | Prüfen, ob Mapping, Routing und Fehlerbehandlung wie erwartet funktionieren. |
| Regressionssicherheit | Nach Deployments automatisch sicherstellen, dass bestehende Szenarien weiterhin funktionieren. |
| CI/CD-Integration | Tests laufen als JUnit-5-Tests in einer Standard-Build-Pipeline. |

**Außerhalb des Scopes:** Lasttest, UI-Tests, End-to-End-Tests über mehrere SAP-Systeme hinweg.

---

## 2. Grundlegende Architektur

```
 ┌───────────────────────────────────────────────────────────┐
 │  Test-Framework (JUnit 5)                                  │
 │                                                           │
 │  1. Testfall löst Nachricht an CPI aus                    │
 │  2. Wartet auf Verarbeitung                               │
 │  3. Ermittelt Correlation ID via CPI-Monitoring-API       │
 │  4. Ermittelt alle Message IDs zur Correlation ID         │
 │  5. Holt Dokumente je Message ID vom Collector            │
 │  6. Führt fachliche Assertions durch                      │
 └─────────────────────────┬─────────────────────────────────┘
                           │
          ┌────────────────┴───────────────────┐
          │                                    │
          ▼                                    ▼
 ┌────────────────────┐             ┌──────────────────────────┐
 │  SAP CPI           │             │  Collector               │
 │  (iFlow)           │             │  (HTTP-Dienst)           │
 │                    │─ POST ──▶   │                          │
 │  Monitoring-API    │  Dokument   │  Ablage:                 │
 │  (/MessageProcessingLogs)       │  {messageId}/{seqNr}     │
 └────────────────────┘             └──────────────────────────┘
```

### Komponenten im Überblick

| Komponente | Verantwortung |
|---|---|
| **JUnit-5-Testfall** | Testszenario definieren, Assertions formulieren |
| **Test-Framework** | Trigger, Warten, Correlation ID + Message IDs ermitteln, Dokumente abrufen |
| **CPI iFlow** | Nachricht verarbeiten, Ergebnis-Dokument(e) an Collector senden |
| **Collector** | Dokumente annehmen und unter Message-ID-basiertem Schlüssel ablegen |
| **CPI-Monitoring-API** | Correlation ID → Message IDs auflösen |

---

## 3. Nachrichtenfluss und Korrelation

### 3.1 Triggerphase

Der Testfall sendet eine Eingabenachricht an CPI. Die Nachricht trägt eine vom Framework generierte **Correlation ID** (z. B. als HTTP-Header oder als Teil der Payload), die CPI intern weitergibt.

### 3.2 Verarbeitungsphase (CPI-seitig)

CPI verarbeitet die Nachricht. Jeder verarbeitete iFlow-Step erhält eine eigene **Message ID**. Wenn ein iFlow mehrere Subprozesse oder Split-Schritte ausführt, entstehen entsprechend mehrere Message IDs – alle unter derselben Correlation ID.

Am Ende jedes relevanten Schrittes sendet der iFlow das Ergebnisdokument per HTTP-POST an den Collector. Der POST enthält:
- `messageId`: die CPI-seitige Message ID dieses Schrittes
- das eigentliche Dokument (Payload)

### 3.3 Abrufphase (Framework-seitig)

Das Test-Framework führt nach dem Warten folgende Schritte aus:

1. **Correlation ID lookup:** `GET /MessageProcessingLogs?correlationId={corrId}` → Liste von Message IDs
2. **Dokument-Abruf:** Für jede Message ID `GET /collector/messages/{messageId}` → Liste der gespeicherten Dokumente (Sequenz 1, 2, …)
3. **Assertion:** Testfall vergleicht die erhaltenen Dokumente mit den erwarteten Werten.

---

## 4. Fehlerszenarien

| Szenario | Verhalten |
|---|---|
| CPI verarbeitet Nachricht nicht | Framework-Timeout, Test schlägt mit `TimeoutException` fehl |
| Collector erhält kein Dokument | Kein Eintrag unter der Message ID, Test schlägt fehl |
| CPI erzeugt mehr Dokumente als erwartet | Assertion prüft Anzahl; Test schlägt fehl wenn `minCount` nicht erfüllt |
| Netzwerkfehler zum Collector | Retry-Logik im iFlow (konfigurierbar); Test wartet bis Timeout |

---

## 5. Konfiguration und Umgebungen

Tests werden über eine Konfigurationsdatei (z. B. `test.properties` oder `application-test.yml`) gegen eine bestimmte CPI-Tenant-Umgebung ausgeführt. Typische Konfigurationsparameter:

```yaml
cpi:
  baseUrl: https://<tenant>.it-cpi.cfapps.eu10.hana.ondemand.com
  clientId: ...
  clientSecret: ...
  tokenUrl: ...

collector:
  baseUrl: https://<collector-host>/collector

test:
  defaultTimeoutSeconds: 30
  pollingIntervalSeconds: 2
```

---

## 6. Offene Punkte

| Nr. | Thema | Stand |
|---|---|---|
| 1 | Authentifizierung CPI-Monitoring-API (OAuth-Scope) | In Klärung |
| 2 | Retention-Zeitraum des Collectors (automatisches Cleanup) | Offen |
| 3 | Parallelbetrieb mehrerer Testläufe (Isolation) | Über Test-Run-ID gelöst (siehe Collector-Konzept) |

---

## 7. Bewusst verworfene Alternativen

| Alternative | Verwurfsgrund |
|---|---|
| Collector gruppiert nach Correlation ID | Verkompliziert den Collector; die fachliche Zuordnung gehört ins Framework |
| Direkte DB-Abfrage statt Collector | Kopplung ans CPI-Datenbankschema; nicht supportet |
| Polling auf CPI-Monitoring-API für Dokumente | Monitoring-API liefert keine Payload-Dokumente, nur Status |

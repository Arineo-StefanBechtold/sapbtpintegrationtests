# Umsetzungsleitfaden: Agenten-Tasks für das CPI-Integrationstest-Framework

## Übersicht

Dieser Leitfaden beschreibt, in welcher Reihenfolge und mit welchen Aufgaben Agenten (z. B. Copilot Coding Agent) die drei Konzepte in lauffähigen Code umsetzen sollen. Jede Aufgabe ist so klein geschnitten, dass sie unabhängig von den anderen abgeschlossen, getestet und gemergt werden kann.

---

## Terminologie (gemeinsames Begriffsmodell)

| Begriff | Bedeutung |
|---|---|
| **Correlation ID** | Vom Test-Framework generierter Schlüssel, der CPI zur Verfügung gestellt wird. Wird genutzt, um über die CPI-Monitoring-API alle zugehörigen Message IDs zu ermitteln. |
| **Message ID** | Von CPI pro verarbeitetem iFlow-Schritt vergebene ID. Mehrere Message IDs können zur gleichen Correlation ID gehören. |
| **Test-Run-ID** | UUID, die das Framework pro Testlauf generiert. Dient der Isolation paralleler Testläufe im Collector. |
| **Dokument** | Payload, den ein iFlow-Schritt an den Collector sendet. Mehrere Dokumente je Message ID sind möglich. |
| **Sequenznummer** | Fortlaufende Zahl, die der Collector beim Eingang eines Dokuments innerhalb einer Message-ID-Gruppe vergibt. |

---

## Implementierungsreihenfolge

### Phase 1: Collector

**Warum zuerst?** Der Collector ist die einfachste und unabhängigste Komponente. Er hat keine Abhängigkeiten zu CPI und ermöglicht sofortiges End-to-End-Testen der Ablage- und Abruflogik.

---

#### Task 1.1 – Collector: Projekt-Setup

**Ziel:** Lauffähiges Spring Boot- oder Node.js-Projekt mit Health-Endpunkt.

**Akzeptanzkriterien:**
- `GET /health` gibt HTTP 200 zurück.
- Projekt baut fehlerfrei durch (`mvn package` oder `npm run build`).
- README mit Startanleitung vorhanden.

---

#### Task 1.2 – Collector: POST-Endpunkt (Dokument einliefern)

**Ziel:** `POST /collector/messages` nimmt Dokumente entgegen und legt sie unter `{testRunId}/{messageId}/{sequenceNumber}` ab.

**Akzeptanzkriterien:**
- Pflicht-Header `X-Message-Id` wird validiert (HTTP 400 bei fehlendem Header).
- Optionaler Header `X-Test-Run-Id`; wenn nicht gesetzt, wird `default` verwendet.
- Sequenznummer wird automatisch, thread-sicher hochgezählt.
- Response: HTTP 201 mit `{ "key": "{testRunId}/{messageId}/{seqNr}" }`.
- Einheit: Unit-Tests für Zähler-Logik und Validierung vorhanden.

---

#### Task 1.3 – Collector: GET-Endpunkt (Dokumente abrufen)

**Ziel:** `GET /collector/messages/{messageId}?testRunId={id}` gibt alle gespeicherten Dokumente zurück.

**Akzeptanzkriterien:**
- Gibt leeres `documents`-Array zurück, wenn keine Dokumente vorhanden.
- Gibt alle Dokumente in aufsteigender Sequenznummer-Reihenfolge zurück.
- Integration-Test: POST → GET liefert dieselbe Payload zurück.

---

#### Task 1.4 – Collector: Hilfendpunkte und Cleanup

**Ziel:** `GET /collector/runs/{testRunId}/messageIds` und `DELETE /collector/runs/{testRunId}`.

**Akzeptanzkriterien:**
- Liste aller Message IDs eines Testlaufs wird korrekt zurückgegeben.
- DELETE entfernt alle Dokumente des Testlaufs; HTTP 204.
- Unit-Tests vorhanden.

---

#### Task 1.5 – Collector: Deployment auf BTP Cloud Foundry

**Ziel:** `manifest.yml` + Deployment-Anleitung; Collector läuft auf BTP.

**Akzeptanzkriterien:**
- `cf push` deployt erfolgreich.
- Health-Endpunkt ist über externe URL erreichbar.
- Konfiguration (Port, Retention-Zeit) über Umgebungsvariablen steuerbar.

---

### Phase 2: CPI-Monitoring-Client

**Warum als Zweites?** Der Monitoring-Client bildet die Brücke zwischen CPI und dem Framework. Er kann mit Mock-Responses entwickelt und getestet werden.

---

#### Task 2.1 – CpiMonitoringClient: OAuth-Authentifizierung

**Ziel:** `CpiMonitoringClient` holt OAuth-Token und fügt es automatisch in Requests ein.

**Akzeptanzkriterien:**
- Token wird gecacht und bei Ablauf automatisch erneuert.
- Unit-Test mit Mock-Token-Endpunkt.

---

#### Task 2.2 – CpiMonitoringClient: waitForCompletion

**Ziel:** Polling auf `GET /MessageProcessingLogs?correlationId={id}` bis Status `COMPLETED`/`FAILED` oder Timeout.

**Akzeptanzkriterien:**
- Wirft `CpiTimeoutException` bei Überschreitung.
- Wirft `CpiProcessingException` bei CPI-Fehler-Status.
- Polling-Intervall und Timeout konfigurierbar.
- Unit-Test mit simulierten Status-Wechseln.

---

#### Task 2.3 – CpiMonitoringClient: resolveMessageIds

**Ziel:** Gibt alle Message IDs zu einer Correlation ID zurück.

**Akzeptanzkriterien:**
- `GET /MessageProcessingLogs?correlationId={id}&$select=messageId` wird korrekt aufgerufen.
- Liste der Message IDs wird zurückgegeben.
- Leere Liste bei unbekannter Correlation ID (kein Fehler).
- Unit-Test mit Mock-Response.

---

### Phase 3: CollectorClient im Framework

---

#### Task 3.1 – CollectorClient: fetchDocuments

**Ziel:** Ruft für eine Liste von Message IDs alle Dokumente vom Collector ab.

**Akzeptanzkriterien:**
- `GET /collector/messages/{messageId}?testRunId={id}` wird für jede Message ID aufgerufen.
- Ergebnis: `Map<String, List<Document>>` (messageId → Dokumente).
- Unit-Test mit Mock-Collector.

---

### Phase 4: CpiTestFramework – Integration

---

#### Task 4.1 – CpiTestFramework: Gesamtorchestierung

**Ziel:** `CpiTestFramework` fasst Trigger, `waitForCompletion`, `resolveMessageIds` und `fetchDocuments` zusammen.

**Akzeptanzkriterien:**
- Alle Schritte werden in der richtigen Reihenfolge ausgeführt.
- `testRunId` wird automatisch pro Framework-Instanz generiert.
- Integration-Test mit Wiremock (simuliertes CPI + simulierter Collector).

---

#### Task 4.2 – CpiTestExtension: JUnit-5-Extension

**Ziel:** `CpiTestExtension` implementiert `BeforeEachCallback`/`AfterEachCallback` und injiziert `CpiTestFramework` in Testklassen.

**Akzeptanzkriterien:**
- `@ExtendWith(CpiTestExtension.class)` funktioniert in einem einfachen Testfall.
- `testRunId` wird je Testfall neu generiert.
- Nach dem Test wird optionaler Cleanup am Collector ausgelöst (konfigurierbar).

---

### Phase 5: Beispiel-Testfall und Dokumentation

---

#### Task 5.1 – Beispiel-Testfall

**Ziel:** Lauffähiger Testfall gegen eine Test-CPI-Umgebung, der den vollständigen Ablauf demonstriert.

**Akzeptanzkriterien:**
- Testfall ist im `src/test/java`-Verzeichnis.
- Testfall läuft grün gegen eine konfigurierte Test-CPI-Umgebung.
- `README.md` erklärt, wie die Umgebungsvariablen gesetzt werden.

---

#### Task 5.2 – Dokumentation und CI-Pipeline

**Ziel:** GitHub Actions Workflow, der Collector und Framework baut und testet.

**Akzeptanzkriterien:**
- `mvn test` / `npm test` läuft in CI grün.
- Konfiguration über Repository Secrets.
- README-Abschnitt „CI/CD-Setup" vorhanden.

---

## Abhängigkeiten zwischen Tasks

```
Task 1.1 → 1.2 → 1.3 → 1.4 → 1.5
Task 2.1 → 2.2 → 2.3
                         ↓
Task 3.1 ────────────────┤
                         ↓
              Task 4.1 → 4.2
                         ↓
              Task 5.1 → 5.2
```

Phase 1 (Collector) und Phase 2 (Monitoring-Client) können **parallel** entwickelt werden.

---

## Hinweise für Agenten

- **Jede Task** beginnt mit: Repository forken/branch erstellen, relevante Konzeptdokumente lesen, bestehenden Code prüfen.
- **Jede Task** endet mit: Tests grün, PR erstellt, Kurzbeschreibung der Änderungen im PR-Body.
- Der Collector soll **keine Correlation-ID-Logik** enthalten. Falls eine solche Logik vorgeschlagen wird, ist das ein Fehler.
- Die Message-ID-Auflösung über die CPI-Monitoring-API gehört ausschließlich in `CpiMonitoringClient.resolveMessageIds`.

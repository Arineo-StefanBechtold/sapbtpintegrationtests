# Konzept: Collector – technische Spezifikation (BTP)

## Überblick

Der Collector ist ein schlanker HTTP-Dienst, der von CPI-iFlows gesendete Ergebnisdokumente entgegennimmt und für das Test-Framework abrufbar macht. Er hat **keine fachliche Intelligenz** und kennt keine Correlation IDs – die Gruppierung und Zuordnung über Correlation IDs ist ausschließlich Aufgabe des Test-Frameworks.

---

## 1. Ablagemodell

### Schlüsselstruktur

Jedes gespeicherte Dokument wird unter folgendem Schlüssel abgelegt:

```
{testRunId} / {messageId} / {sequenceNumber}
```

| Teil | Beschreibung |
|---|---|
| `testRunId` | Optionaler Präfix zur Isolation paralleler Testläufe. Wird vom Framework beim Teststart generiert (z. B. UUID). |
| `messageId` | Die CPI-seitige Message ID des sendenden iFlow-Schritts. |
| `sequenceNumber` | Fortlaufende Nummer innerhalb dieser `messageId`-Gruppe (1, 2, 3, …). Wird vom Collector beim Eingang automatisch vergeben. |

**Mehrere Dokumente je Message ID sind ausdrücklich vorgesehen** – z. B. wenn ein iFlow-Schritt mehrfach aufgerufen wird oder mehrere Teilnachrichten sendet.

### Beispiel

Ein iFlow sendet zwei Teilnachrichten für `messageId = MSG-001`:

```
testRun-abc123 / MSG-001 / 1   →  { "orderId": "4711" }
testRun-abc123 / MSG-001 / 2   →  { "orderId": "4712" }
```

Für eine andere Message ID desselben Testlaufs:

```
testRun-abc123 / MSG-002 / 1   →  { "status": "POSTED" }
```

---

## 2. REST-API

### 2.1 Dokument einliefern (CPI → Collector)

```
POST /collector/messages
```

**Request-Header:**

| Header | Pflicht | Beschreibung |
|---|---|---|
| `X-Message-Id` | ja | CPI Message ID |
| `X-Test-Run-Id` | nein | Test-Run-Isolierungsschlüssel |
| `Content-Type` | ja | z. B. `application/xml`, `application/json` |

**Request-Body:** Das zu speichernde Dokument (beliebiger Content-Type).

**Response:**

```json
{ "key": "testRun-abc123/MSG-001/1" }
```

HTTP 201 Created.

---

### 2.2 Alle Dokumente zu einer Message ID abrufen (Framework → Collector)

```
GET /collector/messages/{messageId}?testRunId={testRunId}
```

**Response:**

```json
{
  "messageId": "MSG-001",
  "documents": [
    { "sequenceNumber": 1, "contentType": "application/xml", "body": "..." },
    { "sequenceNumber": 2, "contentType": "application/xml", "body": "..." }
  ]
}
```

HTTP 200 OK. Falls keine Dokumente vorhanden: leeres `documents`-Array.

---

### 2.3 Alle Message IDs eines Testlaufs auflisten

```
GET /collector/runs/{testRunId}/messageIds
```

**Response:**

```json
{ "testRunId": "testRun-abc123", "messageIds": ["MSG-001", "MSG-002"] }
```

Nützlich für Debugging und Cleanup.

---

### 2.4 Testlauf bereinigen (optional)

```
DELETE /collector/runs/{testRunId}
```

Löscht alle gespeicherten Dokumente des angegebenen Testlaufs. HTTP 204 No Content.

---

## 3. Nicht-funktionale Anforderungen

| Aspekt | Anforderung |
|---|---|
| Persistenz | In-memory für einfache Deployments; persistent (z. B. Redis, Blob Storage) für Produktivbetrieb |
| Retention | Automatisches Cleanup nach konfigurierter Haltezeit (Standard: 24 h) |
| Sicherheit | Endpunkte durch API-Key oder OAuth abgesichert |
| Skalierung | Stateless-Design, horizontal skalierbar (bei persistentem Backend) |
| Fehlertoleranz | Idempotentes POST: Doppeleinlieferung führt zu neuem Sequenzeintrag, nicht zu Fehler |

---

## 4. Sequenzdiagramm: Einliefern und Abrufen

```
  CPI iFlow           Collector              Test-Framework
      │                   │                        │
      │── POST /messages ─▶│                        │
      │   X-Message-Id: M1 │ speichert M1/1         │
      │── POST /messages ─▶│                        │
      │   X-Message-Id: M1 │ speichert M1/2         │
      │── POST /messages ─▶│                        │
      │   X-Message-Id: M2 │ speichert M2/1         │
      │                   │                        │
      │                   │◀── GET /messages/M1 ───│
      │                   │─── [{seq:1},{seq:2}] ──▶│
      │                   │◀── GET /messages/M2 ───│
      │                   │─── [{seq:1}] ──────────▶│
```

---

## 5. Implementierungshinweise

- Der Collector ist bewusst **ohne fachliche Logik** gehalten: Er kennt weder Correlation IDs noch Testfall-Semantik.
- Die Zuordnung „Welche Message IDs gehören zu meinem Testlauf?" beantwortet das Test-Framework über die CPI-Monitoring-API, nicht der Collector.
- `testRunId` dient ausschließlich der Isolation paralleler Läufe; er ist kein fachlicher Schlüssel.
- Der Collector kann als einfache Spring Boot- oder Node.js-Anwendung auf BTP Cloud Foundry deployt werden.

---

## 6. Offene Punkte

| Nr. | Thema | Stand |
|---|---|---|
| 1 | Persistenz-Backend für Produktivbetrieb (Redis vs. Blob Storage) | Offen |
| 2 | Authentifizierungsmodell für CPI-seitige POSTs | In Klärung |
| 3 | Maximale Dokumentgröße pro Einlieferung | Offen (Vorschlag: 10 MB) |

---

## 7. Bewusst verworfene Alternativen

| Alternative | Verwurfsgrund |
|---|---|
| Collector gruppiert nach Correlation ID | Erfordert, dass der Collector die Correlation ID kennt und weitergibt – unnötige Kopplung; die Gruppierung gehört ins Framework |
| Eine Datei pro Correlation ID | Nicht möglich, wenn mehrere Message IDs unterschiedliche Subprozesse abbilden |
| Collector hält eigene Datenbank mit Beziehungsmodell | Zu komplex; widerspricht dem Ziel eines schlanken, zustandslosen Dienstes |

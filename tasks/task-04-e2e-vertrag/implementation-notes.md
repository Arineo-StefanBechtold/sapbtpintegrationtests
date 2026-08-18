# Implementation Notes — Task 04 E2E Vertrag

## Kernidee
Dieser Task ist die Integrationsabsicherung für alles, was in den vorherigen Tasks gebaut wurde.

## Prüfbereiche
- API-Vertrag
- Datenfluss
- Message-ID-basierter Abruf
- Ledger und Zuordnung
- Fehlerdiagnose
- Residualprüfung

## Leitplanken
- Nur verifizieren, nicht neu erfinden
- Fixtures bevorzugen
- Klare Trennung zwischen Infrastruktur- und Fachfehlern
- E2E-Tests müssen stabil und aussagekräftig sein

## Technische Hinweise
- Sammle Diagnoseartefakte an einem klaren Ort.
- Nutze echte oder aufgezeichnete Antworten nur bewusst und kontrolliert.
- Vermeide zu große, monolithische E2E-Szenarien.
- Ein einzelner sauberer End-to-End-Test ist wertvoller als viele fragile.

## Technologieentscheidungen

### Fixtures
- Wiederverwendung bestehender Recordings statt Neuerfindung: Collector-Antworten aus `tasks/task-01-collector/testdata/`, Monitoring-Antworten aus `tasks/task-02-framework/testdata/recordings/`.
- Zusätzlich synthetische E2E-Request-/Expected-Paare, abgeleitet aus den Suite-Beispielfällen `order-created`/`order-cancelled`.
- Der Collector läuft als echte lokale Instanz (kein Mock), um den tatsächlichen Vertrag zu prüfen. Die Monitoring-API wird über WireMock mit den aufgezeichneten Antworten simuliert — kein Live-Tenant nötig.

### Reports
- Standard Gradle/JUnit-Testreport (HTML+XML) als Basis.
- Zusätzlich `reports/e2e-summary.json` (Runübersicht), `reports/contract-mismatches.md` (menschenlesbare Liste von Vertragsabweichungen) und `reports/residual-state.json` (Snapshot überzähliger Dokumente je Run).

### Vertragsbruch-Definition
- Als Vertragsbruch zählen ausschließlich Schema-/Status-/Struktur-Abweichungen: falscher HTTP-Status, fehlendes Pflichtfeld, abweichender Content-Type, Schema-Verletzung in Manifest- oder Header-Struktur.
- Inhaltliche Payload-Abweichungen (z. B. falscher Bestellwert) sind kein Vertragsbruch, sondern ein regulärer fachlicher Testfehler der Suite.

### Mindestdiagnose
- Jede Fehlermeldung bzw. jeder Report-Eintrag enthält mindestens: `runId`, `correlationId`, alle beteiligten `messageIds`, den verletzten Vertragsteil (Endpunkt + erwartetes vs. tatsächliches Schema/Status), einen Zeitstempel und einen gekürzten Response-Ausschnitt (ohne Secrets).

## Hinweis an Agenten
Wenn ein Fehler auftaucht, zuerst Vertragsbruch vs. Testfehler unterscheiden.
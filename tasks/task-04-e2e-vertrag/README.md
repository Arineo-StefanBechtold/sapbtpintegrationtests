# Task 04 — E2E Vertrag

## Ziel
Die gesamte Kette aus Collector, Framework und Suite-Template gemeinsam absichern.

## Kontext
Dieser Task dient dazu, die Vertrags- und Integrationsbeziehungen zwischen den Bausteinen zu prüfen. Es geht nicht um neue Fachlogik, sondern um das zuverlässige Zusammenspiel.

## Umfang
- End-to-End-Tests über die komplette Kette
- Vertragsabgleich zwischen Framework und Collector
- Stabilisierung der Abläufe
- Lauf- und Diagnoseberichte
- Verifikation der wichtigsten Invarianten

## Nicht im Umfang
- Neue Collector-Funktionen
- Neue Framework-Grundfunktionen
- Neue fachliche Testfälle für ein Kundenprojekt
- Produktionsbetrieb

## Artefakte
- `requirements.md`
- `acceptance-tests.md`
- `implementation-notes.md`
- `fixtures/`
- `reports/`

## Ausführung
- Collector-Tests: `cd /home/runner/work/sapbtpintegrationtests/sapbtpintegrationtests/tasks/task-01-collector && npm test`
- Framework/Suite/E2E: `cd /home/runner/work/sapbtpintegrationtests/sapbtpintegrationtests && gradle test`
- Die E2E-Tests starten den Collector lokal und simulieren CPI-Monitoring über WireMock.

## Berichte
Die Task schreibt zusätzlich zu den Gradle-/JUnit-Reports folgende Artefakte nach `reports/`:
- `e2e-summary.json`
- `contract-mismatches.md`
- `residual-state.json`

## Leitplanken
- Dieser Task prüft das Zusammenspiel, nicht neue Features.
- Tests müssen reproduzierbar sein.
- Diagnose ist wichtiger als knappe Fehlertexte.
- Die wichtigsten Pfade müssen mindestens einmal zusammen grün laufen.

## Abschluss
Der Task ist fertig, wenn Collector, Framework und Suite-Vorlage in einem konsistenten End-to-End-Szenario zusammenarbeiten.

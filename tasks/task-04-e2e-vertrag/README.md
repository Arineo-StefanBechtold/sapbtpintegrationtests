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

## Leitplanken
- Dieser Task prüft das Zusammenspiel, nicht neue Features.
- Tests müssen reproduzierbar sein.
- Diagnose ist wichtiger als knappe Fehlertexte.
- Die wichtigsten Pfade müssen mindestens einmal zusammen grün laufen.

## Abschluss
Der Task ist fertig, wenn Collector, Framework und Suite-Vorlage in einem konsistenten End-to-End-Szenario zusammenarbeiten.
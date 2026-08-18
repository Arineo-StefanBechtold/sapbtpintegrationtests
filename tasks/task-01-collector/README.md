# Task 01 — Collector

## Ziel
Den Collector als schlanken Ablage- und Abrufdienst für CPI-Integrationstests umsetzen.

## Kontext
Der Collector speichert eingehende Dokumente message-id-basiert mit fortlaufender Sequenznummer pro Gruppe. Eine Message ID kann mehrere Dokumente haben. Der Collector trifft keine fachlichen Zuordnungen, keine Korrelationsermittlung und keine Testentscheidungen.

## Umfang
- Projektgerüst für den Collector
- Ingest-Endpunkt
- Message-ID-basierte Ablage
- Sequenzierung mehrerer Dokumente je Message ID
- Abruf der gespeicherten Dokumente
- Betriebs- und Statusendpunkte
- Minimale Doku und Testbasis

## Nicht im Umfang
- CPI-Monitoring-Anbindung
- Correlation-ID-Auflösung
- Testframework / JUnit-Integration
- Suite-Template
- E2E-Verifikation gegen einen echten Tenant

## Artefakte
- `requirements.md`
- `acceptance-tests.md`
- `implementation-notes.md`
- `api-contract/openapi.yaml`
- `testdata/`

## Leitplanken
- Der Collector bleibt zustandslos im Prozess.
- Der Collector trifft keine fachlichen Annahmen.
- Nutzdaten werden unverändert gespeichert und wieder ausgegeben.
- Mehrere Dokumente pro Message ID sind zulässig.
- Sequenznummern sind Teil der Ablageadressierung, nicht fachliche Identität.

## Abschluss
Der Task gilt als fertig, wenn der Collector lokal läuft, Dokumente korrekt ablegt und wieder bereitstellt, und die Abnahmekriterien erfüllt sind.

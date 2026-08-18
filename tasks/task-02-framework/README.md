# Task 02 — Framework

## Ziel
Ein wiederverwendbares JUnit-5-basiertes Framework für CPI-Integrationstests erstellen.

## Kontext
Das Framework ermittelt Correlation IDs über die CPI-Monitoring-API, verwaltet das Ledger und holt über die ermittelten Message IDs die passenden Dokumente aus dem Collector ab.

## Umfang
- Konfiguration und Profilverwaltung
- CPI-Monitoring-Anbindung
- Collector-Client
- Request-/Response-DSL
- JUnit-Extensions
- Ledger
- Wartelogik und Nachlaufprüfung
- Hilfen für Golden-Master- und Dokumentenvergleich

## Nicht im Umfang
- Collector-Implementierung selbst
- Projektspezifische Testfälle der Suite-Vorlage
- Produktive Tenant-Konfiguration
- Fachlogik der CPI-Flows

## Artefakte
- `requirements.md`
- `acceptance-tests.md`
- `implementation-notes.md`
- `docs/`
- `testdata/recordings/`

## Leitplanken
- Das Framework bleibt fachlich neutral.
- Keine Business-Regeln im Framework.
- Infrastrukturlogik ist gekapselt und in Tests nicht sichtbar.
- Fehler müssen diagnostisch reichhaltig sein.

## Abschluss
Der Task ist fertig, wenn das Framework einen Collector ansprechen, Message IDs aus Monitoring-Daten ableiten und Dokumente darüber abrufen kann.
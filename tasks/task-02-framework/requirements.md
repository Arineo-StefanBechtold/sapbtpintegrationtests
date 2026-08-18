# Requirements — Task 02 Framework

## Funktionale Anforderungen

1. Das Framework lädt Konfiguration aus Profilen und Umgebungsvariablen.
2. Das Framework validiert fehlende Geheimnisse früh.
3. Das Framework kann CPI-Monitoring-Daten abfragen.
4. Das Framework kann zu einer Correlation ID alle zugehörigen Message IDs bestimmen.
5. Das Framework kann zu einer Liste von Message IDs Dokumente aus dem Collector abrufen.
6. Das Framework bietet eine testfreundliche DSL.
7. Das Framework verwaltet einen Run- bzw. Suite-Kontext.
8. Das Framework führt ein Ledger für Correlation ID → Testfall.
9. Das Framework unterstützt Nachlaufprüfung und Freigabe.
10. Das Framework bietet gute Fehlermeldungen mit Kontext.

## Nicht-funktionale Anforderungen

1. Keine sichtbare Infrastrukturlogik in den Testfällen.
2. Keine Prozesszustände als einzige Quelle der Wahrheit.
3. Die Implementierung muss testbar und reproduzierbar sein.
4. Die Implementierung muss lokale Tests ohne Tenant ermöglichen, soweit sinnvoll.
5. Die Implementierung soll austauschbare HTTP- und Vergleichsmechanismen erlauben.

## Invarianten

- Correlation ID wird im Framework ermittelt, nicht im Collector.
- Der Collector bekommt keine fachliche Intelligenz.
- Message IDs werden als Brücke zum Collector verwendet.
- Tests sollen keine Transportdetails kennen.
- Fehlermeldungen enthalten relevante IDs und Kontext.

## Abgrenzung

Das Framework ist nicht verantwortlich für:
- die konkrete fachliche Suite
- das Collector-Datenmodell
- produktive Betriebsaufgaben
- Tenant-Anlage und Geheimnisverwaltung

## Qualitätsziel

Das Framework soll als Bibliothek in einer späteren Suite wiederverwendbar sein, ohne Änderungen an den Testfällen zu erzwingen.
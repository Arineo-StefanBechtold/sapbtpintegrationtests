# Requirements — Task 01 Collector

## Funktionale Anforderungen

1. Der Collector nimmt HTTP-Nachrichten an.
2. Jede Nachricht wird unter einer Message-ID-basierten Struktur abgelegt.
3. Mehrere Dokumente mit derselben Message ID können gespeichert werden.
4. Jede weitere Nachricht derselben Message ID erhält eine fortlaufende Sequenznummer.
5. Nutzdaten werden unverändert gespeichert.
6. HTTP-Header werden vollständig gespeichert.
7. Der Collector bietet einen Status- bzw. Health-Endpunkt.
8. Der Collector bietet Abrufendpunkte für Nutzdaten und Header.
9. Der Collector bietet einen Manifest- bzw. Übersichtsabruf.
10. Der Collector bietet Freigabe- bzw. Löschoperationen für gespeicherte Gruppen oder Läufe.

## Nicht-funktionale Anforderungen

1. Der Collector soll so einfach wie möglich bleiben.
2. Keine fachliche Verarbeitung im Collector.
3. Keine Speicherung im Prozessspeicher als einzige Wahrheit.
4. Die Ablage muss deterministisch und testbar sein.
5. Die Implementierung muss lokal ausführbar sein.
6. Die Implementierung muss für spätere Cloud-Foundry-Nutzung geeignet sein.

## Invarianten

- Keine Correlation-ID-basierte Gruppierung im Collector.
- Keine Normalisierung der Nutzdaten.
- Kein Umformatieren von Payload oder Headern.
- Keine fachliche Auswahl von Dokumenten im Collector.
- Mehrere Dokumente pro Message ID sind erlaubt.
- Sequenznummern verhindern Überschreiben.

## Abgrenzung

Der Collector ist nicht verantwortlich für:
- Korrelationsermittlung über CPI-Monitoring
- Zuordnung von Dokumenten zu Testfällen
- Golden-Master-Vergleiche
- JUnit-Extensions oder Teststeuerung

## Qualitätsziel

Die API ist so klar beschrieben, dass das Framework sie gegen eine OpenAPI-Beschreibung testen kann.

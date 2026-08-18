# Acceptance Tests — Task 04 E2E Vertrag

## 1. Gesamtintegration
- [ ] Ein End-to-End-Beispieltest läuft durch die komplette Kette.
- [ ] Der Test nutzt Collector, Framework und Suite-Mechanik zusammen.

## 2. Vertragsprüfung
- [ ] Die erwarteten API-Antworten entsprechen dem Collector-Vertrag.
- [ ] Das Framework interpretiert die Collector-Antworten korrekt.
- [ ] Abweichungen werden klar gemeldet.

## 3. Dokumentabruf
- [ ] Dokumente werden über die vom Framework ermittelte Message-ID-Liste abgeholt.
- [ ] Mehrere Dokumente pro Message ID sind korrekt behandelbar.
- [ ] Roh-Payload und Header werden getrennt geprüft.

## 4. Diagnose
- [ ] Fehler enthalten Run-ID, Correlation ID und Message IDs.
- [ ] Reports und Diagnoseartefakte werden erzeugt.
- [ ] Residual- oder Überhangzustände sind sichtbar.

## 5. Reproduzierbarkeit
- [ ] Der Test ist mit fixierten Fixtures wiederholbar.
- [ ] Ein Fehlerfall lässt sich lokal nachvollziehen.
- [ ] Die Ergebnisse ändern sich nicht zufällig zwischen Läufen.

## 6. Nicht-Ziele
- [ ] Keine neue Fachlogik.
- [ ] Keine zusätzlichen Produktfeatures.
- [ ] Kein Ersatz für die eigentlichen Collector- und Framework-Tasks.
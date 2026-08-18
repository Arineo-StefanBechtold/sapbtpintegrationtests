# Acceptance Tests — Task 02 Framework

## 1. Konfiguration
- [ ] Fehlende Pflichtkonfiguration führt zu einer klaren, spezifischen Fehlermeldung.
- [ ] Profile lassen sich umschalten, ohne Code zu ändern.

## 2. Monitoring
- [ ] Eine gespeicherte Monitoring-Antwort kann geparst werden.
- [ ] Das Framework kann aus Monitoring-Daten die Message IDs zu einer Correlation ID ermitteln.
- [ ] Wiederholungs- und Warteverhalten sind testbar.

## 3. Collector-Anbindung
- [ ] Das Framework kann eine Liste von Message IDs an den Collector übergeben.
- [ ] Das Framework kann Payload und Header getrennt abholen.
- [ ] Das Framework kann mehrere Dokumente je Message ID behandeln.

## 4. DSL und Lifecycle
- [ ] Die DSL erlaubt einen kompakten Testaufbau.
- [ ] Der Suite-Kontext wird sauber initialisiert und freigegeben.
- [ ] Ledger-Einträge bleiben über Testklassen hinweg erhalten.

## 5. Nachlaufprüfung
- [ ] Überzählige Dokumente werden eindeutig einem Testfall oder einem Residualfall zugeordnet.
- [ ] Fehlermeldungen enthalten Testfallname, Correlation ID, Message IDs und Run-ID.

## 6. Vergleich
- [ ] XML-/JSON-Vergleich ist gekapselt.
- [ ] Unterschiedliche, veränderliche Werte können gezielt ignoriert werden.
- [ ] Differenzen werden verständlich gemeldet.

## 7. Nicht-Ziele
- [ ] Kein Collector-Business-Logic im Framework.
- [ ] Keine fachliche Zuordnung von Dokumenten durch den Collector.
- [ ] Keine hardcodierten Tenant-spezifischen Annahmen.
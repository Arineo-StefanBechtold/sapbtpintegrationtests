# Implementation Notes — Task 02 Framework

## Kernidee
Das Framework ist die intelligente Schicht. Es kennt Monitoring, Message-ID-Liste, Collector-Zugriff, Testkontext und Fehlermeldungen.

## Wichtige Bausteine
- Konfigurationsmodul
- Monitoring-Client
- Collector-Client
- DSL / Request-Building
- Lifecycle-Extensions
- Ledger
- Vergleichsadapter
- Nachlaufprüfung

## Leitplanken
- Keine fachliche Bewertung im Framework, nur Infrastruktur und Zuordnung
- Correlation ID bleibt eine Monitoring-Aufgabe
- Message IDs dienen als Brücke zum Collector
- Gute Fehlermeldungen sind Pflicht
- Tests und Recorded Fixtures müssen wiederholbar sein

## Technische Hinweise
- Monitoring-Antworten sollten aufgezeichnet und als Testfixtures genutzt werden.
- Das Framework sollte lokale Unit- und Integrationstests haben.
- Die Collector-Schnittstelle sollte gegen das API-Contract getestet werden.
- Die Öffnung für spätere Wiederverwendung ist wichtig; keine Sackgassen bauen.

## Offene Punkte
- Konkrete HTTP-Client-Wahl
- Konkrete XML-/JSON-Vergleichsbibliothek
- Konkrete Paketnamen und Artefakt-Koordinaten
- Konkrete Konfiguration der JUnit-Extensions

## Hinweis an Agenten
Wenn eine technische Entscheidung fehlt, dokumentieren und nicht stillschweigend erfinden.
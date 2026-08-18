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

## Technologieentscheidungen

### Build-Tool & Koordinaten
- Build-Tool: Gradle.
- groupId/Namespace: `me.cxdev.testing`, artifactId: `cpi-test-framework`, Basispaket: `me.cxdev.testing.cpi`.
- Version: `1.0.0-SNAPSHOT`, packaging: `jar`.

### HTTP-Client
- OkHttp für `CpiMonitoringClient` und `CollectorClient`, jeweils hinter einem eigenen Client-Interface gekapselt (z. B. `HttpTransport`), damit die Bibliothek austauschbar bleibt und Tests mit MockWebServer möglich sind.

### Vergleichsbibliothek
- XMLUnit 2.x für XML-Vergleiche (Diff-Builder, Placeholder-/Ignore-Support).
- JSONassert für JSON-Vergleiche (lenient mode, Pfad-Ignorierung).
- Beide Bibliotheken werden hinter einem gemeinsamen `DocumentComparator`-Interface gekapselt, damit Testfälle nicht direkt von XMLUnit/JSONassert abhängen.

### JUnit-Extension-Konfiguration
- `CpiTestExtension` implementiert `BeforeAllCallback` (legt Run an, erzeugt `testRunId`), `AfterAllCallback` (Release/Cleanup des Runs) und `ParameterResolver` (injiziert `CpiTestFramework`/`CpiTestConfig` als Konstruktor- oder Methodenparameter).
- Konfiguration erfolgt über eine YAML-Datei mit Umgebungsvariablen-Override; die Profilwahl erfolgt über die System-Property `cpi.test.profile`.

## Hinweis an Agenten
Wenn eine technische Entscheidung fehlt, dokumentieren und nicht stillschweigend erfinden.
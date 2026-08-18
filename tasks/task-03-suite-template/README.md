# Task 03 — Suite Template

## Ziel
Eine projektspezifische Test-Suite-Vorlage auf Basis des Frameworks erstellen.

## Kontext
Die Vorlage wird pro Projekt kopiert und individualisiert. Sie enthält Testfälle, Testdaten und Konfiguration, aber keine Framework-Implementierung.

## Umfang
- Projektstruktur für die Suite
- Beispieltests
- Beispielkonfiguration
- Golden-Master-Struktur
- Einbindung des Frameworks als Abhängigkeit
- Hinweise für die Anpassung im Kundenprojekt

## Implementierter Einstieg
- Gradle-Projekt unter `src/main/resources/testcases/`
- Fachlich benannte Beispiele `order-created` und `order-cancelled`
- Beispieltest unter `src/test/java` mit Framework-DSL
- Golden-Master-Dateien pro erwartetem Dokument

## Nicht im Umfang
- Framework-Implementierung
- Collector-Implementierung
- Tenant-Anlage
- Betrieb in einer Produktivumgebung

## Artefakte
- `requirements.md`
- `acceptance-tests.md`
- `implementation-notes.md`
- `testcases/`
- `docs/`

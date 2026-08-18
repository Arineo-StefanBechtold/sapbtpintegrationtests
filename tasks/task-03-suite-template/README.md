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

## Leitplanken
- Die Suite enthält nur projektspezifische fachliche Tests.
- Keine Infrastrukturdetails in den Testfällen.
- Testdaten sind fachlich benannt.
- Golden Masters sind bewusst gepflegt.

## Abschluss
Der Task ist fertig, wenn ein neues Projekt aus der Vorlage starten kann und ein erster Testfall gegen den Collector/Framework-Stack funktioniert.
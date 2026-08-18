# Requirements — Task 03 Suite Template

## Funktionale Anforderungen

1. Die Suite hängt vom Framework als versioniertem Artefakt ab.
2. Die Suite enthält Beispieltests für den typischen CPI-Integrationsfall.
3. Die Suite enthält eine klare Verzeichnisstruktur für Testdaten.
4. Die Suite unterstützt Golden-Master-basierte Prüfungen.
5. Die Suite unterstützt Beispielkonfigurationen ohne Geheimnisse.
6. Die Suite kann mit dem Framework gegen einen lokalen oder bereitgestellten Collector laufen.
7. Die Suite ist als Vorlage kopierbar.

## Nicht-funktionale Anforderungen

1. Keine Kopie des Framework-Codes in der Suite.
2. Keine harten Tenant-Abhängigkeiten in der Vorlage.
3. Fachlich lesbare Testnamen und Dateinamen.
4. Automatisierbare Ausführung.
5. Einfache Anpassbarkeit für Kundenprojekte.

## Invarianten

- Testdaten tragen fachliche Namen, keine Laufnummern.
- Infrastruktur bleibt im Framework.
- Die Suite enthält keinen Collector-Code.
- Der Update-Modus für Golden Masters muss bewusst kontrolliert sein.

## Abgrenzung

Die Suite ist nicht zuständig für:
- CPI-Monitoring-Implementierung
- Collector-Speicherlogik
- Framework-Infrastruktur
- Kundenspezifische Tenant-Anlage

## Qualitätsziel

Ein Kollege soll die Vorlage verstehen und für ein neues Projekt verwenden können, ohne das Framework zu verändern.
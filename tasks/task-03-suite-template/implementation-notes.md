# Implementation Notes — Task 03 Suite Template

## Kernidee
Die Vorlage zeigt, wie ein Projekt mit dem Framework arbeitet, ohne selbst Infrastruktur zu erfinden.

## Empfohlene Inhalte
- Beispieltestfall
- Testdatenordner
- Golden-Master-Beispiele
- Umgebungsbeispiel ohne Geheimnisse
- kurze Einstiegshilfe

## Leitplanken
- Testnamen und Dateinamen müssen fachlich lesbar sein
- Die Vorlage darf keine Framework-Kopie werden
- Der Update-Modus für Golden Masters muss absichtlich auffallen
- Beispiele sollen realistisch, aber neutral sein

## Technische Hinweise
- Eine minimale Suite-Struktur ist besser als eine zu große Vorlage.
- Lieber ein funktionierender Testfall als viele halbfertige.
- Die Struktur muss spätere Erweiterung ermöglichen.

## Technologieentscheidungen

### Gradle-Struktur
- Eigenständiges Gradle-Projekt (`settings.gradle.kts`, `build.gradle.kts`), das `cpi-test-framework` als versionierte externe Dependency referenziert (kein Submodul, keine Kopie des Framework-Codes).
- Tests liegen unter `src/test/java`, Testdaten unter `src/test/resources/testcases`.

### Testframework-Parameter
- Beispielkonfiguration in `src/test/resources/application-local.yaml` (nur Platzhalterwerte, keine Geheimnisse), überschreibbar per Umgebungsvariablen.
- `junit-platform.properties` legt `cpi.test.profile=local` als Default fest; Parallelisierung ist in der Vorlage standardmäßig deaktiviert und kann pro Kundenprojekt gezielt aktiviert werden.

### Beispieltestfälle
- Die Vorlage enthält zwei fachlich benannte Beispielfälle: `testcases/order-created/` (einfacher Erfolgsfall, ein Zieldokument, Status `POSTED`) und `testcases/order-cancelled/` (Szenario mit Statuswechsel/zweitem Dokument je Message ID).
- Die bisherigen laufnummernbasierten Ordner `sample-001`/`sample-002` werden bei der Umsetzung entsprechend umbenannt, da fachliche Namen statt Laufnummern Pflicht sind.

### Golden-Master-Verzeichnis
- Je Testfall ein Unterordner `golden-master/` mit `expected.xml` und `expected-header.json`.
- Der Update-Modus wird explizit über die System-Property `-Dgoldenmaster.update=true` aktiviert. Im Update-Modus werden die aktuellen Ist-Werte nach `golden-master/` geschrieben, der Testlauf bleibt dabei bewusst nicht grün (WARN-Ausgabe bzw. gezielter Skip), damit Aktualisierungen nicht unbemerkt bleiben.

## Hinweis an Agenten
Die Vorlage soll Leitplanken geben, nicht das Projekt selbst abschließen.
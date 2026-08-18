# Acceptance Tests — Task 03 Suite Template

## 1. Projektstruktur
- [ ] Die Suite ist in einer klaren, kopierbaren Ordnerstruktur aufgebaut.
- [ ] Testdaten und Golden Masters sind fachlich benannt.

## 2. Framework-Anbindung
- [ ] Die Suite referenziert das Framework als Abhängigkeit.
- [ ] Kein Framework-Code wird dupliziert.

## 3. Beispieltests
- [ ] Mindestens ein vollständiger Beispieltest existiert.
- [ ] Der Beispieltest nutzt die Framework-DSL.
- [ ] Der Beispieltest enthält fachliche Assertions.

## 4. Konfiguration
- [ ] Beispielkonfigurationen funktionieren ohne Geheimnisse.
- [ ] Fehlende Umgebungsdaten führen zu einem Skip oder klaren Hinweis, nicht zu unklaren Fehlern.

## 5. Golden Master
- [ ] Die Golden-Master-Struktur ist nachvollziehbar.
- [ ] Update-Modus ist vorhanden und bewusst kontrolliert.
- [ ] Aktualisierungsmodus darf nicht unbemerkt grün bleiben.

## 6. Anpassbarkeit
- [ ] Die Vorlage kann auf ein neues Projekt übertragen werden.
- [ ] Anpassungspunkte sind dokumentiert.
- [ ] Die Struktur ist verständlich genug für eine Erstübernahme.

## 7. Nicht-Ziele
- [ ] Keine Tenant-geheimen Daten im Repo.
- [ ] Kein eigener Collector.
- [ ] Kein Framework-Fork in der Vorlage.
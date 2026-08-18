# Acceptance Tests — Task 01 Collector

## 1. Health
- [ ] `GET /health` liefert bei laufender Ablage `200`.
- [ ] `GET /health` liefert bei nicht erreichbarer Ablage `503`.

## 2. Ingest
- [ ] `POST /collect` nimmt eine Nachricht mit Payload und Headern an.
- [ ] Eine Nachricht ohne aktive Laufsteuerung wird nicht abgewiesen, sondern sinnvoll abgelegt.
- [ ] Die Antwort enthält Message-ID, Sequenznummer und Speicherort oder äquivalente Metadaten.

## 3. Message-ID-basierte Ablage
- [ ] Zwei Nachrichten mit derselben Message ID werden getrennt gespeichert.
- [ ] Die zweite Nachricht erhält eine andere Sequenznummer als die erste.
- [ ] Keine Nachricht überschreibt eine vorherige Nachricht derselben Message ID.

## 4. Abruf
- [ ] Gespeicherte Payloads können roh wieder abgefragt werden.
- [ ] Gespeicherte Header können als JSON oder strukturierte Form wieder abgefragt werden.
- [ ] Payload und Header einer Nachricht sind getrennt abrufbar.
- [ ] Ein Manifest listet alle gespeicherten Einträge einer Gruppe.

## 5. Löschung / Freigabe
- [ ] Gespeicherte Einträge können freigegeben oder gelöscht werden.
- [ ] Nach Freigabe ist die Gruppe nicht mehr abrufbar.
- [ ] Nach Freigabe neu eintreffende Nachrichten werden nicht stillschweigend einer alten Gruppe zugeordnet.

## 6. Stabilität
- [ ] Die API ist in einem lokalen Testlauf reproduzierbar.
- [ ] Die Abnahme kann mit automatisierten Tests erfolgen.
- [ ] Alle Endpunkte sind gegen den Vertrag abgesichert.

## 7. Nicht-Ziele
- [ ] Keine Correlation-ID-Gruppierung im Collector.
- [ ] Keine fachliche Bewertung von Dokumenten.
- [ ] Kein Zugriff auf CPI-Monitoring oder Tenant-spezifische Logik.

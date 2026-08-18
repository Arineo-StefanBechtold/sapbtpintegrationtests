# Implementation Notes — Task 01 Collector

## Grundprinzip
Der Collector soll bewusst simpel bleiben. Alles, was im Framework liegen kann, bleibt dort.

## Datenmodell
- Ablage auf Basis von Message ID
- Zusätzliche Sequenznummer pro Nachricht innerhalb derselben Message-ID-Gruppe
- Unveränderte Payload
- Vollständige Header-Speicherung
- Manifest für Übersicht und Abruf

## Wichtige Leitplanken
- Keine Correlation-ID-Gruppierung
- Keine fachliche Interpretation
- Keine Normalisierung der Daten
- Keine implizite Sortierung, außer technisch zur Ablage notwendig
- Keine Prozessspeicher-Logik als einzige Wahrheit

## Technische Hinweise
- Die API sollte früh über OpenAPI beschrieben werden.
- Alle Antwortstrukturen sollten testbar sein.
- Ein lokaler Testmodus ist Pflicht.
- Gesundheit und Speichererreichbarkeit müssen separat prüfbar sein.

## Offene Punkte
- Konkrete Technologiewahl für Laufzeit und Paketmanager
- Konkretes Format der Message-ID-basierten Pfadstruktur
- Konkrete Regel für Sequenznummern
- Konkrete TTL-/Aufräumstrategie

## Hinweis an Agenten
Wenn eine Anforderung unklar erscheint, nicht selbst umdeuten, sondern im Zweifel sichtbar markieren und rückfragen.

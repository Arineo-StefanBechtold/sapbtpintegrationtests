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

## Technologieentscheidungen

### Laufzeit & Paketmanager
- Node.js 22 LTS, Paketmanager npm.
- Web-Framework: Fastify (schlank, gute native Unterstützung für Streams/rohe Binary-Bodies, passt zur Anforderung "Payload unverändert speichern").
- Plain JavaScript (kein TypeScript-Build-Schritt), um die Implementierung so einfach wie möglich zu halten.
- Deploybar als klassische Cloud-Foundry-Node-App (nodejs_buildpack) auf BTP, lokal per `npm start` startbar.

### Pfadstruktur der Ablage
- Basisverzeichnis konfigurierbar (z. B. `COLLECTOR_DATA_DIR`, lokal Default `./data`).
- Struktur: `data/{testRunId}/{messageId}/{sequenceNumber}.payload` (rohe Bytes) und `data/{testRunId}/{messageId}/{sequenceNumber}.headers.json` (vollständige Header als JSON).
- `X-Test-Run-Id` ist optional; fehlt der Header, wird die feste testRunId `default` verwendet — es gibt also immer eine Gruppierungsebene.
- Pro Run existiert ein `manifest.json` im Run-Verzeichnis, das alle Message-IDs mit ihren Sequenznummern, Content-Type und Zeitstempel auflistet und bei jeder Ablage aktualisiert wird.

### Sequenznummern-Vergabe
- Sequenznummern beginnen bei 1 pro `(testRunId, messageId)`-Gruppe und werden fortlaufend vergeben.
- Da Node zwar single-threaded ist, async I/O aber interleaven kann, wird die Vergabe pro Gruppe über eine In-Memory-Promise-Chain (Mutex je Schlüssel `testRunId/messageId`) serialisiert, um Race Conditions bei parallelen Requests auf dieselbe Message ID zu verhindern.
- Der Prozessspeicher ist dabei nicht die alleinige Wahrheit: Beim Start des Collectors wird das Datenverzeichnis gescannt und die jeweils höchste vorhandene Sequenznummer je Gruppe rekonstruiert, sodass die Vergabe nach einem Neustart nahtlos fortgesetzt wird.

### TTL- / Aufräumstrategie
- Auto-TTL ist standardmäßig aktiv — auch lokal — über einen periodischen Sweep-Job (z. B. alle 10 Minuten).
- TTL ist konfigurierbar über `COLLECTOR_TTL_HOURS`, Default kurz (z. B. 2 Stunden), da Test-Runs kurzlebig sind.
- Der Zeitstempel je Run wird beim ersten eingehenden Dokument im `manifest.json` festgehalten und dient als Grundlage für die TTL-Prüfung.
- Zusätzlich bleiben die expliziten Endpunkte (`DELETE /runs/{runId}`, Release je Message-Gruppe) der primäre Weg, um Runs gezielt vor Ablauf der TTL zu entfernen.

## Hinweis an Agenten
Wenn eine Anforderung unklar erscheint, nicht selbst umdeuten, sondern im Zweifel sichtbar markieren und rückfragen.

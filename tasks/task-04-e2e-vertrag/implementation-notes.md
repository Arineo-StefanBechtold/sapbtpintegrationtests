# Implementation Notes — Task 04 E2E Vertrag

## Kernidee
Dieser Task ist die Integrationsabsicherung für alles, was in den vorherigen Tasks gebaut wurde.

## Prüfbereiche
- API-Vertrag
- Datenfluss
- Message-ID-basierter Abruf
- Ledger und Zuordnung
- Fehlerdiagnose
- Residualprüfung

## Leitplanken
- Nur verifizieren, nicht neu erfinden
- Fixtures bevorzugen
- Klare Trennung zwischen Infrastruktur- und Fachfehlern
- E2E-Tests müssen stabil und aussagekräftig sein

## Technische Hinweise
- Sammle Diagnoseartefakte an einem klaren Ort.
- Nutze echte oder aufgezeichnete Antworten nur bewusst und kontrolliert.
- Vermeide zu große, monolithische E2E-Szenarien.
- Ein einzelner sauberer End-to-End-Test ist wertvoller als viele fragile.

## Offene Punkte
- Welche Fixtures werden dafür genutzt?
- Welche Reports sind für die Pipeline erforderlich?
- Welche Abweichungen gelten als Vertragsbruch?
- Welche Mindestdiagnose wird erwartet?

## Hinweis an Agenten
Wenn ein Fehler auftaucht, zuerst Vertragsbruch vs. Testfehler unterscheiden.
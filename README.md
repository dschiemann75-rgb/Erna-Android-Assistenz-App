# Erna — Deine persönliche KI-Assistentin

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-M3-purple.svg)](https://developer.android.com/jetpack/compose)
[![Database](https://img.shields.io/badge/Database-Room%20%2F%20SQLite-orange.svg)](https://developer.android.com/training/data-storage/room)

**Erna** ist eine hochentwickelte, futuristische KI-Assistentin für Android, maßgeschneidert als virtueller Begleiter für das Samsung Galaxy S26 Ultra. Inspiriert von Science-Fiction und modernsten Androiden/Cyborgs vereint Erna ein charmantes, dezent futuristisches Auftreten mit nützlichen Alltagsfunktionen und echter, lokaler Datensicherheit.

Das Projekt demonstriert auf eindrucksvolle Weise, wie moderne On-Device-Schnittstellen (Spracherkennung & Text-to-Speech) nahtlos mit Cloud-basierten Large Language Models (LLMs) wie **Google Gemini** und einer lokalen persistenten Datenbank (Room/SQLite) interagieren können.

---

## 🌟 Kernfunktionen

### 1. Sprachsteuerung & Freisprech-Modus („Hey Erna“)
* **Sprachaktivierung:** Über einen robusten, softwareseitigen Aktivierungs-Algorithmus reagiert die App kontinuierlich auf den Weckruf *„Hey Erna“* (mit robuster phonetischer Filterung für unpräzise Aussprachen wie *Erna*, *Ärna*, *Werner* etc.).
* **Continuous Listening:** Ein intelligenter, akkuschonender Audio-Loop startet die Spracherkennung nach Timeouts oder Phrasen ohne Match automatisch neu, um ein echtes Freisprecherlebnis zu bieten.
* **Push-to-Talk:** Alternativ lässt sich die Assistentin per Knopfdruck über ein futuristisches, pulsierendes Mikrofon-Interface aktivieren.

### 2. Menschliche & Sympathische Stimme (TTS)
* **Natürliche Frauenstimme:** Erna spricht Deutsch über eine sorgfältig gefilterte, warme Frauenstimme. Die App analysiert die installierten Text-to-Speech-Engines dynamisch und wählt das hochwertigste weibliche Profil aus.
* **Sanfte Modulation:** Tonhöhe und Sprechgeschwindigkeit sind feinjustiert für ein weiches, beruhigendes und hochmodernes Klangerlebnis.

### 3. Intelligentes On-Device Gedächtnis (Room/SQLite)
* **Absolute Privatsphäre:** Persönliche Daten wie Name, Beruf, Interessen, Notizen oder Erinnerungen werden absolut sicher lokal in einer Room-Datenbank verschlüsselt gespeichert.
* **Kontextuelle Intelligenz:** Vor jeder Anfrage an die Gemini-API werden die relevanten lokalen Informationen aus der Datenbank ausgelesen und als System-Kontext injiziert. Dadurch erinnert sich Erna an alles, was du ihr erzählst, ganz ohne Cloud-Tracking.

### 4. High-End Benutzeroberfläche (Jetpack Compose & Material 3)
* **Sci-Fi Ästhetik:** Ein tiefdunkles, minimalistisches Interface mit flüssigen Übergängen, pulsierenden Welleneffekten beim Sprechen und glühenden Neon-Akzenten.
* **Avatar-Animation:** Ein leuchtender, atmender Androiden-Avatar signalisiert visuell den aktuellen Zustand (Zuhören, Nachdenken, Sprechen).

---

## 🛠️ Technische Highlights (Tech Stack)

* **Kotlin & Jetpack Compose:** Deklarative UI mit modernsten Komponenten nach Material Design 3 Richtlinien (vollständig Edge-to-Edge).
* **Android SpeechRecognizer (STT):** On-Device Spracherkennung zur Echtzeit-Transkription von Sprachbefehlen.
* **Android TextToSpeech (TTS):** Modulierte Sprachausgabe mit dynamischer Auswahl optimierter Frauenstimmen.
* **Room Database:** Typsichere SQLite-Abstraktion zur persistenten Verwaltung von lokalem Gedächtnis und Chatverläufen.
* **Google Gemini API:** Server-Side Integration für blitzschnelle, kreative und kontextbezogene Antworten des LLMs.
* **State Management:** Reaktive Datenströme mittels `StateFlow` und strukturierte Nebenläufigkeit mit `Kotlin Coroutines`.

---

## 🌐 Projektvorstellung für deine Webseite

Nutze diesen strukturierten Pitch, um dein Projekt und das erstellte Video auf deiner Webseite perfekt zu präsentieren:

### 🎬 Struktur-Vorschlag für die Landingpage

#### **1. Hero-Sektion (Überschrift & Video-Einbindung)**
> *„Triff Erna — Die Zukunft der persönlichen Assistenz auf deinem Smartphone.“*
> Platzierte hier dein **Video** im Breitbild-Format (16:9). Das Video fesselt die Besucher sofort visuell und zeigt Erna in Aktion (Weckruf, Sprachantwort, Sci-Fi-Animationen).

#### **2. Die Vision (Kurzbeschreibung)**
> Erna ist kein gewöhnlicher Sprachassistent. Sie ist das Konzept einer nahbaren, intelligenten Begleiterin mit der Persönlichkeit eines hochentwickelten weiblichen Androiden. Das Besondere: Sie kombiniert die generative Power von Künstlicher Intelligenz mit absoluter Datensicherheit. Alles, was Erna über dich weiß, bleibt sicher auf deinem Gerät gespeichert.

#### **3. Interaktive Feature-Cards (Visuell untermalt)**
* **🎤 Sprachsteuerung „Hey Erna“:** *(Zeige im Video die Szene, in der du den Weckruf nutzt)* — Berührungslose Steuerung und nahtloser Dialogfluss.
* **🧠 Lokales Gedächtnis:** *(Zeige, wie Erna auf eine persönliche Frage antwortet)* — Sicherer SQLite-Speicher für deine Notizen und Vorlieben.
* **🎭 Lebendige Interaktion:** *(Zeige die pulsierenden Wellen-Animationen)* — Visuelle Indikatoren erwecken die KI zum Leben.

#### **4. Technischer Hintergrund (Für Entwickler & Tech-Enthusiasten)**
> Eine kurze Auflistung des Tech-Stacks (Kotlin, Compose, Room, Gemini API), um deine Programmier- und Software-Engineering-Expertise hervorzuheben.

---

## 🚀 Lokale Installation & Build-Anleitung

### Voraussetzungen
* Android Studio (Ladybug oder neuer)
* Android SDK 34+
* Ein Google Gemini API Key (über Google AI Studio)

### Setup-Schritte
1. **Repository klonen:**
   ```bash
   git clone https://github.com/DEIN_BENUTZERNAME/erna-assistant.git
   cd erna-assistant
   ```
2. **API-Schlüssel einrichten:**
   Erstelle eine `.env` Datei im Stammverzeichnis des Projekts (oder trage sie in den Secrets deines Build-Projekts ein):
   ```env
   GEMINI_API_KEY=dein_api_schlüssel_hier
   ```
3. **Projekt bauen:**
   Öffne das Projekt in Android Studio. Gradle lädt alle Abhängigkeiten automatisch herunter. Klicke auf **Run**, um die App auf deinem physischen Android-Gerät oder Emulator zu installieren.

---

## 📄 Lizenz
Dieses Projekt ist unter der MIT-Lizenz lizenziert. Siehe die `LICENSE` Datei für Details.

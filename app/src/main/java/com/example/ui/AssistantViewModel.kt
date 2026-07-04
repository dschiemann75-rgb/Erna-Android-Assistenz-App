package com.example.ui

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.MemoryEntity
import com.example.data.database.MemoryRepository
import com.example.data.database.ProfileEntity
import com.example.network.GeminiClient
import com.example.network.GeminiContent
import com.example.network.GeminiPart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class AssistantViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val TAG = "AssistantViewModel"

    // --- Local Database and Repository ---
    private val database = AppDatabase.getDatabase(application)
    private val repository = MemoryRepository(database.memoryDao())

    // --- State Flows for Local Data ---
    val memories: StateFlow<List<MemoryEntity>> = repository.allMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<ProfileEntity?> = repository.profileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- State Flows for Voice and Chat Status ---
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                text = "Hallo! Ich bin Erna, deine persönliche KI-Assistentin. Wie kann ich dir heute helfen?",
                isUser = false
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _speechText = MutableStateFlow("")
    val speechText: StateFlow<String> = _speechText.asStateFlow()

    private val _voiceFeedbackMessage = MutableStateFlow("")
    val voiceFeedbackMessage: StateFlow<String> = _voiceFeedbackMessage.asStateFlow()

    private val _sttError = MutableStateFlow<String?>(null)
    val sttError: StateFlow<String?> = _sttError.asStateFlow()

    // Wake Word / continuous listener toggle
    val isAutoWakeActive = MutableStateFlow(false)

    // --- Custom Video Background State ---
    private val _backgroundVideoPath = MutableStateFlow<String?>(null)
    val backgroundVideoPath: StateFlow<String?> = _backgroundVideoPath.asStateFlow()

    // --- Custom API Key State ---
    private val _customApiKey = MutableStateFlow<String?>(null)
    val customApiKey: StateFlow<String?> = _customApiKey.asStateFlow()

    // --- Speech and Audio Engines ---
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null

    init {
        initializeTTS(application)
        initializeSTT(application)

        // Load custom Gemini API key if saved
        val sharedPrefs = application.getSharedPreferences("erna_prefs", android.content.Context.MODE_PRIVATE)
        _customApiKey.value = sharedPrefs.getString("gemini_api_key", null)

        // Check for local custom video background
        val customVideoFile = File(application.filesDir, "background_video.mp4")
        if (customVideoFile.exists()) {
            _backgroundVideoPath.value = customVideoFile.absolutePath
        }

        // Pre-populate empty profile if it doesn't exist
        viewModelScope.launch {
            val currentProfile = repository.getProfileDirect()
            if (currentProfile == null) {
                repository.saveProfile(
                    ProfileEntity(
                        name = "Nutzer",
                        occupation = "Unbekannt",
                        address = "Unbekannt",
                        additionalInfo = "Keine zusätzlichen Notizen hinterlegt."
                    )
                )
            }
        }
    }

    // --- Local Database Actions ---
    fun addMemory(content: String, category: String = "general") {
        viewModelScope.launch {
            if (content.isNotBlank()) {
                repository.insertMemory(content, category)
                _voiceFeedbackMessage.value = "Notiz erfolgreich in Ernas Gedächtnis gespeichert!"
                speak("Ich habe mir das notiert.")
            }
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            repository.deleteMemory(id)
            _voiceFeedbackMessage.value = "Erfahrung gelöscht."
        }
    }

    fun updateProfile(name: String, occupation: String, address: String, additionalInfo: String) {
        viewModelScope.launch {
            repository.saveProfile(
                ProfileEntity(
                    id = 1,
                    name = name,
                    occupation = occupation,
                    address = address,
                    additionalInfo = additionalInfo
                )
            )
            _voiceFeedbackMessage.value = "Profil erfolgreich gespeichert!"
            speak("Dein Profil wurde aktualisiert. Ich habe es gespeichert.")
        }
    }

    // --- Voice Output (Text to Speech) ---
    private fun initializeTTS(application: Application) {
        textToSpeech = TextToSpeech(application, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.GERMANY)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "German Language is not supported on this device TTS engine")
            } else {
                Log.d(TAG, "TTS Initialized successfully in German")
                
                // Select a natural, soft female voice if available
                selectFemaleVoice()
                
                // Adjust speech pitch and rate for a softer, calmer, more natural sound
                textToSpeech?.setPitch(1.04f)      // Warm, soft, clear tone
                textToSpeech?.setSpeechRate(0.93f) // Slightly slower for a very natural, empathetic flow
            }
        } else {
            Log.e(TAG, "Failed to initialize TTS")
        }
    }

    private fun selectFemaleVoice() {
        try {
            val voices = textToSpeech?.voices
            if (!voices.isNullOrEmpty()) {
                val germanVoices = voices.filter { voice ->
                    val locale = voice.locale
                    locale != null && (locale.language == "de" || locale.language.startsWith("de"))
                }
                
                if (germanVoices.isNotEmpty()) {
                    // Score each voice to find the best female voice
                    val bestVoice = germanVoices.maxByOrNull { voice ->
                        val nameLower = voice.name?.lowercase() ?: ""
                        var score = 0
                        
                        // Female indicators
                        if (nameLower.contains("female") || nameLower.contains("fem")) {
                            score += 100
                        }
                        if (nameLower.contains("sfg") || nameLower.contains("gfg") || nameLower.contains("kfg") || nameLower.contains("hfg")) {
                            score += 100
                        }
                        if (nameLower.contains("standard-a") || nameLower.contains("standard-c") || nameLower.contains("standard-e") || nameLower.contains("standard-f")) {
                            score += 100
                        }
                        if (nameLower.contains("wavenet-a") || nameLower.contains("wavenet-c") || nameLower.contains("wavenet-e") || nameLower.contains("wavenet-f")) {
                            score += 100
                        }
                        // Check for Samsung/other patterns like de_DE_f01, de-DE-f02, etc.
                        if (nameLower.contains("_f0") || nameLower.contains("-f0") || nameLower.contains("_f1") || nameLower.contains("-f1") || nameLower.contains("_f2") || nameLower.contains("-f2")) {
                            score += 80
                        }
                        
                        // Male indicators to avoid
                        if (nameLower.contains("male") || nameLower.contains("masc")) {
                            score -= 200
                        }
                        if (nameLower.contains("dfg") || nameLower.contains("nfc") || nameLower.contains("deg") || nameLower.contains("ctg") || nameLower.contains("rfc")) {
                            score -= 200
                        }
                        if (nameLower.contains("standard-b") || nameLower.contains("standard-d") || nameLower.contains("wavenet-b") || nameLower.contains("wavenet-d")) {
                            score -= 200
                        }
                        if (nameLower.contains("_m0") || nameLower.contains("-m0") || nameLower.contains("_m1") || nameLower.contains("-m1") || nameLower.contains("_m2") || nameLower.contains("-m2")) {
                            score -= 150
                        }
                        
                        score
                    }
                    
                    if (bestVoice != null) {
                        textToSpeech?.voice = bestVoice
                        Log.d(TAG, "Selected German voice: ${bestVoice.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error selecting female voice", e)
        }
    }

    fun speak(text: String) {
        if (textToSpeech == null) return
        
        // Ensure female voice is selected before speaking (in case voices loaded late)
        selectFemaleVoice()
        
        stopSpeaking()
        _isSpeaking.value = true

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "erna_speech")
        }

        textToSpeech?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
                // Re-start wake-word listening if AutoWake is active
                if (isAutoWakeActive.value) {
                    viewModelScope.launch {
                        startListening()
                    }
                }
            }
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
            }
        })

        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "erna_speech")
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        _isSpeaking.value = false
    }

    // --- Voice Input (Speech to Text) ---
    private fun initializeSTT(application: Application) {
        if (SpeechRecognizer.isRecognitionAvailable(application)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(application)
            speechRecognizer?.setRecognitionListener(speechListener)
        } else {
            _sttError.value = "Spracherkennung ist auf diesem Gerät leider nicht verfügbar."
            Log.e(TAG, "SpeechRecognizer is not available on this device")
        }
    }

    fun startListening() {
        stopSpeaking()
        _sttError.value = null
        _speechText.value = ""

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "de-DE")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        try {
            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            _sttError.value = "Fehler beim Starten der Spracherkennung: ${e.localizedMessage}"
            _isListening.value = false
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _isListening.value = false
    }

    private val speechListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _isListening.value = true
            _sttError.value = null
        }

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _isListening.value = false
        }

        override fun onError(error: Int) {
            _isListening.value = false
            val message = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio-Aufnahmefehler."
                SpeechRecognizer.ERROR_CLIENT -> "Schnittstellenfehler."
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mikrofon-Berechtigung fehlt!"
                SpeechRecognizer.ERROR_NETWORK -> "Netzwerkfehler bei Spracherkennung."
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Netzwerk-Timeout."
                SpeechRecognizer.ERROR_NO_MATCH -> "Keine Sprache erkannt."
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Spracherkennungsdienst ist beschäftigt."
                SpeechRecognizer.ERROR_SERVER -> "Serverfehler."
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Keine Spracheingabe erkannt."
                else -> "Unbekannter Fehler bei der Spracherkennung."
            }

            Log.e(TAG, "STT Error code: $error - $message")
            
            // Show errors on screen if not in auto-wake, or if it's a permission error
            if (!isAutoWakeActive.value || error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                _sttError.value = message
            }

            // If auto-wake is active, always try to restart listening (unless permissions are missing)
            if (isAutoWakeActive.value && error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                viewModelScope.launch {
                    kotlinx.coroutines.delay(800) // Small delay to let speech recognition engine reset
                    if (isAutoWakeActive.value) {
                        startListening()
                    }
                }
            }
        }

        override fun onResults(results: Bundle?) {
            _isListening.value = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull() ?: ""
            _speechText.value = text

            if (text.isNotBlank()) {
                processSpokenText(text)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            _speechText.value = matches?.firstOrNull() ?: ""
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun processSpokenText(text: String) {
        // Appends to chat list as user's input
        appendMessage(ChatMessage(text = text, isUser = true))

        // Trigger AI Assistant "Erna"
        if (isAutoWakeActive.value) {
            // Check if the input contains wake word like "Erna" or "Hey Erna" with robust phonetic matches
            val textLower = text.lowercase()
            val hasWakeWord = textLower.contains("erna") || 
                              textLower.contains("ärna") || 
                              textLower.contains("erner") || 
                              textLower.contains("irna") || 
                              textLower.contains("herna") || 
                              textLower.contains("werner") || 
                              textLower.contains("gerne") || 
                              textLower.contains("hey rna") || 
                              textLower.contains("heyerna") || 
                              textLower.contains("hallo erna")

            if (hasWakeWord) {
                // User addressed Erna
                askErna(text)
            } else {
                // In auto mode, if they didn't say Erna, we can still respond or resume silent waiting
                _voiceFeedbackMessage.value = "Spreche 'Erna' an, um eine Antwort zu erhalten."
                startListening()
            }
        } else {
            // Manual push-to-talk answers everything
            askErna(text)
        }
    }

    // --- Ask Erna & Inject Local Memories (RAG) ---
    fun askErna(question: String) {
        if (question.isBlank()) return
        stopSpeaking()
        _isProcessing.value = true

        viewModelScope.launch {
            // 1. Gather User Profile info
            val profile = repository.getProfileDirect()
            val profileContext = if (profile != null) {
                """
                Nutzername: ${profile.name}
                Beruf/Arbeit: ${profile.occupation}
                Wohnort/Adresse: ${profile.address}
                Zusätzliche Infos: ${profile.additionalInfo}
                """.trimIndent()
            } else {
                "Kein Nutzerprofil hinterlegt."
            }

            // 2. Keyword-based search in local Memories/Notes
            val keywords = question.split(" ", ",", ".", "?").filter { it.length > 3 }
            val matchingMemories = mutableSetOf<MemoryEntity>()
            keywords.forEach { word ->
                val matches = repository.searchMemories(word)
                matchingMemories.addAll(matches)
            }

            // If no specific match, load the latest 5 memories to give she context
            val relevantMemories = if (matchingMemories.isEmpty()) {
                memories.value.take(10)
            } else {
                matchingMemories.toList()
            }

            val memoriesContext = if (relevantMemories.isNotEmpty()) {
                relevantMemories.joinToString("\n") { "- [Am ${formatTimestamp(it.timestamp)}]: ${it.content}" }
            } else {
                "Keine aufgezeichneten Notizen oder Erlebnisse gefunden."
            }

            // 3. Assemble System Prompt with Context injection
            val systemPrompt = """
                Du bist Erna, eine hochmoderne, freundliche und intelligente persönliche Assistentin für das Samsung Galaxy S26 Ultra.
                Die App hat dich als futuristischen, hochentwickelten weiblichen Androiden/Cyborg im Hintergrund visualisiert. Antworte charmant, ein wenig futuristisch, aber vor allem extrem hilfsbereit und herzlich auf Deutsch.
                
                Hier sind die PERSÖNLICHEN DATEN des Nutzers, die absolut sicher lokal auf dem Gerät in deiner Datenbank gespeichert sind. Nutze sie, um Fragen personalisiert zu beantworten (z. B. wenn er nach seinem Beruf, Namen, Erlebnissen oder Notizen fragt):
                
                [NUTZERPROFIL]
                $profileContext
                
                [NOTIZEN & ERLEBNISSE DES NUTZERS (DEIN GEDÄCHTNIS)]
                $memoriesContext
                
                WICHTIGE ANWEISUNG:
                - Wenn der Nutzer dich bittet, sich etwas zu merken (z.B. "Merk dir, dass ich heute einen super Kaffee getrunken habe" oder "Erna, schreib auf: Meine Brille liegt auf dem Tisch"), dann fange deine Antwort mit der Kennung "[SPEICHERN: <Notiz-Inhalt>]" an. Die App wird diesen Inhalt dann automatisch in deine lokale Datenbank eintragen!
                - Halte deine Antworten prägnant und ideal für die Sprachausgabe geeignet (Vermeide lange Listen, Bulletpoints oder komplexe Markdown-Formate, da sie vorgelesen werden).
                - Sei warmherzig und sprich den Nutzer mit seinem Namen an, wenn du ihn kennst.
            """.trimIndent()

            // 4. Construct conversation history for Gemini API
            val historyList = _chatMessages.value
                .filter { it.text != "Hallo! Ich bin Erna, deine persönliche KI-Assistentin. Wie kann ich dir heute helfen?" }
                .takeLast(6) // Send last 6 messages to keep context without hitting token limits
                .map { msg ->
                    GeminiContent(
                        parts = listOf(GeminiPart(text = msg.text)),
                        role = if (msg.isUser) "user" else "model"
                    )
                }

            // 5. Call Gemini API
            val rawReply = GeminiClient.generateAnswer(
                prompt = question,
                systemPrompt = systemPrompt,
                history = historyList,
                customApiKey = _customApiKey.value
            )

            // 6. Check if Erna wants to save a memory automatically
            var cleanedReply = rawReply
            if (rawReply.contains("[SPEICHERN:")) {
                val regex = "\\[SPEICHERN:\\s*(.*?)\\]".toRegex()
                val match = regex.find(rawReply)
                val memoToSave = match?.groupValues?.get(1)
                if (!memoToSave.isNullOrBlank()) {
                    repository.insertMemory(memoToSave, "note")
                }
                cleanedReply = rawReply.replace(regex, "").trim()
            }

            // Add AI's reply to the chat interface
            appendMessage(ChatMessage(text = cleanedReply, isUser = false))
            _isProcessing.value = false

            // Speak response
            speak(cleanedReply)
        }
    }

    fun clearChat() {
        _chatMessages.value = listOf(
            ChatMessage(
                text = "Chatverlauf gelöscht. Ich bin Erna, deine Assistentin. Wie kann ich dir helfen?",
                isUser = false
            )
        )
        stopSpeaking()
    }

    private fun appendMessage(message: ChatMessage) {
        val currentList = _chatMessages.value.toMutableList()
        currentList.add(message)
        _chatMessages.value = currentList
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY)
        return sdf.format(java.util.Date(timestamp))
    }

    fun saveBackgroundVideo(uri: android.net.Uri, context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val targetFile = File(context.filesDir, "background_video.mp4")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                _backgroundVideoPath.value = targetFile.absolutePath
            } catch (e: Exception) {
                Log.e(TAG, "Error saving custom video background", e)
            }
        }
    }

    fun resetBackgroundVideo(context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val targetFile = File(context.filesDir, "background_video.mp4")
                if (targetFile.exists()) {
                    targetFile.delete()
                }
                _backgroundVideoPath.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Error resetting background video", e)
            }
        }
    }

    fun saveApiKey(newKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val sharedPrefs = getApplication<Application>().getSharedPreferences("erna_prefs", android.content.Context.MODE_PRIVATE)
            sharedPrefs.edit().putString("gemini_api_key", newKey.trim()).apply()
            _customApiKey.value = newKey.trim()
        }
    }

    fun resetApiKey() {
        viewModelScope.launch(Dispatchers.IO) {
            val sharedPrefs = getApplication<Application>().getSharedPreferences("erna_prefs", android.content.Context.MODE_PRIVATE)
            sharedPrefs.edit().remove("gemini_api_key").apply()
            _customApiKey.value = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
        textToSpeech?.shutdown()
    }
}

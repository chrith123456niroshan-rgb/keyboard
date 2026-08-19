package com.sintrans.keyboard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.inputmethodservice.InputMethodService
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SinTransKeyboardService : InputMethodService(), CoroutineScope by MainScope() {

    private var isTranslateEnabled = false
    private var isShifted = false
    private var isCapsLock = false
    private var lastShiftClickTime = 0L
    private var isDirectEnglishMode = false
    private val translationRepository: TranslationRepository = GoogleTranslationRepository()
    
    // Transliteration buffers
    private val englishInputBuffer = StringBuilder()
    private var previousSinhalaWordLength = 0

    // Sentence-level bulk translation buffers
    private var targetLang = "en" // Default to English ("en"), can toggle to Tamil ("ta")
    private val currentSentenceBuffer = StringBuilder()
    private var committedTargetLength = 0

    // Multi-layout mode management
    private enum class KeyboardMode { QWERTY, SYMBOLS, EMOJI }
    private var currentMode = KeyboardMode.QWERTY

    private var activeKeyboardView: View? = null
    private var notificationReceiver: BroadcastReceiver? = null
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("sintrans_prefs", Context.MODE_PRIVATE)
        targetLang = prefs.getString("target_lang", "en") ?: "en"

        // Register broadcast receiver for translated notifications
        notificationReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "com.sintrans.keyboard.NOTIFICATION_TRANSLATED") {
                    val sender = intent.getStringExtra("sender") ?: "Unknown"
                    val translation = intent.getStringExtra("translation") ?: ""
                    if (translation.isNotEmpty()) {
                        showNotificationBanner(sender, translation)
                    }
                }
            }
        }
        val filter = IntentFilter("com.sintrans.keyboard.NOTIFICATION_TRANSLATED")
        registerReceiver(notificationReceiver, filter)
    }

    override fun onCreateInputView(): View {
        // Inflate layout dynamically based on active mode
        val layoutId = when (currentMode) {
            KeyboardMode.QWERTY -> R.layout.keyboard_qwerty
            KeyboardMode.SYMBOLS -> R.layout.keyboard_symbols
            KeyboardMode.EMOJI -> R.layout.keyboard_emojis
        }

        val keyboardView = LayoutInflater.from(this).inflate(layoutId, null)
        activeKeyboardView = keyboardView
        setupKeyboardBindings(keyboardView)
        return keyboardView
    }

    private fun showNotificationBanner(sender: String, translation: String) {
        val keyboardView = activeKeyboardView ?: return
        val tvTitle = keyboardView.findViewById<TextView>(R.id.tv_title) ?: return
        
        val originalText = tvTitle.text.toString()
        val bannerText = "$sender: $translation"
        
        tvTitle.text = bannerText
        tvTitle.setSelected(true)
        
        tvTitle.postDelayed({
            if (tvTitle.text == bannerText) {
                tvTitle.text = originalText
            }
        }, 5000)
    }

    private fun setupKeyboardBindings(keyboardView: View) {
        // Bind Translate toggle if present in layout
        val btnTranslate = keyboardView.findViewById<Button>(R.id.btn_translate_toggle)
        btnTranslate?.let { btn ->
            updateTranslateButtonState(btn)
            btn.setOnClickListener {
                isTranslateEnabled = !isTranslateEnabled
                updateTranslateButtonState(btn)
                
                // Clear sentence states on translation state change to prevent sync issues
                currentSentenceBuffer.setLength(0)
                committedTargetLength = 0

                val status = if (isTranslateEnabled) "ON" else "OFF"
                Toast.makeText(this, "Real-time Translation $status", Toast.LENGTH_SHORT).show()
            }
        }

        // Bind Language switcher dynamic toggle if present in layout
        val btnLang = keyboardView.findViewById<Button>(R.id.btn_lang_toggle)
        btnLang?.let { btn ->
            updateLangButtonState(btn)
            btn.setOnClickListener {
                btn.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                targetLang = if (targetLang == "en") "ta" else "en"
                updateLangButtonState(btn)
                
                // Save targetLang to SharedPreferences
                val prefs = getSharedPreferences("sintrans_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("target_lang", targetLang).apply()
                
                // Clear buffers to restart typing sentence in the newly toggled target language
                currentSentenceBuffer.setLength(0)
                committedTargetLength = 0

                val displayLang = if (targetLang == "en") "English" else "Tamil"
                Toast.makeText(this, "Target Language: $displayLang", Toast.LENGTH_SHORT).show()
            }
        }


        // Bind shift key if present
        val btnShift = keyboardView.findViewById<Button>(R.id.btn_shift)
        btnShift?.let { btn ->
            btn.setOnClickListener {
                btn.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastShiftClickTime < 400) {
                    // Double tap: toggle Caps Lock
                    isCapsLock = !isCapsLock
                    isShifted = isCapsLock
                } else {
                    // Single tap: toggle Shift (and turn off Caps Lock if it was active)
                    if (isCapsLock) {
                        isCapsLock = false
                        isShifted = false
                    } else {
                        isShifted = !isShifted
                    }
                }
                lastShiftClickTime = currentTime
                updateShiftState(keyboardView)
            }
        }

        // Bind layout mode switcher keys
        keyboardView.findViewById<Button>(R.id.btn_mode_qwerty)?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            switchMode(KeyboardMode.QWERTY)
        }
        keyboardView.findViewById<Button>(R.id.btn_mode_symbols)?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            switchMode(KeyboardMode.SYMBOLS)
        }
        keyboardView.findViewById<Button>(R.id.btn_mode_emoji)?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            switchMode(KeyboardMode.EMOJI)
        }

        // System functional buttons
        keyboardView.findViewById<Button>(R.id.btn_space)?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            handleSpaceClick()
        }

        val btnBackspace = keyboardView.findViewById<Button>(R.id.btn_backspace)
        btnBackspace?.setOnTouchListener(object : View.OnTouchListener {
            private val INITIAL_DELAY = 400L
            private val REPEAT_INTERVAL = 50L

            private val actionRunnable = object : Runnable {
                override fun run() {
                    btnBackspace.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    handleBackspaceClick()
                    mainHandler.postDelayed(this, REPEAT_INTERVAL)
                }
            }

            override fun onTouch(v: View?, event: android.view.MotionEvent?): Boolean {
                when (event?.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        btnBackspace.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        handleBackspaceClick()
                        mainHandler.removeCallbacks(actionRunnable)
                        mainHandler.postDelayed(actionRunnable, INITIAL_DELAY)
                        return true
                    }
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                        mainHandler.removeCallbacks(actionRunnable)
                        return true
                    }
                }
                return false
            }
        })

        val btnGlobe = keyboardView.findViewById<Button>(R.id.btn_globe)
        btnGlobe?.let { btn ->
            btn.setOnClickListener {
                btn.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                isDirectEnglishMode = !isDirectEnglishMode
                updateGlobeButtonState(keyboardView)
                val status = if (isDirectEnglishMode) "ON" else "OFF"
                Toast.makeText(this, "Direct English Mode: $status", Toast.LENGTH_SHORT).show()
            }
        }

        keyboardView.findViewById<Button>(R.id.btn_enter)?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            handleEnterClick()
        }

        // Bind standard typing characters (recursively)
        bindStandardKeys(keyboardView)

        // Initialize character case based on current Shift state and Globe state
        updateShiftState(keyboardView)
        updateGlobeButtonState(keyboardView)
    }

    private fun bindStandardKeys(view: View) {
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                bindStandardKeys(view.getChildAt(i))
            }
        } else if (view is Button) {
            val id = view.id
            // Skip non-character keys
            if (id == R.id.btn_translate_toggle || 
                id == R.id.btn_lang_toggle ||
                id == R.id.btn_space || 
                id == R.id.btn_backspace || 
                id == R.id.btn_globe || 
                id == R.id.btn_enter ||
                id == R.id.btn_shift ||
                id == R.id.btn_mode_symbols ||
                id == R.id.btn_mode_qwerty ||
                id == R.id.btn_mode_emoji) {
                return
            }

            // Standard typing character button listener
            view.setOnClickListener {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                showKeyPreview(view)

                val charText = view.text.toString()
                handleCharacterInput(charText)
            }
        }
    }

    private fun handleCharacterInput(charText: String) {
        val inputConnection = currentInputConnection ?: return
        if (currentMode == KeyboardMode.QWERTY && !isDirectEnglishMode) {
            // Singlish Phonetic input flow
            englishInputBuffer.append(charText)
            val transliterated = SinglishEngine.transliterate(englishInputBuffer.toString())
            
            // Delete previous Sinhalese word from screen
            inputConnection.deleteSurroundingText(previousSinhalaWordLength, 0)
            
            // Commit new Sinhalese transliteration
            inputConnection.commitText(transliterated, 1)
            previousSinhalaWordLength = transliterated.length

            if (isShifted && !isCapsLock) {
                isShifted = false
                activeKeyboardView?.let { updateShiftState(it) }
            }
        } else {
            // Standard typing (symbols / emojis / Direct English), bypass buffers
            inputConnection.commitText(charText, 1)
            englishInputBuffer.setLength(0)
            previousSinhalaWordLength = 0

            if (isShifted && !isCapsLock) {
                isShifted = false
                activeKeyboardView?.let { updateShiftState(it) }
            }
        }
    }

    private fun handleSpaceClick() {
        val inputConnection = currentInputConnection ?: return
        val word = if (englishInputBuffer.isNotEmpty() && !isDirectEnglishMode) SinglishEngine.transliterate(englishInputBuffer.toString()) else ""
        
        // Reset word-level buffers
        englishInputBuffer.setLength(0)
        val prevSinhalaLen = previousSinhalaWordLength
        previousSinhalaWordLength = 0

        if (isTranslateEnabled && currentMode == KeyboardMode.QWERTY && !isDirectEnglishMode) {
            if (word.isNotEmpty()) {
                currentSentenceBuffer.append(word).append(" ")
                val fullSentence = currentSentenceBuffer.toString().trim()
                
                // Delete current Sinhala word + previously committed sentence translation
                val totalDeleteCount = prevSinhalaLen + committedTargetLength
                inputConnection.deleteSurroundingText(totalDeleteCount, 0)
                
                val currentTargetLang = targetLang
                performTranslation(fullSentence, currentTargetLang) { translatedText ->
                    val finalCommit = if (translatedText.isNotEmpty()) translatedText else fullSentence
                    inputConnection.commitText("$finalCommit ", 1)
                    committedTargetLength = finalCommit.length + 1
                }
            } else {
                // If pressing space on empty buffer, just send space and clear sentence tracking
                inputConnection.commitText(" ", 1)
                currentSentenceBuffer.setLength(0)
                committedTargetLength = 0
            }
        } else {
            // Commit normal space and clear buffers
            inputConnection.commitText(" ", 1)
            currentSentenceBuffer.setLength(0)
            committedTargetLength = 0
        }
    }

    private fun handleBackspaceClick() {
        val inputConnection = currentInputConnection ?: return
        
        if (currentMode == KeyboardMode.QWERTY && englishInputBuffer.isNotEmpty()) {
            // Backspace on phonetic buffer
            englishInputBuffer.deleteCharAt(englishInputBuffer.length - 1)
            
            // Erase from screen
            inputConnection.deleteSurroundingText(previousSinhalaWordLength, 0)
            
            if (englishInputBuffer.isNotEmpty()) {
                // Re-transliterate remaining buffer
                val transliterated = SinglishEngine.transliterate(englishInputBuffer.toString())
                inputConnection.commitText(transliterated, 1)
                previousSinhalaWordLength = transliterated.length
            } else {
                previousSinhalaWordLength = 0
            }
        } else {
            // Backspacing on already committed text. Clear sentence-level tracking state.
            currentSentenceBuffer.setLength(0)
            committedTargetLength = 0
            
            val selectedText = inputConnection.getSelectedText(0)
            if (!selectedText.isNullOrEmpty()) {
                inputConnection.commitText("", 1)
            } else {
                inputConnection.deleteSurroundingText(1, 0)
            }
            englishInputBuffer.setLength(0)
            previousSinhalaWordLength = 0
        }
    }

    private fun handleEnterClick() {
        val inputConnection = currentInputConnection ?: return
        val word = if (englishInputBuffer.isNotEmpty() && !isDirectEnglishMode) SinglishEngine.transliterate(englishInputBuffer.toString()) else ""
        
        // Reset word-level buffers
        englishInputBuffer.setLength(0)
        val prevSinhalaLen = previousSinhalaWordLength
        previousSinhalaWordLength = 0

        if (isTranslateEnabled && currentMode == KeyboardMode.QWERTY && !isDirectEnglishMode) {
            if (word.isNotEmpty() || currentSentenceBuffer.isNotEmpty()) {
                if (word.isNotEmpty()) {
                    currentSentenceBuffer.append(word)
                }
                val fullSentence = currentSentenceBuffer.toString().trim()
                
                // Delete everything currently typed in this sentence session
                val totalDeleteCount = prevSinhalaLen + committedTargetLength
                inputConnection.deleteSurroundingText(totalDeleteCount, 0)
                
                // Clear sentence buffers before executing async task to prevent state leaks
                currentSentenceBuffer.setLength(0)
                committedTargetLength = 0

                val currentTargetLang = targetLang
                performTranslation(fullSentence, currentTargetLang) { translatedText ->
                    val finalCommit = if (translatedText.isNotEmpty()) translatedText else fullSentence
                    inputConnection.commitText(finalCommit, 1)
                    sendEnterKeyEvent(inputConnection)
                }
            } else {
                currentSentenceBuffer.setLength(0)
                committedTargetLength = 0
                sendEnterKeyEvent(inputConnection)
            }
        } else {
            currentSentenceBuffer.setLength(0)
            committedTargetLength = 0
            sendEnterKeyEvent(inputConnection)
        }
    }

    private fun sendEnterKeyEvent(inputConnection: InputConnection) {
        val editorInfo = currentInputEditorInfo
        if (editorInfo != null) {
            val action = editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION
            if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
                inputConnection.performEditorAction(action)
                return
            }
        }
        inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
    }

    private fun switchMode(newMode: KeyboardMode) {
        currentMode = newMode
        setInputView(onCreateInputView())
    }

    private fun updateShiftState(rootView: View) {
        updateButtonsCase(rootView)
        val btnShift = rootView.findViewById<Button>(R.id.btn_shift)
        btnShift?.let {
            when {
                isCapsLock -> {
                    it.text = "⇪"
                    it.setBackgroundResource(R.drawable.key_background_accent_on)
                }
                isShifted -> {
                    it.text = "⇧"
                    it.setBackgroundResource(R.drawable.key_background_accent_on)
                }
                else -> {
                    it.text = "⇧"
                    it.setBackgroundResource(R.drawable.key_background_special)
                }
            }
        }
    }

    private fun updateGlobeButtonState(rootView: View) {
        val btnGlobe = rootView.findViewById<Button>(R.id.btn_globe)
        btnGlobe?.let {
            if (isDirectEnglishMode) {
                it.setBackgroundResource(R.drawable.key_background_accent_on)
            } else {
                it.setBackgroundResource(R.drawable.key_background_special)
            }
        }
    }

    private fun updateButtonsCase(view: View) {
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                updateButtonsCase(view.getChildAt(i))
            }
        } else if (view is Button) {
            val id = view.id
            // Skip control, switcher, and language toggle buttons
            if (id != R.id.btn_translate_toggle && 
                id != R.id.btn_lang_toggle &&
                id != R.id.btn_space && 
                id != R.id.btn_backspace && 
                id != R.id.btn_globe && 
                id != R.id.btn_enter && 
                id != R.id.btn_shift && 
                id != R.id.btn_mode_symbols && 
                id != R.id.btn_mode_qwerty && 
                id != R.id.btn_mode_emoji) {
                
                val text = view.text.toString()
                if (text.length == 1) {
                    view.text = if (isShifted) text.uppercase() else text.lowercase()
                }
            }
        }
    }

    private fun showKeyPreview(anchorView: Button) {
        try {
            val popupView = LayoutInflater.from(this).inflate(R.layout.key_preview_popup, null)
            val tvPreview = popupView.findViewById<TextView>(R.id.tv_preview_text)
            tvPreview.text = anchorView.text

            // Measure popup window
            popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            val popupWidth = popupView.measuredWidth
            val popupHeight = popupView.measuredHeight

            val popupWindow = PopupWindow(
                popupView,
                popupWidth,
                popupHeight,
                false
            )

            val location = IntArray(2)
            anchorView.getLocationOnScreen(location)
            
            // Center the preview bubble above the pressed key
            val x = location[0] + (anchorView.width - popupWidth) / 2
            val y = location[1] - popupHeight - 12

            popupWindow.showAtLocation(anchorView, android.view.Gravity.NO_GRAVITY, x, y)

            // Auto-dismiss popup after a brief keypress visualization frame
            anchorView.postDelayed({
                try {
                    if (popupWindow.isShowing) {
                        popupWindow.dismiss()
                    }
                } catch (e: Exception) {
                    // Safe dismiss
                }
            }, 150)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun switchToNextKeyboard() {
        val token = window?.window?.attributes?.token ?: return
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager ?: return
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                switchToNextInputMethod(false)
            } else {
                @Suppress("DEPRECATION")
                imm.switchToNextInputMethod(token, false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun updateTranslateButtonState(btn: Button) {
        if (isTranslateEnabled) {
            btn.text = "Translate: ON"
            btn.setBackgroundResource(R.drawable.key_background_accent_on)
        } else {
            btn.text = "Translate: OFF"
            btn.setBackgroundResource(R.drawable.key_background_accent_off)
        }
    }

    private fun updateLangButtonState(btn: Button) {
        if (targetLang == "en") {
            btn.text = "Lang: EN"
        } else {
            btn.text = "Lang: TA"
        }
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        englishInputBuffer.setLength(0)
        previousSinhalaWordLength = 0
        currentSentenceBuffer.setLength(0)
        committedTargetLength = 0
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        englishInputBuffer.setLength(0)
        previousSinhalaWordLength = 0
        currentSentenceBuffer.setLength(0)
        committedTargetLength = 0
    }

    fun performTranslation(sinhaleseText: String, targetLanguage: String, onResult: (String) -> Unit) {
        launch {
            try {
                val translation = translationRepository.translate(sinhaleseText, targetLanguage)
                onResult(translation)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult("")
            }
        }
    }

    override fun onDestroy() {
        notificationReceiver?.let {
            unregisterReceiver(it)
        }
        cancel()
        super.onDestroy()
    }
}

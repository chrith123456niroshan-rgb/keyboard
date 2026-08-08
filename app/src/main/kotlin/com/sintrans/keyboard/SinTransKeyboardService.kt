package com.sintrans.keyboard

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SinTransKeyboardService : InputMethodService(), CoroutineScope by MainScope() {

    private var isTranslateEnabled: Boolean = false
    private val translationRepository: TranslationRepository = MyMemoryTranslationRepository()
    private val currentWordBuffer = StringBuilder()

    override fun onCreateInputView(): View {
        // Inflate the keyboard view
        val layoutInflater = LayoutInflater.from(this)
        val keyboardView = layoutInflater.inflate(R.layout.keyboard_view, null)

        // Setup individual functional key bindings
        val btnTranslate = keyboardView.findViewById<Button>(R.id.btn_translate_toggle)
        btnTranslate.setOnClickListener {
            isTranslateEnabled = !isTranslateEnabled
            updateTranslateButtonState(btnTranslate)
            
            val status = if (isTranslateEnabled) "ON" else "OFF"
            Toast.makeText(this, "Real-time Translation $status", Toast.LENGTH_SHORT).show()
        }

        val btnSpace = keyboardView.findViewById<Button>(R.id.btn_space)
        btnSpace.setOnClickListener {
            handleSpaceClick()
        }

        val btnBackspace = keyboardView.findViewById<Button>(R.id.btn_backspace)
        btnBackspace.setOnClickListener {
            handleBackspaceClick()
        }

        val btnGlobe = keyboardView.findViewById<Button>(R.id.btn_globe)
        btnGlobe.setOnClickListener {
            switchToNextKeyboard()
        }

        val btnEnter = keyboardView.findViewById<Button>(R.id.btn_enter)
        btnEnter.setOnClickListener {
            handleEnterClick()
        }

        // Dynamically bind standard keys (characters)
        bindStandardKeys(keyboardView)

        // Set initial state of translation button
        updateTranslateButtonState(btnTranslate)

        return keyboardView
    }

    private fun bindStandardKeys(view: View) {
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                bindStandardKeys(view.getChildAt(i))
            }
        } else if (view is Button) {
            val id = view.id
            // Skip functional keys
            if (id == R.id.btn_translate_toggle || 
                id == R.id.btn_space || 
                id == R.id.btn_backspace || 
                id == R.id.btn_globe ||
                id == R.id.btn_enter) {
                return
            }
            // All other buttons are alphabet character keys
            view.setOnClickListener {
                val charText = view.text.toString()
                currentWordBuffer.append(charText)
                commitText(charText)
            }
        }
    }

    private fun commitText(text: CharSequence) {
        val inputConnection = currentInputConnection ?: return
        inputConnection.commitText(text, 1)
    }

    private fun handleSpaceClick() {
        val inputConnection = currentInputConnection ?: return
        val word = currentWordBuffer.toString()

        if (isTranslateEnabled && word.isNotEmpty()) {
            // 1. Delete Sinhalese text from the active field (length of current word buffer)
            inputConnection.deleteSurroundingText(currentWordBuffer.length, 0)
            
            // Clear buffer
            currentWordBuffer.setLength(0)

            // 2. Perform translation asynchronously
            performTranslation(word) { translatedText ->
                if (translatedText.isNotEmpty()) {
                    // 3. Inject English translation followed by space
                    inputConnection.commitText("$translatedText ", 1)
                } else {
                    // Fallback to original word + space
                    inputConnection.commitText("$word ", 1)
                }
            }
        } else {
            // Regular space and clear buffer
            commitText(" ")
            currentWordBuffer.setLength(0)
        }
    }

    private fun handleBackspaceClick() {
        val inputConnection = currentInputConnection ?: return
        
        // Update local buffer if it's not empty
        if (currentWordBuffer.isNotEmpty()) {
            // Delete last character from our tracking buffer
            currentWordBuffer.deleteCharAt(currentWordBuffer.length - 1)
        }

        val selectedText = inputConnection.getSelectedText(0)
        if (!selectedText.isNullOrEmpty()) {
            inputConnection.commitText("", 1)
            // If the user selected text, the local buffer is invalidated since it's hard to track
            currentWordBuffer.setLength(0)
        } else {
            inputConnection.deleteSurroundingText(1, 0)
        }
    }

    private fun handleEnterClick() {
        val inputConnection = currentInputConnection ?: return
        val word = currentWordBuffer.toString()

        if (isTranslateEnabled && word.isNotEmpty()) {
            // Delete Sinhalese text from input field
            inputConnection.deleteSurroundingText(currentWordBuffer.length, 0)
            currentWordBuffer.setLength(0)

            // Translate before sending enter
            performTranslation(word) { translatedText ->
                if (translatedText.isNotEmpty()) {
                    inputConnection.commitText(translatedText, 1)
                } else {
                    inputConnection.commitText(word, 1)
                }
                sendEnterKeyEvent(inputConnection)
            }
        } else {
            currentWordBuffer.setLength(0)
            sendEnterKeyEvent(inputConnection)
        }
    }

    private fun sendEnterKeyEvent(inputConnection: android.view.inputmethod.InputConnection) {
        val editorInfo = currentInputEditorInfo
        if (editorInfo != null) {
            val action = editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION
            if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
                inputConnection.performEditorAction(action)
                return
            }
        }
        // Fallback to key event
        inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
    }

    private fun switchToNextKeyboard() {
        val token = window?.window?.attributes?.token ?: return
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager ?: return
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

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        currentWordBuffer.setLength(0)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        currentWordBuffer.setLength(0)
    }

    /**
     * Translates the given Sinhalese text to English asynchronously using coroutines,
     * delivering the result via the provided callback.
     */
    fun performTranslation(sinhaleseText: String, onResult: (String) -> Unit) {
        launch {
            try {
                val englishTranslation = translationRepository.translate(sinhaleseText)
                onResult(englishTranslation)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult("")
            }
        }
    }

    override fun onDestroy() {
        cancel()
        super.onDestroy()
    }
}

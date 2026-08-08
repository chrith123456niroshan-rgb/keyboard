package com.sintrans.keyboard

import android.inputmethodservice.InputMethodService
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView

class SinTransKeyboardService : InputMethodService() {

    private var isTranslateEnabled: Boolean = false

    override fun onCreateInputView(): View {
        // Inflate the keyboard view
        val layoutInflater = LayoutInflater.from(this)
        val keyboardView = layoutInflater.inflate(R.layout.keyboard_view, null)

        // Setup individual functional key bindings
        val btnTranslate = keyboardView.findViewById<Button>(R.id.btn_translate_toggle)
        btnTranslate.setOnClickListener {
            isTranslateEnabled = !isTranslateEnabled
            updateTranslateButtonState(btnTranslate)
        }

        val btnSpace = keyboardView.findViewById<Button>(R.id.btn_space)
        btnSpace.setOnClickListener {
            commitText(" ")
        }

        val btnBackspace = keyboardView.findViewById<Button>(R.id.btn_backspace)
        btnBackspace.setOnClickListener {
            handleBackspace()
        }

        val btnGlobe = keyboardView.findViewById<Button>(R.id.btn_globe)
        btnGlobe.setOnClickListener {
            switchToNextKeyboard()
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
                id == R.id.btn_globe) {
                return
            }
            // All other buttons are alphabet character keys
            view.setOnClickListener {
                commitText(view.text.toString())
            }
        }
    }

    private fun commitText(text: CharSequence) {
        val inputConnection = currentInputConnection ?: return
        inputConnection.commitText(text, 1)
    }

    private fun handleBackspace() {
        val inputConnection = currentInputConnection ?: return
        val selectedText = inputConnection.getSelectedText(0)
        if (!selectedText.isNullOrEmpty()) {
            // Delete selection
            inputConnection.commitText("", 1)
        } else {
            // Delete character before cursor
            inputConnection.deleteSurroundingText(1, 0)
        }
    }

    private fun switchToNextKeyboard() {
        val token = window?.window?.attributes?.token ?: return
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager ?: return
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                // For modern APIs, directly call InputMethodService method
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
}

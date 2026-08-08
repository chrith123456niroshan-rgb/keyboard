package com.sintrans.keyboard

import android.inputmethodservice.InputMethodService
import android.view.View

class SinTransKeyboardService : InputMethodService() {
    override fun onCreateInputView(): View? {
        // Return null for now; Phase 2 will implement the active keyboard UI
        return null
    }
}

package com.sintrans.keyboard

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationTranslationService : NotificationListenerService(), CoroutineScope by MainScope() {

    private val translationRepository = GoogleTranslationRepository()
    private var windowManager: WindowManager? = null
    private var floatingOverlayView: View? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onDestroy() {
        super.onDestroy()
        removeFloatingOverlay()
        cancel() // Cancel active coroutines
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        // Restrict to whitelisted messaging apps
        val sbnPackage = sbn.packageName
        val allowedPackages = setOf(
            "com.whatsapp",
            "org.telegram.messenger",
            "com.facebook.orca",
            "com.viber.voip"
        )
        if (!allowedPackages.contains(sbnPackage)) return

        val notification = sbn.notification
        val extras = notification.extras

        // Extract title (sender name)
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""

        // Extract message content with fallbacks
        var text = ""

        // Fallback 1: Standard EXTRA_TEXT
        val extraText = extras.getCharSequence(Notification.EXTRA_TEXT)
        if (!extraText.isNullOrEmpty()) {
            text = extraText.toString()
        }

        // Fallback 2: EXTRA_BIG_TEXT
        if (text.isEmpty()) {
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            if (!bigText.isNullOrEmpty()) {
                text = bigText.toString()
            }
        }

        // Fallback 3: EXTRA_TEXT_LINES (for grouped notifications)
        if (text.isEmpty()) {
            val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            if (!textLines.isNullOrEmpty()) {
                for (j in textLines.size - 1 downTo 0) {
                    val line = textLines[j]
                    if (!line.isNullOrEmpty()) {
                        text = line.toString()
                        break
                    }
                }
            }
        }

        // Basic validation: ignore empty notifications or system updates
        if (title.isEmpty() || text.isEmpty()) return

        // Retrieve the selected target language from shared preferences
        val prefs = getSharedPreferences("sintrans_prefs", Context.MODE_PRIVATE)
        val targetLang = prefs.getString("target_lang", "en") ?: "en"

        // Perform translation in a background coroutine
        launch {
            try {
                val translatedText = translationRepository.translate(text, targetLang)
                if (translatedText.isNotEmpty() && translatedText != text) {
                    // Send broadcast containing translation info
                    val broadcastIntent = Intent("com.sintrans.keyboard.NOTIFICATION_TRANSLATED").apply {
                        putExtra("sender", title)
                        putExtra("original", text)
                        putExtra("translation", translatedText)
                    }
                    sendBroadcast(broadcastIntent)

                    // Display floating bubble if overlay permission is granted
                    if (isOverlayPermissionGranted()) {
                        withContext(Dispatchers.Main) {
                            showFloatingOverlay(title, text, translatedText)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun isOverlayPermissionGranted(): Boolean {
        return android.provider.Settings.canDrawOverlays(this)
    }

    private fun showFloatingOverlay(sender: String, original: String, translated: String) {
        removeFloatingOverlay()

        val wm = windowManager ?: return
        val context = applicationContext

        // Inflate layout for overlay
        val overlayView = LayoutInflater.from(context).inflate(R.layout.popup_notification_translation, null)

        val tvSender = overlayView.findViewById<TextView>(R.id.tv_overlay_sender)
        val tvOriginal = overlayView.findViewById<TextView>(R.id.tv_overlay_original)
        val tvTranslated = overlayView.findViewById<TextView>(R.id.tv_overlay_translated)
        val btnClose = overlayView.findViewById<View>(R.id.btn_overlay_close)

        tvSender.text = sender
        tvOriginal.text = original
        tvTranslated.text = translated

        btnClose.setOnClickListener {
            removeFloatingOverlay()
        }

        // Setup Layout Params for draw over other apps
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 120
            width = (resources.displayMetrics.widthPixels * 0.9).toInt()
        }

        try {
            wm.addView(overlayView, params)
            floatingOverlayView = overlayView

            // Auto-dismiss after 6 seconds
            overlayView.postDelayed({
                if (floatingOverlayView == overlayView) {
                    removeFloatingOverlay()
                }
            }, 6000)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeFloatingOverlay() {
        val view = floatingOverlayView ?: return
        try {
            windowManager?.removeView(view)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        floatingOverlayView = null
    }
}

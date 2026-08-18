package com.sintrans.keyboard

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnEnable = findViewById<Button>(R.id.btn_enable)
        btnEnable.setOnClickListener {
            // Open system input method settings
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }

        val btnSwitch = findViewById<Button>(R.id.btn_switch)
        btnSwitch.setOnClickListener {
            // Display the system input method picker dialog
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }

        // Notification translation settings setup
        val btnNotificationAccess = findViewById<Button>(R.id.btn_notification_access)
        btnNotificationAccess.setOnClickListener {
            if (isNotificationServiceEnabled()) {
                Toast.makeText(this, "Notification access is already granted!", Toast.LENGTH_SHORT).show()
            } else {
                try {
                    val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_SETTINGS)
                    startActivity(intent)
                    Toast.makeText(this, "Please search for 'Notification Access' in Settings", Toast.LENGTH_LONG).show()
                }
            }
        }

        val btnOverlayAccess = findViewById<Button>(R.id.btn_overlay_access)
        btnOverlayAccess.setOnClickListener {
            if (isOverlayPermissionGranted()) {
                Toast.makeText(this, "Overlay permission is already granted!", Toast.LENGTH_SHORT).show()
            } else {
                try {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_SETTINGS)
                    startActivity(intent)
                    Toast.makeText(this, "Please find and allow 'Draw over other apps' for SinTrans", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (!flat.isNullOrEmpty()) {
            val names = flat.split(":")
            for (name in names) {
                val cn = ComponentName.unflattenFromString(name)
                if (cn != null && cn.packageName == pkgName) {
                    return true
                }
            }
        }
        return false
    }

    private fun isOverlayPermissionGranted(): Boolean {
        return Settings.canDrawOverlays(this)
    }
}

package org.nitri.gpxplayer

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG = MainActivity::class.simpleName
        const val ACTION_NO_PERMISSION = "no_permission"
    }

    private var noPermissionAlertShown: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        MockLocationProvider.setContext(applicationContext)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            handleIntent(intent)
        }
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action != null && intent.action.equals(ACTION_NO_PERMISSION) && !noPermissionAlertShown) {
            showNoPermissionAlert()
        }

    }

    private fun showNoPermissionAlert() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("GPS mocking permission required")
            .setMessage("Set GpxPlayer as the mock location app in the Developer Options")
            .setPositiveButton("OK") { _, _ ->
                // Open Android Developer Options
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS
                startActivity(intent)
            }
            .setNegativeButton("Cancel") { _, _ ->
                finish()
            }
            .create()
            .show()
        noPermissionAlertShown = true
    }

}
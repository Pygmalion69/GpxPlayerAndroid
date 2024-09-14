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
        intent?.let {
            handleIntent(it)
        }
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == ACTION_NO_PERMISSION && !noPermissionAlertShown) {
            showNoPermissionAlert()
        }
    }

    private fun showNoPermissionAlert() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.gps_mocking_permission_title)
            .setMessage(R.string.gps_mocking_permission_message)
            .setPositiveButton(R.string.ok) { _, _ ->
                // Open Android Developer Options
                val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                finish()
            }
            .create()
            .show()
        noPermissionAlertShown = true
    }

}
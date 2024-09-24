package org.nitri.gpxplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.k3b.geo.api.GeoPointDto
import de.k3b.geo.io.GeoUri


class MockLocationReceiver : BroadcastReceiver() {

    companion object {
        val TAG = MockLocationReceiver::class.simpleName
        const val KEY_SPEED = "speed"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        context?.applicationContext?.let { appContext ->
            intent?.takeIf { it.action == "org.nitri.gpxplayer.ACTION_SET_LOCATION" }?.data?.scheme?.takeIf { it == "geo" }?.let {
                MockLocationProvider.setContext(appContext)
                MockLocationProvider.setMockGeoPoint(getGeoPointDtoFromIntent(intent))
                val speedKmh = intent.getIntExtra(KEY_SPEED, 3)  // Default to 3 if not present
                MockLocationProvider.setMockSpeedKmh(speedKmh)
                MockLocationProvider.setMockLocation()
            }
        }
    }

    private fun getGeoPointDtoFromIntent(intent: Intent?): GeoPointDto? {
        return intent?.data?.toString()?.let { uriString ->
            GeoUri(GeoUri.OPT_PARSE_INFER_MISSING).fromUri(uriString, GeoPointDto())
        }
    }

}
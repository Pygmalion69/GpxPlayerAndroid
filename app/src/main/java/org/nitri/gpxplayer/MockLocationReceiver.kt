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

        if (context != null && intent!!.action.equals("org.nitri.gpxplayer.ACTION_SET_LOCATION")) {

            MockLocationProvider.setContext(context)

            if (intent.data != null) {

                val scheme = intent.data!!.scheme
                if (scheme != null) {
                    when (scheme) {
                        "geo" -> {
                            MockLocationProvider.setContext(context.applicationContext)
                            MockLocationProvider.setMockGeoPoint(getGeoPointDtoFromIntent(intent))
                            val speedKmh = intent.getIntExtra(KEY_SPEED, 3)
                            MockLocationProvider.setMockSpeedKmh(speedKmh)
                            MockLocationProvider.setMockLocation()
                        }
                    }
                }
            }
        }
    }

    private fun getGeoPointDtoFromIntent(intent: Intent?): GeoPointDto? {
        val uri = intent?.data
        val uriAsString = uri?.toString()
        var pointFromIntent: GeoPointDto? = null
        if (uriAsString != null) {
            val parser = GeoUri(GeoUri.OPT_PARSE_INFER_MISSING)
            pointFromIntent = parser.fromUri(uriAsString, GeoPointDto())
        }
        return pointFromIntent
    }

}
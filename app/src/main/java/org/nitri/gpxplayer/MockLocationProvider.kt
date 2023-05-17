package org.nitri.gpxplayer

import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import de.k3b.geo.api.GeoPointDto
import java.lang.ref.WeakReference
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object MockLocationProvider {

    private var contextRef: WeakReference<Context>? = null

    private var noPermissionActionSent: Boolean = false
    private val setMockLocationHandler = Handler(Looper.getMainLooper())
    private var mockGeoPoint: GeoPointDto? = null
    private var prevMockGeoPoint: GeoPointDto? = null
    private var locationManager: LocationManager? = null

    private var repeatRunnable: Runnable = Runnable {
        run {
            setMockLocation()
            setMockLocationHandler.postDelayed(repeatRunnable, 1000)
        }
    }

    fun setContext(context: Context) {
        contextRef = WeakReference(context)
    }

    fun setMockGeoPoint(geoPoint: GeoPointDto?) {
        //prevMockGeoPoint = mockGeoPoint?.let { GeoPointDto(it.latitude, it.longitude, 0) }
        prevMockGeoPoint = mockGeoPoint
        mockGeoPoint = geoPoint
    }

    fun setMockLocation() {

        if (mockGeoPoint != null) {

            if (locationManager == null) {
                locationManager =
                    contextRef?.get()?.getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager
            }
            setMockLocationHandler.removeCallbacksAndMessages(null)
            try {
                locationManager?.addTestProvider(
                    LocationManager.GPS_PROVIDER,
                    false,
                    false,
                    false,
                    false,
                    true,
                    true,
                    true,
                    ProviderProperties.POWER_USAGE_LOW,
                    ProviderProperties.ACCURACY_FINE
                )
                locationManager?.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true)
                val mockLocation = Location(LocationManager.GPS_PROVIDER)
                mockLocation.latitude = mockGeoPoint!!.latitude
                mockLocation.longitude = mockGeoPoint!!.longitude
                Log.d(MockLocationReceiver.TAG, "mock: " + mockGeoPoint!!.latitude + ", " + mockGeoPoint!!.longitude)
                mockLocation.altitude = 10.0
                mockLocation.accuracy = 5.0F
                if (prevMockGeoPoint != null) {
                    Log.d(MockLocationReceiver.TAG, "prev: " + prevMockGeoPoint!!.latitude + ", " + prevMockGeoPoint!!.longitude)
                    mockLocation.bearing = bearing(
                        prevMockGeoPoint!!.latitude, prevMockGeoPoint!!.longitude,
                        mockLocation.latitude, mockLocation.longitude
                    )
                } else {
                    Log.d(MockLocationReceiver.TAG, "prev: null")
                }
                Log.d(MockLocationReceiver.TAG, "bearing: " + mockLocation.bearing)
                mockLocation.time = System.currentTimeMillis()
                mockLocation.elapsedRealtimeNanos = System.nanoTime()
                locationManager?.setTestProviderLocation(
                    LocationManager.GPS_PROVIDER,
                    mockLocation
                )
                noPermissionActionSent = false
                //setMockLocationHandler.postDelayed(repeatRunnable, 1000)
            } catch (e: SecurityException) {
                if (!noPermissionActionSent) {
                    val startMainActivityIntent =
                        Intent(contextRef?.get()?.applicationContext, MainActivity::class.java)
                    startMainActivityIntent.action = MainActivity.ACTION_NO_PERMISSION
                    startMainActivityIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                    contextRef?.get()?.applicationContext?.startActivity(startMainActivityIntent)
                    noPermissionActionSent = true
                }
            }
        }
    }

    private fun bearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val latitude1 = Math.toRadians(lat1)
        val latitude2 = Math.toRadians(lat2)
        val longDiff = Math.toRadians(lon2 - lon1)
        val y = sin(longDiff) * cos(latitude2)
        val x =
            cos(latitude1) * sin(latitude2) - sin(latitude1) * cos(latitude2) * cos(
                longDiff
            )
        return ((Math.toDegrees(atan2(y, x)) + 360) % 360).toFloat()
    }
}
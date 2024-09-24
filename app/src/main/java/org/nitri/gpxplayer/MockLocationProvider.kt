package org.nitri.gpxplayer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import de.k3b.geo.api.GeoPointDto
import java.lang.ref.WeakReference
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object MockLocationProvider {

    private val TAG = MockLocationProvider::class.simpleName
    
    private var prevMockTime: Long? = null
    private var contextRef: WeakReference<Context>? = null

    private var noPermissionActionSent: Boolean = false
    private val setMockLocationHandler = Handler(Looper.getMainLooper())
    private var mockGeoPoint: GeoPointDto? = null
    private var mockSpeedKmh: Int? = null
    private var prevMockGeoPoint: GeoPointDto? = null
    private var locationManager: LocationManager? = null

    private val repeatRunnable: Runnable = object : Runnable {
        override fun run() {
            setMockLocation()
            setMockLocationHandler.postDelayed(this, 1000)
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

    fun setMockSpeedKmh(speedKmh: Int) {
        mockSpeedKmh = speedKmh
    }

    fun setMockLocation() {
        val context = contextRef?.get() ?: return
        val locationManager = locationManager ?: run {
            context.getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager
        }

        mockGeoPoint?.let { geoPoint ->
            try {
                setupLocationProviders(locationManager)

                val mockLocation = createMockLocation(geoPoint)
                locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, mockLocation)
                locationManager.setTestProviderLocation(LocationManager.NETWORK_PROVIDER, mockLocation)

                noPermissionActionSent = false
                if (mockLocation.speed < 1) {
                    setMockLocationHandler.postDelayed(repeatRunnable, 500)
                }
            } catch (e: SecurityException) {
                handleSecurityException(context)
            }
        }
    }

    @SuppressLint("WrongConstant") // No ProviderProperties below API 31, int values passed
    private fun setupLocationProviders(locationManager: LocationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // API level 31
            // Use new ProviderProperties constants
            locationManager.addTestProvider(
                LocationManager.GPS_PROVIDER,
                false, false, false, false, true, true, true,
                ProviderProperties.POWER_USAGE_LOW, ProviderProperties.ACCURACY_FINE
            )
            locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true)

            locationManager.addTestProvider(
                LocationManager.NETWORK_PROVIDER,
                false, false, false, false, true, true, true,
                ProviderProperties.POWER_USAGE_LOW, ProviderProperties.ACCURACY_FINE
            )
            locationManager.setTestProviderEnabled(LocationManager.NETWORK_PROVIDER, false)
        } else {
            // API level 30 or lower
            locationManager.addTestProvider(
                LocationManager.GPS_PROVIDER,
                false, false, false, false, true, true, true,
                1, 1
            )
            locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true)

            locationManager.addTestProvider(
                LocationManager.NETWORK_PROVIDER,
                false, false, false, false, true, true, true,
                1, 1
            )
            locationManager.setTestProviderEnabled(LocationManager.NETWORK_PROVIDER, false)
        }
    }

    private fun createMockLocation(geoPoint: GeoPointDto): Location {
        return Location(LocationManager.GPS_PROVIDER).apply {
            latitude = geoPoint.latitude
            longitude = geoPoint.longitude
            altitude = 10.0
            accuracy = 5.0F
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = System.nanoTime()
            speed = mockSpeedKmh?.div(3.6f) ?: 0f
            prevMockGeoPoint?.let { prevGeoPoint ->
                bearing = calculateBearing(prevGeoPoint.latitude, prevGeoPoint.longitude, latitude, longitude)
                prevMockTime?.let { prevTime ->
                    if (mockSpeedKmh == null || mockSpeedKmh == 0) {
                        speed = calculateSpeed(
                            prevGeoPoint.latitude,
                            prevGeoPoint.longitude,
                            latitude,
                            longitude,
                            time - prevTime ?: 0
                        )
                    }
                }
            }
            prevMockTime = time
        }
    }

    private fun handleSecurityException(context: Context) {
        if (!noPermissionActionSent) {
            val startMainActivityIntent = Intent(context, MainActivity::class.java).apply {
                action = MainActivity.ACTION_NO_PERMISSION
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            context.startActivity(startMainActivityIntent)
            noPermissionActionSent = true
        }
    }

    private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
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

    private fun calculateSpeed(lat1: Double, lon1: Double, lat2: Double, lon2: Double, deltaTimeMillis: Long) : Float {
        val distance = calculateDistance(lat1, lon1, lat2, lon2)
        return (distance / (deltaTimeMillis / 1000f)).toFloat()
    }
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth's radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val distance = r * c * 1000 // meters
        Log.d(TAG, "distance == $distance")
        return distance
    }
}
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

    private val repeatRunnable = Runnable {
        setMockLocation()
    }

    fun setContext(context: Context) {
        if (contextRef?.get() != context) {
            contextRef = WeakReference(context)
        }
    }

    fun setMockGeoPoint(geoPoint: GeoPointDto?) {
        if (geoPoint != mockGeoPoint) {
            prevMockGeoPoint = mockGeoPoint
            mockGeoPoint = geoPoint
        }
    }

    fun setMockSpeedKmh(speedKmh: Int) {
        mockSpeedKmh = speedKmh
    }

    fun setMockLocation() {
        setMockLocationHandler.removeCallbacks(repeatRunnable)
        val context = contextRef?.get() ?: return
        
        if (locationManager == null) {
            locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            try {
                setupLocationProviders(locationManager!!)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to setup location providers", e)
            }
        }

        val lm = locationManager ?: return

        mockGeoPoint?.let { geoPoint ->
            try {
                val mockLocation = createMockLocation(geoPoint)
                lm.setTestProviderLocation(LocationManager.GPS_PROVIDER, mockLocation)
                lm.setTestProviderLocation(LocationManager.NETWORK_PROVIDER, mockLocation)

                noPermissionActionSent = false
                if (mockLocation.speed < 1f) {
                    setMockLocationHandler.postDelayed(repeatRunnable, 1000)
                }
            } catch (e: SecurityException) {
                handleSecurityException(context)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set mock location", e)
            }
        }
    }

    @SuppressLint("WrongConstant") // No ProviderProperties below API 31, int values passed
    private fun setupLocationProviders(locationManager: LocationManager) {
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).forEach { provider ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // API level 31
                    // Use new ProviderProperties constants
                    locationManager.addTestProvider(
                        provider,
                        false, false, false, false, true, true, true,
                        ProviderProperties.POWER_USAGE_LOW, ProviderProperties.ACCURACY_FINE
                    )
                } else {
                    // API level 30 or lower
                    locationManager.addTestProvider(
                        provider,
                        false, false, false, false, true, true, true,
                        1, 1
                    )
                }
                locationManager.setTestProviderEnabled(provider, provider == LocationManager.GPS_PROVIDER)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Test provider $provider already exists")
            }
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
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
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
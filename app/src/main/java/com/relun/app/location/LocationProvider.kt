package com.relun.app.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

data class Coordinates(val latitude: Double, val longitude: Double)

class LocationProvider(private val context: Context) {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    /** Last fix this session, reused by Discover so it doesn't wait for GPS each time. */
    @Volatile var last: Coordinates? = null
        private set

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** City-level accuracy is all Discover needs, so this asks for balanced power. */
    @SuppressLint("MissingPermission")
    suspend fun current(): Coordinates? {
        if (!hasPermission()) return null
        val fix = withTimeoutOrNull(10_000) {
            val token = CancellationTokenSource()
            runCatching {
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, token.token).await()
            }.getOrNull() ?: runCatching { client.lastLocation.await() }.getOrNull()
        }
        return fix?.let { Coordinates(it.latitude, it.longitude) }?.also { last = it }
    }

    suspend fun cityName(coordinates: Coordinates): String? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null
        val geocoder = Geocoder(context)
        val address = if (Build.VERSION.SDK_INT >= 33) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocation(coordinates.latitude, coordinates.longitude, 1) { list ->
                    cont.resume(list.firstOrNull())
                }
            }
        } else {
            @Suppress("DEPRECATION")
            runCatching { geocoder.getFromLocation(coordinates.latitude, coordinates.longitude, 1)?.firstOrNull() }
                .getOrNull()
        }
        address?.let { listOfNotNull(it.subAdminArea ?: it.locality, it.countryName).joinToString(", ") }
            ?.takeIf { it.isNotBlank() }
    }

    /** Coordinates for a typed city name, used when location permission is denied. */
    suspend fun geocodeCity(name: String): Coordinates? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null
        val geocoder = Geocoder(context)
        val address = if (Build.VERSION.SDK_INT >= 33) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocationName(name, 1) { list -> cont.resume(list.firstOrNull()) }
            }
        } else {
            @Suppress("DEPRECATION")
            runCatching { geocoder.getFromLocationName(name, 1)?.firstOrNull() }.getOrNull()
        }
        address?.let { Coordinates(it.latitude, it.longitude) }?.also { last = it }
    }
}

package kr.buswhere.app.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import kr.buswhere.app.model.GeoPointDto

/** Android 위치 제공자를 통해 사용자의 현재 좌표를 한 번 조회합니다. */
class LocationService(private val context: Context) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    /** 위치 권한이 허용됐는지 확인합니다. */
    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    /** GPS 또는 네트워크 제공자에서 현재 위치를 비동기로 가져옵니다. */
    @SuppressLint("MissingPermission")
    fun getCurrentLocation(onResult: (Result<GeoPointDto>) -> Unit) {
        if (!hasPermission()) {
            onResult(Result.failure(SecurityException("위치 권한이 필요합니다.")))
            return
        }

        val provider = when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> {
                onResult(Result.failure(IllegalStateException("휴대전화의 위치 기능을 켜주세요.")))
                return
            }
        }

        var completed = false
        val handler = Handler(Looper.getMainLooper())
        lateinit var listener: LocationListener
        val timeout = Runnable {
            if (!completed) {
                completed = true
                locationManager.removeUpdates(listener)
                onResult(Result.failure(IllegalStateException("현재 위치 확인 시간이 초과되었습니다.")))
            }
        }
        listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (completed) return
                completed = true
                handler.removeCallbacks(timeout)
                locationManager.removeUpdates(this)
                onResult(Result.success(GeoPointDto(location.latitude, location.longitude)))
            }

            @Deprecated("Deprecated in Android")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }

        try {
            locationManager.getLastKnownLocation(provider)?.let { cached ->
                if (System.currentTimeMillis() - cached.time <= 120_000L) {
                    completed = true
                    onResult(Result.success(GeoPointDto(cached.latitude, cached.longitude)))
                    return
                }
            }
            locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
            handler.postDelayed(timeout, 12_000L)
        } catch (exception: SecurityException) {
            onResult(Result.failure(exception))
        }
    }
}

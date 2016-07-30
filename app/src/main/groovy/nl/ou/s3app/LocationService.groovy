package nl.ou.s3app

import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import groovy.transform.CompileStatic
import nl.ou.s3.common.LocationDto

@CompileStatic
class LocationService extends Service {
    private final String TAG = "LocationService";

    private static final int TWO_MINUTES = 1000 * 60 * 2
    private static final int TEN_SECONDS = 1000 * 10
    private static final int FIVE_METERS = 0 // 5

    LocationManager locationManager
    LocationServiceLocationListener listener
    Location previousBestLocation = null

    @Override
    void onCreate() {
        super.onCreate()
        Log.d(TAG, "LocationService is gestart.")
    }

    @Override
    void onStart(Intent intent, int startId) {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE)
        listener = new LocationServiceLocationListener(this)

        def providers = locationManager.getAllProviders()
        if (!providers) throw new IllegalStateException("Geen locatieproviders voor S3App beschikbaar!")

        if (LocationManager.NETWORK_PROVIDER in providers) {
            Log.d(TAG, "NETWORK_PROVIDER is beschikbaar.")
            locationManager
                    .requestLocationUpdates(LocationManager.NETWORK_PROVIDER, TEN_SECONDS, FIVE_METERS, listener)
        }

        if (LocationManager.GPS_PROVIDER in providers) {
            Log.d(TAG, "GPS_PROVIDER is beschikbaar.")
            locationManager
                    .requestLocationUpdates(LocationManager.GPS_PROVIDER, TEN_SECONDS, FIVE_METERS, listener)
        }
    }

    @Override
    IBinder onBind(Intent intent) {
        null
    }

    /**
     * Bepaalt of nieuwe locatie beter is dan de reeds bekende locatie.
     */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (!currentBestLocation) return true    // Alles is beter dan geen locatie.

        // Is nieuwe locatie ouder of nieuwer?
        long timeDelta = location.getTime() - currentBestLocation.getTime()
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES
        boolean isNewer = timeDelta > 0

        if (isSignificantlyNewer) return true
        if (isSignificantlyOlder) return false

        // Is nieuwe locatie meer of minder accuraat?
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy())
        boolean isLessAccurate = accuracyDelta > 0
        boolean isMoreAccurate = accuracyDelta < 0
        boolean isSignificantlyLessAccurate = accuracyDelta > 200

        // Is nieuwe locatie van dezelfde provider?
        boolean fromSameProvider = isSameProvider(location.getProvider(), currentBestLocation.getProvider())

        // Bepaal kwaliteit van de locatie a.d.h.v. tijdigheid en precisie.
        if (isMoreAccurate) {
            return true
        } else if (isNewer && !isLessAccurate) {
            return true
        } else if (isNewer && !isSignificantlyLessAccurate && fromSameProvider) {
            return true
        }

        false
    }

    /**
     * Zijn 2 gegeven providers hetzelfde?
     */
    private boolean isSameProvider(String provider1, String provider2) {
        if (!provider1) {
            return provider2 == null
        }

        provider1 == provider2
    }

    @Override
    void onDestroy() {
        Log.d(TAG, "LocationService wordt afgebroken.")
        locationManager.removeUpdates(listener)
        super.onDestroy()
    }

    /**
     * Voor deze service op de achtergrond uit.
     */
    static Thread performOnBackgroundThread(final Runnable runnable) {
        final Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    runnable.run()
                } finally {
                }
            }
        }

        t.start()
        t
    }

    /**
     * Implementatie LocationListener voor deze LocationService.
     */
    class LocationServiceLocationListener implements LocationListener {
        Context context

        LocationServiceLocationListener(Context context) {
            super()
            this.context = context
        }

        void onLocationChanged(final Location loc) {
            if (isBetterLocation(loc, previousBestLocation)) {
                previousBestLocation = loc
                def locationDto = new LocationDto(time: loc.time, latitude: loc.latitude, longitude: loc.longitude)
                VolatileLocationData.estimatedLocation = locationDto

                Log.d(TAG, "Nieuw locatie: ${VolatileLocationData.estimatedLocation.toString()}")
            }
        }

        void onProviderDisabled(String provider) {
            Log.d(TAG, "GPS uitgezet")
        }

        void onProviderEnabled(String provider) {
            Log.d(TAG, "GPS aangezet")
        }

        void onStatusChanged(String provider, int status, Bundle extras) { }
    }

}

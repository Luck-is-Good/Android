package com.example.service_project;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class BackgroundLocationService extends Service {
    private final LocationServiceBinder binder = new LocationServiceBinder();
    private static final String TAG = "BackgroundLocationServi";
    private LocationListener mLocationListener;
    private LocationManager mLocationManager;
    final FirebaseFirestore db =FirebaseFirestore.getInstance();
    long now;
    Date date;
    String time_now;

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private class LocationListener implements android.location.LocationListener {
        private Location lastLocation = null;
        private final String TAG = "LocationListener";
        private Location mLastLocation;

        public LocationListener(String provider) {
            mLastLocation = new Location(provider);
        }

        public Double getLognitude() {
            return mLastLocation.getLongitude();
        }

        @Override
        public void onLocationChanged(Location location) {
            mLastLocation = location;
            Log.d("debug", "LocationChanged: " + location);
            now = System.currentTimeMillis();
            date = new Date(now);
            SimpleDateFormat time = new SimpleDateFormat("yyyy년 MM월 dd일 HH시 mm분 ss초");
            time_now = time.format(date);

            Map<String, Object> user = new HashMap<>();
            user.put("longitude", location.getLongitude());
            user.put("latitude", location.getLatitude());
            user.put("createdAt", time_now);

            String path = ((MainActivity)MainActivity.context_main).path;
            db.collection("USERS/" + path + "/locations")
                    .add(user)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>(){

                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            //Log.d("debug", "latitude : " + String.valueOf(latitude));
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d("debug", "Error adding", e);
                        }
                    });
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.e(TAG, "onStatusChanged: " + status);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_NOT_STICKY;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate");
        startForeground(12345678, getNotification());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mLocationManager != null) {
            try {
                mLocationManager.removeUpdates(mLocationListener);
            } catch (Exception ex) {
                Log.i(TAG, "fail to remove location listeners, ignore", ex);
            }
        }
    }

    private void initializeLocationManager() {
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    public void startTracking() {
        initializeLocationManager();
        mLocationListener = new LocationListener(LocationManager.GPS_PROVIDER);
        mLocationListener = new LocationListener(LocationManager.NETWORK_PROVIDER);
        try {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    10000, 0, mLocationListener);
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    10000, 0, mLocationListener);
            Log.d("debug", String.valueOf(mLocationListener.getLognitude()));
        } catch (java.lang.SecurityException ex) {
            // Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            // Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }

    }

    public void stopTracking() {
        this.onDestroy();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private Notification getNotification() {

        NotificationChannel channel = new NotificationChannel(
                "channel_01", "My Channel", NotificationManager.IMPORTANCE_DEFAULT);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        Notification.Builder builder = new Notification.Builder(getApplicationContext(), "channel_01").setAutoCancel(true);
        return builder.build();
    }


    public class LocationServiceBinder extends Binder {
        public BackgroundLocationService getService() {
            return BackgroundLocationService.this;
        }
    }
}
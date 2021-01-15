package com.example.service_project;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class UsingActivity extends AppCompatActivity {

    private final int PERMISSION_REQUEST_CODE = 200;
    Button stopButton;
    //Button restartButton;

    public com.example.service_project.BackgroundLocationService gpsService;
    public boolean mTracking = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.using);
        stopButton = (Button) findViewById(R.id.stopButton);
        //restartButton = (Button) findViewById(R.id.restartButton);

        final Intent intent = new Intent(this.getApplication(), com.example.service_project.BackgroundLocationService.class);
        this.getApplication().startService(intent);
        this.getApplication().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mTracking) {
                    startTracking();
                } else {
                    stopTracking();
                }
            }
        });
    }

    public void startTracking() {
        //check for permission
        if (ContextCompat.checkSelfPermission(getApplicationContext(), ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            gpsService.startTracking();
            mTracking = true;
            Intent intent = new Intent(getApplicationContext(), UsingActivity.class);
            startActivity(intent);
            //toggleButtons();
            stopButton.setBackgroundResource(R.drawable.stop);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startTracking();
            }
        }
    }

    public void stopTracking() {
        mTracking = false;
        gpsService.stopTracking();
        //toggleButtons();
        stopButton.setBackgroundResource(R.drawable.start);
    }

    /*private void toggleButtons() {
        restartButton.setEnabled(!mTracking);
        stopButton.setEnabled(mTracking);
        //statusTextView.setText((mTracking) ? "TRACKING" : "GPS Ready");
    }*/
    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            String name = className.getClassName();
            if (name.endsWith("BackgroundLocationService")) {
                gpsService = ((com.example.service_project.BackgroundLocationService.LocationServiceBinder) service).getService();
                stopButton.setEnabled(true);
                //statusTextView.setText("GPS Ready");
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            if (className.getClassName().equals("BackgroundLocationService")) {
                gpsService = null;
            }
        }
    };
}

package com.example.service_project;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;

import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button ok;
    EditText user_id;
    String deviceId;

    public static Context context_main;
    public String path;
    private final int PERMISSION_REQUEST_CODE = 200;
    final FirebaseFirestore db =FirebaseFirestore.getInstance();

    public com.example.service_project.BackgroundLocationService gpsService;
    public boolean mTracking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context_main = this;

        if ( ContextCompat.checkSelfPermission( this, Manifest.permission.READ_PHONE_STATE )
                != PackageManager.PERMISSION_GRANTED ) {
            deviceId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        }

        final Intent intent = new Intent(this.getApplication(), com.example.service_project.BackgroundLocationService.class);
        this.getApplication().startService(intent);
        this.getApplication().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        user_id = (EditText) findViewById(R.id.editText);
        ok = (Button) findViewById(R.id.ok);
        ok.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Log.d("debug", "user_id : "+ user_id.getText().toString());
        db.collection("USERS")
                .whereEqualTo("user_id" ,  user_id.getText().toString() )
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        //Log.d("debug",   " db에서 얻어온 데이터 => " );
                        path = null;
                        if(task.isSuccessful()){
                            QuerySnapshot document = task.getResult();
                            for (QueryDocumentSnapshot i : task.getResult()) {
                                path = i.getId();
                            }
                                Map<String, Object> ID = new HashMap<>();
                                ID.put("device_id", deviceId);
                                db.collection("USERS").document(path)
                                        .set(ID, SetOptions.merge())
                                        .addOnSuccessListener(new OnSuccessListener<Void>(){
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                //Log.d("debug", "latitude : " + String.valueOf(latitude));
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {

                                            }
                                        });

                        }else {
                            Log.d("debug", "Error getting documents: ", task.getException());
                        }
                    }
                });

        Intent intent = new Intent(getApplicationContext(), UsingActivity.class);
        startActivity(intent);
        startTracking();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startTracking();
            }
        }
    }

    public void startTracking() {
        //check for permission
        if (ContextCompat.checkSelfPermission(getApplicationContext(), ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            gpsService.startTracking();
            mTracking = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            String name = className.getClassName();
            if (name.endsWith("BackgroundLocationService")) {
                gpsService = ((com.example.service_project.BackgroundLocationService.LocationServiceBinder) service).getService();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            if (className.getClassName().equals("BackgroundLocationService")) {
                gpsService = null;
            }
        }
    };
}
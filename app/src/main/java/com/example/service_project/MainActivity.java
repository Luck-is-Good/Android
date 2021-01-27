package com.example.service_project;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import android.content.SharedPreferences;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button ok;
    EditText user_id;
    String deviceId;

    public static Context context_main;
    public String path;
    private final int PERMISSION_REQUEST_CODE = 200;
    final FirebaseFirestore db =FirebaseFirestore.getInstance();

    double longitude;
    double latitude;
    public com.example.service_project.BackgroundLocationService gpsService;
    public boolean mTracking = false;
    long now;
    Date date;
    String time_now;

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context_main = this;

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        editor = preferences.edit();

        String Path = preferences.getString("Path", null);
        if(Path != null){
            Log.d("debug" , "Preference => " + Path);
            path = Path;
            Intent intent = new Intent(getApplicationContext(), UsingActivity.class);
            startActivity(intent);
        }
        //현재위치의 위도,경도 저장하기
        final LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if ( Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission( getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions( MainActivity.this, new String[] {  android.Manifest.permission.ACCESS_FINE_LOCATION  },
                    0 );
        }
        else{
            //Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            longitude = location.getLongitude();
            latitude = location.getLatitude();
        }

        //현재시간 저장
        now = System.currentTimeMillis();
        date = new Date(now);
        SimpleDateFormat time = new SimpleDateFormat("yyyy년 MM월 dd일 HH시 mm분 ss초");
        time_now = time.format(date);

        //deviceId 생성
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

    //ok버튼을 클릭하면 device에서 입력하는 번호와 맞는 user_id가 있는지 검사
    @Override
    public void onClick(View v) {
        db.collection("USERS")
                .whereEqualTo("user_id" ,  user_id.getText().toString() ) //쿼리문으로 일치하는 user_id부분이 있는지 검사
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        path = null;
                        if(task.isSuccessful()){
                            QuerySnapshot document = task.getResult();
                            for (QueryDocumentSnapshot i : task.getResult()) {
                                //사용자 이름 받아옴
                                path = i.getId();
                                editor.putString("Path", path);
                                editor.apply();
                            }
                            //일치하는 user_id를 찾지 못한 경우
                            if(path == null) {
                                Toast.makeText(MainActivity.this, "Retry!!", Toast.LENGTH_SHORT).show();
                            } else{
                                //일치하는 user_id 찾음
                                //위에서 받아온 사용자 이름을 경로로 지정해서 device_id firestore에 저장
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

                                //위에서 받은 위치정보를 해당 사용자에게 저장
                                Map<String, Object> Loc = new HashMap<>();
                                Loc.put("longitude", longitude);
                                Loc.put("latitude", latitude);
                                Loc.put("createdAt", time_now);
                                db.collection("USERS/"+path+"/locations").document()
                                        .set(Loc, SetOptions.merge())
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
                                user_id.setText(null);
                                //정보를 다 저장하고,
                                //사용중지화면으로 넘어가도록(web-app에서 위치정보를 확인후에 사용중 클릭하도록)
                                stopTracking();
                            }
                        }else {
                            Log.d("debug", "Error getting documents: ", task.getException());
                        }
                    }
                });
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
            Intent intent = new Intent(getApplicationContext(), UsingActivity.class);
            startActivity(intent);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
        }
    }
    public void stopTracking() {
        mTracking = false;
        gpsService.stopTracking();
        Intent intent = new Intent(getApplicationContext(), UsingActivity.class);
        startActivity(intent);
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
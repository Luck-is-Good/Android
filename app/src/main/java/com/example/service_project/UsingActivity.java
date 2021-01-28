package com.example.service_project;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.File;
import java.util.Collection;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import android.content.SharedPreferences;

public class UsingActivity extends AppCompatActivity {

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    private final int PERMISSION_REQUEST_CODE = 200;
    Button button, reset;
    TextView statusTextView2;
    public com.example.service_project.BackgroundLocationService gpsService;
    public boolean mTracking = false;


    final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.using);
        button = (Button) findViewById(R.id.button);
        reset = (Button) findViewById((R.id.reset));
        statusTextView2 = (TextView) findViewById(R.id.statusTextView2);

        final Intent intent = new Intent(this.getApplication(), com.example.service_project.BackgroundLocationService.class);
        this.getApplication().startService(intent);
        this.getApplication().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        editor = preferences.edit();


        //MainActivity에서 받아온 path 변수에 접근
        String path = ((MainActivity)MainActivity.context_main).path;
        Log.d("debug", "UsingActivity path => " + path);
        final DocumentReference docRef = db.collection("USERS").document(path);
        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException error) {
                        if(error != null) {
                            Log.d("debug", "Listen Failed", error);
                            return;
                        }
                        if(snapshot != null && snapshot.exists()) {
                            Log.d("debug", "Current Data : " + snapshot.getData());
                        } else {
                            Log.d("debug", "Current Data : null");
                            File cache = getCacheDir();
                            File appDir = new File(cache.getParent());
                            if(appDir.exists()){
                                String[] children = appDir.list();
                                for(String s : children) {
                                    if(!s.equals("lib") && !s.equals("files")) {
                                        deleteDir(new File(appDir, s));
                                    }
                                }
                            }
                            moveTaskToBack(true);
                            finish();
                            android.os.Process.killProcess(android.os.Process.myPid());
                        }
                        /*for(QueryDocumentSnapshot i : value) {
                            Log.d("debug", "document snapshot start!!-----");
                            Log.d("debug", "i.getId ==> " + i.getId());
                            Log.d("debug", "path ==> " + path);
                            if(path.equals(i.getId())) {
                                Log.d("debug", "document delete!!!-----");
                                File cache = getCacheDir();
                                File appDir = new File(cache.getParent());
                                if(appDir.exists()){
                                    String[] children = appDir.list();
                                    for(String s : children) {
                                        if(!s.equals("lib") && !s.equals("files")) {
                                            deleteDir(new File(appDir, s));
                                        }
                                    }
                                }
                                moveTaskToBack(true);
                                finish();
                                android.os.Process.killProcess(android.os.Process.myPid());

                            }
                        }*/
                    }
                });

        boolean Tracking = preferences.getBoolean("Track", false);
        if(Tracking){
            statusTextView2.setText("사용자의 위치가 다른 사용자에게\n표시되고 있습니다.");
            button.setBackgroundResource(R.drawable.stop);
        } else {
            statusTextView2.setText("위치 표시가 중지되었습니다.\n");
            button.setBackgroundResource(R.drawable.start);
        }

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mTracking) {
                    stopTracking();
                } else {
                    startTracking();
                }
            }
        });

        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(UsingActivity.this);
                builder.setMessage("사용자 정보가 초기화됩니다. 초기화를 진행할까요? " +
                        "초기화후에는 앱이 종료됩니다. 서비스를 다시 사용하려면 앱을 재실행해주세요");
                builder.setTitle("초기화")
                        .setCancelable(false)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //앱 캐시 지우기
                                File cache = getCacheDir();
                                File appDir = new File(cache.getParent());
                                if(appDir.exists()){
                                    String[] children = appDir.list();
                                    for(String s : children) {
                                        if(!s.equals("lib") && !s.equals("files")) {
                                            deleteDir(new File(appDir, s));
                                        }
                                    }
                                }
                                //UsingActivity 뿐만 아니라 앱 전체 종료
                                // moveTaskToBack(true);
                                finish();
                                //android.os.Process.killProcess(android.os.Process.myPid());

                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.setTitle("초기화");
                alert.show();

            }
        });
    }
    private static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    public void onBackPressed() {
        //using페이지에서 뒤로가기 동작 막음
    }
    public void startTracking() {
        //check for permission
        if (ContextCompat.checkSelfPermission(getApplicationContext(), ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mTracking = false;
            gpsService.startTracking();
            //앱 종료 후 다시 시작했을 때, start버튼으로 유지하기 위해 Track을 true값으로 남겨놓기
            editor.putBoolean("Track", true);
            editor.apply();
            statusTextView2.setText("사용자의 위치가 다른 사용자에게\n표시되고 있습니다.");
            button.setBackgroundResource(R.drawable.stop);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
        }
    }

    public void stopTracking() {
        mTracking = true;
        gpsService.stopTracking();
        editor.putBoolean("Track", false);
        editor.apply();
        statusTextView2.setText("위치 표시가 중지되었습니다.\n");
        button.setBackgroundResource(R.drawable.start);
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

    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            String name = className.getClassName();
            if (name.endsWith("BackgroundLocationService")) {
                gpsService = ((com.example.service_project.BackgroundLocationService.LocationServiceBinder) service).getService();
                button.setEnabled(true);
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

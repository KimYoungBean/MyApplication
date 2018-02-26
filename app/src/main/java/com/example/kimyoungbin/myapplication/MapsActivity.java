package com.example.kimyoungbin.myapplication;

import android.Manifest.permission;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * Created by kimyoungbin on 2018. 2. 26..
 */

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private PolylineOptions mPolylineOptions;
    private Marker mMarker;
    private double mLat;
    private double mLng;
    private double lLat;
    private double lLng;
    public float light;
    SensorManager mSensorManager;
    boolean day;

    final static String folderName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/LatLog";
    ;
    final static String fileName = "log.txt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        day = true;

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        SensorEventListener mListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float[] v = event.values;
                if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
                    light = v[0];
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        mSensorManager.registerListener(mListener, mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT), SensorManager.SENSOR_DELAY_FASTEST);

        mPolylineOptions = new PolylineOptions();

        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        final LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 1, mLocationListener);

    }


    private final LocationListener mLocationListener = new LocationListener() {

        @SuppressLint("SetTextI18n")
        @Override
        public void onLocationChanged(Location location) {
            mLat = location.getLatitude();
            mLng = location.getLongitude();

            mOnFileWrite();
            if (mMarker != null)
                mMarker.remove();


            if (light < 1000) {
                Toast.makeText(getApplicationContext(), "indoors", Toast.LENGTH_SHORT).show();
                lLat = mLat;
                lLng = mLng;
                if (ActivityCompat.checkSelfPermission(MapsActivity.this, permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MapsActivity.this, permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                mMap.setMyLocationEnabled(false);
                LatLng latlng = new LatLng(mLat, mLng);

                mMarker = mMap.addMarker(new MarkerOptions().position(latlng).title("current location"));

                mMap.addPolyline(mPolylineOptions.add(latlng).color(Color.RED).width(5));

                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 20));
            } else {
                Toast.makeText(getApplicationContext(), "outdoors", Toast.LENGTH_SHORT).show();
                mMap.clear();
                mMap.setMyLocationEnabled(true);
//                LatLng latlng = new LatLng(mLat, mLng);
//
//                mMarker = mMap.addMarker(new MarkerOptions().position(latlng).title("current location"));
//
//                mMap.addPolyline(mPolylineOptions.add(latlng).color(Color.RED).width(5));
//
//                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 20));

            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {
            Toast.makeText(getApplicationContext(), "gps enabled", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onProviderDisabled(String provider) {
            Toast.makeText(getApplicationContext(), "gps disabled", Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        // Add a marker in Sydney, Australia, and move the camera.
   /* LatLng sydney = new LatLng(mLat, mLng);
    mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
    mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));*/
    }
    public void mOnFileWrite(){
        String contents = "Log 생성\n"+mLat+" "+mLng+"\n";
        WriteTextFile(folderName, fileName, contents);
    }

    public void WriteTextFile(String folderNameame, String fileNameame, String contents){
        try{
            File dir = new File(folderName);
            if(!dir.exists()){
                dir.mkdir();
            }
            FileOutputStream fos = new FileOutputStream(folderName+"/"+fileName, true);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));
            writer.write(contents);
            writer.flush();

            writer.close();
            fos.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}

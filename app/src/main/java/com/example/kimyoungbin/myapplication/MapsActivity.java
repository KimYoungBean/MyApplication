package com.example.kimyoungbin.myapplication;

import android.Manifest.permission;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
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

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, SensorEventListener {
    private TextView mTextView;
    /* Avoid counting faster than stepping */
    private boolean pocketFlag;
    private boolean callingFlag;
    private boolean handHeldFlag;
    private boolean handTypingFlag;

    /* Set threshold */
    private final double maxPocketThs = 15.0;
    private final double minPocketThs = 11.5;
    private final double maxCallingThs = 12.7;
    private final double minCallingThs = 11.1;
    private final double maxTypingThs = 13.5;
    private final double minTypingThs = 11.0;
    private final double maxHeldThs = 13.8;
    private final double minHeldThs = 11.0;
    private final double handHeldXThs = 1.0;
    private final double handHeldZThs = 1.5;
    private final double handTypingThs = 0.5;

    private int stepCount;
    private int compassCount;

    private SensorManager mSensorManager = null;

    // Using the Accelometer
    private SensorEventListener mAccLis;
    private Sensor mAccelometerSensor = null;

    // Using the Gyroscoper
    private SensorEventListener mGyroLis;
    private Sensor mGgyroSensor = null;

    // Using the Closesensor
    private SensorEventListener mClsLis;
    private Sensor mClsSensor = null;

    // Using the Dirsensor
    private SensorEventListener mDirLis;
    private Sensor mDirSensor = null;

    //Using the LightSensor
    private SensorEventListener mLightLis;
    private Sensor mLightSensor = null;

    // compass Value
    private int compassValue;

    // To distinguish state
    private boolean isPocket;
    private boolean isHandHeld;
    private boolean isHandTyping;
    private boolean isPocketToHand;

    private float distance;

    // prevent abnormal count
    private long startTime;
    private long endTime;

    // To use googlemap
    private GoogleMap mMap;
    private PolylineOptions mPolylineOptions;
    private Marker mMarker;
    double mLat;
    double mLng;
    double lLat;
    double lLng;

    private float light;
    private boolean indoorFlag;

    // View information
    private int oneStepWidth;
    private int oneStepHeight;

    private int mDisplayWidth;
    private int mDisplayHeight;
    private int startWidth;
    private int startHeight;

    private int tempCount = 0;

    private final int lightValue = 1000;

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mClsSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLat = 37.055555;
        mLng = 126.89999;
        lLat = 37.055555;
        lLng = 126.89999;
        pocketFlag = true;
        handHeldFlag = true;
        handTypingFlag = true;
        isPocket = false;
        stepCount = 0;
        compassCount = 0;

        //Using the Sensors
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        //Using the Accelometer
        mAccelometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        //Using the Gyroscoper
        mGgyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        //Using the DirSensor
        mDirSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

        //Using the lightsensor
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        //Using the Closesensor
        mClsSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mClsLis = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float[] v = event.values;
                distance = v[0];
                //Log.e("DISTANCE", String.valueOf(distance));

                if (distance < 5.0) {
                    startTime = 0;
                    isPocket = true;
                } else {
                    isPocket = false;
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };


        mPolylineOptions = new PolylineOptions();

        stepCount = 0;

        setContentView(R.layout.activity_maps);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mTextView = (TextView) findViewById(R.id.tv);

        DisplayMetrics dm = getApplicationContext().getResources().getDisplayMetrics();

        mDisplayWidth = dm.widthPixels;
        mDisplayHeight = dm.heightPixels;

        startWidth = mDisplayWidth / 2;
        startHeight = mDisplayHeight / 2 - 36;

        oneStepWidth = mDisplayWidth / 27;
        oneStepHeight = mDisplayHeight / 67;

        indoorFlag = false;

        final LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, permission.ACCESS_COARSE_LOCATION)
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

        mDirLis = new mDirectionListener();
        mAccLis = new AccelometerListener();
        mGyroLis = new GyroscopeListener();
        mLightLis = new mLightListener();
        mSensorManager
                .registerListener(mAccLis, mAccelometerSensor, SensorManager.SENSOR_DELAY_UI);
        mSensorManager
                .registerListener(mGyroLis, mGgyroSensor, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager
                .registerListener(mClsLis, mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY),
                        SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(mDirLis, mDirSensor, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(mLightLis, mLightSensor, SensorManager.SENSOR_DELAY_FASTEST);

    }

    @Override
    public void onPause() {
        super.onPause();
        Log.e("LOG", "onPause()");
        mSensorManager.unregisterListener(mAccLis);
        mSensorManager.unregisterListener(mClsLis);
        mSensorManager.unregisterListener(mDirLis);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("LOG", "onDestroy()");
        mSensorManager.unregisterListener(mAccLis);
        mSensorManager.unregisterListener(mClsLis);
        mSensorManager.unregisterListener(mDirLis);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private class AccelometerListener implements SensorEventListener {

        Point point;
        LatLng latlng;

        @Override
        public void onSensorChanged(SensorEvent event) {

            if (indoorFlag) {

                if (startTime == 0) {
                    startTime = event.timestamp;
                } else {
                    endTime = event.timestamp;
                }

                if (endTime - startTime > 1700000000) {
                    isPocketToHand = true;
                } else {
                    isPocketToHand = false;
                }
                double accX = event.values[0];
                double accY = event.values[1];
                double accZ = event.values[2];

                double tmp = (accX * accX) + (accY * accY) + (accZ * accZ);
                final double E = Math.sqrt(tmp);

                if (stepCount == 0) {
                    point = new Point(startWidth, startHeight);
                    latlng = new LatLng(lLat, lLng);

                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 21));

                    //TODO : ZOOM 버튼 만들고 버튼으로 줌 조절
                }

                /** In the pocket **/
                if (isPocket) {
                    if (E > minPocketThs && E < maxPocketThs && pocketFlag && isPocketToHand) {
                        stepCount++;
                        pocketFlag = false;

                        Handler mHandler = new Handler();
                        mHandler.postDelayed(new Runnable() {
                            public void run() {
                                pocketFlag = true;
                            }
                        }, 400);
                    }

                    /** Calling **/
                    else if (E > minCallingThs && E < maxCallingThs && callingFlag
                            && isPocketToHand) {
                        stepCount++;
                        callingFlag = false;

                        Handler mHandler = new Handler();
                        mHandler.postDelayed(new Runnable() {
                            public void run() {
                                callingFlag = true;
                            }
                        }, 400);
                    }
                } else {
                    /** Walking with typing **/
                    if (E > minTypingThs && E < maxTypingThs && isHandTyping && handTypingFlag
                            && isPocketToHand) {
                        stepCount++;
                        isHandTyping = false;
                        handTypingFlag = false;

                        Handler mHandler = new Handler();
                        mHandler.postDelayed(new Runnable() {
                            public void run() {
                                handTypingFlag = true;
                            }
                        }, 400);
                    }
                    /** Hand held working **/
                    else if (E > minHeldThs && E < maxHeldThs && isHandHeld && handHeldFlag) {
                        stepCount++;

                        isHandHeld = false;
                        handHeldFlag = false;

                        Handler mHandler = new Handler();
                        mHandler.postDelayed(new Runnable() {
                            public void run() {
                                handHeldFlag = true;
                            }
                        }, 400);
                    }
                }


                if (stepCount == tempCount) {
                    if (mMarker != null) {
                        mMarker.remove();
                    }


                    if (tempCount > 0) {
                        latlng = mMap.getProjection().fromScreenLocation(point);
                    }

                    /** 걸었을 때 방위에 맞춰서 계산 **/
                    if (stepCount > 1) {
                        point.y = startHeight;

                        double widthAngle = Math.sin(Math.toRadians(compassValue));
                        double heightAngle = Math.cos(Math.toRadians(compassValue));

                        double doubleWidth = startWidth + oneStepWidth * widthAngle;
                        double doubleHeight = startHeight - oneStepHeight * heightAngle;

                        point.x = (int) doubleWidth;
                        point.y = (int) doubleHeight;
                    }

                    Log.e("위경도 : ",
                            "위도 : " + String.valueOf(latlng.latitude) + " 경도 : " + String
                                    .valueOf(latlng.longitude));
                    Log.e("포인트 : ",
                            "X : " + String.valueOf(point.x) + " Y : " + String.valueOf(point.y));
                    Log.e("Step ", String.valueOf(stepCount));
                    // TODO : 화면에 몇 걸음 걸었고 몇 미터 걸었는지

                    mTextView.setText(String.valueOf(stepCount));

                    mMarker = mMap.addMarker(
                            new MarkerOptions().position(latlng).title("current location"));
                    mMap.addPolyline(mPolylineOptions.add(latlng).color(Color.RED).width(5));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 21));
                    tempCount++;
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }

    private class GyroscopeListener implements SensorEventListener {

        @Override
        public void onSensorChanged(SensorEvent event) {

      /* receives the angular velocity of each axis. */
            double gyroX = event.values[0];
            double gyroY = event.values[1];
            double gyroZ = event.values[2];

      /* detect gyroZ motion when walking with hand */
            if (Math.abs(gyroZ) > handHeldZThs) {
                isHandHeld = true;
            }

      /* if gyroX moves a lot, it is not time to walking with hand */
            if (Math.abs(gyroX) > handHeldXThs) {
                isHandHeld = false;
            }

      /* detect few motion when walking while typing */
            if (Math.abs(gyroX) < handTypingThs && Math.abs(gyroY) < handTypingThs
                    && Math.abs(gyroZ) < handTypingThs) {
                isHandTyping = true;
            } else {
                isHandTyping = false;
            }

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }

    private class mDirectionListener implements SensorEventListener {

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
                compassValue = (int) event.values[0];
                //Log.e("compassValue : ", String.valueOf(compassValue));

            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    }

    private class mLightListener implements SensorEventListener{

        @Override
        public void onSensorChanged(SensorEvent event) {
            float[] v = event.values;
            if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
                light = v[0];
                if (light < lightValue) {
                    indoorFlag = true;
                } else {
                    indoorFlag = false;
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }

    private final LocationListener mLocationListener = new LocationListener() {


        @Override
        public void onLocationChanged(Location location) {
            if (!indoorFlag) {

                mLat = location.getLatitude();
                mLng = location.getLongitude();
                Log.e("test", "outdoors");
                lLat = mLat;
                lLng = mLng;
                if (mMarker != null) {
                    mMarker.remove();
                }

                LatLng latlng = new LatLng(mLat, mLng);

                mMarker = mMap.addMarker(new MarkerOptions().position(latlng).title("current location"));

                mMap.addPolyline(mPolylineOptions.add(latlng).color(Color.RED).width(5));

                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 21));

            }else{
                Log.e("test", "indoors");
//                mMap.setMyLocationEnabled(false);
//                mMap.getUiSettings().setZoomGesturesEnabled(false);
//                mMap.getUiSettings().setScrollGesturesEnabled(false);
//                mMap.getUiSettings().setRotateGesturesEnabled(false);
//                mMap.getUiSettings().setCompassEnabled(true);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setZoomGesturesEnabled(false);
        mMap.getUiSettings().setScrollGesturesEnabled(false);
        mMap.getUiSettings().setRotateGesturesEnabled(false);
        mMap.getUiSettings().setCompassEnabled(true);

        // Add a marker in Sydney, Australia, and move the camera.
   /* LatLng sydney = new LatLng(mLat, mLng);
    mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
    mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));*/
    }


}

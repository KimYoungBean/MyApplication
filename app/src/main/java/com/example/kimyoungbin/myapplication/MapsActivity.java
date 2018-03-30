package com.example.kimyoungbin.myapplication;

import android.Manifest.permission;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by kimyoungbin on 2018. 2. 26..
 */

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, SensorEventListener {
    private TextView mTextView;
    int cnt;
    private double[] averval = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private double average;
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

    private int height;

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

    private long mStart;
    private long mEnd;

    // To use googlemap
    private GoogleMap mMap;
    private PolylineOptions mPolylineOptions;
    private Marker mMarker;
    double mLat;
    double mLng;
    double lLat;
    double lLng;
    private boolean drawFlag;

    private Button mButton;

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


    /* sun */
    TextView sunset, sunrise;

    int Day;
    int Year;
    int Month;
    double rh,sh,rm,sm,rs,ss;
    int Hour, Minute, Second;

    private static final double PI = 3.141592;

    private boolean IsLeapYear(int year){
        return ((year%4==0 && year%100!=0) || year % 400==0);
    }

    private int GetLastDay(int uiYear, int ucMonth){
        switch (ucMonth){
            case 2:
                if((uiYear%4)==0){
                    if(uiYear%100==0){
                        if(uiYear%400==0) return 29;
                        return 28;
                    }
                    return 29;
                }
                return 28;
            case 4:
            case 6:
            case 9:
            case 11:
                return 30;
        }
        return 31;
    }

    private int CalcJulianDay(int uiYear, int ucMonth, int ucDay){
        int i;
        int iJulDay=0;
        for(i=1; i<ucMonth; i++){
            iJulDay += GetLastDay(uiYear, i);
        }
        iJulDay += ucDay;

        return iJulDay;
    }

    private double CalcGamma(int iJulDay){
        return (2.0 * PI/365.0)*(iJulDay-1);
    }

    private double CalcGamma2(int iJulDay, int hour){
        return (2.0 * PI/365.0)*(iJulDay-1+(hour/24.0));
    }

    private double CalcEqofTime(double gamma){
        return (229.18 *(0.000075 + 0.001868 * Math.cos(gamma)-0.032077*Math.sin(gamma)-0.014615*Math.cos(2*gamma)-0.040849*Math.sin(2*gamma)));
    }

    private double CalcSolarDec(double gamma){
        return (0.006918 - 0.399912 * Math.cos(gamma)+0.070257*Math.sin(gamma)- 0.006758*Math.cos(2*gamma)+0.000907*Math.sin(2*gamma));
    }

    private double DegreeToRadian(double angleDeg){
        return (PI*angleDeg/180.0);
    }

    private double RadianToDegree(double angleRad){
        return (180*angleRad/PI);
    }

    private double CalcHourAngle(double latitude, double solarDec, int time){
        double latRad = DegreeToRadian(latitude);
        double hour_angle = Math.acos(Math.cos(DegreeToRadian(90.833))/(Math.cos(latRad)*Math.cos(solarDec)) - Math.tan(latRad)*Math.tan(solarDec));

        if(time==1){
            return hour_angle;
        }else if(time==0){
            return -hour_angle;
        }
        return 0;
    }

    private double CalcSunriseGMT(int iJulDay, double latitude, double longitude){
        double gamma = CalcGamma(iJulDay);
        double eqTime = CalcEqofTime(gamma);
        double solarDec = CalcSolarDec(gamma);
        double hourAngle = CalcHourAngle(latitude, solarDec, 1);
        double delta = longitude - RadianToDegree(hourAngle);
        double timeDiff = 4.0*delta;
        double timeGMT = 720.0+timeDiff - eqTime;
        double gamma_sunrise = CalcGamma2(iJulDay, (int)(timeGMT/60.0));
        eqTime = CalcEqofTime(gamma_sunrise);
        solarDec = CalcSolarDec(gamma_sunrise);
        hourAngle = CalcHourAngle(latitude, solarDec, 1);
        delta = longitude-RadianToDegree(hourAngle);
        timeDiff = 4.0*delta;
        timeGMT = 720.0+timeDiff-eqTime;

        return timeGMT;
    }

    private double CalcSunsetGMT(int iJulDay, double latitude, double longitude){
        double gamma = CalcGamma(iJulDay+1);
        double eqTime = CalcEqofTime(gamma);
        double solarDec = CalcSolarDec(gamma);
        double hourAngle = CalcHourAngle(latitude, solarDec, 0);
        double delta = longitude-RadianToDegree(hourAngle);
        double timeDiff = 4.0*delta;
        double setTimeGMT = 720.0+timeDiff-eqTime;

        double gamma_sunset = CalcGamma2(iJulDay, (int)(setTimeGMT/60.0));
        eqTime = CalcEqofTime(gamma_sunset);
        solarDec = CalcSolarDec(gamma_sunset);
        hourAngle = CalcHourAngle(latitude, solarDec, 0);
        delta = longitude - RadianToDegree(hourAngle);
        timeDiff = 4.0*delta;
        setTimeGMT = 720.0+timeDiff-eqTime;
        return setTimeGMT;
    }

    private void GetTimeString1(double minutes){
        double floatHour = minutes/60.0;
        double hour = Math.floor(floatHour);
        double floatMinute = 60.0*(floatHour-Math.floor(floatHour));
        double minute = Math.floor(floatMinute);
        double floatSec = 60.0*(floatMinute - Math.floor(floatMinute));
        double second = Math.floor(floatSec);

        rh = hour;
        rm = minute;
        rs = second;
        Log.v(""," "+hour+"시 "+minute+"분 "+second+"초");
    }
    private void GetTimeString2(double minutes){
        double floatHour = minutes/60.0;
        double hour = Math.floor(floatHour);
        double floatMinute = 60.0*(floatHour-Math.floor(floatHour));
        double minute = Math.floor(floatMinute);
        double floatSec = 60.0*(floatMinute - Math.floor(floatMinute));
        double second = Math.floor(floatSec);

        sh = hour;
        sm = minute;
        ss = second;
        Log.v(""," "+hour+"시 "+minute+"분 "+second+"초");


    }

    public double GetSunriseTime(int year, int month, int day, double latitude, double longitude, int zone, int daySavings){
        int julday = CalcJulianDay(year, month, day);
        double timeLST = CalcSunriseGMT(julday, latitude, longitude)-(60.0*zone)+daySavings;
        return timeLST;
    }

    public double GetSunsetTime(int year, int month, int day, double latitude, double longitude, int zone, int daySavings){
        int julday = CalcJulianDay(year, month, day);
        double timeLST = CalcSunsetGMT(julday, latitude, longitude)-(60.0*zone)+daySavings;
        return timeLST;
    }

    public void sunsettest(){
        double latitude, longitude, lst;
        latitude = mLat;
        longitude = mLng;
        lst = GetSunsetTime(Year, Month, Day, latitude, longitude, -9,0);
        GetTimeString2(lst);
    }

    public void sunrisetest(){
        double latitude, longitude, lst;
        latitude = mLat;
        longitude = mLng;
        lst = GetSunriseTime(Year, Month, Day, latitude, longitude, -9,0);
        GetTimeString1(lst);
    }


    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mClsSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        height = 0;
        cnt = 0;
        average = 0;

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
        drawFlag = false;


        // Date & time
        Calendar c= Calendar.getInstance();

        long now = System.currentTimeMillis();
        Date date = new Date(now);

        SimpleDateFormat dataFormat= new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
        SimpleDateFormat CurYearFormat = new SimpleDateFormat("yyyy");
        SimpleDateFormat CurMonthFormat = new SimpleDateFormat("MM");
        SimpleDateFormat CurDayFormat = new SimpleDateFormat("dd");
        SimpleDateFormat CurHourFormat = new SimpleDateFormat("HH");
        SimpleDateFormat CurMinuteFormat = new SimpleDateFormat("mm");

        String strCurYear = CurYearFormat.format(date);
        String strCurMonth = CurMonthFormat.format(date);
        String strCurDay = CurDayFormat.format(date);
        String strCurHour = CurHourFormat.format(date);
        String strCurMinute = CurMinuteFormat.format(date);

        Year = Integer.valueOf(strCurYear);
        Month = Integer.valueOf(strCurMonth);
        Day = Integer.valueOf(strCurDay);
        Hour = Integer.valueOf(strCurHour);
        Minute = Integer.valueOf(strCurMinute);

//        String dateTime = dataFormat.format(c.getTime());
//        String date[] = dateTime.split(".");
//        Year = Integer.parseInt(date[0]);
//        Month = Integer.parseInt(date[1]);
//        Day = Integer.parseInt(date[2]);
//        Hour = Integer.parseInt(date[3]);
//        Minute = Integer.parseInt(date[4]);
//        Second = Integer.parseInt(date[5]);


        sunrisetest();
        sunsettest();
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
        mButton = (Button) findViewById(R.id.btn_draw_gps);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawFlag = true;
            }
        });

        Intent intent = getIntent();
        String data = intent.getExtras().getString("height");
        height = Integer.parseInt(data);

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
                    mTextView.setText("Step : " + String.valueOf(stepCount) + "\nMeter : " + String.format("%.2f", stepCount * (height - 110) * 0.01));


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

    private class mLightListener implements SensorEventListener {

        @Override
        public void onSensorChanged(SensorEvent event) {
            float[] v = event.values;
            if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
                light = v[0];
                //day
                if(rh<Hour&&Hour<sh){
                    if (light < lightValue) {
                        indoorFlag = true;
                    } else {
                        indoorFlag = false;
                    }
                }else if(rh==Hour){
                    if(Minute>=rm){
                        if (light < lightValue) {
                            indoorFlag = true;
                        } else {
                            indoorFlag = false;
                        }
                    }else{
                        if (light < 50) {
                            indoorFlag = false;
                        } else {
                            indoorFlag = true;
                        }
                    }
                }else if(Hour==sh){
                    if(Minute<=sm){
                        if (light < lightValue) {
                            indoorFlag = true;
                        } else {
                            indoorFlag = false;
                        }
                    }
                }else{
                    //(Hour>sh||Hour<rh)
                    //night
                    if (light < 50) {
                        indoorFlag = false;
                    } else {
                        indoorFlag = true;
                    }
                }
//                if (light < lightValue) {
//                    indoorFlag = true;
//                } else {
//                    indoorFlag = false;
//                }
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
                if (drawFlag) {
                    mMap.addPolyline(mPolylineOptions.add(latlng).color(Color.RED).width(5));
                }
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 21));

            } else {
                Log.e("test", "indoors");


//                mMap.setMyLocationEnabled(true);
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
//        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(false);
        mMap.getUiSettings().setTiltGesturesEnabled(false);
        mMap.getUiSettings().setScrollGesturesEnabled(false);
        mMap.getUiSettings().setRotateGesturesEnabled(false);
        mMap.getUiSettings().setCompassEnabled(false);

        // Add a marker in Sydney, Australia, and move the camera.
   /* LatLng sydney = new LatLng(mLat, mLng);
    mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
    mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));*/
    }


}


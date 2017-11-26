package com.mra.temanid;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.mra.temanid.data.StaticConfig;
import com.mra.temanid.service.ServiceUtils;
import com.mra.temanid.service.TemanClientServer;

import com.mra.temanid.ui.ChatActivity;
import com.mra.temanid.ui.FriendsFragment;
import com.mra.temanid.ui.GroupFragment;
import com.mra.temanid.ui.LoginActivity;
import com.mra.temanid.ui.MapsFragment;
import com.mra.temanid.ui.UserProfileFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static String TAG = "MainActivity";
    private ViewPager viewPager;
    private TabLayout tabLayout = null;
    public static String STR_FRIEND_FRAGMENT = "FRIEND";
    public static String STR_GROUP_FRAGMENT = "GROUP";
    public static String STR_INFO_FRAGMENT = "INFO";
    public static String STR_MAP_FRAGMENT = "MAP";
    private RelativeLayout lMaps;
    private FloatingActionButton floatButton;
    private ViewPagerAdapter adapter;
    private List<LatLng> mPoints = new ArrayList<>();
    private List<Marker> mMarkers = new ArrayList<>();
    List<String> s = new ArrayList<>();
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseUser user;
    ImageButton filter;
    ProgressDialog pdialog;

    private GoogleMap mMap;
    private Location mLastLocation;
    private SeekBar mRadiusSeekBar;
    private SeekBar seekBar;
    private Map mapView;
    private Integer zoomLevel;

    private Circle mCircle;
    private double mCircleRadius = 250;
    private LatLng mCircleCenter = new LatLng(38.432398, 27.155882);

    GoogleApiClient mGoogleApiClient;

    public static final int MULTIPLE_PERMISSIONS = 10;
    String[] permissions = new String[]
            {
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.READ_SMS
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();

        //sjhjshshsj
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        pdialog = new ProgressDialog(this);
        pdialog.setMessage("Get Data");
        pdialog.setCancelable(false);
        mRadiusSeekBar = (SeekBar) findViewById(R.id.seek_map);
        mRadiusSeekBar.setEnabled(false);

        mRadiusSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mCircleRadius=progress * 1000;
                filterMarkers(progress * 1000);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });



        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if(toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setTitle("TEMAN.ID");
        }

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        lMaps = (RelativeLayout) findViewById(R.id.lMaps);
        lMaps.setVisibility(View.VISIBLE);


        floatButton = (FloatingActionButton) findViewById(R.id.fab);

        floatButton.setVisibility(View.GONE);
        initTab();
        initFirebase();
        filter = (ImageButton) findViewById(R.id.button_1) ;
        filter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pdialog.show();
                RequestParams param = new RequestParams();
                param.put("lon",mCircleCenter.longitude);
                param.put("radius",mCircleRadius);
                param.put("lat",mCircleCenter.latitude);
                TemanClientServer.post("",param, new JsonHttpResponseHandler(){
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                        pdialog.hide();
                        mMap.clear();

                        for(int i = 0; i < response.length(); i++){
                            try {
                                JSONObject obj = response.getJSONObject(i);
                                JSONObject prop = obj.getJSONObject("properties");
                                JSONObject geo = obj.getJSONObject("geometry");
                                JSONArray coor = geo.getJSONArray("coordinates");
                                Log.d("POS", coor.getString(0) + "-" +coor.getDouble(1));
                                LatLng mPoint1 = new LatLng(coor.getDouble(0), coor.getDouble(1));
                                s.add(prop.getString("name"));
                                mPoints.add(mPoint1);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {

                            @Override
                            public void onInfoWindowClick(Marker arg0) {
                                AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
                                builder1.setMessage("Apakah anda ingin join group chat wifi.id ini ?");
                                builder1.setCancelable(true);

                                builder1.setPositiveButton(
                                        "Yes",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                startActivity(new Intent(MainActivity.this, ChatActivity.class));
                                                //dialog.cancel();
                                            }
                                        });

                                builder1.setNegativeButton(
                                        "No",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                dialog.cancel();
                                            }
                                        });

                                AlertDialog alert11 = builder1.create();
                                alert11.show();
                            }
                        });
                        BitmapDescriptor bd = BitmapDescriptorFactory
                                .defaultMarker(BitmapDescriptorFactory.HUE_VIOLET);

                        for(int i = 0; i<mPoints.size(); i++){
                            Log.d("POS", mPoints.get(i).latitude + "-" +mPoints.get(i).longitude);

                            Marker marker = mMap.addMarker(
                                    new MarkerOptions()
                                            .title(s.get(i))
                                            .visible(false)
                                            .position(mPoints.get(i)).icon(BitmapDescriptorFactory.fromResource(R.drawable.spotid)));

                            mMarkers.add(marker);
                        }
                        //addMarkers();
                        addCircle();
                        mRadiusSeekBar.setEnabled(true);
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                        pdialog.hide();
                        System.out.println(responseString.toString());
                    }
                });
            }
        });
    }

    private void initFirebase() {
        //Khoi tao thanh phan de dang nhap, dang ky
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    StaticConfig.UID = user.getUid();
                } else {
                    MainActivity.this.finish();
                    // User is signed in
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
                // ...
            }
        };
    }


    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
        ServiceUtils.stopServiceFriendChat(getApplicationContext(), false);
        if (checkPermission()) {
            Log.e("Permission: ", "Granted");
        } else {
            Log.e("Permission: ", "DENIED");
        }

        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    protected void onDestroy() {
        ServiceUtils.startServiceFriendChat(getApplicationContext());
        super.onDestroy();
    }

    /**
     * Khoi tao 3 tab
     */
    private void initTab() {
        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setSelectedTabIndicatorColor(getResources().getColor(R.color.colorIndivateTab));
        setupViewPager(viewPager);
        tabLayout.setupWithViewPager(viewPager);
        setupTabIcons();
    }


    private void setupTabIcons() {
        int[] tabIcons = {
                R.drawable.ic_tab_map,
                R.drawable.ic_tab_person,
                R.drawable.ic_tab_group,
                R.drawable.ic_tab_infor
        };

        tabLayout.getTabAt(0).setIcon(tabIcons[0]);
        tabLayout.getTabAt(1).setIcon(tabIcons[1]);
        tabLayout.getTabAt(2).setIcon(tabIcons[2]);
        tabLayout.getTabAt(3).setIcon(tabIcons[3]);
    }

    private void setupViewPager(ViewPager viewPager) {
        adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFrag(new MapsFragment(), STR_MAP_FRAGMENT);
        adapter.addFrag(new FriendsFragment(), STR_FRIEND_FRAGMENT);
        adapter.addFrag(new GroupFragment(), STR_GROUP_FRAGMENT);
        adapter.addFrag(new UserProfileFragment(), STR_INFO_FRAGMENT);
        floatButton.setOnClickListener(((FriendsFragment) adapter.getItem(1)).onClickFloatButton.getInstance(this));
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(3);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                ServiceUtils.stopServiceFriendChat(MainActivity.this.getApplicationContext(), false);
                if (adapter.getItem(position) instanceof FriendsFragment) {
                    floatButton.setVisibility(View.VISIBLE);
                    floatButton.setOnClickListener(((FriendsFragment) adapter.getItem(position)).onClickFloatButton.getInstance(MainActivity.this));
                    floatButton.setImageResource(R.drawable.plus);

                    lMaps.setVisibility(View.GONE);
                } else if (adapter.getItem(position) instanceof GroupFragment) {
                    floatButton.setVisibility(View.VISIBLE);
                    floatButton.setOnClickListener(((GroupFragment) adapter.getItem(position)).onClickFloatButton.getInstance(MainActivity.this));
                    floatButton.setImageResource(R.drawable.ic_float_add_group);

                    lMaps.setVisibility(View.GONE);
                } else if(position == 0){

                    floatButton.setVisibility(View.GONE);
                    lMaps.setVisibility(View.VISIBLE);
                }else {
                    floatButton.setVisibility(View.GONE);
                    lMaps.setVisibility(View.GONE);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.about) {
            Toast.makeText(this, "Where version 1.0 by MRA Developer", Toast.LENGTH_LONG).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }




    /**
     * Adapter hien thi tab
     */
    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFrag(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {

            // return null to display only the icon
            return null;
        }
    }

    //MAPS BISMILAH
    private boolean checkPermission() {

        int result;

        List<String> ListPermission = new ArrayList<>();
        for (String p : permissions) {
            result = ContextCompat.checkSelfPermission(getApplicationContext(), p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                ListPermission.add(p);
            }
        }

        if (!ListPermission.isEmpty()) {
            ActivityCompat.requestPermissions(this, ListPermission.toArray(
                    new String[ListPermission.size()]), MULTIPLE_PERMISSIONS);
            return false;
        }

        return false;
    }

    //INI TIDAK PERLU DIUBAH JIKA MAU TAMBAH LAGI RUN-TIME PERMISSIONS
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MULTIPLE_PERMISSIONS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e("Permissions: ", "Granted");
                } else {
                    Log.e("Permissions: ", "DENIED");
                }
            }
        }
    }

    // VOID DARI ALT+ENTER UNTUK mMAP Google Maps
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LocationManager locationManager = (LocationManager) getSystemService(MainActivity.this.LOCATION_SERVICE);
        Criteria criteria = new Criteria();

        Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
        if (location != null)
        {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 13));

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(location.getLatitude(), location.getLongitude()))      // Sets the center of the map to location user
                    .zoom(17)                   // Sets the zoom
                    .bearing(90)                // Sets the orientation of the camera to east
                    .tilt(40)                   // Sets the tilt of the camera to 30 degrees
                    .build();                   // Creates a CameraPosition from the builder
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 12));
        }
        mMap.setOnMyLocationChangeListener(myLocationChangeListener);



        addMarkers();
        addCircle();


        //UNTUK PENAMBAHAN TEMPAT SECARA TEMBAK

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);

        mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();

    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        //KODINGAN YG AWAL
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null)
        {
            LatLng latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());

            //FUNGSI UNTUK MENAMBAHKAN MARKER PADA MAPS

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12));
        }

    }


    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onResume()
    {
        super.onResume();
        mGoogleApiClient.connect();
    }


    //KODINGAN BARU
    private void addCircle(){

        mCircle = mMap.addCircle(new CircleOptions()
                .strokeWidth(4)
                .radius(mCircleRadius)
                .center(mCircleCenter)
                .strokeColor(Color.parseColor("#D1C4E9"))
                .fillColor(Color.parseColor("#657C4DFF")));
    }


    private GoogleMap.OnMyLocationChangeListener myLocationChangeListener = new GoogleMap.OnMyLocationChangeListener() {
        @Override
        public void onMyLocationChange(Location location) {
            LatLng loc = new LatLng(location.getLatitude(), location.getLongitude());
            //mMarker = mMap.addMarker(new MarkerOptions().position(loc));
            if(mMap != null){
                //mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 12));
                mCircleCenter =  loc;
                //mMap.clear();
                //addCircle();
            }
        }
    };
    private void addMarkers(){
        //mPoints = new ArrayList<>();
        //mMarkers = new ArrayList<>();
        LatLng mPoint1 = new LatLng(38.440925, 27.153672);
        s.add("asd");
        mPoints.add(mPoint1);

        BitmapDescriptor bd = BitmapDescriptorFactory
                .defaultMarker(BitmapDescriptorFactory.HUE_VIOLET);

        for(int i = 0; i<mPoints.size(); i++){
            Marker marker = mMap.addMarker(
                    new MarkerOptions()
                            .title(s.get(i))
                            .visible(false)
                            .position(mPoints.get(i)).icon(BitmapDescriptorFactory.fromResource(R.drawable.spotid)));

            mMarkers.add(marker);
        }
    }

    private void filterMarkers(double radiusForCircle){
        mCircle.setRadius(radiusForCircle);

        float[] distance = new float[2];
        for(int m = 0; m < mMarkers.size(); m++){
            Marker marker = mMarkers.get(m);
            LatLng position = marker.getPosition();
            double lat = position.latitude;
            double lon = position.longitude;

            Location.distanceBetween(lat, lon, mCircleCenter.latitude,
                    mCircleCenter.longitude, distance);

            boolean inCircle = distance[0] <= radiusForCircle;
            marker.setVisible(inCircle);
        }
    }


}


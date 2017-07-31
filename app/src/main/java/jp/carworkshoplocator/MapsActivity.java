package jp.carworkshoplocator;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    //Location
    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    LocationRequest mLocationRequest;
    LatLng latLng;
    final HashMap<String, Marker> markers = new HashMap<>();
    LatLng location;
    long radiusmeasurement = 5000;

    //Firebase
    private DatabaseReference mRootRef;

    //Filter
    String filter = "none";
    String filenamehldr;
    String storeowner,storeservice;

    //Widgets
    LinearLayout seekbarlayout;
    SeekBar radiusbar;
    TextView seekBarValue;
    MaterialDialog dialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        //Radius Custom View
        LayoutInflater factory = LayoutInflater.from(MapsActivity.this);
        final View stdView = factory.inflate(R.layout.radius_slider, null);
        seekbarlayout = (LinearLayout) stdView.findViewById(R.id.seekbarlayout);
        radiusbar = (SeekBar) seekbarlayout.findViewById(R.id.radiusseekbar);
        seekBarValue = (TextView) seekbarlayout.findViewById(R.id.tvRadiusValue);

        seekBarValue.setText("5000");
        radiusbar.setMax(10000);
        radiusbar.setProgress(5000);
        radiusbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                int stepSize = 1000;
                progress = (progress/stepSize)*stepSize;
                seekBar.setProgress(progress);
                seekBarValue.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
            }
        }
        else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }

        //Filter of Services
        final Button btnFilter = (Button) findViewById(R.id.btnFilter);
        btnFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(MapsActivity.this, btnFilter);
                popup.getMenuInflater()
                        .inflate(R.menu.dropdown_menu, popup.getMenu());

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        Toast.makeText(
                                MapsActivity.this,
                                "You Clicked : " + item.getTitle(),
                                Toast.LENGTH_SHORT
                        ).show();
                        filter = (String) item.getTitle();
                        btnFilter.setText(filter);
                        return true;
                    }
                });
                popup.show();
            }
        });

        //Locate Stores
        Button btnLocate = (Button) findViewById(R.id.btnLocate);
        btnLocate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("onClick", "Button is Clicked");

                if(filter.equals("none")){
                    Toast.makeText(MapsActivity.this, "Please choose a Service", Toast.LENGTH_SHORT).show();
                }
                else{
                    RetrieveStores();
                }
            }
        });

        final Button btnRadius = (Button) findViewById(R.id.btnRadius);
        btnRadius.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean wrapInScrollView = true;
                new MaterialDialog.Builder(MapsActivity.this)
                        .title("Adjust Radius")
                        .customView(seekbarlayout, wrapInScrollView)
                        .autoDismiss(false)
                        .negativeText("Cancel")
                        .positiveText("Continue")
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                radiusmeasurement = radiusbar.getProgress();
                                mMap.clear();
                                Circle circle = mMap.addCircle(new CircleOptions()
                                        .center(latLng)
                                        .radius(radiusmeasurement)
                                        .strokeColor(Color.RED));
                                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                                mMap.animateCamera(CameraUpdateFactory.zoomTo(12));
                                RetrieveStores();
                            }
                        })
                        .build()
                        .show();
            }
        });
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {

        mLastLocation = location;
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }

        //Place current location marker
        latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Current Position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        mCurrLocationMarker = mMap.addMarker(markerOptions);

        Circle circle = mMap.addCircle(new CircleOptions()
                .center(latLng)
                .radius(radiusmeasurement)
                .strokeColor(Color.RED));

        //move map camera
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(12));

        //stop location updates
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    public boolean checkLocationPermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted. Do the
                    // contacts-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }

                } else {

                    // Permission denied, Disable the functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other permissions this app might request.
            // You can add here other case statements according to your requirement.
        }
    }

    public void RetrieveStores(){
        mRootRef = FirebaseDatabase.getInstance().getReference().child("Workshops").child(filter);
        mRootRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(final DataSnapshot dataSnapshot, String s) {
                Map<String, String> map = (Map)dataSnapshot.getValue();
                String latitude = map.get("storeLat");
                String longitude = map.get("storeLong");
                final String filename = map.get("name");
                final String storeservice = map.get("storeService");
                location = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));

                final Marker m = mMap.addMarker(new MarkerOptions()
                        .position(location)
                        .title(filename)
                        .snippet("Service: " + storeservice)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                m.setTag(filename);
                markers.put(filename, m);

                mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                    @Override
                    public void onInfoWindowClick(Marker marker) {
                        Object obj = m.getTag();
                        filenamehldr = String.valueOf(obj);

                        Toast.makeText(MapsActivity.this, filenamehldr, Toast.LENGTH_SHORT).show();

                        new Validation().execute();
                        dialog = new MaterialDialog.Builder(MapsActivity.this)
                                .title("Loading Information")
                                .content("Please wait...")
                                .progress(true, 0)
                                .show();

                        Toast.makeText(MapsActivity.this, filter+""+filenamehldr, Toast.LENGTH_SHORT).show();
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            public void run() {
                                dialog.dismiss();
                                new MaterialDialog.Builder(MapsActivity.this)
                                        .titleColorRes(R.color.colorPrimary)
                                        .contentColor(getResources().getColor(R.color.colorPrimary))
                                        .widgetColorRes(R.color.colorPrimary)
                                        .backgroundColorRes(R.color.white)
                                        .positiveColorRes(R.color.colorPrimary)
                                        .title(filenamehldr + " Information")
                                        .content("Store Owner: " + storeowner + "\n" +
                                                "Store Service: " + storeservice)
                                        .autoDismiss(false)
                                        .positiveText("Continue")
                                        .build()
                                        .show();
                            }
                        }, 3000);
                    }
                });

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                Marker m = markers.get(dataSnapshot.getKey());
                m.setPosition(location);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                markers.get(dataSnapshot.getKey()).remove();
                markers.remove(dataSnapshot.getKey());
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    class Validation extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            FirebaseClass firebaseFunctions = new FirebaseClass(MapsActivity.this);
            final DatabaseReference mRootRef = firebaseFunctions.getDatabaseReference();
            mRootRef.child("Workshops").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    mRootRef.child("Workshops")
                            .child(filter)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    mRootRef.child("Workshops")
                                            .child(filter)
                                            .child(filenamehldr)
                                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(DataSnapshot dataSnapshot) {
                                                    final StoreClass store = dataSnapshot.getValue(StoreClass.class);
                                                    storeowner = store.getStoreOwner();
                                                    storeservice = store.getStoreService();
                                                }

                                                @Override
                                                public void onCancelled(DatabaseError databaseError) {

                                                }
                                            });
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
            return null;
        }
    }

}

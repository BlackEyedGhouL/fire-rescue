package com.blackeyedghoul.firefighters;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsetsAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.github.captain_miao.optroundcardview.OptRoundCardView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StationMapFragment extends Fragment implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    View rootView, fragmentMap;
    Context mContext;
    TextView allow, notNow, title, subTitle;
    EditText searchText;
    OptRoundCardView card;
    RelativeLayout search;
    ConstraintLayout layout;
    GoogleMap mMap;
    FloatingActionButton drop, nearby, myLocation, info;
    Animation open, close, from, to;
    private final int GPS_REQUEST_CODE = 9001;
    LatLngBounds sriLanka_boundary;
    private FusedLocationProviderClient mLocationClient;
    private boolean clicked = false;

    public StationMapFragment() {
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_station_map, container, false);

        double bottom_boundary = 5.774112558779486;
        double left_boundary = 79.51377588147243;
        double top_boundary = 10.090428766513725;
        double right_boundary = 82.06510282696583;

        sriLanka_boundary = new LatLngBounds(
                new LatLng(bottom_boundary, left_boundary),
                new LatLng(top_boundary, right_boundary)
        );

        init();

        fragmentMap.setVisibility(View.GONE);
        search.setVisibility(View.GONE);
        drop.setVisibility(View.GONE);
        myLocation.setVisibility(View.GONE);
        nearby.setVisibility(View.GONE);
        info.setVisibility(View.GONE);

        drop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setVisibility(clicked);
                setAnimation(clicked);
                setClickable(clicked);
                clicked = !clicked;
            }
        });

        myLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDeviceLocation();
                if(clicked) {
                    setVisibility(true);
                    setAnimation(clicked);
                    setClickable(clicked);
                    clicked = !clicked;
                }
            }
        });

        myLocation.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(mContext, "My Location", Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        nearby.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });

        nearby.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(mContext, "Nearby Places", Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });

        info.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(mContext, "More Info", Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        allow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkMyPermission();
            }
        });

        notNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });

        checkMyPermission();

        return rootView;
    }

    private void setVisibility(boolean clicked) {
        if (!clicked) {
            myLocation.setVisibility(View.VISIBLE);
            nearby.setVisibility(View.VISIBLE);
            info.setVisibility(View.VISIBLE);
        } else {
            myLocation.setVisibility(View.INVISIBLE);
            nearby.setVisibility(View.INVISIBLE);
            info.setVisibility(View.INVISIBLE);
        }
    }

    private void setAnimation(boolean clicked) {
        if (!clicked) {
            drop.startAnimation(open);
            myLocation.startAnimation(from);
            nearby.startAnimation(from);
            info.startAnimation(from);
        } else {
            drop.startAnimation(close);
            myLocation.startAnimation(to);
            nearby.startAnimation(to);
            info.startAnimation(to);
        }
    }

    private void setClickable(boolean clicked) {
        if (!clicked) {
            myLocation.setClickable(true);
            nearby.setClickable(true);
            info.setClickable(true);
        } else {
            myLocation.setClickable(false);
            nearby.setClickable(false);
            info.setClickable(false);
        }
    }

    private void init_search() {
        searchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || keyEvent.getAction() == KeyEvent.ACTION_DOWN
                        || keyEvent.getAction() == KeyEvent.KEYCODE_ENTER) {

                    geoLocate();
                }

                return false;
            }
        });
    }

    private void getDeviceLocation() {
        mLocationClient = LocationServices.getFusedLocationProviderClient(mContext);

        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        final Task location = mLocationClient.getLastLocation();
        location.addOnCompleteListener(new OnCompleteListener() {
            @Override
            public void onComplete(@NonNull Task task) {
                if (task.isSuccessful()) {
                    Location currentLocation = (Location) task.getResult();
                    LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
                }
                else {
                    Toast.makeText(mContext, "Unable to get current location.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void geoLocate() {
        String searchString = searchText.getText().toString();

        Geocoder geocoder = new Geocoder(mContext);
        List<Address> list = new ArrayList<>();
        try {
            list = geocoder.getFromLocationName(searchString, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (list.size() > 0) {
            Address address = list.get(0);
            Log.d("fms", "geoLocate " + address.toString());

            moveCamera(new LatLng(address.getLatitude(), address.getLongitude()), 15f, address.getAddressLine(0));

        }
    }

    private void moveCamera(LatLng latLng, float zoom, String title) {
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        MarkerOptions options = new MarkerOptions()
                .position(latLng)
                .title(title);
        mMap.addMarker(options);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        //mMap.setLatLngBoundsForCameraTarget(sriLanka_boundary);
        mMap.setMinZoomPreference(7.7f);
        LatLng latLng = new LatLng(7.806167305163772, 80.68711340720736);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 7);
        mMap.moveCamera(cameraUpdate);

        init_search();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private void init() {
        fragmentMap = rootView.findViewById(R.id.fsm_s_map_);
        allow = rootView.findViewById(R.id.fsm_s_allow_access);
        notNow = rootView.findViewById(R.id.fsm_s_allow_access_notNow);
        card = rootView.findViewById(R.id.fsm_s_card);
        title = rootView.findViewById(R.id.fsm_s_title);
        subTitle = rootView.findViewById(R.id.fsm_s_subTitle);
        layout = rootView.findViewById(R.id.fsm_s_layout);
        search = rootView.findViewById(R.id.fsm_s_search_layout);
        searchText = rootView.findViewById(R.id.fsm_s_search_editText);
        myLocation = rootView.findViewById(R.id.fsm_s_floatingActionButton_my_location);
        nearby = rootView.findViewById(R.id.fsm_s_floatingActionButton_nearby);
        drop = rootView.findViewById(R.id.fsm_s_floatingActionButton_drop);
        info = rootView.findViewById(R.id.fsm_s_floatingActionButton_info);
        open = AnimationUtils.loadAnimation(mContext, R.anim.rotate_open_anim);
        close = AnimationUtils.loadAnimation(mContext, R.anim.rotate_close_anim);
        from = AnimationUtils.loadAnimation(mContext, R.anim.from_button_anim);
        to = AnimationUtils.loadAnimation(mContext, R.anim.to_button_anim);
    }

    private void checkMyPermission() {

        Dexter.withContext(mContext).withPermission(Manifest.permission.ACCESS_FINE_LOCATION).withListener(new PermissionListener() {
            @SuppressLint("VisibleForTests")
            @Override
            public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                card.setVisibility(View.GONE);
                title.setVisibility(View.GONE);
                subTitle.setVisibility(View.GONE);
                allow.setVisibility(View.GONE);
                notNow.setVisibility(View.GONE);
                layout.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                fragmentMap.setVisibility(View.VISIBLE);
                search.setVisibility(View.VISIBLE);
                drop.setVisibility(View.VISIBLE);
                myLocation.setVisibility(View.INVISIBLE);
                nearby.setVisibility(View.INVISIBLE);
                info.setVisibility(View.INVISIBLE);
                initMap();
                mLocationClient = new FusedLocationProviderClient(mContext);
            }

            @Override
            public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", mContext.getPackageName(), "");
                intent.setData(uri);
                startActivity(intent);
            }

            @Override
            public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                permissionToken.continuePermissionRequest();
            }
        }).check();
    }

    private void initMap() {
        if (isGPSEnable()) {
            SupportMapFragment mapFragment = (SupportMapFragment) this.getChildFragmentManager()
                    .findFragmentById(R.id.fsm_s_map_);

            assert mapFragment != null;
            mapFragment.getMapAsync(StationMapFragment.this);
        }
    }

    private boolean isGPSEnable() {
        LocationManager locationManager = (LocationManager) getActivity().getSystemService(mContext.LOCATION_SERVICE);

        boolean providerEnable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (providerEnable) {
            return true;
        } else {
            AlertDialog alertDialog = new AlertDialog.Builder(mContext,
                    R.style.AlertDialog)
                    .setTitle("GPS Required")
                    .setMessage("Enable GPS otherwise location tracking won't work.")
                    .setPositiveButton("Settings", ((dialogInterface, i) -> {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(intent, GPS_REQUEST_CODE);
                    }))
                    .setNegativeButton("Cancel", ((dialogInterface, i) -> {
                        Intent intent = new Intent(mContext, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    }))
                    .setCancelable(false)
                    .show();
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GPS_REQUEST_CODE) {
            LocationManager locationManager = (LocationManager) getActivity().getSystemService(mContext.LOCATION_SERVICE);

            boolean providerEnable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            if (providerEnable) {
                Fragment frag = new StationMapFragment();
                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                fragmentManager.beginTransaction().replace(R.id.fsm_s_layout, frag).commit();
            } else {
                Intent intent = new Intent(mContext, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        }
    }
}
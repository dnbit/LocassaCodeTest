package com.dnbitstudio.locassacodetest;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import com.dnbitstudio.locassacodetest.bus.BusProvider;
import com.dnbitstudio.locassacodetest.model.Condition;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import android.content.IntentSender;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.Bind;
import butterknife.ButterKnife;
import retrofit.Response;

public class LaunchActivity extends FragmentActivity implements LocationListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
{
    public static final String TAG = LaunchActivity.class.getSimpleName();

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private final static int IMPOSIBLE_TEMPERATURE = -1000;
    public static final String CAMERA_POSITION = "camera_position";
    public static final String MARKER_LAT_LONG = "marker_lat_long";
    public static final String FORECAST_TV = "forecast_tv";
    public static final String LAST_TEMP = "last_temp";

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private MarkerOptions mMarkerOptions;
    private LatLng mMarkerLatLng;
    private Bus bus;
    public boolean restored = false;
    private int mLastTemp = IMPOSIBLE_TEMPERATURE;

    // Butterknife bindings
    @Bind(R.id.map_frame)
    FrameLayout mMapFrame;
    @Bind(R.id.forecast_tv)
    TextView mForescastTV;
    private CameraPosition mSavedCameraPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);
        ButterKnife.bind(this);
        bus = BusProvider.getBus();

        if (savedInstanceState != null)
        {
            restored = true;
            mSavedCameraPosition = savedInstanceState.getParcelable(CAMERA_POSITION);
            mMarkerLatLng = savedInstanceState.getParcelable(MARKER_LAT_LONG);
            mForescastTV.setText(savedInstanceState.getString(FORECAST_TV));
            mLastTemp = savedInstanceState.getInt(LAST_TEMP);
        }

        buildGoogleApiClient();

        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000)        // 10 seconds, in milliseconds
                .setFastestInterval(1000); // 1 second, in milliseconds
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        initializeMapIfNeeded();
        bus.register(this);
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        bus.unregister(this);
        if (mGoogleApiClient.isConnected())
        {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putParcelable(CAMERA_POSITION, mMap.getCameraPosition());
        outState.putParcelable(MARKER_LAT_LONG, mMarkerLatLng);
        outState.putString(FORECAST_TV, mForescastTV.getText().toString());
        outState.putInt(LAST_TEMP, mLastTemp);
    }

    @Override
    public void onConnected(Bundle bundle)
    {
        Utils.isGPSEnabled(this);
        if(!Utils.isNetworkConnected(this))
        {
            Toast.makeText(getApplicationContext(), R.string.network_unavailable,
                    Toast.LENGTH_SHORT).show();
            mMapFrame.setBackgroundColor(Color.BLACK);
            mForescastTV.setText("");
        } else
        {
            Log.i(TAG, "Location services connected");
            Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (location == null)
            {
                // request location updates and let the location listener handle the updates
                LocationServices.FusedLocationApi
                        .requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            } else
            {
                // use the location
                handleNewLocation(location);
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i)
    {
        Log.i(TAG, "Location services disconnected");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult)
    {
        if (connectionResult.hasResolution())
        {
            try
            {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e)
            {
                e.printStackTrace();
            }
        } else
        {
            Log.i(TAG, "Location services connection failed with code " +
                    connectionResult.getErrorCode());
        }
    }

    @Override
    public void onLocationChanged(Location location)
    {
        Log.d(TAG, "location changed");
        handleNewLocation(location);
    }

    protected synchronized void buildGoogleApiClient()
    {
        // Initialize our GoogleAPIClient object
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    private void handleNewLocation(Location location)
    {
        Log.d(TAG, location.toString());
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        // Listener to check weather on different location
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener()
        {
            @Override
            public void onMapLongClick(LatLng latLng)
            {
                restored = false;
                mMarkerLatLng = latLng;
                setUpMap(latLng);
            }
        });

        // Set up map and move camera to the right position
        LatLng latLng = new LatLng(latitude, longitude);
        setUpMap(latLng);

        // Use camera position before rotation if any. Otherwise get the new one.
        if (mSavedCameraPosition != null)
        {
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(mSavedCameraPosition));
        } else
        {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        }
    }

    private void initializeMapIfNeeded()
    {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null)
        {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
        }
    }

    private void setUpMap(LatLng latLng)
    {
        mMap.clear();

        if (mMarkerLatLng != null)
        {
            latLng = mMarkerLatLng;
        }
        mMarkerOptions = new MarkerOptions().position(latLng);
        mMap.addMarker(mMarkerOptions);

        if(!restored)
        {
            if (!Utils.isNetworkConnected(getApplicationContext()))
            {
                Toast.makeText(getApplicationContext(), R.string.network_unavailable,
                        Toast.LENGTH_SHORT).show();
            } else{
                restored = false;
                FetchWeatherService.startActionFetchWeather(this, latLng.latitude,
                        latLng.longitude);
            }
        } else if (mLastTemp != IMPOSIBLE_TEMPERATURE)
        {
            setBackgroundTemperature(mLastTemp);
        }
    }

    public void setBackgroundTemperature(int temp)
    {
        if (temp < 15)
        {
            mMapFrame.setBackgroundColor(getResources().getColor(R.color.temp_cold));
        } else if (temp > 25)
        {
            mMapFrame.setBackgroundColor(getResources().getColor(R.color.temp_hot));
        } else
        {
            mMapFrame.setBackgroundColor(getResources().getColor(R.color.temp_medium));
        }
    }

    //******************************************//
    //**** EVENT BUS METHODS  SUBSCRIPTIONS ****//
    //******************************************//
    @Subscribe
    public void parseReceivedWeatherData(Response<Condition> response)
    {
        // Note that, by definition, response cannot be null
        if (response.code() == 200)
        {
            Condition condition = response.body();
            // check if the value received is not null
            if (condition != null)
            {
                mForescastTV.setText(condition.getTemp() + "ºC - " + condition.getText());
                mLastTemp = condition.getTemp();
                setBackgroundTemperature(mLastTemp);
            } else
            {
                // Weather is unavailable. i.e select the ocean
                mForescastTV.setText(R.string.weather_unavailable);
                mMapFrame.setBackgroundColor(Color.BLACK);
            }
        } else
        {
            mForescastTV.setText(R.string.weather_error);
        }
    }

    @Subscribe
    // Only using the condition parameter for subscription purposes
    public void processFechtWeatherFailure(Condition condition)
    {
        if (!Utils.isNetworkConnected(getApplicationContext()))
        {
            Toast.makeText(getApplicationContext(), R.string.network_unavailable,
                    Toast.LENGTH_SHORT).show();
        } else
        {
            Toast.makeText(getApplicationContext(), R.string.unknown_error,
                    Toast.LENGTH_SHORT).show();
        }
    }
    //****************************************//
    //**** END OF EVENT BUS SUBSCRIPTIONS ****//
    //****************************************//
}
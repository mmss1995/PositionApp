package matteo.position;

import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private static final int LOCATION_PERMISSION = 1;
    private GoogleApiClient mGoogleApiClient = null;
    private boolean googleApiClientReady = false;
    private boolean permissionGranted = false;
    private TextView mTextView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextView = (TextView)findViewById(R.id.textView);

        //next line checks if user has granted permission to use fine location
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED)  {
            Log.d("MainActivity", "Permission granted");
            permissionGranted = true;
        } else {
            Log.d("MainActivity", "Permission NOT granted");
            // we request the permission. When done,
            // the onRequestPermissionsResult method is called
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        //Check if GooglePlayServices are available
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if(status == ConnectionResult.SUCCESS) {
            Log.d("MainActivity", "GooglePlayServices available");
        } else {
            Log.d("MainActivity", "GooglePlayServices UNAVAILABLE");
            if(googleApiAvailability.isUserResolvableError(status)) {
                Log.d("MainActivity", "Ask the user to fix the problem");
                //If the user accepts to install the google play services,
                //a new app will open. When the user gets back to this activity,
                //the onStart method is invoked again.
                googleApiAvailability.getErrorDialog(this, status, 2404).show();
            } else {
                Log.d("MainActivity", "The problem cannot be fixed");
            }
        }

        // Instantiate and connect GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d("MainActivity", "GoogleApiClient connected");
        googleApiClientReady = true;
        checkAndStartLocationUpdate();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("MainActivity", "GoogleApiClient suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d("MainActivity", "GoogleApiClient failed");
        mTextView.setText("Unable to start GooglePlayServices.");
    }

    //The next method is called after the user grants (or not) a permission
    //In our case we only have the fine_location
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[],
                                           int[] grantResults) {
        //In our example we only have one permission, but we could have more
        //we use the requestCode to distinguish them
        switch (requestCode) {
            case LOCATION_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permissionGranted = true;
                    checkAndStartLocationUpdate();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    mTextView.setText("This app needs permission to access location");
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void checkAndStartLocationUpdate() {
        if (permissionGranted && googleApiClientReady) {
            Log.d("MainActivity", "Start updating location");
            LocationRequest mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(10000);
            mLocationRequest.setFastestInterval(5000);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            try {
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            } catch (SecurityException e) {
                // this should not happen because the exception fires when the user has not
                // granted permission to use location, but we already checked this
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("MainActivity", "Location update received: " + location.getLatitude()+", "+ location.getLongitude());
        mTextView.setText(location.getLatitude()+", "+location.getLongitude()+", "+location.getAltitude());
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://maps.googleapis.com/maps/api/geocode/json?latlng="+location.getLatitude()+","+location.getLongitude()+"&key=AIzaSyAZDxAezqiQq0VHN7lF3LzmyPhRbILiKIc";
        Log.d("url",url);
        JsonObjectRequest request = new JsonObjectRequest(url, null, new Response.Listener<JSONObject>(){
                    @Override
                    public void onResponse(JSONObject response) {
                        // Display the first 500 characters of the response string.
                        try {
                            String address = response.getJSONArray("results").getJSONObject(0).getString("formatted_address");
                            mTextView.append("\n"+address);
                            Log.d("Address", address);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        });
        // Add the request to the RequestQueue.
        queue.add(request);

    }
}

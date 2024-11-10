package com.example.mapwithmarker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Parcel;
import android.util.Log;
import android.util.Pair;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

interface LocationCallback {
    void onLocationReceived(Location location) throws IOException;
}

// [START maps_marker_on_map_ready]
public class MapsMarkerActivity extends AppCompatActivity
        implements OnMapReadyCallback, LocationCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FusedLocationProviderClient fusedLocationClient;
    private SearchView searchView;
    private Location start;
    private List<Pair<String, LatLng>> markers;
    private GoogleMap map;
    private static final OkHttpClient client = new OkHttpClient();
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Places.initialize(getApplicationContext(), "AIzaSyCyk9ckBKQBl2deSh4NjX7plTYe_8l-JCQ");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getUserLocation(this);
        }

        setContentView(R.layout.activity_maps);

        searchView = findViewById(R.id.searchBar);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Geocoder geocoder = new Geocoder(MapsMarkerActivity.this, Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocationName(query, 1); // Get only the first result
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        double latitude = address.getLatitude();
                        double longitude = address.getLongitude();
                        Location input = new Location("gps");
                        input.setLatitude(latitude);
                        input.setLongitude(longitude);
                        Toast.makeText(MapsMarkerActivity.this, "Searching for: " + query, Toast.LENGTH_SHORT).show();
                        findLots(input);
                    }
                } catch (IOException e) {
                    Toast.makeText(MapsMarkerActivity.this, "None found for: " + query, Toast.LENGTH_SHORT).show();

                    e.printStackTrace();
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    public void onLocationReceived(Location location) throws IOException {
        start = location;
        markers = findLots(start);
        Toast.makeText(MapsMarkerActivity.this, "markers amount: " + Integer.toString(markers.size()), Toast.LENGTH_SHORT).show();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Manipulates the map when it's available.
     * The API invokes this callback when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user receives a prompt to install
     * Play services inside the SupportMapFragment. The API invokes this method after the user has
     * installed Google Play services and returned to the app.
     */

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getUserLocation(this);
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        googleMap.clear();
        for (Pair<String,LatLng> marker: markers) {
            googleMap.addMarker(new MarkerOptions()
                    .position(marker.second)
                    .title(marker.first));
        }
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(markers.get(0).second.latitude, markers.get(0).second.longitude), 12));
    }

    @SuppressLint("MissingPermission")
    public void getUserLocation(final LocationCallback callback) {
        fusedLocationClient.getLastLocation()
                .addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            Location location = task.getResult();
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();

                            try {
                                callback.onLocationReceived(location);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            Toast.makeText(MapsMarkerActivity.this, "Unable to get location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    public List<Pair<String, LatLng>> findLots(Location location) throws IOException {
        System.out.println(location);
        String url = "https://maps.googleapis.com/maps/api/place/textsearch/json?query=" +
                "parking+lots+nearby+" + location.getLatitude() + "+" + location.getLongitude() + "&key=CUSTOM_KEY";

        Future<List<Pair<String, LatLng>>> future = executorService.submit(() -> {
            List<Pair<String, LatLng>> result = new ArrayList<>();

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                String responseBody = response.body().string();
                System.out.println("Response Body: " + responseBody);

                JSONObject jsonObject = new JSONObject(responseBody);
                JSONArray resultsArray = jsonObject.getJSONArray("results");

                for (int i = 0; i < resultsArray.length(); i++) {
                    JSONObject place = resultsArray.getJSONObject(i);
                    String name = place.getString("name");
                    JSONObject loc = place.getJSONObject("geometry").getJSONObject("location");
                    double lat = loc.getDouble("lat");
                    double lng = loc.getDouble("lng");
                    System.out.println("Place " + i + ": " + name + " Lat: " + lat + " Lng: " + lng);

                    result.add(new Pair<>(name, new LatLng(lat, lng)));
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

            return result;
        });

        try {
            return future.get();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}

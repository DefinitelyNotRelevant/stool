// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

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
import com.google.android.gms.tasks.Tasks;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;


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

/**
 * An activity that displays a Google map with a marker (pin) to indicate a particular location.
 */

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




    // [START_EXCLUDE]
    // [START maps_marker_get_map_async]
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retrieve the content view that renders the map.

        Places.initialize(getApplicationContext(), "AIzaSyCyk9ckBKQBl2deSh4NjX7plTYe_8l-JCQ");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Check for location permissions
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
                // Handle search query submission
                Geocoder geocoder = new Geocoder(MapsMarkerActivity.this, Locale.getDefault());
                try {
                    // Get a list of addresses from the Geocoder
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
        /*
        // Get the SupportMapFragment and request notification when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

         */
    }

    public void onLocationReceived(Location location) throws IOException {
        start = location;
        markers = findLots(start);
        Toast.makeText(MapsMarkerActivity.this, "markers amount: " + Integer.toString(markers.size()), Toast.LENGTH_SHORT).show();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }
    // [END maps_marker_get_map_async]
    // [END_EXCLUDE]

    // [START_EXCLUDE silent]
    /**
     * Manipulates the map when it's available.
     * The API invokes this callback when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user receives a prompt to install
     * Play services inside the SupportMapFragment. The API invokes this method after the user has
     * installed Google Play services and returned to the app.
     */
    // [END_EXCLUDE]
    // [START maps_marker_on_map_ready_add_marker]
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
        // [START_EXCLUDE silent]
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

        // Use submit to get a Future object
        Future<List<Pair<String, LatLng>>> future = executorService.submit(() -> {
            List<Pair<String, LatLng>> result = new ArrayList<>();

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                // Parse the response body to extract names and locations
                String responseBody = response.body().string();
                System.out.println("Response Body: " + responseBody);

                JSONObject jsonObject = new JSONObject(responseBody);
                JSONArray resultsArray = jsonObject.getJSONArray("results");

                // Loop through the results and extract the name and location
                for (int i = 0; i < resultsArray.length(); i++) {
                    JSONObject place = resultsArray.getJSONObject(i);
                    String name = place.getString("name");
                    // Extract the location (latitude and longitude)
                    JSONObject loc = place.getJSONObject("geometry").getJSONObject("location");
                    double lat = loc.getDouble("lat");
                    double lng = loc.getDouble("lng");
                    System.out.println("Place " + i + ": " + name + " Lat: " + lat + " Lng: " + lng);

                    // Add the name and LatLng pair to the result list
                    result.add(new Pair<>(name, new LatLng(lat, lng)));
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

            return result;
        });

        try {
            // Wait for the result and return it once it's available
            return future.get();  // This will block until the task is done and result is returned
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();  // Return an empty list if there's an error
        }
    }
    // [END maps_marker_on_map_ready_add_marker]
}
// [END maps_marker_on_map_ready]

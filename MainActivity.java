package com.example.pgpb9;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.maps.UiSettings;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private MapView mapView;
    private MapboxMap mapboxMap;
    private FusedLocationProviderClient fusedLocationClient;
    private GeoJsonSource userLocationSource;
    private SymbolLayer userLocationLayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Mapbox
        Mapbox.getInstance(this);

        setContentView(R.layout.activity_main);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize LocationRequest
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000); // 10 seconds
        locationRequest.setFastestInterval(5000); // 5 seconds
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Initialize LocationCallback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        // Update the camera position to the latest location
                        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        mapboxMap.animateCamera(cameraUpdate -> {
                            mapboxMap.setCameraPosition(new CameraPosition.Builder()
                                    .target(userLocation)
                                    .zoom(16.0)
                                    .build());
                            return null;
                        });
                        Toast.makeText(MainActivity.this, "Moved to user location", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        };

        // Initialize buttons
        ImageButton btnZoomIn = findViewById(R.id.btnZoomIn);
        ImageButton btnZoomOut = findViewById(R.id.btnZoomOut);
        ImageButton btnGeolocation = findViewById(R.id.btnGeolocation);

        mapView.getMapAsync(mapboxMap -> {
            this.mapboxMap = mapboxMap;

            // Enable compass
            UiSettings uiSettings = mapboxMap.getUiSettings();
            uiSettings.setCompassEnabled(true);

            String tmsUrl = "https://mt1.google.com/vt/lyrs=y&x={x}&y={y}&z={z}";
            String styleJson = "{\n" +
                    "  \"version\": 8,\n" +
                    "  \"sources\": {\n" +
                    "    \"tms-tiles\": {\n" +
                    "      \"type\": \"raster\",\n" +
                    "      \"tiles\": [\"" + tmsUrl + "\"],\n" +
                    "      \"tileSize\": 256\n" +
                    "    }\n" +
                    "  },\n" +
                    "  \"layers\": [\n" +
                    "    {\n" +
                    "      \"id\": \"tms-tiles\",\n" +
                    "      \"type\": \"raster\",\n" +
                    "      \"source\": \"tms-tiles\",\n" +
                    "      \"minzoom\": 0,\n" +
                    "      \"maxzoom\": 22\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";

            mapboxMap.setStyle(new Style.Builder().fromJson(styleJson), style -> {
                // Add custom markers with updated drawable and size
                style.addImage("location2", BitmapFactory.decodeResource(getResources(), R.drawable.location2));

                List<Feature> featureList = new ArrayList<>();
                // Updated coordinates for Surakarta landmarks:
                featureList.add(Feature.fromGeometry(Point.fromLngLat(110.8317275, -7.5755404))); // Keraton Surakarta (Main coordinate)
                featureList.add(Feature.fromGeometry(Point.fromLngLat(110.8219885, -7.5683426))); // Pasar Gede
                featureList.add(Feature.fromGeometry(Point.fromLngLat(110.8392483, -7.5598208))); // Stasiun Solo Balapan
                featureList.add(Feature.fromGeometry(Point.fromLngLat(110.8157051, -7.5709318))); // Pasar Klewer
                featureList.add(Feature.fromGeometry(Point.fromLngLat(110.8224595, -7.5561788))); // Solo Paragon Mall

                GeoJsonSource geoJsonSource = new GeoJsonSource("marker-source", FeatureCollection.fromFeatures(featureList));
                style.addSource(geoJsonSource);

                SymbolLayer symbolLayer = new SymbolLayer("marker-layer", "marker-source")
                        .withProperties(
                                PropertyFactory.iconImage("location2"),
                                PropertyFactory.iconAllowOverlap(true),
                                PropertyFactory.iconIgnorePlacement(true),
                                PropertyFactory.iconSize(0.1f) // Adjust size to 50% of original

                        );
                style.addLayer(symbolLayer);

                // Set initial camera position to Keraton Surakarta
                mapboxMap.setCameraPosition(new CameraPosition.Builder()
                        .target(new LatLng(-7.5755404, 110.8317275))
                        .zoom(15.0)
                        .build());

                // Set button listeners
                btnZoomIn.setOnClickListener(v -> {
                    double currentZoom = mapboxMap.getCameraPosition().zoom;
                    mapboxMap.setCameraPosition(new CameraPosition.Builder()
                            .zoom(currentZoom + 1)
                            .build());
                });

                btnZoomOut.setOnClickListener(v -> {
                    double currentZoom = mapboxMap.getCameraPosition().zoom;
                    mapboxMap.setCameraPosition(new CameraPosition.Builder()
                            .zoom(currentZoom - 1)
                            .build());
                });

                btnGeolocation.setOnClickListener(v -> {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                    } else {
                        // Start location updates
                        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
                    }
                });
            });
        });
    }

    // Lifecycle methods remain unchanged
    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}

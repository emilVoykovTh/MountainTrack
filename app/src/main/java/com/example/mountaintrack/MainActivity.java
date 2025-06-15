package com.example.mountaintrack;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.widget.NestedScrollView;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import android.Manifest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import android.net.Uri;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private boolean isTracking = false, shouldDisplaySelectedTrail = false;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Marker userMarker;
    private ArrayList<ImageData> capturedImages, selectedImages;
    private ArrayList<TrackPoint> currentTrail, selectedTrailPointList;
    private PolylineOptions polylineOptions, currentOnTrailPolylineOptions, offTrailPolylineOptions;
    private Polyline currentOnTrailPolyline, offTrailPolyline;
    private static final float ON_TRAIL_DISTANCE_THRESHOLD = 20f, OFF_TRAIL_THRESHOLD = 30.0f; // meters
    private static final long ANIMATION_DURATION = 1000; // 1 second animation duration
    private static final int ACCESS_LOCATION_REQUEST_CODE = 1001, REQUEST_IMAGE_CAPTURE = 1, REQUEST_CAMERA_PERMISSION = 100;
    private LatLng lastPosition;
    private float lastBearing = 0f, distanceOfSelectedTrail, totalOffCourseDistance = 0f;
    private Uri photoUri;
    private String currentPhotoPath, durationOfSelectedTrail;
    private Location lastKnownLocation;
    private Double focusLat = null;
    private Double focusLng = null;
    private String focusImagePath = null;
    private final ArrayList<Float> speedSamples = new ArrayList<>();
    private TextView tvTrailHelpInfo;
    //For Break calculation while hiking
    private static final float BREAK_DISTANCE_THRESHOLD = 15f; // meters
    private static final long BREAK_TIME_THRESHOLD = 5 * 60 * 1000L; // 5 minutes in ms

    private LatLng lastStablePosition = null;
    private long lastMovementTimestamp = 0L;
    private long totalBreakTimeMillis = 0L;
    private boolean isOnBreak = false, wasOnTrail = true;
    private Button btnZoomIn, btnZoomOut;

    private void showImageMarkerOnMap(double lat, double lng, String imagePath) {
        LatLng imageLocation = new LatLng(lat, lng);

        Marker marker;
        if (isTracking) {
            marker = mMap.addMarker(new MarkerOptions()
                    .position(imageLocation)
                    .title(getString(R.string.photo))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        } else {
            marker = mMap.addMarker(new MarkerOptions()
                    .position(imageLocation)
                    .title(getString(R.string.photo))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        }
        marker.setTag(imagePath);

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(imageLocation, 18f));

        mMap.setOnMarkerClickListener(clickedMarker -> {
            Object tag = clickedMarker.getTag();
            if (tag instanceof String) {
                showImageDialog((String) tag);
                return true; // consume the click
            }
            return false;
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    ACCESS_LOCATION_REQUEST_CODE);
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 10));
            }
        });
        if (shouldDisplaySelectedTrail) {
            displaySelectedTrail();
        }

        mMap.setOnMarkerClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof String) {
                String imagePath = (String) tag;
                showImageDialog(imagePath, marker);
                return true; // return true to indicate event was handled
            }
            return false;
        });

        mMap.setOnMarkerClickListener(marker -> {
            marker.showInfoWindow(); // optional
            Object tag = marker.getTag();
            if (tag instanceof String) {
                String imagePath = (String) tag;
                showImageDialog(imagePath, marker); // pass the marker now
                return true;
            }
            return false;
        });

        if (focusLat != null && focusLng != null && focusImagePath != null) {
            showImageMarkerOnMap(focusLat, focusLng, focusImagePath);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        capturedImages = new ArrayList<>();
        currentTrail = new ArrayList<>();
        selectedImages = new ArrayList<>();
        selectedTrailPointList = new ArrayList<>();
        tvTrailHelpInfo = findViewById(R.id.tvTrailInfo);


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    ACCESS_LOCATION_REQUEST_CODE);
            return;
        }

        findViewById(R.id.btnStart).setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        ACCESS_LOCATION_REQUEST_CODE);
            } else {
                startHike();
            }
        });

        findViewById(R.id.btnStop).setOnClickListener(v -> {
            stopHike();
        });

        findViewById(R.id.btnViewTrack).setOnClickListener(v -> {
            displayCurrentTrail();
        });

        findViewById(R.id.btnFindTrails).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FindTrailsActivity.class);
            if (isTracking) {
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.exit_recording))
                        .setMessage(getString(R.string.exit_recording_msg))
                        .setPositiveButton(getString(R.string.yes), (d, w) -> {
                            isTracking = false;
                            currentTrail.clear();
                            selectedTrailPointList.clear();
                            startActivity(new Intent(this, FindTrailsActivity.class));
                        })
                        .setNegativeButton(getString(R.string.cancel), null)
                        .show();
            } else {
                startActivity(new Intent(this, FindTrailsActivity.class));
            }
        });

        btnZoomIn = findViewById(R.id.btnZoomIn);
        btnZoomOut = findViewById(R.id.btnZoomOut);

        btnZoomIn.setOnClickListener(v -> {
            if (mMap != null) {
                mMap.animateCamera(CameraUpdateFactory.zoomIn());
            }
        });

        btnZoomOut.setOnClickListener(v -> {
            if (mMap != null) {
                mMap.animateCamera(CameraUpdateFactory.zoomOut());
            }
        });

        // Callback for GPS
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    if (location == null || !isTracking) {
                        continue;
                    } else {
                        if (!location.hasAccuracy() || location.getAccuracy() > 10.0f) continue;
                        if (location.hasSpeed() && location.getSpeed() > 0.25f) { // Filter out noise ( < 0.25 m/s)
                            speedSamples.add(location.getSpeed()); // in m/s
                        }

                        if (lastKnownLocation != null) {
                            float distance = location.distanceTo(lastKnownLocation);
                            if (distance < 2.0f && location.getSpeed() < 0.25f) {
                                continue; // Skip GPS noise
                            }
                        }

                        double lat = location.getLatitude();
                        double lng = location.getLongitude();
                        double alt = location.getAltitude();
                        LatLng latLng = new LatLng(lat, lng);
                        lastKnownLocation = location;
                        showUserMarker(lastKnownLocation);
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f));

                        if (selectedTrailPointList != null && !selectedTrailPointList.isEmpty()) {
                            float remainingDistance = calculateRemainingDistanceFromCurrentLocation(lastKnownLocation, selectedTrailPointList);
                            handleBreakDetection(lastKnownLocation);
                            String eta = estimateTimeForTrail(remainingDistance);
                            updateTrailInfoUI(distanceOfSelectedTrail, remainingDistance, eta, totalBreakTimeMillis);
                            boolean isOnTrail = isLocationNearSelectedTrail(latLng);

                            if (isOnTrail) {
                                currentOnTrailPolylineOptions.add(latLng);
                                if (currentOnTrailPolyline != null) currentOnTrailPolyline.remove();
                                currentOnTrailPolyline = mMap.addPolyline(currentOnTrailPolylineOptions);

                                wasOnTrail = true; // Update state
                            } else {
                                if (wasOnTrail) {
                                    // Just switched from on-trail to off-trail
                                    offTrailPolylineOptions = new PolylineOptions()
                                            .color(Color.RED)
                                            .width(18);
                                }

                                offTrailPolylineOptions.add(latLng);
                                if (offTrailPolyline != null) offTrailPolyline.remove();
                                offTrailPolyline = mMap.addPolyline(offTrailPolylineOptions);

                                wasOnTrail = false; // Update state
                            }
                        }
                        // Check if current location is close to selected trail


                        long currentTime = System.currentTimeMillis();

                        // Save the current point
                        TrackPoint point = new TrackPoint(0, lat, lng, alt, currentTime);
                        currentTrail.add(point);
                    }
                }
            }
        };

        // getting the selected trail from FindTrailsActivity
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("trailPoints")) {
            selectedTrailPointList = intent.getParcelableArrayListExtra("trailPoints");
            shouldDisplaySelectedTrail = true; // отбелязваме че трябва да се покаже
            if (intent.hasExtra("allImages")) {
                ArrayList<ImageData> updatedImages = intent.getParcelableArrayListExtra("allImages");
                selectedImages = new ArrayList<>(updatedImages);
            }
            distanceOfSelectedTrail = intent.getFloatExtra("distanceOfTrail", 0);
            durationOfSelectedTrail = intent.getStringExtra("durationOfTrail");
        }//single image selected from recycler view - photos
        if (intent != null && intent.hasExtra("focusLatitude")) {
            focusLat = intent.getDoubleExtra("focusLatitude", 0);
            focusLng = intent.getDoubleExtra("focusLongitude", 0);
            focusImagePath = intent.getStringExtra("focusImagePath");
        }

        // When opening the camera
        findViewById(R.id.btnTakePhoto).setOnClickListener(v -> checkCameraPermissionAndTakePicture());

        //Плъзгащ се прозорец с помощна информация и бутони
        NestedScrollView bottomSheet = findViewById(R.id.bottom_sheet);
        BottomSheetBehavior<NestedScrollView> sheetBehavior = BottomSheetBehavior.from(bottomSheet);

        //convert dp to px
        int peekHeight = (int) (40 * getResources().getDisplayMetrics().density) +
                (int) (8 * getResources().getDisplayMetrics().density);

        sheetBehavior.setPeekHeight(peekHeight);
        sheetBehavior.setHideable(false);  // so bottom sheet cannot be hidden completely

        sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

    }

    /**
     * This function checks if a location is within the ON_TRAIL_DISTANCE_THRESHOLD to any points of the selected
     * trail.
     *
     * @param location - the location that needs to be compared
     * @return true- if within the threshold or if the no trail was selected
     */
    private boolean isLocationNearSelectedTrail(LatLng location) {
        if (selectedTrailPointList == null || selectedTrailPointList.isEmpty()) {
            return true; // No selected trail, so always "on trail"
        }

        // Check distance to every segment or point of the selected trail, return true if within threshold
        for (int i = 0; i < selectedTrailPointList.size() - 1; i++) {
            LatLng start = new LatLng(selectedTrailPointList.get(i).getLatitude(),
                    selectedTrailPointList.get(i).getLongitude());
            LatLng end = new LatLng(selectedTrailPointList.get(i + 1).getLatitude(),
                    selectedTrailPointList.get(i + 1).getLongitude());

            double distance = distanceToSegment(location, start, end);
            if (distance <= ON_TRAIL_DISTANCE_THRESHOLD) {
                return true;
            }
        }
        return false;
    }

    /**
     * Calculates the shortest distance in meters from a given point  (p) to the line segment defined by points
     * (start) and (end), using distance via Android's Location distanceBetween() method.
     * <p>
     * This method handles edge cases where the projection of point p onto the segment lies
     * beyond either endpoint. It approximates the projection in a 2D lat/lng space.
     *
     * @param p     The point (LatLng) from which the distance is measured.
     * @param start The starting point of the segment.
     * @param end   The ending point of the segment.
     * @return The shortest distance in meters from point p to the segment vw.
     */
    private double distanceToSegment(LatLng p, LatLng start, LatLng end) {
        // Convert LatLng to meters using Android Location class to ease math
        float[] resultsStart = new float[1];
        Location.distanceBetween(p.latitude, p.longitude, start.latitude, start.longitude, resultsStart);
        float[] resultsEnd = new float[1];
        Location.distanceBetween(p.latitude, p.longitude, end.latitude, end.longitude, resultsEnd);
        float[] resultsSegment = new float[1];
        Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, resultsSegment);

        double lengthSquared = resultsSegment[0] * resultsSegment[0];
        if (lengthSquared == 0) return resultsStart[0]; // start == end case

        // Approximate using vectors in meters:

        // Vector pv
        float[] pv = new float[2];
        pv[0] = (float) (p.latitude - start.latitude);
        pv[1] = (float) (p.longitude - start.longitude);

        // Vector vw
        float[] vw = new float[2];
        vw[0] = (float) (end.latitude - start.latitude);
        vw[1] = (float) (end.longitude - start.longitude);

        // Dot product pv·vw
        float dot = pv[0] * vw[0] + pv[1] * vw[1];
        double t = dot / lengthSquared;

        if (t < 0) return resultsStart[0]; // Beyond start end
        else if (t > 1) return resultsEnd[0]; // Beyond end end

        // Projection falls on segment
        // Compute projection point coordinates:
        double projLat = start.latitude + t * (end.latitude - start.latitude);
        double projLng = start.longitude + t * (end.longitude - start.longitude);

        float[] resultsProj = new float[1];
        Location.distanceBetween(p.latitude, p.longitude, projLat, projLng, resultsProj);
        return resultsProj[0];
    }

    /**
     * Displays the current trail on the map (gray), including both the selected trail (green),off-trail (red) segments,
     * if there is a selected trail. Also centers the camera on the last recorded location and restarts
     * tracking after a short delay of 3 seconds.
     *
     * @throws IllegalStateException if mMap is not initialized or currentTrail TrackPoint list is empty while tracking.
     * @requires ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION permissions.
     */
    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    private void displayCurrentTrail() {
        if (mMap == null) {
            Toast.makeText(this, getString(R.string.map_not_ready), Toast.LENGTH_SHORT).show();
            return;
        }
        if (userMarker != null) {
            userMarker.remove();
            userMarker = null;
        }

        if (currentTrail.isEmpty() || !isTracking) {
            Toast.makeText(this, getString(R.string.no_saved_trail), Toast.LENGTH_SHORT).show();
            return;
        }
/*
        //Move camera to latest location
        LatLng startLatLng = new LatLng(
                currentTrail.get(currentTrail.size() - 1).getLatitude(),
                currentTrail.get(currentTrail.size() - 1).getLongitude()
        );
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startLatLng, 18f));

        fusedLocationClient.removeLocationUpdates(locationCallback);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (selectedTrailPointList != null && !selectedTrailPointList.isEmpty()) {
                displaySelectedTrail();
            }
            startHike();
        }, 3000);*/

        PolylineOptions currentTrackLine = new PolylineOptions().color(Color.GRAY).width(18);
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

        for (TrackPoint point : currentTrail) {
            LatLng latLng = new LatLng(point.getLatitude(), point.getLongitude());
            currentTrackLine.add(latLng);
            boundsBuilder.include(latLng);
        }

        mMap.addPolyline(currentTrackLine);
        LatLngBounds bounds = boundsBuilder.build();
        fusedLocationClient.removeLocationUpdates(locationCallback);
        final int padding = 300; // pixels

        if (capturedImages != null && !capturedImages.isEmpty()) {
            for (ImageData image : capturedImages) {
                showImageMarkerOnMap(image.getLatitude(), image.getLongitude(), image.getImagePath());
            }
        }

        mMap.setOnMapLoadedCallback(() -> {
            try {
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
            } catch (Exception e) {
                e.printStackTrace();
                // fallback to first point zoom
                LatLng first = new LatLng(selectedTrailPointList.get(0).getLatitude(), selectedTrailPointList.get(0).getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(first, 15f));
            }
        });

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startHike();
        }, 3000);
    }


    private void showUserMarker(Location location) {
        LatLng newPosition = new LatLng(location.getLatitude(), location.getLongitude());

        float bearing;
        if (location.hasBearing()) {
            bearing = location.getBearing();
        } else {
            bearing = 0f;
        }

        // Smooth the bearing to avoid sudden jumps (low-pass filter)
        bearing = smoothBearing(lastBearing, bearing, 0.2f);
        lastBearing = bearing;

        if (userMarker == null) {
            userMarker = mMap.addMarker(new MarkerOptions()
                    .position(newPosition)
                    .anchor(0.5f, 0.5f)
                    .flat(true)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.arrow_icon))
                    .rotation(bearing));
            lastPosition = newPosition;
        } else {
            // Animate position and rotation smoothly
            animateMarkerToPosition(userMarker, newPosition, bearing);
        }
    }

    // filter to smooth bearing changes
    private float smoothBearing(float lastBearing, float newBearing, float alpha) {
        float diff = newBearing - lastBearing;
        if (diff > 180) diff -= 360;
        if (diff < -180) diff += 360;
        return (lastBearing + alpha * diff + 360) % 360;
    }


    /**
     * Smoothly animates a marker from its current position and rotation
     * to a new target position and bearing. The animation uses linear interpolation
     * for position and handles rotation wrap-around (0–360°) for smooth turning.
     * Runs on the main UI thread at approximately 60 frames per second.
     *
     * @param marker     The marker to be animated.
     * @param toPosition The target LatLng position to move the marker to.
     * @param toRotation The target rotation angle in degrees (0–360°), relative to north.
     */
    private void animateMarkerToPosition(final Marker marker, final LatLng toPosition, final float toRotation) {
        final LatLng startPosition = marker.getPosition();
        final float startRotation = marker.getRotation();

        final long start = System.currentTimeMillis();
        final Handler handler = new Handler(Looper.getMainLooper());

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsedTime = System.currentTimeMillis() - start;
                float t = Math.min(1, (float) elapsedTime / ANIMATION_DURATION);

                // Interpolate latitude and longitude
                double lat = (toPosition.latitude - startPosition.latitude) * t + startPosition.latitude;
                double lng = (toPosition.longitude - startPosition.longitude) * t + startPosition.longitude;
                LatLng newPos = new LatLng(lat, lng);

                // Interpolate rotation, accounting for wrap-around
                float rotationDiff = toRotation - startRotation;
                if (rotationDiff > 180) rotationDiff -= 360;
                if (rotationDiff < -180) rotationDiff += 360;
                float rotation = (startRotation + t * rotationDiff + 360) % 360;

                marker.setPosition(newPos);
                marker.setRotation(rotation);

                if (t < 1.0f) {
                    handler.postDelayed(this, 16); // approx 60fps
                } else {
                    lastPosition = toPosition;
                }
            }
        });
    }

    /**
     * Displays the currently selected trail on the map in green.
     * If the trail contains only one point, it places a single polyline segment at that location.
     * Otherwise, it draws the entire trail using a green polyline and adjusts the camera
     * to fit the trail's bounds with padding.
     * <p>
     * If image markers are associated with the trailId, they are also displayed.
     */
    private void displaySelectedTrail() {
        if (selectedTrailPointList == null || selectedTrailPointList.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_trail), Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedTrailPointList.size() == 1) {
            LatLng singlePoint = new LatLng(selectedTrailPointList.get(0).getLatitude(), selectedTrailPointList.get(0).getLongitude());
            mMap.addPolyline(new PolylineOptions().color(Color.GREEN).width(18).add(singlePoint));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(singlePoint, 15f));
            return;
        }

        PolylineOptions selectedTrackLine = new PolylineOptions().color(Color.GREEN).width(18);
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

        for (TrackPoint point : selectedTrailPointList) {
            LatLng latLng = new LatLng(point.getLatitude(), point.getLongitude());
            selectedTrackLine.add(latLng);
            boundsBuilder.include(latLng);
        }

        mMap.addPolyline(selectedTrackLine);
        LatLngBounds bounds = boundsBuilder.build();
        fusedLocationClient.removeLocationUpdates(locationCallback);
        final int padding = 300; // pixels

        if (selectedImages != null && !selectedImages.isEmpty()) {
            for (ImageData image : selectedImages) {
                showImageMarkerOnMap(image.getLatitude(), image.getLongitude(), image.getImagePath());
            }
        }

        mMap.setOnMapLoadedCallback(() -> {
            try {
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
            } catch (Exception e) {
                e.printStackTrace();
                // fallback to first point zoom
                LatLng first = new LatLng(selectedTrailPointList.get(0).getLatitude(), selectedTrailPointList.get(0).getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(first, 15f));
            }
        });

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startHike();
        }, 3000);
    }

    private void startHike() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    ACCESS_LOCATION_REQUEST_CODE);
            return;
        }

        //When starting hike for the first time
        if (!isTracking) {
            currentTrail.clear();
            capturedImages.clear();
            speedSamples.clear();

            isTracking = true;
        }

        if (userMarker != null) {
            userMarker.remove();
            userMarker = null;
        }

        currentOnTrailPolylineOptions = new PolylineOptions()
                .color(Color.GRAY)
                .width(18);

        offTrailPolylineOptions = new PolylineOptions()
                .color(Color.RED)
                .width(18);

        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)  // Highest GPS accuracy
                .setInterval(4000)      // Request location every 4 seconds
                .setFastestInterval(2000)  // Accept faster updates if available
                .setMaxWaitTime(3000);

        //initial location
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        LatLng startLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(startLatLng, 18f));
                        showUserMarker(location);
                    }
                });

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest).setAlwaysShow(true);

        SettingsClient settingsClient = LocationServices.getSettingsClient(this);

        settingsClient.checkLocationSettings(builder.build())
                .addOnSuccessListener(locationSettingsResponse -> {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, getString(R.string.no_permission_for_location_access), Toast.LENGTH_LONG).show();
                        return;
                    }
                    fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
                })
                .addOnFailureListener(e -> {
                    if (e instanceof ResolvableApiException) {
                        try {
                            ResolvableApiException resolvable = (ResolvableApiException) e;
                            resolvable.startResolutionForResult(MainActivity.this, ACCESS_LOCATION_REQUEST_CODE);
                        } catch (IntentSender.SendIntentException sendEx) {
                            sendEx.printStackTrace();
                        }
                    }
                });
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    private void stopHike() {
        isTracking = false;
        fusedLocationClient.removeLocationUpdates(locationCallback);

        if (currentTrail.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_saved_trail_for_stopping), Toast.LENGTH_SHORT).show();
            mMap.clear();
            userMarker = null;
            return;
        }

        displayCurrentTrail();
        if (selectedTrailPointList != null && selectedTrailPointList.isEmpty()) {
            selectedTrailPointList.clear();
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText nameInput = new EditText(this);
        nameInput.setHint(getString(R.string.trail_name));
        layout.addView(nameInput);

        final EditText descriptionInput = new EditText(this);
        descriptionInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100)});
        descriptionInput.setHint(getString(R.string.trail_description));
        layout.addView(descriptionInput); // Add description input first

        TextView charCounter = new TextView(this);
        charCounter.setText("0/100");
        charCounter.setTextSize(12);
        charCounter.setTextColor(Color.GRAY);
        charCounter.setGravity(Gravity.END);
        layout.addView(charCounter); // Add counter right after input

        descriptionInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                charCounter.setText(s.length() + "/100");
            }

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.save_trail_and_images))
                .setMessage(getString(R.string.dialog_hint_save_trail))
                .setView(layout)
                .setPositiveButton(getString(R.string.save), (dialog, which) -> {
                    String name = nameInput.getText().toString().trim();
                    String description = descriptionInput.getText().toString().trim();
                    String duration = calculateDurationInMillis(currentTrail);
                    float distance = calculateTrailDistance(currentTrail);
                    AltitudeStats stats = calculateAltitudeChanges(currentTrail);
                    double totalAscent = stats.totalAscent;
                    double totalDescent = stats.totalDescent;

                    if (name.isEmpty()) name = getString(R.string.no_name);

                    Intent intent = new Intent(MainActivity.this, FindTrailsActivity.class);
                    intent.putParcelableArrayListExtra("trailPoints", currentTrail);
                    intent.putParcelableArrayListExtra("trailImages", capturedImages);
                    intent.putExtra("trailName", name);
                    intent.putExtra("trailDescription", description);
                    intent.putExtra("trailDuration", duration);
                    intent.putExtra("trailDistance", distance);
                    intent.putExtra("totalAscent", totalAscent);
                    intent.putExtra("totalDescent", totalDescent);
                    startActivity(intent);
                })
                .setNegativeButton(getString(R.string.no), (dialog, which) -> {
                    Toast.makeText(this, getString(R.string.trail_not_saved), Toast.LENGTH_SHORT).show();
                })
                .show();

        userMarker = null;
        mMap.clear();

        if (!isTracking) {

        }


    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }


    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            if (lastKnownLocation != null) {
                LatLng photoLocation = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                long timestamp = System.currentTimeMillis();

                // ID и trailId are 0 by default. They will be assigned the new trail Id if saved through FindTrailsActivity
                ImageData imageData = new ImageData(
                        0, // id
                        0, // temporary trailId
                        currentPhotoPath,
                        photoLocation.latitude,
                        photoLocation.longitude,
                        timestamp,
                        null
                );

                capturedImages.add(imageData);
                for (ImageData image : capturedImages) {
                    showImageMarkerOnMap(image.getLatitude(), image.getLongitude(), image.getImagePath());
                }

                Toast.makeText(this, getString(R.string.image_saved) + currentPhotoPath, Toast.LENGTH_SHORT).show();
            }
        }

    }


    private void checkCameraPermissionAndTakePicture() {
        if (lastKnownLocation == null) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.location_unavailable))
                    .setMessage(getString(R.string.location_unavailable_no_picture))
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        } else {
            // Permission granted, proceed
            dispatchTakePictureIntent();
        }
    }


    //Permission for the camera
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, getString(R.string.camera_permission_needed), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showImageDialog(String imagePath, Marker marker) {
        File imgFile = new File(imagePath);
        if (!imgFile.exists()) {
            Toast.makeText(this, getString(R.string.img_not_found), Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(bitmap);
        imageView.setAdjustViewBounds(true);
        imageView.setPadding(20, 20, 20, 20);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.photo))
                .setView(imageView)
                .setPositiveButton(getString(R.string.close), null)
                .setNegativeButton("Delete", (dialog, which) -> {
                    if (imgFile.delete()) {
                        marker.remove();
                        Toast.makeText(this, getString(R.string.deletion_successful), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, getString(R.string.deletion_unsuccessful), Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void showImageDialog(String imagePath) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.photo));

        ImageView imageView = new ImageView(this);
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        imageView.setImageBitmap(bitmap);
        imageView.setAdjustViewBounds(true);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        builder.setView(imageView);

        builder.setPositiveButton(getString(R.string.close), (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private float calculateTrailDistance(ArrayList<TrackPoint> trailPoints) {
        float totalDistance = 0f;
        for (int i = 1; i < trailPoints.size(); i++) {
            LatLng start = trailPoints.get(i - 1).getLatLng();
            LatLng end = trailPoints.get(i).getLatLng();
            float[] results = new float[1];
            Location.distanceBetween(
                    start.latitude, start.longitude,
                    end.latitude, end.longitude,
                    results
            );
            totalDistance += results[0]; // meters
        }
        return totalDistance; // in meters
    }

    /**
     * This function calculates the whole duration of a trail point list
     */
    private String calculateDurationInMillis(ArrayList<TrackPoint> trailPoints) {
        if (trailPoints.isEmpty()) return "0";

        long start = trailPoints.get(0).getTimestamp();
        long end = trailPoints.get(trailPoints.size() - 1).getTimestamp();

        long seconds = (end - start) / 1000;
        long minutes = (seconds / 60) % 60;
        long hours = seconds / 3600;

        return String.format("%02d:%02d", hours, minutes);
    }

    public class AltitudeStats {
        public double totalAscent = 0.0;
        public double totalDescent = 0.0;
    }

    /**
     * This function calculates the total altitude changes within a trail point list
     */
    public AltitudeStats calculateAltitudeChanges(ArrayList<TrackPoint> points) {
        AltitudeStats stats = new AltitudeStats();

        for (int i = 1; i < points.size(); i++) {
            double diff = points.get(i).getAltitude() - points.get(i - 1).getAltitude();

            if (diff > 0) {
                stats.totalAscent += diff;  // climbed meters
            } else if (diff < 0) {
                stats.totalDescent += -diff;  // descended meters
            }
        }
        return stats;
    }

    /**
     * Find the nearest point on the trail from the current location.
     * Calculate the distance from that point to the end of the trail.
     *
     * @param currentLocation - the users current location
     * @param trailPoints     - List of the trail points the user is following
     * @return
     */
    private float calculateRemainingDistanceFromCurrentLocation(Location currentLocation, ArrayList<TrackPoint> trailPoints) {
        // 1. Find nearest point on trail
        int nearestIndex = 0;
        float minDistance = Float.MAX_VALUE;
        for (int i = 0; i < trailPoints.size(); i++) {
            TrackPoint tp = trailPoints.get(i);
            float[] result = new float[1];
            Location.distanceBetween(
                    currentLocation.getLatitude(), currentLocation.getLongitude(),
                    tp.getLatitude(), tp.getLongitude(),
                    result
            );
            if (result[0] < minDistance) {
                minDistance = result[0];
                nearestIndex = i;
            }
        }

        // 2. Sum the distance from the nearest point to the end of the trail
        float remainingDistance = 0f;
        for (int i = nearestIndex; i < trailPoints.size() - 1; i++) {
            TrackPoint p1 = trailPoints.get(i);
            TrackPoint p2 = trailPoints.get(i + 1);
            float[] dist = new float[1];
            Location.distanceBetween(
                    p1.getLatitude(), p1.getLongitude(),
                    p2.getLatitude(), p2.getLongitude(),
                    dist
            );
            remainingDistance += dist[0];
        }

        return remainingDistance + minDistance; // include distance from current to nearest point
    }

    /**
     * This function calculates the median walking speed dynamically using recorded speed samples between trail points.
     */
    private float getMedianSpeed() {
        if (speedSamples.isEmpty()) return 1.39f; //default walking speed - 5km/h

        ArrayList<Float> sorted = new ArrayList<>(speedSamples);
        Collections.sort(sorted);

        int size = sorted.size();
        if (size % 2 == 0) {
            return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2f;
        } else {
            return sorted.get(size / 2);
        }
    }

    /**
     * Estimates the time required to complete a trail based on its distance and the user's walking speed.
     *
     * This method calculates the expected duration in hours and minutes using the median walking speed
     * (in meters per second) previously collected during the hike. If the walking speed is too low
     * (e.g., due to no data), a default value of 1.0 m/s is used to avoid division by zero or unrealistic results.
     *
     * @param distanceMeters The total trail distance in meters.
     * @return A formatted time string in "HH:mm" representing the estimated duration to complete the trail.
     */
    private String estimateTimeForTrail(float distanceMeters) {
        float walkingSpeed = getMedianSpeed(); // in m/s

        // Avoid division by zero
        if (walkingSpeed < 0.1f) walkingSpeed = 1.0f;

        float timeInSeconds = distanceMeters / walkingSpeed;

        int totalMinutes = (int) (timeInSeconds / 60);
        int hoursFinal = totalMinutes / 60;
        int minutesFinal = totalMinutes % 60;

        return String.format(Locale.getDefault(), "%02d:%02d", hoursFinal, minutesFinal);
    }

    /**
     * This is to present the help information on the UI
     */
    private void updateTrailInfoUI(float totalTrailDistance, float remainingDistance, String eta, long breaksCount) {
        long seconds = breaksCount / 1000;
        long minutes = (seconds / 60) % 60;
        long hours = seconds / 3600;
        String totalBreakTime = String.format("%02d:%02d", hours, minutes);

        String info = String.format(Locale.getDefault(),
                getString(R.string.format_break_info) + totalBreakTime,
                totalTrailDistance / 1000f,
                remainingDistance / 1000f,
                eta
        );

        tvTrailHelpInfo.setText(info);
    }

/**
 * Detects whether the user has taken a break based on their movement and time spent stationary.
 *
 * This method checks the distance between the current location and the last stable position.
 * If the user has not moved beyond a defined distance threshold for a certain time period,
 * they are considered to be on a break, and the break time is recorded.
 * If movement is detected beyond the threshold, the break status is reset.
 *
 * @param location - The current Location of the user.
 *
 * @Preconditions:
 * - BREAK_DISTANCE_THRESHOLD defines the minimum distance (in meters) that qualifies as movement.
 * - BREAK_TIME_THRESHOLD defines the duration (in milliseconds) the user must remain stationary to be considered on a break.
 */
    private void handleBreakDetection(Location location) {
        long now = System.currentTimeMillis();
        LatLng currentPosition = new LatLng(location.getLatitude(), location.getLongitude());

        if (lastStablePosition == null) {
            lastStablePosition = currentPosition;
            lastMovementTimestamp = now;
            return;
        }

        float[] result = new float[1];
        Location.distanceBetween(
                currentPosition.latitude, currentPosition.longitude,
                lastStablePosition.latitude, lastStablePosition.longitude,
                result
        );

        if (result[0] > BREAK_DISTANCE_THRESHOLD) {
            // Moved enough -> reset
            if (isOnBreak) {
                isOnBreak = false;
            }
            lastStablePosition = currentPosition;
            lastMovementTimestamp = now;
        } else {
            // Still in same spot
            if (!isOnBreak && now - lastMovementTimestamp >= BREAK_TIME_THRESHOLD) {
                long breakDuration = now - lastMovementTimestamp;
                totalBreakTimeMillis += breakDuration;
                isOnBreak = true;
            }
        }
    }

}
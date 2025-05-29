package com.example.mountaintrack;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;


public class FindTrailsActivity extends AppCompatActivity {

    private RecyclerView recyclerTrails, recyclerImages;
    private TrailAdapter trailsAdapter;
    private List<Trail> trailsList;
    private HikeDatabaseHelper dbHelper;
    private ImageButton btnMap, btnFavorites;
    private Button btnAllTrails, btnMyTrails, btnViewImages;
    private boolean showingFavorites, imagesVisible;
    private ArrayList<ImageData> allImages;
    private ImageAdapter imageAdapter;
    private EditText searchInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_trails);

        dbHelper = new HikeDatabaseHelper(this);
        trailsList = new ArrayList<>();

        allImages = dbHelper.getAllImages();
        recyclerImages = findViewById(R.id.recyclerImages);
        recyclerImages.setLayoutManager(new LinearLayoutManager(this));

        //setting up the trails adapter
        recyclerTrails = findViewById(R.id.recyclerTrails);
        recyclerTrails.setLayoutManager(new LinearLayoutManager(this));
        trailsAdapter = new TrailAdapter(this, trailsList, new TrailAdapter.OnTrailClickListener() {
            @Override
            public void onFavoriteClick(Trail trail, int position) {
                boolean newStatus = !trail.isFavorite();
                trail.setFavorite(newStatus);
                dbHelper.setTrailFavoriteStatus(trail.getId(), newStatus);

                if (showingFavorites) {
                    trailsList.remove(position);
                    trailsAdapter.updateList(trailsList);
                    if (trailsList.isEmpty()) {
                        Toast.makeText(FindTrailsActivity.this, getString(R.string.no_saved_favorite_trails), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    trailsAdapter.notifyItemChanged(position);
                }
            }

            @Override
            public void onViewTrailClick(Trail trail) {
                ArrayList<TrackPoint> trackPoints = dbHelper.getTrackPointsByTrailId(trail.getId());
                ArrayList<ImageData> images = dbHelper.getImagesByTrailId(trail.getId());
                String durationOfTrail = trail.getDuration();
                float distanceOfTrail = trail.getDistance();

                if (trackPoints == null || trackPoints.isEmpty()) {
                    Toast.makeText(FindTrailsActivity.this, getString(R.string.no_available_trail_points), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (images == null || images.isEmpty()) {
                    Toast.makeText(FindTrailsActivity.this, getString(R.string.no_available_photos), Toast.LENGTH_SHORT).show();
                }

                //Sending the selected trail to MainActivity
                Intent intent = new Intent(FindTrailsActivity.this, MainActivity.class);
                intent.putExtra("trailPoints", trackPoints);
                intent.putExtra("allImages", images);
                intent.putExtra("durationOfTrail", durationOfTrail);
                intent.putExtra("distanceOfTrail", distanceOfTrail);

                startActivity(intent);
            }

            @Override
            public void onDeleteTrailRequest(Trail trail, int position) {

                if (!trail.isUserCreated()) {
                    Toast.makeText(FindTrailsActivity.this, getString(R.string.no_deletion_for_default_trails), Toast.LENGTH_SHORT).show();
                    return;
                }

                new AlertDialog.Builder(FindTrailsActivity.this)
                        .setTitle(getString(R.string.trail_deletion))
                        .setMessage(getString(R.string.deletion_question_part1) + trail.getName() + getString(R.string.deletion_question_part2))
                        .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                            dbHelper.deleteTrailById(trail.getId());
                            dbHelper.deleteTrackPointsByTrailId(trail.getId());
                            dbHelper.deleteImagesByTrailId(trail.getId());
                            trailsList.remove(position);
                            trailsAdapter.updateList(trailsList);
                            Toast.makeText(FindTrailsActivity.this, getString(R.string.trail_deleted), Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton(getString(R.string.no), null)
                        .show();
            }
        }, new TrailAdapter.ImageProvider() {
            @Override
            public List<ImageData> getImagesForTrail(int trailId) {
                return dbHelper.getImagesByTrailId(trailId);
            }
        });
        recyclerTrails.setAdapter(trailsAdapter);

        //setting up the image adapter
        imageAdapter = new ImageAdapter(this, allImages, new ImageAdapter.OnImageClickListener() {

            @Override
            public void onViewImageOnMap(ImageData imageData) {
                // You could start MainActivity with location info
                Intent intent = new Intent(FindTrailsActivity.this, MainActivity.class);
                intent.putExtra("focusLatitude", imageData.getLatitude());
                intent.putExtra("focusLongitude", imageData.getLongitude());
                intent.putExtra("focusImagePath", imageData.getImagePath());
                startActivity(intent);
            }

            @Override
            public void onDeleteImage(ImageData imageData, int position) {
                new AlertDialog.Builder(FindTrailsActivity.this)
                        .setTitle(getString(R.string.photo_deletion))
                        .setMessage(getString(R.string.photo_deletion_confirmation))
                        .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dbHelper.deleteImageById(imageData.getId());
                                allImages.remove(position);
                                imageAdapter.updateImageList(allImages);
                                imageAdapter.notifyItemRemoved(position);
                                Toast.makeText(FindTrailsActivity.this, getString(R.string.photo_deleted), Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton(getString(R.string.cancel), null)
                        .show();
            }

        });
        recyclerImages.setAdapter(imageAdapter);

        Intent intent = getIntent();
        if (intent.hasExtra("trailPoints") && intent.hasExtra("trailImages")) {
            ArrayList<TrackPoint> trailPoints = getIntent().getParcelableArrayListExtra("trailPoints");
            ArrayList<ImageData> trailImages = getIntent().getParcelableArrayListExtra("trailImages");
            String trailName = getIntent().getStringExtra("trailName");
            String duration = getIntent().getStringExtra("trailDuration");
            String trailDescription = getIntent().getStringExtra("trailDescription");
            float distance = getIntent().getFloatExtra("trailDistance", 0);
            double totalAscent = getIntent().getDoubleExtra("totalAscent", 0);
            double totalDescent = getIntent().getDoubleExtra("totalDescent", 0);
            int trailId = dbHelper.getNextAvailableTrailId();

            Trail trail = new Trail(trailId, trailName, trailDescription, true, false, duration, distance
                    , totalAscent, totalDescent);
            if (trailPoints != null) {
                saveTrailToDatabase(trail, trailPoints, trailImages);
            }
        } else {
            showTrails();
            showingFavorites = false;
            updateTrailList();
        }


        btnMap = findViewById(R.id.btnMap);
        btnMap.setOnClickListener(v -> {
            showTrails();
            Intent goToMapIntent = new Intent(FindTrailsActivity.this, MainActivity.class);
            startActivity(goToMapIntent);
            finish();
        });

        //Reloads all userCreated trails
        btnMyTrails = findViewById(R.id.btnMyTrails);
        btnMyTrails.setOnClickListener(v -> {
            showingFavorites = false;
            showTrails();
            trailsList.clear();
            trailsList = dbHelper.getUserTrails();

            if (trailsList.isEmpty()) {
                Toast.makeText(this, getString(R.string.no_saved_my_trails), Toast.LENGTH_SHORT).show();
            }

            trailsAdapter.updateList(trailsList);
        });

        //Reloads all favorite trails
        btnFavorites = findViewById(R.id.btnFavorites);
        btnFavorites.setOnClickListener(v -> {
            showTrails();
            showingFavorites = true;
            trailsList = dbHelper.getFavoriteTrails();
            if (trailsList.isEmpty()) {
                Toast.makeText(this, getString(R.string.no_saved_favorite_trails), Toast.LENGTH_SHORT).show();
            }
            trailsAdapter.updateList(trailsList);
        });

        //Reloads all trails (userCreated and default)
        btnAllTrails = findViewById(R.id.btnAllTrails);
        btnAllTrails.setOnClickListener(v -> {
            showTrails();
            showingFavorites = false;
            updateTrailList();
        });

        btnViewImages = findViewById(R.id.btnViewImages);
        btnViewImages.setOnClickListener(v -> {
            updateImageList();
            showImages();
        });

        searchInput = findViewById(R.id.searchInput);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterTrails(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

    }

    private void filterTrails(String query) {
        List<Trail> filteredList = new ArrayList<>();
        for (Trail trail : trailsList) {
            if (trail.getName().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(trail);
            }
        }
        trailsAdapter.updateList(filteredList);
    }

    private void showImages() {
        recyclerImages.setVisibility(View.VISIBLE);
        recyclerTrails.setVisibility(View.GONE);
        imagesVisible = true;
    }

    private void showTrails() {
        recyclerImages.setVisibility(View.GONE);
        recyclerTrails.setVisibility(View.VISIBLE);
        imagesVisible = false;
    }

    private void updateImageList() {
        allImages = dbHelper.getAllImages();
        imageAdapter.updateImageList(allImages);
    }

    private void updateTrailList() {
        trailsList.clear();
        trailsList = dbHelper.getAllTrails();
        trailsAdapter.updateList(trailsList);
    }

    private void saveTrailToDatabase(Trail trail, List<TrackPoint> points, List<ImageData> images) {
        int trailID = trail.getId();
        dbHelper.insertTrail(trail);

        for (TrackPoint p : points) {
            TrackPoint point = new TrackPoint(
                    trail.getId(), p.getLatitude(), p.getLongitude(),
                    p.getAltitude(), p.getTimestamp()
            );
            dbHelper.insertTrackPoint(point);
        }

        if (images != null && !images.isEmpty()) {
            for (ImageData image : images) {
                image = new ImageData(
                        image.getId(),
                        trailID,
                        image.getImagePath(),
                        image.getLatitude(),
                        image.getLongitude(),
                        image.getTimestamp(),
                        trail.getName()
                );
                dbHelper.insertImage(image);
            }
        }

        Toast.makeText(this, getString(R.string.trail_images_successfully_saved), Toast.LENGTH_SHORT).show();
    }
}

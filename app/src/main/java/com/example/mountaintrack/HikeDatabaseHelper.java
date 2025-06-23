package com.example.mountaintrack;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class HikeDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "hike.db";
    private static final int DATABASE_VERSION = 6;

    public HikeDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Таблица за Trail
        String createTrailsTable = "CREATE TABLE " + "trails" + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "description TEXT, " +
                "isUserCreated INTEGER DEFAULT 0," +
                "isFavorite INTEGER DEFAULT 0, " +
                "duration TEXT NOT NULL, " +
                "distance FLOAT NOT NULL, " +
                "totalAscent DOUBLE NOT NULL," +
                "totalDescent DOUBLE NOT NULL" +
                ");";

        // Таблица за TrackPoint
        String createTrackPointsTable = "CREATE TABLE " + "track_points" + " (" +
                "trailId INTEGER NOT NULL, " +
                "latitude REAL NOT NULL, " +
                "longitude REAL NOT NULL, " +
                "altitude REAL, " +
                "timestamp INTEGER, " +
                "FOREIGN KEY(trailId) REFERENCES " + "trails" + "(id) ON DELETE CASCADE" +
                ");";

        String createImagesTable = "CREATE TABLE images (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "trailId INTEGER NOT NULL, " +
                "imagePath TEXT NOT NULL, " +
                "latitude REAL NOT NULL, " +
                "longitude REAL NOT NULL, " +
                "timestamp INTEGER NOT NULL, " +
                "trailName String NOT NULL" +
                ");";

        db.execSQL(createTrailsTable);
        db.execSQL(createTrackPointsTable);
        db.execSQL(createImagesTable);
        loadDefaultTrails(db);
    }

    private void loadDefaultTrails(SQLiteDatabase db) {
        // Trail 1
        ContentValues trail1 = new ContentValues();
        trail1.put("id", 1);
        trail1.put("name", "Хотел Морени - Черни връх");
        trail1.put("description", "Еко пътека от Хотел Морени до Черни връх във Витоша и обратно");
        trail1.put("isUserCreated", 0);
        trail1.put("isFavorite", 0);
        trail1.put("duration", "01:45");
        trail1.put("distance", 4.9);
        trail1.put("totalAscent", 521);
        trail1.put("totalDescent", 521);

        db.insert("trails", null, trail1);
        insertDefaultTrackPoint(db, 1, 42.588319634222465, 23.293144351729243, 1780, 1704096000L); // Хотел Морени
        insertDefaultTrackPoint(db, 1, 42.590041580471876, 23.293423301468348, 1810, 1704096900L);
        insertDefaultTrackPoint(db, 1, 42.59130536396404, 23.293916827929845, 1860, 1704097800L);
        insertDefaultTrackPoint(db, 1, 42.59181087018608, 23.289131767020564, 1980, 1704098700L);
        insertDefaultTrackPoint(db, 1, 42.586107890354555, 23.283402568532775, 2100, 1704099600L);
        insertDefaultTrackPoint(db, 1, 42.58786149372251, 23.279282695462904, 2200, 1704100500L);
        insertDefaultTrackPoint(db, 1, 42.575475136833994, 23.285848743168014, 2200, 1704101400L);
        insertDefaultTrackPoint(db, 1, 42.56344672498585, 23.280546963274535, 2290, 1704102300L); // Черни връх

        // Trail 2
        ContentValues trail2 = new ContentValues();
        trail2.put("id", 2);
        trail2.put("name", "Златни мостове - Черни връх");
        trail2.put("description", "Еко пътека от местност Златни мостове до Черни връх във Витоша");
        trail2.put("isUserCreated", 0);
        trail2.put("isFavorite", 0);
        trail2.put("duration", "02:35");
        trail2.put("distance", 6.9);
        trail2.put("totalAscent", 941);
        trail2.put("totalDescent", 0);
        db.insert("trails", null, trail2);
        insertDefaultTrackPoint(db, 2, 42.60984865377234, 23.239536957856455, 1398, 1704096000L);//08:00:00//Златни мостове
        insertDefaultTrackPoint(db, 2, 42.60770084587939, 23.24129648698004, 1380, 170409703L);// 08:17:13);
        insertDefaultTrackPoint(db, 2, 42.60281710207536, 23.24187411809524, 1500, 170409806L); // 08:34:26);
        insertDefaultTrackPoint(db, 2, 42.59848933663688, 23.245092768907252, 1650, 170409909L); // 08:51:39);
        insertDefaultTrackPoint(db, 2, 42.59041740905973, 23.246938128716334, 1800, 170410013L); // 09:08:52);//Връх  Конярника
        insertDefaultTrackPoint(db, 2, 42.580724975923474, 23.248289962036942, 1950, 170410116L); // 09:26:05);
        insertDefaultTrackPoint(db, 2, 42.566250665663944, 23.25820340657037, 2100, 170410219L); // 09:43:18);//Заслон Самара
        insertDefaultTrackPoint(db, 2, 42.56327954021298, 23.268159766450175, 2190, 170410323L); // 10:00:31);
        insertDefaultTrackPoint(db, 2, 42.56344672498585, 23.280546963274535, 2250, 170410426L);// 10:17:44);
        insertDefaultTrackPoint(db, 2, 42.56359562410901, 23.28081979307114, 2290, 1704105297L);// 10:34:57 Черни връх
    }

    private void insertDefaultTrackPoint(SQLiteDatabase db, int trailId, double lat, double lon, double alt, long timestamp) {
        ContentValues point = new ContentValues();
        point.put("trailId", trailId);
        point.put("latitude", lat);
        point.put("longitude", lon);
        point.put("altitude", alt);
        point.put("timestamp", timestamp);
        db.insert("track_points", null, point);
    }



    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + "track_points");
        db.execSQL("DROP TABLE IF EXISTS " + "trails");
        db.execSQL("DROP TABLE IF EXISTS images");
        onCreate(db);
    }


    /**
     * This method adds a point to the database.*
     *
     * @param point - object of type TrackPoint to be added to the database.
     */
    public void insertTrackPoint(TrackPoint point) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("trailId", point.getTrailId());
        values.put("latitude", point.getLatitude());
        values.put("longitude", point.getLongitude());
        values.put("altitude", point.getAltitude());
        values.put("timestamp", point.getTimestamp());

        long result = db.insert("track_points", null, values);

        if (result == -1) {
            Log.e("DB_ERROR", "Failed to insert track point! trailId=" + point.getTrailId());
        } else {
            Log.d("DB", "Inserted track point ID=" + result + " for trailId=" + point.getTrailId());
        }
    }

    public void setTrailFavoriteStatus(int trailId, boolean isFavorite) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("isFavorite", isFavorite ? 1 : 0);
        db.update("trails", values, "id = ?", new String[]{String.valueOf(trailId)});
        db.close();
    }


    public void insertTrail(Trail trail) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("name", trail.getName());
        values.put("description", trail.getDescription());
        values.put("isUserCreated", trail.isUserCreated() ? 1 : 0);
        values.put("isFavorite", trail.isFavorite() ? 1 : 0);
        values.put("duration", trail.getDuration());
        values.put("distance", trail.getDistance());
        values.put("totalAscent", trail.getTotalAscent());
        values.put("totalDescent", trail.getTotalDescent());


        long result = db.insert("trails", null, values);
        if (result == -1) {
            Log.e("DB", "Неуспешен запис на Trail: " + trail.getName());
        } else {
            Log.d("DB", "Успешно записан Trail: " + trail.getName());
        }
    }


    /**
     * This method returns a list of points for a specific trail id from the database if it exists.
     *
     * @param trailId - id of the trail that should be returned
     * @return list of points of the selected trail
     */
    public ArrayList<TrackPoint> getTrackPointsByTrailId(int trailId) {
        ArrayList<TrackPoint> points = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM track_points WHERE trailId = ?",
                new String[]{String.valueOf(trailId)}
        );

        if (cursor.moveToFirst()) {
            do {
                int trailID = cursor.getInt(cursor.getColumnIndexOrThrow("trailId"));
                double lat = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude"));
                double lon = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"));
                double altitude = cursor.getDouble(cursor.getColumnIndexOrThrow("altitude"));
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"));

                points.add(new TrackPoint(trailID, lat, lon, altitude, timestamp));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return points;
    }

    /**
     * This method compares each ID with the next (id + 1) to find the first gap.
     *
     * Example: if existing IDs are 1, 2, 4 → it will find 3 as the next available.
     *
     * If there are no gaps, it returns MAX(id) + 1 as fallback.
     *
     * */
    public int getNextAvailableTrailId() {
        SQLiteDatabase db = this.getReadableDatabase();

        // Find the smallest unused ID (starting from 1)
        Cursor cursor = db.rawQuery(
                "SELECT MIN(t1.id + 1) AS nextId " +
                        "FROM trails t1 " +
                        "LEFT JOIN trails t2 ON t2.id = t1.id + 1 " +
                        "WHERE t2.id IS NULL", null);

        int nextId = 1; // default if table is empty
        if (cursor.moveToFirst() && !cursor.isNull(0)) {
            nextId = cursor.getInt(0);
        }

        cursor.close();
        return nextId;
    }


    public void deleteTrackPointsByTrailId(int trailId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("track_points", "trailId = ?", new String[]{String.valueOf(trailId)});
    }

    public void deleteTrailById(int trailId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("trails", "id = ?", new String[]{String.valueOf(trailId)});
    }


    public List<Trail> getAllTrails() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM trails",
                null
        );
        return this.parseTrails(cursor);
    }

    public List<Trail> getUserTrails() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM trails WHERE isUserCreated = 1",
                null
        );
        return parseTrails(cursor);
    }

    public List<Trail> getFavoriteTrails() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM trails WHERE isFavorite = 1",
                null
        );
        return parseTrails(cursor);
    }

    private List<Trail> parseTrails(Cursor cursor) {
        List<Trail> trails = new ArrayList<>();

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String description = cursor.getString(cursor.getColumnIndexOrThrow("description"));
                boolean isUserCreated = cursor.getInt(cursor.getColumnIndexOrThrow("isUserCreated")) == 1;
                boolean isFavorite = cursor.getInt(cursor.getColumnIndexOrThrow("isFavorite")) == 1;
                String duration = cursor.getString(cursor.getColumnIndexOrThrow("duration"));
                float distance = cursor.getFloat(cursor.getColumnIndexOrThrow("distance"));
                double totalAscent = cursor.getDouble(cursor.getColumnIndexOrThrow("totalAscent"));
                double totalDescent = cursor.getDouble(cursor.getColumnIndexOrThrow("totalDescent"));

                trails.add(new Trail(id, name, description, isUserCreated, isFavorite, duration, distance, totalAscent, totalDescent));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return trails;
    }

    public void insertImage(ImageData image) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("trailId", image.getTrailId());
        values.put("imagePath", image.getImagePath());
        values.put("latitude", image.getLatitude());
        values.put("longitude", image.getLongitude());
        values.put("timestamp", image.getTimestamp());
        values.put("trailName", image.getTrailName());

        long result = db.insert("images", null, values);
        if (result == -1) {
            Log.e("DB", "Неуспешен запис на изображение");
        } else {
            Log.d("DB", "Успешен запис на изображение с ID = " + result);
        }
    }


    public void deleteImageById(int imageId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete("images", "id = ?", new String[]{String.valueOf(imageId)});
        Log.d("DB", "Изтрити изображения: " + rows);
    }

    public ArrayList<ImageData> getAllImages() {
        ArrayList<ImageData> images = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM images", null);
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                int trailId = cursor.getInt(cursor.getColumnIndexOrThrow("trailId"));
                String imagePath = cursor.getString(cursor.getColumnIndexOrThrow("imagePath"));
                double latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude"));
                double longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"));
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"));
                String trailName = cursor.getString(cursor.getColumnIndexOrThrow("trailName"));

                ImageData imageData = new ImageData(id, trailId, imagePath, latitude, longitude, timestamp, trailName);
                images.add(imageData);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return images;
    }

    public ArrayList<ImageData> getImagesByTrailId(int trailId) {
        ArrayList<ImageData> images = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(
                "images",
                null,
                "trailId = ?",
                new String[]{String.valueOf(trailId)},
                null,
                null,
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String imagePath = cursor.getString(cursor.getColumnIndexOrThrow("imagePath"));
                double latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude"));
                double longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"));
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"));
                String trailName = cursor.getString(cursor.getColumnIndexOrThrow("trailName"));

                ImageData image = new ImageData(id, trailId, imagePath, latitude, longitude, timestamp, trailName);
                images.add(image);
            } while (cursor.moveToNext());
            cursor.close();
        }

        return images;
    }

    public void deleteImagesByTrailId(int trailId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int deleted = db.delete("images", "trailId = ?", new String[]{String.valueOf(trailId)});

        Log.d("DB", "Изтрити изображения за trailId = " + trailId + ": " + deleted);
    }


}

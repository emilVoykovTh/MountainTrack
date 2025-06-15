package com.example.mountaintrack;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;

import java.io.IOException;

public class ImageData implements Parcelable {
    private int id;
    private int trailId;
    private String imagePath;
    private double latitude;
    private double longitude;
    private long timestamp;
    private String trailName;

    public ImageData(int id, int trailId, String imagePath, double latitude, double longitude, long timestamp, String trailName) {
        this.id = id;
        this.trailId = trailId;
        this.imagePath = imagePath;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.trailName = trailName;
    }

    // Parcelable constructor
    protected ImageData(Parcel in) {
        id = in.readInt();
        trailId = in.readInt();
        imagePath = in.readString();
        latitude = in.readDouble();
        longitude = in.readDouble();
        timestamp = in.readLong();
        trailName = in.readString();
    }

    // Parcelable.Creator implementation
    public static final Creator<ImageData> CREATOR = new Creator<ImageData>() {
        @Override
        public ImageData createFromParcel(Parcel in) {
            return new ImageData(in);
        }

        @Override
        public ImageData[] newArray(int size) {
            return new ImageData[size];
        }
    };

    // Parcelable interface methods
    @Override
    public int describeContents() {
        return 0; // no special objects
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(id);
        parcel.writeInt(trailId);
        parcel.writeString(imagePath);
        parcel.writeDouble(latitude);
        parcel.writeDouble(longitude);
        parcel.writeLong(timestamp);
        parcel.writeString(trailName);
    }


    public int getId() {
        return id;
    }

    public int getTrailId() {
        return trailId;
    }

    public String getImagePath() {
        return imagePath;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getTrailName() {
        return trailName;
    }

    public Uri getImageUri() {
        return Uri.parse(imagePath);
    }

    public Bitmap getBitmap(Context context) {
        try {
            Uri uri = Uri.parse(imagePath);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.Source source = ImageDecoder.createSource(context.getContentResolver(), uri);
                return ImageDecoder.decodeBitmap(source);
            } else {
                return MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}

package com.example.mountaintrack;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

public class TrackPoint implements Parcelable {
    private int trailId;
    private double latitude;
    private double longitude;
    private double altitude;
    private long timestamp;

    public TrackPoint(int trailId, double latitude, double longitude, double altitude, long timestamp) {
        this.trailId = trailId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.timestamp = timestamp;
    }

    protected TrackPoint(Parcel in) {
        trailId = in.readInt();
        latitude = in.readDouble();
        longitude = in.readDouble();
        altitude = in.readDouble();
        timestamp = in.readLong();
    }

    public static final Creator<TrackPoint> CREATOR = new Creator<TrackPoint>() {
        @Override
        public TrackPoint createFromParcel(Parcel in) {
            return new TrackPoint(in);
        }

        @Override
        public TrackPoint[] newArray(int size) {
            return new TrackPoint[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(trailId);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeDouble(altitude);
        dest.writeLong(timestamp);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public int getTrailId() { return trailId; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public double getAltitude() { return altitude; }
    public long getTimestamp() { return timestamp; }

    public LatLng getLatLng() {
        LatLng latLng = new LatLng(this.getLatitude(), this.getLongitude());
        return latLng;
    }
}
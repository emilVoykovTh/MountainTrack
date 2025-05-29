package com.example.mountaintrack;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    private List<ImageData> imageList;
    private Context context;
    private OnImageClickListener listener;

    public ImageAdapter(Context context, List<ImageData> imageList, OnImageClickListener listener) {
        this.context = context;
        this.imageList = new ArrayList<>(imageList);
        this.listener = listener;
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        Button btnViewTrail;
        ImageButton btnDelete;
        TextView fieldTrail;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image);
            btnViewTrail = itemView.findViewById(R.id.btnViewTrail);
            btnDelete = itemView.findViewById(R.id.btnDeleteImage);
            fieldTrail = itemView.findViewById(R.id.fieldTrail);
        }
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        ImageData imageData = imageList.get(position);
        String path = imageData.getImagePath();
        File imgFile = new File(path);
        if (imgFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            if (bitmap != null) {
                holder.image.setImageBitmap(bitmap);
            } else {
                holder.image.setImageResource(R.drawable.image_placeholder);
            }
        } else {
            holder.image.setImageResource(R.drawable.image_placeholder);
        }


        // Set trail name
        holder.fieldTrail.setText(imageData.getNearestTrailName());

        // View on map button
        holder.btnViewTrail.setOnClickListener(v -> listener.onViewImageOnMap(imageData));

        // Delete button
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteImage(imageData, position));
    }

    @Override
    public int getItemCount() {
        return imageList.size();
    }

    public interface OnImageClickListener {
        void onViewImageOnMap(ImageData imageData);
        void onDeleteImage(ImageData imageData, int position);
    }

    /**
     * Replace current list of images and refresh.
     */
    public void updateImageList(List<ImageData> newImages) {
        this.imageList = new ArrayList<>(newImages);
        notifyDataSetChanged();
    }
}

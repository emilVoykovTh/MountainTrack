package com.example.mountaintrack;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrailAdapter extends RecyclerView.Adapter<TrailAdapter.TrailViewHolder> {
    private List<Trail> trailList;
    private final Context context;
    private final OnTrailClickListener listener;
    private final ImageProvider imageProvider;
    private final Map<Integer, Integer> imageIndices = new HashMap<>();
    public TrailAdapter(Context context, List<Trail> trailList, OnTrailClickListener listener, ImageProvider imageProvider) {
        this.context = context;
        this.trailList = new ArrayList<>(trailList);
        this.listener = listener;
        this.imageProvider = imageProvider;
    }

    @NonNull
    @Override
    public TrailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_trail, parent, false);
        return new TrailViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrailViewHolder holder, int position) {
        Trail trail = trailList.get(position);
        int trailId = trail.getId();
        holder.textTrailName.setText(trail.getName());

        //textDistance, textAscend, textDescent, textDuration;
        holder.textDistance.setText(String.format("%.1f m", trail.getDistance()));
        holder.textAscend.setText(String.format("↑%.2f m", trail.getTotalAscent()));
        holder.textDescent.setText(String.format("↓%.2f m", trail.getTotalDescent()));
        holder.textDuration.setText(trail.getDuration());

        holder.btnViewTrail.setOnClickListener(v -> listener.onViewTrailClick(trail));

        holder.btnAddOrRemoveFromFavorites.setImageResource(
                trail.isFavorite() ? R.drawable.trashbin_button : R.drawable.add_to_favorites
        );
        holder.btnAddOrRemoveFromFavorites.setOnClickListener(v ->
                listener.onFavoriteClick(trail, holder.getAdapterPosition())
        );

        holder.itemView.setOnLongClickListener(v -> {
            listener.onDeleteTrailRequest(trail, holder.getAdapterPosition());
            return true;
        });

        // Image logic
        List<ImageData> imageList = imageProvider.getImagesForTrail(trailId);
        if (imageList == null || imageList.isEmpty()) {
            Glide.with(context)
                    .load(R.drawable.black_peak)
                    .into(holder.imageTrail);
            holder.imageTrail.setOnClickListener(null);
        } else {
            // Initialize index if not present
            imageIndices.putIfAbsent(trailId, 0);
            int index = imageIndices.get(trailId);

            // Load current image
            Glide.with(context)
                    .load(imageList.get(index).getImagePath())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(holder.imageTrail);

            holder.imageTrail.setOnClickListener(v -> {
                // Move to next image, or loop back to start
                int newIndex = (imageIndices.get(trailId) + 1) % imageList.size();
                imageIndices.put(trailId, newIndex);

                Glide.with(context)
                        .load(imageList.get(newIndex).getImagePath())
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(holder.imageTrail);
            });
        }
    }

    @Override
    public int getItemCount() {
        return trailList.size();
    }

    public void updateList(List<Trail> newList) {
        this.trailList = new ArrayList<>(newList);
        imageIndices.clear();
        notifyDataSetChanged();
    }

    public static class TrailViewHolder extends RecyclerView.ViewHolder {
        TextView textTrailName, textDistance, textAscend, textDescent, textDuration;
        ImageView imageTrail;
        Button btnViewTrail;
        ImageButton btnAddOrRemoveFromFavorites;

        public TrailViewHolder(@NonNull View itemView) {
            super(itemView);
            textTrailName = itemView.findViewById(R.id.textTrailName);
            imageTrail = itemView.findViewById(R.id.imageTrail);
            btnViewTrail = itemView.findViewById(R.id.btnViewTrail);
            textDistance = itemView.findViewById(R.id.textDistance);
            textAscend = itemView.findViewById(R.id.textAscent);
            textDescent = itemView.findViewById(R.id.textDescent);
            textDuration = itemView.findViewById(R.id.textDuration);
            btnAddOrRemoveFromFavorites = itemView.findViewById(R.id.btnAddOrRemoveFromFavorites);
        }
    }

    public interface OnTrailClickListener {
        void onFavoriteClick(Trail trail, int position);
        void onViewTrailClick(Trail trail);
        void onDeleteTrailRequest(Trail trail, int position);
    }

    public interface ImageProvider {
        List<ImageData> getImagesForTrail(int trailId);
    }
}

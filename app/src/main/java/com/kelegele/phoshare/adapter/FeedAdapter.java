package com.kelegele.phoshare.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextSwitcher;

import androidx.recyclerview.widget.RecyclerView;

import com.kelegele.phoshare.R;
import com.kelegele.phoshare.view.SquaredImageView;
import com.kelegele.phoshare.Utils;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FeedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener{
    private static final int ANIMATED_ITEMS_COUNT = 2;

    private OnFeedItemClickListener onFeedItemClickListener;
    private final Map<Integer, Integer> likesCount = new HashMap<>();

    private Context context;
    private int lastAnimatedPosition = -1;
    private int itemsCount = 0;

    @BindView(R.id.ivFeedCenter)
    ImageView ivFeedCenter;
    @BindView(R.id.ivFeedBottom)
    ImageView ivFeedBottom;
    @BindView(R.id.btnComments)
    ImageButton btnComments;
    @BindView(R.id.btnLike)
    ImageButton btnLike;
//    @BindView(R.id.vBgLike)
//    View vBgLike;
//    @BindView(R.id.ivLike)
//    ImageView ivLike;
    @BindView(R.id.tsLikesCounter)
    TextSwitcher tsLikesCounter;

    public FeedAdapter(Context context) {
        this.context = context;
    }

    @NotNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(context).inflate(R.layout.item_feed, parent, false);
        return new CellFeedViewHolder(view);
    }

    private void runEnterAnimation(View view, int position) {
        if (position >= ANIMATED_ITEMS_COUNT - 1) {
            return;
        }

        if (position > lastAnimatedPosition) {
            lastAnimatedPosition = position;
            view.setTranslationY(Utils.getScreenHeight(context));
            view.animate()
                    .translationY(0)
                    .setInterpolator(new DecelerateInterpolator(3.f))
                    .setDuration(700)
                    .start();
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        runEnterAnimation(viewHolder.itemView, position);
        CellFeedViewHolder holder = (CellFeedViewHolder) viewHolder;
        if (position % 2 == 0) {
            holder.ivFeedCenter.setImageResource(R.drawable.img_feed_center_1);
            holder.ivFeedBottom.setImageResource(R.drawable.img_feed_bottom_1);
        } else {
            holder.ivFeedCenter.setImageResource(R.drawable.img_feed_center_2);
            holder.ivFeedBottom.setImageResource(R.drawable.img_feed_bottom_2);
        }

        holder.ivFeedBottom.setOnClickListener(this);
        holder.ivFeedBottom.setTag(position);
    }

    @Override
    public int getItemCount() {
        return itemsCount;
    }

//    private void updateLikesCounter(CellFeedViewHolder holder, boolean animated) {
//        int currentLikesCount = likesCount.get(holder.getPosition()) + 1;
//        String likesCountText = context.getResources().getQuantityString(
//                R.plurals.likes_count, currentLikesCount, currentLikesCount
//        );
//
//        if (animated) {
//            holder.tsLikesCounter.setText(likesCountText);
//        } else {
//            holder.tsLikesCounter.setCurrentText(likesCountText);
//        }
//
//        likesCount.put(holder.getPosition(), currentLikesCount);
//    }

    public static class CellFeedViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.ivFeedCenter)
        SquaredImageView ivFeedCenter;
        @BindView(R.id.ivFeedBottom)
        ImageView ivFeedBottom;

        public CellFeedViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

    public void updateItems() {
        itemsCount = 10;
        //animateItems = animated;
        fillLikesWithRandomValues();
        notifyDataSetChanged();
    }
    private void fillLikesWithRandomValues() {
        for (int i = 0; i < getItemCount(); i++) {
            likesCount.put(i, new Random().nextInt(100));
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.ivFeedBottom) {
            if (onFeedItemClickListener != null) {
                onFeedItemClickListener.onCommentsClick(v, (Integer) v.getTag());
            }
        }
    }

    public void setOnFeedItemClickListener(OnFeedItemClickListener onFeedItemClickListener) {
        this.onFeedItemClickListener = onFeedItemClickListener;
    }

    public interface OnFeedItemClickListener {
        public void onCommentsClick(View v, int position);
    }


    public static class FeedItem {
        public int likesCount;
        public boolean isLiked;

        public FeedItem(int likesCount, boolean isLiked) {
            this.likesCount = likesCount;
            this.isLiked = isLiked;
        }
    }
}

package com.kelegele.phoshare.adapter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.kelegele.phoshare.R;
import com.kelegele.phoshare.Utils;
import com.kelegele.phoshare.activity.LoginActivity;
import com.kelegele.phoshare.utils.CircleTransformation;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.leancloud.AVFile;
import cn.leancloud.AVObject;
import cn.leancloud.AVQuery;
import cn.leancloud.AVUser;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

import static com.kelegele.phoshare.Utils.isFastClick;

public class FeedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener {
    private static final int ANIMATED_ITEMS_COUNT = 2;

    private static final int LIKE_ADD = 0;
    private static final int LIKE_SUB = 1;
    private static final int LIKE_SETUP = 2;


    private static final DecelerateInterpolator DECCELERATE_INTERPOLATOR = new DecelerateInterpolator();
    private static final AccelerateInterpolator ACCELERATE_INTERPOLATOR = new AccelerateInterpolator();
    private static final OvershootInterpolator OVERSHOOT_INTERPOLATOR = new OvershootInterpolator(4);

    private OnFeedItemClickListener onFeedItemClickListener;
    private final Map<Integer, Integer> likesCount = new HashMap<>();

    private final Map<RecyclerView.ViewHolder, AnimatorSet> likeAnimations = new HashMap<>();
    private final ArrayList<Integer> likedPositions = new ArrayList<>();

    private Context context;
    private int lastAnimatedPosition = -1;
    private int itemsCount = 0;
    private List<AVObject> feedList;

    @BindView(R.id.ivFeedCenter)
    ImageView ivFeedCenter;
    @BindView(R.id.tvFeedBottom)
    TextView tvFeedBottom;
    @BindView(R.id.btnComments)
    ImageButton btnComments;
    @BindView(R.id.btnLike)
    ImageButton btnLike;
    @BindView(R.id.vBgLike)
    View vBgLike;
    @BindView(R.id.ivLike)
    ImageView ivLike;
    @BindView(R.id.tsLikesCounter)
    TextSwitcher tsLikesCounter;


    public FeedAdapter(Context context) {
        this.context = context;
    }

    @NotNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(context).inflate(R.layout.item_feed, parent, false);
        final CellFeedViewHolder cellFeedViewHolder = new CellFeedViewHolder(view);
        cellFeedViewHolder.btnComments.setOnClickListener(this);
        cellFeedViewHolder.ivFeedCenter.setOnClickListener(this);
        cellFeedViewHolder.btnLike.setOnClickListener(this);
        cellFeedViewHolder.ivOwnerAvatar.setOnClickListener(this);
        return cellFeedViewHolder;
    }

    @Override
    public void onBindViewHolder(@NotNull RecyclerView.ViewHolder viewHolder, int position) {
        runEnterAnimation(viewHolder.itemView, position);
        CellFeedViewHolder holder = (CellFeedViewHolder) viewHolder;


        AVObject item = feedList.get(position);
        holder.tvUserName.setText(item.getString("ownerName"));

        AVFile avatarFile = item.getAVFile("ownerAvatar");
        String avatarUrl = null;
        int avatarSize = context.getResources().getDimensionPixelSize(R.dimen.feed_owner_avatar_size);
        if (avatarFile == null) {
            avatarUrl = context.getResources().getString(R.string.user_profile_photo);
        } else {
            avatarUrl = avatarFile.getUrl();
        }

        //加载头像
        Picasso.get()
                .load(avatarUrl)
                .placeholder(R.drawable.img_circle_placeholder)
                .resize(avatarSize, avatarSize)
                .centerCrop()
                .transform(new CircleTransformation())
                .into(holder.ivOwnerAvatar);

        //加载图片
        AVFile imgFile = item.getAVFile("image");
        if (imgFile == null) {
            holder.ivFeedCenter.setImageResource(R.drawable.img_phoshare_black);
        } else {
            Picasso.get()
                    .load(imgFile.getUrl())
                    .placeholder(R.drawable.img_toolbar_logo)
                    .fit()
                    .centerCrop()
                    .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                    .into(holder.ivFeedCenter);
        }

        //加载图片介绍
        holder.tvFeedBottom.setText(item.getString("description"));

        //获取feedCommentsId
       holder.feedCommentsId = item.getAVObject("comments").getObjectId();


        updateLikesCounter(holder, false, LIKE_SETUP);
        updateHeartButton(holder, false);

        AVQuery<AVObject> unlikeQuery = new AVQuery<>("Likes");
        unlikeQuery.whereEqualTo("userLike", AVUser.getCurrentUser());
        unlikeQuery.whereEqualTo("feedLiked",feedList.get(holder.getAdapterPosition()).getObjectId());
        unlikeQuery.getFirstInBackground().subscribe(new Observer<AVObject>() {
            @Override
            public void onSubscribe(Disposable d) { }

            @Override
            public void onNext(AVObject avObject) {
                if (!likedPositions.contains(holder.getAdapterPosition())) {
                    likedPositions.add(holder.getAdapterPosition());
                    updateHeartButton(holder, true);
                }
            }

            @Override
            public void onError(Throwable e) { }

            @Override
            public void onComplete() { }
        });

        holder.btnComments.setTag(holder);
        holder.ivFeedCenter.setTag(holder);
        holder.btnLike.setTag(holder);

        if (likeAnimations.containsKey(holder)) {
            Objects.requireNonNull(likeAnimations.get(holder)).cancel();
        }
        resetLikeAnimationState(holder);
    }

    @Override
    public int getItemCount() {
        return itemsCount;
    }

    private void updateLikesCounter(CellFeedViewHolder holder, boolean animated, int action) {

        int currentLikesCount = likesCount.get(holder.getAdapterPosition());
        AVObject toUpdate = AVObject.createWithoutData("Feeds", feedList.get(holder.getAdapterPosition()).getObjectId());

        switch (action) {
            case LIKE_ADD:
                currentLikesCount++;
                toUpdate.increment("likedCount", 1);
                break;
            case LIKE_SUB:
                currentLikesCount--;
                toUpdate.increment("likedCount", -1);
                break;
            case LIKE_SETUP:
                break;
            default:
                break;
        }

        if (action == LIKE_ADD || action == LIKE_SUB) {
            toUpdate.saveInBackground().subscribe(new Observer<AVObject>() {
                @Override
                public void onSubscribe(Disposable d) { }

                @Override
                public void onNext(AVObject avObject) {
                    if(action == LIKE_ADD)
                    {
                        AVObject like = new AVObject("Likes");
                        like.put("userLike", AVUser.getCurrentUser());
                        like.put("feedLiked",feedList.get(holder.getAdapterPosition()).getObjectId());
                        like.put("feedOwner",feedList.get(holder.getAdapterPosition()).get("owner"));
                        like.saveInBackground().subscribe();
                    }
                    else if(action == LIKE_SUB)
                    {
                        AVQuery<AVObject> unlikeQuery = new AVQuery<>("Likes");
                        unlikeQuery.whereEqualTo("userLike", AVUser.getCurrentUser());
                        unlikeQuery.whereEqualTo("feedLiked",feedList.get(holder.getAdapterPosition()).getObjectId());
                        unlikeQuery.getFirstInBackground().subscribe(new Observer<AVObject>() {
                            @Override
                            public void onSubscribe(Disposable d) { }

                            @Override
                            public void onNext(AVObject todo) {
                                String unlikeId = todo.getObjectId();
                                AVObject unlikeObj = AVObject.createWithoutData("Likes",unlikeId);
                                unlikeObj.deleteInBackground().subscribe();
                            }

                            @Override
                            public void onError(Throwable e) { }

                            @Override
                            public void onComplete() { }
                        });
                    }

                }

                @Override
                public void onError(Throwable e) { }

                @Override
                public void onComplete() { }
            });

        }

        String likesCountText = context.getResources().getQuantityString(
                R.plurals.likes_count, currentLikesCount, currentLikesCount
        );

        if (animated) {
            holder.tsLikesCounter.setText(likesCountText);
        } else {
            holder.tsLikesCounter.setCurrentText(likesCountText);
        }


//        AVObject feed = feedList.get(position);
//        feed.increment("likedCount",sum);
//        feed.save();


        likesCount.put(holder.getAdapterPosition(), currentLikesCount);
    }



    public static class CellFeedViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.ivFeedCenter)
        ImageView ivFeedCenter;
        @BindView(R.id.tvFeedBottom)
        TextView tvFeedBottom;
        @BindView(R.id.btnComments)
        ImageButton btnComments;
        @BindView(R.id.btnLike)
        ImageButton btnLike;
        @BindView(R.id.vBgLike)
        View vBgLike;
        @BindView(R.id.ivLike)
        ImageView ivLike;
        @BindView(R.id.tsLikesCounter)
        TextSwitcher tsLikesCounter;
        @BindView(R.id.tvUserName)
        TextView tvUserName;
        @BindView(R.id.ivOwnerAvatar)
        ImageView ivOwnerAvatar;
        @BindView(R.id.vImageRoot)
        FrameLayout vImageRoot;

        FeedItem feedItem;

        String feedCommentsId;

        public CellFeedViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }

//        public void bindView(FeedItem feedItem) {
//            this.feedItem = feedItem;
//            int adapterPosition = getAdapterPosition();
//            ivFeedCenter.setImageResource(adapterPosition % 2 == 0 ? R.drawable.img_feed_center_1 : R.drawable.img_feed_center_2);
//            //ivFeedBottom.setImageResource(adapterPosition % 2 == 0 ? R.drawable.img_feed_bottom_1 : R.drawable.img_feed_bottom_2);
//            btnLike.setImageResource(feedItem.isLiked ? R.drawable.ic_heart_red : R.drawable.ic_heart_outline_grey);
//            tsLikesCounter.setCurrentText(vImageRoot.getResources().getQuantityString(
//                    R.plurals.likes_count, feedItem.likesCount, feedItem.likesCount
//            ));
//        }

        public FeedItem getFeedItem() {
            return feedItem;
        }
    }

    public interface OnFeedItemClickListener {
        void onCommentsClick(View v, String feedCommentId);

        void onProfileClick(View v);

        void onUpdateFeeds(View v);
    }

    private void updateHeartButton(final CellFeedViewHolder holder, boolean animated) {
        if (animated) {
            if (!likeAnimations.containsKey(holder)) {
                AnimatorSet animatorSet = new AnimatorSet();
                likeAnimations.put(holder, animatorSet);

                ObjectAnimator rotationAnim = ObjectAnimator.ofFloat(holder.btnLike, "rotation", 0f, 360f);
                rotationAnim.setDuration(300);
                rotationAnim.setInterpolator(ACCELERATE_INTERPOLATOR);

                ObjectAnimator bounceAnimX = ObjectAnimator.ofFloat(holder.btnLike, "scaleX", 0.2f, 1f);
                bounceAnimX.setDuration(300);
                bounceAnimX.setInterpolator(OVERSHOOT_INTERPOLATOR);

                ObjectAnimator bounceAnimY = ObjectAnimator.ofFloat(holder.btnLike, "scaleY", 0.2f, 1f);
                bounceAnimY.setDuration(300);
                bounceAnimY.setInterpolator(OVERSHOOT_INTERPOLATOR);
                bounceAnimY.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        holder.btnLike.setImageResource(R.drawable.ic_heart_red);
                    }
                });

                animatorSet.play(rotationAnim);
                animatorSet.play(bounceAnimX).with(bounceAnimY).after(rotationAnim);

                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        resetLikeAnimationState(holder);
                    }
                });

                animatorSet.start();
            }
        } else {
            if (likedPositions.contains(holder.getAdapterPosition())) {
                holder.btnLike.setImageResource(R.drawable.ic_heart_red);
            } else {
                holder.btnLike.setImageResource(R.drawable.ic_heart_outline_grey);
            }
        }
    }

    private void animatePhotoLike(final CellFeedViewHolder holder) {
        holder.vBgLike.setVisibility(View.VISIBLE);
        holder.ivLike.setVisibility(View.VISIBLE);

        holder.vBgLike.setScaleY(0.1f);
        holder.vBgLike.setScaleX(0.1f);
        holder.vBgLike.setAlpha(1f);
        holder.ivLike.setScaleY(0.1f);
        holder.ivLike.setScaleX(0.1f);

        AnimatorSet animatorSet = new AnimatorSet();
        likeAnimations.put(holder, animatorSet);

        ObjectAnimator bgScaleYAnim = ObjectAnimator.ofFloat(holder.vBgLike, "scaleY", 0.1f, 1f);
        bgScaleYAnim.setDuration(200);
        bgScaleYAnim.setInterpolator(DECCELERATE_INTERPOLATOR);
        ObjectAnimator bgScaleXAnim = ObjectAnimator.ofFloat(holder.vBgLike, "scaleX", 0.1f, 1f);
        bgScaleXAnim.setDuration(200);
        bgScaleXAnim.setInterpolator(DECCELERATE_INTERPOLATOR);
        ObjectAnimator bgAlphaAnim = ObjectAnimator.ofFloat(holder.vBgLike, "alpha", 1f, 0f);
        bgAlphaAnim.setDuration(200);
        bgAlphaAnim.setStartDelay(150);
        bgAlphaAnim.setInterpolator(DECCELERATE_INTERPOLATOR);

        ObjectAnimator imgScaleUpYAnim = ObjectAnimator.ofFloat(holder.ivLike, "scaleY", 0.1f, 1f);
        imgScaleUpYAnim.setDuration(300);
        imgScaleUpYAnim.setInterpolator(DECCELERATE_INTERPOLATOR);
        ObjectAnimator imgScaleUpXAnim = ObjectAnimator.ofFloat(holder.ivLike, "scaleX", 0.1f, 1f);
        imgScaleUpXAnim.setDuration(300);
        imgScaleUpXAnim.setInterpolator(DECCELERATE_INTERPOLATOR);

        ObjectAnimator imgScaleDownYAnim = ObjectAnimator.ofFloat(holder.ivLike, "scaleY", 1f, 0f);
        imgScaleDownYAnim.setDuration(300);
        imgScaleDownYAnim.setInterpolator(ACCELERATE_INTERPOLATOR);
        ObjectAnimator imgScaleDownXAnim = ObjectAnimator.ofFloat(holder.ivLike, "scaleX", 1f, 0f);
        imgScaleDownXAnim.setDuration(300);
        imgScaleDownXAnim.setInterpolator(ACCELERATE_INTERPOLATOR);

        animatorSet.playTogether(bgScaleYAnim, bgScaleXAnim, bgAlphaAnim, imgScaleUpYAnim, imgScaleUpXAnim);
        animatorSet.play(imgScaleDownYAnim).with(imgScaleDownXAnim).after(imgScaleUpYAnim);

        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                resetLikeAnimationState(holder);
            }
        });
        animatorSet.start();
    }

    public void updateItems(List<AVObject> list) {
        itemsCount = list.size();
        feedList = list;
        initLikedCount();
        notifyDataSetChanged();
    }

    private void initLikedCount() {
        for (int i = 0; i < getItemCount(); i++) {
            likesCount.put(i, feedList.get(i).getInt("likedCount"));
        }
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
    public void onClick(View view) {
        if (!isFastClick() && LoginActivity.checkLogin((Activity) context)) {
            clickViewID(view);
        }
    }

    private void clickViewID(View view) {

        final int viewId = view.getId();
        if (viewId == R.id.btnComments) {       //点击评论
            if (onFeedItemClickListener != null) {
                CellFeedViewHolder holder = (CellFeedViewHolder) view.getTag();
                onFeedItemClickListener.onCommentsClick(view, holder.feedCommentsId);
            }
        } else if (viewId == R.id.btnLike) {    //点击爱心
            CellFeedViewHolder holder = (CellFeedViewHolder) view.getTag();
            if (!likedPositions.contains(holder.getAdapterPosition())) {
                likedPositions.add(holder.getAdapterPosition());
                updateLikesCounter(holder, true, LIKE_ADD);
                updateHeartButton(holder, true);
            } else {
                likedPositions.clear();
                holder.btnLike.setImageResource(R.drawable.ic_heart_outline_grey);
                updateLikesCounter(holder, true, LIKE_SUB);

            }
        } else if (viewId == R.id.ivFeedCenter) {   //点击图片
            CellFeedViewHolder holder = (CellFeedViewHolder) view.getTag();
            if (!likedPositions.contains(holder.getAdapterPosition())) {
                likedPositions.add(holder.getAdapterPosition());
                updateLikesCounter(holder, true, LIKE_ADD);
                updateHeartButton(holder, true);
                animatePhotoLike(holder);

            }
        } else if (viewId == R.id.ivUserAvatar) {//用户
            if (onFeedItemClickListener != null) {
                onFeedItemClickListener.onProfileClick(view);
            }
        }
    }

    private void resetLikeAnimationState(CellFeedViewHolder holder) {
        likeAnimations.remove(holder);
        holder.vBgLike.setVisibility(View.GONE);
        holder.ivLike.setVisibility(View.GONE);
    }

    public void setOnFeedItemClickListener(OnFeedItemClickListener onFeedItemClickListener) {
        this.onFeedItemClickListener = onFeedItemClickListener;
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

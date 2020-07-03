package com.kelegele.phoshare.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.tabs.TabLayout;
import com.kelegele.phoshare.R;
import com.kelegele.phoshare.adapter.UserProfileAdapter;
import com.kelegele.phoshare.utils.CircleTransformation;
import com.kelegele.phoshare.view.RevealBackgroundView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.InputStream;

import butterknife.BindView;
import butterknife.OnClick;
import cn.leancloud.AVFile;
import cn.leancloud.AVObject;
import cn.leancloud.AVQuery;
import cn.leancloud.AVUser;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.operators.observable.ObservableCache;

public class UserProfileActivity extends BaseActivity implements RevealBackgroundView.OnStateChangeListener {

    public static final String ARG_REVEAL_START_LOCATION = "reveal_start_location";

    private static final int USER_OPTIONS_ANIMATION_DELAY = 300;
    private static final int ANIM_DURATION_FAB = 400;
    private static final Interpolator INTERPOLATOR = new DecelerateInterpolator();


    @BindView(R.id.vRevealBackground)
    RevealBackgroundView vRevealBackground;
    @BindView(R.id.rvUserProfile)
    RecyclerView rvUserProfile;

    @BindView(R.id.tlUserProfileTabs)
    TabLayout tlUserProfileTabs;

    @BindView(R.id.ivUserProfilePhoto)
    ImageView ivUserProfilePhoto;
    @BindView(R.id.vUserDetails)
    View vUserDetails;
    @BindView(R.id.vUserStats)
    View vUserStats;
    @BindView(R.id.vUserProfileRoot)
    View vUserProfileRoot;
    @BindView(R.id.btnCreate)
    ImageButton btnCreate;

    @BindView(R.id.userName)
    TextView userName;
    @BindView(R.id.userIntroduce)
    TextView userIntroduce;
    @BindView(R.id.userFeeds)
    TextView userFeeds;
    @BindView(R.id.userLiked)
    TextView userLiked;

    private AVUser user;
    private int avatarSize;
    private String profilePhoto;
    private AVFile avatarFile;
    private UserProfileAdapter userPhotosAdapter;

    public static void startUserProfileFromLocation(int[] startingLocation, Activity startingActivity) {
        Intent intent = new Intent(startingActivity, UserProfileActivity.class);
        intent.putExtra(ARG_REVEAL_START_LOCATION, startingLocation);
        startingActivity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        this.user = AVUser.getCurrentUser();
        this.avatarFile = user.getAVFile("avatar");
        this.avatarSize = getResources().getDimensionPixelSize(R.dimen.user_profile_avatar_size);

        if (avatarFile == null){
            this.profilePhoto = getResources().getString(R.string.user_profile_photo);
        }else {
            this.profilePhoto = avatarFile.getUrl();
        }

        initUserProfile();

        hideToolbar();
        setupTabs();
        setupUserProfileGrid();
        setupRevealBackground(savedInstanceState);

        btnCreate.setTranslationY(2 * getResources().getDimensionPixelOffset(R.dimen.btn_fab_size));
        btnCreate.animate()
                .translationY(0)
                .setInterpolator(new OvershootInterpolator(1.f))
                .setStartDelay(300)
                .setDuration(ANIM_DURATION_FAB)
                .start();
    }

    private void initUserProfile() {

        //查询获得用户发布量及获得喜欢量
        AVQuery<AVObject> feedsQuery = new AVQuery<>("Feeds");
        feedsQuery.whereEqualTo("owner", AVUser.getCurrentUser());
        feedsQuery.countInBackground().subscribe(new Observer<Integer>() {
            public void onSubscribe(Disposable disposable) {}
            public void onNext(Integer count) {
                AVObject updateCount = AVObject.createWithoutData("_User",user.getObjectId());
                updateCount.put("feedsCount",count);
                updateCount.saveInBackground().subscribe();
                userFeeds.setText(String.valueOf(count.intValue()));
            }
            public void onError(Throwable throwable) {}
            public void onComplete() {}
        });

        AVQuery<AVObject> likesQuery = new AVQuery<>("Likes");
        likesQuery.whereEqualTo("feedOwner", AVUser.getCurrentUser());
        likesQuery.countInBackground().subscribe(new Observer<Integer>() {
            public void onSubscribe(Disposable disposable) {}
            public void onNext(Integer count) {
                AVObject updateCount = AVObject.createWithoutData("_User",user.getObjectId());
                updateCount.put("likesCount",count);
                updateCount.saveInBackground().subscribe();
                userLiked.setText(String.valueOf(count.intValue()));
            }
            public void onError(Throwable throwable) {}
            public void onComplete() {}
        });


        //设置用户名和介绍
        userName.setText(user.getUsername());
        userIntroduce.setText(user.getString("introduce"));

        Picasso.get()
                .load(profilePhoto)
                .placeholder(R.drawable.img_circle_placeholder)
                .resize(avatarSize, avatarSize)
                .centerCrop()
                .transform(new CircleTransformation())
                .into(ivUserProfilePhoto, new Callback() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void hideToolbar() {
        assert getToolbar() != null;
        getToolbar().setNavigationIcon(null);
        //getInboxMenuItem().getActionView().setVisibility(View.INVISIBLE);
    }

    private void setupTabs() {
        tlUserProfileTabs.addTab(tlUserProfileTabs.newTab().setIcon(R.drawable.ic_grid_on_white));
        tlUserProfileTabs.addTab(tlUserProfileTabs.newTab().setIcon(R.drawable.ic_list_white));
    }


    private void setupRevealBackground(Bundle savedInstanceState) {
        vRevealBackground.setOnStateChangeListener(this);
        if (savedInstanceState == null) {
            final int[] startingLocation = getIntent().getIntArrayExtra(ARG_REVEAL_START_LOCATION);
            vRevealBackground.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    vRevealBackground.getViewTreeObserver().removeOnPreDrawListener(this);
                    vRevealBackground.startFromLocation(startingLocation);
                    return false;
                }
            });
        } else {
            userPhotosAdapter.setLockedAnimations(true);
            vRevealBackground.setToFinishedFrame();
        }
    }

    private void setupUserProfileGrid() {
        final StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL);
        rvUserProfile.setLayoutManager(layoutManager);
        rvUserProfile.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                userPhotosAdapter.setLockedAnimations(true);
            }
        });
    }

    @Override
    public void onStateChange(int state) {
        if (RevealBackgroundView.STATE_FINISHED == state) {
            rvUserProfile.setVisibility(View.VISIBLE);
            tlUserProfileTabs.setVisibility(View.VISIBLE);
            vUserProfileRoot.setVisibility(View.VISIBLE);
            userPhotosAdapter = new UserProfileAdapter(this);
            rvUserProfile.setAdapter(userPhotosAdapter);
            animateUserProfileOptions();
            animateUserProfileHeader();
        } else {
            tlUserProfileTabs.setVisibility(View.INVISIBLE);
            rvUserProfile.setVisibility(View.INVISIBLE);
            vUserProfileRoot.setVisibility(View.INVISIBLE);
        }
    }

    @OnClick(R.id.btnCreate)
    public void onCreateClick() {
        if (LoginActivity.checkLogin(this)) {
            PublishActivity.openPublisPhoto(this);
            overridePendingTransition(0, 0);
        }
    }

    private void animateUserProfileOptions() {
        tlUserProfileTabs.setTranslationY(-tlUserProfileTabs.getHeight());
        tlUserProfileTabs.animate().translationY(0).setDuration(300).setStartDelay(USER_OPTIONS_ANIMATION_DELAY).setInterpolator(INTERPOLATOR);
    }

    private void animateUserProfileHeader() {
        vUserProfileRoot.setTranslationY(-vUserProfileRoot.getHeight());
        ivUserProfilePhoto.setTranslationY(-ivUserProfilePhoto.getHeight());
        vUserDetails.setTranslationY(-vUserDetails.getHeight());
        vUserStats.setAlpha(0);

        vUserProfileRoot.animate().translationY(0).setDuration(300).setInterpolator(INTERPOLATOR);
        ivUserProfilePhoto.animate().translationY(0).setDuration(300).setStartDelay(100).setInterpolator(INTERPOLATOR);
        vUserDetails.animate().translationY(0).setDuration(300).setStartDelay(200).setInterpolator(INTERPOLATOR);
        vUserStats.animate().alpha(1).setDuration(200).setStartDelay(400).setInterpolator(INTERPOLATOR).start();
    }

}

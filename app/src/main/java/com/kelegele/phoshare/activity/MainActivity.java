package com.kelegele.phoshare.activity;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.OvershootInterpolator;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;


import com.kelegele.phoshare.R;
import com.kelegele.phoshare.Utils;
import com.kelegele.phoshare.adapter.FeedAdapter;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import cn.leancloud.AVException;
import cn.leancloud.AVObject;
import cn.leancloud.AVQuery;
import cn.leancloud.callback.FindCallback;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

import static com.kelegele.phoshare.Utils.isFastClick;

public class MainActivity extends BaseDrawerActivity implements FeedAdapter.OnFeedItemClickListener {
    public static final String ACTION_SHOW_LOADING_ITEM = "action_show_loading_item";

    private static final int ANIM_DURATION_TOOLBAR = 300;
    private static final int ANIM_DURATION_FAB = 400;

    private MenuItem inboxMenuItem;
    private FeedAdapter feedAdapter;
    private boolean pendingIntroAnimation;
    private List<AVObject> feedList = new ArrayList<>();
    private LinearLayoutManager linearLayoutManager;


    @BindView(R.id.swipeRefreshLayout)
    SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.btnCreate)
    ImageButton btnCreate;
    @BindView(R.id.rvFeed)
    RecyclerView rvFeed;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //LoginActivity.logOut();

        onRequestPermissionsResult();
        setupToolbar();
        setupSwipeRefresh();


        if (savedInstanceState == null) {
            pendingIntroAnimation = true;
        }


    }


    @Override
    protected void onResume() {
        super.onResume();
        setupFeed();
//        Toast.makeText(getApplicationContext(), "onResume", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
//        Toast.makeText(getApplicationContext(), "onStart", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        feedAdapter = null;
//        Toast.makeText(getApplicationContext(), "onStop", Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("CheckResult")
    private void onRequestPermissionsResult() {
        RxPermissions rxPermissions = new RxPermissions(this);

        rxPermissions.request(
                Manifest.permission.INTERNET,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                .subscribe(aBoolean -> {
                    if (aBoolean) {
                        //申请权限成功，操作
                        //Toast.makeText(getApplicationContext(),"获取权限成功",Toast.LENGTH_SHORT).show();

                    } else {
                        //申请权限失败，操作
                        Toast.makeText(getApplicationContext(), "获取权限失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupFeed() {
        if (linearLayoutManager == null) {
            linearLayoutManager = new LinearLayoutManager(MainActivity.this);
            rvFeed.setLayoutManager(linearLayoutManager);
        }

        feedList.clear();
        AVQuery<AVObject> avQuery = new AVQuery<>("Feeds");
        avQuery.orderByDescending("createdAt");

        avQuery.findInBackground().subscribe(new Observer<List<AVObject>>() {
            @Override
            public void onSubscribe(Disposable d) {
//                Toast.makeText(getApplicationContext(),"onSubscribe:"+d.toString(),Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNext(List<AVObject> avObjects) {
//                Toast.makeText(getApplicationContext(),"onNext",Toast.LENGTH_SHORT).show();
                feedList.addAll(avObjects);

                if (feedAdapter == null) {
                    feedAdapter = new FeedAdapter(MainActivity.this);
                    rvFeed.setAdapter(feedAdapter);
                }
                feedAdapter.updateItems(feedList);
                feedAdapter.setOnFeedItemClickListener(MainActivity.this);
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });


    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.style_color_accent));
        swipeRefreshLayout.setOnRefreshListener(refreshListener);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        inboxMenuItem = menu.findItem(R.id.action_inbox);
        inboxMenuItem.setActionView(R.layout.menu_item_view);
        if (pendingIntroAnimation) {
            pendingIntroAnimation = false;
            startIntroAnimation();
        }
        return true;
    }

    @Override
    public void onCommentsClick(View v, String feedCommentId) {
        final Intent intent = new Intent(this, CommentsActivity.class);
        int[] startingLocation = new int[2];
        v.getLocationOnScreen(startingLocation);
        intent.putExtra(CommentsActivity.ARG_DRAWING_START_LOCATION, startingLocation[1]);

        intent.putExtra(CommentsActivity.FEED_COMMENTS_ID,feedCommentId);

        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    @Override
    public void onProfileClick(View v) {
//        int[] startingLocation = new int[2];
//        v.getLocationOnScreen(startingLocation);
//        startingLocation[0] += v.getWidth() / 2;
//        UserProfileActivity.startUserProfileFromLocation(startingLocation, this);
//        overridePendingTransition(0, 0);
    }

    @Override
    public void onUpdateFeeds(View v) {

    }

    @OnClick(R.id.btnCreate)
    public void onCreateClick(View v) {
        if (!isFastClick() && LoginActivity.checkLogin(this)) {
            PublishActivity.openPublisPhoto(this);
        }
    }

    private SwipeRefreshLayout.OnRefreshListener refreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            //下拉刷新要执行的操作
            if (swipeRefreshLayout.isRefreshing()) {
                Toast.makeText(getApplicationContext(),"已刷新",Toast.LENGTH_SHORT).show();
                feedAdapter = null;
                setupFeed();
                swipeRefreshLayout.setRefreshing(false);    //取消刷新
            }
        }
    };

    private void startIntroAnimation() {
        btnCreate.setTranslationY(2 * getResources().getDimensionPixelOffset(R.dimen.btn_fab_size));

        int actionbarSize = Utils.dpToPx(56);

        toolbar.setTranslationY(-actionbarSize);
        ivLogo.setTranslationY(-actionbarSize);
        inboxMenuItem.getActionView().setTranslationY(-actionbarSize);

        toolbar.animate()
                .translationY(0)
                .setDuration(ANIM_DURATION_TOOLBAR)
                .setStartDelay(300);
        ivLogo.animate()
                .translationY(0)
                .setDuration(ANIM_DURATION_TOOLBAR)
                .setStartDelay(400);
        inboxMenuItem.getActionView().animate()
                .translationY(0)
                .setDuration(ANIM_DURATION_TOOLBAR)
                .setStartDelay(500)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        startContentAnimation();
                    }
                })
                .start();
    }

    private void startContentAnimation() {
        btnCreate.animate()
                .translationY(0)
                .setInterpolator(new OvershootInterpolator(1.f))
                .setStartDelay(300)
                .setDuration(ANIM_DURATION_FAB)
                .start();

//            feedAdapter.updateItems(feedList.size());


    }
}

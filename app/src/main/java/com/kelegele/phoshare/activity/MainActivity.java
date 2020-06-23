package com.kelegele.phoshare.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;


import com.kelegele.phoshare.R;
import com.kelegele.phoshare.Utils;
import com.kelegele.phoshare.adapter.FeedAdapter;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends BaseDrawerActivity implements FeedAdapter.OnFeedItemClickListener {

    private static final int ANIM_DURATION_TOOLBAR = 300;
    private static final int ANIM_DURATION_FAB = 400;

    private MenuItem inboxMenuItem;
    private FeedAdapter feedAdapter;
    private boolean pendingIntroAnimation;

    @BindView(R.id.btnCreate)
    ImageButton btnCreate;

    @BindView(R.id.rvFeed)
    RecyclerView rvFeed;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupToolbar();
        setupFeed();

        if (savedInstanceState == null) {
            pendingIntroAnimation = true;
        }
    }

//    private void setupToolbar() {
//        setSupportActionBar(toolbar);
//        toolbar.setNavigationIcon(R.drawable.ic_menu_white);
//    }

    private void setupFeed() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this){
            @Override
            protected int getExtraLayoutSpace(RecyclerView.State state) {
                return 300;
            }
        };
        rvFeed.setLayoutManager(linearLayoutManager);
        feedAdapter = new FeedAdapter(this);
        rvFeed.setAdapter(feedAdapter);

        feedAdapter.setOnFeedItemClickListener(this);
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
        feedAdapter.updateItems();
    }

    @Override
    public void onCommentsClick(View v, int position) {
        final Intent intent = new Intent(this, CommentsActivity.class);

        //Get location on screen for tapped view
        int[] startingLocation = new int[2];
        v.getLocationOnScreen(startingLocation);
        intent.putExtra(CommentsActivity.ARG_DRAWING_START_LOCATION, startingLocation[1]);

        startActivity(intent);
        overridePendingTransition(0, 0);
    }
}

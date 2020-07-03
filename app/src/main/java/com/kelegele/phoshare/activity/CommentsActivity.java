package com.kelegele.phoshare.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kelegele.phoshare.R;
import com.kelegele.phoshare.Utils;
import com.kelegele.phoshare.adapter.CommentsAdapter;
import com.kelegele.phoshare.view.SendCommentButton;

import java.util.ArrayList;
import java.util.HashMap;

import butterknife.BindView;
import cn.leancloud.AVObject;
import cn.leancloud.AVQuery;
import cn.leancloud.AVUser;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class CommentsActivity extends BaseDrawerActivity implements SendCommentButton.OnSendClickListener {
    public static final String ARG_DRAWING_START_LOCATION = "arg_drawing_start_location";
    public static final String FEED_COMMENTS_ID = "feed_comments_id";

    @BindView(R.id.contentRoot)
    LinearLayout contentRoot;
    @BindView(R.id.rvComments)
    RecyclerView rvComments;
    @BindView(R.id.llAddComment)
    LinearLayout llAddComment;
    @BindView(R.id.btnSendComment)
    SendCommentButton btnSendComment;
    @BindView(R.id.etComment)
    EditText etComment;

    private CommentsAdapter commentsAdapter;
    private int drawingStartLocation;
    private String commentsId;
    private ArrayList<HashMap<String, String>> commentsArr;;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        assert getToolbar() != null;
        getToolbar().setNavigationIcon(null);

        commentsId = getIntent().getStringExtra(FEED_COMMENTS_ID);


        initCommentArr();
        setupComments();
        setupSendCommentButton();

        drawingStartLocation = getIntent().getIntExtra(ARG_DRAWING_START_LOCATION, 0);
        if (savedInstanceState == null) {
            ViewTreeObserver.OnPreDrawListener onPreDrawListener = new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    contentRoot.getViewTreeObserver().removeOnPreDrawListener(this);
                    startIntroAnimation();
                    return true;
                }
            };
            contentRoot.getViewTreeObserver().addOnPreDrawListener(onPreDrawListener);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onGlobalMenuHeaderClick(View v) {
        super.onGlobalMenuHeaderClick(v);
    }


    private void initCommentArr() {
        AVQuery<AVObject> query = new AVQuery<>("Comments");
        query.getInBackground(commentsId).subscribe(new Observer<AVObject>() {
            @Override
            public void onSubscribe(Disposable d) {
//                Toast.makeText(getApplicationContext(), "onSubscribe:" + d.toString(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onNext(AVObject avObject) {
//                Toast.makeText(getApplicationContext(), avObject.toJSONString(), Toast.LENGTH_LONG).show();
               commentsArr = (ArrayList<HashMap<String, String>>) avObject.get("commentList");

               if (commentsArr == null){
                   commentsArr =  new ArrayList<>();
               }

            }

            @Override
            public void onError(Throwable e) {
                Toast.makeText(getApplicationContext(), "onError:" + e.toString(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onComplete() {

            }
        });
    }

    private void setupSendCommentButton() {
        btnSendComment.setOnSendClickListener(this);
    }

    private void setupComments() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);

        rvComments.setLayoutManager(linearLayoutManager);
        rvComments.setHasFixedSize(true);

        commentsAdapter = new CommentsAdapter(this);
        rvComments.setAdapter(commentsAdapter);
        rvComments.setOverScrollMode(View.OVER_SCROLL_NEVER);
        rvComments.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    commentsAdapter.setAnimationsLocked(true);
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        contentRoot.animate()
                .translationY(Utils.getScreenHeight(this))
                .setDuration(200)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        CommentsActivity.super.onBackPressed();
                        overridePendingTransition(0, 0);
                    }
                })
                .start();
    }

    @Override
    public void onSendClickListener(View v) {
        if (validateComment()) {

            HashMap<String,String> comment = new HashMap<>();
            comment.put("userId",AVUser.getCurrentUser().getUuid());
            comment.put("userName", AVUser.getCurrentUser().getUsername());
            comment.put("userAvatar",AVUser.getCurrentUser().getAVFile("avatar").getUrl());
            comment.put("userComment",etComment.getText().toString());
            AVObject commentObj = AVObject.createWithoutData("Comments",commentsId);

            commentObj.add("commentList",comment);
            commentObj.saveInBackground().subscribe(new Observer<AVObject>() {
                @Override
                public void onSubscribe(Disposable d) {

                }

                @Override
                public void onNext(AVObject avObject) {
                    etComment.setText(null);
                    commentsAdapter.addItem(comment);

                    // 隐藏软键盘
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(etComment.getWindowToken(), 0) ;


                    Toast.makeText(getApplicationContext(),"评论成功",Toast.LENGTH_SHORT).show();

                    commentsAdapter.setAnimationsLocked(false);
                    commentsAdapter.setDelayEnterAnimation(false);

                    if(rvComments.getChildCount() != 0){
                        rvComments.smoothScrollBy(0,rvComments.getChildAt(0).getHeight() * commentsAdapter.getItemCount());
                    }

//                    rvComments.smoothScrollToPosition(rvComments.getBottom());
                    btnSendComment.setCurrentState(SendCommentButton.STATE_DONE);
                }

                @Override
                public void onError(Throwable e) {
                    Toast.makeText(getApplicationContext(),"Error: "+e.toString(),Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onComplete() {

                }
            });
        }
    }

    private boolean validateComment() {
        if (TextUtils.isEmpty(etComment.getText())) {
            btnSendComment.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake_error));
            return false;
        }

        return true;
    }

    private void startIntroAnimation() {
        contentRoot.setScaleY(0.1f);
        contentRoot.setPivotY(drawingStartLocation);
        llAddComment.setTranslationY(100);

        contentRoot.animate()
                .scaleY(1)
                .setDuration(200)
                .setInterpolator(new AccelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        animateContent();
                    }
                })
                .start();
    }

    private void animateContent() {
        commentsAdapter.updateItems(commentsArr);
        llAddComment.animate().translationY(0)
                .setInterpolator(new DecelerateInterpolator())
                .setDuration(200)
                .start();
    }
}

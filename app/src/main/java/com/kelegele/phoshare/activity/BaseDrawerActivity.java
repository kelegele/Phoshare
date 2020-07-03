package com.kelegele.phoshare.activity;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.kelegele.phoshare.R;
import com.kelegele.phoshare.Utils;
import com.kelegele.phoshare.utils.CircleTransformation;
import com.squareup.picasso.Picasso;

import butterknife.BindDimen;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.OnClick;
import cn.leancloud.AVUser;

import static com.kelegele.phoshare.Utils.isFastClick;

public class BaseDrawerActivity extends BaseActivity {

    @BindView(R.id.drawerLayout)
    DrawerLayout drawerLayout;
    @BindView(R.id.vNavigation)
    NavigationView vNavigation;
    @BindDimen(R.dimen.global_menu_avatar_size)
    int avatarSize;
    @BindString(R.string.user_profile_photo)
    String profilePhoto;

    private ImageView ivMenuUserProfilePhoto;
    private TextView tvMenuUserProfileName;

    @Override
    public void setContentView(int layoutResID) {
        updateStatusBarColor();
        super.setContentViewWithoutInject(R.layout.activity_drawer);
        ViewGroup viewGroup = (ViewGroup) findViewById(R.id.flContentRoot);
        LayoutInflater.from(this).inflate(layoutResID, viewGroup, true);
        bindViews();
        setupHeader();
        initAVOSCloud();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    protected void updateStatusBarColor() {
        if (Utils.isAboveAndroid5()) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.style_color_primary));
        }
    }

    @Override
    protected void setupToolbar() {
        super.setupToolbar();
        if (getToolbar() != null) {
            View headerView = vNavigation.getHeaderView(0);
            tvMenuUserProfileName = headerView.findViewById(R.id.tvMenuUserProfileName);

            getToolbar().setNavigationOnClickListener(v -> {
                drawerLayout.openDrawer(Gravity.LEFT);

                //Toast.makeText(getApplicationContext(),"get",Toast.LENGTH_LONG).show();
                if (AVUser.getCurrentUser() != null){

                    if (AVUser.getCurrentUser().getAVFile("avatar") == null){
                        profilePhoto = getResources().getString(R.string.user_profile_photo);
                    }else {
                        profilePhoto = AVUser.getCurrentUser().getAVFile("avatar").getUrl();
                    }
                    tvMenuUserProfileName.setText(AVUser.getCurrentUser().getUsername());

                }else {
                    tvMenuUserProfileName.setText("未登录");
                    profilePhoto = getResources().getString(R.string.user_profile_photo);
                }

                Picasso.get()
                        .load(profilePhoto)
                        .placeholder(R.drawable.img_circle_placeholder)
                        .resize(avatarSize, avatarSize)
                        .centerCrop()
                        .transform(new CircleTransformation())
                        .into(ivMenuUserProfilePhoto);
            });
        }
    }

    private void setupHeader() {
        View headerView = vNavigation.getHeaderView(0);
        ivMenuUserProfilePhoto =  headerView.findViewById(R.id.ivMenuUserProfilePhoto);

        vNavigation.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                String text = null;
                switch (item.getItemId()){
                    case R.id.menu_feed:
                    case R.id.menu_news:
                    case R.id.menu_photo_you_liked:
                    case R.id.menu_logout:
                        if (AVUser.getCurrentUser() == null) {
                            text = "您还未登录";
                        }else {
                            if (item.getItemId() == R.id.menu_logout){
                                LoginActivity.logOut();
                                text = "您已登出";
                            }
                        }

                        break;
                    default:
                        break;
                }
                if (text != null){
                    Toast.makeText(getApplicationContext(),text,Toast.LENGTH_SHORT).show();
                    drawerLayout.closeDrawer(Gravity.LEFT);
                }

                return true;
            }
        });

        headerView.findViewById(R.id.vGlobalMenuHeader)
                .setOnClickListener(this::onGlobalMenuHeaderClick);

        Picasso.get()
                .load(profilePhoto)
                .placeholder(R.drawable.img_circle_placeholder)
                .resize(avatarSize, avatarSize)
                .centerCrop()
                .transform(new CircleTransformation())
                .into(ivMenuUserProfilePhoto);

    }

    public void onGlobalMenuHeaderClick(final View v) {

        drawerLayout.closeDrawer(Gravity.LEFT);
        new Handler().postDelayed(() -> {
            int[] startingLocation = new int[2];
            v.getLocationOnScreen(startingLocation);
            startingLocation[0] += v.getWidth() / 2;

            if (!isFastClick() && LoginActivity.checkLogin(this)){
                UserProfileActivity.startUserProfileFromLocation(startingLocation, BaseDrawerActivity.this);
                overridePendingTransition(0,0);
            }

        }, 200);
    }
}

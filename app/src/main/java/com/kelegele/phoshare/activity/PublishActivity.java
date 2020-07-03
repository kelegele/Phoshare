package com.kelegele.phoshare.activity;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;

import com.kelegele.phoshare.R;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import butterknife.BindView;
import butterknife.OnClick;
import cn.leancloud.AVFile;
import cn.leancloud.AVObject;
import cn.leancloud.AVUser;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

import static com.kelegele.phoshare.Utils.isFastClick;
import static java.security.AccessController.getContext;

public class PublishActivity extends BaseActivity {
    public static final String ARG_TAKEN_PHOTO_URI = "arg_taken_photo_uri";

    static final int TAKE_PHOTO = 1;
    static final int OPEN_ALBUM = 0;


    @BindView(R.id.tbOpenAlbum)
    Button tbOpenAlbum;
    @BindView(R.id.tbTakePhoto)
    Button tbTakePhoto;
    @BindView(R.id.tbPublish)
    Button tbPublish;
    @BindView(R.id.ivPhoto)
    ImageView ivPhoto;
    @BindView(R.id.etDescription)
    EditText etDescription;
    @BindView(R.id.progressBar)
    View progressView;
    @BindView(R.id.actionView)
    View actionView;


    private boolean propagatingToggleState = false;
    private Uri photoUri;
    private int photoSize;
    private int current;
    private byte[] mImageBytes = null;
    private Context content;


    public static void openPublisPhoto(Activity openingActivity) {
        Intent intent = new Intent(openingActivity, PublishActivity.class);
        openingActivity.startActivity(intent);
        openingActivity.overridePendingTransition(R.anim.slide_in_from_right,R.anim.slide_out_to_left);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish);

        assert getToolbar() != null;
        getToolbar().setNavigationIcon(null);

    }

    private void bringMainActivityToTop() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.setAction(MainActivity.ACTION_SHOW_LOADING_ITEM);
        startActivity(intent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(ARG_TAKEN_PHOTO_URI, photoUri);
    }

    @OnClick(R.id.tbOpenAlbum)
    public void onOpenAlbumClick() {
        Intent getContentIntent = new Intent(Intent.ACTION_GET_CONTENT);
        getContentIntent.setType("image/*");
        startActivityForResult(getContentIntent, OPEN_ALBUM);
    }

    @OnClick(R.id.tbTakePhoto)
    public void onTakePhotoClick() {
        onRequestPermissionsResult(TAKE_PHOTO);
    }

    @SuppressLint("CheckResult")
    private void onRequestPermissionsResult(int state) {
        RxPermissions rxPermissions = new RxPermissions(this);

        rxPermissions.request(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                .subscribe(aBoolean -> {
                    if (aBoolean) {
                        //申请权限成功，操作
                        if (state == TAKE_PHOTO) {
                            startCamera();
                        } else if (state == OPEN_ALBUM) {

                        }
                    } else {
                        //申请权限失败，操作
                    }
                });
    }

    private void startCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
            }

            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(
                        this,
                        "com.kelegele.phoshare.fileprovider",
                        photoFile);
            }

            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            startActivityForResult(takePictureIntent, TAKE_PHOTO);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == TAKE_PHOTO && resultCode == RESULT_OK) {
            galleryAddPic();
            setPic();
        }

        if (requestCode == OPEN_ALBUM && resultCode == RESULT_OK) {
            try {
                //mImageBytes = getBytes(getContentResolver().openInputStream(data.getData()));
                //压缩相册上传图像
                Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(data.getData()));
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 40, baos);
                mImageBytes = baos.toByteArray();

                ivPhoto.setImageBitmap(MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void setPic() {
        int targetW = ivPhoto.getWidth();
        int targetH = ivPhoto.getHeight();

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 40, baos);
        mImageBytes = baos.toByteArray();

        ivPhoto.setImageBitmap(bitmap);
    }

    private void loadThumbnailPhoto() {
        ivPhoto.setScaleX(0);
        ivPhoto.setScaleY(0);
        Picasso.get()
                .load(photoUri)
                .centerCrop()
                .resize(photoSize, photoSize)
                .into(ivPhoto, new Callback() {
                    @Override
                    public void onSuccess() {
                        ivPhoto.animate()
                                .scaleX(1.f).scaleY(1.f)
                                .setInterpolator(new OvershootInterpolator())
                                .setDuration(400)
                                .setStartDelay(200)
                                .start();
                    }

                    @Override
                    public void onError(Exception e) {

                    }
                });
    }

    private String currentPhotoPath;

    private File createImageFile() throws IOException {
        @SuppressLint("SimpleDateFormat")
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    public byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, len);
        }
        return byteArrayOutputStream.toByteArray();
    }

    @OnClick(R.id.tbPublish)
    public void onPublishClick() {
        if (mImageBytes == null || isFastClick()) {
            Toast.makeText(getApplicationContext(), "还未选择发布照片！", Toast.LENGTH_SHORT).show();
        }else {
            showProgress(true);

            Map<String,Object> meta = new HashMap<String, Object>();
            meta.put("mime_type","image/jpeg");
            AVObject product = new AVObject("Feeds");
            product.put("owner", AVUser.getCurrentUser());
            product.put("ownerName",AVUser.getCurrentUser().getUsername());
            product.put("ownerAvatar",AVUser.getCurrentUser().getAVFile("avatar"));
            product.put("description", etDescription.getText().toString());
            product.put("comments",new AVObject("Comments"));
            //product.put("likes",new AVObject("Likes"));

            AVFile image = new AVFile("feedsPic", mImageBytes);
            image.getThumbnailUrl(true,100,100);
            product.put("image", image);

            product.saveInBackground().subscribe(new Observer<AVObject>() {
                @Override
                public void onSubscribe(Disposable d) {

                }

                @Override
                public void onNext(AVObject avObject) {
                    showProgress(false);
                    Toast.makeText(PublishActivity.this, "发布成功", Toast.LENGTH_SHORT).show();
                    bringMainActivityToTop();
                    PublishActivity.this.finish();
                }

                @Override
                public void onError(Throwable e) {
                    showProgress(false);
                    Toast.makeText(PublishActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onComplete() {

                }
            });
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            tbPublish.setEnabled(false);
            actionView.setVisibility(show ? View.GONE : View.VISIBLE);
            actionView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    actionView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            progressView.setVisibility(show ? View.VISIBLE : View.GONE);
            progressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    progressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            tbPublish.setEnabled(false);
            progressView.setVisibility(show ? View.VISIBLE : View.GONE);
            actionView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

}

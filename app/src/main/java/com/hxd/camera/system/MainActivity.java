package com.hxd.camera.system;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.FileProvider;
import com.yanzhenjie.permission.runtime.Permission;

import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageView mIvOne, mIvTwo, mIvThree;

    private final int TAKE_PHOTO_TAG = 101;
    private final int OPEN_ALBUM_TAG = 102;

    private Uri imageUri;
    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSharedPreferences = getSharedPreferences("system", Context.MODE_PRIVATE);

        findViewById(R.id.rb_one).setOnClickListener(this);
        findViewById(R.id.rb_two).setOnClickListener(this);
        findViewById(R.id.rb_three).setOnClickListener(this);

        mIvOne = findViewById(R.id.iv_one);
        mIvTwo = findViewById(R.id.iv_two);
        mIvThree = findViewById(R.id.iv_three);

        if (mSharedPreferences.getBoolean("isRequest", false)) {
            Glide.with(this).load(R.mipmap.yes_p).into(mIvOne);
        } else {
            Glide.with(this).load(R.mipmap.no_p).into(mIvOne);
        }
        Glide.with(this).load(R.mipmap.camera).into(mIvTwo);
        Glide.with(this).load(R.mipmap.album).into(mIvThree);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.rb_one:
                getSysAuthority();
                break;
            case R.id.rb_two://camera
                takeCamera();
                break;
            case R.id.rb_three://album
                openAlbum();
                break;
        }
    }

    /**
     * 申请必要权限
     */
    private void getSysAuthority() {
        if (mSharedPreferences.getBoolean("isRequest", false)) {
            toastUtils(true);
        } else {
            AndPermission.with(this).runtime()
                    .permission(Permission.CAMERA, Permission.WRITE_EXTERNAL_STORAGE, Permission.READ_EXTERNAL_STORAGE)
                    .onGranted(new Action<List<String>>() {
                        @Override
                        public void onAction(List<String> data) {
                            mSharedPreferences.edit().putBoolean("isRequest", true).apply();
                            Glide.with(MainActivity.this).load(R.mipmap.yes_p).into(mIvOne);
                        }
                    }).onDenied(new Action<List<String>>() {
                @Override
                public void onAction(List<String> data) {
                    toastUtils(false);
                    Glide.with(MainActivity.this).load(R.mipmap.no_p).into(mIvOne);
                }
            }).start();
        }
    }

    private void toastUtils(boolean isRequest) {
        Toast.makeText(MainActivity.this, isRequest ? "已经申请必要权限" : "请同意必要权限，否则无法正常使用相关功能", Toast.LENGTH_LONG).show();
    }

    /**
     * 打开系统相册
     */
    private void openAlbum() {
        if (!mSharedPreferences.getBoolean("isRequest", false)) {
            toastUtils(false);
            return;
        }

        Intent photoPickerIntent = new Intent(Intent.ACTION_GET_CONTENT);
        //        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);  传统打开相册
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, OPEN_ALBUM_TAG);
    }


    /**
     * 相机拍照
     */

    private void takeCamera() {
        if (!mSharedPreferences.getBoolean("isRequest", false)) {
            toastUtils(false);
            return;
        }

        Intent intentCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !Environment.isExternalStorageLegacy()) {
            imageUri = createImageUri();
            intentCamera.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            File jpgFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), System.currentTimeMillis() + ".jpg");
            imageUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID, jpgFile);
            //            //添加这一句表示对目标应用临时授权该Uri所代表的文件
            intentCamera.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            File jpgFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), System.currentTimeMillis() + ".jpg");
            imageUri = Uri.fromFile(jpgFile);
        }
        //将拍照结果保存至 photo_file 的 Uri 中，不保留在相册中
        intentCamera.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intentCamera, TAKE_PHOTO_TAG);
    }

    /**
     * Android 10 获取 图片 uri
     *
     * @return 生成的图片uri
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public Uri createImageUri() {
        ContentValues values = new ContentValues();
        // 需要指定文件信息时，非必须
        values.put(MediaStore.Images.Media.DESCRIPTION, "This is an image");
        values.put(MediaStore.Images.Media.DISPLAY_NAME, System.currentTimeMillis() + ".jpg");
        values.put(MediaStore.Images.Media.TITLE, System.currentTimeMillis() + ".jpg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/albumCameraImg");
        return getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TAKE_PHOTO_TAG && resultCode == RESULT_OK) {
            Glide.with(this).load(imageUri).into(mIvTwo);
        } else if (requestCode == OPEN_ALBUM_TAG && resultCode == RESULT_OK) {
            Uri uri;
            if (data != null) {
                uri = data.getData();
            } else {
                return;
            }
            String path = GetImgFromAlbum.getRealPathFromUri(this, uri);
            Glide.with(this).load(path).into(mIvThree);
        }
    }
}
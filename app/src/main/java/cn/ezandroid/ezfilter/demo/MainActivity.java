package cn.ezandroid.ezfilter.demo;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import me.weyye.hipermission.HiPermission;
import me.weyye.hipermission.PermissionItem;

public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_STORAGE = 2;
    private static final int REQUEST_AUDIO = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        List<PermissionItem> permissionItems = new ArrayList<PermissionItem>();
        permissionItems.add(new PermissionItem(Manifest.permission.CAMERA,
                "CAMERA", R.drawable.permission_ic_camera));
        permissionItems.add(new PermissionItem(Manifest.permission.RECORD_AUDIO,
                "RECORD_AUDIO", R.drawable.permission_ic_micro_phone));
        permissionItems.add(new PermissionItem(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                "WRITE_EXTERNAL_STORAGE", R.drawable.permission_ic_storage));
        HiPermission.create(MainActivity.this)
                .permissions(permissionItems).checkMutiPermission(null);

        $(R.id.image_filter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ImageFilterActivity.class);
                startActivity(intent);
            }
        });

        $(R.id.video_filter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, VideoFilterActivity.class);
                startActivity(intent);
            }
        });

        $(R.id.camera_filter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, CameraFilterActivity.class);
                startActivity(intent);
            }
        });

        $(R.id.camera2_filter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, Camera2FilterActivity.class);
                startActivity(intent);
            }
        });

        $(R.id.view_filter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ViewFilterActivity.class);
                startActivity(intent);
            }
        });

        $(R.id.video_offscreen).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, VideoOffscreenActivity.class);
                startActivity(intent);
            }
        });
    }
}

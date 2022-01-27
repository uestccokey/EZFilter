package cn.ezandroid.ezfilter;

import android.content.Intent;
import android.os.Bundle;

import cn.ezandroid.ezpermission.EZPermission;
import cn.ezandroid.ezpermission.Permission;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 申请权限
        EZPermission.permissions(Permission.CAMERA, Permission.STORAGE, Permission.MICROPHONE)
                .apply(this, null);

        $(R.id.image_filter).setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, ImageFilterActivity.class);
            startActivity(intent);
        });

        $(R.id.video_filter).setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, VideoFilterActivity.class);
            startActivity(intent);
        });

        $(R.id.camera_filter).setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, CameraFilterActivity.class);
            startActivity(intent);
        });

        $(R.id.camera2_filter).setOnClickListener(view -> {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                Intent intent = new Intent(MainActivity.this, Camera2FilterActivity.class);
                startActivity(intent);
            }
        });

        $(R.id.view_filter).setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, ViewFilterActivity.class);
            startActivity(intent);
        });

        $(R.id.multi_input_filter).setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, MultiInputActivity.class);
            startActivity(intent);
        });

        $(R.id.split_filter).setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, SplitInputActivity.class);
            startActivity(intent);
        });

        $(R.id.particle_render).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ParticleRenderActivity.class);
            startActivity(intent);
        });

        $(R.id.sticker_render).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, StickerRenderActivity.class);
            startActivity(intent);
        });
    }
}

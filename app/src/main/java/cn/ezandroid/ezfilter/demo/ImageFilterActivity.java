package cn.ezandroid.ezfilter.demo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;

import cn.ezandroid.ezfilter.EZFilter;
import cn.ezandroid.ezfilter.demo.render.LookupRender;
import cn.ezandroid.ezfilter.view.TextureRenderView;

/**
 * ImageFilterActivity
 *
 * @author like
 * @date 2017-09-16
 */
public class ImageFilterActivity extends BaseActivity {

    private TextureRenderView mRenderView;
    private ImageView mPreviewImage;

    private Bitmap mBitmap1;
    private Bitmap mBitmap2;

    private Bitmap mCurrentBitmap;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_filter);
        mRenderView = $(R.id.render_view);
        mPreviewImage = $(R.id.preview_image);

        mBitmap1 = BitmapFactory.decodeResource(getResources(), R.drawable.preview);
        mBitmap2 = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);

        mCurrentBitmap = mBitmap1;

        new EZFilter.BitmapBuilder()
                .setBitmap(mCurrentBitmap)
                .addFilter(new LookupRender(ImageFilterActivity.this, R.drawable.langman))
                .addFilter(new LookupRender(ImageFilterActivity.this, R.drawable.lianghong))
//                .addFilter(new LookupRender(ImageFilterActivity.this, R.drawable.lom))
//                .addFilter(new LookupRender(ImageFilterActivity.this, R.drawable.rixi))
//                .addFilter(new LookupRender(ImageFilterActivity.this, R.drawable.rouguang))
//                .addFilter(new LookupRender(ImageFilterActivity.this, R.drawable.shaokao))
//                .addFilter(new LookupRender(ImageFilterActivity.this, R.drawable.shishang))
//                .addFilter(new LookupRender(ImageFilterActivity.this, R.drawable.tianmei))
                .into(mRenderView);

        new Thread() {
            public void run() {
                final Bitmap bitmap = new EZFilter.BitmapBuilder()
                        .setBitmap(mCurrentBitmap)
                        .addFilter(new LookupRender(ImageFilterActivity.this, R.drawable.shishang))
                        .addFilter(new LookupRender(ImageFilterActivity.this, R.drawable.tianmei))
                        .capture();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mPreviewImage.setImageBitmap(bitmap);
                    }
                });
            }
        }.start();

        $(R.id.change_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeBitmap();
            }
        });
    }

    private void changeBitmap() {
        if (mCurrentBitmap == mBitmap1) {
            mCurrentBitmap = mBitmap2;
        } else {
            mCurrentBitmap = mBitmap1;
        }

        new EZFilter.BitmapBuilder()
                .setBitmap(mCurrentBitmap)
                .addFilter(new LookupRender(ImageFilterActivity.this, R.drawable.langman))
                .addFilter(new LookupRender(ImageFilterActivity.this, R.drawable.lianghong))
//                .addFilter(new LookupRender(ImageFilterActivity.this, R.drawable.lom))
//                .addFilter(new LookupRender(ImageFilterActivity.this, R.drawable.rixi))
//                .addFilter(new LookupRender(ImageFilterActivity.this, R.drawable.rouguang))
//                .addFilter(new LookupRender(ImageFilterActivity.this, R.drawable.shaokao))
//                .addFilter(new LookupRender(ImageFilterActivity.this, R.drawable.shishang))
//                .addFilter(new LookupRender(ImageFilterActivity.this, R.drawable.tianmei))
                .into(mRenderView);

        new Thread() {
            public void run() {
                final Bitmap bitmap = new EZFilter.BitmapBuilder()
                        .setBitmap(mCurrentBitmap)
                        .addFilter(new LookupRender(ImageFilterActivity.this, R.drawable.shishang))
                        .addFilter(new LookupRender(ImageFilterActivity.this, R.drawable.tianmei))
                        .capture();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mPreviewImage.setImageBitmap(bitmap);
                    }
                });
            }
        }.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}

package cn.ezandroid.ezfilter.demo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import cn.ezandroid.ezfilter.EZFilter;
import cn.ezandroid.ezfilter.demo.render.MirrorRender;
import cn.ezandroid.ezfilter.view.TextureRenderView;

/**
 * ImageFilterActivity
 *
 * @author like
 * @date 2017-09-16
 */
public class ImageFilterActivity extends BaseActivity {

    private TextureRenderView mRenderView;
    private Button mChangeImageButton;
    private ImageView mPreviewImage;

    private Bitmap mBitmap1;
    private Bitmap mBitmap2;

    private Bitmap mCurrentBitmap;

//    private BitmapInput mBitmapInput;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_filter);
        mRenderView = $(R.id.render_view);
        mChangeImageButton = $(R.id.change_image);
        mPreviewImage = $(R.id.preview_image);

        mBitmap1 = BitmapFactory.decodeResource(getResources(), R.drawable.preview);
        mBitmap2 = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);

        mCurrentBitmap = mBitmap1;

//        final BitmapOutput bitmapOutput = new BitmapOutput();
//        bitmapOutput.setBitmapOutputCallback(new BitmapOutput.BitmapOutputCallback() {
//            @Override
//            public void bitmapOutput(final Bitmap bitmap) {
//                if (bitmap == null) {
//                    return;
//                }
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        mPreviewImage.setImageBitmap(bitmap);
//                    }
//                });
//            }
//        });

        new EZFilter.Builder().setBitmap(mCurrentBitmap).addFilter(new MirrorRender()).into(mRenderView);

        mChangeImageButton.setOnClickListener(new View.OnClickListener() {
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

        new EZFilter.Builder().setBitmap(mCurrentBitmap).addFilter(new MirrorRender()).into(mRenderView);
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

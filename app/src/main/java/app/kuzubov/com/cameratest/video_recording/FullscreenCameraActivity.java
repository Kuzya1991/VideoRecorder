package app.kuzubov.com.cameratest.video_recording;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import app.kuzubov.com.cameratest.R;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenCameraActivity extends AppCompatActivity implements RecordVideoManager.IVideoRecordingListener {

    private static final String TAG = FullscreenCameraActivity.class.getSimpleName();

    public static void start(Context context) {
        context.startActivity(new Intent(context, FullscreenCameraActivity.class));
    }

    private RecordVideoManager mVideoManager;
    private View mRoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen_camera);

        mRoot = findViewById(R.id.root);

        mVideoManager = new RecordVideoManager(this, 10, 30, this);
        mVideoManager.start(mRoot);
    }


    @Override
    protected void onResume() {
        super.onResume();
        mVideoManager.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mVideoManager.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mVideoManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onStop() {
        mVideoManager.onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mVideoManager.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mVideoManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void closeCameraView() {
        finish();
    }
}

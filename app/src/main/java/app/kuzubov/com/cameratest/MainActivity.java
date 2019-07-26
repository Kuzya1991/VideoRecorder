package app.kuzubov.com.cameratest;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.FrameLayout;

import app.kuzubov.com.cameratest.video_recording.RecordVideoManager;

public class MainActivity extends AppCompatActivity{

    private FrameLayout mContainer;
    private RecordVideoManager mVideoManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.start_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RecordVideoManager.start(MainActivity.this);
            }
        });

        findViewById(R.id.start_with_view_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mContainer.setVisibility(View.VISIBLE);
                mVideoManager = new RecordVideoManager(MainActivity.this, 10, 30, new RecordVideoManager.IVideoRecordingListener() {
                    @Override
                    public void closeCameraView() {
                        mContainer.setVisibility(View.GONE);
                        mVideoManager.onDestroy();
                    }
                });
                mVideoManager.start(mContainer);
                mVideoManager.onResume();
            }
        });

        mContainer = findViewById(R.id.camera_container);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mVideoManager != null)
            mVideoManager.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mVideoManager != null)
            mVideoManager.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (mVideoManager != null)
            mVideoManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
        else
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onStop() {
        if (mVideoManager != null)
            mVideoManager.onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mVideoManager != null)
            mVideoManager.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(mVideoManager != null)
            mVideoManager.onActivityResult(requestCode, resultCode, data);
    }
}

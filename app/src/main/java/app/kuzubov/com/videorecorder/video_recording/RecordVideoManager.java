package app.kuzubov.com.videorecorder.video_recording;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.util.Range;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import app.kuzubov.com.videorecorder.R;
import app.kuzubov.com.videorecorder.video_recording.helpers.FileHelper;
import app.kuzubov.com.videorecorder.video_recording.helpers.PermissionUtils;
import app.kuzubov.com.videorecorder.video_recording.ui.AutoFitTextureView;
import app.kuzubov.com.videorecorder.video_recording.ui.FullscreenCameraActivity;

import static app.kuzubov.com.videorecorder.R.layout.frame_camera_recording;

/**
 * The main class that provide api to record video with special time period and frame rate.
 */

public class RecordVideoManager {

    private static final String TAG = RecordVideoManager.class.getSimpleName();

    private final FrameLayout mVideoContainer;

    private CameraDevice mCameraDevice;
    private MediaRecorder mMediaRecorder;
    private Timer timer;
    private TimerTask timerTask;
    private TextView mVideoTimer;
    private Button mRecordButton;
    private AutoFitTextureView mCameraPreview;

    private CameraCaptureSession mCameraCaptureSessions;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    private Activity mActivity;
    private boolean mIsVideoRecording;
    private File mCurrentVideoFile;
    private int mDurationLimit;
    private final int mFrameRate;
    private ViewGroup mParentView;
    private final IVideoRecordingListener mListener;
    private final FileHelper mFileHelper;


    public RecordVideoManager(Activity activity, int durationLimit, int frameRate, IVideoRecordingListener listener, FileHelper helper) {
        this.mDurationLimit = durationLimit;
        this.mFrameRate = frameRate;
        this.mActivity = activity;
        this.mListener = listener;
        this.mFileHelper = helper;

        mVideoContainer = (FrameLayout) LayoutInflater.from(mActivity).inflate(frame_camera_recording, mParentView);
    }

    /**
     * Start full screen activity with camera preview
     */
    public static void start(Context context) {
        FullscreenCameraActivity.start(context);
    }

    /**
     * Add camera preview to a ViewGroup element
     *
     * @param parentView is a ViewGroup ui element
     */
    public void start(ViewGroup parentView) {

        this.mParentView = parentView;
        mCameraPreview = mVideoContainer.findViewById(R.id.fullscreen_content);
        mCameraPreview.setSurfaceTextureListener(mTextureListener);
        mCameraPreview.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

        mVideoTimer = mVideoContainer.findViewById(R.id.video_timer);

        mRecordButton = mVideoContainer.findViewById(R.id.start_record_button);
        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mIsVideoRecording)
                    stopRecordingVideo();
                else
                    startRecordingVideo();
            }
        });

        mParentView.addView(mVideoContainer);
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            mCameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        }
    };

    private void createCameraPreview() {
        try {
            SurfaceTexture texture = mCameraPreview.getSurfaceTexture();
            if (texture == null) return;
            texture.setDefaultBufferSize(Contract.VIDEO_WIDTH, Contract.VIDEO_HEIGHT);
            Surface surface = new Surface(texture);
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(surface);
            mCameraDevice.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if (null == mCameraDevice) {
                        return;
                    }
                    mCameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                }
            }, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        if (null == mCameraDevice) {
            Log.e(TAG, mActivity.getString(R.string.camera_init_error));
            return;
        }
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, getRange());
        try {
            mCameraCaptureSessions.setRepeatingRequest(mCaptureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
        try {
            if (hasPermissions()) {
                initCamera();
            } else {
                requestPermissionWithRationale();
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("MissingPermission")
    private void initCamera() throws CameraAccessException {
        android.hardware.camera2.CameraManager manager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
        assert manager != null;
        CameraCharacteristics characteristics = manager.getCameraCharacteristics(Contract.CAMERA_BACK);
        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        assert map != null;
        manager.openCamera(Contract.CAMERA_BACK, stateCallback, null);
    }

    private Range<Integer> getRange() {
        Range<Integer> result = null;

        try {
            CameraManager manager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics chars;
            assert manager != null;
            chars = manager.getCameraCharacteristics(Contract.CAMERA_BACK);
            Range<Integer>[] ranges = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);

            assert ranges != null;
            for (Range<Integer> range : ranges) {
                int upper = range.getUpper();

                // 10 - min range upper for my needs
                if (upper >= 10) {
                    if (result == null || upper < result.getUpper()) {
                        result = range;
                    }
                }
            }

            if (result == null) {
                result = ranges[0];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    private void closeCamera() {
        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    /**
     * Start helper thread to avoid ui stuck.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread(Contract.VIDEO_HANDLER_NAME);
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stop helper thread.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void onResume() {
        startBackgroundThread();
    }

    public void onPause() {
        stopBackgroundThread();
    }

    /**
     * Obtain user permission's decision
     */
    public void onRequestPermissionsResult(int requestCode, @NonNull int[] grantResults) {
        boolean allowed = true;

        if (requestCode == Contract.REQUEST_CAMERA_PERMISSION) {
            for (int res : grantResults) {
                allowed = allowed && (res == PackageManager.PERMISSION_GRANTED);
            }
            if (allowed) {
                if (hasPermissions()) {
                    try {
                        initCamera();
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (mActivity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                        Toast.makeText(mActivity, R.string.camera_or_storage_denied, Toast.LENGTH_SHORT).show();
                    } else {
                        showNoCameraPermissionSnackBar();
                    }
                }
            }
        }
    }

    /**
     * Show bottom dialog that notify user about not granted permissions
     */
    private void showNoCameraPermissionSnackBar() {
        Snackbar.make(mParentView, R.string.camera_or_storage_not_granted, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.settings, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openApplicationSettings();

                        Toast.makeText(mActivity.getApplicationContext(),
                                R.string.camera_storage_grant,
                                Toast.LENGTH_SHORT)
                                .show();
                    }
                })
                .show();
    }

    private void openApplicationSettings() {
        Intent appSettingsIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse(mActivity.getString(R.string.app_settings_package, mActivity.getPackageName())));
        mActivity.startActivityForResult(appSettingsIntent, Contract.REQUEST_CAMERA_PERMISSION);
    }

    private final TextureView.SurfaceTextureListener mTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    /**
     * Start video recording. User will be notified with timer on the top of the screen about remaining time.
     */

    private void startRecordingVideo() {
        if (null == mCameraDevice || !mCameraPreview.isAvailable()) {
            return;
        }
        mRecordButton.setText(R.string.end_video_button);
        try {
            closePreviewSession();
            SurfaceTexture texture = mCameraPreview.getSurfaceTexture();
            assert texture != null;
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            List<Surface> surfaces = new ArrayList<>();

            // Set up Surface for the camera preview
            Surface previewSurface = new Surface(texture);
            setUpMediaRecorder();
            surfaces.add(previewSurface);
            mCaptureRequestBuilder.addTarget(previewSurface);

            // Set up Surface for the MediaRecorder
            Surface recorderSurface = mMediaRecorder.getSurface();
            surfaces.add(recorderSurface);
            mCaptureRequestBuilder.addTarget(recorderSurface);

            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    mCameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            mMediaRecorder.start();
                            startRecordingFlow();
                        }
                    });
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(mActivity, R.string.failed, Toast.LENGTH_SHORT).show();
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException | IOException e) {
            e.printStackTrace();
        }
    }

    private void startRecordingFlow() {
        startTimer();

        mIsVideoRecording = true;
        mVideoTimer.setVisibility(View.VISIBLE);
    }

    /**
     * Start timer to notify user about remaining time
     */
    private void startTimer() {
        try {
            timer = new Timer();
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    if (mDurationLimit == 0) {
                        timer.cancel();
                        timer.purge();
                        timerTask.cancel();
                        stopRecordingVideo();
                    } else {
                        mDurationLimit--;

                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    mVideoTimer.setText(String.format(Locale.getDefault(), mActivity.getString(R.string.timer_scheme), 0, mDurationLimit));
                                } catch (Exception ignored) {
                                }
                            }
                        });
                    }
                }
            };
            timer.schedule(timerTask, Contract.TIMER_DELAY, Contract.TIMER_PERIOD);
        } catch (IllegalStateException ignored) {
        }
    }

    private void stopRecordingVideo() {
        timer.cancel();
        timer.purge();
        timerTask.cancel();

        closePreviewSession();
        mIsVideoRecording = true;

        shareVideo();
    }

    /**
     * Share recorded video through the appropriate app you will choose.
     */

    private void shareVideo() {

        Uri videoURI = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                ? FileProvider.getUriForFile(mActivity, mActivity.getString(R.string.authority_suffix, mActivity.getPackageName()), mCurrentVideoFile)
                : Uri.fromFile(mCurrentVideoFile);

        Intent chooser = ShareCompat.IntentBuilder.from(mActivity)
                .setStream(videoURI)
                .setType(Contract.VIDEO_MIME_TYPE)
                .setChooserTitle(R.string.share_video_title)
                .createChooserIntent();

        if (chooser.resolveActivity(mActivity.getPackageManager()) != null) {
            mActivity.startActivityForResult(chooser, Contract.SHARE_RESULT_CODE);
        }
    }

    /**
     * Set up media recorder with appropriate params to start video recording
     */
    private void setUpMediaRecorder() throws IOException {
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        String mNextVideoAbsolutePath = prepareVideoFilePath();
        mMediaRecorder.setOutputFile(mNextVideoAbsolutePath);
        mMediaRecorder.setVideoEncodingBitRate(Contract.VIDEO_BIT_RATE);
        mMediaRecorder.setVideoFrameRate(mFrameRate);
        mMediaRecorder.setVideoSize(Contract.VIDEO_WIDTH, Contract.VIDEO_HEIGHT);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.setOrientationHint(Contract.CAMERA_ORIENTATION);
        mMediaRecorder.prepare();
    }

    private String prepareVideoFilePath() {
        mCurrentVideoFile = mFileHelper.getVideoFile();
        return mCurrentVideoFile.getPath();
    }

    private void closePreviewSession() {
        if (mCameraCaptureSessions != null) {
            mCameraCaptureSessions.close();
            mCameraCaptureSessions = null;
        }
    }

    public void onStop() {
        try {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        } catch (Exception ignored) {
        }
    }

    /**
     * Clear all resources to about memory leaks
     */
    public void onDestroy() {
        if (timer != null) {
            timer.purge();
            timer.cancel();
        }
        if (timerTask != null) {
            timerTask.cancel();
        }
        if (mFileHelper != null)
            mFileHelper.clearCache();
        if (mMediaRecorder != null && mIsVideoRecording) {
            try {
                mMediaRecorder.stop();
                mMediaRecorder.reset();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        closeCamera();

        if (mActivity != null)
            mActivity = null;
    }

    public void onActivityResult(int requestCode) {
        if (requestCode == Contract.SHARE_RESULT_CODE)
            mListener.closeCameraView();

    }

    //-----------------------------Permission's logic start---------------------

    private boolean hasPermissions() {
        int res;

        for (String perms : PermissionUtils.VIDEO_PERMISSIONS) {
            res = mActivity.checkCallingOrSelfPermission(perms);
            if (!(res == PackageManager.PERMISSION_GRANTED)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Show permission grant bottom dialog if user reject permissions
     */
    private void requestPermissionWithRationale() {
        if (checkVideoPermissionsRationale()) {
            Snackbar.make(mParentView, R.string.camera_permission_explain, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.grant, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            requestPerms();
                        }
                    })
                    .show();
        } else {
            requestPerms();
        }
    }

    private boolean checkPhotoPermissionsRationale() {
        return ActivityCompat.shouldShowRequestPermissionRationale(mActivity, Manifest.permission.CAMERA);
    }

    private boolean checkVideoPermissionsRationale() {
        return checkPhotoPermissionsRationale() &&
                ActivityCompat.shouldShowRequestPermissionRationale(mActivity, Manifest.permission.RECORD_AUDIO);
    }

    private void requestPerms() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mActivity.requestPermissions(PermissionUtils.VIDEO_PERMISSIONS, Contract.REQUEST_CAMERA_PERMISSION);
        }
    }

    //-----------------------------Permission's logic end---------------------

    /**
     * Interface to notify controller about record video events
     */
    public interface IVideoRecordingListener {
        void closeCameraView();
    }

    /**
     * Contract class for constants fields
     */
    static final class Contract {
        static final int VIDEO_WIDTH = 1920;
        static final int VIDEO_HEIGHT = 1080;

        static final String CAMERA_BACK = "0";
        static final int VIDEO_BIT_RATE = 2000000;
        static final int REQUEST_CAMERA_PERMISSION = 1001;
        static final int SHARE_RESULT_CODE = 101;
        static final int CAMERA_ORIENTATION = 90;
        static final int TIMER_DELAY = 0;
        static final int TIMER_PERIOD = 1000;
        static final String VIDEO_MIME_TYPE = "video/mp4";
        static final String VIDEO_HANDLER_NAME = "Camera Background";
    }
}

package app.kuzubov.com.cameratest.video_recording;

import android.Manifest;

public class PermissionUtils {

    public static final String[] PHOTO_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public static final String[] VIDEO_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };

    public static final String[] SPEECH_RECOGNITION_PERMISSIONS = new String[]{
            Manifest.permission.RECORD_AUDIO,
    };
}

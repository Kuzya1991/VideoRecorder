package app.kuzubov.com.videorecorder.video_recording.helpers;

import android.Manifest;

/**
 * Helper class to work with permissions
 */

public class PermissionUtils {

    public static final String[] PHOTO_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public static final String[] VIDEO_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };
}

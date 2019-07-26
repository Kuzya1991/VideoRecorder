package app.kuzubov.com.videorecorder.video_recording.helpers;

import android.content.Context;

import java.io.File;
import java.io.IOException;

/**
 * Helper class to simplify working with cash files
 */

@SuppressWarnings("ResultOfMethodCallIgnored")
public class FileHelper {

    private static final String TEMP_DIR = "CacheDirectory";
    private static final String TEMP_VIDEO = "CameraVideo";

    private final Context mContext;

    public FileHelper (Context context){
        mContext = context;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void clearCache(){
        File dir = new File(mContext.getCacheDir(), TEMP_VIDEO);

        String[] children = dir.list();
        if(children == null) return;
        for (String aChildren : children) {
            (new File(dir, aChildren)).delete();
        }
        dir.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public File getVideoFile() {
        File myDir = new File(mContext.getCacheDir(), TEMP_DIR);
        if(!myDir.exists()){
            myDir.mkdir();
        }
        try {
            return File.createTempFile(System.currentTimeMillis() + "", ".mp4", myDir);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}

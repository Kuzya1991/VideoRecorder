package app.kuzubov.com.cameratest.video_recording;

import java.io.File;
import java.io.IOException;

public class FileHelper {

    public static final String TEMP_DIR = "CacheDirectory";
    public static final String TEMP_VIDEO = "CameraVideo";

    private static FileHelper mIam;

    public static FileHelper getInstance(){
        if(mIam == null){
            mIam = new FileHelper();
        }
        return mIam;
    }

    private FileHelper(){}

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public File prepareTempFile(){
        File myDir = new File(CameraTextApplication.getAppContext().getCacheDir(), TEMP_DIR);
        if(!myDir.exists()){
            myDir.mkdir();
        }
        try {
            return File.createTempFile(System.currentTimeMillis() + "", ".jpg", myDir);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void clearCache(){
        File dir = new File(CameraTextApplication.getAppContext().getCacheDir(), TEMP_DIR);

        String[] children = dir.list();
        if(children == null) return;
        for (String aChildren : children) {
            (new File(dir, aChildren)).delete();
        }
        dir.delete();
    }

    public void clearVideoCache(){
        File dir = new File(CameraTextApplication.getAppContext().getCacheDir(), TEMP_VIDEO);

        String[] children = dir.list();
        if(children == null) return;
        for (String aChildren : children) {
            (new File(dir, aChildren)).delete();
        }
        dir.delete();
    }

    public File getVideoFile() {
        File myDir = new File(CameraTextApplication.getAppContext().getCacheDir(), TEMP_DIR);
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

    public File prepareVideoFile(String name) {
        File myDir = new File(CameraTextApplication.getAppContext().getCacheDir(), TEMP_VIDEO);
        if(!myDir.exists()){
            myDir.mkdir();
        }
        try {
            return File.createTempFile(name, ".mp4", myDir);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public File getVideoDir(){
        File myDir = new File(CameraTextApplication.getAppContext().getCacheDir(), TEMP_VIDEO);
        if(!myDir.exists()){
            myDir.mkdir();
        }
        return myDir;
    }

    public File findFileByName(String name) {
        File myDir = new File(CameraTextApplication.getAppContext().getCacheDir(), TEMP_VIDEO);
        if(!myDir.exists()){
            myDir.mkdir();
        }
        for(File file : myDir.listFiles()){
            if(file.getName().contains(name)){
                return file;
            }
        }
        return null;
    }
}

package app.kuzubov.com.cameratest.video_recording;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

@SuppressLint("Registered")
public class CameraTextApplication extends Application {

    @SuppressLint("StaticFieldLeak") //This variable will be used throughout all app lifecycle
    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
    }

    public static Context getAppContext(){
        return mContext;
    }
}

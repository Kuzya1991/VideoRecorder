package app.kuzubov.com.cameratest.video_recording;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import static android.content.Intent.EXTRA_CHOSEN_COMPONENT;

public class ShareVideoReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
//        context.sendBroadcast();
        Log.d("trololo", "onReceive : " + intent.toString());
        String selectedAppPackage = String.valueOf(intent.getExtras().get(EXTRA_CHOSEN_COMPONENT));
        Log.d("trololo", "onReceive  package : " + selectedAppPackage);
        for (String key : intent.getExtras().keySet()) {
            Log.d("trololo", "key : " + key);
        }
    }
}

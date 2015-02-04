package org.mozilla.gecko.loadinbackground;

import org.mozilla.gecko.BrowserApp;
import org.mozilla.gecko.GeckoSharedPrefs;
import org.mozilla.gecko.R;
import org.mozilla.gecko.preferences.GeckoPreferences;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

public class LoadInBackgroundPromptService extends Service {

    private WindowManager windowManager;
    private WindowManager.LayoutParams mParams;
    private View layout;


    @Override
    public IBinder onBind(Intent intent) {
        // Not used
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);


        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        layout = layoutInflater.inflate(R.layout.loadinbackground_prompt, null);

        mParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.FILL_PARENT,
                WindowManager.LayoutParams.FILL_PARENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        //mParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        mParams.x = 0;
        mParams.y = 0;
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {

        layout.findViewById(R.id.ok_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GeckoSharedPrefs.forApp(getBaseContext()).edit().putBoolean(GeckoPreferences.PREFS_OPEN_IN_BACKGROUND, true).apply();
                intent.setClass(getApplicationContext(), LoadInBackgroundService.class);
                getApplicationContext().startService(intent);
                hide();
            }
        });
        layout.findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent forwardIntent = new Intent(intent);
                forwardIntent.setClass(getApplicationContext(), BrowserApp.class);
                getApplicationContext().startActivity(intent);
                hide();
            }
        });

        windowManager.addView(layout, mParams);

        return super.onStartCommand(intent, flags, startId);
    }

    private void hide() {
        windowManager.removeView(layout);
    }


}
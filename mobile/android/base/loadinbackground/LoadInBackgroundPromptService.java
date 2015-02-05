package org.mozilla.gecko.loadinbackground;

import org.mozilla.gecko.BrowserApp;
import org.mozilla.gecko.GeckoSharedPrefs;
import org.mozilla.gecko.R;
import org.mozilla.gecko.preferences.GeckoPreferences;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

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
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        mParams.x = 0;
        mParams.y = 0;
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {

        layout.findViewById(R.id.ok_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hide();
                GeckoSharedPrefs.forApp(getBaseContext()).edit().putBoolean(GeckoPreferences.PREFS_OPEN_IN_BACKGROUND, true).apply();
                intent.setClass(getApplicationContext(), LoadInBackgroundService.class);
                getBaseContext().startService(intent);
            }
        });
        layout.findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hide();
                intent.setClass(getApplicationContext(), BrowserApp.class);
                getBaseContext().startActivity(intent);

            }
        });

        windowManager.addView(layout, mParams);

        // Start the slide-up animation.
        Animation anim = AnimationUtils.loadAnimation(this, R.anim.overlay_slide_up);
        layout.findViewById(R.id.loadinbackground_container).startAnimation(anim);


        return super.onStartCommand(intent, flags, startId);
    }

    private void hide() {
        windowManager.removeView(layout);
    }



}
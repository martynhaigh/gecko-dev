package org.mozilla.gecko;

import org.mozilla.gecko.mozglue.ContextUtils;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import ch.boye.httpclientandroidlib.util.TextUtils;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoadInBackgroundService extends Service {

    private WindowManager windowManager;
    private View layout;
    private TextView mMessageView;
    private Button mButton;
    private GeckoProfile mProfile;
    private final Handler mHideHandler = new Handler();
    public static int LENGTH_SHORT = 3000;
    private boolean mShown = false;
    private WindowManager.LayoutParams mParams;
    private HideRunnable mHideRunnable;


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
        layout = layoutInflater.inflate(R.layout.button_toast, null);

        mMessageView = (TextView) layout.findViewById(R.id.toast_message);
        mButton = (Button) layout.findViewById(R.id.toast_button);


        mButton.setEnabled(true);
        mMessageView.setText("Open in background");
        mButton.setText("Open now");

        mParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        mParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        mParams.x = 0;
        mParams.y = 0;
    }

    private abstract class HideRunnable implements Runnable {

        private boolean mShouldHide = true;

        public void shouldHide(boolean hide) {
            mShouldHide = hide;
        }

        public void run() {
            if (mShouldHide) {
                hide();
            }
            execute();
        }

        public abstract void execute();

    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        if (mHideRunnable != null) {
            Log.d("MTEST", "runnable already running - run it immediately and cancel the timeout - count ");

            mHideHandler.removeCallbacks(mHideRunnable);
            mHideRunnable.shouldHide(false);
            mHideRunnable.run();
        } else {
            Log.d("MTEST", "Add view to window ");

            windowManager.addView(layout, mParams);
        }

        mHideRunnable = new HideRunnable() {
            @Override
            public void execute() {
                addUrlToList(intent);
                mHideRunnable = null;
            }
        };
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("MTEST", "button clicked ");

                mHideHandler.removeCallbacks(mHideRunnable);
                mHideRunnable = null;

                Intent forwardIntent = new Intent(intent);
                forwardIntent.setClass(getApplicationContext(), BrowserApp.class);
                forwardIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(forwardIntent);
                hide();
            }
        });

        mHideHandler.postDelayed(mHideRunnable, LENGTH_SHORT);

        return super.onStartCommand(intent, flags, startId);
    }

    private void hide() {
        Log.d("MTEST", "HIDE!!!");
        windowManager.removeView(layout);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    public synchronized GeckoProfile getProfile() {
        // fall back to default profile if we didn't load a specific one
        if (mProfile == null) {
            mProfile = GeckoProfile.get(this);
        }
        return mProfile;
    }

    private void addUrlToList(Intent intentParam) {
        final ContextUtils.SafeIntent intent = new ContextUtils.SafeIntent(intentParam);
        final String action = intent.getAction();
        final String args = intent.getStringExtra("args");
        String intentData = intent.getDataString();
        getProfile();

//        Log.d("MTEST", "Gecko in background: " + GeckoApplication.get().isApplicationInBackground());
        Log.d("MTEST", "Intent data: " + intentData);


        if (mProfile == null) {
            String profileName = null;
            String profilePath = null;
            if (args != null) {
                Pattern p;
                Matcher m;
                if (args.contains("-P")) {
                    p = Pattern.compile("(?:-P\\s*)(\\w*)(\\s*)");
                    m = p.matcher(args);
                    if (m.find()) {
                        profileName = m.group(1);
                    }
                }

                if (args.contains("-profile")) {
                    p = Pattern.compile("(?:-profile\\s*)(\\S*)(\\s*)");
                    m = p.matcher(args);
                    if (m.find()) {
                        profilePath = m.group(1);
                    }
                    if (profileName == null) {
                        profileName = GeckoProfile.DEFAULT_PROFILE;
                    }
                    GeckoProfile.sIsUsingCustomProfile = true;
                }

                if (profileName != null || profilePath != null) {
                    mProfile = GeckoProfile.get(this, profileName, profilePath);
                }
            }
        }

        appendSiteToList("temp_reading_list.json", intentData);

//        boolean readingListExists = false;
//        String readingListContent = null;
//        try {
//            readingListContent = mProfile.readFile("temp_reading_list.json");
//        } catch (IOException e) {
//            Log.d("MTEST", "Error reading file");
//
//            e.printStackTrace();
//        }
//        readingListExists = !TextUtils.isEmpty(readingListContent);
//        String format = readingListExists ? "~%s" : "%s";
//        Log.d("MTEST", "Reading list contents:" + readingListContent);
//        Log.d("MTEST", "Reading list exists:" + readingListExists);
//        mProfile.appendToFile("temp_reading_list.json", String.format(format, intentData));
//        Log.d("MTEST", "Added " + intentData);
    }

    private void appendSiteToList(String filename, String url) {
        String readingListContent = null;
        try {
            readingListContent = mProfile.readFile("temp_reading_list.json");
        } catch (IOException e) {
            Log.d("MTEST", "Error reading file");

            e.printStackTrace();
        }
        boolean readingListExists = !TextUtils.isEmpty(readingListContent);
        JSONArray jsonArray = null;
        if (readingListExists) {
            try {
                jsonArray = new JSONArray(readingListContent);
            } catch (JSONException e) {
                Log.d("MTEST", "Error parsing JSONARRAY");
                e.printStackTrace();
            }
        } else {
            jsonArray = new JSONArray();
        }
        jsonArray.put(url);

        Log.d("MTEST", "Reading list contents:" + readingListContent);
        Log.d("MTEST", "Reading list exists:" + readingListExists);
        mProfile.writeFile("temp_reading_list.json", jsonArray.toString());
        Log.d("MTEST", "Added " + url);
    }

}


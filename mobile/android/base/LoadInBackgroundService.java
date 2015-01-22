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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
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
    private AtomicInteger mCount = new AtomicInteger(0);
    private boolean mShown = false;
    private WindowManager.LayoutParams mParams;
    private Runnable mHideRunnable;


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

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        Log.d("MTEST", "onStartCommand - count " + mCount);

        mCount.incrementAndGet();
        if (mHideRunnable != null) {
            Log.d("MTEST", "runnable already running - run it immediately and cancel the timeout - count " + mCount);

            mHideHandler.removeCallbacks(mHideRunnable);
            mHideRunnable.run();
        }

        mHideRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d("MTEST", "hide runnable - count " + mCount);
                mCount.decrementAndGet();

                addUrlToList(intent);

                if (mCount.get() == 0) {
                    hide();
                }
                mHideRunnable = null;
            }
        };
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("MTEST", "button click - count " + mCount);

                mCount.decrementAndGet();
                mHideHandler.removeCallbacks(mHideRunnable);
                mHideRunnable = null;

                Intent forwardIntent = new Intent(intent);
                forwardIntent.setClass(getApplicationContext(), BrowserApp.class);
                forwardIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(forwardIntent);
                hide();
            }
        });

        windowManager.addView(layout, mParams);
        mHideHandler.postDelayed(mHideRunnable, LENGTH_SHORT);
        Log.d("MTEST", "SHOWING STUFF");
        return super.onStartCommand(intent, flags, startId);
    }

    private void hide() {
        Log.d("MTEST", "HIDE!!!");
        windowManager.removeView(layout);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //hide();
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

        Log.d("MTEST", "Gecko in background: " + GeckoApplication.get().isApplicationInBackground());
        Log.d("MTEST", "Intent data: " + intentData);

        URL url;
        String htmlContent = "";
        try {
            url = new URL(intentData);

            HttpURLConnection urlConnection = (HttpURLConnection) url
                    .openConnection();

            Map<String, List<String>> map = urlConnection.getHeaderFields();
            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                Log.d("MTEST", "Key : " + entry.getKey() +
                        " ,Value : " + entry.getValue());
            }
            htmlContent = convertInputStreamToString(urlConnection.getInputStream());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Log.d("MTEST", "HTML CONTENT: " + htmlContent);
        htmlContent = htmlContent.replaceAll("\\s+", " ");
        Pattern p = Pattern.compile("<title>(.*?)</title>");
        Matcher m = p.matcher(htmlContent);
        String htmlTitle = "";
        while (m.find() == true) {
            htmlTitle = m.group(1);
            Log.d("MTEST", "MATCH: " + htmlTitle);
        }

        if (mProfile == null) {
            String profileName = null;
            String profilePath = null;
            if (args != null) {
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

        try {
            Log.d("MTEST", "Reading list contents: " + mProfile.readFile("temp_reading_list.json"));
        } catch (IOException e) {
            Log.d("MTEST", "Couldn't read : " + e.getMessage());

            e.printStackTrace();
        }
        mProfile.appendToFile("temp_reading_list.json", String.format("%s|%s", intentData, htmlTitle);
    }

    private String convertInputStreamToString(InputStream inputStream) throws IOException {

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        String line = "";
        String result = "";

        while ((line = bufferedReader.readLine()) != null) {
            result += line;
        }

            /* Close Stream */
        if (null != inputStream) {
            inputStream.close();
        }

        return result;
    }
}


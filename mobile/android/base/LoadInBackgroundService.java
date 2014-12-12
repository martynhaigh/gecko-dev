package org.mozilla.gecko;

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

public class LoadInBackgroundService extends Service {

    private static final String LOGTAG = "MTEST - LoadInBackgroundService";
//    private static final String LOGTAG = "LoadInBackgroundService";

    private WindowManager windowManager;
    private View layout;
    private TextView mMessageView;
    private Button mButton;
    private final Handler mHideHandler = new Handler();
    public static int LENGTH_SHORT = 3000;
    private boolean mShown = false;
    private WindowManager.LayoutParams mParams;
    private IntentRunnable mHideRunnable;
    private boolean mReplace = false;
    private Object mMutex = new Object();

    public LoadInBackgroundService() {
        super();
        Log.i(LOGTAG, "Start service");
    }

    protected String getURIFromIntent(Intent intent) {
        return intent.getDataString();
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

    private abstract class IntentRunnable implements Runnable {
        final Intent mIntent;

        public IntentRunnable(Intent intent) {
            Log.d(LOGTAG, "IntentRunnable created with intent : " + intent);
            mIntent = intent;
        }

        public Intent getIntent() {
            return mIntent;
        }
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        if (null == intent) {
            Log.d(LOGTAG, "Empty intent received - quitting early");
            return super.onStartCommand(intent, flags, startId);
        }
        Log.d(LOGTAG, "onStartCommand - shown " + mShown + " startId " + startId + " - " + intent);

        if (mShown) {
            mReplace = true;
            Log.d(LOGTAG, "runnable already running - run it immediately and cancel the timeout - mShown " + mShown);

            mHideHandler.removeCallbacks(mHideRunnable);
            mHideRunnable.run();
            Log.d(LOGTAG, "hide runnable fired");
            mReplace = false;
        }

        mHideRunnable = new IntentRunnable(intent) {

            @Override
            public void run() {

                Log.d(LOGTAG, "hide runnable - " + getIntent());

                Intent intent = getIntent();
                intent.setClass(LoadInBackgroundService.this, BackgroundGeckoService.class);
                startService(intent);

                if (!mReplace) {
                    hide();
                }
                mHideRunnable = null;
            }
        };
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(LOGTAG, "button click - mShown " + mShown);

                mHideHandler.removeCallbacks(mHideRunnable);
                mHideRunnable = null;

                Intent forwardIntent = new Intent(intent);
                forwardIntent.setClass(getApplicationContext(), BrowserApp.class);
                forwardIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(forwardIntent);
                hide();
            }
        });
        if (!mShown) {
            windowManager.addView(layout, mParams);
            mShown = true;
            Log.d(LOGTAG, "Added view to window manager");
        }
        mHideHandler.postDelayed(mHideRunnable, LENGTH_SHORT);

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void hide() {
        mShown = false;
        Log.d(LOGTAG, "HIDE!!!");
        windowManager.removeView(layout);
    }


}


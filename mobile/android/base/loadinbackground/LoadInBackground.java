package org.mozilla.gecko.loadinbackground;

import org.mozilla.gecko.AppConstants;
import org.mozilla.gecko.BrowserApp;
import org.mozilla.gecko.GeckoAppShell;
import org.mozilla.gecko.GeckoSharedPrefs;
import org.mozilla.gecko.Locales;
import org.mozilla.gecko.R;
import org.mozilla.gecko.Telemetry;
import org.mozilla.gecko.TelemetryContract;
import org.mozilla.gecko.preferences.GeckoPreferences;
import org.mozilla.gecko.sync.setup.activities.WebURLFinder;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

public class LoadInBackground extends Locales.LocaleAwareActivity {
    private static final String LOGTAG = "LoadInBackground";

    private SharedPreferences prefs;

    // Flag set during animation to prevent animation multiple-start.
    private boolean isAnimating;

    final private String PREFERENCE_NAME = "external_opens";
    final private int OPENS_BEFORE_PROMPT = 3;
    private View containerView;
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GeckoAppShell.ensureCrashHandling();
        getWindow().setWindowAnimations(0);

        intent = getIntent();

        // For the moment lets exit early if we're not in Nightly
        if (!AppConstants.NIGHTLY_BUILD) {
            loadNormally();
            finish();
        }

//        final Resources resources = getResources();

        // The URL is usually hiding somewhere in the extra text. Extract it.
        final String dataString = intent.getDataString();
        if (TextUtils.isEmpty(dataString)) {
            abortDueToNoURL();
            return;
        }

        final String pageUrl = new WebURLFinder(dataString).bestWebURL();
        if (TextUtils.isEmpty(pageUrl)) {
            abortDueToNoURL();
            return;
        }

        String subjectText = intent.getStringExtra(Intent.EXTRA_SUBJECT);

        Log.d("MTEST", "LIBA: Processing pageUrl: " + pageUrl);
        Log.d("MTEST", "LIBA: Processing dataString: " + dataString);
        Log.d("MTEST", "LIBA: Processing subjectText: " + subjectText);

        //Telemetry.sendUIEvent(TelemetryContract.Event.SHOW, TelemetryContract.Method.SHARE_OVERLAY, telemetryExtras);


        prefs = GeckoSharedPrefs.forApp(this);
        boolean showOpenInBackgroundToast = prefs.getBoolean(GeckoPreferences.PREFS_OPEN_IN_BACKGROUND, false);
        int timesOpened = prefs.getInt(PREFERENCE_NAME, 1);
        Log.d("MTEST", "Opened in BG " + timesOpened + " times");

        // Don't inflate a layout - we're using this activity to simply decide if we want to show the overlay toast
        // which happens in the service, or to open fennec as normal.
        if (showOpenInBackgroundToast) {
            intent.setClass(getApplicationContext(), LoadInBackgroundService.class);
            startService(intent);
            finish();
        } else if (true || timesOpened == OPENS_BEFORE_PROMPT) {
            showLoadInBackgroundEnablePrompt();


        } else {
            loadNormally();
        }

        prefs.edit().putInt(PREFERENCE_NAME, timesOpened + 1).apply();


    }

    private void showLoadInBackgroundEnablePrompt() {

        //Remove title bar
        //this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        //Remove notification bar
        //this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.loadinbackground_prompt);
        containerView = findViewById(R.id.loadinbackground_container);

        findViewById(R.id.ok_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Telemetry.sendUIEvent(TelemetryContract.Event.ACTION, TelemetryContract.Method.OPEN_IN_BACKGROUND);

                GeckoSharedPrefs.forApp(getBaseContext()).edit().putBoolean(GeckoPreferences.PREFS_OPEN_IN_BACKGROUND, true).apply();
                intent.setClass(getApplicationContext(), LoadInBackgroundService.class);
                startService(intent);
                finish();
            }
        });
        findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Telemetry.sendUIEvent(TelemetryContract.Event.NOT_NOW, TelemetryContract.Method.OPEN_IN_BACKGROUND);


                intent.setClass(getApplicationContext(), BrowserApp.class);
                startActivity(intent);
                finish();
            }
        });

        // Start the slide-up animation.
        Animation anim = AnimationUtils.loadAnimation(this, R.anim.overlay_slide_up);
        findViewById(R.id.loadinbackground_container).startAnimation(anim);


        // Start the slide-up animation.
       // anim = AnimationUtils.loadAnimation(this, R.anim.abc_fade_in);
        //findViewById(R.id.background_container).startAnimation(anim);
    }

    private void loadNormally() {
        Intent forwardIntent = new Intent(getIntent());
        forwardIntent.setClass(getApplicationContext(), BrowserApp.class);
        startActivity(forwardIntent);
        finish();
    }


    @Override
    public void finish() {
        super.finish();

        // Don't perform an activity-dismiss animation.
        overridePendingTransition(0, 0);
    }

    /**
     * Show a toast indicating we were started with no URL, and then stop.
     */
    private void abortDueToNoURL() {
        Log.e(LOGTAG, "Unable to process shared intent. No URL found!");

        // Display toast notifying the user of failure (most likely a developer who screwed up
        // trying to send a share intent).
        Toast toast = Toast.makeText(this, getResources().getText(R.string.overlay_share_no_url), Toast.LENGTH_SHORT);
        toast.show();
        finish();
    }

    /**
     * Slide the overlay down off the screen and destroy it.
     */
    private void slideOut() {
        if (isAnimating) {
            return;
        }

        isAnimating = true;
        Animation anim = AnimationUtils.loadAnimation(this, R.anim.overlay_slide_down);
        findViewById(R.id.loadinbackground_container).startAnimation(anim);

        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // Unused. I can haz Miranda method?
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                finish();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // Unused.
            }
        });
    }

    /**
     * Close the dialog if back is pressed.
     */
    @Override
    public void onBackPressed() {
        slideOut();
        Telemetry.sendUIEvent(TelemetryContract.Event.CANCEL, TelemetryContract.Method.OPEN_IN_BACKGROUND);
    }

    /**
     * Close the dialog if the anything that isn't a button is tapped.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        slideOut();
        Telemetry.sendUIEvent(TelemetryContract.Event.CANCEL, TelemetryContract.Method.OPEN_IN_BACKGROUND);
        return true;
    }


//    @Override
//    public boolean dispatchTouchEvent(MotionEvent ev) {
//        Rect dialogBounds = new Rect();
//        getWindow().getDecorView().getHitRect(dialogBounds);
//
//        if (!dialogBounds.contains((int) ev.getX(), (int) ev.getY())) {
//            // Tapped outside so we finish the activity
//            this.finish();
//        }
//        return super.dispatchTouchEvent(ev);
//    }
}

package org.mozilla.gecko.loadinbackground;

import org.mozilla.gecko.GeckoAppShell;
import org.mozilla.gecko.GeckoSharedPrefs;
import org.mozilla.gecko.Locales;
import org.mozilla.gecko.R;
import org.mozilla.gecko.Telemetry;
import org.mozilla.gecko.TelemetryContract;
import org.mozilla.gecko.preferences.GeckoPreferences;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

public class LoadInBackgroundPrompt extends Locales.LocaleAwareActivity {
    private static final String LOGTAG = "LoadInBackground";


    // Flag set during animation to prevent animation multiple-start.
    private boolean isAnimating;

    private View containerView;
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GeckoAppShell.ensureCrashHandling();
        getWindow().setWindowAnimations(0);

        intent = getIntent();

        showLoadInBackgroundEnablePrompt();


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

                GeckoSharedPrefs.forApp(getBaseContext()).edit().putBoolean(GeckoPreferences.PREFS_OPEN_IN_BACKGROUND_ENABLED, true).apply();
                intent.setClass(getApplicationContext(), LoadInBackgroundService.class);
                startService(intent);
                setResult(LoadInBackgroundHelper.LOAD_IN_BACKGROUND_TRY_IT);
                finish();
            }
        });
        findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Telemetry.sendUIEvent(TelemetryContract.Event.NOT_NOW, TelemetryContract.Method.OPEN_IN_BACKGROUND);

                setResult(LoadInBackgroundHelper.LOAD_IN_BACKGROUND_OPEN_NOW);
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
        setResult(LoadInBackgroundHelper.LOAD_IN_BACKGROUND_CANCEL);

    }

    /**
     * Close the dialog if the anything that isn't a button is tapped.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        slideOut();
        Telemetry.sendUIEvent(TelemetryContract.Event.CANCEL, TelemetryContract.Method.OPEN_IN_BACKGROUND);
        setResult(LoadInBackgroundHelper.LOAD_IN_BACKGROUND_CANCEL);

        return true;
    }

}

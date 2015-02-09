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
    public static final String LOAD_URLS = "LOAD_IN_BACKGROUND_LOAD_URLS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        // For the moment lets exit early if we're not in Nightly
        if (!AppConstants.NIGHTLY_BUILD) {
            loadNormally(intent);
            finish();
        }

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

        //Telemetry.sendUIEvent(TelemetryContract.Event.SHOW, TelemetryContract.Method.SHARE_OVERLAY, telemetryExtras);

        boolean showOpenInBackgroundToast = GeckoSharedPrefs.forApp(this).getBoolean(GeckoPreferences.PREFS_OPEN_IN_BACKGROUND, false);

        // Don't inflate a layout - we're using this activity to simply decide if we want to show the overlay toast
        // which happens in the service, or to open fennec as normal.
        if (showOpenInBackgroundToast) {
            showToast(intent);
        } else {
            loadNormally(intent);
        }
    }

    private void showToast(Intent intent) {
        intent.setClass(getApplicationContext(), LoadInBackgroundService.class);
        startService(intent);
        finish();
    }

    private void loadNormally(Intent intent) {
        intent.setClass(getApplicationContext(), BrowserApp.class);
        startActivity(intent);
        finish();
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
}

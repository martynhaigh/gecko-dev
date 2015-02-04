package org.mozilla.gecko.loadinbackground;

import org.mozilla.gecko.AppConstants;
import org.mozilla.gecko.BrowserApp;
import org.mozilla.gecko.GeckoAppShell;
import org.mozilla.gecko.GeckoSharedPrefs;
import org.mozilla.gecko.preferences.GeckoPreferences;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

public class LoadInBackground extends Activity {

    private SharedPreferences prefs;

    final private String PREFERENCE_NAME = "external_opens";
    final private int OPENS_BEFORE_PROMPT = 3;

    final private boolean forcePrompt = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        GeckoAppShell.ensureCrashHandling();
        super.onCreate(savedInstanceState);

        Intent forwardIntent = new Intent(getIntent());

        String intentData = forwardIntent.getDataString();
        Log.d("MTEST", "LIBA: Processing URL: " + intentData);

        // For the moment lets exit early if we're not in Nightly
        if (!AppConstants.NIGHTLY_BUILD) {
            loadNormally();
            finish();
        }

        prefs = GeckoSharedPrefs.forApp(this);
        boolean showOpenInBackgroundToast = prefs.getBoolean(GeckoPreferences.PREFS_OPEN_IN_BACKGROUND, false);
        int timesOpened = prefs.getInt(PREFERENCE_NAME, 1);
        Log.d("MTEST", "Opened in BG " + timesOpened + " times");

        // Don't inflate a layout - we're using this activity to simply decide if we want to show the overlay toast
        // which happens in the service, or to open fennec as normal.
        if (showOpenInBackgroundToast) {
            forwardIntent.setClass(getApplicationContext(), LoadInBackgroundService.class);
            startService(forwardIntent);
        } else if (forcePrompt || timesOpened == OPENS_BEFORE_PROMPT) {
            forwardIntent.setClass(getApplicationContext(), LoadInBackgroundPromptService.class);
            startService(forwardIntent);
        } else {
            loadNormally();
        }

        prefs.edit().putInt(PREFERENCE_NAME, timesOpened + 1).apply();

        finish();
    }

    private void loadNormally() {
        Intent forwardIntent = new Intent(getIntent());
        forwardIntent.setClass(getApplicationContext(), BrowserApp.class);
        startActivity(forwardIntent);
    }
}

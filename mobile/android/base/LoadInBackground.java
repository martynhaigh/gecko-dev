package org.mozilla.gecko;

import org.mozilla.gecko.preferences.GeckoPreferences;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class LoadInBackground extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        GeckoAppShell.ensureCrashHandling();
        super.onCreate(savedInstanceState);

        // Don't inflate a layout - we're using this activity to simply decide if we want to show the overlay toast
        // which happens in the service, or to open fennec as normal.

        Intent forwardIntent = new Intent(getIntent());

        boolean showOpenInBackgroundToast = GeckoSharedPrefs.forApp(this).getBoolean(GeckoPreferences.PREFS_OPEN_IN_BACKGROUND, false);

        if (AppConstants.NIGHTLY_BUILD && showOpenInBackgroundToast) {
            forwardIntent.setClass(getApplicationContext(), LoadInBackgroundService.class);
            startService(forwardIntent);
        } else {
            forwardIntent.setClass(getApplicationContext(), BrowserApp.class);
            startActivity(forwardIntent);
        }

        finish();
    }
}

/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.tabqueue;

import org.mozilla.gecko.AppConstants;
import org.mozilla.gecko.BrowserApp;
import org.mozilla.gecko.GeckoApplication;
import org.mozilla.gecko.GeckoSharedPrefs;
import org.mozilla.gecko.Locales;
import org.mozilla.gecko.R;
import org.mozilla.gecko.Telemetry;
import org.mozilla.gecko.TelemetryContract;
import org.mozilla.gecko.preferences.GeckoPreferences;
import org.mozilla.gecko.sync.setup.activities.WebURLFinder;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

public class TabQueue extends Locales.LocaleAwareActivity {
    private static final String LOGTAG = "TabQueue";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        // For the moment lets exit early and start fennec as normal if we're not in nightly with
        // the tab queue build flag.
        if (!(AppConstants.MOZ_ANDROID_TAB_QUEUE && AppConstants.NIGHTLY_BUILD)) {
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

        boolean showOpenInBackgroundToast = GeckoSharedPrefs.forApp(this).getBoolean(GeckoPreferences.PREFS_TAB_QUEUE_ENABLED, false);

        // Don't inflate a layout - we're using this activity to simply decide if we want to show the overlay toast
        // which happens in the service, or to open fennec as normal.
        if (showOpenInBackgroundToast) {
            showToast(intent);
        } else {
            loadNormally(intent);
        }
    }

    private void showToast(Intent intent) {
        Telemetry.sendUIEvent(TelemetryContract.Event.LAUNCH, TelemetryContract.Method.TAB_QUEUE);

        intent.setClass(getApplicationContext(), TabQueueService.class);
        startService(intent);
        finish();
    }

    private void loadNormally(Intent intent) {
        Telemetry.sendUIEvent(TelemetryContract.Event.LOAD_URL, TelemetryContract.Method.TAB_QUEUE);

        intent.setClass(getApplicationContext(), BrowserApp.class);
        startActivity(intent);
        finish();
    }

    /**
     * Abort as we were started with no URL.
     */
    private void abortDueToNoURL() {
        Log.d(LOGTAG, "Unable to process tab queue insertion. No URL found!");
        finish();
    }
}
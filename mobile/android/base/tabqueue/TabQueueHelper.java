/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.tabqueue;

import org.mozilla.gecko.BrowserApp;
import org.mozilla.gecko.GeckoProfile;
import org.mozilla.gecko.GeckoSharedPrefs;
import org.mozilla.gecko.R;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.StrictMode;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;

public class TabQueueHelper {
    private static final String LOGTAG = "Gecko" + TabQueueHelper.class.getSimpleName();

    public static final String FILE_NAME = "tab_queue_url_list.json";
    public static final long TOAST_TIMEOUT = 3000;
    public static final String LOAD_URLS_ACTION = "TAB_QUEUE_LOAD_URLS_ACTION";

    public static final int TAB_QUEUE_NOTIFICATION_ID = 783;

    public static final String PREF_TAB_QUEUE_COUNT = "tab_queue_count";


    /**
     * Reads file and converts any content to JSON, adds passed in url to the data and writes back to the file,
     * creating the file if it doesn't already exist.  This should not be run on the main thread.
     *
     * @param profile
     * @param url     URL to add
     */
    static public int queueUrl(GeckoProfile profile, String url) {

        String readingListContent = null;
        try {
            readingListContent = profile.readFile(FILE_NAME);
        } catch (IOException e) {
            e.printStackTrace();
        }

        JSONArray jsonArray = new JSONArray();
        if (!TextUtils.isEmpty(readingListContent)) {
            try {
                jsonArray = new JSONArray(readingListContent);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        jsonArray.put(url);

        profile.writeFile(FILE_NAME, jsonArray.toString());

        return jsonArray.length();
    }

    static public void showNotification(Context context, int tabsQueued) {
        Intent resultIntent = new Intent(context, BrowserApp.class);
        resultIntent.setAction(TabQueueHelper.LOAD_URLS_ACTION);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, TabQueueHelper.TAB_QUEUE_NOTIFICATION_ID, resultIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_status_logo)
                        .setContentTitle(context.getResources().getQuantityString(R.plurals.tab_queue_notification_title, tabsQueued))
                        .setContentText(context.getResources().getQuantityString(R.plurals.tab_queue_notification_message, tabsQueued, tabsQueued));

        mBuilder.setContentIntent(pendingIntent);

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(TabQueueHelper.TAB_QUEUE_NOTIFICATION_ID, mBuilder.build());

        // Store the number of URLs queued so that we don't have to read the FILE_NAME to see if we have
        // any urls to open
        final StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
        try {
            final SharedPreferences prefs = GeckoSharedPrefs.forApp(context);

            int openInBackgroundCount = prefs.getInt(TabQueueHelper.PREF_TAB_QUEUE_COUNT, 0);
            prefs.edit().putInt(TabQueueHelper.PREF_TAB_QUEUE_COUNT, openInBackgroundCount + 1).apply();
        } finally {
            StrictMode.setThreadPolicy(savedPolicy);
        }
    }
}
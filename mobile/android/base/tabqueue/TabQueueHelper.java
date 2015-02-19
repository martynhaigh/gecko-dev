/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.tabqueue;

import org.mozilla.gecko.BrowserApp;
import org.mozilla.gecko.GeckoProfile;
import org.mozilla.gecko.R;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
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

    /**
     * Displays a notification showing the total number of tabs queue.  If there is already a notification displayed, it
     * will be replaced.
     * @param context
     * @param tabsQueued
     */
    static public void showNotification(Context context, int tabsQueued) {
        Intent resultIntent = new Intent(context, BrowserApp.class);
        resultIntent.setAction(TabQueueHelper.LOAD_URLS_ACTION);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, TabQueueHelper.TAB_QUEUE_NOTIFICATION_ID, resultIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_status_logo)
                        .setContentTitle(context.getResources().getQuantityString(R.plurals.tab_queue_notification_title, tabsQueued))
                        .setContentText(context.getResources().getQuantityString(R.plurals.tab_queue_notification_message, tabsQueued, tabsQueued));

        builder.setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(TabQueueHelper.TAB_QUEUE_NOTIFICATION_ID, builder.build());
    }
}
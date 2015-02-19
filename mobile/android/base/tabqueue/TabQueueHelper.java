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
import org.mozilla.gecko.BrowserApp;
import org.mozilla.gecko.GeckoAppShell;
import org.mozilla.gecko.GeckoEvent;
import org.mozilla.gecko.GeckoProfile;
import org.mozilla.gecko.GeckoSharedPrefs;
import org.mozilla.gecko.R;
import org.mozilla.gecko.preferences.GeckoPreferences;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.StrictMode;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class TabQueueHelper {
    private static final String LOGTAG = "Gecko" + TabQueueHelper.class.getSimpleName();


    // result codes for returning from the prompt
    public static final int TAB_QUEUE_TRY_IT = 201;
    public static final int TAB_QUEUE_NOT_NOW = 202;
    public static final int TAB_QUEUE_CANCEL = 203;

    public static final int MAX_TIMES_TO_SHOW_PROMPT = 3;
    public static final int EXTERNAL_LAUNCHES_BEFORE_SHOWING_PROMPT = 3;

    public static final int ACTIVITY_REQUEST_TAB_QUEUE = 1002;

    public static final String FILE_NAME = "tab_queue_url_list.json";
    public static final int TOAST_TIMEOUT = 3000;
    public static final String LOAD_URLS_ACTION = "TAB_QUEUE_LOAD_URLS_ACTION";

    public static final int TAB_QUEUE_NOTIFICATION_ID = 783;

    public static final String PREF_TAB_QUEUE_LAUNCHES = "tab_queue_launches";
    public static final String PREF_TAB_QUEUE_COUNT = "tab_queue_count";
    public static final String PREF_TAB_QUEUE_TIMES_PROMPT_SHOWN = "tab_queue_times_prompt_shown";

    /**
     * Check if we should show the tab queue prompt
     */
    public static boolean shouldShowTabQueuePrompt(Context context) {
        Log.d("MTEST" + LOGTAG, "TabQueueHelper - shouldShowTabQueuePrompt - [context]");
        final StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
        try {
            final SharedPreferences prefs = GeckoSharedPrefs.forApp(context);

            boolean tabQueueEnabled = prefs.getBoolean(GeckoPreferences.PREFS_TAB_QUEUE, false);
            int timesPromptShown = prefs.getInt(PREF_TAB_QUEUE_TIMES_PROMPT_SHOWN, 0);

            // exit early if the feature is enabled or the user has seen the
            // prompt more than MAX_TIMES_TO_SHOW_PROMPT times.
            if (tabQueueEnabled || timesPromptShown >= MAX_TIMES_TO_SHOW_PROMPT) {
                Log.d("MTEST" + LOGTAG, "TabQueueHelper - shouldShowTabQueuePrompt - NEVER AGAIN!");
                return false;
            }

            final int timesOpened = prefs.getInt(PREF_TAB_QUEUE_LAUNCHES, 0) + 1;

            if (timesOpened < EXTERNAL_LAUNCHES_BEFORE_SHOWING_PROMPT) {
                // Allow a few external links to open before we prompt the user
                prefs.edit().putInt(PREF_TAB_QUEUE_LAUNCHES, timesOpened).apply();
                Log.d("MTEST" + LOGTAG, "TabQueueHelper - shouldShowTabQueuePrompt - false");

                return false;
            } else if (timesOpened == EXTERNAL_LAUNCHES_BEFORE_SHOWING_PROMPT) {
                Log.d("MTEST" + LOGTAG, "TabQueueHelper - shouldShowTabQueuePrompt - true");

                // Show the prompt
                return true;
            }
        } finally {
            StrictMode.setThreadPolicy(savedPolicy);
        }
        Log.d("MTEST" + LOGTAG, "TabQueueHelper - shouldShowTabQueuePrompt - false");

        return false;
    }

    static public void showTabQueuePrompt(Activity activity) {
        Log.d("MTEST" + LOGTAG, "TabQueueHelper - showTabQueuePrompt - [activity]");

        activity.startActivityForResult(new Intent(activity, TabQueuePrompt.class), ACTIVITY_REQUEST_TAB_QUEUE);
    }

    static public boolean shouldOpenTabQueueUrls(Context context) {
        final StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
        try {
            final SharedPreferences prefs = GeckoSharedPrefs.forApp(context);

            boolean tabQueueEnabled = prefs.getBoolean(GeckoPreferences.PREFS_TAB_QUEUE, false);
            int tabsQueued = prefs.getInt(PREF_TAB_QUEUE_COUNT, 0);
            Log.d("MTEST" + LOGTAG, "shouldOpenTabQueueUrls ! " + (tabQueueEnabled && tabsQueued > 0));

            return tabQueueEnabled && tabsQueued > 0;
        } finally {
            StrictMode.setThreadPolicy(savedPolicy);
        }
    }

    static public int getTabQueueLength(Context context) {
        Log.d("MTEST" + LOGTAG, "getTabQueueLength ! ");

        final StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
        try {
            final SharedPreferences prefs = GeckoSharedPrefs.forApp(context);
            Log.d("MTEST" + LOGTAG, "getTabQueueLength ! " + prefs.getInt(PREF_TAB_QUEUE_COUNT, 0));

            return prefs.getInt(PREF_TAB_QUEUE_COUNT, 0);
        } finally {
            StrictMode.setThreadPolicy(savedPolicy);
        }
    }

    /**
     * Add a url to the tab queue, create a notification
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
    
    /**
     * Opens the file with filename of {@link TabQueueHelper#FILE_NAME} and constructs a JSON object to send to
     * the JS which will open the tabs and optionally send a callback notification.
     *
     * @param context
     * @param profile
     * @param performCallback Specify is the JS will perform a callback on the "Tabs:TabsOpened" event after opening the passed in tabs
     */
    static public void openQueuedUrls(Context context, GeckoProfile profile, boolean performCallback) {
        Log.d("MTEST" + LOGTAG, "TabQueueHelper - openQueuedUrls");
        //remove the notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(TAB_QUEUE_NOTIFICATION_ID);

        // exit early if we don't have any tabs queued
        if (getTabQueueLength(context) < 1) {
            return;
        }

        String readingList = null;
        try {
            readingList = profile.readFile(FILE_NAME);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!TextUtils.isEmpty(readingList)) {
            JSONArray jsonArray;

            try {
                jsonArray = new JSONArray(readingList);
            } catch (JSONException e) {
                jsonArray = null;
                e.printStackTrace();
            }

            if (jsonArray != null) {
                JSONArray dataArray = new JSONArray();
                JSONObject jsonObject;

                for (int i = 0; i < jsonArray.length(); i++) {
                    String site;
                    try {
                        site = jsonArray.getString(i);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        continue;
                    }

                    if (!TextUtils.isEmpty(site)) {
                        jsonObject = new JSONObject();

                        // construct the object as expected by the JS
                        try {
                            jsonObject.put("url", site);
                            jsonObject.put("isPrivate", false);
                            jsonObject.put("desktopMode", false);
                            dataArray.put(jsonObject);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        // NO-OP
                    }
                }

                JSONObject data = new JSONObject();
                try {
                    data.put("urls", dataArray);
                    data.put("performCallback", performCallback);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                GeckoAppShell.sendEventToGecko(GeckoEvent.createBroadcastEvent("Tabs:OpenMultiple", data.toString()));
            }

            profile.deleteFile(FILE_NAME);

            final StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
            try {
                final SharedPreferences prefs = GeckoSharedPrefs.forApp(context);
                prefs.edit().remove(PREF_TAB_QUEUE_COUNT).apply();
            } finally {
                StrictMode.setThreadPolicy(savedPolicy);
            }
        }
    }
    /**
     * Opens the file with filename of {@link TabQueueHelper#FILE_NAME} and constructs a JSON object to send to
     * the JS which will open the tabs and optionally send a callback notification.
     *
     * @param context
     * @param profile
     * @param performCallback Specify is the JS will perform a callback on the "Tabs:TabsOpened" event after opening the passed in tabs
     */
    static public void openQueuedUrls(Context context, GeckoProfile profile, boolean performCallback) {
        Log.d("MTEST" + LOGTAG, "TabQueueHelper - openQueuedUrls");
        //remove the notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(TAB_QUEUE_NOTIFICATION_ID);

        // exit early if we don't have any tabs queued
        if (getTabQueueLength(context) < 1) {
            return;
        }

        String readingList = null;
        try {
            readingList = profile.readFile(FILE_NAME);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!TextUtils.isEmpty(readingList)) {
            JSONArray jsonArray;

            try {
                jsonArray = new JSONArray(readingList);
            } catch (JSONException e) {
                jsonArray = null;
                e.printStackTrace();
            }

            if (jsonArray != null) {
                JSONArray dataArray = new JSONArray();
                JSONObject jsonObject;

                for (int i = 0; i < jsonArray.length(); i++) {
                    String site;
                    try {
                        site = jsonArray.getString(i);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        continue;
                    }

                    if (!TextUtils.isEmpty(site)) {
                        jsonObject = new JSONObject();

                        // construct the object as expected by the JS
                        try {
                            jsonObject.put("url", site);
                            jsonObject.put("isPrivate", false);
                            jsonObject.put("desktopMode", false);
                            dataArray.put(jsonObject);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        // NO-OP
                    }
                }

                JSONObject data = new JSONObject();
                try {
                    data.put("urls", dataArray);
                    data.put("performCallback", performCallback);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                GeckoAppShell.sendEventToGecko(GeckoEvent.createBroadcastEvent("Tabs:OpenMultiple", data.toString()));
            }

            profile.deleteFile(FILE_NAME);

            final StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
            try {
                final SharedPreferences prefs = GeckoSharedPrefs.forApp(context);
                prefs.edit().remove(PREF_TAB_QUEUE_COUNT).apply();
            } finally {
                StrictMode.setThreadPolicy(savedPolicy);
            }
        }
    }
}
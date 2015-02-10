package org.mozilla.gecko.tabqueue;

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
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by martyn on 09/02/15.
 */
public class TabQueueHelper {

    public static final int TAB_QUEUE_TRY_IT = 201;
    public static final int TAB_QUEUE_OPEN_NOW = 202;
    public static final int TAB_QUEUE_CANCEL = 203;

    public static final String TAB_QUEUE_PROMPT_TIMES_PROMPT_SHOWN = "tab_queue_prompt_times_prompt_shown";
    public static final int MAX_TIMES_TO_SHOW = 3;

    public static final String TAB_QUEUE_LAUNCHES = "external_opens";
    public static final int TAB_QUEUE_LAUNCHES_BEFORE_PROMPT = 3;

    public static final int ACTIVITY_REQUEST_TAB_QUEUE = 1002;

    public static final String TAB_QUEUE_FILE_NAME = "tab_queue_url_list.json";
    public static final int TAB_QUEUE_NOTIFICATION_ID = 783;
    public static final String TAB_QUEUE_COUNT = "tab_queue_count";
    public static final int LENGTH_SHORT = 3000;
    public static final String LOAD_URLS_ACTION = "TAB_QUEUE_LOAD_URLS";


    /**
     * Check and show the tab queue prompt if the user has launched TAB_QUEUE_LAUNCHES_BEFORE_PROMPT external links
     */
    public static boolean shouldShowTabQueuePrompt(Context context) {
        final StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
        try {
            final SharedPreferences prefs = GeckoSharedPrefs.forApp(context);
            boolean showToast = prefs.getBoolean(GeckoPreferences.PREFS_TAB_QUEUE_ENABLED, false);
            int timesPromptShown = prefs.getInt(TAB_QUEUE_PROMPT_TIMES_PROMPT_SHOWN, 0);

            if (showToast || timesPromptShown > MAX_TIMES_TO_SHOW) {
                // exit early if the option is enabled or the user has seen the prompt too many times.
                Log.d("MTEST", "CHECK: Toast activated - exit");
                return false;
            }

            final int timesOpened = prefs.getInt(TAB_QUEUE_LAUNCHES, 0) + 1;
            Log.d("MTEST", "CHECK: Times opened = " + timesOpened);

            if (timesOpened < TAB_QUEUE_LAUNCHES_BEFORE_PROMPT) {
                // Allow a few external links to open before we prompt the user
                prefs.edit().putInt(TAB_QUEUE_LAUNCHES, timesOpened).apply();
            } else if (timesOpened == TAB_QUEUE_LAUNCHES_BEFORE_PROMPT) {
                prefs.edit().putInt(TAB_QUEUE_LAUNCHES, TAB_QUEUE_LAUNCHES_BEFORE_PROMPT + 1).apply();
                return true;
            }
        } finally {
            StrictMode.setThreadPolicy(savedPolicy);
        }
        return false;
    }

    static public void showTabQueuePrompt(Activity activity) {
        activity.startActivityForResult(new Intent(activity, TabQueuePrompt.class), ACTIVITY_REQUEST_TAB_QUEUE);
    }

    static public boolean shouldProcessTabQueue(Context context) {
        final StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
        try {
            final SharedPreferences prefs = GeckoSharedPrefs.forApp(context);
            return prefs.getBoolean(GeckoPreferences.PREFS_TAB_QUEUE_ENABLED, false);
        } finally {
            StrictMode.setThreadPolicy(savedPolicy);
        }
    }

    static public int getTabQueueLength(Context context) {
        final StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
        try {
            final SharedPreferences prefs = GeckoSharedPrefs.forProfile(context);
            return prefs.getInt(TAB_QUEUE_COUNT, 0);
        } finally {
            StrictMode.setThreadPolicy(savedPolicy);
        }
    }

    static public void queueUrl(Context context, String url, GeckoProfile profile) {
        Log.d("MTEST", "queueUrl");

        String readingListContent = null;
        try {
            readingListContent = profile.readFile(TAB_QUEUE_FILE_NAME);
        } catch (IOException e) {
            e.printStackTrace();
        }
        boolean readingListExists = !ch.boye.httpclientandroidlib.util.TextUtils.isEmpty(readingListContent);
        JSONArray jsonArray = null;
        if (readingListExists) {
            try {
                jsonArray = new JSONArray(readingListContent);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            jsonArray = new JSONArray();
        }
        Log.d("MTEST", "Adding: " + url);
        Log.d("MTEST", "Sites Queued: " + jsonArray.length() + " -> " + (1 + jsonArray.length()));

        jsonArray.put(url);

        Log.d("MTEST", jsonArray.toString());

        profile.writeFile(TAB_QUEUE_FILE_NAME, jsonArray.toString());


        // create the notification
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_status_logo)
                        .setContentTitle(context.getResources().getQuantityString(R.plurals.tab_queue_notification_title, jsonArray.length()))
                        .setContentText(context.getResources().getQuantityString(R.plurals.tab_queue_notification_message, jsonArray.length(), jsonArray.length()));

        Intent resultIntent = new Intent(context, BrowserApp.class);
        resultIntent.setAction(TabQueueHelper.LOAD_URLS_ACTION);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, TabQueueHelper.TAB_QUEUE_NOTIFICATION_ID, resultIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        mBuilder.setContentIntent(pendingIntent);
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(TabQueueHelper.TAB_QUEUE_NOTIFICATION_ID, mBuilder.build());

        // Store the number of URLs queued
        final StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
        try {
            final SharedPreferences prefs = GeckoSharedPrefs.forProfile(context);
            int openInBackgroundCount = prefs.getInt(TabQueueHelper.TAB_QUEUE_COUNT, 0);
            prefs.edit().putInt(TabQueueHelper.TAB_QUEUE_COUNT, openInBackgroundCount + 1).apply();
        } finally {
            StrictMode.setThreadPolicy(savedPolicy);
        }
    }

    static public void openQueuedUrls(Context context, GeckoProfile profile, boolean performCallback) {
        //remove the notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(TAB_QUEUE_NOTIFICATION_ID);

        // exit early if we don't have any tabs queued
        if(getTabQueueLength(context) < 1)  {
            return;
        }

        String readingList = null;
        try {
            readingList = profile.readFile(TAB_QUEUE_FILE_NAME);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d("MTEST", "Opening URLs.  List content: " + readingList);

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
                    Log.d("MTEST", "opening " + site);

                    if (!TextUtils.isEmpty(site)) {
                        jsonObject = new JSONObject();
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

            profile.deleteFile(TAB_QUEUE_FILE_NAME);

            final StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
            try {
                final SharedPreferences prefs = GeckoSharedPrefs.forProfile(context);
                prefs.edit().remove(TAB_QUEUE_COUNT).apply();
            } finally {
                StrictMode.setThreadPolicy(savedPolicy);
            }
        }
    }
}

package org.mozilla.gecko.loadinbackground;

import org.mozilla.gecko.GeckoAppShell;
import org.mozilla.gecko.GeckoEvent;
import org.mozilla.gecko.GeckoProfile;
import org.mozilla.gecko.GeckoSharedPrefs;
import org.mozilla.gecko.preferences.GeckoPreferences;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.StrictMode;
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
public class LoadInBackgroundHelper {

    public static final int LOAD_IN_BACKGROUND_TRY_IT = 201;
    public static final int LOAD_IN_BACKGROUND_OPEN_NOW = 202;
    public static final int LOAD_IN_BACKGROUND_CANCEL = 203;

    public static final String OPEN_IN_BACKGROUND_PROMPT_TIMES_PROMPT_SHOWN = "open_in_background_prompt_times_prompt_shown";
    public static final int MAX_TIMES_TO_SHOW = 3;

    public static final String OPEN_IN_BACKGROUND_LAUNCHES = "external_opens";
    public static final int OPEN_IN_BACKGROUND_LAUNCHES_BEFORE_PROMPT = 3;

    public static final int ACTIVITY_REQUEST_OPEN_IN_BACKGROUND = 1002;


    /**
     * Check and show the firstrun pane if the browser has never been launched and
     * is not opening an external link from another application.
     */
    public static boolean shouldShowOpenInBackgroundPrompt(Context context) {


        final StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
        try {
            final SharedPreferences prefs = GeckoSharedPrefs.forProfile(context);
            boolean showOpenInBackgroundToast = prefs.getBoolean(GeckoPreferences.PREFS_OPEN_IN_BACKGROUND_ENABLED, false);
            int timesPromptShown = prefs.getInt(OPEN_IN_BACKGROUND_PROMPT_TIMES_PROMPT_SHOWN, 0);

            if (showOpenInBackgroundToast || timesPromptShown > MAX_TIMES_TO_SHOW) {
                // exit early if the option is enabled or the user has seen the prompt too many times.
                Log.d("MTEST", "CHECK: Toast activated - exit");
                return false;
            }

            final int timesOpened = prefs.getInt(OPEN_IN_BACKGROUND_LAUNCHES, 0) + 1;
            Log.d("MTEST", "CHECK: Times opened = " + timesOpened);

            if (timesOpened < OPEN_IN_BACKGROUND_LAUNCHES_BEFORE_PROMPT) {
                // Allow a few external links to open before we prompt the user
                prefs.edit().putInt(OPEN_IN_BACKGROUND_LAUNCHES, timesOpened).apply();
            } else if (timesOpened == OPEN_IN_BACKGROUND_LAUNCHES_BEFORE_PROMPT) {
                prefs.edit().putInt(OPEN_IN_BACKGROUND_LAUNCHES, OPEN_IN_BACKGROUND_LAUNCHES_BEFORE_PROMPT + 1).apply();
                return true;
            }
        } finally {
            StrictMode.setThreadPolicy(savedPolicy);
        }
        return false;
    }

    static public void showOpenInBackgroundPrompt(Activity activity) {
        activity.startActivityForResult(new Intent(activity, LoadInBackgroundPrompt.class), ACTIVITY_REQUEST_OPEN_IN_BACKGROUND);
    }

    static public boolean shouldProcessOpenInBackgroundQueue(Context context) {
        final StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
        try {
            final SharedPreferences prefs = GeckoSharedPrefs.forProfile(context);
            return prefs.getBoolean(GeckoPreferences.PREFS_OPEN_IN_BACKGROUND_ENABLED, false);
        } finally {
            StrictMode.setThreadPolicy(savedPolicy);
        }
    }

    static public int getOpenInBackgroundUrlCount(Context context) {
        final StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
        try {
            final SharedPreferences prefs = GeckoSharedPrefs.forProfile(context);
            return prefs.getInt(LoadInBackgroundService.OPEN_IN_BACKGROUND_COUNT, 0);
        } finally {
            StrictMode.setThreadPolicy(savedPolicy);
        }
    }

    static public void processOpenInBackgroundUrls(Context context, GeckoProfile profile) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(LoadInBackgroundService.OPEN_IN_BACKGROUND_NOTIFICATION_ID);

        if(getOpenInBackgroundUrlCount(context) < 1)  {
            return;
        }
        Log.d("MTEST", "Processing processOpenInBackgroundUrls");
        final String filename = "open_in_background_url_list.json";

        // Check background load tabs
        String readingList = null;
        try {
            readingList = profile.readFile(filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d("MTEST", "reading list - found " + readingList);

        if (!TextUtils.isEmpty(readingList)) {
            JSONArray jsonArray;
            try {
                jsonArray = new JSONArray(readingList);

            } catch (JSONException e) {
                Log.e("MTEST", "ERROR parsing reading list");
                jsonArray = null;
                e.printStackTrace();
            }

            if (jsonArray != null) {
                JSONArray dataArray = new JSONArray();

                Log.d("MTEST", "reading list - found " + jsonArray.length());

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
                        try {
                            jsonObject.put("url", site);
                            jsonObject.put("isPrivate", false);
                            jsonObject.put("desktopMode", false);
                            dataArray.put(jsonObject);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Log.d("MTEST", " - empty");
                    }
                }

                JSONObject data = new JSONObject();
                try {
                    data.put("urls", dataArray);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                GeckoAppShell.sendEventToGecko(GeckoEvent.createBroadcastEvent("Tabs:OpenMultiple", data.toString()));

            }
            Toast.makeText(context, "Found" + jsonArray.length() + " sites", Toast.LENGTH_SHORT).show();
            profile.deleteFile(filename);
            final StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
            try {
                final SharedPreferences prefs = GeckoSharedPrefs.forProfile(context);
                prefs.edit().putInt(LoadInBackgroundService.OPEN_IN_BACKGROUND_COUNT, 0).apply();
            } finally {
                StrictMode.setThreadPolicy(savedPolicy);
            }
        }
    }
}

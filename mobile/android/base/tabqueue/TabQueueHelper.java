/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.tabqueue;

import org.mozilla.gecko.GeckoProfile;
import org.mozilla.gecko.GeckoSharedPrefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;

public class TabQueueHelper {
    private static final String LOGTAG = "Gecko" + TabQueueHelper.class.getSimpleName();

    public static final String FILE_NAME = "tab_queue_url_list.json";
    public static final long TOAST_TIMEOUT = 3000;
    public static final String LOAD_URLS_ACTION = "TAB_QUEUE_LOAD_URLS_ACTION";

    public static final String PREF_TAB_QUEUE_COUNT = "tab_queue_count";

    /**
     * Add a url to the tab queue, create a notification
     *
     * @param context
     * @param profile
     * @param url     URL to add
     */
    static public void queueUrl(Context context, GeckoProfile profile, String url) {
        Log.d("MTEST" + LOGTAG, "TabQueueHelper - queueUrl - " + url);

        String readingListContent = null;
        try {
            readingListContent = profile.readFile(FILE_NAME);
            Log.d("MTEST" + LOGTAG, "TabQueueHelper - queueUrl - content=" + readingListContent);
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
        Log.d("MTEST" + LOGTAG, "Writing file!");
    }
}
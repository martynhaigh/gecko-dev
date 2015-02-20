/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.tabqueue;

import org.mozilla.gecko.GeckoProfile;
import org.mozilla.gecko.util.ThreadUtils;

import android.text.TextUtils;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;

public class TabQueueHelper {
    private static final String LOGTAG = "Gecko" + TabQueueHelper.class.getSimpleName();

    public static final String FILE_NAME = "tab_queue_url_list.json";
    public static final long TOAST_TIMEOUT = 3000;

    /**
     * Reads file and converts any content to JSON, adds passed in url to the data and writes back to the file,
     * creating the file if it doesn't already exist.  This should not be run on the main thread.
     *
     * @param profile
     * @param url     URL to add
     */
    public static void queueUrl(GeckoProfile profile, String url) {
        ThreadUtils.assertNotOnUiThread();

        String readingListContent = null;
        try {
            readingListContent = profile.readFile(FILE_NAME);
        } catch (IOException e) {
            Log.e(LOGTAG, "Error reading Tab Queue file contents.");
        }

        JSONArray jsonArray;
        try {
            jsonArray = new JSONArray(readingListContent);
        } catch (JSONException e) {
            jsonArray = new JSONArray();
            Log.e(LOGTAG, "Error converting Tab Queue data to JSON.");
        }

        jsonArray.put(url);

        profile.writeFile(FILE_NAME, jsonArray.toString());
    }
}
/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.tabqueue;

import org.mozilla.gecko.GeckoProfile;

import android.text.TextUtils;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;

public class TabQueueHelper {
    private static final String LOGTAG = "Gecko" + TabQueueHelper.class.getSimpleName();

    public static final String FILE_NAME = "tab_queue_url_list.json";
    public static final long TOAST_TIMEOUT = 3000;

    /**
     * Reads file and converts any content to JSON, adds passed in url to the data and writes back to the file,
     * creating the file if it doesn't already exist.
     *
     * @param profile
     * @param url     URL to add
     */
    static public void queueUrl(GeckoProfile profile, String url) {

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
    }
}
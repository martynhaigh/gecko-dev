/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.tabs;

import org.mozilla.gecko.R;
import org.mozilla.gecko.tabs.TabsPanel;
import org.mozilla.gecko.widget.TabThumbnailWrapper;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

interface TabsLayout extends TabsPanel.PanelView {
    public void setEmptyView(View view);
    public void closeAll();

    // ViewHolder for a row in the list
    public static class TabRow {
        int id;
        TextView title;
        ImageView thumbnail;
        ImageButton close;
        ViewGroup info;
        TabThumbnailWrapper thumbnailWrapper;

        public TabRow(View view) {
            info = (ViewGroup) view;
            title = (TextView) view.findViewById(R.id.title);
            thumbnail = (ImageView) view.findViewById(R.id.thumbnail);
            close = (ImageButton) view.findViewById(R.id.close);
            thumbnailWrapper = (TabThumbnailWrapper) view.findViewById(R.id.wrapper);
        }
    }
}

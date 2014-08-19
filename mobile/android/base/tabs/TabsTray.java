/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.tabs;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import org.mozilla.gecko.R;
import org.mozilla.gecko.tabs.TabsPanel;
import org.mozilla.gecko.widget.TabThumbnailWrapper;

interface TabsTray extends TabsPanel.PanelView{
    public void setIsPrivate(boolean isPrivate);
    public void setEmptyView(View view);
    public void closeAll();

    public enum TabsTrayType {
        LIST, GRID;

        private int resId;

        static {
            LIST.resId = R.layout.tabs_tray_list;
            GRID.resId = R.layout.tabs_tray_grid;
        }

        public int getLayoutId() {
            return resId;
        }
    }

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

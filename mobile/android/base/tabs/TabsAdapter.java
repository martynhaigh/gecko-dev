/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
/* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this file,
* You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.tabs;

import java.util.ArrayList;
import java.util.List;

import org.mozilla.gecko.animation.ViewHelper;
import org.mozilla.gecko.R;
import org.mozilla.gecko.Tab;
import org.mozilla.gecko.tabs.TabsTray.TabRow;
import org.mozilla.gecko.tabs.TabsTray;
import org.mozilla.gecko.Tabs;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.util.Log;

// Adapter to bind tabs into a list
public abstract class TabsAdapter extends BaseAdapter implements Tabs.OnTabsChangedListener {
    private static final String LOGTAG = "TabsAdapter";
    private Context mContext;
    private ArrayList<Tab> mTabs;
    private LayoutInflater mInflater;
    private Button.OnClickListener mOnCloseClickListener;
    private AdapterView<ListAdapter> mAdapterView;

    public TabsAdapter(Context context, AdapterView<ListAdapter> adapterView) {
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        mAdapterView = adapterView;

        mOnCloseClickListener = new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                TabRow tab = (TabRow) v.getTag();
                final int pos = (isVertical() ? tab.info.getWidth() : 0 - tab.info.getHeight());
                animateClose(tab.info, pos);
            }
        };
    }

    abstract boolean isVertical();
    abstract void animateClose(final View view, int pos);
    abstract boolean isPrivate();
    abstract int getOriginalSize();
    abstract void setItemChecked(int pos, boolean isSelected);

    @Override
    public void onTabChanged(Tab tab, Tabs.TabEvents msg, Object data) {
        switch (msg) {
            case ADDED:
                // Refresh the list to make sure the new tab is added in the right position.
                refreshTabsData();
                break;

            case CLOSED:
                removeTab(tab);
                break;

            case SELECTED:
                // Update the selected position, then fall through...
                updateSelectedPosition();
            case UNSELECTED:
                // We just need to update the style for the unselected tab...
            case THUMBNAIL:
            case TITLE:
            case RECORDING_CHANGE:
                View view = mAdapterView.getChildAt(getPositionForTab(tab) - mAdapterView.getFirstVisiblePosition());
                if (view == null)
                    return;

                TabRow row = (TabRow) view.getTag();
                assignValues(row, tab);
                break;
        }
    }

    public void refreshTabsData() {
        // Store a different copy of the tabs, so that we don't have to worry about
        // accidentally updating it on the wrong thread.
        mTabs = new ArrayList<Tab>();

        Iterable<Tab> tabs = Tabs.getInstance().getTabsInOrder();
        for (Tab tab : tabs) {
            if (tab.isPrivate() == isPrivate())
                mTabs.add(tab);
        }

        notifyDataSetChanged(); // Be sure to call this whenever mTabs changes.
        updateSelectedPosition();
    }

    // Updates the selected position in the list so that it will be scrolled to the right place.
    private void updateSelectedPosition() {
        int selected = getPositionForTab(Tabs.getInstance().getSelectedTab());
        updateSelectedStyle(selected);

        if (selected != -1) {
            mAdapterView.setSelection(selected);
        }
    }

    /**
     * Updates the selected/unselected style for the tabs.
     *
     * @param selected position of the selected tab
     */
    private void updateSelectedStyle(int selected) {
        for (int i = 0; i < getCount(); i++) {
            setItemChecked(i, (i == selected));
        }
    }

    public void clear() {
        mTabs = null;
        notifyDataSetChanged(); // Be sure to call this whenever mTabs changes.
    }

    @Override
    public int getCount() {
        return (mTabs == null ? 0 : mTabs.size());
    }

    @Override
    public Tab getItem(int position) {
        return mTabs.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private int getPositionForTab(Tab tab) {
        if (mTabs == null || tab == null)
            return -1;

        return mTabs.indexOf(tab);
    }

    private void removeTab(Tab tab) {
        if (tab.isPrivate() == isPrivate() && mTabs != null) {
            mTabs.remove(tab);
            notifyDataSetChanged(); // Be sure to call this whenever mTabs changes.

            int selected = getPositionForTab(Tabs.getInstance().getSelectedTab());
            updateSelectedStyle(selected);
        }
    }

    private void assignValues(TabRow row, Tab tab) {
        if (row == null || tab == null)
            return;

        row.id = tab.getId();

        Drawable thumbnailImage = tab.getThumbnail();
        if (thumbnailImage != null) {
            row.thumbnail.setImageDrawable(thumbnailImage);
        } else {
            row.thumbnail.setImageResource(R.drawable.tab_thumbnail_default);
        }
        if (row.thumbnailWrapper != null) {
            row.thumbnailWrapper.setRecording(tab.isRecording());
        }
        row.title.setText(tab.getDisplayTitle());
        row.close.setTag(row);
    }

    private void resetTransforms(View view) {
        ViewHelper.setAlpha(view, 1);

        if (isVertical()) {
            ViewHelper.setTranslationX(view, 0);
        } else {
            ViewHelper.setTranslationY(view, 0);
        }

        // We only need to reset the height or width after individual tab close animations.
        if (getOriginalSize() != 0) {
            if (isVertical()) {
                ViewHelper.setHeight(view, getOriginalSize());
            } else {
                ViewHelper.setWidth(view, getOriginalSize());
            }
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TabRow row;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.tabs_row, null);
            row = new TabRow(convertView);
            row.close.setOnClickListener(mOnCloseClickListener);
            convertView.setTag(row);
        } else {
            row = (TabRow) convertView.getTag();
            // If we're recycling this view, there's a chance it was transformed during
            // the close animation. Remove any of those properties.
            resetTransforms(convertView);
        }

        Tab tab = mTabs.get(position);
        assignValues(row, tab);

        return convertView;
    }
}
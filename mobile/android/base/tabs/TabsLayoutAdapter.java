/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.tabs;

import java.util.ArrayList;
import java.util.List;

import org.mozilla.gecko.Tab;
import org.mozilla.gecko.R;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.view.View;

// Adapter to bind tabs into a list
public class TabsLayoutAdapter extends BaseAdapter {
    private Context mContext;
    private ArrayList<Tab> mTabs;
    private LayoutInflater mInflater;

    public TabsLayoutAdapter (Context context) {
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
    }

    final public void setTabs (ArrayList<Tab> tabs) {
        mTabs = tabs;
        notifyDataSetChanged(); // Be sure to call this whenever mTabs changes.
    }

    final public boolean removeTab (Tab tab) {
        boolean tabRemoved = mTabs.remove(tab);
        if (tabRemoved) {
            notifyDataSetChanged(); // Be sure to call this whenever mTabs changes.
        }
        return tabRemoved;
    }

    final public void clear() {
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

    final protected int getPositionForTab(Tab tab) {
        if (mTabs == null || tab == null)
            return -1;

        return mTabs.indexOf(tab);
    }

    @Override
    final public View getView(int position, View convertView, ViewGroup parent) {
        final Context context = parent.getContext();
        final Tab tab = mTabs.get(position);

        final TabsLayoutItemView item;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.tabs_row, null);
            item = newView(convertView);
        } else {
            item = (TabsLayoutItemView) convertView.getTag();
        }

        bindView(convertView, item, tab);

        return convertView;
    }

    public TabsLayoutItemView newView(View convertView) {
        TabsLayoutItemView item = new TabsLayoutItemView(convertView);
        convertView.setTag(item);
        return item;
    }

    public void bindView(View view, TabsLayoutItemView item, Tab tab) {
        item.assignValues(tab);
    }
}
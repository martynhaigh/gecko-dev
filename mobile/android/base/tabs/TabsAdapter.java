/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
/* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this file,
* You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.tabs;

import java.util.ArrayList;
import java.util.List;

import org.mozilla.gecko.R;
import org.mozilla.gecko.Tab;
import org.mozilla.gecko.tabs.TabsLayoutItemView;

import android.content.Context;
import android.widget.BaseAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

// Adapter to bind tabs into a list
public class TabsAdapter <T extends TabsLayoutItemView> extends BaseAdapter  {
    private Context mContext;
    private ArrayList<Tab> mTabs;
    private LayoutInflater mInflater;
    private Button.OnClickListener mOnCloseClickListener;
    private TabsLayoutFactory<T> mTabsLayoutFactory;

    public TabsAdapter(Context context, TabsLayoutFactory<T> tabsLayoutFactory) {
        mContext = context;
        mTabsLayoutFactory = tabsLayoutFactory;
        mInflater = LayoutInflater.from(mContext);
        mOnCloseClickListener = tabsLayoutFactory.createOnClickListener();
    }

    public static interface TabsLayoutFactory<T> {
      public T createItemView(View view, ViewGroup parent);
      public Button.OnClickListener createOnClickListener();
    }

    public void setTabs (ArrayList<Tab> tabs) {
        mTabs = tabs;
        notifyDataSetChanged(); // Be sure to call this whenever mTabs changes.
    }

    public boolean removeTab (Tab tab) {
        boolean tabRemoved = mTabs.remove(tab);
        if (tabRemoved) {
            notifyDataSetChanged(); // Be sure to call this whenever mTabs changes.
        }
        return tabRemoved;
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

    protected int getPositionForTab(Tab tab) {
        if (mTabs == null || tab == null)
            return -1;

        return mTabs.indexOf(tab);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TabsLayoutItemView item;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.tabs_row, null);
            item = mTabsLayoutFactory.createItemView(convertView, parent);
            item.close.setOnClickListener(mOnCloseClickListener);
            convertView.setTag(item);
        } else {
            item = (TabsLayoutItemView) convertView.getTag();
            // If we're recycling this view, there's a chance it was transformed during
            // the close animation. Remove any of those properties.
            item.resetView();
        }

        Tab tab = mTabs.get(position);
        item.assignValues(tab);

        return convertView;
    }
}
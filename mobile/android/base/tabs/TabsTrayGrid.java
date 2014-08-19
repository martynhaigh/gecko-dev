/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.tabs;

import java.util.ArrayList;
import java.util.List;

// import org.mozilla.gecko.AboutPages;
import org.mozilla.gecko.animation.PropertyAnimator.Property;
import org.mozilla.gecko.animation.PropertyAnimator;
import org.mozilla.gecko.animation.ViewHelper;
import org.mozilla.gecko.GeckoAppShell;
import org.mozilla.gecko.GeckoEvent;
import org.mozilla.gecko.R;
import org.mozilla.gecko.Tab;
import org.mozilla.gecko.tabs.TabsAdapter;
import org.mozilla.gecko.tabs.TabsTray;
import org.mozilla.gecko.Tabs;
import org.mozilla.gecko.util.ThreadUtils;
// import org.mozilla.gecko.widget.TabThumbnailWrapper;

import android.content.Context;
// import android.graphics.drawable.Drawable;
import android.graphics.Rect;
import android.util.AttributeSet;
// import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
// import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
// import android.widget.BaseAdapter;
// import android.widget.Button;
import android.widget.GridView;
// import android.widget.ImageButton;
// import android.widget.ImageView;
import android.widget.ListAdapter;
// import android.widget.TextView;

import android.util.Log;

class TabsTrayGrid extends GridView
                   implements TabsTray,
                              TabsPanel.PanelView,
                              TabsPanel.CloseAllPanelView {

    private static final String LOGTAG = "Gecko" + TabsTrayGrid.class.getSimpleName();

    private Context mContext;
    private TabsPanel mTabsPanel;

    private boolean mIsPrivate;

    private TabsGridAdapter mTabsAdapter;

    private List<View> mPendingClosedTabs;
    private int mCloseAnimationCount;
    private int mCloseAllAnimationCount;

    // Time to animate non-flinged tabs of screen, in milliseconds
    private static final int ANIMATION_DURATION = 250;

    // Time between starting successive tab animations in closeAllTabs.
    private static final int ANIMATION_CASCADE_DELAY = 75;


    private int mOriginalSize;

    public TabsTrayGrid(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        mPendingClosedTabs = new ArrayList<View>();

        setColumnWidth(20);
        setNumColumns(4);

        //setItemsCanFocus(true);

        mTabsAdapter = new TabsGridAdapter(mContext, this);
        setAdapter(mTabsAdapter);

        setRecyclerListener(new RecyclerListener() {
            @Override
            public void onMovedToScrapHeap(View view) {
                TabRow row = (TabRow) view.getTag();
                row.thumbnail.setImageDrawable(null);
                row.close.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void setIsPrivate(boolean isPrivate) {
        mIsPrivate = isPrivate;
    }

    @Override
    public void setTabsPanel(TabsPanel panel) {
        mTabsPanel = panel;
    }

    @Override
    public void show() {
        setVisibility(View.VISIBLE);
        Tabs.getInstance().refreshThumbnails();
        Tabs.registerOnTabsChangedListener(mTabsAdapter);
        mTabsAdapter.refreshTabsData();
    }

    @Override
    public void hide() {
        setVisibility(View.GONE);
        Tabs.unregisterOnTabsChangedListener(mTabsAdapter);
        GeckoAppShell.sendEventToGecko(GeckoEvent.createBroadcastEvent("Tab:Screenshot:Cancel",""));
        mTabsAdapter.clear();
    }

    @Override
    public boolean shouldExpand() {
        return isVertical();
    }

    private void autoHidePanel() {
        mTabsPanel.autoHidePanel();
    }


    public boolean isVertical() {
        return true;
    }
    @Override
    public void closeAll() {
        final int childCount = getChildCount();

        // Just close the panel if there are no tabs to close.
        if (childCount == 0) {
            autoHidePanel();
            return;
        }

        // Disable the view so that gestures won't interfere wth the tab close animation.
        setEnabled(false);

        // Delay starting each successive animation to create a cascade effect.
        int cascadeDelay = 0;

        for (int i = childCount - 1; i >= 0; i--) {
            final View view = getChildAt(i);
            final PropertyAnimator animator = new PropertyAnimator(ANIMATION_DURATION);
            animator.attach(view, Property.ALPHA, 0);

            if (isVertical()) {
                animator.attach(view, Property.TRANSLATION_X, view.getWidth());
            } else {
                animator.attach(view, Property.TRANSLATION_Y, view.getHeight());
            }

            mCloseAllAnimationCount++;

            animator.addPropertyAnimationListener(new PropertyAnimator.PropertyAnimationListener() {
                @Override
                public void onPropertyAnimationStart() { }

                @Override
                public void onPropertyAnimationEnd() {
                    mCloseAllAnimationCount--;
                    if (mCloseAllAnimationCount > 0) {
                        return;
                    }

                    // Hide the panel after the animation is done.
                    autoHidePanel();

                    // Re-enable the view after the animation is done.
                    TabsTrayGrid.this.setEnabled(true);

                    // Then actually close all the tabs.
                    final Iterable<Tab> tabs = Tabs.getInstance().getTabsInOrder();
                    for (Tab tab : tabs) {
                        // In the normal panel we want to close all tabs (both private and normal),
                        // but in the private panel we only want to close private tabs.
                        if (!mIsPrivate || tab.isPrivate()) {
                            Tabs.getInstance().closeTab(tab, false);
                        }
                    }
                }
            });

            ThreadUtils.getUiHandler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    animator.start();
                }
            }, cascadeDelay);

            cascadeDelay += ANIMATION_CASCADE_DELAY;
        }
    }

    private void animateClose(final View view, int pos) {
        PropertyAnimator animator = new PropertyAnimator(ANIMATION_DURATION);
        animator.attach(view, Property.ALPHA, 0);

        if (isVertical())
            animator.attach(view, Property.TRANSLATION_X, pos);
        else
            animator.attach(view, Property.TRANSLATION_Y, pos);

        mCloseAnimationCount++;
        mPendingClosedTabs.add(view);

        animator.addPropertyAnimationListener(new PropertyAnimator.PropertyAnimationListener() {
            @Override
            public void onPropertyAnimationStart() { }
            @Override
            public void onPropertyAnimationEnd() {
                mCloseAnimationCount--;
                if (mCloseAnimationCount > 0)
                    return;

                for (View pendingView : mPendingClosedTabs) {
                    animateFinishClose(pendingView);
                }

                mPendingClosedTabs.clear();
            }
        });

        if (mTabsAdapter.getCount() == 1)
            autoHidePanel();

        animator.start();
    }

    private void animateFinishClose(final View view) {
        PropertyAnimator animator = new PropertyAnimator(ANIMATION_DURATION);

        final boolean isVertical = isVertical();
        if (isVertical)
            animator.attach(view, Property.HEIGHT, 1);
        else
            animator.attach(view, Property.WIDTH, 1);

        TabRow tab = (TabRow)view.getTag();
        final int tabId = tab.id;

        // Caching this assumes that all rows are the same height
        if (mOriginalSize == 0) {
            mOriginalSize = (isVertical ? view.getHeight() : view.getWidth());
        }

        animator.addPropertyAnimationListener(new PropertyAnimator.PropertyAnimationListener() {
            @Override
            public void onPropertyAnimationStart() { }
            @Override
            public void onPropertyAnimationEnd() {
                Tabs tabs = Tabs.getInstance();
                Tab tab = tabs.getTab(tabId);
                tabs.closeTab(tab, true);
            }
        });

        animator.start();
    }

    private void animateCancel(final View view) {
        PropertyAnimator animator = new PropertyAnimator(ANIMATION_DURATION);
        animator.attach(view, Property.ALPHA, 1);

        if (isVertical())
            animator.attach(view, Property.TRANSLATION_X, 0);
        else
            animator.attach(view, Property.TRANSLATION_Y, 0);


        animator.addPropertyAnimationListener(new PropertyAnimator.PropertyAnimationListener() {
            @Override
            public void onPropertyAnimationStart() { }
            @Override
            public void onPropertyAnimationEnd() {
                TabRow tab = (TabRow) view.getTag();
                tab.close.setVisibility(View.VISIBLE);
            }
        });

        animator.start();
    }

    class TabsGridAdapter extends TabsAdapter {

        public TabsGridAdapter(Context context, AdapterView<ListAdapter> adapterView)  {
            super(context, adapterView);
        }

        @Override
        boolean isVertical() {
            return TabsTrayGrid.this.isVertical();
        }

        @Override
        void animateClose(final View view, int pos) {
            TabsTrayGrid.this.animateClose(view, pos);
        }

        @Override
        boolean isPrivate() {
            return mIsPrivate;
        }

        @Override
        void setItemChecked(int pos, boolean isSelected) {
            setItemChecked(pos, isSelected);
        }

        @Override
        int getOriginalSize() {
            return mOriginalSize;
        }

        @Override
        protected boolean shouldImplementClickHandler() {
            return true;
        }

        @Override
        protected void onItemClicked(View v) {
            TabRow tab = (TabRow) v.getTag();
            Tabs.getInstance().selectTab(tab.id);
            TabsTrayGrid.this.autoHidePanel();
        }

    }
}
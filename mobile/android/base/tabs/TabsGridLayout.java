/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.tabs;

import java.util.ArrayList;
import java.util.List;

import org.mozilla.gecko.AboutPages;
import org.mozilla.gecko.animation.PropertyAnimator.Property;
import org.mozilla.gecko.animation.PropertyAnimator;
import org.mozilla.gecko.animation.ViewHelper;
import org.mozilla.gecko.GeckoAppShell;
import org.mozilla.gecko.GeckoEvent;
import org.mozilla.gecko.R;
import org.mozilla.gecko.Tab;
import org.mozilla.gecko.tabs.TabsLayoutAdapter;
import org.mozilla.gecko.tabs.TabsPanel.TabsLayout;
import org.mozilla.gecko.Tabs;
import org.mozilla.gecko.util.ThreadUtils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.GridView;
import android.view.ViewGroup;
import android.view.ViewConfiguration;
import android.widget.Button;
import android.util.Log;
import android.os.Build;

import java.lang.Math;

/**
 * A tabs layout implementation for the tablet redesign (bug 1014156).
 * Expected to replace TabsListLayout once complete.
 */

class TabsGridLayout extends GridView
                     implements TabsLayout,
                                Tabs.OnTabsChangedListener {
    private static final String LOGTAG = "Gecko" + TabsGridLayout.class.getSimpleName();

    private Context mContext;
    private TabsPanel mTabsPanel;

    final private boolean mIsPrivate;

    private TabsLayoutAdapter mTabsAdapter;

    private List<View> mPendingClosedTabs;
    private int mCloseAnimationCount;
    private int mCloseAllAnimationCount;

    // Time to animate non-flinged tabs of screen, in milliseconds
    private static final int ANIMATION_DURATION = 250;

    // Time between starting successive tab animations in closeAllTabs.
    private static final int ANIMATION_CASCADE_DELAY = 75;

    private int mOriginalSize;
    
    public TabsGridLayout(Context context, AttributeSet attrs) {
        super(context, attrs, R.attr.tabGridLayoutViewStyle);
        mContext = context;

        mPendingClosedTabs = new ArrayList<View>();

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TabsTray);
        mIsPrivate = (a.getInt(R.styleable.TabsTray_tabs, 0x0) == 1);
        a.recycle();

        mTabsAdapter = new TabsGridLayoutAdapter(mContext);
        setAdapter(mTabsAdapter);

        setRecyclerListener(new RecyclerListener() {
            @Override
            public void onMovedToScrapHeap(View view) {
                TabsLayoutItemView item = (TabsLayoutItemView) view.getTag();
                item.thumbnail.setImageDrawable(null);
                item.close.setVisibility(View.VISIBLE);
            }
        });
    }

    private class TabsGridLayoutAdapter extends TabsLayoutAdapter {
        private Button.OnClickListener mOnClickListener;
        public TabsGridLayoutAdapter (Context context) {
            super(context);

            mOnClickListener = new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TabsLayoutItemView tab = (TabsLayoutItemView) v.getTag();
                    final int pos = (isVertical() ? tab.info.getWidth() : 0 - tab.info.getHeight());
                    animateClose(tab.info, pos);
                }
            };
        }

        @Override
        public View newView(int position, ViewGroup parent) {
            View view = super.newView(position, parent);

            // This is nasty and once we change TabsLayoutItemView to an actual view
            // we can get rid of it.
            TabsLayoutItemView item = (TabsLayoutItemView) view.getTag();
            item.close.setOnClickListener(mOnClickListener);

            return view;
        }

        @Override
        public void bindView(View view, Tab tab) {
            super.bindView(view, tab);
            view.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    TabsLayoutItemView tab = (TabsLayoutItemView) v.getTag();
                    Tabs.getInstance().selectTab(tab.id);
                    TabsGridLayout.this.autoHidePanel();
                }
            });

            // If we're recycling this view, there's a chance it was transformed during
            // the close animation. Remove any of those properties.
            TabsGridLayout.this.resetTransforms(view);
        }

    }

    @Override
    public void setTabsPanel(TabsPanel panel) {
        mTabsPanel = panel;
    }

    @Override
    public void show() {
        setVisibility(View.VISIBLE);
        Tabs.getInstance().refreshThumbnails();
        Tabs.registerOnTabsChangedListener(this);
        refreshTabsData();
    }

    @Override
    public void hide() {
        setVisibility(View.GONE);
        Tabs.unregisterOnTabsChangedListener(this);
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

    @Override
    public void onTabChanged(Tab tab, Tabs.TabEvents msg, Object data) {
        switch (msg) {
            case ADDED:
                // Refresh the list to make sure the new tab is added in the right position.
                refreshTabsData();
                break;

            case CLOSED:
               if (tab.isPrivate() == mIsPrivate && mTabsAdapter.getCount() > 0) {
                   if (mTabsAdapter.removeTab(tab)) {
                       int selected = mTabsAdapter.getPositionForTab(Tabs.getInstance().getSelectedTab());
                       updateSelectedStyle(selected);
                   }
               }
               break;

            case SELECTED:
                // Update the selected position, then fall through...
                updateSelectedPosition();
            case UNSELECTED:
                // We just need to update the style for the unselected tab...
            case THUMBNAIL:
            case TITLE:
            case RECORDING_CHANGE:
                View view = getChildAt(mTabsAdapter.getPositionForTab(tab) - getFirstVisiblePosition());
                if (view == null)
                    return;

                TabsLayoutItemView item = (TabsLayoutItemView) view.getTag();
                item.assignValues(tab);
                break;
        }
    }

    // Updates the selected position in the list so that it will be scrolled to the right place.
    private void updateSelectedPosition() {
        int selected = mTabsAdapter.getPositionForTab(Tabs.getInstance().getSelectedTab());
        updateSelectedStyle(selected);

        if (selected != -1) {
            setSelection(selected);
        }
    }

    /**
     * Updates the selected/unselected style for the tabs.
     *
     * @param selected position of the selected tab
     */
    private void updateSelectedStyle(int selected) {
        for (int i = 0; i < mTabsAdapter.getCount(); i++) {
            setItemChecked(i, (i == selected));
        }
    }

    private void refreshTabsData() {
        // Store a different copy of the tabs, so that we don't have to worry about
        // accidentally updating it on the wrong thread.
        ArrayList<Tab> tabData = new ArrayList<Tab>();

        Iterable<Tab> allTabs = Tabs.getInstance().getTabsInOrder();
        for (Tab tab : allTabs) {
            if (tab.isPrivate() == mIsPrivate)
                tabData.add(tab);
        }

        mTabsAdapter.setTabs(tabData);
        updateSelectedPosition();
    }

    public void resetTransforms(View view) {
        ViewHelper.setAlpha(view, 1);

        if (isVertical()) {
            ViewHelper.setTranslationX(view, 0);
        } else {
            ViewHelper.setTranslationY(view, 0);
        }

        // We only need to reset the height or width after individual tab close animations.
        if (mOriginalSize != 0) {
            ViewHelper.setHeight(view, mOriginalSize);
        }
    }

    private boolean isVertical() {
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

            animator.attach(view, Property.TRANSLATION_X, view.getWidth());

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
                    TabsGridLayout.this.setEnabled(true);

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

        animator.attach(view, Property.TRANSLATION_X, pos);

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

        TabsLayoutItemView tab = (TabsLayoutItemView)view.getTag();
        final int tabId = tab.id;

        // Caching this assumes that all rows are the same height
        if (mOriginalSize == 0) {
            mOriginalSize = view.getHeight();
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
                TabsLayoutItemView tab = (TabsLayoutItemView) view.getTag();
                tab.close.setVisibility(View.VISIBLE);
            }
        });

        animator.start();
    }

}

/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.tabs;

import java.util.ArrayList;
import java.util.List;

import org.mozilla.gecko.animation.ViewHelper;
import org.mozilla.gecko.GeckoAppShell;
import org.mozilla.gecko.GeckoEvent;
import org.mozilla.gecko.R;
import org.mozilla.gecko.Tab;
import org.mozilla.gecko.tabs.TabsPanel.TabsLayout;
import org.mozilla.gecko.Tabs;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.GridView;
import android.view.ViewGroup;
import android.widget.Button;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;

/**
 * A tabs layout implementation for the tablet redesign (bug 1014156).
 * Expected to replace TabsListLayout once complete.
 */

class TabsGridLayout extends GridView
                     implements TabsLayout,
                                Tabs.OnTabsChangedListener {
    private static final String LOGTAG = "Gecko" + TabsGridLayout.class.getSimpleName();
    private static final int ANIM_TIME_MS = 200;
    private static final DecelerateInterpolator ANIM_INTERPOLATOR =
            new DecelerateInterpolator();
    private final Context mContext;
    private TabsPanel mTabsPanel;

    final private boolean mIsPrivate;

    private final TabsLayoutAdapter mTabsAdapter;

    public TabsGridLayout(Context context, AttributeSet attrs) {
        super(context, attrs, R.attr.tabGridLayoutViewStyle);
        mContext = context;

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TabsLayout);
        mIsPrivate = (a.getInt(R.styleable.TabsLayout_tabs, 0x0) == 1);
        a.recycle();

        mTabsAdapter = new TabsGridLayoutAdapter(mContext);
        setAdapter(mTabsAdapter);

        setRecyclerListener(new RecyclerListener() {
            @Override
            public void onMovedToScrapHeap(View view) {
                TabsLayoutItemView item = (TabsLayoutItemView) view;
                item.setThumbnail(null);
            }
        });

        setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        setStretchMode(GridView.STRETCH_SPACING);
        setGravity(Gravity.CENTER);
        setNumColumns(GridView.AUTO_FIT);

        final Resources resources = getResources();
        final int columnWidth = resources.getDimensionPixelSize(R.dimen.new_tablet_tab_panel_column_width);
        setColumnWidth(columnWidth);

        final int padding = resources.getDimensionPixelSize(R.dimen.new_tablet_tab_panel_grid_padding);
        final int paddingTop = resources.getDimensionPixelSize(R.dimen.new_tablet_tab_panel_grid_padding_top);
        setPadding(padding, paddingTop, padding, padding);
    }

    private class TabsGridLayoutAdapter extends TabsLayoutAdapter {

        final private Button.OnClickListener mCloseClickListener;
        final private View.OnClickListener mSelectClickListener;

        public TabsGridLayoutAdapter (Context context) {
            super(context, R.layout.new_tablet_tabs_item_cell);

            mCloseClickListener = new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TabsLayoutItemView itemView = (TabsLayoutItemView) v.getTag();
                    Tab tab = Tabs.getInstance().getTab(itemView.getTabId());
                    closeTab(tab);
                }
            };

            mSelectClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TabsLayoutItemView tab = (TabsLayoutItemView) v;
                    Tabs.getInstance().selectTab(tab.getTabId());
                    autoHidePanel();
                }
            };
        }

        @Override
        TabsLayoutItemView newView(int position, ViewGroup parent) {
            final TabsLayoutItemView item = super.newView(position, parent);
            item.setOnClickListener(mSelectClickListener);
            item.setCloseOnClickListener(mCloseClickListener);
            return item;
        }

        @Override
        public void bindView(TabsLayoutItemView view, Tab tab) {
            super.bindView(view, tab);

            // If we're recycling this view, there's a chance it was transformed during
            // the close animation. Remove any of those properties.
            resetTransforms(view);
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
        return true;
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

                ((TabsLayoutItemView) view).assignValues(tab);
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
        ArrayList<Tab> tabData = new ArrayList<>();

        Iterable<Tab> allTabs = Tabs.getInstance().getTabsInOrder();
        for (Tab tab : allTabs) {
            if (tab.isPrivate() == mIsPrivate)
                tabData.add(tab);
        }

        mTabsAdapter.setTabs(tabData);
        updateSelectedPosition();
    }

    private void resetTransforms(View view) {
        ViewHelper.setAlpha(view, 1);
        ViewHelper.setTranslationX(view, 0);
    }

    @Override
    public void closeAll() {

        autoHidePanel();

        if (getChildCount() == 0) {
            return;
        }

        final Iterable<Tab> tabs = Tabs.getInstance().getTabsInOrder();
        for (Tab tab : tabs) {
            // In the normal panel we want to close all tabs (both private and normal),
            // but in the private panel we only want to close private tabs.
            if (!mIsPrivate || tab.isPrivate()) {
                Tabs.getInstance().closeTab(tab, false);
            }
        }
    }

    void closeTab(Tab tab) {
        animateRemoveTab(tab);
        Tabs.getInstance().closeTab(tab);

        updateSelectedPosition();
    }

    private View getViewForTab(Tab tab) {
        final int position = mTabsAdapter.getPositionForTab(tab);
        return getChildAt(position - getFirstVisiblePosition());
    }

    private void animateRemoveTab(Tab removedTab) {
        final int removedPosition = mTabsAdapter.getPositionForTab(removedTab);

        final View removedView = getViewForTab(removedTab);

        // The removed position might not have a matching child view
        // when it's not within the visible range of positions in the strip.
        if (removedView == null) {
            return;
        }

        // We don't animate the removed child view (it just disappears)
        // but we still need its size of animate all affected children
        // within the visible viewport.
        final int removedWidth = removedView.getWidth();
        final int removedHeight = removedView.getHeight();

        final int numberOfColumns = getNumColumnsCompat();

        getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                getViewTreeObserver().removeOnPreDrawListener(this);

                final int firstPosition = getFirstVisiblePosition();
                final List<Animator> childAnimators = new ArrayList<>();
                final int delayMultiple = 25; //in ms

                final int childCount = getChildCount();
                for (int x = 0, i = removedPosition - firstPosition; i < childCount; i++, x++) {
                    final View child = getChildAt(i);
                    ObjectAnimator animator;
                    if (i % numberOfColumns == numberOfColumns - 1) {
                        // animate height as well as width
                        animator = ObjectAnimator.ofFloat(child, "translationY", removedHeight, 0);
                        animator.setStartDelay(x * delayMultiple);
                        childAnimators.add(animator);
                        animator = ObjectAnimator.ofFloat(child, "translationX", -(removedWidth * numberOfColumns), 0);
                        animator.setStartDelay(x * delayMultiple);
                        childAnimators.add(animator);

                    } else {
                        // just animate width
                        // TODO: optimize with Valueresolver
                        animator =
                                ObjectAnimator.ofFloat(child, "translationX", removedWidth, 0);
                        animator.setStartDelay(x * delayMultiple);
                        childAnimators.add(animator);
                    }

                }

                final AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.playTogether(childAnimators);
                animatorSet.setDuration(ANIM_TIME_MS);
                animatorSet.setInterpolator(ANIM_INTERPOLATOR);
                animatorSet.start();

                return true;
            }
        });
    }
    private int getNumColumnsCompat() {
        if (Build.VERSION.SDK_INT >= 11) {
            return getNumColumnsCompat11();

        } else {
            int columns = 0;
            int children = getChildCount();
            if (children > 0) {
                int width = getChildAt(0).getMeasuredWidth();
                if (width > 0) {
                    columns = getWidth() / width;
                }
            }
            return columns > 0 ? columns : AUTO_FIT;
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private int getNumColumnsCompat11() {
        return getNumColumns();
    }
}

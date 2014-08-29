/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mozilla.gecko.tabs;

import org.mozilla.gecko.NewTabletUI;
import org.mozilla.gecko.R;
import org.mozilla.gecko.tabs.TabsTray;
import org.mozilla.gecko.tabs.TabsTray.TabsTrayType;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import java.lang.ref.WeakReference;
import android.util.Log;
import java.lang.IllegalStateException;

// Stripped down version of android.view.ViewStub used to facilitate inflation
// of the TabsTray implementations
public class TabLayoutViewStub extends View {
    private static final String LOGTAG = "TabLayoutViewStub";

    private int mLayoutResource = 0;
    private WeakReference<View> mInflatedViewRef;
    private Context mContext;
    private boolean mIsPrivate = false;


    public TabLayoutViewStub(Context context) {
        super(context);
        initialize(context);
    }
    /**
     * Creates a new TabLayoutViewStub with the specified layout resource.
     *
     * @param context The application's environment.
     * @param layoutResource The reference to a layout resource that will be inflated.
     */
    public TabLayoutViewStub(Context context, int layoutResource) {
        super(context);
        mLayoutResource = layoutResource;
        initialize(context);
    }
    public TabLayoutViewStub(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    @SuppressWarnings({"UnusedDeclaration"})
    public TabLayoutViewStub(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize(context);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TabsLayoutViewStub,
                defStyle, 0);

        mIsPrivate = (a.getInt(R.styleable.TabsLayoutViewStub_tabs, 0x0) == 1);
        a.recycle();
    }
    private void initialize(Context context) {
        mContext = context;
        setVisibility(GONE);
        setWillNotDraw(true);

    }
    /**
     * Specifies the layout resource to inflate when this StubbedView becomes visible or invisible
     * or when {@link #inflate()} is invoked. The View created by inflating the layout resource is
     * used to replace this StubbedView in its parent.
     * 
     * @param layoutResource A valid layout resource identifier (different from 0.)
     * 
     * @see #getLayoutResource()
     * @see #setVisibility(int)
     * @see #inflate()
     * @attr ref android.R.styleable#ViewStub_layout
     */
    private void setLayoutResource(int layoutResource) {
        mLayoutResource = layoutResource;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(0, 0);
    }

    /**
     * Inflates the layout resource identified by {@link #getLayoutResource()}
     * and replaces this StubbedView in its parent by the inflated layout resource.
     *
     * @return The inflated layout resource.
     *
     */
    public View inflate() {
        // Work out which view we want to inflate
        TabsTrayType trayType = NewTabletUI.isEnabled(mContext) ?
                                TabsTrayType.GRID : TabsTrayType.LIST;

        setLayoutResource(trayType.getLayoutId());

        final ViewParent viewParent = getParent();
        if (viewParent != null && viewParent instanceof ViewGroup) {
            if (mLayoutResource != 0) {
                final ViewGroup parent = (ViewGroup) viewParent;
                final LayoutInflater factory = LayoutInflater.from(mContext);
                final View view = factory.inflate(mLayoutResource, parent, false);
                if (!(view instanceof TabsTray)) {
                    throw new IllegalArgumentException("TabLayoutViewStub must have an " +
                        "inflated view which has a super class of type TabsTray");
                }
                final int index = parent.indexOfChild(this);
                parent.removeViewInLayout(this);
                final ViewGroup.LayoutParams layoutParams = getLayoutParams();
                if (layoutParams != null) {
                    parent.addView(view, index, layoutParams);
                } else {
                    parent.addView(view, index);
                }
                mInflatedViewRef = new WeakReference<View>(view);

                ((TabsTray) view).setIsPrivate(mIsPrivate);

                return view;
            } else {
                throw new IllegalArgumentException("TabLayoutViewStub must have a valid layoutResource");
            }
        } else {
            throw new IllegalStateException("TabLayoutViewStub must have a non-null ViewGroup viewParent");
        }
    }
}
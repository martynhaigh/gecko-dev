/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.tabs;

import org.mozilla.gecko.R;
import org.mozilla.gecko.Tab;
import org.mozilla.gecko.widget.TabThumbnailWrapper;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Checkable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TabsLayoutItemView extends LinearLayout
                                implements Checkable {
    private static final String LOGTAG = "Gecko" + TabsLayoutItemView.class.getSimpleName();
    private static final int[] STATE_CHECKED = { android.R.attr.state_checked };
    private boolean mChecked;

    private int mId;
    private TextView mTitle;
    private ImageView mThumbnail;
    private ImageButton mClose;
    private TabThumbnailWrapper mThumbnailWrapper;

    public TabsLayoutItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);

        if (mChecked)
            mergeDrawableStates(drawableState, STATE_CHECKED);

        return drawableState;
    }

    @Override
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void setChecked(boolean checked) {
        if (mChecked != checked) {
            mChecked = checked;
            refreshDrawableState();

            int count = getChildCount();
            for (int i=0; i < count; i++) {
                final View child = getChildAt(i);
                if (child instanceof Checkable)
                    ((Checkable) child).setChecked(checked);
            }
        }
    }

    @Override
    public void toggle() {
        mChecked = !mChecked;
    }

    private void populateChildReferences() {
        mTitle = (TextView) findViewById(R.id.title);
        mThumbnail = (ImageView) findViewById(R.id.thumbnail);
        mClose = (ImageButton) findViewById(R.id.close);
        mThumbnailWrapper = (TabThumbnailWrapper) findViewById(R.id.wrapper);
    }

    public void setCloseOnClickListener(OnClickListener mOnClickListener) {
        getCloseButton().setOnClickListener(mOnClickListener);
    }

    protected void assignValues(Tab tab)  {
        if (tab == null) {
            return;
        }

        mId = tab.getId();

        Drawable thumbnailImage = tab.getThumbnail();
        if (thumbnailImage != null) {
            getThumbnail().setImageDrawable(thumbnailImage);
        } else {
            getThumbnail().setImageResource(R.drawable.tab_thumbnail_default);
        }
        if (getThumbnailWrapper() != null) {
            getThumbnailWrapper().setRecording(tab.isRecording());
        }
        getTitle().setText(tab.getDisplayTitle());
        getCloseButton().setTag(this);
    }

    public int getTabId() {
        return mId;
    }

    public TextView getTitle() {
        if (mTitle == null) {
            populateChildReferences();
        }
        return mTitle;
    }

    public ImageView getThumbnail() {
        if (mThumbnail == null) {
            populateChildReferences();
        }
        return mThumbnail;
    }

    public ImageButton getCloseButton() {
        if (mClose == null) {
            populateChildReferences();
        }
        return mClose;
    }

    public TabThumbnailWrapper getThumbnailWrapper() {
        if (mThumbnailWrapper == null) {
            populateChildReferences();
        }
        return mThumbnailWrapper;
    }
}

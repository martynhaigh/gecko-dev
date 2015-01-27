/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.tests.components;

import static org.mozilla.gecko.tests.helpers.AssertionHelper.*;

import org.mozilla.gecko.R;
import org.mozilla.gecko.tabs.TabStripView;
import org.mozilla.gecko.tests.UITestContext;

import android.view.View;

/**
 * A class representing any interactions that take place in the TabStrip.
 */
public class TabStripComponent extends BaseComponent {
    private static final String LOGTAG = AboutHomeComponent.class.getSimpleName();

    public TabStripComponent(UITestContext testContext) {
        super(testContext);
    }

    private TabStripView getTabStrip() {
        return (TabStripView) mSolo.getView(R.id.tab_strip);
    }
    private View getAddButton() {
        return mSolo.getView(R.id.add_tab);
    }


    public TabStripComponent assertNotVisible() {
        fAssertTrue("The TabStrip is not visible",
                getTabStrip().getVisibility() != View.VISIBLE);
        return this;
    }

    public TabStripComponent assertVisible() {
        fAssertTrue("The TabStrip is visible",
                getTabStrip().getVisibility() == View.VISIBLE);
        return this;
    }

    public TabStripComponent assertNumberOfTabs(int numberOfTabs) {
        fAssertEquals("The correct number of tabs are shown",
                numberOfTabs, getTabStrip().getAdapter().getCount());

        return this;
    }

    public TabStripComponent clickAddTab() {
        assertVisible();

        mTestContext.dumpLog(LOGTAG, "Clicking add tab button.");
        mSolo.clickOnView(getAddButton());
        return this;
    }

//    public TabStripComponent swipeToRightOnTabStrip() {
//        mTestContext.dumpLog(LOGTAG, "Swiping right on the TabStrip.");
//        swipeToPanel(Solo.RIGHT);
//        return this;
//    }
//
//    public TabStripComponent swipeToLeftOnTabStrip() {
//        mTestContext.dumpLog(LOGTAG, "Swiping left on the TabStrip.");
//        swipeToPanel(Solo.LEFT);
//        return this;
//    }

}

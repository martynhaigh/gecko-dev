/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.tests;

import org.mozilla.gecko.tests.helpers.GeckoHelper;

public class testTabStrip extends UITest {



    public void testTabStrip() {
        GeckoHelper.blockForReady();

        mTabStrip.assertNumberOfTabs(1);

        mTabStrip.clickAddTab();

        mTabStrip.assertNumberOfTabs(2);


        mTabStrip.closeTab(1);

        mTabStrip.assertNumberOfTabs(1);
        mTabStrip.clickAddTab();
        mTabStrip.clickAddTab();


        mTabStrip.assertNumberOfTabs(3);

        mTabStrip.addPrivateTabThroughMenu(mAppMenu);

        mTabStrip.assertNumberOfTabs(1);

        mTabStrip.finish();
    }


}
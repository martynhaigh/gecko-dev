/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.tests.components;

import static org.mozilla.gecko.tests.helpers.AssertionHelper.*;

import org.mozilla.gecko.R;
import org.mozilla.gecko.Tab;
import org.mozilla.gecko.Tabs;
import org.mozilla.gecko.tabs.TabStripView;
import org.mozilla.gecko.tests.UITestContext;
import org.mozilla.gecko.tests.helpers.WaitHelper;

import android.view.View;
import com.jayway.android.robotium.solo.Condition;

/**
 * A class representing any interactions that take place in the TabStrip.
 */
public class TabStripComponent extends BaseComponent {
    private static final String LOGTAG = AboutHomeComponent.class.getSimpleName();

    private TabsListener tabsListener;

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
        fAssertTrue("The TabStrip is visible",
                getTabStrip().getVisibility() != View.VISIBLE);
        return this;
    }

    public TabStripComponent assertVisible() {
        fAssertTrue("The TabStrip is not visible",
                getTabStrip().getVisibility() == View.VISIBLE);
        return this;
    }

    public TabStripComponent assertNumberOfTabs(int numberOfTabs) {
        fAssertEquals("The correct number of tabs are not shown",
                numberOfTabs, getTabStrip().getAdapter().getCount());

        return this;
    }

    public TabStripComponent clickAddTab() {

        getTabsListener().setTrigger(new Trigger() {
            @Override
            public boolean process(Tab tab, Tabs.TabEvents msg) {
                return msg == Tabs.TabEvents.ADDED;
            }

            @Override
            public String getName() {
                return "Add Tab";
            }
        });

        assertVisible();

        mTestContext.dumpLog(LOGTAG, "Clicking add tab button.");
        mSolo.clickOnView(getAddButton());

        getTabsListener().blockUntilTriggered();
        return this;
    }

    public TabStripComponent closeTab(int index) {
        assertVisible();
        fAssertTrue("The number of tabs is greater than the index of the tab being closed",
                getTabStrip().getAdapter().getCount() > index);

        Tab tab = (Tab) getTabStrip().getAdapter().getItem(index);

        getTabsListener().setTrigger(new Trigger() {
            @Override
            public boolean process(Tab tab, Tabs.TabEvents msg) {
                return msg == Tabs.TabEvents.CLOSED;
            }

            @Override
            public String getName() {
                return "Close Tab";
            }
        });

        Tabs.getInstance().closeTab(tab);

        getTabsListener().blockUntilTriggered();
        return this;
    }


    public TabStripComponent addPrivateTabThroughMenu(AppMenuComponent appMenu) {
        assertVisible();

        getTabsListener().setTrigger(new Trigger() {
            @Override
            public boolean process(Tab tab, Tabs.TabEvents msg) {
                return msg == Tabs.TabEvents.SELECTED && tab.isPrivate();
            }

            @Override
            public String getName() {
                return "Add private tab through menu";
            }
        });

        appMenu.pressMenuItem(AppMenuComponent.MenuItem.NEW_PRIVATE_TAB);

        getTabsListener().blockUntilTriggered();
        return this;
    }

    public void finish() {
        if(tabsListener != null) {
            Tabs.unregisterOnTabsChangedListener(tabsListener);
        }
    }

    public abstract class Trigger {
        private boolean triggered = false;

        public boolean getTriggered() {
            return triggered;
        }

        public void setTriggered() {
            triggered = true;
        }

        public abstract boolean process(Tab tab, Tabs.TabEvents msg);

        public abstract String getName();

    }

    TabsListener getTabsListener() {
        if(tabsListener == null) {
            tabsListener = new TabsListener();
            Tabs.registerOnTabsChangedListener(tabsListener);
        }
        return tabsListener;
    }

    private class TabsListener implements Tabs.OnTabsChangedListener {

        private Trigger trigger;

        public void setTrigger(Trigger trigger) {
            this.trigger = trigger;
        }

        public void blockUntilTriggered() {
            WaitHelper.waitFor(trigger.getName(), new Condition() {
                @Override
                public boolean isSatisfied() {
                    return trigger.getTriggered();
                }
            });
        }

        @Override
        public void onTabChanged(Tab tab, Tabs.TabEvents msg, Object data) {
            if (trigger.process(tab, msg)) {
                trigger.setTriggered();
            }
        }
    }

}

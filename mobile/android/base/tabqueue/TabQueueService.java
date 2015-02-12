/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.tabqueue;

import org.mozilla.gecko.BrowserApp;
import org.mozilla.gecko.R;
import org.mozilla.gecko.mozglue.ContextUtils;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

// On launch of an external url this service displays a view overtop of the currently running activity with an action
// to open the url in fennec immediately.  If the user takes no action or the service receives another intent, the
// url is added to a file which is then read in fennec on next launch.
public class TabQueueService extends Service {

    private WindowManager windowManager;
    private View layout;
    private TextView mMessageView;
    private Button openNowButton;
    private final Handler mHideHandler = new Handler();
    private WindowManager.LayoutParams mParams;
    private HideRunnable mHideRunnable;


    @Override
    public IBinder onBind(Intent intent) {
        // Not used
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        layout = layoutInflater.inflate(R.layout.button_toast, null);

        mMessageView = (TextView) layout.findViewById(R.id.toast_message);
        mMessageView.setText(getResources().getText(R.string.tab_queue_toast_message));

        openNowButton = (Button) layout.findViewById(R.id.toast_button);
        openNowButton.setEnabled(true);
        openNowButton.setText(getResources().getText(R.string.tab_queue_toast_action));

        mParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        mParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        mParams.x = 0;
        mParams.y = 0;
    }

    private abstract class HideRunnable implements Runnable {
        // If true then remove the toast from the view hierarchy when run.
        private boolean mShouldHide = true;

        public void shouldHide(boolean hide) {
            mShouldHide = hide;
        }

        public void run() {
            if (mShouldHide) {
                hide();
            }
            execute();
        }

        public abstract void execute();
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {

        if (mHideRunnable != null) {
            // If there's already a runnable then run it but keep the view attached to the window.
            mHideHandler.removeCallbacks(mHideRunnable);
            mHideRunnable.shouldHide(false);
            mHideRunnable.run();
        } else {
            windowManager.addView(layout, mParams);
        }

        mHideRunnable = new HideRunnable() {
            @Override
            public void execute() {
                addUrlToList(intent);
                mHideRunnable = null;
            }
        };

        openNowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mHideHandler.removeCallbacks(mHideRunnable);
                mHideRunnable = null;

                Intent forwardIntent = new Intent(intent);
                forwardIntent.setClass(getApplicationContext(), BrowserApp.class);
                forwardIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(forwardIntent);
                hide();
            }
        });

        mHideHandler.postDelayed(mHideRunnable, TabQueueHelper.TOAST_TIMEOUT);

        return super.onStartCommand(intent, flags, startId);
    }

    private void hide() {
        windowManager.removeView(layout);
    }

    private void addUrlToList(Intent intentParam) {
        if (intentParam == null) {
            return;
        }
        final ContextUtils.SafeIntent intent = new ContextUtils.SafeIntent(intentParam);
        final String args = intent.getStringExtra("args");
        final String intentData = intent.getDataString();

        // TODO Add url to list here.

    }
}
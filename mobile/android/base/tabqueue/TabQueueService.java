/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.tabqueue;

import org.mozilla.gecko.BrowserApp;
import org.mozilla.gecko.GeckoProfile;
import org.mozilla.gecko.R;
import org.mozilla.gecko.mozglue.ContextUtils;

import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * On launch this service displays a view over the currently running activity with an action
 * to open the url in fennec immediately.  If the user takes no action or the service receives another intent, the
 * url is added to a file which is then read in fennec on next launch.  The view is inserted from this service, in
 * conjunction with the SYSTEM_ALERT_WINDOW permission, to display the view on top of the application in the background
 * whilst still allowing the user to interact with the background application.
 */
public class TabQueueService extends Service {
    private static final String LOGTAG = "Gecko" + TabQueueService.class.getSimpleName();

    private WindowManager windowManager;
    private View layout;
    private Button openNowButton;
    private final Handler handler = new Handler();
    private WindowManager.LayoutParams layoutParams;
    private HideRunnable hideRunnable;
    private ExecutorService executorService;

    @Override
    public IBinder onBind(Intent intent) {
        // Not used
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        executorService = Executors.newSingleThreadExecutor();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        layout = layoutInflater.inflate(R.layout.button_toast, null);

        final Resources resources = getResources();

        TextView messageView = (TextView) layout.findViewById(R.id.toast_message);
        messageView.setText(resources.getText(R.string.tab_queue_toast_message));

        openNowButton = (Button) layout.findViewById(R.id.toast_button);
        openNowButton.setEnabled(true);
        openNowButton.setText(resources.getText(R.string.tab_queue_toast_action));

        layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        layoutParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        layoutParams.x = 0;
        layoutParams.y = 0;
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

        if (hideRunnable != null) {
            // If there's already a runnable then run it but keep the view attached to the window.
            handler.removeCallbacks(hideRunnable);
            hideRunnable.shouldHide(false);
            hideRunnable.run();
        } else {
            windowManager.addView(layout, layoutParams);
        }

        hideRunnable = new HideRunnable() {
            @Override
            public void execute() {
                addUrlToList(intent);
                hideRunnable = null;
            }
        };

        openNowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handler.removeCallbacks(hideRunnable);
                hideRunnable = null;

                Intent forwardIntent = new Intent(intent);
                forwardIntent.setClass(getApplicationContext(), BrowserApp.class);
                forwardIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(forwardIntent);
                hide();
            }
        });

        handler.postDelayed(hideRunnable, TabQueueHelper.TOAST_TIMEOUT);

        return super.onStartCommand(intent, flags, startId);
    }

    private void hide() {
        windowManager.removeView(layout);
    }

    private void addUrlToList(final Intent intentParam) {
        if (intentParam == null) {
            // This should never happen, but let's return silently instead of crashing if it does.
            return;
        }
        final ContextUtils.SafeIntent intent = new ContextUtils.SafeIntent(intentParam);
        final String args = intent.getStringExtra("args");
        final String intentData = intent.getDataString();

        // As we're doing disk IO, let's run this stuff in a separate thread.
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                TabQueueHelper.queueUrl(GeckoProfile.get(getApplicationContext()), intentData);
            }
        });
    }
}
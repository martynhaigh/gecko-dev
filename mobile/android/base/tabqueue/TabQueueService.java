/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.tabqueue;

import org.mozilla.gecko.BrowserApp;
import org.mozilla.gecko.GeckoProfile;
import org.mozilla.gecko.R;
import org.mozilla.gecko.mozglue.ContextUtils;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * On launch this service displays a view over the currently running activity with an action
 * to open the url in fennec immediately.  If the user takes no action or the service receives another intent, the
 * url is added to a file which is then read in fennec on next launch, in order to allow the user to quickly queue
 * urls to open without having to open Fennec each time.  A View is inserted from this service, in
 * conjunction with the SYSTEM_ALERT_WINDOW permission, to display the view on top of the application in the background
 * whilst still allowing the user to interact with the background application.
 *
 * General approach taken is similar to the FB chat heads functionality:
 *   http://stackoverflow.com/questions/15975988/what-apis-in-android-is-facebook-using-to-create-chat-heads
 */
public class TabQueueService extends IntentService {
    private static final String LOGTAG = "Gecko" + TabQueueService.class.getSimpleName();

    public static final long TOAST_TIMEOUT = 3000;

    private WindowManager windowManager;
    private View toastLayout;
    private Button openNowButton;
    private Handler tabQueueHandler;
    private GeckoProfile mProfile;
    private WindowManager.LayoutParams toastLayoutParams;
    private ExecutorService executorService;
    private StopServiceRunnable stopServiceRunnable;

    public TabQueueService(final String name) {
        super(name);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Not used
        return null;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        executorService = Executors.newSingleThreadExecutor();

        HandlerThread thread = new HandlerThread("TabQueueHandlerThread");
        thread.start();
        tabQueueHandler = new Handler(thread.getLooper());

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        toastLayout = layoutInflater.inflate(R.layout.button_toast, null);

        final Resources resources = getResources();

        TextView messageView = (TextView) toastLayout.findViewById(R.id.toast_message);
        messageView.setText(resources.getText(R.string.tab_queue_toast_message));

        openNowButton = (Button) toastLayout.findViewById(R.id.toast_button);
        openNowButton.setText(resources.getText(R.string.tab_queue_toast_action));

        toastLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        toastLayoutParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
    }


    @Override
    protected void onHandleIntent(final Intent intent) {

        if (stopServiceRunnable != null) {
            // If we're already displaying a toast, keep displaying it but store the previous link's url.
            // The open button will refer to the most recently opened link.
            tabQueueHandler.removeCallbacks(stopServiceRunnable);
            stopServiceRunnable.setShouldNotStopService();
            stopServiceRunnable.run();
        } else {
            windowManager.addView(toastLayout, toastLayoutParams);
        }

        stopServiceRunnable = new StopServiceRunnable() {
            @Override
            public void onRun() {
                addUrlToTabQueue(intent);
                stopServiceRunnable = null;
            }
        };

        openNowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tabQueueHandler.removeCallbacks(stopServiceRunnable);
                stopServiceRunnable = null;


                Intent forwardIntent = new Intent(intent);
                forwardIntent.setClass(getApplicationContext(), BrowserApp.class);
                forwardIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(forwardIntent);

                destroy();
            }
        });

        tabQueueHandler.postDelayed(stopServiceRunnable, TOAST_TIMEOUT);
    }

    /**
     * A modified Runnable which additionally removes the view from the window view hierarchy and stops the service
     * when run, unless explicitly instructed not to.
     */
    private abstract class StopServiceRunnable implements Runnable {

        private boolean shouldStopService = true;

        public void setShouldNotStopService() {
            this.shouldStopService = false;
        }

        public void run() {
            onRun();

            if (shouldStopService) {
                destroy();
            }
        }

        public abstract void onRun();
    }

    /**
     * Removes the View from the view hierarchy and stops the service.
     */
    private void destroy() {
        windowManager.removeView(toastLayout);
        //stopSelf();
    }

    private void addUrlToTabQueue(Intent intentParam) {
        if (intentParam == null) {
            // This should never happen, but let's return silently instead of crash if it does.
            Log.w(LOGTAG, "Error adding URL to tab queue - invalid intent passed in.");
            return;
        }
        final ContextUtils.SafeIntent intent = new ContextUtils.SafeIntent(intentParam);
        final String args = intent.getStringExtra("args");
        final String intentData = intent.getDataString();

        // As we're doing disk IO, let's run this stuff in a separate thread.
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                int tabsQueued = TabQueueHelper.queueUrl(GeckoProfile.get(getApplicationContext()), intentData);
                TabQueueHelper.showNotification(getApplicationContext(), tabsQueued);
            }
        });
    }
}
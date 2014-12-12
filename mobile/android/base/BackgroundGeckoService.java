package org.mozilla.gecko;

import android.app.Activity;
import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.RectF;
import android.hardware.SensorEventListener;
import android.location.LocationListener;
import android.os.Binder;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.AbsoluteLayout;
import org.mozilla.gecko.db.BrowserDB;
import org.mozilla.gecko.mozglue.ContextUtils;
import org.mozilla.gecko.prompts.PromptService;

public class BackgroundGeckoService extends Service implements ContextGetter, GeckoAppShell.GeckoInterface {

    private static final String LOGTAG = "MTEST - GeckoBGService";

    /**
     * A constructor is required, and must call the super IntentService(String)
     * constructor with a name for the worker thread.
     */
    public BackgroundGeckoService() {
        super();
        Log.i(LOGTAG, "Start service");
    }

    protected String getURIFromIntent(Intent intent) {
        return intent.getDataString();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(LOGTAG, "Received start id " + startId + ": " + intent);

        final String uri = getURIFromIntent(intent);
        final boolean created = GeckoThread.isCreated();
        if (!created) {
            final String action = intent.getAction();
            final String args = intent.getStringExtra("args");
            GeckoAppShell.setContextGetter(this);
            GeckoAppShell.setGeckoInterface(this);
            BrowserDB.initialize(getProfile().getName());

            GeckoThread.setArgs(args);
            GeckoThread.setAction(action);
            GeckoThread.setUri(TextUtils.isEmpty(uri) ? null : uri);
            GeckoThread.createAndStart();
        }

        // GeckoAppShell.sendEventToGecko(GeckoEvent.createURILoadEvent(uri));
        Tabs.getInstance().attachToContext(this);
        Tabs.getInstance().loadUrl(uri, Tabs.LOADURL_NEW_TAB);
        Log.d(LOGTAG, "Tabs open = " + Tabs.getInstance().getDisplayCount());

        // TODO: We need to kill our self when Gecko is finished loading....
        return GeckoThread.isCreated() ? START_NOT_STICKY : START_STICKY;
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public SharedPreferences getSharedPreferences() {
        return GeckoSharedPrefs.forApp(this);
    }

    @Override
    public GeckoProfile getProfile() {
        return GeckoProfile.get(this);
    }

    @Override
    public PromptService getPromptService() {
        return new PromptService(this);
    }

    @Override
    public Activity getActivity() {
        return null;
    }

    @Override
    public String getDefaultUAString() {
        return "";
    }

    @Override
    public LocationListener getLocationListener() {
        return null;
    }

    @Override
    public SensorEventListener getSensorEventListener() {
        return null;
    }

    @Override
    public void doRestart() {

    }

    @Override
    public void setFullScreen(boolean fullscreen) {

    }

    @Override
    public void addPluginView(View view, RectF rect, boolean isFullScreen) {

    }

    @Override
    public void removePluginView(View view, boolean isFullScreen) {

    }

    @Override
    public void enableCameraView() {

    }

    @Override
    public void disableCameraView() {

    }

    @Override
    public void addAppStateListener(GeckoAppShell.AppStateListener listener) {

    }

    @Override
    public void removeAppStateListener(GeckoAppShell.AppStateListener listener) {

    }

    @Override
    public View getCameraView() {
        return new SurfaceView(this);
    }

    @Override
    public void notifyWakeLockChanged(String topic, String state) {

    }

    @Override
    public FormAssistPopup getFormAssistPopup() {
        return null;
    }

    @Override
    public boolean areTabsShown() {
        return false;
    }

    @Override
    public AbsoluteLayout getPluginContainer() {
        return null;
    }

    @Override
    public void notifyCheckUpdateResult(String result) {

    }

    @Override
    public boolean hasTabsSideBar() {
        return false;
    }

    @Override
    public void invalidateOptionsMenu() {

    }

    private class LocalBinder extends Binder {
        BackgroundGeckoService getService() {
            return BackgroundGeckoService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
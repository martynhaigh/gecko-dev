package org.mozilla.gecko;

import org.mozilla.gecko.mozglue.ContextUtils;
import org.mozilla.gecko.widget.ButtonToast;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by martyn on 03/12/14.
 */
public class LoadInBackground extends Activity {

    private GeckoProfile mProfile;
    private TextView mMessageView;
    private Button mButton;
    public static int LENGTH_SHORT = 3000;


    private final Handler mHideHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        Log.d("MTEST", "LIB Activity - intent " + intent);

        intent.setClass(LoadInBackground.this, BackgroundGeckoService.class);

        startService(intent);
        finish();

//        LayoutInflater inflater = getLayoutInflater();

//
//        setCon(R.layout.button_toast,
//                (ViewGroup) findViewById(R.id.toast));
//        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
//
//        mMessageView = (TextView) layout.findViewById(R.id.toast_message);
//        mButton = (Button) layout.findViewById(R.id.toast_button);
//        mButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent forwardIntent = new Intent(getIntent());
//                forwardIntent.setClass(getApplicationContext(), BrowserApp.class);
//                forwardIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                startActivity(forwardIntent);
//
//                hide();
//            }
//        });
//
//        mButton.setEnabled(true);
//        mMessageView.setText("Open in background");
//        mButton.setText("Open now");
//
//
//        final int length = Toast.LENGTH_LONG;
//        Toast toast = new Toast(getApplicationContext());
//        toast.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL, 0, 0);
//        toast.setDuration(length);
//        toast.setView(layout);
//
//
//        toast.show();


//        mButton.setCompoundDrawablesWithIntrinsicBounds(t.buttonDrawable, null, null, null);

//        mHideHandler.postDelayed(mHideRunnable, length);
//
//        WindowManager.LayoutParams lp=dialog.getWindow().getAttributes();
//        lp.x=100;
//        lp.y=100;
//        lp.width=100;
//        lp.height=200;
//        lp.gravity= Gravity.BOTTOM | Gravity.CENTER;
//        lp.dimAmount=0;
//        lp.flags= WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
//        //   dialog.getWindow().setAttributes(lp);
//        dialog.setCanceledOnTouchOutside(true);
//
//
//        mMessageView = (TextView) dialog.findViewById(R.id.toast_message);
//        mButton = (Button) dialog.findViewById(R.id.toast_button);
//        mButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent forwardIntent = new Intent(getIntent());
//                forwardIntent.setClass(getApplicationContext(), BrowserApp.class);
//                startActivity(forwardIntent);
//
//                dialog.hide();
//            }
//        });
//        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
//            @Override
//            public void onDismiss(DialogInterface dialog) {
//                addUrlToList();
//            }
//        });
//        dialog.show();

//        getButtonToast().show(false, "Load in background",  ButtonToast.LENGTH_SHORT ,"Open now", getResources().getDrawable(R.drawable.switch_button_icon), new ButtonToast.ToastListener() {
//            @Override
//            public void onButtonClicked() {
//                Intent forwardIntent = new Intent(getIntent());
//                forwardIntent.setClass(getApplicationContext(), BrowserApp.class);
//                startActivity(forwardIntent);
//
//                finish();
//            }
//
//            @Override
//            public void onToastHidden(ButtonToast.ReasonHidden reason) {
//                if (reason == ButtonToast.ReasonHidden.TIMEOUT) {
//                    addUrlToList();
//
//                }
//
//                finish();
//            }
//        });

    }

    private void hide() {
        finish();
    }

    private void addUrlToList() {
        final ContextUtils.SafeIntent intent = new ContextUtils.SafeIntent(getIntent());
        final String action = intent.getAction();
        final String args = intent.getStringExtra("args");
        String data = intent.getDataString();
        getProfile();

        Log.d("MTEST", "Gecko in background: " + GeckoApplication.get().isApplicationInBackground());


        if (mProfile == null) {
            String profileName = null;
            String profilePath = null;
            if (args != null) {
                if (args.contains("-P")) {
                    Pattern p = Pattern.compile("(?:-P\\s*)(\\w*)(\\s*)");
                    Matcher m = p.matcher(args);
                    if (m.find()) {
                        profileName = m.group(1);
                    }
                }

                if (args.contains("-profile")) {
                    Pattern p = Pattern.compile("(?:-profile\\s*)(\\S*)(\\s*)");
                    Matcher m = p.matcher(args);
                    if (m.find()) {
                        profilePath = m.group(1);
                    }
                    if (profileName == null) {
                        profileName = GeckoProfile.DEFAULT_PROFILE;
                    }
                    GeckoProfile.sIsUsingCustomProfile = true;
                }

                if (profileName != null || profilePath != null) {
                    mProfile = GeckoProfile.get(this, profileName, profilePath);
                }
            }
        }

        try {
            Log.d("MTEST", "Reading list contents: " + mProfile.readFile("temp_reading_list.json"));
        } catch (IOException e) {
            Log.d("MTEST", "Couldn't read : " + e.getMessage());

            e.printStackTrace();
        }
        mProfile.appendToFile("temp_reading_list.json", data);
    }

    public synchronized GeckoProfile getProfile() {
        // fall back to default profile if we didn't load a specific one
        if (mProfile == null) {
            mProfile = GeckoProfile.get(this);
        }
        return mProfile;
    }

    public ButtonToast getButtonToast() {
        ViewStub toastStub = (ViewStub) findViewById(R.id.toast_stub);
        return new ButtonToast(toastStub.inflate());
    }

    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            addUrlToList();
            hide();
        }
    };

}

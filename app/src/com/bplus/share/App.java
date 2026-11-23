package com.bplus.share;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.graphics.Typeface;
import android.os.Build;

public class App extends Application {
    public static final String CH = "bplus_share";
    public static Typeface cairo, cairoSB, cairoB;
    private static App instance;
    public static App ctx() { return instance; }

    @Override public void onCreate() {
        super.onCreate();
        instance = this;
        try { cairo = Typeface.createFromAsset(getAssets(), "fonts/cairo-400.ttf"); } catch (Exception e) { cairo = Typeface.DEFAULT; }
        try { cairoSB = Typeface.createFromAsset(getAssets(), "fonts/cairo-600.ttf"); } catch (Exception e) { cairoSB = cairo; }
        try { cairoB = Typeface.createFromAsset(getAssets(), "fonts/cairo-700.ttf"); } catch (Exception e) { cairoB = cairo; }
        Store.init(this);Attachments.snapshot();
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(new NotificationChannel(CH, "B+ Share", NotificationManager.IMPORTANCE_LOW));
            Alerts.setup(this);
        }
        TransferService.start(this);
    }
}

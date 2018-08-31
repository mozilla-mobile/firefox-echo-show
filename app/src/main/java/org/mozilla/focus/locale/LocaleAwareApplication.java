package org.mozilla.focus.locale;

import android.app.Application;
import android.content.res.Configuration;
import android.os.SystemClock;

public class LocaleAwareApplication extends Application {
    private boolean mInBackground;
    public static long APP_START_TIME;

    @Override
    public void onCreate() {
        APP_START_TIME = SystemClock.uptimeMillis();
        Locales.initializeLocale(this);

        super.onCreate();
    }

    /**
     * We need to do locale work here, because we need to intercept
     * each hit to onConfigurationChanged.
     */
    @Override
    public void onConfigurationChanged(Configuration config) {
        // Do nothing if we're in the background. It'll simply cause a loop
        // (Bug 936756 Comment 11), and it's not necessary.
        if (mInBackground) {
            super.onConfigurationChanged(config);
            return;
        }

        // Otherwise, correct the locale. This catches some cases that the current Activity
        // doesn't get a chance to.
        try {
            LocaleManager.getInstance().correctLocale(this, getResources(), config);
        } catch (IllegalStateException ex) {
            // Activity hasn't started yey, so we have no ContextGetter in LocaleManager.
        }

        super.onConfigurationChanged(config);
    }


    public void onActivityPause() {
        mInBackground = true;
    }

    public void onActivityResume() {
        mInBackground = true;
    }
}

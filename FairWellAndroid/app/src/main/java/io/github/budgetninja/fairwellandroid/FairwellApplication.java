package io.github.budgetninja.fairwellandroid;

import android.app.Application;

import com.facebook.FacebookSdk;
import com.parse.Parse;
import com.parse.ParseCrashReporting;
import com.parse.ParseFacebookUtils;
import com.parse.ParseTwitterUtils;

/**
 *Created by HuMengpei on 9/16/2015.
 */
public class FairwellApplication extends Application{

    public final static int APP_VERSION = 1;
    public static final int DISK_CACHE_COUNT = 1;
    public static final long DISK_CACHE_SIZE = 1024 * 1024 * 10; // 10MB
    public static final String TAG = "ImageCache";
    public static final int DISK_CACHE_INDEX = 0;
    public static final Object mDiskCacheLock = new Object();
    public static boolean mDiskCacheStarting = true;
    public static final String DISK_CACHE_SUBDIR = "images";

    @Override
    public void onCreate() {
        super.onCreate();
        Parse.enableLocalDatastore(this);
        ParseCrashReporting.enable(this);
        Parse.initialize(this, getString(R.string.parse_app_id), getString(R.string.parse_client_id));
        FacebookSdk.sdkInitialize(getApplicationContext());
        ParseFacebookUtils.initialize(getApplicationContext());
        ParseTwitterUtils.initialize(getString(R.string.twitter_consumer_key), getString(R.string.twitter_consumer_secret));
    }
}

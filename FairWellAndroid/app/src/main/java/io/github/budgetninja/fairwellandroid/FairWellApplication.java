package io.github.budgetninja.fairwellandroid;

import android.app.Application;
import android.content.Intent;

import com.facebook.FacebookSdk;
import com.parse.Parse;
import com.parse.ParseCrashReporting;
import com.parse.ParseFacebookUtils;
import com.parse.ParseTwitterUtils;
import com.parse.ParseUser;

/**
 * Created by HuMengpei on 9/16/2015.
 */
public class FairwellApplication extends Application{
    @Override
    public void onCreate() {
        super.onCreate();
        Parse.enableLocalDatastore(this);
        ParseCrashReporting.enable(this);
        Parse.initialize(this, getString(R.string.parse_app_id), getString(R.string.parse_client_id));
        FacebookSdk.sdkInitialize(getApplicationContext());
        ParseFacebookUtils.initialize(getApplicationContext());
        ParseTwitterUtils.initialize(getString(R.string.twitter_consumer_key), getString(R.string.twitter_consumer_secret));

        ParseUser currentUser = ParseUser.getCurrentUser();
        Intent intent = new Intent(this, MainActivity.class);
        if (currentUser != null) {
            intent = new Intent(this, LoggedInActivity.class);
        }
        //startActivity(intent);
    }
}

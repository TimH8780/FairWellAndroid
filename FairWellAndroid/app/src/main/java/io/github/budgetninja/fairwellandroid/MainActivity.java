package io.github.budgetninja.fairwellandroid;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.parse.ParseFacebookUtils;
import com.parse.ParseUser;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_container);

        Intent intent;
        if (ParseUser.getCurrentUser() != null) {                         //Already logged in
            intent = new Intent(MainActivity.this, ContentActivity.class);
        } else {                                                        //Need to log in
            intent = new Intent(MainActivity.this, LoginActivity.class);
        }
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ParseFacebookUtils.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.finish();
    }

}
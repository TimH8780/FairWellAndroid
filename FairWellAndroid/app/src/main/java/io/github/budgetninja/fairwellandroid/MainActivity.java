package io.github.budgetninja.fairwellandroid;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.parse.ParseFacebookUtils;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    /* TODO:
    Check if the user is first time fragment_login or not. My idea is to have a counter store
    locally on the phone (probably using SQLite) and increments it as soon as the
    user finishes the tutorial. Decrement if the user logout.

    Mengpei and Tim, if you guys have better solution. Please use whichever way you
    feel easier and more feasible.
     */

    private final static int SHOW_TUTORIAL = 0;     //don't know when to show tutorial

    private SamplesAdapter mAdapter;
    private int checkState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //show tutorial slide?

        ParseUser currentUser = ParseUser.getCurrentUser();

        if(currentUser != null){            //Already logged in (current user exists)
            goToLoggedInPage();
        } else {                            //Need to log in
            goToLoginPage();
            //goToRegisterPage(false);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ParseFacebookUtils.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause(){           //Kill this activity after login
        super.onPause();
        this.finish();
    }
    public void goToLoginPage(){
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
    }

    public void goToLoggedInPage(){
        Intent intent = new Intent(MainActivity.this, ContentActivity.class);
        startActivity(intent);
    }

    // Do not modify the code below, it is part of the side panel code.

    private static class SampleItem {

        String mTitle;
        String mSummary;
        Class mClazz;

        public SampleItem(String title, String summary, Class clazz) {
            mTitle = title;
            mSummary = summary;
            mClazz = clazz;
        }
    }

    public class SamplesAdapter extends BaseAdapter {

        private List<SampleItem> mSamples = new ArrayList<SampleItem>();

        public void addSample(String title, String summary, Class clazz) {
            mSamples.add(new SampleItem(title, summary, clazz));
        }

        @Override
        public int getCount() {
            return mSamples.size();
        }

        @Override
        public Object getItem(int position) {
            return mSamples.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            SampleItem sample = (SampleItem) getItem(position);

            View v = convertView;
            if (v == null) {
                v = getLayoutInflater().inflate(R.layout.list_row_sample, parent, false);
            }

            ((TextView) v.findViewById(R.id.title)).setText(sample.mTitle);
            ((TextView) v.findViewById(R.id.summary)).setText(sample.mSummary);

            return v;
        }
    }
}

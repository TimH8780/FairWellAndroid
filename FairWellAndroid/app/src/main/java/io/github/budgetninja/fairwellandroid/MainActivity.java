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

    /* TODO:
    Check if the user is first time fragment_login or not. My idea is to have a counter store
    locally on the phone (probably using SQLite) and increments it as soon as the
    user finishes the tutorial. Decrement if the user logout.

    Mengpei and Tim, if you guys have better solution. Please use whichever way you
    feel easier and more feasible.
     */


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //show tutorial slide?

        ParseUser currentUser = ParseUser.getCurrentUser();
        Intent intent;
        ConnectivityManager connMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        Utility.addReferenceConnectivityManager(connMgr);

        if(currentUser != null){            //Already logged in (current user exists)
            intent = new Intent(MainActivity.this, ContentActivity.class);
            if(networkInfo != null && networkInfo.isConnected()) {
                if(Utility.checkNewEntryField()){
                    Utility.setChangedRecord();
                    Utility.generateRawFriendList(ParseUser.getCurrentUser());
                }
            }
        } else {                            //Need to log in
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
    protected void onPause(){           //Kill this activity after login
        super.onPause();
        this.finish();
    }




    // Never used, comment out for now  --- Tim

/*
    // Do not modify the code below, it is part of the side panel code
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

        private List<SampleItem> mSamples = new ArrayList<>();

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
*/

}

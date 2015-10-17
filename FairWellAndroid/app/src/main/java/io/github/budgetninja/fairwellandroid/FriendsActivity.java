package io.github.budgetninja.fairwellandroid;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.List;

/**
 *Created by HuMengpei on 9/30/2015.
 */
public class FriendsActivity extends AppCompatActivity{

    private FriendAdaptor adapter;
    private ConnectivityManager connMgr;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend);
        connMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setElevation(0);
        }

        List<Utility.Friend> friendList;
        if(isNetworkConnected()) {
            friendList = Utility.generateFriendArray();
        }
        else {
            friendList = Utility.generateFriendArrayOffline();
            TextView subtitle = (TextView) findViewById(R.id.subtitle);
            subtitle.setText("OFFLINE MODE");
        }

        ListView view = (ListView) findViewById(R.id.friendlistview);
        adapter = new FriendAdaptor(this, R.layout.friend_item, friendList);
        view.setAdapter(adapter);

        LinearLayout layout = (LinearLayout)findViewById(R.id.EmptyListView);
        TextView text = (TextView)layout.findViewById(R.id.EmptyListViewText);
        text.setText("No Friend");
        view.setEmptyView(layout);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if(id == android.R.id.home){
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause(){
        super.onPause();
        this.finish();
    }

    private boolean isNetworkConnected(){
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }


    public class FriendAdaptor extends ArrayAdapter<Utility.Friend>{

        Context mContext;
        int mResource;
        List<Utility.Friend> mObject;
        private TextView[] textCollection;

        public FriendAdaptor(Context context, int resource, List<Utility.Friend> objects){
            super(context, resource, objects);
            mContext = context;
            mResource = resource;
            mObject = objects;
            textCollection = new TextView[objects.size() + 1];
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            Utility.Friend currentItem = mObject.get(position);
            if(convertView == null){
                convertView = getLayoutInflater().inflate(mResource, parent, false);
            }
            TextView name = (TextView) convertView.findViewById(R.id.friend_name);
            name.setText(currentItem.name);
            TextView email = (TextView) convertView.findViewById(R.id.friend_email);
            email.setText(currentItem.email);
            TextView status = (TextView) convertView.findViewById(R.id.confirmResult);
            Button confirm = (Button) convertView.findViewById(R.id.button_friend_confirm);
            if(currentItem.confirm){
                status.setText("Yes");
                confirm.setEnabled(false);
            }
            else{ status.setText("No"); }
            textCollection[position] = status;

            confirm.setTag(position);
            confirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!isNetworkConnected()) {
                        Toast.makeText(getApplicationContext(), "Check Internet Connection", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int position = (int) v.getTag();
                    Utility.Friend currentItem = mObject.get(position);
                    if (currentItem.isUserOne) {
                        Toast.makeText(getApplicationContext(), "Waiting for confirmation from <" + currentItem.name
                                + ">", Toast.LENGTH_SHORT).show();
                    } else {
                        textCollection[position].setText("Yes");
                        currentItem.setConfirm();
                        v.setEnabled(false);
                        Toast.makeText(getApplicationContext(), "Confirmed", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            Button delete = (Button) convertView.findViewById(R.id.button_friend_delete);
            delete.setTag(position);
            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!isNetworkConnected()) {
                        Toast.makeText(getApplicationContext(), "Check Internet Connection", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int position = (int) v.getTag();
                    final Utility.Friend currentItem = mObject.get(position);

                    final AlertDialog.Builder builder = new AlertDialog.Builder(FriendsActivity.this);
                    TextView message = new TextView(FriendsActivity.this);
                    message.setText("Are you sure you want to delete \n <" + currentItem.name + "> ?");
                    message.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
                    message.setPadding(20, 20, 20, 20);
                    builder.setTitle("Delete Friend");
                    builder.setView(message);
                    builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            currentItem.deleteFriend();
                            Utility.removeFromExistingFriendList(currentItem);
                            adapter.remove(currentItem);
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    final AlertDialog dialog = builder.create();
                    dialog.show();
                }
            });
            return convertView;
        }
    }
}

package io.github.budgetninja.fairwellandroid;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseObject;
import com.parse.ParseUser;

import java.io.UTFDataFormatException;
import java.util.List;

/**
 *Created by HuMengpei on 9/30/2015.
 */
public class FriendsActivity extends AppCompatActivity{

    private FriendAdaptor adapter;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend);

        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setElevation(0);
        }

        List<ParseObject> list = Utility.getListLocation().getList("list");

        List<Utility.Friend> friendList = Utility.convertToFriend(list);
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
                    int position = (int) v.getTag();
                    Utility.Friend currentItem = mObject.get(position);
                    if (textCollection[position].getText().equals("No")) {
                        if (currentItem.userOne) {
                            Toast.makeText(getApplicationContext(), "Waiting for confirmation from your friend",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            textCollection[position].setText("Yes");
                            currentItem.setConfirm();
                            v.setEnabled(false);
                            Toast.makeText(getApplicationContext(), "Confirmed", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
            Button delete = (Button) convertView.findViewById(R.id.button_friend_delete);
            delete.setTag(position);
            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //A pop-up window here

                    int position = (int)v.getTag();
                    Utility.Friend currentItem = mObject.get(position);
                    currentItem.deleteFriend();
                    Utility.removeFromExistingFriendList(currentItem);
                    adapter.remove(currentItem);
                }
            });
            return convertView;
        }
    }
}

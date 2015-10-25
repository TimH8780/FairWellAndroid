package io.github.budgetninja.fairwellandroid;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.Arrays;
import java.util.List;

/**
 *Created by HuMengpei on 9/30/2015.
 */
public class FriendsActivity extends AppCompatActivity{
    private ParseUser user;
    private FriendAdaptor adapter;
    private ConnectivityManager connMgr;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend);
        connMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        user = ParseUser.getCurrentUser();

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
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_friend, menu);

        // search box in friend activity's action bar
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        // Configure the search info and add any event listeners

        return super.onCreateOptionsMenu(menu);
    }

    //this section is to programmatically set the search icon to be user's icon instead
    //of the default ugly black search icon
    //Issac
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem searchViewMenuItem = menu.findItem(R.id.action_search);
        SearchView mSearchView = (SearchView) MenuItemCompat.getActionView(searchViewMenuItem);
        int searchImgId = android.support.v7.appcompat.R.id.search_button;
        ImageView v = (ImageView) mSearchView.findViewById(searchImgId);
        v.setImageResource(R.drawable.search_small);
        return super.onPrepareOptionsMenu(menu);
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_add_friend) {
            if(!isNetworkConnected()) {
                Toast.makeText(getApplicationContext(), "Check Internet Connection", Toast.LENGTH_SHORT).show();
            }
            else{
                displayAddFriendDialog();
            }
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

    private void displayAddFriendDialog(){         //show dialog and prompt user to enter email
        final AlertDialog.Builder builder = new AlertDialog.Builder(FriendsActivity.this);
        final LinearLayout layout = new LinearLayout(FriendsActivity.this);
        final TextView message = new TextView(FriendsActivity.this);
        final EditText userInput = new EditText(FriendsActivity.this);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams para = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        para.setMargins(20, 20, 20, 0);
        message.setText("Please enter the email address of your friend:");
        message.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        message.setLayoutParams(para);
        userInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        userInput.setLayoutParams(para);
        layout.addView(message);
        layout.addView(userInput);
        builder.setTitle("Add Friend");             //use e-mail for now, may need to change
        builder.setView(layout);

        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ParseQuery<ParseUser> query = ParseUser.getQuery();                 //find other user by searching email
                query.whereEqualTo("email", userInput.getText().toString());
                query.getFirstInBackground(new GetCallback<ParseUser>() {
                    @Override
                    public void done(ParseUser parseUser, ParseException e) {
                        if (e == null) {
                            addFriend(parseUser);
                        } else {
                            Log.d("AddFriend", e.getMessage());
                            Toast.makeText(getApplicationContext(), "Failed to Find E-mail: " +
                                    userInput.getText().toString(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
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

    private void addFriend(ParseUser friend){
        if(user.getObjectId().equals(friend.getObjectId())){                         // can't add yourself as friend
            Toast.makeText(getApplicationContext(), "Invalid Email Address",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isDuplicateFriend(user, friend)) {
            ParseObject friendList = new ParseObject("FriendList");
            friendList.put("userOne", user);
            friendList.put("userTwo", friend);
            friendList.put("confirmed", false);
            friendList.put("owedByOne", 0);
            friendList.put("owedByTwo", 0);
            friendList.saveInBackground();

            ParseObject temp = Utility.getRawListLocation();
            temp.getList("list").add(friendList);
            temp.pinInBackground();
            Utility.addToExistingFriendList(friendList.getObjectId(), friend);

            Toast.makeText(getApplicationContext(), "Sent a notification to <" +
                    Utility.getUserName(friend) + ">", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(getApplicationContext(), "<" + Utility.getUserName(friend) +
                "> and you are already friend", Toast.LENGTH_SHORT).show();
    }

    private boolean isDuplicateFriend(ParseUser userOne, ParseUser userTwo){            // check if added before
        ParseQuery<ParseObject> query = ParseQuery.getQuery("FriendList");
        ParseUser[] list = {userOne, userTwo};
        query.whereContainedIn("userOne", Arrays.asList(list));
        query.whereContainedIn("userTwo", Arrays.asList(list));
        try {
            return (query.count() != 0);
        }
        catch (ParseException x) {
            Log.d("checkDuplicate",x.getMessage());
            return true;
        }
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
                        Toast.makeText(mContext, "Check Internet Connection", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int position = (int) v.getTag();
                    Utility.Friend currentItem = mObject.get(position);
                    if (currentItem.isUserOne) {
                        Toast.makeText(mContext, "Waiting for confirmation from <" + currentItem.name
                                + ">", Toast.LENGTH_SHORT).show();
                    } else {
                        textCollection[position].setText("Yes");
                        currentItem.setConfirm();
                        v.setEnabled(false);
                        Toast.makeText(mContext, "Confirmed", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            Button delete = (Button) convertView.findViewById(R.id.button_friend_delete);
            delete.setTag(position);
            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!isNetworkConnected()) {
                        Toast.makeText(mContext, "Check Internet Connection", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int position = (int) v.getTag();
                    final Utility.Friend currentItem = mObject.get(position);

                    final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    TextView message = new TextView(mContext);
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

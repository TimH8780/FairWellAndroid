package io.github.budgetninja.fairwellandroid;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.text.InputType;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.github.budgetninja.fairwellandroid.Utility.getDPI;

/**
 *Created by HuMengpei on 9/30/2015.
 */
public class FriendsActivity extends AppCompatActivity{
    private ParseUser user;
    private FriendAdaptor adapter;
    private ListView view;
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

        view = (ListView) findViewById(R.id.friendlistview);
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
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.getFilter().filter(query.toLowerCase());
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText.toLowerCase());
                return false;
            }
        });
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                adapter.getFilter().filter("");
                return false;
            }
        });

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

    private void addFriend(final ParseUser friend){
        if(user.getObjectId().equals(friend.getObjectId())){                         // can't add yourself as friend
            Toast.makeText(getApplicationContext(), "Invalid Email Address",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isDuplicateFriend(user, friend)) {
            final ParseObject friendList = new ParseObject("FriendList");
            friendList.put("userOne", user);
            friendList.put("userTwo", friend);
            friendList.put("confirmed", false);
            friendList.put("owedByOne", 0);
            friendList.put("owedByTwo", 0);
            friendList.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    ParseObject temp = Utility.getRawListLocation();
                    temp.getList("list").add(friendList);
                    temp.pinInBackground();
                    Utility.Friend newItem = new Utility.Friend(friendList.getObjectId(), friend,
                            Utility.getUserName(friend), friend.getEmail(), 0, 0, false, true);
                    Utility.addToExistingFriendList(newItem);
                    adapter.updateData(Utility.generateFriendArray());

                    Toast.makeText(getApplicationContext(), "Sent a notification to <" +
                            Utility.getUserName(friend) + ">", Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }
        Toast.makeText(getApplicationContext(), "<" + Utility.getUserName(friend) +
                "> are already in your friend list", Toast.LENGTH_SHORT).show();
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
        List<Utility.Friend> mData;
        private List<Utility.Friend> backupData;

        public FriendAdaptor(Context context, int resource, List<Utility.Friend> objects){
            super(context, resource, objects);
            mContext = context;
            mResource = resource;
            mData = objects;
            backupData = objects;
        }

        public void updateData(List<Utility.Friend> data){
            mData = data;
            backupData = data;
            FriendAdaptor.this.notifyDataSetChanged();
        }

        private class ViewHolder{
            TextView nameText, emailText, statusText;
            Button confirmButton, deleteButton;
            ImageView photoImage;
        }

        @Override
        public int getCount(){
            return mData.size();
        }

        @Override
        public void remove(Utility.Friend item){
            mData.remove(item);
            backupData.remove(item);
            FriendAdaptor.this.notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            Utility.Friend currentItem = mData.get(position);
            ViewHolder holder;
            if(convertView == null){
                convertView = getLayoutInflater().inflate(mResource, parent, false);
                holder = new ViewHolder();
                holder.nameText = (TextView) convertView.findViewById(R.id.friend_name);
                holder.emailText = (TextView) convertView.findViewById(R.id.friend_email);
                holder.statusText = (TextView) convertView.findViewById(R.id.confirmResult);
                holder.confirmButton = (Button) convertView.findViewById(R.id.button_friend_confirm);
                holder.deleteButton = (Button) convertView.findViewById(R.id.button_friend_delete);
                holder.photoImage = (ImageView) convertView.findViewById(R.id.friend_photo);
                convertView.setTag(holder);
            }
            else{
                holder = (ViewHolder) convertView.getTag();
            }
            holder.nameText.setText(currentItem.name);
            holder.emailText.setText(currentItem.email);
            if(currentItem.photo != null){
                int DPI = getDPI(mContext);
                int pixel = 70 * (DPI / 160);
                Bitmap bmp = ContentActivity.decodeSampledBitmapFromByteArray(currentItem.photo, pixel, pixel);
                holder.photoImage.setImageBitmap(bmp);
            }
            else{
                holder.photoImage.setImageResource(R.drawable.profilepic);
            }


            if(!currentItem.isUserOne && !currentItem.confirm) {
                holder.confirmButton.setVisibility(View.VISIBLE);
                holder.statusText.setVisibility(View.VISIBLE);
                holder.statusText.setText("Awaiting for your\nresponse");
                Pair<Integer, TextView> data = new Pair<>(position, holder.statusText);
                holder.confirmButton.setTag(data);
                holder.confirmButton.setOnClickListener(new View.OnClickListener() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public void onClick(View button) {
                        if (!isNetworkConnected()) {
                            Toast.makeText(mContext, "Check Internet Connection", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Pair<Integer, TextView> data = (Pair<Integer, TextView>) button.getTag();
                        Utility.Friend currentItem = mData.get(data.first);
                        currentItem.setConfirm();
                        button.setVisibility(View.INVISIBLE);
                        data.second.setVisibility(View.INVISIBLE);
                        Toast.makeText(mContext, "Confirmed", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            else if(currentItem.isUserOne && !currentItem.confirm){
                holder.confirmButton.setVisibility(View.INVISIBLE);
                holder.statusText.setVisibility(View.VISIBLE);
                holder.statusText.setText("Awaiting for confirmation");
            }
            else{           //confirmed
                holder.confirmButton.setVisibility(View.INVISIBLE);
                holder.statusText.setVisibility(View.INVISIBLE);
            }

            holder.deleteButton.setTag(position);
            holder.deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View button) {
                    if (!isNetworkConnected()) {
                        Toast.makeText(mContext, "Check Internet Connection", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int position = (int) button.getTag();
                    final Utility.Friend currentItem = mData.get(position);

                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    TextView message = new TextView(mContext);
                    if (currentItem.confirm) {
                        message.setText("Are you sure you want to delete \n <" + currentItem.name + "> ?");
                        builder.setTitle("Delete Friend");
                    } else if (currentItem.isUserOne) {
                        message.setText("Are you sure you want to cancel the friend request to <" + currentItem.name + "> ?");
                        builder.setTitle("Cancel Friend Request");
                    } else {
                        message.setText("Are you sure you want to deny the friend request from <" + currentItem.name + "> ?");
                        builder.setTitle("Deny Friend Request");
                    }
                    message.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
                    message.setPadding(20, 20, 20, 20);
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

        @Override
        public Filter getFilter(){
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    if(constraint.equals("")){
                        results.values = backupData;
                    }
                    else{
                        List<Utility.Friend> data = new ArrayList<>();
                        for(int i = 0; i < backupData.size(); i++){
                            if(backupData.get(i).name.toLowerCase().contains(constraint)){
                                data.add(backupData.get(i));
                            }
                        }
                        results.values = data;
                    }
                    return results;
                }

                @SuppressWarnings("unchecked")
                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    mData = (List< Utility.Friend>) results.values;
                    FriendAdaptor.this.notifyDataSetChanged();
                }
            };
        }
    }
}

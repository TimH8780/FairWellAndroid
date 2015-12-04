package io.github.budgetninja.fairwellandroid;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.budgetninja.fairwellandroid.FriendObject.Friend;
import static io.github.budgetninja.fairwellandroid.Utility.getDPI;

/**
 *Created by HuMengpei on 9/30/2015.
 */
public class FriendsFragment extends Fragment{

    private static final int IMAGE_WIDTH_HEIGHT = 80;
    private ParseUser user;
    private ContentActivity parent;
    private FriendAdaptor adapter;
    protected FragmentManager fragMgr;
    protected FragmentTransaction fragTrans;

    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        setHasOptionsMenu(true);
        user = ParseUser.getCurrentUser();
        parent = (ContentActivity)getActivity();
        fragMgr = getFragmentManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_friend, container, false);
        ActionBar actionBar = parent.getSupportActionBar();
        if(actionBar != null) {
            final Drawable upArrow = ContextCompat.getDrawable(getContext(), R.drawable.abc_ic_ab_back_mtrl_am_alpha);
            upArrow.setColorFilter(ContextCompat.getColor(getContext(), R.color.coolBackground), PorterDuff.Mode.SRC_ATOP);
            actionBar.setHomeAsUpIndicator(upArrow);
        }
        parent.setTitle("Friend");

        final List<Friend> friendList;
        if(parent.isNetworkConnected()) { friendList = Utility.generateFriendArray(); }
        else { friendList = Utility.generateFriendArrayOffline(); }

        ListView view = (ListView) rootView.findViewById(R.id.friendlistview);
        adapter = new FriendAdaptor(parent, R.layout.item_friend, friendList);
        view.setAdapter(adapter);
        view.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @SuppressLint("CommitTransaction")
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

                fragTrans = fragMgr.beginTransaction();
                fragTrans.replace(R.id.container, new FriendDetailFragment(), "Friend_Detail").addToBackStack("Friend_Detail");
                fragTrans.commit();
                fragMgr.executePendingTransactions();
                FriendDetailFragment fragment = (FriendDetailFragment) fragMgr.findFragmentByTag("Friend_Detail");
                if(fragment != null){
                    fragment.setDate(friendList.get(position));
                }
            }
        });

        LinearLayout layout = (LinearLayout) rootView.findViewById(R.id.EmptyListView);
        TextView text = (TextView)layout.findViewById(R.id.EmptyListViewText);
        text.setText("No Friend");
        view.setEmptyView(layout);

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        //hide refresh button
        MenuItem item = menu.findItem(R.id.action_refresh);
        item.setVisible(false);

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
                parent.mMenuDrawer.closeMenu(false);
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
    }

    //this section is to programmatically set the search icon to be user's icon instead
    //of the default ugly black search icon
    //Issac
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem searchViewMenuItem = menu.findItem(R.id.action_search);
        SearchView mSearchView = (SearchView) MenuItemCompat.getActionView(searchViewMenuItem);
        int searchImgId = android.support.v7.appcompat.R.id.search_button;
        ImageView v = (ImageView) mSearchView.findViewById(searchImgId);
        v.setImageResource(R.drawable.ic_search_white);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_add_friend) {
            parent.mMenuDrawer.closeMenu(false);
            if(!parent.isNetworkConnected()) {
                Toast.makeText(parent, "Check Internet Connection", Toast.LENGTH_SHORT).show();
            }
            else{ displayAddFriendDialog(); }
            return true;
        }
        if(id == android.R.id.home){
            parent.mMenuDrawer.closeMenu(false);
            parent.fragMgr.popBackStack();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void displayAddFriendDialog(){          //show dialog and prompt user to enter email
        final AlertDialog.Builder builder = new AlertDialog.Builder(parent);
        final LinearLayout layout = new LinearLayout(parent);
        final TextView message = new TextView(parent);
        final EditText userInput = new EditText(parent);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams para = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        para.setMargins(20, 20, 20, 0);
        message.setText("Please enter the email address of your friend:");
        message.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        message.setLayoutParams(para);
        userInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        userInput.setLayoutParams(para);
        layout.addView(message);
        layout.addView(userInput);
        builder.setTitle("Add Friend");
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
                            e.printStackTrace();
                            Toast.makeText(parent, "Failed to Find E-mail: " + userInput.getText().toString(), Toast.LENGTH_SHORT).show();
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
            Toast.makeText(parent, "Invalid Email Address", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isDuplicateFriend(user, friend)) {
            AddFriendLoading task = new AddFriendLoading(parent, friend);
            task.execute();
            return;
        }
        Toast.makeText(parent, "<" + Utility.getProfileName(friend) + "> are already in your friend list", Toast.LENGTH_SHORT).show();
    }

    private boolean isDuplicateFriend(ParseUser userOne, ParseUser userTwo){            // check if added before
        ParseQuery<ParseObject> query = ParseQuery.getQuery("FriendList");
        ParseUser[] list = {userOne, userTwo};
        query.whereContainedIn("userOne", Arrays.asList(list));
        query.whereContainedIn("userTwo", Arrays.asList(list));
        try {
            return (query.count() != 0);
        }
        catch (ParseException e) {
            e.printStackTrace();
            return true;
        }
    }

    private class FriendAdaptor extends ArrayAdapter<Friend>{

        Context mContext;
        int mResource;
        List<Friend> mData;
        private List<Friend> backupData;

        public FriendAdaptor(Context context, int resource, List<Friend> objects){
            super(context, resource, objects);
            mContext = context;
            mResource = resource;
            mData = objects;
            backupData = objects;
        }

        public void updateData(List<Friend> data){
            mData = data;
            backupData = data;
            FriendAdaptor.this.notifyDataSetChanged();
        }

        private class ViewHolder{
            TextView nameText, emailText, statusText;
            ImageView photoImage;
        }

        @Override
        public int getCount(){
            return mData.size();
        }

        @Override
        public void remove(Friend item){
            mData.remove(item);
            backupData.remove(item);
            FriendAdaptor.this.notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup viewGroup){
            Friend currentItem = mData.get(position);
            ViewHolder holder;
            if(convertView == null){
                convertView = parent.getLayoutInflater().inflate(mResource, viewGroup, false);
                holder = new ViewHolder();
                holder.nameText = (TextView) convertView.findViewById(R.id.friend_name);
                holder.emailText = (TextView) convertView.findViewById(R.id.friend_email);
                holder.statusText = (TextView) convertView.findViewById(R.id.confirmResult);
                holder.photoImage = (ImageView) convertView.findViewById(R.id.friend_photo);
                convertView.setTag(holder);
            }
            else{
                holder = (ViewHolder) convertView.getTag();
            }
            holder.nameText.setText(currentItem.displayName);
            holder.emailText.setText(currentItem.email);
            if(currentItem.hasPhoto()){
                int DPI = getDPI(mContext);
                int pixel = IMAGE_WIDTH_HEIGHT * (DPI / 160);
                Bitmap bmp = HomepageFragment.decodeSampledBitmapFromByteArray(currentItem.photo, pixel, pixel);
                holder.photoImage.setImageBitmap(bmp);
            } else {
                holder.photoImage.setImageResource(R.drawable.profilepic);
            }

            if(!currentItem.isUserOne && !currentItem.confirm) {
                holder.statusText.setVisibility(View.VISIBLE);
                holder.statusText.setText("Awaiting for your response");
            }
            else if(currentItem.isUserOne && !currentItem.confirm){
                holder.statusText.setVisibility(View.VISIBLE);
                holder.statusText.setText("Awaiting for confirmation");
            }
            else{           //confirmed
                holder.statusText.setVisibility(View.INVISIBLE);
            }

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
                        List<Friend> data = new ArrayList<>();
                        for(int i = 0; i < backupData.size(); i++){
                            if(backupData.get(i).displayName.toLowerCase().contains(constraint)){
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
                    mData = (List<Friend>) results.values;
                    FriendAdaptor.this.notifyDataSetChanged();
                }
            };
        }
    }

    private class AddFriendLoading extends AsyncTask<Boolean, Void, Boolean> {
        private ProgressDialog dialog;
        private ParseUser friend;

        public AddFriendLoading(Context activity, ParseUser friend) {
            dialog = new ProgressDialog(activity);
            this.friend = friend;
        }

        @Override
        protected void onPreExecute() {
            dialog.setMessage("Searching and Adding Friend... Please Wait...");
            dialog.setCancelable(false);
            dialog.show();
        }

        @Override
        protected Boolean doInBackground(Boolean... params) {
            try {
                final ParseObject friendList = new ParseObject("FriendList");
                friendList.put("userOne", user);
                friendList.put("userTwo", friend);
                friendList.put("confirmed", false);
                friendList.put("owedByOne", 0);
                friendList.put("owedByTwo", 0);
                friendList.put("pendingStatement", false);
                friendList.save();
                ParseObject temp = Utility.getRawListLocation();
                temp.getList("list").add(friendList);
                temp.pinInBackground();
                Friend newItem = new Friend(friendList.getObjectId(), friendList, friend,
                        Utility.getProfileName(friend), friend.getEmail(), 0, 0, false, false, true);
                Utility.addToExistingFriendList(newItem);
                return true;
            } catch (ParseException e){
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            adapter.updateData(Utility.generateFriendArray());
            if (result) {
                Toast.makeText(parent, "Sent a notification to <" + Utility.getProfileName(friend) + ">", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(parent, "Request Failed, Please Retry", Toast.LENGTH_SHORT).show();
            }
        }
    }


    public static class FriendDetailFragment extends Fragment {

        private ContentActivity parent;
        private Friend object;
        private Button confirm, reject, delete;
        private TextView name, email;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

            final View rootView = inflater.inflate(R.layout.fragment_friend_detail, container, false);
            ActionBar actionBar = parent.getSupportActionBar();
            if (actionBar != null) {
                final Drawable upArrow = ContextCompat.getDrawable(getContext(), R.drawable.abc_ic_ab_back_mtrl_am_alpha);
                upArrow.setColorFilter(ContextCompat.getColor(getContext(), R.color.coolBackground), PorterDuff.Mode.SRC_ATOP);
                actionBar.setHomeAsUpIndicator(upArrow);
            }
            parent.setTitle("Friend Information");

            confirm = (Button) rootView.findViewById(R.id.friend_detail_confirm);
            reject = (Button) rootView.findViewById(R.id.friend_detail_reject);
            delete = (Button) rootView.findViewById(R.id.friend_detail_delete);
            name = (TextView) rootView.findViewById(R.id.friend_detail_name);
            email = (TextView) rootView.findViewById(R.id.friend_detail_email);

            return rootView;
        }

        @Override
        public void onCreate( Bundle savedInstanceState) {
            parent = (ContentActivity)getActivity();
            setHasOptionsMenu(true);
            super.onCreate(savedInstanceState);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            MenuItem item = menu.findItem(R.id.action_refresh);
            item.setVisible(false);
           // inflater.inflate(R.menu.menu_setting, menu);
            super.onCreateOptionsMenu(menu, inflater);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case android.R.id.home:
                    parent.mMenuDrawer.closeMenu(false);
                    parent.fragMgr.popBackStack();
                    return true;
                default:
                    return super.onOptionsItemSelected(item);
            }
        }

        private void setDate(Friend item){
            object = item;
            buttonSetup();
            dataDisplay();
        }

        private void buttonSetup(){
            if(!object.isUserOne && !object.confirm) {
                confirm.setVisibility(View.VISIBLE);
                reject.setVisibility(View.VISIBLE);
                delete.setVisibility(View.GONE);
            } else {
                confirm.setVisibility(View.GONE);
                reject.setVisibility(View.GONE);
                delete.setVisibility(View.VISIBLE);
            }

            confirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View button) {
                    if (!parent.isNetworkConnected()) {
                        Toast.makeText(parent, "Check Internet Connection", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    object.setConfirm();
                    confirm.setVisibility(View.GONE);
                    reject.setVisibility(View.GONE);
                    delete.setVisibility(View.VISIBLE);
                    Toast.makeText(parent, "Confirmed", Toast.LENGTH_SHORT).show();
                    }
                });

            reject.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!parent.isNetworkConnected()) {
                        Toast.makeText(parent, "Check Internet Connection", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(parent);
                    TextView message = new TextView(parent);
                    message.setText("Are you sure you want to deny the friend request from <" + object.displayName + "> ?");
                    builder.setTitle("Deny Friend Request");
                    message.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
                    message.setPadding(20, 20, 20, 20);
                    builder.setView(message);
                    builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (object.currentUserOwed > 0 || object.friendOwed > 0 || object.isPendingStatement) {
                                Toast.makeText(parent, "You can't delete friend with non-zero balance/ pending statement!", Toast.LENGTH_LONG).show();
                            } else {
                                object.deleteFriend("You denied the friend request from " + object.getRealName(), null);
                                Utility.removeFromExistingFriendList(object);
                                parent.fragMgr.popBackStack();
                            }
                        }
                    });
                    builder.setNegativeButton("Cancel", null);
                    final AlertDialog dialog = builder.create();
                    dialog.show();
                }
            });

            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!parent.isNetworkConnected()) {
                        Toast.makeText(parent, "Check Internet Connection", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(parent);
                    TextView message = new TextView(parent);
                    message.setText("Are you sure you want to delete <" + object.displayName + "> ?");
                    builder.setTitle("Delete Friend");
                    message.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
                    message.setPadding(20, 20, 20, 20);
                    builder.setView(message);
                    builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (object.currentUserOwed > 0 || object.friendOwed > 0 || object.isPendingStatement) {
                                Toast.makeText(parent, "You can't delete friend with non-zero balance/ pending statement!", Toast.LENGTH_LONG).show();
                            } else {
                                object.deleteFriend("You deleted the friendship with " + object.getRealName(), null);
                                Utility.removeFromExistingFriendList(object);
                                parent.fragMgr.popBackStack();
                            }
                        }
                    });
                    builder.setNegativeButton("Cancel", null);
                    final AlertDialog dialog = builder.create();
                    dialog.show();
                }
            });
        }

        private void dataDisplay(){
            name.setText(object.displayName);
            email.setText(object.email);

            //more info
        }
    }

}

package io.github.budgetninja.fairwellandroid;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.Arrays;
import java.util.List;

import static io.github.budgetninja.fairwellandroid.ContentActivity.OWE_BALANCE;

/**
 *Created by Tim on 11/03/15.
 */
public class FriendObject {

    public static class Friend implements Comparable<Friend> {

        private static final int CONFIRM = 0;
        private static final int DELETE = 1;

        private String parseObjectID;
        private ParseObject friendRelationship;
        private ParseUser friend;
        private ParseUser user = ParseUser.getCurrentUser();
        private boolean reload;
        String displayName, email, firstName, lastName, phoneNumber, address_1, address_2, selfDescription;
        boolean isPendingStatement;
        boolean confirm;
        boolean isUserOne;
        double currentUserOwed;
        double friendOwed;
        byte[] photo;

        Friend(String parseObjectID, ParseObject friendRelation, ParseUser friend, String displayName, String email, double currentUserOwed,
               double friendOwed, boolean isPendingStatement, boolean confirm, boolean isUserOne, String firstName, String lastName,
               String phoneNumber, String address_1, String address_2, String selfDescription) {
            this.friend = friend;
            obtainPhoto();
            this.parseObjectID = parseObjectID;
            this.friendRelationship = friendRelation;
            this.displayName = displayName;
            this.email = email;
            this.isPendingStatement = isPendingStatement;
            this.confirm = confirm;
            this.currentUserOwed = currentUserOwed;
            this.friendOwed = friendOwed;
            this.isUserOne = isUserOne;
            this.firstName = firstName;
            this.lastName = lastName;
            this.phoneNumber = phoneNumber;
            this.address_1 = address_1;
            this.address_2 = address_2;
            this.selfDescription = selfDescription;
            reload = false;
        }

        @Override
        public int compareTo(@NonNull Friend another) {
            return displayName.compareToIgnoreCase(another.displayName);
        }

        private void obtainPhoto() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (friend != null) {
                        ParseFile data = friend.getParseFile("photo");
                        if (data != null) {
                            data.getDataInBackground(new GetDataCallback() {
                                @Override
                                public void done(byte[] bytes, ParseException e) {
                                    if (e == null) {
                                        Friend.this.photo = bytes;
                                    } else {
                                        Friend.this.photo = null;
                                    }
                                }
                            });
                        } else photo = null;
                    } else photo = null;
                }
            }).start();
        }

        public boolean hasPhoto() {
            if (photo != null) {
                reload = false;
                return true;
            }
            if (!reload) {
                obtainPhoto();
                reload = true;
            }
            return false;
        }

        protected ParseObject insertParseUser(ParseObject object, String key) {
            object.put(key, friend);
            return object;
        }

        protected ParseObject insertFriendship(ParseObject object, String key) {
            object.put(key, friendRelationship);
            return object;
        }

        public String getRealName() {
            return firstName + " " + lastName;
        }

        public ParseObject generateFriendToFriendRelationship(final Friend another) {
            ParseQuery<ParseObject> query = ParseQuery.getQuery("FriendList");
            ParseUser[] list = {friend, another.friend};
            query.whereContainedIn("userOne", Arrays.asList(list));
            query.whereContainedIn("userTwo", Arrays.asList(list));
            try {
                return query.getFirst();
            } catch (ParseException e) {
                ParseObject friendList = new ParseObject("FriendList");
                friendList.put("userOne", friend);
                friendList.put("userTwo", another.friend);
                friendList.put("confirmed", false);
                friendList.put("owedByOne", 0);
                friendList.put("owedByTwo", 0);
                friendList.put("pendingStatement", true);
                friendList.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        Utility.editNewEntryField(friend, true, "You sent a friend request to " + Utility.getName(another.friend));
                        Utility.editNewEntryField(another.friend, true, Utility.getName(friend) + " sent you a friend request");
                    }
                });
                return friendList;
            }
        }

        public void setConfirm(Context context) {
            FriendProcess task = new FriendProcess(context, CONFIRM, null, null);
            task.execute();
        }

        public void setPendingStatement() {
            try {
                friendRelationship.put("pendingStatement", true);
                isPendingStatement = true;
                friendRelationship.save();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        public void deleteFriend(Context context, String message_you, String message_friend) {
            FriendProcess task = new FriendProcess(context, DELETE, message_you, message_friend);
            task.execute();
        }

        public void notifyChange(String message_you, String message_friend) {
            Utility.editNewEntryField(friend, true, message_friend);
            Utility.editNewEntryField(user, message_you);
        }

        public String toString() {       //For arrayAdapter
            return displayName;
        }

        public String toStringAllData() {
            StringBuilder builder = new StringBuilder();
            builder.append(displayName).append(" | ");
            builder.append(email).append(" | ");
            builder.append(confirm).append(" | ");
            builder.append(isUserOne).append(" | ");
            builder.append(currentUserOwed).append(" | ");
            builder.append(friendOwed).append(" | ");
            builder.append(currentUserOwed - friendOwed).append(" | ");
            builder.append(isPendingStatement).append(" | ");
            builder.append(firstName).append(" | ");
            builder.append(lastName).append(" | ");
            builder.append(phoneNumber).append(" | ");
            builder.append(address_1).append(" | ");
            builder.append(address_2).append(" | ");
            builder.append(selfDescription).append(" | ");
            return builder.toString();
        }

        public boolean isEqual(Friend another) {
            return parseObjectID.equals(another.parseObjectID);
        }

        public boolean isSamePerson(ParseUser another) {
            return friend.getObjectId().equals(another.getObjectId());
        }

        private class FriendProcess extends AsyncTask<Boolean, Void, Boolean> {
            private ProgressDialog dialog;
            private Context activity;
            private int type;
            private String message_you, message_friend;

            public FriendProcess(Context activity, int type, String message_you, String message_friend) {
                dialog = new ProgressDialog(activity);
                this.activity = activity;
                this.type = type;
                this.message_you = message_you;
                this.message_friend = message_friend;
            }

            @Override
            protected void onPreExecute() {
                dialog.setMessage("Processing... Please Wait...");
                dialog.setCancelable(false);
                dialog.show();
            }

            @Override
            protected Boolean doInBackground(Boolean... params) {
                if (type == CONFIRM) {
                    friendRelationship.put("confirmed", true);
                    friendRelationship.saveInBackground();
                    confirm = true;
                    Utility.editNewEntryField(friend, true, Utility.getName(user) + " confirmed your friend request");
                    Utility.editNewEntryField(user, "You confirmed the friend request from " + Utility.getName(friend));
                }

                if (type == DELETE) {
                    ParseObject object = Utility.getRawListLocation();
                    object.getList("list").remove(friendRelationship);
                    object.pinInBackground();
                    friendRelationship.deleteInBackground();
                    Utility.editNewEntryField(friend, true, message_friend);
                    Utility.editNewEntryField(user, message_you);
                }
                return true;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
                if(type == CONFIRM){
                    FriendsFragment.FriendDetailFragment fragment =
                            (FriendsFragment.FriendDetailFragment)((ContentActivity) activity).fragMgr.findFragmentByTag("Friend_Detail");
                    if(fragment != null){
                        fragment.dataDisplay();
                    }
                }
            }
        }
    }

}

package io.github.budgetninja.fairwellandroid;

import android.support.annotation.NonNull;

import com.parse.GetCallback;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.Arrays;

/**
 *Created by Tim on 11/03/15.
 */
public class FriendObject {

    public static class Friend implements Comparable<Friend>{

        private String parseObjectID;
        private ParseUser friend;
        private boolean reload;
        String name;
        String email;
        boolean confirm;
        boolean isUserOne;
        double currentUserOwed;
        double friendOwed;
        byte[] photo;

        Friend(String parseObjectID, ParseUser friend, String name, String email, double currentUserOwed,
               double friendOwed, boolean confirm, boolean isUserOne){
            this.parseObjectID = parseObjectID;
            this.friend = friend;
            this.name = name;
            this.email = email;
            this.confirm = confirm;
            this.currentUserOwed = currentUserOwed;
            this.friendOwed = friendOwed;
            this.isUserOne = isUserOne;
            reload = false;
            obtainPhoto();
        }

        @Override
        public int compareTo(@NonNull Friend another){
            return name.compareToIgnoreCase(another.name);
        }

        private void obtainPhoto(){         //Run by other thread
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

        public boolean hasPhoto(){
            if(photo != null){
                reload = false;
                return true;
            }
            if(!reload){
                obtainPhoto();
                reload = true;
            }
            return false;
        }

        public void generateFriendToFriendRelationship(final Friend another){
            ParseQuery<ParseObject> query = ParseQuery.getQuery("FriendList");
            ParseUser[] list = {friend, another.friend};
            query.whereContainedIn("userOne", Arrays.asList(list));
            query.whereContainedIn("userTwo", Arrays.asList(list));
            query.getFirstInBackground(new GetCallback<ParseObject>() {
                @Override
                public void done(ParseObject parseObject, ParseException e) {
                    if(e != null){
                        final ParseObject friendList = new ParseObject("FriendList");
                        friendList.put("userOne", friend);
                        friendList.put("userTwo", another.friend);
                        friendList.put("confirmed", false);
                        friendList.put("owedByOne", 0);
                        friendList.put("owedByTwo", 0);
                        friendList.saveInBackground(new SaveCallback() {
                            @Override
                            public void done(ParseException e) {
                                Utility.editNewEntryField(friend, true);
                                Utility.editNewEntryField(another.friend, true);
                            }
                        });
                    }
                }
            });
        }

        public void setConfirm(){
            ParseQuery<ParseObject> query = ParseQuery.getQuery("FriendList");
            query.getInBackground(parseObjectID, new GetCallback<ParseObject>() {
                @Override
                public void done(ParseObject parseObject, ParseException e) {
                    if (e == null) {
                        parseObject.put("confirmed", true);
                        confirm = true;
                        parseObject.saveInBackground();
                        Utility.editNewEntryField(friend, true);
                    }
                }
            });
        }

        public void deleteFriend(){
            ParseQuery<ParseObject> query = ParseQuery.getQuery("FriendList");
            query.getInBackground(parseObjectID, new GetCallback<ParseObject>() {
                @Override
                public void done(ParseObject parseObject, ParseException e) {
                    if (e == null) {
                        ParseObject object = Utility.getRawListLocation();
                        object.getList("list").remove(parseObject);
                        object.pinInBackground();
                        parseObject.deleteInBackground();
                        Utility.editNewEntryField(friend, true);
                    }
                }
            });
        }

        public void notifyChange(){ Utility.editNewEntryField(friend, true); }

        public String toString(){       //For arrayAdapter
            return name;
        }

        public String toStringAllData(){
            StringBuilder builder = new StringBuilder();
            builder.append(name).append(" | ");
            builder.append(email).append(" | ");
            builder.append(confirm).append(" | ");
            builder.append(isUserOne).append(" | ");
            builder.append(currentUserOwed).append(" | ");
            builder.append(friendOwed).append(" | ");
            builder.append(currentUserOwed - friendOwed).append(" | ");
            return builder.toString();
        }
    }

}

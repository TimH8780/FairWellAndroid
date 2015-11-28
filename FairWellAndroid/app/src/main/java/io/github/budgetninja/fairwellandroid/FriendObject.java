package io.github.budgetninja.fairwellandroid;

import android.support.annotation.NonNull;

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
        private ParseObject friendRelationship;
        private ParseUser friend;
        private boolean reload;
        String name;
        String email;
        boolean isPendingStatement;
        boolean confirm;
        boolean isUserOne;
        double currentUserOwed;
        double friendOwed;
        byte[] photo;

        Friend(String parseObjectID, ParseObject friendRelation, ParseUser friend, String name, String email, double currentUserOwed,
               double friendOwed, boolean isPendingStatement, boolean confirm, boolean isUserOne){
            obtainPhoto();
            this.parseObjectID = parseObjectID;
            this.friendRelationship = friendRelation;
            this.friend = friend;
            this.name = name;
            this.email = email;
            this.isPendingStatement = isPendingStatement;
            this.confirm = confirm;
            this.currentUserOwed = currentUserOwed;
            this.friendOwed = friendOwed;
            this.isUserOne = isUserOne;
            reload = false;
        }

        @Override
        public int compareTo(@NonNull Friend another){
            return name.compareToIgnoreCase(another.name);
        }

        public double getNetBalance(){
            return friendOwed - currentUserOwed;
        }

        private void obtainPhoto(){
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

        protected ParseObject insertParseUser(ParseObject object, String key){
            object.put(key, friend);
            return object;
        }

        protected ParseObject insertFriendship(ParseObject object, String key){
            object.put(key, friendRelationship);
            return object;
        }

        public ParseObject generateFriendToFriendRelationship(final Friend another){
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
                        Utility.editNewEntryField(friend, true);
                        Utility.editNewEntryField(another.friend, true);
                    }
                });
                return friendList;
            }
        }

        public void setConfirm(){
            friendRelationship.put("confirmed", true);
            confirm = true;
            friendRelationship.saveInBackground();
            Utility.editNewEntryField(friend, true);
        }

        public void setPendingStatement(){
            try {
                friendRelationship.put("pendingStatement", true);
                isPendingStatement = true;
                friendRelationship.save();
                Utility.editNewEntryField(friend, true);
            } catch (ParseException e){
                e.printStackTrace();
            }
        }

        public void deleteFriend(){
            ParseObject object = Utility.getRawListLocation();
            object.getList("list").remove(friendRelationship);
            object.pinInBackground();
            friendRelationship.deleteInBackground();
            Utility.editNewEntryField(friend, true);
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
            builder.append(isPendingStatement).append(" | ");
            return builder.toString();
        }

        public boolean isEqual(Friend another){
            return parseObjectID.equals(another.parseObjectID);
        }
    }

}

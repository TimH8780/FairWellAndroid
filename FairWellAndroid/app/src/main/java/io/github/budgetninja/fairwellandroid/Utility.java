package io.github.budgetninja.fairwellandroid;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.widget.DatePicker;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 *Created by Issac on 9/23/2015.
 */
public class Utility {

    public static boolean isNormalUser(ParseUser user) {
        return (!isFacebookUser(user) && !isTwitterUser(user));
    }

    public static boolean isFacebookUser(ParseUser user){
        try {
            if (user.fetchIfNeeded().get("usernameFacebook") != null) {
                return true;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isTwitterUser(ParseUser user){
        try {
            if (user.fetchIfNeeded().get("usernameTwitter") != null) {
                return true;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String getUserName(ParseUser user){
        if(isFacebookUser(user)){
            return user.getString("usernameFacebook");
        }
        if(isTwitterUser(user)){
            return user.getString("usernameTwitter");
        }
        return (user.getString("First_Name") + " " + user.getString("Last_Name"));
    }

    public static class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

        private int viewSel;
        private static final int YEAR = 0;
        private static final int MONTH = 1;
        private static final int DAY = 2;

        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            viewSel = getArguments().getInt("ViewSel");
            ArrayList<Integer> dateList = getArguments().getIntegerArrayList("DateList");

            int year, month, day;
            if (dateList.get(viewSel + YEAR) < 1900 || dateList.get(viewSel + YEAR) > 2100) {
                // Use the current date as the default date in the picker
                final Calendar c = Calendar.getInstance();
                year = c.get(Calendar.YEAR);
                month = c.get(Calendar.MONTH);
                day = c.get(Calendar.DAY_OF_MONTH);
            }
            else{
                year = dateList.get(viewSel + YEAR);
                month = dateList.get(viewSel + MONTH);
                day = dateList.get(viewSel + DAY);
            }
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {
            ((AddStatementFragment)getActivity().getSupportFragmentManager().findFragmentByTag("Add")).setDate(year, month, day, viewSel);
        }
    }

    public static class Friend implements Comparable<Friend>{

        private String parseObjectID;
        private ParseUser friend;
        String name;
        String email;
        boolean confirm;
        boolean isUserOne;
        double currentUserOwed;
        double friendOwed;

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
        }

        @Override
        public int compareTo(Friend another){
            return name.compareToIgnoreCase(another.name);
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
                        editNewEntryField(friend, true);
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
                        editNewEntryField(friend, true);
                    }
                }
            });
        }

        public void addNewStatementMoney(final double userOwe, final double friendOwe){
            currentUserOwed += userOwe;
            friendOwed += friendOwe;
            ParseQuery<ParseObject> query = ParseQuery.getQuery("FriendList");
            query.getInBackground(parseObjectID, new GetCallback<ParseObject>() {
                @Override
                public void done(ParseObject parseObject, ParseException e) {
                    if (e == null) {
                        double owedByOne = parseObject.getDouble("owedByOne");
                        double owedByTwo = parseObject.getDouble("owedByTwo");
                        if(isUserOne){
                            parseObject.put("owedByOne", owedByOne + userOwe);
                            parseObject.put("owedByTwo", owedByTwo + friendOwe);
                        }
                        else{
                            parseObject.put("owedByOne", owedByOne + friendOwe);
                            parseObject.put("owedByTwo", owedByTwo + userOwe);
                        }
                        editNewEntryField(friend, true);
                        generateRawFriendList(ParseUser.getCurrentUser());
                    }
                }
            });
        }

        public void notifyChange(){ editNewEntryField(friend, true); }

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

    public static void generateRawFriendList(ParseUser user){
        ParseQuery<ParseObject> queryA = ParseQuery.getQuery("FriendList");
        queryA.whereEqualTo("userOne", user);
        ParseQuery<ParseObject> queryB = ParseQuery.getQuery("FriendList");
        queryB.whereEqualTo("userTwo", user);
        List<ParseQuery<ParseObject>> list = new ArrayList<>();
        list.add(queryA);
        list.add(queryB);
        ParseQuery.or(list).findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> list, ParseException e) {
                if (e == null) {
                    ParseObject temp = ParseUser.getCurrentUser().getParseObject("newEntry");
                    temp.put("list", list);
                    temp.pinInBackground();
                    editNewEntryField(ParseUser.getCurrentUser(), false);        // causing the local data to upload
                }
            }
        });
    }

    private static List<Friend> pFriendList = null;   // change back to private later
    private static boolean changedRecord = true;

    public static List<Friend> generateFriendArray(){
        if(pFriendList == null || changedRecord) {                                                     //need to test the case if the list in userA change if
            boolean isUserOne;                                                                           //userB confirm the friendship (2 phones needed)
            List<Utility.Friend> friendList = new ArrayList<>();
            List<String> offlineList = new ArrayList<>();
            List<ParseObject> rawList = getRawListLocation().getList("list");

            for (int i = 0; i < rawList.size(); i++) {
                ParseUser user = ParseUser.getCurrentUser();
                Double userowed, friendowed;
                try {
                    ParseObject object = rawList.get(i).fetch();
                    if (user.getObjectId().equals(object.getParseUser("userOne").getObjectId())) {
                        user = object.getParseUser("userTwo");
                        userowed = object.getDouble("owedByOne");
                        friendowed = object.getDouble("owedByTwo");
                        isUserOne = true;
                    } else {
                        user = object.getParseUser("userOne");
                        userowed = object.getDouble("owedByTwo");
                        friendowed = object.getDouble("owedByOne");
                        isUserOne = false;
                    }
                    Friend friendItem = new Utility.Friend(object.getObjectId(), user, Utility.getUserName(user),
                            user.getString("email"), userowed, friendowed, object.getBoolean("confirmed"), isUserOne);
                    offlineList.add(friendItem.toStringAllData());
                    friendList.add(friendItem);
                } catch (ParseException e) {
                    Log.d("Fetch", e.getMessage());
                }
            }
            ParseObject temp = ParseUser.getCurrentUser().getParseObject("newEntry");
            temp.put("offlineFriendList", offlineList);
            temp.pinInBackground();
            pFriendList = new ArrayList<>(friendList);
            Collections.sort(pFriendList);
            changedRecord = false;
        }
        return pFriendList;
    }

    public static List<Friend> generateFriendArrayOffline(){
        if(pFriendList == null){
            List<Utility.Friend> offlineFriendList = new ArrayList<>();
            List<String> offlineList = getRawListLocation().getList("offlineFriendList");
            int indexA, indexB;
            String name, email;
            boolean confirm, isUserOne;
            double userOwed, friendOwed;
            for(int i = 0; i < offlineList.size(); i++){
                String item = offlineList.get(i);
                indexA = item.indexOf(" | ", 0);
                name = item.substring(0, indexA);
                indexB = item.indexOf(" | ", indexA + 3);
                email = item.substring(indexA + 3, indexB);
                indexA = item.indexOf(" | ", indexB + 3);
                confirm = Boolean.parseBoolean(item.substring(indexB + 3, indexA));
                indexB = item.indexOf(" | ", indexA + 3);
                isUserOne = Boolean.parseBoolean(item.substring(indexA + 3, indexB));
                indexA = item.indexOf(" | ", indexB + 3);
                userOwed = Double.parseDouble(item.substring(indexB + 3, indexA));
                indexB = item.indexOf(" | ", indexA + 3);
                friendOwed = Double.parseDouble(item.substring(indexA + 3, indexB));
                offlineFriendList.add(new Friend(null, null, name, email, userOwed, friendOwed, confirm, isUserOne));
            }
            pFriendList = new ArrayList<>(offlineFriendList);
            Collections.sort(pFriendList);
            setChangedRecord();
        }
        return pFriendList;
    }

    public static void addToExistingFriendList(Friend newItem){
        if(pFriendList != null){
            newItem.notifyChange();
            ParseObject object = getRawListLocation();
            List<String> offlist = object.getList("offlineFriendList");
            offlist.add(newItem.toStringAllData());
            object.put("offlineFriendList", offlist);
            object.pinInBackground();
            int pos = searchPosition(0, pFriendList.size(), newItem);
            pFriendList.add(pos, newItem);
        }
    }

    private static int searchPosition(int start, int end, Friend item){
        int pos = (start + end)/2;
        if(pos == start){
            return item.compareTo(pFriendList.get(start)) < 0 ? start : end;
        }
        if(item.compareTo(pFriendList.get(pos)) < 0){
            return searchPosition(start, pos, item);
        }
        return searchPosition(pos, end, item);
    }

    public static void removeFromExistingFriendList(Friend item){
        if(pFriendList != null){
            ParseObject object = getRawListLocation();
            List<String> offlist = object.getList("offlineFriendList");
            offlist.remove(item.toStringAllData());
            object.put("offlineFriendList", offlist);
            object.pinInBackground();
            pFriendList.remove(item);
        }
    }

    public static void resetExistingFriendList(){ pFriendList = null; }
    public static void setChangedRecord(){ changedRecord = true; }

    public static boolean checkNewEntryField(){
        try {
            return ParseUser.getCurrentUser().getParseObject("newEntry").fetch().getBoolean("newEntry");
        }catch (ParseException e) {
            Log.d("checkNewEntry", e.getMessage());
        }
        return true;
    }

    private static void editNewEntryField(ParseUser user, final boolean newResult){
        user.getParseObject("newEntry").fetchIfNeededInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject parseObject, ParseException e) {
                parseObject.put("newEntry", newResult);
                parseObject.saveInBackground();
            }
        });
    }

    public static ParseObject getRawListLocation(){
        try {
            ParseObject x = ParseUser.getCurrentUser().getParseObject("newEntry");
            x.fetchFromLocalDatastore();
            return x;
        }catch (ParseException e) {
            Log.d("checkNewEntryField", e.getMessage());
        }
        return new ParseObject("Friend_update");
    }

    public static void setNewEntryFieldForAllFriend(){
        List<Friend> allFriend = generateFriendArray();
        for(int i = 0; i < allFriend.size(); i++){
            allFriend.get(i).notifyChange();
        }
    }

    public static int getDPI(Context ctx) {
        return (int)(ctx.getResources().getDisplayMetrics().density*160f);
    }
}

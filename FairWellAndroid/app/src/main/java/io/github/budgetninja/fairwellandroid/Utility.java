package io.github.budgetninja.fairwellandroid;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.widget.DatePicker;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Calendar;
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

        @Override
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

    public static class Friend {

        private String parseObjectID;
        private ParseUser friend;
        String name;
        String email;
        boolean confirm;
        boolean isUserOne;

        Friend(String parseObjectID, ParseUser friend, String name, String email, boolean confirm, boolean isUserOne){
            this.parseObjectID = parseObjectID;
            this.friend = friend;
            this.name = name;
            this.email = email;
            this.confirm = confirm;
            this.isUserOne = isUserOne;
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

    private static List<Friend> pFriendList = null;
    private static boolean changedRecord = true;

    public static List<Friend> generateFriendArray(){
        if(pFriendList == null || changedRecord) {                                                     //need to test the case if the list in userA change if
            boolean isUserOne;                                                                           //userB confirm the friendship (2 phones needed)
            List<Utility.Friend> friendList = new ArrayList<>();
            List<ParseObject> rawList = getRawListLocation().getList("list");

            for (int i = 0; i < rawList.size(); i++) {
                ParseUser user = ParseUser.getCurrentUser();
                try {
                    ParseObject object = rawList.get(i).fetch();
                    if (user.getObjectId().equals(object.getParseUser("userOne").getObjectId())) {
                        user = object.getParseUser("userTwo");
                        isUserOne = true;
                    } else {
                        user = object.getParseUser("userOne");
                        isUserOne = false;
                    }
                    friendList.add(new Utility.Friend(object.getObjectId(), user, Utility.getUserName(user),
                            user.getString("email"), object.getBoolean("confirmed"), isUserOne));
                } catch (ParseException e) {
                    Log.d("Fetch", e.getMessage());
                }
            }
            pFriendList = new ArrayList<>(friendList);
            changedRecord = false;
        }
        return pFriendList;
    }

    public static void addToExistingFriendList(String pParseObjectID, ParseUser pFriend){
        if(pFriendList != null){
            pFriendList.add(new Friend(pParseObjectID, pFriend, getUserName(pFriend), pFriend.getEmail(), false, true));
        }
    }

    public static void removeFromExistingFriendList(Friend item){
        if(pFriendList != null){
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

    public static void editNewEntryField(ParseUser user, final boolean newResult){
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

}

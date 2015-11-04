package io.github.budgetninja.fairwellandroid;

import android.content.Context;
import android.util.Log;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.budgetninja.fairwellandroid.FriendObject.Friend;

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
                    editNewEntryField(ParseUser.getCurrentUser(), false);
                    setChangedRecord();
                }
            }
        });
    }

    private static List<Friend> pFriendList = null;
    private static boolean changedRecord = true;

    public static List<Friend> generateFriendArray(){
        Log.d("Friend", "Start");
        if(pFriendList == null || changedRecord) {
            boolean isUserOne;
            Log.d("Friend", "Generating");
            List<Friend> friendList = new ArrayList<>();
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
                    Friend friendItem = new Friend(object.getObjectId(), user, Utility.getUserName(user),
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
            List<Friend> offlineFriendList = new ArrayList<>();
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
        }catch (ParseException|NullPointerException e) {
            Log.d("getRawListLocation", "Not exist online, Generating Now...");          // Generate a new one if not exist
            ParseObject tempA = new ParseObject("Friend_update");
            tempA.put("newEntry", false);
            tempA.saveInBackground();
            ParseUser.getCurrentUser().put("newEntry", tempA);
            ParseUser.getCurrentUser().saveInBackground();
            ParseObject tempB = ParseUser.getCurrentUser().getParseObject("newEntry");
            tempB.put("list", new ArrayList<ParseObject>());
            tempB.put("offlineFriendList", new ArrayList<String>());
            tempB.pinInBackground();
            return false;
        }
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
        ParseUser user = ParseUser.getCurrentUser();
        try {
            Log.d("getRawListLocation", "From offline");                        // Get it from local if possible
            ParseObject offline = user.getParseObject("newEntry");
            offline.fetchFromLocalDatastore();
            return offline;
        }catch (ParseException|NullPointerException e) {
            try {
                Log.d("getRawListLocation", "From online");                     // Get it from online if can't find in local
                ParseObject online = user.getParseObject("newEntry");
                online.fetch();
                online.pinInBackground();
                return online;
            } catch (ParseException|NullPointerException e1) {
                Log.d("getRawListLocation", "Not exist, Generating Now...");  // Generate a new one if neither in local nor online
                ParseObject tempA = new ParseObject("Friend_update");
                tempA.put("newEntry", false);
                tempA.saveInBackground();
                user.put("newEntry", tempA);
                user.saveInBackground();
                ParseObject tempB = user.getParseObject("newEntry");
                tempB.put("list", new ArrayList<ParseObject>());
                tempB.put("offlineFriendList", new ArrayList<String>());
                tempB.pinInBackground();
                return tempB;
            }
        }
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

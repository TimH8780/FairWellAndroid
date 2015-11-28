package io.github.budgetninja.fairwellandroid;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import io.github.budgetninja.fairwellandroid.FriendObject.Friend;
import io.github.budgetninja.fairwellandroid.StatementObject.Statement;
import static io.github.budgetninja.fairwellandroid.ContentActivity.OWE_BALANCE;
import static io.github.budgetninja.fairwellandroid.ContentActivity.OWN_BALANCE;
import static io.github.budgetninja.fairwellandroid.ContentActivity.NORMAL_USER;
import static io.github.budgetninja.fairwellandroid.ContentActivity.FACEBOOK_USER;
import static io.github.budgetninja.fairwellandroid.ContentActivity.TWITTER_USER;

/**
 *Created by Issac on 9/23/2015.
 */
public class Utility {

    public static boolean isNormalUser(ParseUser user) {
        try{
            int userType = user.fetchIfNeeded().getInt("userType");
            return userType == NORMAL_USER;
        } catch (ParseException e){
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isFacebookUser(ParseUser user){
        try{
            int userType = user.fetchIfNeeded().getInt("userType");
            return userType == FACEBOOK_USER;
        } catch (ParseException e){
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isTwitterUser(ParseUser user){
        try{
            int userType = user.fetchIfNeeded().getInt("userType");
            return userType == TWITTER_USER;
        } catch (ParseException e){
            e.printStackTrace();
        }
        return false;
    }

    public static String getName(ParseUser user){
        try {
            user.fetchIfNeeded();
            return (user.getString("First_Name") + " " + user.getString("Last_Name"));
        } catch (ParseException e){
            return "";
        }
    }

    public static String getProfileName(ParseUser user){
        try {
            user.fetchIfNeeded();
            return (user.getString("profileName"));
        } catch (ParseException e){
            return "";
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
        try{
            List<ParseObject> rawList = ParseQuery.or(list).find();
            ParseObject temp = getRawListLocation();
            if (temp != null) {
                temp.put("list", rawList);
                temp.pinInBackground();
                editNewEntryField(ParseUser.getCurrentUser(), false);

                Utility.setChangedRecordFriend();
                List<FriendObject.Friend> tempB = Utility.generateFriendArray();
                Double runningSum = 0.0;
                Double runningSub = 0.0;
                for (int i = 0; i < tempB.size(); i++) {
                    runningSum += tempB.get(i).friendOwed;
                    runningSub -= tempB.get(i).currentUserOwed;
                }
                OWN_BALANCE = runningSum;
                OWE_BALANCE = runningSub;
            }
        } catch (ParseException e){
            Log.d("RawFriendList", e.getMessage());
        }
    }

    private static List<Friend> pFriendList = null;
    private static boolean changedRecordFriend = true;

    public static List<Friend> generateFriendArray(){
        if(pFriendList == null || changedRecordFriend) {
            Log.d("Friend", "Generating-Start");
            boolean isUserOne;
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
                    Friend friendItem = new Friend(object.getObjectId(), object, user, Utility.getName(user), user.getString("email"),
                            userowed, friendowed, object.getBoolean("pendingStatement"), object.getBoolean("confirmed"), isUserOne);
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
            changedRecordFriend = false;
            Log.d("Friend", "Generating-End");
        }
        return pFriendList;
    }

    public static List<Friend> generateFriendArrayOffline(){
        if(pFriendList == null){
            List<Friend> offlineFriendList = new ArrayList<>();
            List<String> offlineList = getRawListLocation().getList("offlineFriendList");
            int indexA, indexB;
            String name, email;
            boolean confirm, isUserOne, isPendingStatement;
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
                indexA = item.indexOf(" | ", indexB + 3);
                isPendingStatement = Boolean.parseBoolean(item.substring(indexB + 3, indexA));
                offlineFriendList.add(new Friend(null, null, null, name, email, userOwed, friendOwed, isPendingStatement, confirm, isUserOne));
            }
            pFriendList = new ArrayList<>(offlineFriendList);
            Collections.sort(pFriendList);
            setChangedRecordFriend();
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
        if(end == 0){
            return 0;
        }
        int pos = (start + end)/2;
        if(pos == start){
            return item.compareTo(pFriendList.get(start)) < 0 ? start : end;
        }
        if(item.compareTo(pFriendList.get(pos)) < 0){
            return searchPosition(start, pos, item);
        }
        return searchPosition(pos, end, item);
    }

    private static int searchPosition(int start, int end, Statement item){
        if(end == 0){
            return 0;
        }
        int pos = (start + end)/2;
        if(pos == start){
            return item.compareTo(pStatementList.get(start)) < 0 ? start : end;
        }
        if(item.compareTo(pStatementList.get(pos)) < 0){
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

    public static void resetExistingList(){
        pFriendList = null;
        pStatementList = null;
    }

    public static void setChangedRecordFriend(){ changedRecordFriend = true; }
    public static void setChangedRecordStatement(){ changedRecordStatement = true; }

    public static boolean checkNewEntryField(){
        try {
            return ParseUser.getCurrentUser().getParseObject("newEntry").fetch().getBoolean("newEntry");
        }catch (ParseException|NullPointerException e) {
            Log.d("getRawListLocation", "Not exist");
            return true;
        }
    }

    public static void editNewEntryField(ParseUser user, final boolean newResult){
        if(user != null) {
            user.getParseObject("newEntry").fetchIfNeededInBackground(new GetCallback<ParseObject>() {
                @Override
                public void done(ParseObject parseObject, ParseException e) {
                    parseObject.put("newEntry", newResult);
                    parseObject.saveInBackground();
                }
            });
        }
    }

    public static ParseObject getRawListLocation(){
        ParseUser user = ParseUser.getCurrentUser();
        try {                                                                                // Get it from local if possible
            ParseObject offline = user.getParseObject("newEntry");
            offline.fetchFromLocalDatastore();
            Log.d("getRawListLocation", "From offline");
            return offline;
        }catch (ParseException|NullPointerException e) {
            try {                                                                           // Get it from online if can't find in local
                ParseObject online = user.getParseObject("newEntry");
                online.fetch();
                online.pinInBackground();
                Log.d("getRawListLocation", "From online");
                return online;
            } catch (ParseException|NullPointerException e1) {
                if(user != null) {                                                          // Generate a new one if neither in local nor online
                    Log.d("getRawListLocation", "Not exist! Generating now...");
                    ParseObject tempA = new ParseObject("Friend_update");
                    tempA.put("newEntry", false);
                    tempA.put("list", new ArrayList<ParseObject>());
                    tempA.put("offlineFriendList", new ArrayList<String>());
                    tempA.put("statementList", new ArrayList<ParseObject>());
                    tempA.saveInBackground();
                    user.put("newEntry", tempA);
                    user.saveInBackground();
                    ParseObject tempB = user.getParseObject("newEntry");
                    tempB.pinInBackground();
                    return tempB;
                }
                return null;
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

    public static int getPixel(int desiredDp, Resources resources){
        float scale = resources.getDisplayMetrics().density;
        return  (int) (desiredDp * scale + 0.5f);
    }

    public static void generateRawStatementList(final ParseUser user){
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Statement");
        query.whereEqualTo("payer", user);
        query.whereEqualTo("payerReject", false);
        query.whereEqualTo("payerPaid", false);
        try{
            List<ParseObject> list = query.find();
            List<ParseObject> result = new ArrayList<>();
            ParseObject temp = getRawListLocation();
            try{
                ParseQuery<ParseObject> queryB;
                for (int i = 0; i < list.size(); i++){
                    queryB = ParseQuery.getQuery("StatementGroup");
                    queryB.whereEqualTo("payer", list.get(i));
                    result.add(queryB.getFirst());
                }
                queryB = ParseQuery.getQuery("StatementGroup");
                queryB.whereEqualTo("payee", user);
                result.addAll(queryB.find());
                editNewEntryField(ParseUser.getCurrentUser(), false);
            } catch (ParseException e1){
                Log.d("RawStatementList", e1.toString());
            }
            if(temp != null){
                temp.put("statementList", result);
                temp.pinInBackground();
                Utility.setChangedRecordStatement();
                Utility.generateStatementArray();
            }
        } catch (ParseException e){
            Log.d("RawStatementList", e.toString());
        }
    }

    private static List<Statement> pStatementList = null;
    private static boolean changedRecordStatement = true;

    public static List<Statement> generateStatementArray(){
        Log.d("generate", Boolean.toString(pStatementList == null) + " " + Boolean.toString(changedRecordStatement));
        if(pStatementList == null || changedRecordStatement){
            Log.d("Statement", "Generating-Start");
            List<Statement> statementList = new ArrayList<>();
            List<ParseObject> rawList = getRawListLocation().getList("statementList");

            boolean isPayee = false;
            boolean payeeConfirm;
            String description, category, submitBy;
            Date date, deadline;
            int mode, unknown;
            double unknownAmount, totalAmount;
            List<ParseObject> list;
            for (int i = 0; i < rawList.size(); i++) try{
                ParseObject object = rawList.get(i).fetch();
                if(!isPayee) {
                    if (object.getParseUser("payee") == ParseUser.getCurrentUser()) {
                        Log.d("StatementArray", "Switch");
                        isPayee = true;
                    }
                }
                payeeConfirm = object.getBoolean("payeeConfirm");
                description = object.getString("description");
                category = object.getString("category");
                submitBy = object.getString("submittedBy");
                date = object.getDate("date");
                deadline = object.getDate("deadline");
                mode = object.getInt("mode");
                unknown = object.getInt("unknown");
                unknownAmount = object.getDouble("unknownAmount");
                totalAmount = object.getDouble("paymentAmount");
                list = object.getList("payer");
                Statement statement = new Statement(object, payeeConfirm, description, category, date, deadline, mode, unknown, unknownAmount, totalAmount,
                        submitBy, object.getParseUser("payee"), list, isPayee);
                statementList.add(statement);
            } catch (ParseException e) {
                Log.d("Fetch", e.getMessage());
            }
            pStatementList = new ArrayList<>(statementList);
            Collections.sort(pStatementList);
            changedRecordStatement = false;
            Log.d("Statement", "Generating-End");
        }
        return pStatementList;
    }

    public static void addToExistingStatementList(Statement newItem){
        if(pStatementList != null){
            int pos = searchPosition(0, pStatementList.size(), newItem);
            pStatementList.add(pos, newItem);
            Log.d("SubmitStatement", "Add Statement: " + Integer.toString(pos));
        }
    }

    public static void removeFromExistingStatementList(Statement item){
        if(pFriendList != null){
            //ParseObject object = getRawListLocation();
            //List<String> offlist = object.getList("offlineFriendList");
            //offlist.remove(item.toStringAllData());
            //object.put("offlineFriendList", offlist);
            //object.pinInBackground();
            pStatementList.remove(item);
        }
    }

}

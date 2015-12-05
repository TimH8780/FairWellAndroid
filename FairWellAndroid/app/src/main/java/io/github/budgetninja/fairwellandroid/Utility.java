package io.github.budgetninja.fairwellandroid;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import io.github.budgetninja.fairwellandroid.FriendObject.Friend;
import io.github.budgetninja.fairwellandroid.StatementObject.Statement;

import static io.github.budgetninja.fairwellandroid.ContentActivity.NORMAL_USER;
import static io.github.budgetninja.fairwellandroid.ContentActivity.OWE_BALANCE;
import static io.github.budgetninja.fairwellandroid.ContentActivity.OWN_BALANCE;

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
            String displayName = user.getString("profileName");
            if(displayName == null || displayName.isEmpty()){
                displayName = getName(user);
                user.put("profileName", displayName);
                user.saveInBackground();
            }
            return displayName;
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
                editNewEntryField(ParseUser.getCurrentUser(), false, null);

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
            e.printStackTrace();
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
                    Friend friendItem = new Friend(object.getObjectId(), object, user, Utility.getProfileName(user), user.getString("email"),
                            userowed, friendowed, object.getBoolean("pendingStatement"), object.getBoolean("confirmed"), isUserOne,
                            user.getString("First_Name"), user.getString("Last_Name"), user.getString("phoneNumber"),
                            user.getString("addressLine1"), user.getString("addressLine2"), user.getString("selfDescription"));
                    offlineList.add(friendItem.toStringAllData());
                    friendList.add(friendItem);
                } catch (ParseException e) {
                    e.printStackTrace();
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
            String name, email, firstName, lastName, phoneNumber, address_1, address_2, selfDescription;
            boolean confirm, isUserOne, isPendingStatement;
            double userOwed, friendOwed;
            Double runningSum = 0.0;
            Double runningSub = 0.0;

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
                indexB = item.indexOf(" | ", indexA + 3);
                firstName = item.substring(indexA + 3, indexB);
                indexA = item.indexOf(" | ", indexB + 3);
                lastName = item.substring(indexB + 3, indexA);
                indexB = item.indexOf(" | ", indexA + 3);
                phoneNumber = item.substring(indexA + 3, indexB);
                indexA = item.indexOf(" | ", indexB + 3);
                address_1 = item.substring(indexB + 3, indexA);
                indexB = item.indexOf(" | ", indexA + 3);
                address_2 = item.substring(indexA + 3, indexB);
                indexA = item.indexOf(" | ", indexB + 3);
                selfDescription = item.substring(indexB + 3, indexA);

                offlineFriendList.add(new Friend(null, null, null, name, email, userOwed, friendOwed, isPendingStatement, confirm,
                        isUserOne, firstName, lastName, phoneNumber, address_1, address_2, selfDescription));
                runningSum += friendOwed;
                runningSub -= userOwed;
            }

            OWN_BALANCE = runningSum;
            OWE_BALANCE = runningSub;
            pFriendList = new ArrayList<>(offlineFriendList);
            Collections.sort(pFriendList);
            setChangedRecordFriend();
        }
        return pFriendList;
    }

    public static void addToExistingFriendList(Friend newItem){
        if(pFriendList != null){
            ParseObject object = getRawListLocation();
            List<String> offlist = object.getList("offlineFriendList");
            offlist.add(newItem.toStringAllData());
            object.put("offlineFriendList", offlist);
            object.pinInBackground();
            int pos = searchPosition(0, pFriendList.size(), newItem);
            pFriendList.add(pos, newItem);
        }
        newItem.notifyChange("You sent a friend request to " + newItem.getRealName(), newItem.getRealName() + " sent you a friend request");
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
        pDashBoardData = null;
    }

    public static void setChangedRecordFriend(){ changedRecordFriend = true; }
    public static void setChangedRecordStatement(){ changedRecordStatement = true; }
    public static void setChangedRecordDashboard(){ changedRecordDashBoard = true; }

    public static boolean checkNewEntryField(){
        try {
            return ParseUser.getCurrentUser().getParseObject("newEntry").fetch().getBoolean("newEntry");
        } catch (ParseException|NullPointerException e) {
            Log.d("getRawListLocation", "Not exist");
            return true;
        }
    }

    public static void editNewEntryField(ParseUser user, final boolean newResult, final String message){
        if(user != null) try{
            ParseObject parseObject = user.getParseObject("newEntry").fetchIfNeeded();
            if(message != null){
                List<String> temp = parseObject.getList("dashboardData");
                Log.d("Dashboard Number: ", Integer.toString(temp.size()));
                temp.add(Utility.generateMessage(message));
                parseObject.put("dashboardData", temp);
            }
            parseObject.put("newEntry", newResult);
            parseObject.save();

            if(message != null && user.getObjectId().equals(ParseUser.getCurrentUser().getObjectId())){
                //addToExistingDashboardList(generateMessage(message));
                setChangedRecordDashboard();
                getDashboardData();
            }
        } catch(ParseException e){
            Log.d("ChangeFriendNewEntry", e.getMessage());
        }
    }

    public static void editNewEntryField(ParseUser user, final String message){
        if(user != null && message != null) try{
            ParseObject parseObject = user.getParseObject("newEntry").fetchIfNeeded();
            List<String> temp = parseObject.getList("dashboardData");
            temp.add(Utility.generateMessage(message));
            parseObject.put("dashboardData", temp);
            parseObject.save();

            if(user.getObjectId().equals(ParseUser.getCurrentUser().getObjectId())){
                //addToExistingDashboardList(generateMessage(message));
                setChangedRecordDashboard();
                getDashboardData();
            }
        } catch (ParseException e){
            Log.d("ChangeFriendNewEntry", e.getMessage());
        }
    }

    public static ParseObject getRawListLocation(){
        ParseUser user = ParseUser.getCurrentUser();
        try {                                                                                // Get it from local if possible
            ParseObject offline = user.getParseObject("newEntry");
            offline.fetchFromLocalDatastore();
            Log.d("getRawListLocation", "From offline");
            return offline;
        } catch (ParseException|NullPointerException e) {
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
                    tempA.put("dashboardData", new ArrayList<String>());
                    tempA.put("list", new ArrayList<ParseObject>());
                    tempA.put("offlineFriendList", new ArrayList<String>());
                    tempA.put("statementList", new ArrayList<ParseObject>());
                    tempA.saveInBackground();
                    user.put("newEntry", tempA);
                    user.put("userType", 0);
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
            allFriend.get(i).notifyChange(null, null);
        }
    }

    public static int getDPI(Context ctx) {
        return (int)(ctx.getResources().getDisplayMetrics().density*160f);
    }

    public static int getPixel(int desiredDp, Resources resources){
        float scale = resources.getDisplayMetrics().density;
        return  (int)(desiredDp * scale + 0.5f);
    }

    public static void generateRawStatementList(final ParseUser user){
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Statement");
        query.whereEqualTo("payer", user);
        query.whereEqualTo("payerReject", false);
        query.whereEqualTo("payerPaid", false);
        query.whereEqualTo("paymentPending", false);

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
                editNewEntryField(ParseUser.getCurrentUser(), false, null);
            } catch (ParseException e1){
                e1.printStackTrace();
            }

            if(temp != null){
                temp.put("statementList", result);
                temp.pinInBackground();
                Utility.setChangedRecordStatement();
                Utility.generateStatementArray();
            }
        } catch (ParseException e){
            e.printStackTrace();
        }
    }

    private static List<Statement> pStatementList = null;
    private static boolean changedRecordStatement = true;

    public static List<Statement> generateStatementArray(){
        if(pStatementList == null || changedRecordStatement){
            Log.d("Statement", "Generating-Start");
            List<Statement> statementList = new ArrayList<>();
            List<ParseObject> rawList = getRawListLocation().getList("statementList");

            boolean isPayee = false;
            boolean payeeConfirm;
            String note, description, category, submitBy;
            ParseFile picture;
            Date date, deadline;
            int mode, unknown;
            double unknownAmount, totalAmount;
            List<ParseObject> list;

            for (int i = 0; i < rawList.size(); i++) try{
                ParseObject object = rawList.get(i).fetch();
                if(!isPayee) {
                    if (object.getParseUser("payee") == ParseUser.getCurrentUser()) {
                        isPayee = true;
                    }
                }
                note = object.getString("note");
                picture = object.getParseFile("picture");
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
                Statement statement = new Statement(note, picture, object, payeeConfirm, description, category, date, deadline, mode, unknown,
                        unknownAmount, totalAmount, submitBy, object.getParseUser("payee"), list, isPayee);
                statementList.add(statement);
            } catch (ParseException e) {
                e.printStackTrace();
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
        }
    }

    public static void removeFromExistingStatementList(Statement item){
        if(pFriendList != null){
            pStatementList.remove(item);
        }
    }

    public static String generateMessage(String message){
        Calendar calendar = Calendar.getInstance();
        DateFormat format_one = new SimpleDateFormat("MMMM yyyy", Locale.US);
        DateFormat format_two = new SimpleDateFormat("MM/dd", Locale.US);
        format_one.setTimeZone(TimeZone.getTimeZone("GMT-05:00"));
        format_two.setTimeZone(TimeZone.getTimeZone("GMT-05:00"));

        return format_one.format(calendar.getTime()) + " | " + message + " on " + format_two.format(calendar.getTime());
    }

    private static List<String> pDashBoardData = null;
    private static boolean changedRecordDashBoard = true;

    public static List<String> getDashboardData(){
        if(pDashBoardData == null || changedRecordDashBoard) try{
            ParseObject location = ParseUser.getCurrentUser().fetchIfNeeded().getParseObject("newEntry").fetch();
            pDashBoardData = location.getList("dashboardData");
            changedRecordDashBoard = false;
            location.pinInBackground();
        } catch (ParseException e){
            e.printStackTrace();
        }
        return pDashBoardData;
    }

    public static List<String> getDashboardDataOffline(){
        if(pDashBoardData == null){
            pDashBoardData = getRawListLocation().getList("dashboardData");
            setChangedRecordDashboard();
        }
        return pDashBoardData;
    }

    public static void addToExistingDashboardList(String newItem) {
        if (pDashBoardData != null) {
            pDashBoardData.add(newItem);
        }
    }

    public static Bitmap bitmapCompress(Bitmap b, int rate){
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.JPEG, rate, stream);
        //BitmapFactory.Options o = new BitmapFactory.Options();
        //o.inJustDecodeBounds = true;
        return BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.toByteArray().length);
    }

    public static byte[] getBytesFromBitmap(Bitmap bitmap, int rate) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, rate, stream);
        return stream.toByteArray();
    }
}

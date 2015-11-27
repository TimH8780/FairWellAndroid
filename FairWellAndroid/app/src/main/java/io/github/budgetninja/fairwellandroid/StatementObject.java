package io.github.budgetninja.fairwellandroid;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.github.budgetninja.fairwellandroid.FriendObject.Friend;
import static io.github.budgetninja.fairwellandroid.ContentActivity.OWE_BALANCE;
import static io.github.budgetninja.fairwellandroid.ContentActivity.OWN_BALANCE;

/**
 *Created by Tim on 11/07/15.
 */
public class StatementObject {

    public static class SummaryStatement {

        String description, category;
        Date date, deadline;
        int mode, unknown;
        double totalAmount;
        Friend payee;
        List<Pair<Friend,Double>> payer;

        public SummaryStatement(String description, String category, Date date, Date deadline, int mode, int unknown, double totalAmount,
                                Friend payee, List<Pair<Friend, Double>> payer){
            this.description = description;
            this.category = category;
            this.date = date;
            this.deadline = deadline;
            this.mode = mode;
            this.unknown = unknown;
            this.totalAmount = totalAmount;
            this.payee = payee;
            this.payer = payer;
        }
    }


    public static class Statement implements Comparable<Statement> {

        private ParseObject object;
        private List<ParseObject> payer;
        List<SubStatement> payerList;
        String description, category;
        Date date, deadline;
        int mode, unknown;
        double totalAmount, unknownAmount;
        String submitBy;
        ParseUser payee;
        boolean isPayee, payeeConfirm;

        public Statement(ParseObject object, boolean payeeConfirm, String description, String category, Date date, Date deadline, int mode,
                         int unknown, double unknownAmount, double totalAmount, String submitBy, ParseUser payee, List<ParseObject> payer, boolean isPayee) {
            this.object = object;
            this.payee = payee;
            this.payer = payer;
            getPayer();
            this.payeeConfirm = payeeConfirm;
            this.description = description;
            this.category = category;
            this.date = date;
            this.deadline = deadline;
            this.mode = mode;
            this.unknown = unknown;
            this.unknownAmount = unknownAmount;
            this.totalAmount = totalAmount;
            this.submitBy = submitBy;
            this.isPayee = isPayee;
        }

        public Statement(ParseObject object, boolean isPayee){
            this.object = object;
            payee = object.getParseUser("payee");
            payer = object.getList("payer");
            getPayer();
            payeeConfirm = object.getBoolean("payeeConfirm");
            description = object.getString("description");
            category = object.getString("category");
            date = object.getDate("date");
            deadline = object.getDate("deadline");
            mode = object.getInt("mode");
            unknown = object.getInt("unknown");
            unknownAmount = object.getDouble("unknownAmount");
            totalAmount = object.getDouble("paymentAmount");
            submitBy = object.getString("submittedBy");
            this.isPayee = isPayee;
        }

        private void getPayer(){
            this.payerList = new ArrayList<>();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for(int i = 0; i < payer.size(); i++) try{
                        ParseObject item = payer.get(i).fetch();
                        payerList.add(new SubStatement(item, payee, item.getParseUser("payer"), item.getBoolean("payerConfirm"),
                                item.getDouble("amount"), item.getParseObject("friendship")));
                    } catch (ParseException e){
                        Log.d("Fetch", e.toString());
                    }
                }
            }).start();
        }

        public SubStatement findPayerStatement(ParseUser item){
            for(int i = 0; i < payerList.size(); i++){
                if(payerList.get(i).isUserStatement(item)){
                    return payerList.get(i);
                }
            }
            return null;
        }

        private void notifyChange(){
            for(int i = 0; i < payerList.size(); i++){
                payerList.get(i).notifyChange();
            }
        }

        @Override
        public int compareTo(Statement another) {
            return deadline.compareTo(another.deadline);
        }

        public void setPayeeConfirm() {
            payeeConfirm = true;
            object.put("payeeConfirm", true);
            object.saveInBackground();
            notifyChange();
        }
    }


    public static class SubStatement {

        private ParseObject object;
        private ParseObject payerRelation;
        private ParseUser payee;
        private ParseUser payer;
        String payerName;
        boolean payerConfirm;
        double payerAmount;

        private SubStatement(ParseObject object, ParseUser payee, ParseUser payer, boolean payerConfirm, double payerAmount,
                             ParseObject payerRelation) {
            this.object = object;
            this.payee = payee;
            this.payer = payer;
            this.payerName = Utility.getName(payer);
            this.payerConfirm = payerConfirm;
            this.payerAmount = payerAmount;
            this.payerRelation = payerRelation;
        }

        private void notifyChange() {
            Utility.editNewEntryField(payer, true);
        }

        private boolean isUserStatement(ParseUser item) {
            return payer == item;
        }

        public void setPayerConfirm(final ContentActivity context) {
            payerConfirm = true;
            new Thread(new Runnable() {
                @Override
                public void run(){
                    try {
                        Log.d("Balance", "Start");
                        object.put("payerConfirm", true);
                        object.saveInBackground();
                        Log.d("Balance", "Continue_1");

                        ParseQuery query = ParseQuery.getQuery("Statement");
                        query.whereEqualTo("payerConfirm", false);
                        query.whereEqualTo("friendship", payerRelation);
                        int counter = query.count();
                        if(counter == 0) {
                            payerRelation.put("pendingStatement", false);
                        }
                        Log.d("Balance", "Continue_2");

                        double currentBalance;
                        if (payerRelation.getParseUser("userOne") == payer) {
                            currentBalance = payerRelation.fetch().getDouble("owedByOne");
                            currentBalance += payerAmount;
                            payerRelation.put("owedByOne", currentBalance);
                            payerRelation.saveInBackground(new SaveCallback() {
                                @Override
                                public void done(ParseException e) {
                                    if(e == null) Log.d("Balance", "Success");
                                    else Log.d("Balance", "Fail - " + e.getMessage());
                                }
                            });
                        } else {
                            currentBalance = payerRelation.fetch().getDouble("owedByTwo");
                            currentBalance += payerAmount;
                            payerRelation.put("owedByTwo", currentBalance);
                            payerRelation.saveInBackground(new SaveCallback() {
                                @Override
                                public void done(ParseException e) {
                                    if(e == null) Log.d("Balance", "Success");
                                    else Log.d("Balance", "Fail - " + e.getMessage());
                                }
                            });
                        }
                        Log.d("Balance", "Continue_3");

                        OWE_BALANCE -= payerAmount;
                        Utility.editNewEntryField(payee, true);
                        Log.d("Balance", "End");

                        context.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "Statement Processed", Toast.LENGTH_LONG).show();
                            }
                        });
                    } catch (ParseException e) {
                        Log.d("Balance", e.toString());
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }
}

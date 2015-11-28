package io.github.budgetninja.fairwellandroid;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
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

    public static final int CONFIRM = 0;
    public static final int REJECT = 1;
    public static final int DELETE = 2;

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
            for(int i = 0; i < payer.size(); i++) try{
                ParseObject item = payer.get(i).fetch();
                payerList.add(new SubStatement(Statement.this, item, payee, item.getParseUser("payer"), item.getBoolean("payerConfirm"),
                        item.getBoolean("payerReject"), item.getBoolean("payerPaid"), item.getBoolean("paymentPending"),
                        item.getDouble("amount"), item.getParseObject("friendship")));
            } catch (ParseException e){
                Log.d("Fetch", e.toString());
            }
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

        public void setPayeeConfirm(ContentActivity activity) {
            PayeeStatementProcess process = new PayeeStatementProcess(activity, CONFIRM);
            process.execute();
        }

        protected class PayeeStatementProcess extends AsyncTask<Boolean, Void, Boolean> {
            private ProgressDialog dialog;
            private Context activity;
            int type;

            public PayeeStatementProcess(Context activity, int type) {
                dialog = new ProgressDialog(activity);
                this.activity = activity;
                this.type = type;
            }

            @Override
            protected void onPreExecute() {
                dialog.setMessage("Processing... Please Wait...");
                dialog.show();
            }

            @Override
            protected Boolean doInBackground(Boolean... params) {
                try {
                    if(type == REJECT || type == DELETE) {
                        for (int i = 0; i < payer.size(); i++) {
                            SubStatement temp = payerList.get(i);
                            int amount = (!temp.payerReject && !temp.payerConfirm && !temp.payerPaid) ? 1 : 0;
                            ParseQuery query = ParseQuery.getQuery("Statement");
                            query.whereEqualTo("payerConfirm", false);
                            query.whereEqualTo("payerReject", false);
                            query.whereEqualTo("payerPaid", false);
                            query.whereEqualTo("friendship", temp.payerRelation);
                            if (query.count() == amount) {
                                Log.d("Reject", temp.payerRelation.getObjectId());
                                temp.payerRelation.put("pendingStatement", false);
                                temp.payerRelation.save();
                            }
                        }
                        for (int i = 0; i < payer.size(); i++) {
                            payer.get(i).delete();
                        }
                        Utility.removeFromExistingStatementList(Statement.this);
                        object.delete();
                        return true;
                    }

                    if(type == CONFIRM){
                        payeeConfirm = true;
                        object.put("payeeConfirm", true);
                        object.saveInBackground();
                        notifyChange();
                        return true;
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                    return false;
                }
                return false;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
                if(result){ Toast.makeText(activity, "Statement processed", Toast.LENGTH_SHORT).show(); }
                else{ Toast.makeText(activity, "Failed to complete, Please retry", Toast.LENGTH_SHORT).show(); }
                ((ContentActivity)activity).fragMgr.popBackStack();
            }
        }
    }


    public static class SubStatement {

        private Statement parent;
        private ParseObject object;
        private ParseObject payerRelation;
        private ParseUser payee;
        private ParseUser payer;
        String payerName;
        boolean payerConfirm, payerReject, payerPaid, paymentPending;
        double payerAmount;

        private SubStatement(Statement parent, ParseObject object, ParseUser payee, ParseUser payer, boolean payerConfirm, boolean payerReject,
                             boolean payerPaid, boolean paymentPending, double payerAmount, ParseObject payerRelation) {
            this.parent = parent;
            this.object = object;
            this.payee = payee;
            this.payer = payer;
            this.payerName = Utility.getName(payer);
            this.payerConfirm = payerConfirm;
            this.payerReject = payerReject;
            this.payerPaid = payerPaid;
            this.paymentPending = paymentPending;
            this.payerAmount = payerAmount;
            this.payerRelation = payerRelation;
        }

        private void notifyChange() {
            Utility.editNewEntryField(payer, true);
        }

        private boolean isUserStatement(ParseUser item) {
            return payer == item;
        }

        public void setPayerConfirm(ContentActivity context) {
            payerConfirm = true;
            PayerStatementProcess process = new PayerStatementProcess(context, CONFIRM);
            process.execute();
        }

        public void setPayerReject(ContentActivity context){
            payerReject = true;
            PayerStatementProcess process = new PayerStatementProcess(context, REJECT);
            process.execute();
        }

        private class PayerStatementProcess extends AsyncTask<Boolean, Void, Boolean> {
            private ProgressDialog dialog;
            private Context activity;
            private int type;

            public PayerStatementProcess(Context activity, int type) {
                dialog = new ProgressDialog(activity);
                this.activity = activity;
                this.type = type;
            }

            @Override
            protected void onPreExecute() {
                dialog.setMessage("Processing... Please Wait...");
                dialog.show();
            }

            @Override
            protected Boolean doInBackground(Boolean... params) {
                try {
                    if(type == CONFIRM) {
                        object.put("payerConfirm", true);
                        object.save();

                        ParseQuery query = ParseQuery.getQuery("Statement");
                        query.whereEqualTo("payerConfirm", false);
                        query.whereEqualTo("payerReject", false);
                        query.whereEqualTo("payerPaid", false);
                        query.whereEqualTo("friendship", payerRelation);
                        int counter = query.count();
                        if (counter == 0) {
                            payerRelation.put("pendingStatement", false);
                        }

                        double currentBalance;
                        if (payerRelation.getParseUser("userOne") == payer) {
                            currentBalance = payerRelation.fetch().getDouble("owedByOne");
                            currentBalance += payerAmount;
                            payerRelation.put("owedByOne", currentBalance);
                            payerRelation.save();
                        } else {
                            currentBalance = payerRelation.fetch().getDouble("owedByTwo");
                            currentBalance += payerAmount;
                            payerRelation.put("owedByTwo", currentBalance);
                            payerRelation.save();
                        }

                        OWE_BALANCE -= payerAmount;
                        Utility.editNewEntryField(payee, true);
                        return true;
                    }

                    if(type == REJECT){
                        Utility.removeFromExistingStatementList(parent);
                        object.put("payerReject", true);
                        object.save();

                        ParseQuery query = ParseQuery.getQuery("Statement");
                        query.whereEqualTo("payerConfirm", false);
                        query.whereEqualTo("payerReject", false);
                        query.whereEqualTo("payerPaid", false);
                        query.whereEqualTo("friendship", payerRelation);
                        int counter = query.count();
                        if (counter == 0) {
                            payerRelation.put("pendingStatement", false);
                            payerRelation.save();
                        }
                        Utility.editNewEntryField(payee, true);
                        return true;
                    }

                } catch (ParseException e) {
                    e.printStackTrace();
                    return false;
                }
                return false;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
                if(result){
                    Toast.makeText(activity, "Statement processed", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(activity, "Failed to complete, Please retry", Toast.LENGTH_SHORT).show();
                }
                ((ContentActivity)activity).fragMgr.popBackStack();
            }
        }
    }

}

package io.github.budgetninja.fairwellandroid;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Pair;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

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
    public static final int BY_DISPLAY_NAME = 0;
    public static final int BY_DEADLINE = 1;
    public static final int BY_AMOUNT = 2;

    public static int SORT_TYPE = BY_DEADLINE;

    public static class SummaryStatement {
        String note;
        ParseFile picture;
        String description, category;
        Date date, deadline;
        int mode, unknown;
        double totalAmount;
        Friend payee;
        List<Pair<Friend,Double>> payer;

        public SummaryStatement(String note, ParseFile picture, String description, String category, Date date, Date deadline, int mode,
                                int unknown, double totalAmount, Friend payee, List<Pair<Friend, Double>> payer){
            this.note = note;
            this.picture = picture;
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

        String note;
        ParseFile picture;
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

        public Statement(String note, ParseFile picture, ParseObject object, boolean payeeConfirm, String description, String category, Date date,
                         Date deadline, int mode, int unknown, double unknownAmount, double totalAmount, String submitBy, ParseUser payee,
                         List<ParseObject> payer, boolean isPayee) {
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
            this.note = note;
            this.picture = picture;
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
            note = object.getString("note");
            picture = object.getParseFile("picture");
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
                e.printStackTrace();
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
                payerList.get(i).notifyChange("A new statement, <" + description + ">, was added");
            }
        }

        @Override
        public int compareTo(@NonNull Statement another) {
            switch (SORT_TYPE) {
                case 0:
                    if(payee == ParseUser.getCurrentUser()){
                        if(another.payee == ParseUser.getCurrentUser()){
                            return 0;
                        }
                        return ("YOU".compareTo(Utility.getProfileName(another.payee))) * -1;
                    } else {
                        if(another.payee == ParseUser.getCurrentUser()){
                            return (Utility.getProfileName(payee).compareTo("YOU")) * -1;
                        }
                        return (Utility.getProfileName(payee).compareTo(Utility.getProfileName(another.payee))) * -1;
                    }

                case 1:
                    return deadline.compareTo(another.deadline);

                case 2:
                    SubStatement temp, temp2;
                    if(payee == ParseUser.getCurrentUser()){
                        if(another.payee == ParseUser.getCurrentUser()){
                            return Double.valueOf(totalAmount).compareTo(another.totalAmount);
                        }
                        temp = another.findPayerStatement(ParseUser.getCurrentUser());
                        if(temp != null) {
                            return Double.valueOf(totalAmount).compareTo(temp.payerAmount);
                        }
                        return 1;
                    } else {
                        temp = findPayerStatement(ParseUser.getCurrentUser());
                        if(temp != null) {
                            if (another.payee == ParseUser.getCurrentUser()) {
                                return Double.valueOf(temp.payerAmount).compareTo(another.totalAmount);
                            }
                            temp2 = another.findPayerStatement(ParseUser.getCurrentUser());
                            if(temp2 != null) {
                                return Double.valueOf(temp.payerAmount).compareTo(temp2.payerAmount);
                            }
                            return 1;
                        }
                        return -1;
                    }
            }
            return 0;
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
                dialog.setCancelable(false);
                dialog.show();
            }

            @Override
            protected Boolean doInBackground(Boolean... params) {
                try {
                    if(type == REJECT || type == DELETE) {
                        for (int i = 0; i < payer.size() && type == REJECT; i++) {
                            SubStatement temp = payerList.get(i);
                            int amount = (!temp.payerReject && !temp.payerConfirm && !temp.payerPaid) ? 1 : 0;
                            ParseQuery query = ParseQuery.getQuery("Statement");
                            query.whereEqualTo("payerConfirm", false);
                            query.whereEqualTo("payerReject", false);
                            query.whereEqualTo("payerPaid", false);
                            query.whereEqualTo("friendship", temp.payerRelation);
                            if (query.count() == amount) {
                                temp.payerRelation.put("pendingStatement", false);
                                temp.payerRelation.save();
                            }
                        }
                        for (int i = 0; i < payer.size(); i++) {
                            payer.get(i).delete();
                        }
                        object.delete();
                        if(type == REJECT){ Utility.editNewEntryField(payee, "You rejected a statement, <" + description + ">"); }
                        else { Utility.editNewEntryField(payee, "You deleted a completed statement, <" + description + ">"); }
                        Utility.removeFromExistingStatementList(Statement.this);
                        return true;
                    }

                    if(type == CONFIRM){
                        payeeConfirm = true;
                        object.put("payeeConfirm", true);
                        object.save();
                        Utility.editNewEntryField(payee, "You confirmed a statement, <" + description + ">");
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

        private Statement parentStatement;
        private ParseObject object;
        private ParseObject payerRelation;
        private ParseUser payee;
        private ParseUser payer;
        String payerName;
        boolean payerConfirm, payerReject, payerPaid, paymentPending;
        double payerAmount;

        private SubStatement(Statement parent, ParseObject object, ParseUser payee, ParseUser payer, boolean payerConfirm, boolean payerReject,
                             boolean payerPaid, boolean paymentPending, double payerAmount, ParseObject payerRelation) {
            this.parentStatement = parent;
            this.object = object;
            this.payee = payee;
            this.payer = payer;
            this.payerName = Utility.getProfileName(payer);
            this.payerConfirm = payerConfirm;
            this.payerReject = payerReject;
            this.payerPaid = payerPaid;
            this.paymentPending = paymentPending;
            this.payerAmount = payerAmount;
            this.payerRelation = payerRelation;
        }

        private void notifyChange(String message_payer) {
            Utility.editNewEntryField(payer, true, message_payer);
        }

        private boolean isUserStatement(ParseUser item) {
            return payer == item;
        }

        public void setPayerConfirm(ContentActivity context) {
            PayerStatementProcess process = new PayerStatementProcess(context, CONFIRM);
            process.execute();
            payerConfirm = true;
        }

        public void setPayerReject(ContentActivity context){
            PayerStatementProcess process = new PayerStatementProcess(context, REJECT);
            process.execute();
            payerReject = true;
        }

        public void setPayerResolving() throws ParseException{
            object.put("paymentPending", true);
            object.save();
            paymentPending = true;
            Utility.removeFromExistingStatementList(parentStatement);
            Utility.editNewEntryField(payer, "You sent a resolve request for statement, <" +
                    parentStatement.description + ">, to " + Utility.getName(payee));
            Utility.editNewEntryField(payee, true, Utility.getName(payer) + " sent a resolve request for statement, <" +
                    parentStatement.description + ">,");
        }

        public void setPaymentDenied() throws ParseException{
            object.put("paymentPending", false);
            object.save();
            paymentPending = false;
            Utility.editNewEntryField(payer, true, Utility.getName(payee) + " denied your resolve request for statement, <" +
                    parentStatement.description + ">,");
            Utility.editNewEntryField(payee, "You denied the resolve request from " + Utility.getName(payer) +
                    " for statement, <" + parentStatement.description + ">,");
        }

        public void setPaymentApproved() throws ParseException{
            paymentPending = false;
            payerPaid = true;
            object.put("paymentPending", false);
            object.put("payerPaid", true);
            object.save();

            double currentBalance;
            if (payerRelation.getParseUser("userOne") == payer) {
                currentBalance = payerRelation.fetch().getDouble("owedByOne");
                currentBalance -= payerAmount;
                if(currentBalance > -0.009 && currentBalance < 0.009 ) { currentBalance = 0.00; }
                payerRelation.put("owedByOne", currentBalance);
            } else {
                currentBalance = payerRelation.fetch().getDouble("owedByTwo");
                currentBalance -= payerAmount;
                if(currentBalance > -0.009 && currentBalance < 0.009 ) { currentBalance = 0.00; }
                payerRelation.put("owedByTwo", currentBalance);
            }
            payerRelation.save();

            List<Friend> friends = Utility.generateFriendArray();
            for(int i = 0; i < friends.size(); i++){
                if(friends.get(i).isSamePerson(payer)){
                    friends.get(i).friendOwed -= payerAmount;
                    if(friends.get(i).friendOwed > -0.009 && friends.get(i).friendOwed < 0.009 ) {
                        friends.get(i).friendOwed = 0.00;
                    }
                    break;
                }
            }

            OWN_BALANCE -= payerAmount;
            if(OWN_BALANCE > -0.009 && OWN_BALANCE < 0.009 ) { OWN_BALANCE = 0.00; }
            Utility.editNewEntryField(payer, true, Utility.getName(payee) + " approved your resolve request for statement, <" +
                    parentStatement.description + ">,");
            Utility.editNewEntryField(payee, "You approved the resolve request from " + Utility.getName(payer) +
                    " for statement, <" + parentStatement.description + ">,");
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
                dialog.setCancelable(false);
                dialog.show();
            }

            @Override
            protected Boolean doInBackground(Boolean... params) {
                try {
                    if(type == CONFIRM) {
                        ParseQuery query = ParseQuery.getQuery("Statement");
                        query.whereEqualTo("payerConfirm", false);
                        query.whereEqualTo("payerReject", false);
                        query.whereEqualTo("friendship", payerRelation);
                        if (query.count() == 1) {
                            payerRelation.put("pendingStatement", false);
                        }

                        double currentBalance;
                        if (payerRelation.getParseUser("userOne") == payer) {
                            currentBalance = payerRelation.fetch().getDouble("owedByOne");
                            currentBalance += payerAmount;
                            payerRelation.put("owedByOne", currentBalance);
                        } else {
                            currentBalance = payerRelation.fetch().getDouble("owedByTwo");
                            currentBalance += payerAmount;
                            payerRelation.put("owedByTwo", currentBalance);
                        }
                        payerRelation.put("confirmed", true);
                        payerRelation.save();
                        List<Friend> friends = Utility.generateFriendArray();
                        for(int i = 0; i < friends.size(); i++){
                            if(friends.get(i).isSamePerson(payee)){
                                friends.get(i).currentUserOwed += payerAmount;
                                break;
                            }
                        }

                        object.put("payerConfirm", true);
                        object.save();
                        OWE_BALANCE -= payerAmount;
                        Utility.editNewEntryField(payee, true, Utility.getName(payer) + " confirmed the statement, <" +
                                parentStatement.description + ">, ");
                        Utility.editNewEntryField(payer, "You confirmed the statement, <" +
                                parentStatement.description + ">, ");
                        return true;
                    }

                    if(type == REJECT){
                        ParseQuery query = ParseQuery.getQuery("Statement");
                        query.whereEqualTo("payerConfirm", false);
                        query.whereEqualTo("payerReject", false);
                        query.whereEqualTo("payerPaid", false);
                        query.whereEqualTo("friendship", payerRelation);
                        if (query.count() == 1) {
                            payerRelation.put("pendingStatement", false);
                            payerRelation.save();
                        }

                        object.put("payerReject", true);
                        object.save();
                        Utility.removeFromExistingStatementList(parentStatement);
                        Utility.editNewEntryField(payee, true, Utility.getName(payer) + " rejected the statement, <" +
                                parentStatement.description + ">, ");
                        Utility.editNewEntryField(payer, "You rejected the statement, <" + parentStatement.description + ">, ");
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

package io.github.budgetninja.fairwellandroid;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;

import io.github.budgetninja.fairwellandroid.FriendObject.Friend;
import io.github.budgetninja.fairwellandroid.StatementObject.SummaryStatement;
import io.github.budgetninja.fairwellandroid.StatementObject.Statement;
import static io.github.budgetninja.fairwellandroid.ContentActivity.POSITION_HOME;
import static io.github.budgetninja.fairwellandroid.AddStatementFragment.SPLIT_EQUALLY;
import static io.github.budgetninja.fairwellandroid.AddStatementFragment.SPLIT_UNEQUALLY;
import static io.github.budgetninja.fairwellandroid.AddStatementFragment.BY_RATIO;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class SubmitStatementSummaryFragment extends Fragment {

    private View previousState;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem item = menu.findItem(R.id.action_refresh);
        item.setVisible(false);
        super.onCreateOptionsMenu(menu, inflater);
    }

    private DateFormat dateFormat;
    private ParseUser user;
    private ContentActivity parent;
    private String descriptionText, categoryText, sumbitByText;
    private Date date, deadline;
    private int modeNum, unknownNum;
    private double amountNum, runningDif;
    private Friend payee;
    private List<Pair<Friend, Double>> payer;
    private TextView descriptionView, categoryView, dateView, deadlineView, totalAmountView, modeView, sumbitByView;
    private TableLayout layout;

    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        setHasOptionsMenu(true);
        parent = (ContentActivity)getActivity();
        user = ParseUser.getCurrentUser();
        dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH);
        previousState = null;
        runningDif = 0.00;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ActionBar actionBar = parent.getSupportActionBar();
        if (actionBar != null) {
            final Drawable upArrow = ContextCompat.getDrawable(getContext(), R.drawable.abc_ic_ab_back_mtrl_am_alpha);
            upArrow.setColorFilter(ContextCompat.getColor(getContext(), R.color.coolBackground), PorterDuff.Mode.SRC_ATOP);
            actionBar.setHomeAsUpIndicator(upArrow);
        }
        parent.setTitle("Summary");
        if(previousState != null){
            return previousState;
        }

        View view = inflater.inflate(R.layout.fragment_statement_summary, container, false);
        descriptionView = (TextView) view.findViewById(R.id.summary_description);
        categoryView = (TextView) view.findViewById(R.id.summary_category);
        dateView = (TextView) view.findViewById(R.id.summary_date);
        deadlineView = (TextView) view.findViewById(R.id.summary_deadline);
        totalAmountView = (TextView) view.findViewById(R.id.summary_totalAmount);
        modeView = (TextView) view.findViewById(R.id.summary_mode);
        sumbitByView = (TextView) view.findViewById(R.id.summary_submitBy);
        layout = (TableLayout) view.findViewById(R.id.summary_tableLayout);
        Button cancelButton = (Button) view.findViewById(R.id.summary_cancelButton);
        Button modifyButton = (Button) view.findViewById(R.id.summary_modifyButton);
        Button submitButton = (Button) view.findViewById(R.id.summary_submitButton);
        Button deleteButton = (Button) view.findViewById(R.id.summary_deleteButton);
        deleteButton.setVisibility(View.GONE);
        Button confirmButton = (Button) view.findViewById(R.id.summary_confirmButton);
        confirmButton.setVisibility(View.GONE);
        Button rejectButton = (Button) view.findViewById(R.id.summary_rejectButton);
        rejectButton.setVisibility(View.GONE);
        LinearLayout paymentOptionLayout = (LinearLayout) view.findViewById(R.id.resolve_Option_layout);
        paymentOptionLayout.setVisibility(View.GONE);

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.layoutManage(POSITION_HOME);
            }
        });

        modifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.mMenuDrawer.closeMenu(false);
                parent.fragMgr.popBackStack();
            }
        });

        submitButton.setOnClickListener(submitListener);
        previousState = view;
        return view;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                parent.mMenuDrawer.closeMenu(false);
                parent.fragMgr.popBackStack();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void setData(SummaryStatement data){
        descriptionText = data.description;
        categoryText = data.category;
        date = data.date;
        deadline = data.deadline;
        sumbitByText = Utility.getName(user);
        modeNum = data.mode;
        unknownNum = data.unknown;
        amountNum = data.totalAmount;
        this.payee = data.payee;
        this.payer = data.payer;
        displayData();
    }

    private void displayData(){
        descriptionView.setText(descriptionText);
        categoryView.setText(categoryText);
        dateView.setText(dateFormat.format(date));
        deadlineView.setText(dateFormat.format(deadline));
        totalAmountView.setText("$ " + String.format("%.2f", this.amountNum));
        modeView.setText(Integer.toString(modeNum));
        sumbitByView.setText("YOU");

        String payeeName;
        if(this.payee == null){ payeeName = "YOU"; }
        else { payeeName = this.payee.name; }

        TableRow memberRow;
        TextView payee, payer, amount;
        runningDif = this.amountNum;
        switch (modeNum){
            case SPLIT_EQUALLY:
                modeView.setText("Split Equally");

                for(int i = 0; i < this.payer.size(); i++){
                    String payerName = this.payer.get(i).first.name;
                    memberRow = new TableRow(parent);
                    memberRow.setPadding(0, 0, 0,Utility.getPixel(2, getResources()));

                    payee = new TextView(parent);
                    payee.setGravity(Gravity.CENTER);
                    payee.setText(payeeName);

                    payer = new TextView(parent);
                    payer.setGravity(Gravity.CENTER);
                    payer.setText(payerName.equals("Self") ? "YOU" : payerName);

                    amount = new TextView(parent);
                    amount.setGravity(Gravity.CENTER);
                    amount.setText("$ " + String.format("%.2f", this.payer.get(i).second));
                    runningDif -= this.payer.get(i).second;

                    memberRow.addView(payer, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    memberRow.addView(payee, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    memberRow.addView(amount, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    layout.addView(memberRow);
                }
                if(unknownNum > 0 && runningDif >= 0.01){
                    String entry;
                    if(unknownNum == 1) { entry = "(1 non-user)"; }
                    else{ entry = "(" + Integer.toString(unknownNum) + " non-users)"; }

                    memberRow = new TableRow(parent);
                    memberRow.setPadding(0, 0, 0, Utility.getPixel(2, getResources()));

                    payee = new TextView(parent);
                    payee.setGravity(Gravity.CENTER);
                    payee.setText(payeeName);

                    payer = new TextView(parent);
                    payer.setGravity(Gravity.CENTER);
                    payer.setText(entry);

                    amount = new TextView(parent);
                    amount.setGravity(Gravity.CENTER);
                    amount.setText("$ " + String.format("%.2f", runningDif));

                    memberRow.addView(payer, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    memberRow.addView(payee, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    memberRow.addView(amount, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    layout.addView(memberRow);
                }
                break;

            case SPLIT_UNEQUALLY:
                modeView.setText("Split Unequally");
                Toast.makeText(parent, "Coming Soon! - Use Split Equally", Toast.LENGTH_SHORT).show();
                break;

            case BY_RATIO:
                modeView.setText("Split by Ratio");
                Toast.makeText(parent, "Coming Soon! - Use Split Equally", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private View.OnClickListener submitListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            SubmitStatementLoading submitStatementLoading = new SubmitStatementLoading(parent);
            submitStatementLoading.execute();
        }
    };


    private class SubmitStatementLoading extends AsyncTask<Boolean, Void, Boolean> {
        private ProgressDialog dialog;

        public SubmitStatementLoading(Context activity) {
            dialog = new ProgressDialog(activity);
        }

        @Override
        protected void onPreExecute() {
            dialog.setMessage("Submitting Statement... Please Wait...");
            dialog.show();
        }

        @Override
        protected Boolean doInBackground(Boolean... params) {
            try {
                ParseObject object = new ParseObject("StatementGroup");
                Pair<Boolean, Boolean> isCurrentUserInvolved = new Pair<>(false, false);

                if (payee == null) {              //payee == current user
                    object.put("payee", user);
                    object.put("payeeConfirm", true);
                    isCurrentUserInvolved = new Pair<>(true, true);
                } else {
                    object = payee.insertParseUser(object, "payee");
                    payee.notifyChange();
                    object.put("payeeConfirm", false);
                }
                object.put("description", descriptionText);
                object.put("category", categoryText);
                object.put("paymentAmount", amountNum);
                object.put("submittedBy", sumbitByText);
                object.put("mode", modeNum);
                object.put("date", date);
                object.put("deadline", deadline);
                object.put("unknown", unknownNum);
                object.put("unknownAmount", runningDif);

                ArrayList<ParseObject> statementArray = new ArrayList<>();
                for (int i = 0; i < payer.size(); i++) {
                    Pair<Friend, Double> item = payer.get(i);
                    if (item.first.name.equals("Self") && payee == null) {       //The two cases when payer == payee
                        continue;
                    }
                    if (!item.first.name.equals("Self") && payee != null) {
                        if (item.first.isEqual(payee)) {
                            continue;
                        }
                    }

                    ParseObject statementObject = new ParseObject("Statement");
                    if (item.first.name.equals("Self") && payee != null) {       //The case when payee is someone else and payer is current user
                        statementObject = payee.insertFriendship(statementObject, "friendship");
                        payee.setPendingStatement();
                        statementObject.put("payer", user);
                        isCurrentUserInvolved = new Pair<>(true, false);
                    } else {
                        if (payee != null) {                                    //The case when payee is someone else
                            ParseObject temp = payee.generateFriendToFriendRelationship(item.first);
                            temp.put("pendingStatement", true);
                            temp.save();
                            statementObject.put("friendship", temp);
                        } else {                                                //The case when payee is current user
                            statementObject = item.first.insertFriendship(statementObject, "friendship");
                            item.first.setPendingStatement();
                        }
                        statementObject = item.first.insertParseUser(statementObject, "payer");
                        item.first.notifyChange();
                    }
                    statementObject.put("payerConfirm", false);
                    statementObject.put("payerReject", false);
                    statementObject.put("payerPaid", false);
                    statementObject.put("paymentPending", false);
                    statementObject.put("amount", item.second);
                    statementObject.save();
                    statementArray.add(statementObject);
                }

                SystemClock.sleep(2000);                        //Attempt to resolve the following issue

                object.put("payer", statementArray);
                ParseObject objectCopy = object;
                object.save();                               //For unknown reason, sometime this line causes a ConcurrentModificationException
                if (isCurrentUserInvolved.first) {
                    Statement statement = new Statement(objectCopy, isCurrentUserInvolved.second);
                    ParseObject temp = Utility.getRawListLocation();
                    temp.getList("statementList").add(objectCopy);
                    temp.pinInBackground();
                    Utility.addToExistingStatementList(statement);
                }
                return true;
            } catch (ParseException|ConcurrentModificationException e){
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            parent.layoutManage(POSITION_HOME);
            if(result) {
                Toast.makeText(parent, "Statement Submitted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(parent, "Submission Failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

}

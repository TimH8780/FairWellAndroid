package io.github.budgetninja.fairwellandroid;

import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import io.github.budgetninja.fairwellandroid.FriendObject.Friend;
import io.github.budgetninja.fairwellandroid.StatementObject.SummaryStatement;
import io.github.budgetninja.fairwellandroid.StatementObject.Statement;
import static io.github.budgetninja.fairwellandroid.ContentActivity.POSITION_HOME;
import static io.github.budgetninja.fairwellandroid.AddStatementFragment.SPLIT_EQUALLY;
import static io.github.budgetninja.fairwellandroid.AddStatementFragment.BY_PERCENTAGE;
import static io.github.budgetninja.fairwellandroid.AddStatementFragment.BY_RATIO;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SubmitStatementSummaryFragment extends Fragment {

    private View previousState;
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
    private ParseObject object;
    private Pair<Boolean, Boolean> isCurrentUserInvolved;

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
            actionBar.setHomeAsUpIndicator(null);
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
        Button confirmButton = (Button) view.findViewById(R.id.summary_confirmButton);
        confirmButton.setVisibility(View.GONE);
        Button rejectButton = (Button) view.findViewById(R.id.summary_rejectButton);
        rejectButton.setVisibility(View.GONE);

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
        sumbitByText = Utility.getUserName(user);
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
        sumbitByView.setText(sumbitByText);
        String payeeName;
        if(this.payee == null){ payeeName = Utility.getUserName(ParseUser.getCurrentUser()); }
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
                    payer.setText(payerName.equals("Self") ? Utility.getUserName(user) : payerName);

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
                    String entry = "(" + Integer.toString(unknownNum) + " Unknown)";
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

            case BY_PERCENTAGE:
                modeView.setText("By Percentage");
                Toast.makeText(parent, "Coming Soon! - Use Split Equally", Toast.LENGTH_SHORT).show();
                break;

            case BY_RATIO:
                modeView.setText("By Ratio");
                Toast.makeText(parent, "Coming Soon! - Use Split Equally", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private View.OnClickListener submitListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            object = new ParseObject("StatementGroup");
            isCurrentUserInvolved = new Pair<>(false, false);    //First = isInvolved, Second = isPayee

            if(payee == null){      //payee == current user
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
            for(int i = 0; i < payer.size(); i++){
                Pair<Friend, Double> item = payer.get(i);
                if(item.first.name.equals("Self") && payee == null) {       //The two cases when payer == payee
                    continue;
                }
                if(!item.first.name.equals("Self") && payee != null) {
                    if(item.first.isEqual(payee)) { continue; }
                }

                ParseObject statementObject = new ParseObject("Statement");
                if(item.first.name.equals("Self") && payee != null) {       //The case when payee is someone else and payer is current user
                    statementObject = payee.insertFriendship(statementObject, "friendship");
                    payee.setPendingStatement();
                    statementObject.put("payer", user);
                    statementObject.put("payerConfirm", false);
                    statementObject.put("amount", item.second);
                    statementArray.add(statementObject);
                    isCurrentUserInvolved = new Pair<>(true, false);
                } else {
                    if (payee != null) {                                    //The case when payee is someone else
                        ParseObject temp = payee.generateFriendToFriendRelationship(item.first);
                        temp.put("pendingStatement", true);
                        temp.saveInBackground();
                        statementObject.put("friendship", temp);
                    } else {                                                //The case when payee is current user
                        statementObject = item.first.insertFriendship(statementObject, "friendship");
                        item.first.setPendingStatement();
                    }
                    statementObject = item.first.insertParseUser(statementObject, "payer");
                    item.first.notifyChange();
                    statementObject.put("payerConfirm", false);
                    statementObject.put("amount", item.second);
                    statementArray.add(statementObject);
                }
            }

            object.put("payer", statementArray);
            object.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    if(e == null){
                        Toast.makeText(parent, "Submitted", Toast.LENGTH_SHORT).show();
                        parent.layoutManage(POSITION_HOME);
                        if(isCurrentUserInvolved.first){
                            Statement statement = new Statement(object, isCurrentUserInvolved.second);
                            ParseObject temp = Utility.getRawListLocation();
                            temp.getList("statementList").add(object);
                            temp.pinInBackground();
                            Utility.addToExistingStatementList(statement);
                        }
                    } else {
                        Toast.makeText(parent, "Submission Failed", Toast.LENGTH_SHORT).show();
                        Log.d("Statement", e.toString());
                    }
                }
            });
        }
    };

}

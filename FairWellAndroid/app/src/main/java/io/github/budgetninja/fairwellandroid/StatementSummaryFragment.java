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

public class StatementSummaryFragment extends Fragment {

    private View previousState;
    private DateFormat dateFormat;
    private ParseUser user;
    private ContentActivity parent;
    private String descriptionText, categoryText, dateText, deadlineText, sumbitByText;
    private Date date, deadline;
    private int mode, unknown;
    private double amount, runningDif;
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
        dateText = dateFormat.format(date);
        deadline = data.deadline;
        deadlineText = dateFormat.format(deadline);
        sumbitByText = data.submitBy;
        mode = data.mode;
        unknown = data.unknown;
        amount = data.totalAmount;
        this.payee = data.payee;
        this.payer = data.payer;
        displayData();
    }

    private void displayData(){
        descriptionView.setText(descriptionText);
        categoryView.setText(categoryText);
        dateView.setText(dateText);
        deadlineView.setText(deadlineText);
        totalAmountView.setText("$ " + String.format("%.2f", this.amount));
        modeView.setText(Integer.toString(mode));
        sumbitByView.setText(sumbitByText);
        String payeeName;
        if(this.payee == null){ payeeName = Utility.getUserName(ParseUser.getCurrentUser()); }
        else { payeeName = this.payee.name; }

        TableRow memberRow;
        TextView payee, payer, amount;
        runningDif = this.amount;
        switch (mode){
            case SPLIT_EQUALLY:
                modeView.setText("Split Equally");

                for(int i = 0; i < this.payer.size(); i++){
                    String payerName = this.payer.get(i).first.name;
                    memberRow = new TableRow(parent);
                    memberRow.setPadding(0, 0, 0, getPixel(2));

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
                if(unknown > 0 && runningDif > 0.00){
                    String entry = "(" + Integer.toString(unknown) + " Unknown)";
                    memberRow = new TableRow(parent);
                    memberRow.setPadding(0, 0, 0, getPixel(2));

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

    private int getPixel(int desiredDp){
        float scale = getResources().getDisplayMetrics().density;
        return  (int) (desiredDp * scale + 0.5f);
    }

    private View.OnClickListener submitListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ParseObject object = new ParseObject("StatementGroup");

            if(payee == null){      //submitter == payee
                object.put("payee", user);
                object.put("payeeConfirm", true);
            } else {
                object = payee.insertParseUser(object, "payee");
                payee.notifyChange();
                object.put("payeeConfirm", false);
            }
            object.put("description", descriptionText);
            object.put("category", categoryText);
            object.put("paymentAmount", amount);
            object.put("submittedBy", sumbitByText);
            object.put("mode", mode);
            object.put("date", date);
            object.put("deadline", deadline);
            object.put("unknown", unknown);
            object.put("unknownAmount", runningDif);

            ArrayList<ParseObject> statementArray = new ArrayList<>();
            for(int i = 0; i < payer.size(); i++){
                Pair<Friend, Double> item = payer.get(i);           //the Friend object of payer i
                if(item.first.name.equals("Self") && payee == null) {
                    continue;
                }
                else if(!item.first.name.equals("Self") && payee != null) {
                    if(item.first.isEqual(payee)) { continue; }
                }

                ParseObject statementObject = new ParseObject("Statement");
                if(item.first.name.equals("Self") && payee != null) {
                    statementObject = payee.insertFriendship(statementObject, "friendship");
                    payee.setPendingStatement();
                    Utility.editNewEntryField(user, true);
                    statementObject.put("payer", user);
                    statementObject.put("payerConfirm", false);
                    statementObject.put("amount", item.second);
                    statementArray.add(statementObject);
                } else {
                    if (payee != null) {
                        ParseObject temp = payee.generateFriendToFriendRelationship(item.first);
                        statementObject.put("friendship", temp);
                    } else {
                        statementObject = item.first.insertFriendship(statementObject, "friendship");
                    }
                    statementObject = item.first.insertParseUser(statementObject, "payer");
                    item.first.notifyChange();
                    item.first.setPendingStatement();
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
                    } else {
                        Toast.makeText(parent, "Submission Failed", Toast.LENGTH_SHORT).show();
                        Log.d("Statement", e.toString());
                    }
                }
            });
        }
    };

}

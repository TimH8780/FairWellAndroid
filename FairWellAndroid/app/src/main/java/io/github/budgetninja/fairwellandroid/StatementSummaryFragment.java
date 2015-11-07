package io.github.budgetninja.fairwellandroid;

import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
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

import com.parse.ParseUser;

import io.github.budgetninja.fairwellandroid.FriendObject.Friend;
import static io.github.budgetninja.fairwellandroid.ContentActivity.POSITION_HOME;
import static io.github.budgetninja.fairwellandroid.AddStatementFragment.SPLIT_EQUALLY;
import static io.github.budgetninja.fairwellandroid.AddStatementFragment.BY_PERCENTAGE;
import static io.github.budgetninja.fairwellandroid.AddStatementFragment.BY_RATIO;

import java.util.List;

public class StatementSummaryFragment extends Fragment {

    private ContentActivity parent;
    private String description, category, date, deadline, amount;
    private int mode, totalPeople;
    private Friend payee;
    private List<Friend> payer;
    private TextView descriptionText, categoryText, dateText, deadlineText, totalAmountTotal, modeText, sumbitByText;
    private TableLayout layout;

    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        setHasOptionsMenu(true);
        parent = (ContentActivity)getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statement_summary, container, false);
        ActionBar actionBar = parent.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(null);
        }
        parent.setTitle("Summary");

        descriptionText = (TextView) view.findViewById(R.id.summary_description);
        categoryText = (TextView) view.findViewById(R.id.summary_category);
        dateText = (TextView) view.findViewById(R.id.summary_date);
        deadlineText = (TextView) view.findViewById(R.id.summary_deadline);
        totalAmountTotal = (TextView) view.findViewById(R.id.summary_totalAmount);
        modeText = (TextView) view.findViewById(R.id.summary_mode);
        sumbitByText = (TextView) view.findViewById(R.id.summary_submitBy);
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
                Toast.makeText(parent, "Coming Soon!", Toast.LENGTH_SHORT).show();
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(parent, "Coming Soon!", Toast.LENGTH_SHORT).show();
            }
        });

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

    public void setData(String description, String category, String date, String deadline,  int mode, int totalPeople,
                   String amount, FriendObject.Friend payee, List<FriendObject.Friend> payer){
        this.description = description;
        this.category = category;
        this.date = date;
        this.deadline = deadline;
        this.mode = mode;
        this.totalPeople = totalPeople;
        this.amount = amount;
        this.payee = payee;
        this.payer = payer;
        displayData();
    }

    private void displayData(){
        descriptionText.setText(description);
        categoryText.setText(category);
        dateText.setText(date);
        deadlineText.setText(deadline);
        totalAmountTotal.setText("$ " + String.format("%.2f", Double.valueOf(this.amount)));
        modeText.setText(category);
        sumbitByText.setText(Utility.getUserName(ParseUser.getCurrentUser()));
        String payeeName;
        if(this.payee == null){ payeeName = Utility.getUserName(ParseUser.getCurrentUser()); }
        else { payeeName = this.payee.name; }

        switch (mode){
            case SPLIT_EQUALLY:
                modeText.setText("Split Equally");
                Double each = Double.valueOf(this.amount) / totalPeople;

                TableRow memberRow;
                TextView payee, payer, amount;
                for(int i = 0; i < this.payer.size(); i++){
                    memberRow = new TableRow(parent);
                    memberRow.setPadding(0, 0, 0, getPixel(2));

                    payee = new TextView(parent);
                    payee.setGravity(Gravity.CENTER);
                    payee.setText(payeeName);

                    payer = new TextView(parent);
                    payer.setGravity(Gravity.CENTER);
                    payer.setText(this.payer.get(i).name.equals("Self") ? Utility.getUserName(ParseUser.getCurrentUser()) : this.payer.get(i).name);

                    amount = new TextView(parent);
                    amount.setGravity(Gravity.CENTER);
                    amount.setText("$ " + String.format("%.2f", each));

                    memberRow.addView(payer, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    memberRow.addView(payee, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    memberRow.addView(amount, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    layout.addView(memberRow);
                }
                if(totalPeople > this.payer.size()){
                    int diff = totalPeople - this.payer.size();
                    String entry = "(" + Integer.toString(diff) + " Unknown)";
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
                    amount.setText("$ " + String.format("%.2f", each * diff));

                    memberRow.addView(payer, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    memberRow.addView(payee, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    memberRow.addView(amount, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    layout.addView(memberRow);
                }
                break;

            case BY_PERCENTAGE:
                modeText.setText("By Percentage");
                Toast.makeText(parent, "Coming Soon! - Use Split Equally", Toast.LENGTH_SHORT).show();
                break;

            case BY_RATIO:
                modeText.setText("By Ratio");
                Toast.makeText(parent, "Coming Soon! - Use Split Equally", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private int getPixel(int desiredDp){
        float scale = getResources().getDisplayMetrics().density;
        return  (int) (desiredDp * scale + 0.5f);
    }

}

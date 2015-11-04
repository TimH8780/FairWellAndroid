package io.github.budgetninja.fairwellandroid;

import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseUser;

import io.github.budgetninja.fairwellandroid.FriendObject.Friend;
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
    private LayoutInflater inflater;
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
        this.inflater = inflater;

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
        Double amountValue = Double.valueOf(this.amount);
        totalAmountTotal.setText("$ " + String.format("%.2f", amountValue));
        modeText.setText(category);
        sumbitByText.setText(Utility.getUserName(ParseUser.getCurrentUser()));

        switch (mode){
            case SPLIT_EQUALLY:
                modeText.setText("Split Equally");

                Double each = Double.valueOf(this.amount) / totalPeople;
                String payeeName;
                if(this.payee == null){ payeeName = Utility.getUserName(ParseUser.getCurrentUser()); }
                else { payeeName = this.payee.name; }

                for(int i = 0; i < this.payer.size(); i++){
                    TableRow memberRow = new TableRow(parent);
                    inflater.inflate(R.layout.list_row_statement_summary, memberRow);
                    TextView payee = (TextView) memberRow.findViewById(R.id.summary_payee);
                    TextView payer = (TextView) memberRow.findViewById(R.id.summary_payer);
                    TextView amount = (TextView) memberRow.findViewById(R.id.summary_amount);
                    payee.setText(payeeName);
                    payer.setText(this.payer.get(i).name);
                    amount.setText("$ " + String.format("%.2f", each));
                    layout.addView(memberRow);
                }
                if(totalPeople > this.payer.size()){
                    int diff = totalPeople - this.payer.size();
                    String entry = "(" + Integer.toString(diff) + "Unknown)";
                    TableRow memberRow = new TableRow(parent);
                    inflater.inflate(R.layout.list_row_statement_summary, memberRow);
                    TextView payee = (TextView) memberRow.findViewById(R.id.summary_payee);
                    TextView payer = (TextView) memberRow.findViewById(R.id.summary_payer);
                    TextView amount = (TextView) memberRow.findViewById(R.id.summary_amount);
                    payee.setText(payeeName);
                    payer.setText(entry);
                    amount.setText("$ " + String.format("%.2f", each * diff));
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

}

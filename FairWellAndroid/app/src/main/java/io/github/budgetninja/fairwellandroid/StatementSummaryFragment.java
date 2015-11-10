package io.github.budgetninja.fairwellandroid;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import io.github.budgetninja.fairwellandroid.StatementObject.Statement;
import io.github.budgetninja.fairwellandroid.StatementObject.SubStatement;
import static io.github.budgetninja.fairwellandroid.AddStatementFragment.SPLIT_EQUALLY;
import static io.github.budgetninja.fairwellandroid.AddStatementFragment.BY_PERCENTAGE;
import static io.github.budgetninja.fairwellandroid.AddStatementFragment.BY_RATIO;

/**
 *Created by Tim on 11/09/15.
 */
public class StatementSummaryFragment extends Fragment{

    private TextView descriptionView, categoryView, dateView, deadlineView, totalAmountView, modeView, sumbitByView;
    private TextView payeeField, amountField;
    private Button confirmButton;
    private TableLayout layout;
    private DateFormat dateFormat;
    private Statement data;
    private ParseUser user;
    private ContentActivity parent;
    private View previousState;

    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        setHasOptionsMenu(true);
        parent = (ContentActivity)getActivity();
        dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH);
        user = ParseUser.getCurrentUser();
        previousState = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ActionBar actionBar = parent.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(null);
        }
        parent.setTitle("Summary");
        if (previousState != null) {
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
        payeeField = (TextView) view.findViewById(R.id.summary_payee_subtitle);
        amountField = (TextView) view.findViewById(R.id.summary_amount_subtitle);
        layout = (TableLayout) view.findViewById(R.id.summary_tableLayout);
        confirmButton = (Button) view.findViewById(R.id.summary_confirmButton);
        Button cancelButton = (Button) view.findViewById(R.id.summary_cancelButton);
        cancelButton.setVisibility(View.GONE);
        Button modifyButton = (Button) view.findViewById(R.id.summary_modifyButton);
        modifyButton.setVisibility(View.GONE);
        Button submitButton = (Button) view.findViewById(R.id.summary_submitButton);
        submitButton.setVisibility(View.GONE);

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

    public void setData(Statement data){
        this.data = data;
        displayData();
    }

    private void displayData(){
        descriptionView.setText(data.description);
        categoryView.setText(data.category);
        dateView.setText(dateFormat.format(data.date));
        deadlineView.setText(dateFormat.format(data.deadline));
        totalAmountView.setText("$ " + String.format("%.2f", data.totalAmount));
        switch (data.mode){
            case SPLIT_EQUALLY:
                modeView.setText("Split Equally");
                break;
            case BY_PERCENTAGE:
                modeView.setText("By Percentage");
                break;
            case BY_RATIO:
                modeView.setText("By Ratio");
                break;
        }
        sumbitByView.setText(data.submitBy);
        if(data.isPayee){
            displayDataPayee();
        } else {
            displayDataPayer();
        }
    }

    private void displayDataPayer(){
        final SubStatement subStatement = data.findPayerStatement(user);
        TableRow memberRow = new TableRow(parent);
        memberRow.setPadding(0, 0, 0, Utility.getPixel(2, getResources()));

        TextView payee = new TextView(parent);
        payee.setGravity(Gravity.CENTER);
        payee.setText(subStatement.payerName);

        TextView payer = new TextView(parent);
        payer.setGravity(Gravity.CENTER);
        payer.setText(Utility.getUserName(data.payee));

        TextView amount = new TextView(parent);
        amount.setGravity(Gravity.CENTER);
        amount.setText("$ " + String.format("%.2f", subStatement.payerAmount));

        memberRow.addView(payer, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        memberRow.addView(payee, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        memberRow.addView(amount, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layout.addView(memberRow);

        if(subStatement.payerConfirm){
            confirmButton.setVisibility(View.GONE);
        } else {
            confirmButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(parent, "Processing...", Toast.LENGTH_SHORT).show();
                    subStatement.setPayerConfirm(parent);
                    confirmButton.setVisibility(View.GONE);
                    parent.fragMgr.popBackStack();
                }
            });
        }
    }

    private void displayDataPayee(){
        payeeField.setText("Amount");
        amountField.setText("Status");
        TableRow memberRow;
        TextView payer, amount, status;
        for(int i = 0; i < data.payerList.size(); i++){
            SubStatement item = data.payerList.get(i);
            memberRow = new TableRow(parent);
            memberRow.setPadding(0, 0, 0, Utility.getPixel(2, getResources()));

            payer = new TextView(parent);
            payer.setGravity(Gravity.CENTER);
            payer.setText(item.payerName);

            amount = new TextView(parent);
            amount.setGravity(Gravity.CENTER);
            amount.setText("$ " + String.format("%.2f", item.payerAmount));

            status = new TextView(parent);
            if(!item.payerConfirm){
                status.setGravity(Gravity.CENTER);
                status.setText("Pending");
            }

            memberRow.addView(payer, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            memberRow.addView(amount, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            memberRow.addView(status, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layout.addView(memberRow);
        }

        if(data.unknown > 0){
            memberRow = new TableRow(parent);
            memberRow.setPadding(0, 0, 0, Utility.getPixel(2, getResources()));

            payer = new TextView(parent);
            payer.setGravity(Gravity.CENTER);
            payer.setText("(" + Integer.toString(data.unknown) + " Unknown)");

            amount = new TextView(parent);
            amount.setGravity(Gravity.CENTER);
            amount.setText("$ " + String.format("%.2f", data.unknownAmount));

            status = new TextView(parent);
            status.setGravity(Gravity.CENTER);
            status.setText("N/A");

            memberRow.addView(payer, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            memberRow.addView(amount, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            memberRow.addView(status, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layout.addView(memberRow);
        }

        if(data.payeeConfirm){
            confirmButton.setVisibility(View.GONE);
        } else {
            confirmButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    data.setPayeeConfirm();
                    confirmButton.setVisibility(View.GONE);
                    parent.fragMgr.popBackStack();
                }
            });
        }
    }
}

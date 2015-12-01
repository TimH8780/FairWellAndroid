package io.github.budgetninja.fairwellandroid;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.ParseUser;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.github.budgetninja.fairwellandroid.StatementObject.Statement;
import io.github.budgetninja.fairwellandroid.StatementObject.SubStatement;
import static io.github.budgetninja.fairwellandroid.AddStatementFragment.SPLIT_EQUALLY;
import static io.github.budgetninja.fairwellandroid.AddStatementFragment.SPLIT_UNEQUALLY;
import static io.github.budgetninja.fairwellandroid.AddStatementFragment.SPLIT_BY_RATIO;
import static io.github.budgetninja.fairwellandroid.StatementObject.CONFIRM;
import static io.github.budgetninja.fairwellandroid.StatementObject.REJECT;
import static io.github.budgetninja.fairwellandroid.StatementObject.DELETE;

/**
 *Created by Tim on 11/09/15.
 */
public class StatementSummaryFragment extends Fragment{

    private TextView descriptionView, categoryView, dateView, deadlineView, totalAmountView, modeView, sumbitByView;
    private TextView payeeField, amountField;
    private LinearLayout paymentOptionLayout;
    private Button confirmButton, rejectButton, deleteButton, confirmPaymentButton, denyPaymentButton;
    private TableLayout layout;
    private DateFormat dateFormat;
    private Statement data;
    private ParseUser user;
    private ContentActivity parent;
    private Boolean[] tempResult;
    private View previousState;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem item = menu.findItem(R.id.action_refresh);
        item.setVisible(false);
        super.onCreateOptionsMenu(menu, inflater);
    }

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
            final Drawable upArrow = ContextCompat.getDrawable(getContext(), R.drawable.abc_ic_ab_back_mtrl_am_alpha);
            upArrow.setColorFilter(ContextCompat.getColor(getContext(), R.color.coolBackground), PorterDuff.Mode.SRC_ATOP);
            actionBar.setHomeAsUpIndicator(upArrow);
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
        paymentOptionLayout = (LinearLayout) view.findViewById(R.id.resolve_Option_layout);
        confirmPaymentButton = (Button) view.findViewById(R.id.confirmPendingPaymentButton);
        denyPaymentButton = (Button) view.findViewById(R.id.denyPendingPaymentButton);
        confirmButton = (Button) view.findViewById(R.id.summary_confirmButton);
        rejectButton = (Button) view.findViewById(R.id.summary_rejectButton);
        deleteButton = (Button) view.findViewById(R.id.summary_deleteButton);

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
            case SPLIT_UNEQUALLY:
                modeView.setText("Split Unequally");
                break;
            case SPLIT_BY_RATIO:
                modeView.setText("Split by Ratio");
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
        payer.setText("YOU");

        TextView amount = new TextView(parent);
        amount.setGravity(Gravity.CENTER);
        amount.setText("$ " + String.format("%.2f", subStatement.payerAmount));

        memberRow.addView(payer, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        memberRow.addView(payee, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        memberRow.addView(amount, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layout.addView(memberRow);
        deleteButton.setVisibility(View.GONE);
        paymentOptionLayout.setVisibility(View.GONE);

        if(subStatement.payerConfirm){
            confirmButton.setVisibility(View.GONE);
            rejectButton.setVisibility(View.GONE);
        } else {
            confirmButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    subStatement.setPayerConfirm(parent);
                    confirmButton.setVisibility(View.GONE);
                    rejectButton.setVisibility(View.GONE);
                }
            });

            rejectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    subStatement.setPayerReject(parent);
                }
            });
        }
    }

    private void displayDataPayee(){
        payeeField.setText("Amount");
        amountField.setText("Status");
        TableRow memberRow;
        TextView payer, amount, status;
        boolean deletable = true;
        boolean pendingPayment = false;

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
            status.setGravity(Gravity.CENTER);
            if(item.payerConfirm){
                if(item.payerPaid){
                    status.setText("Paid");
                } else if(item.paymentPending){
                    status.setText("Resolving");
                    deletable = false;
                    pendingPayment = true;
                } else {
                    status.setText("Confirmed");
                    deletable = false;
                }
            } else if(item.payerReject){
                status.setText("Denied");
            } else {
                status.setText("Pending");
                deletable = false;
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
            if(data.unknown == 1){
                payer.setText("(1 non-user)");
            } else {
                payer.setText("(" + Integer.toString(data.unknown) + " non-users)");
            }

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
            rejectButton.setVisibility(View.GONE);
            if(!deletable){
                deleteButton.setVisibility(View.GONE);
            } else {
                deleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Statement.PayeeStatementProcess task = data.new PayeeStatementProcess(parent, DELETE);
                        task.execute();
                    }
                });
            }
            if(!pendingPayment){
                paymentOptionLayout.setVisibility(View.GONE);
            } else {
                confirmPaymentButton.setOnClickListener(new paymentOption(CONFIRM));
                denyPaymentButton.setOnClickListener(new paymentOption(REJECT));
            }
        } else {
            deleteButton.setVisibility(View.GONE);
            paymentOptionLayout.setVisibility(View.GONE);
            confirmButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    data.setPayeeConfirm(parent);
                    confirmButton.setVisibility(View.GONE);
                    rejectButton.setVisibility(View.GONE);
                }
            });

            rejectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Statement.PayeeStatementProcess task = data.new PayeeStatementProcess(parent, REJECT);
                    task.execute();
                }
            });
        }
    }

    private class paymentOption implements View.OnClickListener{

        private int type;

        public paymentOption(int type){
            this.type = type;
        }

        @Override
        public void onClick(View v) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(parent);
            final ListView container = new ListView(parent);
            final List<SubStatement> list = new ArrayList<>();
            for(int i = 0; i < data.payerList.size(); i++){
                if(data.payerList.get(i).paymentPending){
                    list.add(data.payerList.get(i));
                }
            }
            tempResult = new Boolean[list.size()];

            PayerSelectionAdaptor adaptor = new PayerSelectionAdaptor(parent, R.layout.item_add_member_one, list);
            container.setAdapter(adaptor);
            container.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    CheckBox checkBox = (CheckBox) view.findViewById(R.id.memberCheckBox);
                    if (checkBox.isChecked()) {
                        checkBox.setChecked(false);
                        tempResult[position] = false;
                    } else {
                        checkBox.setChecked(true);
                        tempResult[position] = true;
                    }
                }
            });
            builder.setView(container);
            if(type == CONFIRM){
                builder.setTitle("Confirm Pending Payment");
                builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PendingPaymentProcess task = new PendingPaymentProcess(parent, tempResult, list, CONFIRM);
                        task.execute();
                    }
                });
            } else {
                builder.setTitle("Deny Pending Payment");
                builder.setPositiveButton("Deny", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PendingPaymentProcess task = new PendingPaymentProcess(parent, tempResult, list, REJECT);
                        task.execute();
                    }
                });
            }
            builder.setNegativeButton("Cancel", null);
            final AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    private class PayerSelectionAdaptor extends ArrayAdapter<SubStatement> {

        Context mContext;
        int mResource;
        List<SubStatement> mObject;

        public PayerSelectionAdaptor(Context context, int resource, List<SubStatement> objects){
            super(context, resource, objects);
            mContext = context;
            mResource = resource;
            mObject = objects;
        }

        private class ViewHolder{
            TextView nameText;
            CheckBox box;
            int position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parentGroup){
            SubStatement currentItem = mObject.get(position);
            final ViewHolder viewHolder;
            if(convertView == null){
                convertView = parent.getLayoutInflater().inflate(mResource, parentGroup, false);
                viewHolder = new ViewHolder();
                viewHolder.nameText = (TextView) convertView.findViewById(R.id.memberName);
                viewHolder.box = (CheckBox) convertView.findViewById(R.id.memberCheckBox);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            viewHolder.position = position;
            viewHolder.nameText.setText(currentItem.payerName);
            if(tempResult[position] == null){
                viewHolder.box.setChecked(false);
            } else {
                viewHolder.box.setChecked(tempResult[position]);
            }

            return convertView;
        }
    }

    private class PendingPaymentProcess extends AsyncTask<Boolean, Void, Boolean> {
        private ProgressDialog dialog;
        private Context activity;
        private Boolean[] result;
        private List<SubStatement> list;
        private int type;

        public PendingPaymentProcess(Context activity, Boolean[] result, List<SubStatement> list, int type) {
            dialog = new ProgressDialog(activity);
            this.activity = activity;
            this.result = result;
            this.list = list;
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
                for(int i = 0; i < list.size(); i++){
                    if(result[i] != null){
                        if(result[i] && type == CONFIRM) {
                            list.get(i).setPaymentApproved();
                        } else if(result[i] && type == REJECT){
                            list.get(i).setPaymentDenied();
                        }
                    }
                }
                return true;
            } catch (ParseException e) {
                e.printStackTrace();
                return false;
            }
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

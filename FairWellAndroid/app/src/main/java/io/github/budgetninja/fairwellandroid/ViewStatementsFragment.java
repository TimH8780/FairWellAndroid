package io.github.budgetninja.fairwellandroid;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseUser;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.github.budgetninja.fairwellandroid.StatementObject.Statement;
import io.github.budgetninja.fairwellandroid.StatementObject.SubStatement;

import static io.github.budgetninja.fairwellandroid.ContentActivity.STATEMENT_REFRESH;
import static io.github.budgetninja.fairwellandroid.ContentActivity.INDEX_STATEMENT_SUMMARY;

/**
 * A placeholder fragment containing a simple view.
 */
public class ViewStatementsFragment extends Fragment {

    private ContentActivity parent;
    private List<Statement> statementList;
    private DateFormat dateFormat;
    private StatementAdaptor adapter;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem item = menu.findItem(R.id.action_refresh);
        item.setVisible(true);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        setHasOptionsMenu(true);
        parent = (ContentActivity)getActivity();
        dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_view_statements, container, false);
        ActionBar actionBar = parent.getSupportActionBar();
        if(actionBar != null) {
            final Drawable upArrow = ContextCompat.getDrawable(getContext(), R.drawable.abc_ic_ab_back_mtrl_am_alpha);
            upArrow.setColorFilter(ContextCompat.getColor(getContext(), R.color.coolBackground), PorterDuff.Mode.SRC_ATOP);
            actionBar.setHomeAsUpIndicator(upArrow);
        }
        parent.setTitle("View Statement");

        statementList = new ArrayList<>();
        List<Statement> temp = Utility.generateStatementArray();
        for(int i = 0; i < temp.size(); i++){
            if(temp.get(i).payeeConfirm || temp.get(i).payee == ParseUser.getCurrentUser()){
                statementList.add(temp.get(i));
            }
        }

        ListView view = (ListView) rootView.findViewById(R.id.viewStatementsListView);
        adapter = new StatementAdaptor(parent, R.layout.item_view_statements, statementList);
        view.setAdapter(adapter);

        LinearLayout layout = (LinearLayout) rootView.findViewById(R.id.EmptyListView);
        TextView text = (TextView)layout.findViewById(R.id.EmptyListViewText);
        text.setText("No Statement");
        view.setEmptyView(layout);

        view.setOnItemClickListener(viewItemClickListener);

        return rootView;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                parent.mMenuDrawer.closeMenu(false);
                parent.fragMgr.popBackStack();
                return true;

            case R.id.action_refresh:
                if(!parent.isNetworkConnected()){
                    Toast.makeText(parent, "Check Internet Connection", Toast.LENGTH_SHORT).show();
                    return true;
                }
                parent.mMenuDrawer.closeMenu(false);
                ContentActivity.UpdateInBackground task = parent.new UpdateInBackground(parent, STATEMENT_REFRESH);
                task.execute();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void notifyAdaptor(){
        adapter.updateData(Utility.generateStatementArray());
    }

    private class StatementAdaptor extends ArrayAdapter<Statement>{

        Context mContext;
        int mResource;
        List<Statement> mData;

        public StatementAdaptor(Context context, int resource, List<Statement> objects){
            super(context, resource, objects);
            mContext = context;
            mResource = resource;
            mData = objects;
        }

        private void updateData(List<Statement> data){
            mData = data;
            StatementAdaptor.this.notifyDataSetChanged();
        }

        private class ViewHolder{
            TextView nameText, dueDateText, amountText, statusText;
        }

        @Override
        public int getCount(){
            return mData.size();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup viewGroup){
            Statement currentItem = mData.get(position);
            ViewHolder holder;
            if(convertView == null){
                convertView = parent.getLayoutInflater().inflate(mResource, viewGroup, false);
                holder = new ViewHolder();
                holder.nameText = (TextView) convertView.findViewById(R.id.payee);
                holder.dueDateText = (TextView) convertView.findViewById(R.id.dueDate);
                holder.amountText = (TextView) convertView.findViewById(R.id.amount);
                holder.statusText = (TextView) convertView.findViewById(R.id.status);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            if(!currentItem.isPayee) {
                SubStatement target = currentItem.findPayerStatement(ParseUser.getCurrentUser());
                holder.nameText.setText(Utility.getProfileName(currentItem.payee));
                holder.dueDateText.setText(dateFormat.format(currentItem.deadline));
                holder.amountText.setText("$ " + String.format("%.2f", target.payerAmount));
                holder.statusText.setText(target.payerConfirm ? "      " : "Required");
            }
            else{
                holder.nameText.setText(String.format("%s", "YOU"));
                holder.dueDateText.setText(dateFormat.format(currentItem.deadline));
                holder.amountText.setText("$ " + String.format("%.2f", currentItem.totalAmount));

                if(currentItem.payeeConfirm){
                    boolean check = true;
                    boolean required = false;
                    for(int i = 0; i < currentItem.payerList.size(); i++){
                        SubStatement temp = currentItem.payerList.get(i);
                        if(check) {
                            check = temp.payerPaid || temp.payerReject;
                        }
                        required = temp.paymentPending;
                        if(required){
                            holder.statusText.setText("Required");
                            break;
                        }
                    }
                    if(!check && !required){ holder.statusText.setText("Pending"); }
                    if(check && !required){ holder.statusText.setText("Settled"); }
                } else {
                    holder.statusText.setText("Required");
                }
            }

            return convertView;
        }
    }

    private AdapterView.OnItemClickListener viewItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parentView, View view, int position, long id) {
            parent.layoutManage(INDEX_STATEMENT_SUMMARY);
            parent.setStatementSummaryData(statementList.get(position));
        }
    };
}

package io.github.budgetninja.fairwellandroid;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import io.github.budgetninja.fairwellandroid.StatementObject.Statement;

/**
 * A placeholder fragment containing a simple view.
 */
public class ViewStatementsFragment extends Fragment {

    private ContentActivity parent;
    private StatementAdaptor adapter;
    private List<Statement> statementList;
    private DateFormat dateFormat;

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
            actionBar.setHomeAsUpIndicator(null);
        }
        parent.setTitle("View Statement");

        statementList = Utility.generateStatementArray();
        //if(parent.isNetworkConnected()) { statementList = Utility.generateStatementArray(); }
        //else { statementList = Utility.generateStatementArrayOffline(); }

        ListView view = (ListView) rootView.findViewById(R.id.viewStatementsListView);
        adapter = new StatementAdaptor(parent, R.layout.item_view_statements, statementList);
        view.setAdapter(adapter);

        LinearLayout layout = (LinearLayout) rootView.findViewById(R.id.EmptyListView);
        TextView text = (TextView)layout.findViewById(R.id.EmptyListViewText);
        text.setText("No Statement");
        view.setEmptyView(layout);

        return rootView;
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
            }
            else{
                holder = (ViewHolder) convertView.getTag();
            }

            ParseObject target = null;
            List<ParseObject> payer = currentItem.payer;
            for(int i = 0; i < payer.size(); i++) try{
                if (payer.get(i).fetch().getParseUser("payer") == ParseUser.getCurrentUser()) {
                    target = payer.get(i).fetch();
                    break;
                }
            } catch (ParseException e){
                Log.d("Fetch", e.toString());
            }

            holder.nameText.setText(Utility.getUserName(currentItem.payee));
            holder.dueDateText.setText(dateFormat.format(currentItem.deadline));
            if(target != null){
                Double number = target.getDouble("amount");
                holder.amountText.setText("$ " + String.format("%.2f", number));
                if(target.getBoolean("payerConfirm")){
                    holder.statusText.setText("");
                } else {
                    holder.statusText.setText("Required");
                }
            }

            return convertView;
        }
    }
}

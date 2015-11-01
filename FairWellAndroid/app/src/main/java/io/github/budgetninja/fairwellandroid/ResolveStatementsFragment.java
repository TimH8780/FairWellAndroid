package io.github.budgetninja.fairwellandroid;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class ResolveStatementsFragment extends Fragment {

    private ConnectivityManager connMgr;
    private ContentActivity parent;

    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        setHasOptionsMenu(true);
        parent = (ContentActivity)getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_resolve_statements, container, false);
        ActionBar actionBar = parent.getSupportActionBar();
        if(actionBar != null) {
            actionBar.setHomeAsUpIndicator(null);
        }
        getActivity().setTitle("Resolve Statement");
        connMgr = (ConnectivityManager)getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

        List<Utility.Friend> friendList;
        if(isNetworkConnected()) {
            friendList = Utility.generateFriendArray();
        }
        else {
            friendList = Utility.generateFriendArrayOffline();
        }

        ListView view = (ListView) rootView.findViewById(R.id.ResolveStatementsListView);
        ResolveStatementAdaptor adapter = new ResolveStatementAdaptor(getActivity(), R.layout.item_resolve_statements, friendList);
        view.setAdapter(adapter);

        LinearLayout layout = (LinearLayout)rootView.findViewById(R.id.EmptyListView_resolve);
        TextView text = (TextView)layout.findViewById(R.id.EmptyListViewText_resolve);
        text.setText("No Resolvable Statement");
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

    private boolean isNetworkConnected(){
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }


    public class ResolveStatementAdaptor extends ArrayAdapter<Utility.Friend> {

        Context mContext;
        int mResource;
        List<Utility.Friend> mObject;

        public ResolveStatementAdaptor(Context context, int resource, List<Utility.Friend> objects){
            super(context, resource, objects);
            mContext = context;
            mResource = resource;
            mObject = objects;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            Utility.Friend currentItem = mObject.get(position);
            if(convertView == null){
                convertView = getActivity().getLayoutInflater().inflate(mResource, parent, false);
            }
            TextView name = (TextView) convertView.findViewById(R.id.name_resolve);
            name.setText(currentItem.name);
            TextView youOwedAmount = (TextView) convertView.findViewById(R.id.youOweAmount_resolve);
            youOwedAmount.setText(String.format("%.2f",currentItem.currentUserOwed));
            TextView friendOwedAmount = (TextView) convertView.findViewById(R.id.heOweAmount_resolve);
            friendOwedAmount.setText(String.format("%.2f",currentItem.friendOwed));
            TextView netBalance = (TextView) convertView.findViewById(R.id.netBalanceAmount_resolve);
            netBalance.setText(String.format("%.2f", currentItem.currentUserOwed - currentItem.friendOwed));
            Button resolveButton = (Button) convertView.findViewById(R.id.button_resolve);
            resolveButton.setTag(position);

            resolveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!isNetworkConnected()) {
                        Toast.makeText(getActivity().getApplicationContext(), "Check Internet Connection", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Utility.Friend currentItem = mObject.get((int)v.getTag());
                    final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    final ScrollView input = new ScrollView(getActivity());
                    final ListView container = new ListView(getActivity());
                    input.addView(container);
                    builder.setTitle("Resolve Statement(s) with \n<" + currentItem.name + ">");
                    builder.setView(input);
                    builder.setPositiveButton("Select All", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            TextView message = new TextView(getActivity());
                            message.setText("Are you sure you want to resolve all statement?");
                            message.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
                            message.setPadding(20, 20, 20, 20);
                            builder.setTitle("Resolve All Statement");
                            builder.setView(message);
                            builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Do something (optional button)
                                }
                            });
                            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
                            final AlertDialog dialogI = builder.create();
                            dialogI.show();
                        }
                    });
                    builder.setNeutralButton("Select", new DialogInterface.OnClickListener(){
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Do something
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    final AlertDialog dialog = builder.create();
                    dialog.show();
                }
            });
            return convertView;
        }
    }
}

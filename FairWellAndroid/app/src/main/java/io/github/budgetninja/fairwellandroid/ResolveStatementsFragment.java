package io.github.budgetninja.fairwellandroid;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.ActionBar;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

import io.github.budgetninja.fairwellandroid.FriendObject.Friend;
import io.github.budgetninja.fairwellandroid.StatementObject.Statement;
import io.github.budgetninja.fairwellandroid.StatementObject.SubStatement;

import static io.github.budgetninja.fairwellandroid.Utility.getDPI;

/**
 * A placeholder fragment containing a simple view.
 */
public class ResolveStatementsFragment extends Fragment {

    private static final int IMAGE_WIDTH_HEIGHT = 90;
    private ContentActivity parent;
    private Boolean[] tempResult;
    private List<Friend> friendList;
    private List<Pair<Statement, SubStatement>> statementList;

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_resolve_statements, container, false);
        ActionBar actionBar = parent.getSupportActionBar();
        if(actionBar != null) {
            final Drawable upArrow = ContextCompat.getDrawable(getContext(), R.drawable.abc_ic_ab_back_mtrl_am_alpha);
            upArrow.setColorFilter(ContextCompat.getColor(getContext(), R.color.coolBackground), PorterDuff.Mode.SRC_ATOP);
            actionBar.setHomeAsUpIndicator(upArrow);
        }
        parent.setTitle("Resolve Statement");

        List<Friend> tempFriend;
        List<StatementObject.Statement> tempStatement;
        friendList = new ArrayList<>();
        statementList = new ArrayList<>();
        if(parent.isNetworkConnected()) {
            tempFriend = Utility.generateFriendArray();
            tempStatement = Utility.generateStatementArray();
        }
        else {
            tempFriend = Utility.generateFriendArrayOffline();
            tempStatement = new ArrayList<>();
        }

        for(int i = 0; i < tempFriend.size(); i++){
            Friend item = tempFriend.get(i);
            if(item.friendOwed > 0 || item.currentUserOwed > 0){
                friendList.add(item);
            }
        }

        for(int i = 0; i < tempStatement.size(); i++){
            SubStatement subStatement = tempStatement.get(i).findPayerStatement(ParseUser.getCurrentUser());
            if(subStatement != null) {
                if (subStatement.payerConfirm && !subStatement.payerReject && !subStatement.payerPaid && !subStatement.paymentPending) {
                    statementList.add(new Pair<>(tempStatement.get(i), subStatement));
                }
            }
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

    private class ResolveStatementAdaptor extends ArrayAdapter<Friend> {

        Context mContext;
        int mResource;
        List<Friend> mObject;

        public ResolveStatementAdaptor(Context context, int resource, List<Friend> objects){
            super(context, resource, objects);
            mContext = context;
            mResource = resource;
            mObject = objects;
        }

        private class ViewHolder{
            TextView nameText, youOwedAmount, friendOwedAmount, netBalance;
            Button resolveButton;
            ImageView photoImage;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parentGroup){
            Friend currentItem = mObject.get(position);
            ViewHolder viewHolder;
            if(convertView == null){
                viewHolder = new ViewHolder();
                convertView = getActivity().getLayoutInflater().inflate(mResource, parentGroup, false);
                viewHolder.nameText = (TextView) convertView.findViewById(R.id.name_resolve);
                viewHolder.youOwedAmount = (TextView) convertView.findViewById(R.id.youOweAmount_resolve);
                viewHolder.friendOwedAmount = (TextView) convertView.findViewById(R.id.heOweAmount_resolve);
                viewHolder.netBalance = (TextView) convertView.findViewById(R.id.netBalanceAmount_resolve);
                viewHolder.resolveButton = (Button) convertView.findViewById(R.id.button_resolve);
                viewHolder.photoImage = (ImageView) convertView.findViewById(R.id.pic_resolve);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            viewHolder.nameText.setText(currentItem.name);
            viewHolder.youOwedAmount.setText(String.format("%.2f",currentItem.currentUserOwed));
            viewHolder.friendOwedAmount.setText(String.format("%.2f",currentItem.friendOwed));
            viewHolder.netBalance.setText(String.format("%.2f", currentItem.currentUserOwed - currentItem.friendOwed));
            if(currentItem.hasPhoto()){
                int DPI = getDPI(mContext);
                int pixel = IMAGE_WIDTH_HEIGHT * (DPI / 160);
                Bitmap bmp = HomepageFragment.decodeSampledBitmapFromByteArray(currentItem.photo, pixel, pixel);
                viewHolder.photoImage.setImageBitmap(bmp);
            }
            else{ viewHolder.photoImage.setImageResource(R.drawable.profilepic); }

            viewHolder.resolveButton.setTag(position);
            viewHolder.resolveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!parent.isNetworkConnected()) {
                        Toast.makeText(getActivity().getApplicationContext(), "Check Internet Connection", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    final Friend currentItem = mObject.get((int) v.getTag());
                    final List<Pair<Statement, SubStatement>> list = new ArrayList<>();
                    for (int i = 0; i < statementList.size(); i++) {
                        if (currentItem.isSamePerson(statementList.get(i).first.payee)) {
                            list.add(statementList.get(i));
                        }
                    }
                    tempResult = new Boolean[list.size()];
                    final AlertDialog.Builder builder = new AlertDialog.Builder(parent);
                    final ListView container = new ListView(parent);

                    StatementSelectionAdaptor adaptor = new StatementSelectionAdaptor(parent, R.layout.item_statement, list);
                    container.setAdapter(adaptor);
                    container.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            CheckBox checkBox = (CheckBox) view.findViewById(R.id.statementCheckBox);
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
                    builder.setTitle("Resolve Statement(s) with \n<" + currentItem.name + ">");
                    builder.setPositiveButton("Select", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            PayerResolvingProcess task = new PayerResolvingProcess(parent, tempResult, list);
                            task.execute();
                        }
                    });
                    builder.setNegativeButton("Cancel", null);
                    final AlertDialog dialog = builder.create();
                    dialog.show();
                }
            });

            return convertView;
        }
    }

    private class StatementSelectionAdaptor extends ArrayAdapter<Pair<Statement, SubStatement>>{

        Context mContext;
        int mResource;
        List<Pair<Statement, SubStatement>> mObject;

        public StatementSelectionAdaptor(Context context, int resource, List<Pair<Statement, SubStatement>> objects){
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
            Pair<Statement, SubStatement> currentItem = mObject.get(position);
            final ViewHolder viewHolder;
            if(convertView == null){
                convertView = parent.getLayoutInflater().inflate(mResource, parentGroup, false);
                viewHolder = new ViewHolder();
                viewHolder.nameText = (TextView) convertView.findViewById(R.id.statementDescription);
                viewHolder.box = (CheckBox) convertView.findViewById(R.id.statementCheckBox);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.position = position;
            viewHolder.nameText.setText(currentItem.first.description + " [$ " +
                    String.format("%.2f", currentItem.second.payerAmount) + "]");
            if(tempResult[position] == null){
                viewHolder.box.setChecked(false);
            } else {
                viewHolder.box.setChecked(tempResult[position]);
            }

            return convertView;
        }
    }

    private class PayerResolvingProcess extends AsyncTask<Boolean, Void, Boolean> {
        private ProgressDialog dialog;
        private Context activity;
        private Boolean[] result;
        private List<Pair<Statement, SubStatement>> list;

        public PayerResolvingProcess(Context activity, Boolean[] result, List<Pair<Statement, SubStatement>> list) {
            dialog = new ProgressDialog(activity);
            this.activity = activity;
            this.result = result;
            this.list = list;
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
                        if(result[i]) {
                            list.get(i).second.setPayerResolving();
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

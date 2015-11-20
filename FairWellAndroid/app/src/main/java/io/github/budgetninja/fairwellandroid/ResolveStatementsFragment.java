package io.github.budgetninja.fairwellandroid;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import io.github.budgetninja.fairwellandroid.FriendObject.Friend;
import static io.github.budgetninja.fairwellandroid.Utility.getDPI;

/**
 * A placeholder fragment containing a simple view.
 */
public class ResolveStatementsFragment extends Fragment {

    private static final int IMAGE_WIDTH_HEIGHT = 90;
    private ContentActivity parent;

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
            actionBar.setHomeAsUpIndicator(R.drawable.ic_keyboard_arrow_left);
        }
        parent.setTitle("Resolve Statement");

        List<Friend> friendList, temp;
        friendList = new ArrayList<>();
        if(parent.isNetworkConnected()) { temp = Utility.generateFriendArray(); }
        else { temp = Utility.generateFriendArrayOffline(); }

        for(int i = 0; i < temp.size(); i++){
            Friend item = temp.get(i);
            if(item.friendOwed > 0 || item.currentUserOwed > 0){
                friendList.add(item);
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
                    Friend currentItem = mObject.get((int)v.getTag());
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
                            message.setText("Are you sure you want to resolve all statements?");
                            message.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
                            message.setPadding(20, 20, 20, 20);
                            builder.setTitle("Resolve All Statements");
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

package io.github.budgetninja.fairwellandroid;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseUser;
import io.github.budgetninja.fairwellandroid.ContentActivity.UpdateInBackground;


public class DashboardFragment extends Fragment {

    private SwipeRefreshLayout swipeContainer;
    private ListView listView;
    private ParseUser user;
    private ContentActivity parent;


    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        setHasOptionsMenu(true);
        user = ParseUser.getCurrentUser();
        parent = (ContentActivity)getActivity();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_dashboard, container, false);
        ActionBar actionBar = parent.getSupportActionBar();
        if(actionBar != null) {
            final Drawable upArrow = getResources().getDrawable(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
            upArrow.setColorFilter(getResources().getColor(R.color.coolBackground), PorterDuff.Mode.SRC_ATOP);
            actionBar.setHomeAsUpIndicator(upArrow);
        }
        parent.setTitle("Dashboard Activity");

        swipeContainer = (SwipeRefreshLayout) rootView.findViewById(R.id.swipeContainer);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Your code to refresh the list here.
                // Make sure you call swipeContainer.setRefreshing(false)
                // once the network request has completed successfully.

                swipeContainer.setRefreshing(true);
                ( new Handler()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        swipeContainer.setRefreshing(false);
                        Toast.makeText(getContext(), "List has been refreshed!", Toast.LENGTH_SHORT).show();
                    }
                }, 3000);
            }
        });

        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        listView = (ListView) rootView.findViewById(R.id.dashboardlistview);

        final ListViewItem[] items = new ListViewItem[20];

        items[0] = new ListViewItem("October 29, 2015", CustomAdapter.TYPE_DATE);
        items[1] = new ListViewItem("I paid mengpei $200 ", CustomAdapter.TYPE_STATEMENT);
        items[2] = new ListViewItem("Tim paid me $250", CustomAdapter.TYPE_STATEMENT);
        items[3] = new ListViewItem("October 26, 2015", CustomAdapter.TYPE_DATE);
        items[4] = new ListViewItem("You owe winnie $40", CustomAdapter.TYPE_STATEMENT);
        items[5] = new ListViewItem("October 25, 2015", CustomAdapter.TYPE_DATE);
        items[6] = new ListViewItem("Tim owes me $250", CustomAdapter.TYPE_STATEMENT);
        items[7] = new ListViewItem("Jarret owes me $100", CustomAdapter.TYPE_STATEMENT);
        items[8] = new ListViewItem("October 23, 2015", CustomAdapter.TYPE_DATE);
        items[9] = new ListViewItem("You owe mengpei $200", CustomAdapter.TYPE_STATEMENT);
        items[10] = new ListViewItem("October 22, 2015", CustomAdapter.TYPE_DATE);
        items[11] = new ListViewItem("I paid mengpei $200 ", CustomAdapter.TYPE_STATEMENT);
        items[12] = new ListViewItem("Tim paid me $250", CustomAdapter.TYPE_STATEMENT);
        items[13] = new ListViewItem("October 20, 2015", CustomAdapter.TYPE_DATE);
        items[14] = new ListViewItem("You owe winnie $40", CustomAdapter.TYPE_STATEMENT);
        items[15] = new ListViewItem("You owe winnie $40", CustomAdapter.TYPE_STATEMENT);
        items[16] = new ListViewItem("Tim owes me $250", CustomAdapter.TYPE_STATEMENT);
        items[17] = new ListViewItem("Jarret owes me $100", CustomAdapter.TYPE_STATEMENT);
        items[18] = new ListViewItem("October 11, 2015", CustomAdapter.TYPE_DATE);
        items[19] = new ListViewItem("You owe mengpei $200", CustomAdapter.TYPE_STATEMENT);


        CustomAdapter customAdapter = new CustomAdapter(getContext(), R.id.text, items);
        listView.setAdapter(customAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Toast.makeText(getContext(), items[i].getText(), Toast.LENGTH_SHORT).show();
            }
        });

        return rootView;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == android.R.id.home){
            parent.mMenuDrawer.closeMenu(false);
            parent.fragMgr.popBackStack();
            return true;
        }
        if(id == R.id.action_refresh){
            Toast.makeText(parent,"Not functional yet", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }




    public class CustomAdapter extends ArrayAdapter<ListViewItem> {

        public static final int TYPE_DATE = 0;
        public static final int TYPE_STATEMENT = 1;

        private ListViewItem[] objects;

        @Override
        public int getViewTypeCount() {
            return 4;
        }

        @Override
        public int getItemViewType(int position) {
            return objects[position].getType();
        }

        public CustomAdapter(Context context, int resource, ListViewItem[] objects) {
            super(context, resource, objects);
            this.objects = objects;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder viewHolder = null;
            ListViewItem listViewItem = objects[position];
            int listViewItemType = getItemViewType(position);


            if (convertView == null) {

                if (listViewItemType == TYPE_DATE) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.type_date, null);
                } else if (listViewItemType == TYPE_STATEMENT) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.type_statement, null);
                }

                TextView textView = (TextView) convertView.findViewById(R.id.text);
                viewHolder = new ViewHolder(textView);

                convertView.setTag(viewHolder);

            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            viewHolder.getText().setText(listViewItem.getText());

            return convertView;
        }
    }

    public class ListViewItem {
        private String text;
        private int type;

        public ListViewItem(String text, int type) {
            this.text = text;
            this.type = type;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

    }

    public class ViewHolder {
        TextView text;

        public ViewHolder(TextView text) {
            this.text = text;
        }

        public TextView getText() {
            return text;
        }

        public void setText(TextView text) {
            this.text = text;
        }

    }


}

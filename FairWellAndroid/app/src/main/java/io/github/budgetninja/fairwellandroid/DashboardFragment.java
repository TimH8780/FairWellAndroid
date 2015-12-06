package io.github.budgetninja.fairwellandroid;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.hb.views.PinnedSectionListView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static io.github.budgetninja.fairwellandroid.ContentActivity.DASHBOARD_REFRESH;
import static io.github.budgetninja.fairwellandroid.Utility.getDPI;


public class DashboardFragment extends ListFragment {

    /**
     * ISSAC: Uncommented for now. If we no longer use this code, I will remove it.
     */

//    private SwipeRefreshLayout swipeContainer;
//    private ListView listView;
//    private ParseUser user;
//    private ContentActivity parent;
//
//
//    @Override
//    public void onCreate(Bundle bundle){
//        super.onCreate(bundle);
//        setHasOptionsMenu(true);
//        user = ParseUser.getCurrentUser();
//        parent = (ContentActivity)getActivity();
//
//    }
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        View rootView = inflater.inflate(R.layout.fragment_dashboard, container, false);
//        ActionBar actionBar = parent.getSupportActionBar();
//        if(actionBar != null) {
//            final Drawable upArrow = getResources().getDrawable(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
//            upArrow.setColorFilter(getResources().getColor(R.color.coolBackground), PorterDuff.Mode.SRC_ATOP);
//            actionBar.setHomeAsUpIndicator(upArrow);
//        }
//        parent.setTitle("Dashboard Activity");
//
//        swipeContainer = (SwipeRefreshLayout) rootView.findViewById(R.id.swipeContainer);
//        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
//            @Override
//            public void onRefresh() {
//                // Your code to refresh the list here.
//                // Make sure you call swipeContainer.setRefreshing(false)
//                // once the network request has completed successfully.
//
//                swipeContainer.setRefreshing(true);
//                ( new Handler()).postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        swipeContainer.setRefreshing(false);
//                        Toast.makeText(getContext(), "List has been refreshed!", Toast.LENGTH_SHORT).show();
//                    }
//                }, 3000);
//            }
//        });
//
//        // Configure the refreshing colors
//        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
//                android.R.color.holo_green_light,
//                android.R.color.holo_orange_light,
//                android.R.color.holo_red_light);
//
//        listView = (ListView) rootView.findViewById(R.id.dashboardlistview);
//
//        final ListViewItem[] items = new ListViewItem[20];
//
//        items[0] = new ListViewItem("October 29, 2015", CustomAdapter.TYPE_DATE);
//        items[1] = new ListViewItem("I paid mengpei $200 ", CustomAdapter.TYPE_STATEMENT);
//        items[2] = new ListViewItem("Tim paid me $250", CustomAdapter.TYPE_STATEMENT);
//        items[3] = new ListViewItem("October 26, 2015", CustomAdapter.TYPE_DATE);
//        items[4] = new ListViewItem("You owe winnie $40", CustomAdapter.TYPE_STATEMENT);
//        items[5] = new ListViewItem("October 25, 2015", CustomAdapter.TYPE_DATE);
//        items[6] = new ListViewItem("Tim owes me $250", CustomAdapter.TYPE_STATEMENT);
//        items[7] = new ListViewItem("Jarret owes me $100", CustomAdapter.TYPE_STATEMENT);
//        items[8] = new ListViewItem("October 23, 2015", CustomAdapter.TYPE_DATE);
//        items[9] = new ListViewItem("You owe mengpei $200", CustomAdapter.TYPE_STATEMENT);
//        items[10] = new ListViewItem("October 22, 2015", CustomAdapter.TYPE_DATE);
//        items[11] = new ListViewItem("I paid mengpei $200 ", CustomAdapter.TYPE_STATEMENT);
//        items[12] = new ListViewItem("Tim paid me $250", CustomAdapter.TYPE_STATEMENT);
//        items[13] = new ListViewItem("October 20, 2015", CustomAdapter.TYPE_DATE);
//        items[14] = new ListViewItem("You owe winnie $40", CustomAdapter.TYPE_STATEMENT);
//        items[15] = new ListViewItem("You owe winnie $40", CustomAdapter.TYPE_STATEMENT);
//        items[16] = new ListViewItem("Tim owes me $250", CustomAdapter.TYPE_STATEMENT);
//        items[17] = new ListViewItem("Jarret owes me $100", CustomAdapter.TYPE_STATEMENT);
//        items[18] = new ListViewItem("October 11, 2015", CustomAdapter.TYPE_DATE);
//        items[19] = new ListViewItem("You owe mengpei $200", CustomAdapter.TYPE_STATEMENT);
//
//
//        CustomAdapter customAdapter = new CustomAdapter(getContext(), R.id.text, items);
//        listView.setAdapter(customAdapter);
//        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//                Toast.makeText(getContext(), items[i].getText(), Toast.LENGTH_SHORT).show();
//            }
//        });
//
//        return rootView;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        int id = item.getItemId();
//
//        if(id == android.R.id.home){
//            parent.mMenuDrawer.closeMenu(false);
//            parent.fragMgr.popBackStack();
//            return true;
//        }
//        if(id == R.id.action_refresh){
//            Toast.makeText(parent,"Not functional yet", Toast.LENGTH_SHORT).show();
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }
//
//
//
//
//    public class CustomAdapter extends ArrayAdapter<ListViewItem> {
//
//        public static final int TYPE_DATE = 0;
//        public static final int TYPE_STATEMENT = 1;
//
//        private ListViewItem[] objects;
//
//        @Override
//        public int getViewTypeCount() {
//            return 4;
//        }
//
//        @Override
//        public int getItemViewType(int position) {
//            return objects[position].getType();
//        }
//
//        public CustomAdapter(Context context, int resource, ListViewItem[] objects) {
//            super(context, resource, objects);
//            this.objects = objects;
//        }
//
//        @Override
//        public View getView(int position, View convertView, ViewGroup parent) {
//
//            ViewHolder viewHolder = null;
//            ListViewItem listViewItem = objects[position];
//            int listViewItemType = getItemViewType(position);
//
//
//            if (convertView == null) {
//
//                if (listViewItemType == TYPE_DATE) {
//                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.type_date, null);
//                } else if (listViewItemType == TYPE_STATEMENT) {
//                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.type_statement, null);
//                }
//
//                TextView textView = (TextView) convertView.findViewById(R.id.text);
//                viewHolder = new ViewHolder(textView);
//
//                convertView.setTag(viewHolder);
//
//            } else {
//                viewHolder = (ViewHolder) convertView.getTag();
//            }
//
//            viewHolder.getText().setText(listViewItem.getText());
//
//            return convertView;
//        }
//    }
//
//    public class ListViewItem {
//        private String text;
//        private int type;
//
//        public ListViewItem(String text, int type) {
//            this.text = text;
//            this.type = type;
//        }
//
//        public String getText() {
//            return text;
//        }
//
//        public void setText(String text) {
//            this.text = text;
//        }
//
//        public int getType() {
//            return type;
//        }
//
//        public void setType(int type) {
//            this.type = type;
//        }
//
//    }
//
//    public class ViewHolder {
//        TextView text;
//
//        public ViewHolder(TextView text) {
//            this.text = text;
//        }
//
//        public TextView getText() {
//            return text;
//        }
//
//        public void setText(TextView text) {
//            this.text = text;
//        }
//
//    }

    private ContentActivity parent;
    private SimpleAdapter adapter;

    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        setHasOptionsMenu(true);
        parent = (ContentActivity)getActivity();
        adapter = new SimpleAdapter(getContext(), android.R.layout.simple_list_item_1, android.R.id.text1);
    }

    @Nullable @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_dashboard, container, false);
        ActionBar actionBar = parent.getSupportActionBar();
        if(actionBar != null) {
            final Drawable upArrow = ContextCompat.getDrawable(getContext(), R.drawable.abc_ic_ab_back_mtrl_am_alpha);
            upArrow.setColorFilter(ContextCompat.getColor(getContext(), R.color.coolBackground), PorterDuff.Mode.SRC_ATOP);
            actionBar.setHomeAsUpIndicator(upArrow);
        }
        parent.setTitle("Dashboard");

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        ((PinnedSectionListView)getListView()).setShadowVisible(false);
        initializeAdapter();
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        /* Do nothing */
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
            parent.mMenuDrawer.closeMenu(false);
            ContentActivity.UpdateInBackground task = parent.new UpdateInBackground(parent, DASHBOARD_REFRESH);
            task.execute();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void notifyAdaptor(){
        adapter.clear();
        adapter.generateDataSet();
        adapter.notifyDataSetChanged();
    }

    private void initializeAdapter() {
        getListView().setFastScrollEnabled(false);
        setListAdapter(adapter);
    }

    private class SimpleAdapter extends ArrayAdapter<Item> implements PinnedSectionListView.PinnedSectionListAdapter {

        private final int[] COLORS = new int[] {R.color.green_light, R.color.orange_light, R.color.blue_light, R.color.red_light};

        public SimpleAdapter(Context context, int resource, int textViewResourceId) {
            super(context, resource, textViewResourceId);
            generateDataSet();
        }

        public void generateDataSet() {
            List<String> data;
            if(parent.isNetworkConnected()) { data = Utility.getDashboardData();}
            else { data = Utility.getDashboardDataOffline(); }

            int sectionPosition = 0, listPosition = 0;
            Calendar calendar = Calendar.getInstance();
            DateFormat format = new SimpleDateFormat("MMMM yyyy", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("GMT-05:00"));
            String currentMonth = format.format(calendar.getTime());

            Item section = new Item(Item.SECTION, currentMonth);
            section.sectionPosition = sectionPosition;
            section.listPosition = listPosition++;
            add(section);

            for (int i = data.size() - 1; i >= 0; i--) {
                int index = data.get(i).indexOf("|");
                String month = data.get(i).substring(0, index - 1);
                String message = data.get(i).substring(index + 2);
                if(!month.equals(currentMonth)){
                    currentMonth = month;
                    section = new Item(Item.SECTION, month);
                    section.sectionPosition = ++sectionPosition;
                    section.listPosition = listPosition++;
                    add(section);
                }

                Item item = new Item(Item.ITEM, message);
                item.sectionPosition = sectionPosition;
                item.listPosition = listPosition++;
                add(item);
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView view = (TextView) super.getView(position, convertView, parent);
            view.setTextColor(Color.DKGRAY);

            view.setTag("" + position);
            Item item = getItem(position);
            if (item.type == Item.SECTION) {
                Drawable img = ContextCompat.getDrawable(getContext(), R.drawable.ic_date_range_black_24dp);

                int DPI = getDPI(this.getContext());
                double size_double = 75 * DPI / 480;
                int size = (int) size_double;
                img.setBounds(0, 0, size, size);
                view.setCompoundDrawablePadding(10);
                view.setCompoundDrawables(img, null, null, null);
                view.setTypeface(null, Typeface.BOLD);
                view.setBackgroundColor(ContextCompat.getColor(getContext(), COLORS[item.sectionPosition % COLORS.length]));
            } else {
                view.setTextSize(14);
            }
            return view;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return getItem(position).type;
        }

        @Override
        public boolean isItemViewTypePinned(int viewType) {
            return viewType == Item.SECTION;
        }
    }

    private static class Item {

        public static final int ITEM = 0;
        public static final int SECTION = 1;
        public final int type;
        public final String text;
        public int sectionPosition;
        public int listPosition;

        public Item(int type, String text) {
            this.type = type;
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }

    }

}

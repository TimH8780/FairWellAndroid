package io.github.budgetninja.fairwellandroid;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.LogOutCallback;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import net.simonvt.menudrawer.MenuDrawer;

import java.util.ArrayList;
import java.util.List;

import io.github.budgetninja.fairwellandroid.StatementObject.Statement;
import io.github.budgetninja.fairwellandroid.StatementObject.SummaryStatement;

public class ContentActivity extends AppCompatActivity{

    public static String tempString = "";

    private static final String STATE_ACTIVE_POSITION = "net.simonvt.menudrawer.samples.ContentActivity.activePosition";
    private static final String STATE_CONTENT_TEXT = "net.simonvt.menudrawer.samples.ContentActivity.contentText";
    public static final int NORMAL_USER = 0;
    public static final int FACEBOOK_USER = 1;
    public static final int TWITTER_USER = 2;
    public static final int ALL_REFRESH = 0;
    public static final int DASHBOARD_REFRESH = 1;
    public static final int FRIEND_REFRESH = 2;
    public static final int STATEMENT_REFRESH = 3;
    public static final int POSITION_HOME = 0;
    private static final int POSITION_FRIENDS = 2;
    private static final int POSITION_DASHBOARD = 3;
    private static final int POSITION_SMART_SOLVE = 4;
    private static final int POSITION_ACCOUNT_SETTING = 6;
    private static final int POSITION_NOTIFICATION_SETTING = 7;
    private static final int POSITION_TUTORIAL = 9;
    private static final int POSITION_RATE_THIS_APP = 10;
    private static final int POSITION_ABOUT_US = 11;
    private static final int POSITION_LOGOUT = 12;
    public static final int INDEX_VIEW_STATEMENT = 14;
    public static final int INDEX_ADD_STATEMENT = 15;
    public static final int INDEX_RESOLVE_STATEMENT = 16;
    public static final int INDEX_SUBMIT_STATEMENT_SUMMARY = 17;
    public static final int INDEX_STATEMENT_SUMMARY = 18;
    public static double OWN_BALANCE = 0.00;
    public static double OWE_BALANCE = 0.00;

    private boolean verification = false;
    protected MenuDrawer mMenuDrawer;
    private int mActivePosition = -1;
    private String mContentText;
    boolean doubleBackToExitPressedOnce = false;
    CheckForUpdate checkForUpdate;

    protected ConnectivityManager connectMgr;
    protected FragmentManager fragMgr;
    protected FragmentTransaction fragTrans;
    private ParseUser user;

    @Override
    protected void onCreate(final Bundle inState) {
        super.onCreate(inState);
        connectMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        fragMgr = getSupportFragmentManager();
        user = ParseUser.getCurrentUser();

        //ActionBar
        if(getSupportActionBar() != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            getSupportActionBar().setElevation(0);
        }

        //SideMenu
        if (inState != null) {
            mActivePosition = inState.getInt(STATE_ACTIVE_POSITION);
            mContentText = inState.getString(STATE_CONTENT_TEXT);
        }

        mMenuDrawer = MenuDrawer.attach(this, MenuDrawer.MENU_DRAG_CONTENT);
        mMenuDrawer.setContentView(R.layout.activity_container);
        mMenuDrawer.setTouchMode(MenuDrawer.TOUCH_MODE_FULLSCREEN);

        List<Object> items = new ArrayList<>();
        items.add(new Item(getString(R.string.home), R.drawable.ic_home_white_24dp));
        items.add(new Category(getString(R.string.features)));
        items.add(new Item(getString(R.string.friends), R.drawable.ic_people_white_24dp));
        items.add(new Item(getString(R.string.dashboard), R.drawable.ic_dashboard_white_24dp));
        items.add(new Item(getString(R.string.smart_solve), R.drawable.ic_gavel_white_24dp));
        items.add(new Category(getString(R.string.setting)));
        items.add(new Item(getString(R.string.account_setting), R.drawable.ic_settings_white_24dp));
        items.add(new Item(getString(R.string.notification_setting), R.drawable.ic_notifications_active_white_24dp));
        items.add(new Category(getString(R.string.others)));
        items.add(new Item(getString(R.string.tutorial), R.drawable.ic_library_books_white_24dp));
        items.add(new Item(getString(R.string.rate_this_app), R.drawable.ic_thumb_up_white_24dp));
        items.add(new Item(getString(R.string.about_us), R.drawable.ic_face_white_24dp));
        items.add(new Item(getString(R.string.logout), R.drawable.ic_exit_to_app_white_24dp));

        ListView mList = new ListView(this);
        MenuAdapter mAdapter = new MenuAdapter(items);
        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(mItemClickListener);
        mMenuDrawer.setMenuView(mList);
        mMenuDrawer.setOnInterceptMoveEventListener(new MenuDrawer.OnInterceptMoveEventListener() {
            @Override
            public boolean isViewDraggable(View v, int dx, int x, int y) {return v instanceof SeekBar;}
        });

        //Prompt Facebook and Twitter User to setup email
        if(isNetworkConnected()) {
            if (user != null) {
                if (user.getEmail() == null) {
                    setEmailFacebookTwitterUser();
                }
            }
        }

        layoutManage(POSITION_HOME);

        checkForUpdate = new CheckForUpdate();
        UpdateInBackground task = new UpdateInBackground(this, ALL_REFRESH);
        task.execute();
        new Thread(checkForUpdate).start();

        String notificationKey = getIntent().getStringExtra("notificationKey");
        if(notificationKey != null){
            if(notificationKey.equals("FRIEND_REQUEST") || notificationKey.equals("FRIEND_REQUEST_ACCEPTED")){
                switchFriends();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_ACTIVE_POSITION, mActivePosition);
        outState.putString(STATE_CONTENT_TEXT, mContentText);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    //MUST not put anything function for id equal to android.R.id.home or R.id.action_add_friend,
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    @Override
    public void onBackPressed() {
        final int drawerState = mMenuDrawer.getDrawerState();
        if(drawerState == MenuDrawer.STATE_OPEN || drawerState == MenuDrawer.STATE_OPENING) {
            mMenuDrawer.closeMenu();
            return;
        }
        if(doubleBackToExitPressedOnce) {
            // this is to close the app entirely, but it will still be in the stack
            Intent a = new Intent(Intent.ACTION_MAIN);
            a.addCategory(Intent.CATEGORY_HOME);
            a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(a);
        }
        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Click BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }

    public void iconClick(final View view){
        String clicked = "";
        if(view == findViewById(R.id.icon_1)){
            ((ImageView) view).setImageResource(R.drawable.i01on);
            ((ImageView) findViewById(R.id.icon_2)).setImageResource(R.drawable.i02);
            ((ImageView) findViewById(R.id.icon_3)).setImageResource(R.drawable.i03);
            ((ImageView) findViewById(R.id.icon_4)).setImageResource(R.drawable.i04);
            ((ImageView) findViewById(R.id.icon_5)).setImageResource(R.drawable.i05);
            ((ImageView) findViewById(R.id.icon_6)).setImageResource(R.drawable.i06);
            ((ImageView) findViewById(R.id.icon_7)).setImageResource(R.drawable.i07);

            clicked = "Food";
        }
        else if(view == findViewById(R.id.icon_2)){
            ((ImageView) view).setImageResource(R.drawable.i02on);
            ((ImageView) findViewById(R.id.icon_1)).setImageResource(R.drawable.i01);
            ((ImageView) findViewById(R.id.icon_3)).setImageResource(R.drawable.i03);
            ((ImageView) findViewById(R.id.icon_4)).setImageResource(R.drawable.i04);
            ((ImageView) findViewById(R.id.icon_5)).setImageResource(R.drawable.i05);
            ((ImageView) findViewById(R.id.icon_6)).setImageResource(R.drawable.i06);
            ((ImageView) findViewById(R.id.icon_7)).setImageResource(R.drawable.i07);
            clicked = "Movie";
        }
        else if(view == findViewById(R.id.icon_3)){
            ((ImageView) view).setImageResource(R.drawable.i03on);
            ((ImageView) findViewById(R.id.icon_1)).setImageResource(R.drawable.i01);
            ((ImageView) findViewById(R.id.icon_2)).setImageResource(R.drawable.i02);
            ((ImageView) findViewById(R.id.icon_4)).setImageResource(R.drawable.i04);
            ((ImageView) findViewById(R.id.icon_5)).setImageResource(R.drawable.i05);
            ((ImageView) findViewById(R.id.icon_6)).setImageResource(R.drawable.i06);
            ((ImageView) findViewById(R.id.icon_7)).setImageResource(R.drawable.i07);
            clicked = "Travel";
        }
        else if(view == findViewById(R.id.icon_4)){
            ((ImageView) view).setImageResource(R.drawable.i04on);
            ((ImageView) findViewById(R.id.icon_1)).setImageResource(R.drawable.i01);
            ((ImageView) findViewById(R.id.icon_2)).setImageResource(R.drawable.i02);
            ((ImageView) findViewById(R.id.icon_3)).setImageResource(R.drawable.i03);
            ((ImageView) findViewById(R.id.icon_5)).setImageResource(R.drawable.i05);
            ((ImageView) findViewById(R.id.icon_6)).setImageResource(R.drawable.i06);
            ((ImageView) findViewById(R.id.icon_7)).setImageResource(R.drawable.i07);
            clicked = "Grocery";
        }
        else if(view == findViewById(R.id.icon_5)){
            ((ImageView) view).setImageResource(R.drawable.i05on);
            ((ImageView) findViewById(R.id.icon_1)).setImageResource(R.drawable.i01);
            ((ImageView) findViewById(R.id.icon_2)).setImageResource(R.drawable.i02);
            ((ImageView) findViewById(R.id.icon_3)).setImageResource(R.drawable.i03);
            ((ImageView) findViewById(R.id.icon_4)).setImageResource(R.drawable.i04);
            ((ImageView) findViewById(R.id.icon_6)).setImageResource(R.drawable.i06);
            ((ImageView) findViewById(R.id.icon_7)).setImageResource(R.drawable.i07);
            clicked = "Utility";
        }
        else if(view == findViewById(R.id.icon_6)){
            ((ImageView) view).setImageResource(R.drawable.i06on);
            ((ImageView) findViewById(R.id.icon_1)).setImageResource(R.drawable.i01);
            ((ImageView) findViewById(R.id.icon_2)).setImageResource(R.drawable.i02);
            ((ImageView) findViewById(R.id.icon_3)).setImageResource(R.drawable.i03);
            ((ImageView) findViewById(R.id.icon_4)).setImageResource(R.drawable.i04);
            ((ImageView) findViewById(R.id.icon_5)).setImageResource(R.drawable.i05);
            ((ImageView) findViewById(R.id.icon_7)).setImageResource(R.drawable.i07);
            clicked = "Entertainment";
        }
        else if(view == findViewById(R.id.icon_7)){
            ((ImageView) view).setImageResource(R.drawable.i07on);
            ((ImageView) findViewById(R.id.icon_1)).setImageResource(R.drawable.i01);
            ((ImageView) findViewById(R.id.icon_2)).setImageResource(R.drawable.i02);
            ((ImageView) findViewById(R.id.icon_3)).setImageResource(R.drawable.i03);
            ((ImageView) findViewById(R.id.icon_4)).setImageResource(R.drawable.i04);
            ((ImageView) findViewById(R.id.icon_5)).setImageResource(R.drawable.i05);
            ((ImageView) findViewById(R.id.icon_6)).setImageResource(R.drawable.i06);




            final CategoryItem[] items = {

                    new CategoryItem("Phone Bill", R.drawable.i08, R.drawable.i08on),
                    new CategoryItem("Money", R.drawable.i09, R.drawable.i09on),
                    new CategoryItem("Gift", R.drawable.i10, R.drawable.i10on),
                    new CategoryItem("Shopping", R.drawable.i11, R.drawable.i11on),
                    new CategoryItem("Maintenance", R.drawable.i12, R.drawable.i12on),
                    new CategoryItem("Credit bills", R.drawable.i13, R.drawable.i13on),
                    new CategoryItem("Restaurant", R.drawable.i14, R.drawable.i14on),
            };

            final ListAdapter adapter = new ArrayAdapter<CategoryItem>(
                    this,
                    android.R.layout.select_dialog_item,
                    android.R.id.text1,
                    items){
                public View getView(int position, View convertView, ViewGroup parent) {
                    //Use super class to create the View
                    View v = super.getView(position, convertView, parent);


                    TextView tv = (TextView)v.findViewById(android.R.id.text1);

                    Drawable img  = ContextCompat.getDrawable(getApplicationContext(), items[position].icon);
                    img.setBounds(0, 0, 100, 100);
                    //Put the image on the TextView
                    tv.setCompoundDrawables(img, null, null, null);

                 //   tv.setCompoundDrawablesWithIntrinsicBounds(items[position].icon, 0, 0, 0);

                    //Add margin between image and text (support various screen densities)
                    int dp5 = (int) (5 * getResources().getDisplayMetrics().density + 0.5f);
                    tv.setCompoundDrawablePadding(dp5);
               //     tv.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, 100 /* this is item height */));

                    return v;
                }
            };


            new AlertDialog.Builder(this)
                    .setTitle("Select category")
                    .setAdapter(adapter, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {


                            ((ImageView) view).setImageResource(items[item].icon_selected);
                            ((AddStatementFragment)getSupportFragmentManager().
                                    findFragmentByTag("Add")).setClickedIconText(items[item].text);


                            //...
                        }
                    }).show();









            clicked = "Other";
        }

        ((AddStatementFragment)getSupportFragmentManager().findFragmentByTag("Add")).setClickedIconText(clicked);
    }

    @SuppressLint("CommitTransaction")
    private void switchHome(){
        fragTrans = fragMgr.beginTransaction();
        Fragment fragment = fragMgr.findFragmentByTag("Home");
        if(fragment != null){
            if(fragment.isVisible()) { return; }
        }
        fragMgr.popBackStack("Home", FragmentManager.POP_BACK_STACK_INCLUSIVE);
        fragTrans.replace(R.id.container, new HomepageFragment(), "Home").addToBackStack("Home");
        fragTrans.commit();
        fragMgr.executePendingTransactions();
    }

    @SuppressLint("CommitTransaction")
    private void switchDashBoard(){
        if(!emailVerificationCheck()){
            emailNotVerifiedDialog();
            return;
        }
        fragTrans = fragMgr.beginTransaction();
        Fragment fragment = fragMgr.findFragmentByTag("Dashboard");
        if(fragment != null){
            if(fragment.isVisible()) { return; }
        }
        fragTrans.replace(R.id.container, new DashboardFragment(), "Dashboard").addToBackStack("Dashboard");
        fragTrans.commit();
        fragMgr.executePendingTransactions();
    }

    @SuppressLint("CommitTransaction")
    private void switchFriends(){
        if(!emailVerificationCheck()){
            emailNotVerifiedDialog();
            return;
        }
        fragTrans = fragMgr.beginTransaction();
        Fragment fragment = fragMgr.findFragmentByTag("Friend");
        if(fragment != null){
            if(fragment.isVisible()) { return; }
        }
        fragTrans.replace(R.id.container, new FriendsFragment(), "Friend").addToBackStack("Friend");
        fragTrans.commit();
        fragMgr.executePendingTransactions();
    }

    @SuppressLint("CommitTransaction")
    private void switchSmartSolve(){
        if(!emailVerificationCheck()){
            emailNotVerifiedDialog();
            return;
        }
        fragTrans = fragMgr.beginTransaction();
        Fragment fragment = fragMgr.findFragmentByTag("Solve");
        if(fragment != null){
            if(fragment.isVisible()) { return; }
        }
        fragTrans.replace(R.id.container, new SmartSolveFragment(), "Solve").addToBackStack("Solve");
        fragTrans.commit();
        fragMgr.executePendingTransactions();
    }

    @SuppressLint("CommitTransaction")
    private void switchAccountSetting(){
        fragTrans = fragMgr.beginTransaction();
        Fragment fragment = fragMgr.findFragmentByTag("Account");
        if(fragment != null){
            if(fragment.isVisible()) { return; }
        }
        fragTrans.replace(R.id.container, new AccountSettingFragment(), "Account").addToBackStack("Account");
        fragTrans.commit();
        fragMgr.executePendingTransactions();
    }

    @SuppressLint("CommitTransaction")
    private void switchNotificationSetting(){
        fragTrans = fragMgr.beginTransaction();
        Fragment fragment = fragMgr.findFragmentByTag("Notification");
        if(fragment != null){
            if(fragment.isVisible()) { return; }
        }
        fragTrans.replace(R.id.container, new NotificationSettingFragment(), "Notification").addToBackStack("Notification");
        fragTrans.commit();
        fragMgr.executePendingTransactions();
    }

    @SuppressLint("CommitTransaction")
    private void switchViewStatement(){
        if(!emailVerificationCheck()){
            emailNotVerifiedDialog();
            return;
        }
        fragTrans = fragMgr.beginTransaction();
        fragTrans.replace(R.id.container, new ViewStatementsFragment(), "View").addToBackStack("View");
        fragTrans.commit();
        fragMgr.executePendingTransactions();
    }

    @SuppressLint("CommitTransaction")
    private void switchAddStatement(){
        if(!emailVerificationCheck()){
            emailNotVerifiedDialog();
            return;
        }
        fragTrans = fragMgr.beginTransaction();
        fragTrans.replace(R.id.container, new AddStatementFragment(), "Add").addToBackStack("Add");
        fragTrans.commit();
        fragMgr.executePendingTransactions();
    }

    @SuppressLint("CommitTransaction")
    private void switchResolveStatement(){
        if(!emailVerificationCheck()){
            emailNotVerifiedDialog();
            return;
        }
        fragTrans = fragMgr.beginTransaction();
        fragTrans.replace(R.id.container, new ResolveStatementsFragment(), "Resolve").addToBackStack("Resolve");
        fragTrans.commit();
        fragMgr.executePendingTransactions();
    }

    @SuppressLint("CommitTransaction")
    private void switchSubmitStatementSummary(){
        fragTrans = fragMgr.beginTransaction();
        fragTrans.replace(R.id.container, new SubmitStatementSummaryFragment(), "Summary").addToBackStack("Summary");
        fragTrans.commit();
        fragMgr.executePendingTransactions();
    }

    @SuppressLint("CommitTransaction")
    private void switchStatementSummary(){
        fragTrans = fragMgr.beginTransaction();
        fragTrans.replace(R.id.container, new StatementSummaryFragment(), "Summary").addToBackStack("Summary");
        fragTrans.commit();
        fragMgr.executePendingTransactions();
    }

    protected void layoutManage(int index){
        switch (index) {
            case POSITION_HOME:
                switchHome();
                break;

            case POSITION_DASHBOARD:
                switchDashBoard();
                break;

            case POSITION_FRIENDS:
                switchFriends();
                break;

            case POSITION_SMART_SOLVE:
                switchSmartSolve();
                break;

            case POSITION_ACCOUNT_SETTING:
                switchAccountSetting();
                break;

            case POSITION_NOTIFICATION_SETTING:
                switchNotificationSetting();
                break;

            case INDEX_VIEW_STATEMENT:
                if(!isNetworkConnected()) {
                    Toast.makeText(getApplicationContext(), "Check Internet Connection", Toast.LENGTH_SHORT).show();
                    break;
                }
                switchViewStatement();
                break;

            case INDEX_ADD_STATEMENT:
                switchAddStatement();
                break;

            case INDEX_RESOLVE_STATEMENT:
                switchResolveStatement();
                break;

            case INDEX_SUBMIT_STATEMENT_SUMMARY:
                switchSubmitStatementSummary();
                break;

            case INDEX_STATEMENT_SUMMARY:
                switchStatementSummary();
                break;
        }
    }

    protected void setSubmitStatementSummaryData(SummaryStatement data){
        SubmitStatementSummaryFragment child = (SubmitStatementSummaryFragment) getSupportFragmentManager().findFragmentByTag("Summary");
        if(child != null) {
            child.setData(data);
        }
    }

    protected void setStatementSummaryData(Statement data){
        StatementSummaryFragment child = (StatementSummaryFragment) getSupportFragmentManager().findFragmentByTag("Summary");
        if(child != null) {
            child.setData(data);
        }
    }

    protected boolean isNetworkConnected(){
        NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    protected void setEmailFacebookTwitterUser(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(ContentActivity.this);
        final LinearLayout layout = new LinearLayout(ContentActivity.this);
        final TextView message = new TextView(ContentActivity.this);
        final EditText userInput = new EditText(ContentActivity.this);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams para = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        para.setMargins(20, 20, 20, 0);
        message.setText("An email address is required for some functionality. Please link your email address to the account.");
        message.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        message.setLayoutParams(para);
        userInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        userInput.setLayoutParams(para);
        layout.addView(message);
        layout.addView(userInput);
        builder.setTitle("Link your Email");
        builder.setView(layout);

        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String email = userInput.getText().toString();
                user.setEmail(email);
                user.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if(e == null){
                            Toast.makeText(getApplicationContext(), "Success. A verification email was sent to " + email, Toast.LENGTH_SHORT).show();
                            Utility.setNewEntryFieldForAllFriend();
                            AccountSettingFragment fragment = (AccountSettingFragment)fragMgr.findFragmentByTag("Account");
                            if(fragment != null){
                                fragment.removeAddEmailButton();
                            }
                            return;
                        }
                        Toast.makeText(getApplicationContext(), "Invalid Email Address", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        builder.setNegativeButton("Do it Later", null);
        final AlertDialog dialog = builder.create();
        dialog.show();
    }

    private AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            mActivePosition = position;
            mMenuDrawer.setActiveView(view, position);

            switch(position) {
                case POSITION_HOME:
                    switchHome();
                    break;

                case POSITION_DASHBOARD:
                    switchDashBoard();
                    break;

                case POSITION_FRIENDS:
                    switchFriends();
                    break;

                case POSITION_SMART_SOLVE:
                    if(!isNetworkConnected()) {
                        Toast.makeText(getApplicationContext(), "Check Internet Connection", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    switchSmartSolve();
                    break;

                case POSITION_ACCOUNT_SETTING:
                    switchAccountSetting();
                    break;

                case POSITION_NOTIFICATION_SETTING:
                    switchNotificationSetting();
                    break;

                case POSITION_RATE_THIS_APP:
                    if(!isNetworkConnected()) {
                        Toast.makeText(getApplicationContext(), "Check Internet Connection", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    Intent i = new Intent();
                    i.setAction(Intent.ACTION_VIEW);
                    i.addCategory(Intent.CATEGORY_BROWSABLE);
                    i.setData(Uri.parse("https://play.google.com/store/apps/details?id=io.github.budgetninja.fairwellandroid&ah=kmmwX-tUpzW2NWz-3BPS18Orv4c&hl=en-GB"));
                    startActivity(i);
                    break;

                case POSITION_ABOUT_US:
                    if(!isNetworkConnected()) {
                        Toast.makeText(getApplicationContext(), "Check Internet Connection", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    Intent i2 = new Intent();
                    i2.setAction(Intent.ACTION_VIEW);
                    i2.addCategory(Intent.CATEGORY_BROWSABLE);
                    i2.setData(Uri.parse("http://budgetninja.github.io"));
                    startActivity(i2);
                    break;

                case POSITION_TUTORIAL:
                    Intent tutorial = new Intent(ContentActivity.this, MyIntro.class);
                    startActivity(tutorial);
                    break;

                case POSITION_LOGOUT:
                    if(!isNetworkConnected()) {
                        Toast.makeText(getApplicationContext(), "Check Internet Connection", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    ParseUser.logOutInBackground(new LogOutCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                Intent intent = new Intent(ContentActivity.this, MainActivity.class);
                                ContentActivity.this.finish();
                                Utility.resetExistingList();
                                Utility.setChangedRecordFriend();
                                Utility.setChangedRecordStatement();
                                Utility.setChangedRecordDashboard();
                                startActivity(intent);
                            } else {
                                Toast.makeText(getApplicationContext(), getString(R.string.logout_failed), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    break;
            }
            mMenuDrawer.closeMenu(false);
        }
    };

    private boolean emailVerificationCheck(){
        if(!verification) {
            verification = user.getBoolean("emailVerified");
        }
        return verification;
    }

    private void emailNotVerifiedDialog(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(ContentActivity.this);
        final LinearLayout layout = new LinearLayout(ContentActivity.this);
        final TextView message = new TextView(ContentActivity.this);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams para = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        para.setMargins(20, 20, 20, 20);
        message.setText("You have to link and verify your email address in order to use this function");
        message.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        message.setLayoutParams(para);
        layout.addView(message);
        builder.setTitle("Warning");
        builder.setView(layout);
        builder.setPositiveButton("Okay", null);
        builder.setNegativeButton("Bypass it", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                verification = true;
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    public static class CategoryItem{
        public final String text;
        public final int icon;
        public final int icon_selected;
        public CategoryItem(String text, Integer icon , Integer icon_selected) {
            this.text = text;
            this.icon = icon;
            this.icon_selected = icon_selected;
        }
        @Override
        public String toString() {
            return text;
        }
    }





    //Related to Side-Menu
    private static class Item {
        String mTitle;
        int mIconRes;

        Item(String title, int iconRes) {
            mTitle = title;
            mIconRes = iconRes;
        }
    }

    private static class Category {
        String mTitle;
        Category(String title) {
            mTitle = title;
        }
    }

    private class MenuAdapter extends BaseAdapter {
        private List<Object> mItems;

        MenuAdapter(List<Object> items) {
            mItems = items;
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public Object getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemViewType(int position) { return getItem(position) instanceof Item ? 0 : 1; }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public boolean isEnabled(int position) {
            return getItem(position) instanceof Item;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            Object item = getItem(position);

            if (item instanceof Category) {
                if (v == null) {
                    v = getLayoutInflater().inflate(R.layout.menu_row_category, parent, false);
                }
                ((TextView) v).setText(((Category) item).mTitle);
            } else {
                if (v == null) {
                    v = getLayoutInflater().inflate(R.layout.menu_row_item, parent, false);
                }
                TextView tv = (TextView) v;
                tv.setText(((Item) item).mTitle);
                tv.setCompoundDrawablesWithIntrinsicBounds(((Item) item).mIconRes, 0, 0, 0);

                Drawable img = ContextCompat.getDrawable(getApplicationContext(), ((Item) item).mIconRes);
                img.setBounds(0, 0, 75, 75);
                tv.setCompoundDrawables(img, null, null, null);
            }

            v.setTag(R.id.mdActiveViewPosition, position);
            if (position == mActivePosition) {
                mMenuDrawer.setActiveView(v, position);
            }
            return v;
        }
    }

    protected class UpdateInBackground extends AsyncTask <Void, Void, Void> {
        private ProgressDialog dialog;
        private int type;

        public UpdateInBackground(Context activity, int type) {
            this.type = type;
            dialog = new ProgressDialog(activity);
        }

        @Override
        protected void onPreExecute() {
            if(type == ALL_REFRESH){ dialog.setMessage("Loading Data... Please Wait..."); }
            else if(type == DASHBOARD_REFRESH){ dialog.setMessage("Refreshing Dashboard... Please Wait..."); }
            else if(type == FRIEND_REFRESH){ dialog.setMessage("Refreshing Friend List... Please Wait..."); }
            else{ dialog.setMessage("Refreshing Statement List... Please Wait..."); }

            dialog.show();
            dialog.setCancelable(false);
            checkForUpdate.pause();
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                if(isNetworkConnected()){
                    if(type == ALL_REFRESH || type == STATEMENT_REFRESH){
                        Utility.generateRawStatementList(user);
                    }
                    if(type == ALL_REFRESH || type == FRIEND_REFRESH){
                        Utility.generateRawFriendList(user);
                    }
                    if(type == ALL_REFRESH || type == DASHBOARD_REFRESH){
                        Utility.setChangedRecordDashboard();
                        Utility.getDashboardData();
                    }
                    if(type == ALL_REFRESH) {
                        emailVerificationCheck();
                    }
                } else {
                    if(type == ALL_REFRESH || type == FRIEND_REFRESH){
                        Utility.generateFriendArrayOffline();
                    }
                    if(type == ALL_REFRESH || type == DASHBOARD_REFRESH){
                        Utility.getDashboardDataOffline();
                    }
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            HomepageFragment child_1 = (HomepageFragment) getSupportFragmentManager().findFragmentByTag("Home");
            DashboardFragment child_2 = (DashboardFragment) getSupportFragmentManager().findFragmentByTag("Dashboard");
            FriendsFragment child_3 = (FriendsFragment) getSupportFragmentManager().findFragmentByTag("Friend");
            ViewStatementsFragment child_4 = (ViewStatementsFragment) getSupportFragmentManager().findFragmentByTag("ViewStatements");
            if(child_1 != null){
                child_1.setBalance();
            }
            if(child_2 != null){
                child_2.notifyAdaptor();
            }
            if(child_3 != null){
                child_3.notifyAdaptor();
            }
            if(child_4 != null){
                child_4.notifyAdaptor();
            }

            if (dialog.isShowing()) {
                dialog.dismiss();
                if(isNetworkConnected()){ Toast.makeText(getApplicationContext(), "Data is updated!", Toast.LENGTH_SHORT).show(); }
                else{ Toast.makeText(getApplicationContext(), "Offline data is updated", Toast.LENGTH_SHORT).show(); }
            }
            checkForUpdate.resume();
        }
    }

    private class CheckForUpdate implements Runnable{
        private final Object pauseLock;
        private boolean paused;

        public CheckForUpdate() {
            pauseLock = new Object();
            paused = false;
        }

        @Override
        public void run() {
           while(true) try{
               SystemClock.sleep(60000);        //Check for update every minute
               if(paused){ continue; }
               Log.d("Update", "Check");
               if(Utility.checkNewEntryField()){
                   if(paused){ continue; }
                   Utility.generateRawStatementList(user);
                   if(paused){ continue; }
                   Utility.generateRawFriendList(user);
                   if(paused){ continue; }
                   Utility.setChangedRecordDashboard();
                   Utility.getDashboardData();
               }
           } catch (NullPointerException e){
               e.getStackTrace();
               break;
           }
        }

        public void pause(){
            synchronized (pauseLock) {
                Log.d("Update", "Paused");
                paused = true;
            }
        }

        public void resume(){
            synchronized (pauseLock) {
                Log.d("Update", "Resumed");
                paused = false;
                pauseLock.notifyAll();
            }
        }
    }
}

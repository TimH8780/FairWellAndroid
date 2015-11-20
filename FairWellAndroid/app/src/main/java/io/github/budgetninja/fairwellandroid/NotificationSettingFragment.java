package io.github.budgetninja.fairwellandroid;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.Toast;

import com.parse.ParseUser;

public class NotificationSettingFragment extends Fragment {

    private ParseUser user;
    private ContentActivity parent;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        user = ParseUser.getCurrentUser();
        parent = (ContentActivity)getActivity();
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_notification_setting, container, false);
        ActionBar actionBar = parent.getSupportActionBar();
        if(actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_keyboard_arrow_left);
        }
        parent.setTitle("Notification Setting");




        final CheckedTextView ctv = (CheckedTextView) rootView.findViewById(R.id.checkedTextView0);
        ctv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ctv.isChecked()) {
                    ctv.setChecked(false);
                    ((CheckedTextView) rootView.findViewById(R.id.checkedTextView1)).setChecked(false);
                    ((CheckedTextView) rootView.findViewById(R.id.checkedTextView2)).setChecked(false);
                    ((CheckedTextView) rootView.findViewById(R.id.checkedTextView3)).setChecked(false);
                    ((CheckedTextView) rootView.findViewById(R.id.checkedTextView4)).setChecked(false);
                    ((CheckedTextView) rootView.findViewById(R.id.checkedTextView5)).setChecked(false);
                    ((CheckedTextView) rootView.findViewById(R.id.checkedTextView6)).setChecked(false);
                    ((CheckedTextView) rootView.findViewById(R.id.checkedTextView7)).setChecked(false);
                    ((CheckedTextView) rootView.findViewById(R.id.checkedTextView8)).setChecked(false);
                    ((CheckedTextView) rootView.findViewById(R.id.checkedTextView9)).setChecked(false);


                } else {
                    ctv.setChecked(true);
                    ((CheckedTextView) rootView.findViewById(R.id.checkedTextView1)).setChecked(true);
                    ((CheckedTextView) rootView.findViewById(R.id.checkedTextView2)).setChecked(true);
                    ((CheckedTextView) rootView.findViewById(R.id.checkedTextView3)).setChecked(true);
                    ((CheckedTextView) rootView.findViewById(R.id.checkedTextView4)).setChecked(true);
                    ((CheckedTextView) rootView.findViewById(R.id.checkedTextView5)).setChecked(true);
                    ((CheckedTextView) rootView.findViewById(R.id.checkedTextView6)).setChecked(true);
                    ((CheckedTextView) rootView.findViewById(R.id.checkedTextView7)).setChecked(true);
                    ((CheckedTextView) rootView.findViewById(R.id.checkedTextView8)).setChecked(true);
                    ((CheckedTextView) rootView.findViewById(R.id.checkedTextView9)).setChecked(true);
                }
            }
        });

        final CheckedTextView ctv1 = (CheckedTextView) rootView.findViewById(R.id.checkedTextView1);
        ctv1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ctv1.isChecked()){
                    ctv1.setChecked(false);
                }
                else{
                    ctv1.setChecked(true);
                }
            }
        });

        final CheckedTextView ctv2 = (CheckedTextView) rootView.findViewById(R.id.checkedTextView2);
        ctv2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ctv2.isChecked()){
                    ctv2.setChecked(false);
                }
                else{
                    ctv2.setChecked(true);
                }
            }
        });

        final CheckedTextView ctv3 = (CheckedTextView) rootView.findViewById(R.id.checkedTextView3);
        ctv3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ctv3.isChecked()){
                    ctv3.setChecked(false);
                }
                else{
                    ctv3.setChecked(true);
                }
            }
        });

        final CheckedTextView ctv4 = (CheckedTextView) rootView.findViewById(R.id.checkedTextView4);
        ctv4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ctv4.isChecked()){
                    ctv4.setChecked(false);
                }
                else{
                    ctv4.setChecked(true);
                }
            }
        });

        final CheckedTextView ctv5 = (CheckedTextView) rootView.findViewById(R.id.checkedTextView5);
        ctv5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ctv5.isChecked()){
                    ctv5.setChecked(false);
                }
                else{
                    ctv5.setChecked(true);
                }
            }
        });


        final CheckedTextView ctv6 = (CheckedTextView) rootView.findViewById(R.id.checkedTextView6);
        ctv6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ctv6.isChecked()){
                    ctv6.setChecked(false);
                }
                else{
                    ctv6.setChecked(true);
                }
            }
        });

        final CheckedTextView ctv7 = (CheckedTextView) rootView.findViewById(R.id.checkedTextView7);
        ctv7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ctv7.isChecked()){
                    ctv7.setChecked(false);
                }
                else{
                    ctv7.setChecked(true);
                }
            }
        });

        final CheckedTextView ctv8 = (CheckedTextView) rootView.findViewById(R.id.checkedTextView8);
        ctv8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ctv8.isChecked()){
                    ctv8.setChecked(false);
                }
                else{
                    ctv8.setChecked(true);
                }
            }
        });

        final CheckedTextView ctv9 = (CheckedTextView) rootView.findViewById(R.id.checkedTextView9);
        ctv9.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ctv9.isChecked()){
                    ctv9.setChecked(false);
                }
                else{
                    ctv9.setChecked(true);
                }
            }
        });



        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem item = menu.findItem(R.id.action_refresh);
        item.setVisible(false);
        inflater.inflate(R.menu.menu_setting, menu);
        super.onCreateOptionsMenu(menu, inflater);
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
}

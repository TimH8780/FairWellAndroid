package io.github.budgetninja.fairwellandroid;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.widget.Toolbar;
//import android.support.v7fix.preference.PreferenceFragmentCompatFix;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

public class AccountSettingFragment extends PreferenceFragmentCompat {

    private ContentActivity parent;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.activity_account_setting);
        setHasOptionsMenu(true);

        parent = (ContentActivity)getActivity();
        ActionBar actionBar = parent.getSupportActionBar();
        if(actionBar != null) {
            actionBar.setHomeAsUpIndicator(null);
        }
        getActivity().setTitle("Account Setting");

        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());

        String strUserName = SP.getString("username", "NA");
        String strPassword = SP.getString("password", "NA");
        boolean notification = SP.getBoolean("notification", false);
        String ringtone = SP.getString("ringtone", "1");

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

package io.github.budgetninja.fairwellandroid;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.MenuItem;


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
        parent.setTitle("Account Setting");

        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(parent.getBaseContext());

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

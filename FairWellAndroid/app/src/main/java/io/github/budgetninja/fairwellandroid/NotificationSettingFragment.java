package io.github.budgetninja.fairwellandroid;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.MenuItem;

public class NotificationSettingFragment extends PreferenceFragmentCompat {

    private ContentActivity parent;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.activity_notification_setting);
        setHasOptionsMenu(true);

        parent = (ContentActivity)getActivity();
        ActionBar actionBar = parent.getSupportActionBar();
        if(actionBar != null) {
            actionBar.setHomeAsUpIndicator(null);
        }
        parent.setTitle("Notification Setting");

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

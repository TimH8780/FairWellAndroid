package io.github.budgetninja.fairwellandroid;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;


public class AccountSettingFragment extends Fragment {

    private ParseUser user;
    private ContentActivity parent;
    private View rootView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        user = ParseUser.getCurrentUser();
        parent = (ContentActivity)getActivity();
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem item = menu.findItem(R.id.action_refresh);
        item.setVisible(false);
        inflater.inflate(R.menu.menu_setting, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_account_setting, container, false);
        ActionBar actionBar = parent.getSupportActionBar();
        if(actionBar != null) {
            final Drawable upArrow = ContextCompat.getDrawable(getContext(), R.drawable.abc_ic_ab_back_mtrl_am_alpha);
            upArrow.setColorFilter(ContextCompat.getColor(getContext(), R.color.coolBackground), PorterDuff.Mode.SRC_ATOP);
            actionBar.setHomeAsUpIndicator(upArrow);
        }
        parent.setTitle("Account Setting");
        ((TextView) rootView.findViewById(R.id.email)).setText((user.getEmail()));
        if(user.get("profileName")==null||((String)(user.get("profileName"))).isEmpty()) {
            String profileNameString = user.get("First_Name") + " " + user.get("Last_Name");
            ((TextView) rootView.findViewById(R.id.profile_name_view)).setText(profileNameString);
        }else{
            ((TextView) rootView.findViewById(R.id.profile_name_view)).setText((String)(user.get("profileName")));
        }
        return rootView;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                parent.mMenuDrawer.closeMenu(false);
                parent.fragMgr.popBackStack();
                return true;
            case R.id.action_save:
                saveAccountSetting();
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    private void saveAccountSetting(){
        if(user!=null&&parent!=null){
            showProgressBar();
            String profileNameString = ((EditText)rootView.findViewById(R.id.profile_name)).getText().toString();
            final String firstNameString = ((EditText)rootView.findViewById(R.id.first_name)).getText().toString();
            final String lastNameString = ((EditText)rootView.findViewById(R.id.last_name)).getText().toString();
            String phoneNumberString = ((EditText)rootView.findViewById(R.id.phone_number)).getText().toString();
            String addressLine1String = ((EditText)rootView.findViewById(R.id.address_line_1)).getText().toString();
            String addressLine2String = ((EditText)rootView.findViewById(R.id.address_line_2)).getText().toString();
            String selfDescriptionString = ((EditText)rootView.findViewById(R.id.self_description)).getText().toString();
            if(!profileNameString.isEmpty()) {
                user.put("profileName", profileNameString);
            }else{
                user.put("profileName", firstNameString+" "+lastNameString);
            }
            user.put("First_Name",firstNameString);
            user.put("Last_Name",lastNameString);
            user.put("phoneNumber",phoneNumberString);
            user.put("addressLine1",addressLine1String);
            user.put("addressLine2",addressLine2String);
            user.put("selfDescription",selfDescriptionString);
            user.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    Toast.makeText(getContext(),"Data Saved",Toast.LENGTH_SHORT).show();
                    if(user.get("profileName")==null||((String)(user.get("profileName"))).isEmpty()) {
                        String profileNameString = user.get("First_Name") + " " + user.get("Last_Name");
                        ((TextView) rootView.findViewById(R.id.profile_name_view)).setText(profileNameString);
                    }else{
                        ((TextView) rootView.findViewById(R.id.profile_name_view)).setText((String)(user.get("profileName")));
                    }
                    hideProgressBar();
                }
            });

        }
    }

    private void showProgressBar(){
        View progressView = getActivity().findViewById(R.id.loadingPanel);
        if(progressView != null){
            progressView.setVisibility(View.VISIBLE);
        }
    }

    private void hideProgressBar(){
        View progressView = getActivity().findViewById(R.id.loadingPanel);
        if(progressView != null){
            progressView.setVisibility(View.GONE);
        }
    }
}

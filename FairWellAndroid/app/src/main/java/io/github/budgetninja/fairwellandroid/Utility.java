package io.github.budgetninja.fairwellandroid;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.DatePicker;

import com.parse.ParseException;
import com.parse.ParseUser;

import java.util.Calendar;

/**
 * Created by Issac on 9/23/2015.
 */
public class Utility {
    public static boolean isNormalUser(ParseUser user) {
        if (!isFacebookUser(user) && !isTwitterUser(user)) {
            return true;
        }
        return false;
    }
    public static boolean isFacebookUser(ParseUser user){
        try {
            if (user.fetchIfNeeded().get("usernameFacebook") != null) {
                return true;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
    }
    public static boolean isTwitterUser(ParseUser user){
        try {
            if (user.fetchIfNeeded().get("usernameTwitter") != null) {
                return true;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
    }
    public static class DatePickerFragment extends DialogFragment
            implements DatePickerDialog.OnDateSetListener {

        private int num;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);
            Bundle arg = getArguments();
            num = arg.getInt("View");

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {
            ((AddStatementFragment)getActivity().getSupportFragmentManager().findFragmentByTag("Add")).setDate(year, month, day, num);
        }
    }

}

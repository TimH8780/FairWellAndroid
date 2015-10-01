package io.github.budgetninja.fairwellandroid;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.InputType;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.ParseUser;

import java.util.ArrayList;
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

    public static String getName(ParseUser user){
        String nameString;
        if(isFacebookUser(user)){
            nameString = user.getString("usernameFacebook");
        }else if(isTwitterUser(user)){
            nameString = user.getString("usernameTwitter");
        }else {
            nameString = user.getString("First_Name") + " " + user.getString("Last_Name");
        }
        return nameString;
    }

    public static class DatePickerFragment extends DialogFragment
            implements DatePickerDialog.OnDateSetListener {

        private int viewSel;
        private static final int YEAR = 0;
        private static final int MONTH = 1;
        private static final int DAY = 2;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Bundle args = getArguments();
            viewSel = args.getInt("ViewSel");
            ArrayList<Integer> dateList = args.getIntegerArrayList("DateList");

            int year, month, day;
            if (dateList.get(viewSel + YEAR) <= 1899 || dateList.get(viewSel + YEAR) >= 2101) {
                // Use the current date as the default date in the picker
                final Calendar c = Calendar.getInstance();
                year = c.get(Calendar.YEAR);
                month = c.get(Calendar.MONTH);
                day = c.get(Calendar.DAY_OF_MONTH);
            }
            else{
                year = dateList.get(viewSel + YEAR);
                month = dateList.get(viewSel + MONTH);
                day = dateList.get(viewSel + DAY);
            }

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {
            ((AddStatementFragment)getActivity().getSupportFragmentManager().findFragmentByTag("Add")).setDate(year, month, day, viewSel);
        }
    }

}

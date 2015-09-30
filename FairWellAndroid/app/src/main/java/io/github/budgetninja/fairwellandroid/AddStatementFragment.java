package io.github.budgetninja.fairwellandroid;

import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class AddStatementFragment extends Fragment {

    private TextView ClickedText;
    private TextView deadlineField;
    private TextView dateField;
    private ArrayList<Integer> dateRecord;
    private int editTextLength;
    private static final int DATE = 0;
    private static final int DEADLINE = 3;
    private static final int YEAR = 0;
    private static final int MONTH = 1;
    private static final int DAY = 2;


    public AddStatementFragment() {
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setHasOptionsMenu(true);        //force to recreate optionMenu
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_add_statement, container, false);

        ((ContainerActivity) getActivity()).setTitle("Add Statement");
        editTextLength = 4;

        dateRecord = new ArrayList<>(6);
        dateRecord.add(DATE + YEAR, 1899);
        dateRecord.add(DATE + MONTH, 1);
        dateRecord.add(DATE + DAY, 1);
        dateRecord.add(DEADLINE + YEAR, 2101);
        dateRecord.add(DEADLINE + MONTH, 12);
        dateRecord.add(DEADLINE + DAY, 31);

        Spinner spinner = (Spinner) rootView.findViewById(R.id.spinner);
        Spinner spinner2 = (Spinner) rootView.findViewById(R.id.spinner2);
        final EditText moneyAmount = (EditText) rootView.findViewById(R.id.moneyAmount);
        ClickedText = (TextView) rootView.findViewById(R.id.clickText);
        dateField = (TextView) rootView.findViewById(R.id.dateField);
        deadlineField = (TextView) rootView.findViewById(R.id.deadlineField);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.name_array, android.R.layout.simple_spinner_item);
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(getContext(),
                R.array.mode_array, android.R.layout.simple_spinner_item);

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        spinner2.setAdapter(adapter2);

        Button closeButton = (Button) rootView.findViewById(R.id.confirmButton);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "Fairwell will send notification to the party members! "
                        , Toast.LENGTH_SHORT).show();
                getActivity().finish();
            }
        });

        dateField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(v, DATE);
            }
        });

        deadlineField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(v, DEADLINE);
            }
        });

        moneyAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }      //do nothing
            @Override
            public void afterTextChanged(Editable s) { }        //do nothing

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String temp = moneyAmount.getText().toString();
                if(temp.length() == 0){ return; }
                if (temp.contains(".")) {
                    int dotPos = temp.indexOf(".");
                    if (dotPos < temp.length() - 3) {
                        moneyAmount.setText(temp.substring(0, dotPos + 3));
                    }
                }
                if (temp.length() == 2 && temp.charAt(0) == '0' && temp.charAt(1) != '.') {
                    moneyAmount.setText(temp.substring(1, temp.length()));
                }
                if (temp.charAt(0) == '.') {
                    moneyAmount.setText("0.");
                }
                moneyAmount.setSelection(moneyAmount.getText().length());
            }
        });

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_add_statement, menu);
    }

    public void showDatePickerDialog(View v, int args) {
        Bundle arg = new Bundle();
        arg.putInt("View", args);
        DialogFragment newFragment = new Utility.DatePickerFragment();
        newFragment.setArguments(arg);
        newFragment.show(getFragmentManager(), "datePicker");
    }

    public void setClickedText(String string) {
        if (!string.equals("")) {
            ClickedText.setText(string + " is selected");
        }
    }

    public void setDate(int year, int month, int day, int view) {
        if(dateCheck(year, month, day, view)) {
            dateRecord.set(view + YEAR, year);
            dateRecord.set(view + MONTH, month);
            dateRecord.set(view + DAY, day);

            StringBuilder data = new StringBuilder("");
            data.append(month + 1).append("/").append(day).append("/").append(year);

            if (view == DATE) {
                dateField.setText(data.toString());
            } else if (view == DEADLINE) {
                deadlineField.setText(data.toString());
            }
            return;
        }
        Toast.makeText(getActivity().getApplicationContext(), "Invalid Input: 'Deadline' must be after 'Date'"
                , Toast.LENGTH_SHORT).show();
    }


    public boolean dateCheck(int year, int month, int day, int view) {
        Boolean result = (view == DATE);
        view = (view == DATE) ? DEADLINE : DATE;
        if (dateRecord.get(view + YEAR) > year) { return result; }
        if (dateRecord.get(view + YEAR) < year) { return !result; }
        if (dateRecord.get(view + MONTH) > month) { return result; }
        if (dateRecord.get(view + MONTH) < month) { return !result; }
        if (dateRecord.get(view + DAY) > day) { return result; }
        if((dateRecord.get(view + DAY) == day)) { return false; }       //special case
        return !result;
    }
}

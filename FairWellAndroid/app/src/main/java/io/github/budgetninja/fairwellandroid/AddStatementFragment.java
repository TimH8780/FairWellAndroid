package io.github.budgetninja.fairwellandroid;

import android.app.DatePickerDialog;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/**
 * A placeholder fragment containing a simple view.
 */
public class AddStatementFragment extends Fragment {

    private TextView ClickedText;
    private TextView deadlineField;
    private TextView dateField;
    private static final int date = 0;
    private static final int deadline = 1;


    public AddStatementFragment() {
    }

    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        setHasOptionsMenu(true);        //force to recreate optionMenu
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_add_statement, container, false);

        ((ContainerActivity)getActivity()).setTitle("Add Statement");

        Spinner spinner = (Spinner) rootView.findViewById(R.id.spinner);
        Spinner spinner2 = (Spinner) rootView.findViewById(R.id.spinner2);
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
                showDatePickerDialog(v, date);
            }
        });

        deadlineField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(v, deadline);
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

    public void setClickedText(String string){
        if(!string.equals("")) {
            ClickedText.setText(string + " is selected");
        }
    }

    public void setDate(int year, int month, int day, int view){
        StringBuilder data = new StringBuilder("");
        data.append(month+1).append("/").append(day).append("/").append(year);
        if(view == date){ dateField.setText(data.toString()); }
        else if (view == deadline) { deadlineField.setText(data.toString()); }
    }

}

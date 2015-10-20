package io.github.budgetninja.fairwellandroid;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class AddStatementFragment extends Fragment {

    private TextView ClickedText;
    private TextView deadlineField;
    private TextView dateField;
    private Button addSnapshotButton;
    private Button addMemberButton;
    private Button closeButton;
    private ArrayList<Integer> dateRecord;

    private ParseUser user;
    private int paidByPosition;
    private List<Utility.Friend> friendList;

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
        getActivity().setTitle("Add Statement");
        user = ParseUser.getCurrentUser();
        paidByPosition = 0;

        // An array used to record the date set by user for DATE and DEADLINE
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
        addMemberButton = (Button) rootView.findViewById(R.id.addMemberButton);
        addSnapshotButton = (Button) rootView.findViewById(R.id.addSnapshotButton);
        ClickedText = (TextView) rootView.findViewById(R.id.clickText);
        dateField = (TextView) rootView.findViewById(R.id.dateField);
        deadlineField = (TextView) rootView.findViewById(R.id.deadlineField);

        if(((ContainerActivity)getActivity()).isNetworkConnected()) {
            friendList = new ArrayList<>(Utility.generateFriendArray());
        }
        else {
            friendList = new ArrayList<>(Utility.generateFriendArrayOffline());
        }
        friendList.add(0, new Utility.Friend(null, user, "Self", null, -1, -1, true, true)); //user her/himself

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<Utility.Friend> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, friendList);
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(getContext(),
                R.array.mode_array, android.R.layout.simple_spinner_item);

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        spinner2.setAdapter(adapter2);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                paidByPosition = position;
                //Then reset "Member"
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { /* do nothing */ }
        });

        closeButton = (Button) rootView.findViewById(R.id.confirmButton);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!((ContainerActivity)getActivity()).isNetworkConnected()){
                    Toast.makeText(getActivity().getApplicationContext(), "Check Internet Connection", Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(getContext(), "Fairwell will send notification to the party members! "
                        , Toast.LENGTH_SHORT).show();
                getActivity().finish();
            }
        });

        dateField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(DATE);
            }
        });

        deadlineField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(DEADLINE);
            }
        });

        moneyAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* do nothing */ }

            @Override
            public void afterTextChanged(Editable s) { /* do nothing */ }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String temp = moneyAmount.getText().toString();
                if (temp.length() == 0) {
                    return;
                }
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

        addMemberButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                ListView container = new ListView(getActivity());
                MemberSelectionAdaptor memberAdaptor;
                if(paidByPosition == 0) {
                    memberAdaptor = new MemberSelectionAdaptor(getContext(), R.layout.addmember_item, friendList);
                }
                else {
                    memberAdaptor = new MemberSelectionAdaptor(getContext(), R.layout.addmember_item, friendList.subList(0, 1));
                }
                container.setAdapter(memberAdaptor);
                builder.setTitle("Select Member(s)");
                builder.setView(container);
                builder.setPositiveButton("Select", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // do something
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                final AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        addSnapshotButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity().getApplicationContext(), "Unavailable now"
                        , Toast.LENGTH_SHORT).show();
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

    public void showDatePickerDialog(int args) {
        Bundle arg = new Bundle();
        arg.putInt("ViewSel", args);
        arg.putIntegerArrayList("DateList", dateRecord);
        DialogFragment newFragment = new Utility.DatePickerFragment();
        newFragment.setArguments(arg);
        newFragment.show(getFragmentManager(), "datePicker");
    }

    public void setClickedIconText(String string) {
        if (!string.equals("")) {
            ClickedText.setText("Category: " + string);
        }
    }

    public void setDate(int year, int month, int day, int viewSel) {
        if(isValidDate(year, month, day, viewSel)) {
            dateRecord.set(viewSel + YEAR, year);
            dateRecord.set(viewSel + MONTH, month);
            dateRecord.set(viewSel + DAY, day);

            StringBuilder data = new StringBuilder("");
            data.append(String.format("%02d",month + 1)).append("/").append(String.format("%02d", day)).append("/").append(year);


            if (viewSel == DATE) {
                dateField.setText(data.toString());
            } else if (viewSel == DEADLINE) {
                deadlineField.setText(data.toString());
            }
            return;
        }
        Toast.makeText(getActivity().getApplicationContext(), "Invalid Input:'Deadline' must be after 'Date'"
                , Toast.LENGTH_SHORT).show();
    }

    public boolean isValidDate(int year, int month, int day, int view) {
        Boolean result = (view == DATE);
        view = (view == DATE) ? DEADLINE : DATE;
        if (dateRecord.get(view + YEAR) > year) { return result; }
        if (dateRecord.get(view + YEAR) < year) { return !result; }
        if (dateRecord.get(view + MONTH) > month) { return result; }
        if (dateRecord.get(view + MONTH) < month) { return !result; }
        if (dateRecord.get(view + DAY) > day) { return result; }
        return (dateRecord.get(view + DAY) != day) && (!result);
    }

    private class MemberSelectionAdaptor extends ArrayAdapter<Utility.Friend>{

        Context mContext;
        int mResource;
        List<Utility.Friend> mObject;

        public MemberSelectionAdaptor(Context context, int resource, List<Utility.Friend> objects){
            super(context, resource, objects);
            mContext = context;
            mResource = resource;
            mObject = objects;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            Utility.Friend currentItem = mObject.get(position);
            if(convertView == null){
                convertView = getActivity().getLayoutInflater().inflate(mResource, parent, false);
            }
            TextView memberName = (TextView) convertView.findViewById(R.id.memberName);
            memberName.setText(currentItem.name);
            //Listener
            return convertView;
        }
    }

}

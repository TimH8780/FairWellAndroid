package io.github.budgetninja.fairwellandroid;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseUser;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import io.github.budgetninja.fairwellandroid.FriendObject.Friend;
import static io.github.budgetninja.fairwellandroid.ContentActivity.INDEX_STATEMENT_SUMMARY;

/**
 * A placeholder fragment containing a simple view.
 */
public class AddStatementFragment extends Fragment {

    private TextView clickedText;
    private TextView description;
    private TextView deadlineField;
    private TextView dateField;
    private EditText moneyAmount;
    private LinearLayout layoutMemberDisplay;
    private DateFormat format;
    private static ArrayList<Integer> dateRecord;
    private static int viewSel;
    private int counter;
    private int maxCapacity;

    private ParseUser user;
    private ContentActivity parent;
    private int paidByPosition;
    private int modePosition;
    private Boolean[] friendSelected;
    private List<Friend> friendList;
    private List<Friend> selectedMember;
    private ViewStatementSummaryListener callBack;

    private static final int DATE = 0;
    private static final int DEADLINE = 3;
    private static final int YEAR = 0;
    private static final int MONTH = 1;
    private static final int DAY = 2;
    private static final int SELF = 0;
    public static final int SPLIT_EQUALLY = 0;
    public static final int BY_PERCENTAGE = 1;
    public static final int BY_RATIO = 2;


    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setHasOptionsMenu(true);

        maxCapacity = -1;
        counter = -1;
        parent = (ContentActivity)getActivity();
        user = ParseUser.getCurrentUser();
        paidByPosition = SELF;
        modePosition = SPLIT_EQUALLY;
        format = new SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH);

        if(parent.isNetworkConnected()) { friendList = new ArrayList<>(Utility.generateFriendArray()); }
        else { friendList = new ArrayList<>(Utility.generateFriendArrayOffline()); }
        friendList.add(0, new Friend(null, user, "Self", null, -1, -1, true, true)); //user her/himself
        friendSelected = new Boolean[friendList.size()];

        // An array used to record the date set by user for DATE and DEADLINE
        dateRecord = new ArrayList<>(6);
        dateRecord.add(DATE + YEAR, 1899);
        dateRecord.add(DATE + MONTH, 1);
        dateRecord.add(DATE + DAY, 1);
        dateRecord.add(DEADLINE + YEAR, 2101);
        dateRecord.add(DEADLINE + MONTH, 12);
        dateRecord.add(DEADLINE + DAY, 31);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_add_statement, container, false);
        ActionBar actionBar = parent.getSupportActionBar();
        if(actionBar != null) {
            actionBar.setHomeAsUpIndicator(null);
        }
        parent.setTitle("Add Statement");

        Spinner paidBySpinner = (Spinner) rootView.findViewById(R.id.spinner);
        Spinner modeSpinner = (Spinner) rootView.findViewById(R.id.spinner2);
        moneyAmount = (EditText) rootView.findViewById(R.id.moneyAmount);
        Button addMemberButton = (Button) rootView.findViewById(R.id.addMemberButton);
        Button addSnapshotButton = (Button) rootView.findViewById(R.id.addSnapshotButton);
        Button confirmButton = (Button) rootView.findViewById(R.id.confirmButton);
        description = (TextView) rootView.findViewById(R.id.statement_description);
        clickedText = (TextView) rootView.findViewById(R.id.clickText);
        dateField = (TextView) rootView.findViewById(R.id.dateField);
        deadlineField = (TextView) rootView.findViewById(R.id.deadlineField);
        layoutMemberDisplay = (LinearLayout) rootView.findViewById(R.id.layout_member_display);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<Friend> paidByAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, friendList);
        ArrayAdapter<CharSequence> modeAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.mode_array, android.R.layout.simple_spinner_item);

        // Specify the layout to use when the list of choices appears
        paidByAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        paidBySpinner.setAdapter(paidByAdapter);
        modeSpinner.setAdapter(modeAdapter);

        paidBySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                paidByPosition = position;
                friendSelected = new Boolean[friendList.size()];
                selectedMember = new ArrayList<>();
                displayMemberSelected();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { /* do nothing */ }
        });

        modeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                modePosition = position;
                friendSelected = new Boolean[friendList.size()];
                selectedMember = new ArrayList<>();
                displayMemberSelected();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { /* do nothing */ }
        });

        confirmButton.setOnClickListener(confirmButtonListener);
        addMemberButton.setOnClickListener(addMemberButtonListener);
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

        addSnapshotButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(parent, "Unavailable now", Toast.LENGTH_SHORT).show();
            }
        });

        return rootView;
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

    @Override
    public void onAttach(Context context){
        super.onAttach(context);

        if(context instanceof ContentActivity) {
            try {
                callBack = (ViewStatementSummaryListener) context;
            } catch (ClassCastException e) {
                throw new ClassCastException(context.toString() + " must implement this listener");
            }
        }
    }

    public interface ViewStatementSummaryListener{
        void statementData(String description, String category, String date, String deadline, int mode, int totalPeople,
                           String amount, Friend payee, List<Friend> payer);
    }

    protected void setClickedIconText(String string) {
        if (!string.equals("")) {
            clickedText.setText("Category: " + string);
        }
    }

    private void displayMemberSelected(){
        layoutMemberDisplay.removeAllViews();
        int counter = selectedMember.size();
        if(counter != 0) {
            TextView text = new TextView(parent);
            text.setTextColor(Color.RED);
            text.setGravity(Gravity.CENTER_HORIZONTAL);
            text.setTypeface(null, Typeface.BOLD);
            text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            TextView memberName = new TextView(parent);
            memberName.setGravity(Gravity.CENTER_HORIZONTAL);

            StringBuilder data = new StringBuilder("");
            for (int i = 0; i < counter; i++) {
                data.append(selectedMember.get(i).name).append("\n");
            }
            if(this.counter > 0){
                text.setText("Selected Member [" + Integer.toString(counter) + "+" + Integer.toString(this.counter) + "]");
                data.append("(").append(this.counter).append(" Unknown People)").append("\n");
            } else{
                text.setText("Selected Member [" + Integer.toString(counter) + "]");
            }
            memberName.setText(data.toString());
            layoutMemberDisplay.addView(text);
            layoutMemberDisplay.addView(memberName);
        }
    }

    private View.OnClickListener confirmButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!parent.isNetworkConnected()) {
                Toast.makeText(parent, "Check Internet Connection", Toast.LENGTH_SHORT).show();
                return;
            }
            String descr = description.getText().toString();
            String categ = clickedText.getText().toString();
            String amount = moneyAmount.getText().toString();
            String date = dateField.getText().toString();
            String deadline = deadlineField.getText().toString();
            Boolean member = selectedMember.isEmpty();

            if(!descr.equals("") && !categ.equals("") && !amount.equals("") && !date.equals("") && !deadline.equals("") && !member){
                parent.layoutManage(INDEX_STATEMENT_SUMMARY);
                Friend payee = (paidByPosition == 0) ? null : friendList.get(paidByPosition);
                callBack.statementData(descr, categ.substring(10), date, deadline, modePosition, maxCapacity, amount, payee, selectedMember);
            } else {
                Toast.makeText(getContext(), "Please fill in all information with SnapShot as optional", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private View.OnClickListener addMemberButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (modePosition) {
                case SPLIT_EQUALLY:
                    final AlertDialog.Builder builder = new AlertDialog.Builder(parent);
                    LinearLayout linearLayout = new LinearLayout(parent);
                    linearLayout.setOrientation(LinearLayout.HORIZONTAL);
                    linearLayout.setPadding(20, 20, 20, 20);
                    TextView textView = new TextView(parent);
                    textView.setText("Number of People Involved:  ");
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
                    final EditText editText = new EditText(parent);
                    editText.setHint("Input");
                    editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                    editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
                    editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2)});
                    linearLayout.addView(textView);
                    linearLayout.addView(editText);
                    builder.setTitle("Select Member(s)");
                    builder.setView(linearLayout);
                    builder.setPositiveButton("Next", null);
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    final AlertDialog dialog = builder.create();

                    dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(DialogInterface dialogInterface) {
                            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                            button.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    String text = editText.getText().toString();
                                    if (text.equals("")) {
                                        Toast.makeText(parent, "Please enter number greater than 0", Toast.LENGTH_SHORT).show();
                                    } else {
                                        int num = Integer.valueOf(text);
                                        if (num < 1) {
                                            editText.setText("");
                                            Toast.makeText(parent, "Please enter number greater than 0", Toast.LENGTH_SHORT).show();
                                        } else {
                                            dialog.dismiss();
                                            showMemberSelectionList(num);
                                        }
                                    }
                                }
                            });
                        }
                    });
                    dialog.show();
                    break;

                case BY_PERCENTAGE:
                    showMemberSelectionList(-1);
                    break;

                case BY_RATIO:
                    showMemberSelectionList(-1);
                    break;
            }
        }
    };

    private void showMemberSelectionList(final int capacity){
        final AlertDialog.Builder builder = new AlertDialog.Builder(parent);
        final TextView capacityText = new TextView(parent);
        final Boolean[] result = new Boolean[friendList.size()];
        ListView container = new ListView(parent);
        maxCapacity = capacity;
        counter = capacity;
        if(capacity > 0){
            TextView textView = new TextView(parent);
            textView.setText("Maximum Capacity: ");
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
            capacityText.setText(Integer.toString(capacity));
            capacityText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
            LinearLayout linearLayout = new LinearLayout(parent);
            linearLayout.setHorizontalGravity(Gravity.CENTER_HORIZONTAL);
            linearLayout.setOrientation(LinearLayout.HORIZONTAL);
            linearLayout.addView(textView);
            linearLayout.addView(capacityText);
            linearLayout.setPadding(10, 10, 10, 10);
            LinearLayout layout = new LinearLayout(parent);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.addView(linearLayout);
            layout.addView(container);
            builder.setView(layout);
        } else {
            builder.setView(container);
        }
        MemberSelectionAdaptor memberAdaptor = new MemberSelectionAdaptor(getContext(), R.layout.item_add_member, friendList);
        container.setAdapter(memberAdaptor);
        container.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CheckBox checkBox = (CheckBox) view.findViewById(R.id.memberCheckBox);
                if (checkBox.isChecked()) {
                    result[position] = false;
                    checkBox.setChecked(false);
                    if (counter >= 0) {
                        capacityText.setText(Integer.toString(++counter));
                    }
                } else if (counter != 0 && (maxCapacity != 1 || paidByPosition != position)) {
                    result[position] = true;
                    checkBox.setChecked(true);
                    if (counter > 0) {
                        capacityText.setText(Integer.toString(--counter));
                    }
                }
            }
        });
        builder.setTitle("Select Member(s)");
        builder.setPositiveButton("Select", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                friendSelected = Arrays.copyOf(result, result.length);
                selectedMember = new ArrayList<>();
                for (int i = 0; i < result.length; i++) {
                    if (friendSelected[i] != null) {
                        if (friendSelected[i]) {
                            selectedMember.add(friendList.get(i));
                        }
                    }
                }
                displayMemberSelected();
            }
        });
        builder.setNegativeButton("Cancel", null);
        final AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showDatePickerDialog(int args) {
        viewSel = args;
        DialogFragment newFragment = new DatePickerFragment();
        newFragment.show(getFragmentManager(), "datePicker");
    }

    private void setDate(int year, int month, int day, int viewSel) {
        if(isValidDate(year, month, day, viewSel)) {
            dateRecord.set(viewSel + YEAR, year);
            dateRecord.set(viewSel + MONTH, month);
            dateRecord.set(viewSel + DAY, day);

            StringBuilder data = new StringBuilder("");
            data.append(String.format("%02d",month + 1)).append("/").append(String.format("%02d", day)).append("/").append(year);

            if (viewSel == DATE) {
                dateField.setText(data.toString());
            } else {
                deadlineField.setText(data.toString());
            }
            return;
        }
        Toast.makeText(parent, "Invalid Date Selection", Toast.LENGTH_SHORT).show();
    }

    private boolean isValidDate(int year, int month, int day, int view) {
        Boolean result = (view == DATE);
        view = (view == DATE) ? DEADLINE : DATE;
        if (dateRecord.get(view + YEAR) > year) { return result; }
        if (dateRecord.get(view + YEAR) < year) { return !result; }
        if (dateRecord.get(view + MONTH) > month) { return result; }
        if (dateRecord.get(view + MONTH) < month) { return !result; }
        if (dateRecord.get(view + DAY) > day) { return result; }
        return (dateRecord.get(view + DAY) != day) && (!result);
    }

    private class MemberSelectionAdaptor extends ArrayAdapter<Friend>{

        Context mContext;
        int mResource;
        List<Friend> mObject;

        public MemberSelectionAdaptor(Context context, int resource, List<Friend> objects){
            super(context, resource, objects);
            mContext = context;
            mResource = resource;
            mObject = objects;
        }

        private class ViewHolder{
            TextView nameText;
            CheckBox box;
            int position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parentGroup){
            Friend currentItem = mObject.get(position);
            final ViewHolder viewHolder;
            if(convertView == null){
                convertView = parent.getLayoutInflater().inflate(mResource, parentGroup, false);
                viewHolder = new ViewHolder();
                viewHolder.nameText = (TextView) convertView.findViewById(R.id.memberName);
                viewHolder.box = (CheckBox) convertView.findViewById(R.id.memberCheckBox);
                convertView.setTag(viewHolder);
            } else {
               viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.position = position;
            viewHolder.nameText.setText(currentItem.name);
            viewHolder.box.setChecked(false);

            return convertView;
        }
    }

    public static class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

        private int viewIndex;

        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            viewIndex = viewSel;
            int year, month, day;
            if (dateRecord.get(viewSel + YEAR) >= 1900 && dateRecord.get(viewSel + YEAR) <= 2100) {
                // Use selected date as the default date in the picker
                year = dateRecord.get(viewSel + YEAR);
                month = dateRecord.get(viewSel + MONTH);
                day = dateRecord.get(viewSel + DAY);
            } else {
                // Use the current date as the default date in the picker
                final Calendar c = Calendar.getInstance();
                year = c.get(Calendar.YEAR);
                month = c.get(Calendar.MONTH);
                day = c.get(Calendar.DAY_OF_MONTH);
            }
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {
            ((AddStatementFragment)getActivity().getSupportFragmentManager().findFragmentByTag("Add")).setDate(year, month, day, viewIndex);
        }
    }

}

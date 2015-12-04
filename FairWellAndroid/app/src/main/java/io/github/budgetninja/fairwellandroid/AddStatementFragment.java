package io.github.budgetninja.fairwellandroid;


import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.renderscript.ScriptGroup;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import io.github.budgetninja.fairwellandroid.FriendObject.Friend;
import io.github.budgetninja.fairwellandroid.StatementObject.SummaryStatement;

import static io.github.budgetninja.fairwellandroid.ContentActivity.INDEX_SUBMIT_STATEMENT_SUMMARY;

/**
 * A placeholder fragment containing a simple view.
 */
public class AddStatementFragment extends Fragment {

    private View previousState;
    private boolean pageCheck;
    private boolean isAmountChanged;
    private TextView clickedText;
    private TextView capacityText;
    private Button deadlineFieldButton;
    private Button dateFieldButton;
    private EditText description;
    private EditText moneyAmount;
    private LinearLayout layoutMemberDisplay;
    private DateFormat format;
    private static ArrayList<Integer> dateRecord;
    private static int viewSel;
    private double counter;
    private double maxCapacity;

    private ContentActivity parent;
    private int paidByPosition;
    private int modePosition;
    private String previousMoneyAmount;
    private Double[] friendSelected;
    private Double[] tempResult;
    private List<Friend> friendList;
    private List<Pair<Friend, Double>> selectedMember;

    private static final int DATE = 0;
    private static final int DEADLINE = 3;
    private static final int YEAR = 0;
    private static final int MONTH = 1;
    private static final int DAY = 2;
    private static final int SELF = 0;
    private static final int PAYER_HINT = -1;
    public static final int MODE_HINT = -1;
    public static final int SPLIT_EQUALLY = 0;
    public static final int SPLIT_UNEQUALLY = 1;
    public static final int SPLIT_BY_RATIO = 2;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem item = menu.findItem(R.id.action_refresh);
        item.setVisible(false);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setHasOptionsMenu(true);

        pageCheck = false;
        isAmountChanged = false;
        previousState = null;
        maxCapacity = -1;
        counter = -1;
        previousMoneyAmount = "";
        parent = (ContentActivity)getActivity();
        ParseUser user = ParseUser.getCurrentUser();
        format = new SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH);
        paidByPosition = PAYER_HINT;
        modePosition = MODE_HINT;

        List<Friend> temp;
        friendList = new ArrayList<>();
        if(parent.isNetworkConnected()) { temp = new ArrayList<>(Utility.generateFriendArray()); }
        else { temp = new ArrayList<>(Utility.generateFriendArrayOffline()); }

        for(int i = 0; i < temp.size(); i++){
            Friend item = temp.get(i);
            if(item.confirm){
                friendList.add(item);
            }
        }

        // First item: User himself, Last item: hint
        friendList.add(0, new Friend(null, null, user, "Self", null, -1, -1, false, true, true));
        friendList.add(friendList.size(),new Friend(null, null, user, "Select Payer", null, -1, -1, false, true, true));

        friendSelected = new Double[friendList.size()];

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
        ActionBar actionBar = parent.getSupportActionBar();
        if(actionBar != null) {
            final Drawable upArrow = ContextCompat.getDrawable(getContext(), R.drawable.abc_ic_ab_back_mtrl_am_alpha);
            upArrow.setColorFilter(ContextCompat.getColor(getContext(), R.color.coolBackground), PorterDuff.Mode.SRC_ATOP);
            actionBar.setHomeAsUpIndicator(upArrow);
        }
        parent.setTitle("Add Statement");

        if(previousState != null && pageCheck){
            pageCheck = false;
            return previousState;
        }
        else if(previousState != null){
            dateRecord.set(DATE + YEAR, 1899);
            dateRecord.set(DEADLINE + YEAR, 2101);
        }

        View rootView = inflater.inflate(R.layout.fragment_add_statement, container, false);
        Spinner paidBySpinner = (Spinner) rootView.findViewById(R.id.spinner);
        final Spinner modeSpinner = (Spinner) rootView.findViewById(R.id.spinner2);
        Button addMemberButton = (Button) rootView.findViewById(R.id.addMemberButton);
        Button addSnapshotButton = (Button) rootView.findViewById(R.id.addSnapshotButton);
        Button addNoteButton = (Button) rootView.findViewById(R.id.addNoteButton);
        Button confirmButton = (Button) rootView.findViewById(R.id.confirmButton);
        moneyAmount = (EditText) rootView.findViewById(R.id.moneyAmount);
        description = (EditText) rootView.findViewById(R.id.statement_description);
        clickedText = (TextView) rootView.findViewById(R.id.clickText);
        dateFieldButton = (Button) rootView.findViewById(R.id.dateFieldButton);
        deadlineFieldButton = (Button) rootView.findViewById(R.id.deadlineFieldButton);
        layoutMemberDisplay = (LinearLayout) rootView.findViewById(R.id.layout_member_display);

        final ArrayAdapter<Friend> paidByAdapter = new ArrayAdapter<Friend>(getActivity(), android.R.layout.simple_spinner_dropdown_item) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                if (position == getCount()) {
                    ((TextView)v.findViewById(android.R.id.text1)).setText(getItem(getCount()).displayName);   //"Hint to be displayed"
                }
                return v;
            }

            @Override
            public int getCount() {
                return super.getCount()-1; // you dont display last item. It is used as hint.
            }
        };

        paidByAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        paidByAdapter.addAll(friendList);
        paidBySpinner.setAdapter(paidByAdapter);
        paidBySpinner.setSelection(paidByAdapter.getCount());

        final ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_dropdown_item) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                if (position == getCount()) {
                    ((TextView)v.findViewById(android.R.id.text1)).setText(getItem(getCount())); //"Hint to be displayed"
                }
                return v;
            }

            @Override
            public int getCount() {
                return super.getCount()-1; // you dont display last item. It is used as hint.
            }
        };

        modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modeAdapter.add("Split Equally");
        modeAdapter.add("Split Unequally");
        modeAdapter.add("Split by Ratio");
        modeAdapter.add("Select mode");

        modeSpinner.setAdapter(modeAdapter);
        modeSpinner.setSelection(modeAdapter.getCount());

        paidBySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(position == paidByAdapter.getCount()){
                    paidByPosition = PAYER_HINT;
                } else {
                    paidByPosition = position;
                }
                friendSelected = new Double[friendList.size()];
                selectedMember = new ArrayList<>();
                displayMemberSelected();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { /* do nothing */ }
        });

        modeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(position == modeAdapter.getCount()){
                    modePosition = MODE_HINT;
                } else {
                    modePosition = position;
                }
                friendSelected = new Double[friendList.size()];
                selectedMember = new ArrayList<>();
                displayMemberSelected();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { /* do nothing */ }
        });

        confirmButton.setOnClickListener(confirmButtonListener);
        addMemberButton.setOnClickListener(addMemberButtonListener);
        dateFieldButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(DATE);
            }
        });
        deadlineFieldButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {showDatePickerDialog(DEADLINE);
            }
        });

        moneyAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* do nothing */ }

            @Override
            public void afterTextChanged(Editable s) {
                if(!s.toString().equals(previousMoneyAmount)) {
                    if (modePosition == SPLIT_EQUALLY || modePosition == SPLIT_BY_RATIO) {
                        isAmountChanged = true;
                    } else if (modePosition == SPLIT_UNEQUALLY) {
                        isAmountChanged = false;
                        friendSelected = new Double[friendList.size()];
                        selectedMember = new ArrayList<>();
                        displayMemberSelected();
                    }
                    previousMoneyAmount = s.toString();
                }
            }

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

        addNoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(parent, "Unavailable now", Toast.LENGTH_SHORT).show();
            }
        });

        TextView text0 = (TextView) rootView.findViewById(R.id.text0);
        TextView text1 = (TextView) rootView.findViewById(R.id.text1);
        TextView text2 = (TextView) rootView.findViewById(R.id.text2);
        TextView text3 = (TextView) rootView.findViewById(R.id.text3);
        TextView text4 = (TextView) rootView.findViewById(R.id.text4);

        Drawable img = ContextCompat.getDrawable(getContext(), R.drawable.ic_label_outline_white_24dp);
        img.setBounds(0, 0, 75, 75);
        text0.setCompoundDrawables(img, null, null, null);
        img = ContextCompat.getDrawable(getContext(), R.drawable.ic_call_split_white_24dp);
        img.setBounds(0, 0, 75, 75);
        text1.setCompoundDrawables(img, null, null, null);
        img = ContextCompat.getDrawable(getContext(), R.drawable.ic_date_range_white_24dp);
        img.setBounds(0, 0, 75, 75);
        text2.setCompoundDrawables(img, null, null, null);
        img = ContextCompat.getDrawable(getContext(), R.drawable.ic_wc_white_24dp);
        img.setBounds(0, 0, 75, 75);
        text3.setCompoundDrawables(img, null, null, null);
        img = ContextCompat.getDrawable(getContext(), R.drawable.ic_info_outline_white_24dp);
        img.setBounds(0, 0, 75, 75);
        text4.setCompoundDrawables(img, null, null, null);
        img = ContextCompat.getDrawable(getContext(), R.drawable.ic_description_black_24dp);
        img.setBounds(0, 0, 75, 75);
        description.setCompoundDrawables(img, null, null, null);

        previousState = rootView;
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

    protected void setClickedIconText(String string) {
        if (!string.equals("")) {
            clickedText.setText(string);
        }
    }

    private void displayMemberSelected(){
        layoutMemberDisplay.removeAllViews();
        int counter = selectedMember.size();
        if(counter != 0) {
            TextView text = new TextView(parent);
            text.setTextColor(Color.RED);
            text.setTextColor(Color.RED);
            text.setGravity(Gravity.CENTER_HORIZONTAL);
            text.setTypeface(null, Typeface.BOLD);
            text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            TextView memberName = new TextView(parent);
            memberName.setGravity(Gravity.CENTER_HORIZONTAL);

            StringBuilder data = new StringBuilder("");
            for (int i = 0; i < counter; i++) {
                data.append(selectedMember.get(i).first.displayName).append("\n");
            }
            if(this.counter > 0.009 && modePosition != SPLIT_BY_RATIO){
                if(modePosition == SPLIT_UNEQUALLY){
                    text.setText("Selected Member [" + Integer.toString(counter) + "+n]");
                    data.append("(Some non-Fairwell users)").append("\n");
                } else {
                    text.setText("Selected Member [" + Integer.toString(counter) + "+" + String.format("%.0f", this.counter) + "]");
                    if ((int) this.counter == 1) {
                        data.append("(").append((int) this.counter).append(" non-Fairwell user)").append("\n");
                    } else {
                        data.append("(").append((int) this.counter).append(" non-Fairwell users)").append("\n");
                    }
                }

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
            String date = dateFieldButton.getText().toString();
            String deadline = deadlineFieldButton.getText().toString();
            Boolean member = selectedMember.isEmpty();

            if(!descr.equals("") && !categ.equals("Select Category") && !amount.equals("") && !date.equals("EVENT DATE")
                    && !deadline.equals("DUE DATE") && !member && paidByPosition != PAYER_HINT && modePosition != MODE_HINT){

                Friend payee = paidByPosition == SELF ? null : friendList.get(paidByPosition);
                if(selectedMember.size() == 1){
                    if(selectedMember.get(0).first.displayName.equals("Self") && payee == null) {
                        Toast.makeText(getContext(), "Statement must be made between at least 2 Fairwell-users", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    else if(payee != null && !selectedMember.get(0).first.displayName.equals("Self")){
                        if(selectedMember.get(0).first.isEqual(payee)){
                            Toast.makeText(getContext(), "Statement must be made between at least 2 Fairwell-users", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                }

                parent.layoutManage(INDEX_SUBMIT_STATEMENT_SUMMARY);
                pageCheck = true;

                if(isAmountChanged){
                    if(modePosition == SPLIT_EQUALLY){
                        selectedMember = new ArrayList<>();
                        double each = Double.valueOf(moneyAmount.getText().toString()) / maxCapacity;
                        for (int i = 0; i < friendSelected.length; i++) {
                            if (friendSelected[i] != null) {
                                if (friendSelected[i].intValue() == 1) {
                                    selectedMember.add(new Pair<>(friendList.get(i), each));
                                }
                            }
                        }
                    } else if(modePosition == SPLIT_BY_RATIO){
                        selectedMember = new ArrayList<>();
                        double eachPortion = Double.valueOf(moneyAmount.getText().toString()) / counter;
                        for (int i = 0; i < friendSelected.length; i++) {
                            if (friendSelected[i] != null) {
                                if (friendSelected[i] > 0.99) {
                                    selectedMember.add(new Pair<>(friendList.get(i), friendSelected[i] * eachPortion));
                                }
                            }
                        }
                    }
                    isAmountChanged = false;
                }

                try {
                    int unknown;
                    if(modePosition == SPLIT_EQUALLY){ unknown = (int)counter; }
                    else if(modePosition == SPLIT_BY_RATIO){ unknown = 0; }
                    else { unknown = -1; }

                    SummaryStatement summaryStatement = new SummaryStatement(descr, categ, format.parse(date), format.parse(deadline),
                            modePosition, unknown, Double.valueOf(amount), payee, selectedMember);
                    parent.setSubmitStatementSummaryData(summaryStatement);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(getContext(), "Please fill in all required information", Toast.LENGTH_SHORT).show();
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
                    builder.setNegativeButton("Cancel", null);
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
                                            showMemberSelectionListOne(num);
                                        }
                                    }
                                }
                            });
                        }
                    });
                    dialog.show();
                    break;

                case SPLIT_UNEQUALLY:
                    String money = moneyAmount.getText().toString();
                    if(!money.equals("")) {
                        showMemberSelectionListTwo(Double.valueOf(money), SPLIT_UNEQUALLY);
                    } else {
                        Toast.makeText(parent, "Please enter payment amount first", Toast.LENGTH_SHORT).show();
                    }
                    break;

                case SPLIT_BY_RATIO:
                    showMemberSelectionListTwo(0, SPLIT_BY_RATIO);
                    break;

                default:
                    Toast.makeText(parent, "Please select payment mode first", Toast.LENGTH_SHORT).show();
            }
        }
    };

    //For SPLIT_EQUALLY only
    private void showMemberSelectionListOne(final int capacity){
        AlertDialog.Builder builder = new AlertDialog.Builder(parent);
        capacityText = new TextView(parent);
        tempResult = new Double[friendList.size()];
        final ListView container = new ListView(parent);
        maxCapacity = capacity;
        counter = capacity;
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

        MemberSelectionAdaptorOne memberAdaptor = new MemberSelectionAdaptorOne(getContext(), R.layout.item_add_member_one,
                friendList.subList(0,friendList.size()-1));
        container.setAdapter(memberAdaptor);
        container.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CheckBox checkBox = (CheckBox) view.findViewById(R.id.memberCheckBox);
                if (checkBox.isChecked()) {
                    tempResult[position] = 0.00;
                    checkBox.setChecked(false);
                    if (counter >= 0) {
                        counter += 1.00;
                        capacityText.setText(String.format("%.0f", counter));
                    }
                } else if (counter != 0 && ((int)maxCapacity != 1 || paidByPosition != position)) {
                    tempResult[position] = 1.00;
                    checkBox.setChecked(true);
                    if (counter > 0) {
                        counter -= 1.00;
                        if(counter < 0.00 && counter > -0.01){ counter = 0.00; }
                        capacityText.setText(String.format("%.0f", counter));
                    }
                }
            }
        });
        builder.setTitle("Select Member(s)");
        builder.setPositiveButton("Select", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                friendSelected = Arrays.copyOf(tempResult, tempResult.length);
                isAmountChanged = false;
                selectedMember = new ArrayList<>();
                double each = 0.00;
                if (!moneyAmount.getText().toString().equals("")) {
                    each = Double.valueOf(moneyAmount.getText().toString()) / capacity;
                }

                for (int i = 0; i < tempResult.length; i++) {
                    if (friendSelected[i] != null) {
                        if (friendSelected[i].intValue() == 1) {
                            selectedMember.add(new Pair<>(friendList.get(i), each));
                        }
                    }
                }
                displayMemberSelected();

            }
        });
        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    //For SPLIT_UNEQUALLY and SPLIT_BY_RATIO
    private void showMemberSelectionListTwo(final double capacity, final int type){
        AlertDialog.Builder builder = new AlertDialog.Builder(parent);
        builder.setTitle("Select Member(s)");
        maxCapacity = capacity;
        counter = capacity;
        tempResult = new Double[friendList.size()];
        ListView container = new ListView(parent);
        MemberSelectionAdaptorTwo memberAdaptor;
        if(type == SPLIT_UNEQUALLY) {
            capacityText = new TextView(parent);
            TextView textView = new TextView(parent);
            textView.setText("Maximum Capacity: $ ");
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
            capacityText.setText(String.format("%.2f", capacity));
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
            memberAdaptor = new MemberSelectionAdaptorTwo(getContext(), R.layout.item_add_member_two,
                    friendList.subList(0,friendList.size()-1), SPLIT_UNEQUALLY);
        } else {
            builder.setView(container);
            memberAdaptor = new MemberSelectionAdaptorTwo(getContext(), R.layout.item_add_member_two,
                    friendList.subList(0,friendList.size()-1), SPLIT_BY_RATIO);
        }

        container.setAdapter(memberAdaptor);
        builder.setPositiveButton("Select", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                friendSelected = Arrays.copyOf(tempResult, tempResult.length);
                isAmountChanged = false;
                selectedMember = new ArrayList<>();
                if(modePosition == SPLIT_UNEQUALLY && counter != maxCapacity){
                    for (int i = 0; i < tempResult.length; i++) {
                        if (friendSelected[i] != null) {
                            if (friendSelected[i] > 0.009) {
                                selectedMember.add(new Pair<>(friendList.get(i), friendSelected[i]));
                            }
                        }
                    }
                } else if(modePosition == SPLIT_BY_RATIO && counter > 0.99){
                    double eachPortion = Double.valueOf(moneyAmount.getText().toString())/ counter;
                    for (int i = 0; i < tempResult.length; i++) {
                        if (friendSelected[i] != null) {
                            if (friendSelected[i] > 0.99) {
                                selectedMember.add(new Pair<>(friendList.get(i), friendSelected[i].intValue() * eachPortion));
                            }
                        }
                    }
                }
                displayMemberSelected();
            }
        });
        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
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
                dateFieldButton.setText(data.toString());
            } else {
                deadlineFieldButton.setText(data.toString());
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

    private class MemberSelectionAdaptorOne extends ArrayAdapter<Friend>{

        Context mContext;
        int mResource;
        List<Friend> mObject;

        public MemberSelectionAdaptorOne(Context context, int resource, List<Friend> objects){
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
            viewHolder.nameText.setText(currentItem.displayName);
            if(tempResult[position] == null){
                viewHolder.box.setChecked(false);
            } else {
                viewHolder.box.setChecked(tempResult[position].intValue() == 1);
            }

            return convertView;
        }
    }

    private class MemberSelectionAdaptorTwo extends ArrayAdapter<Friend>{

        private Handler repeatUpdateHandler = new Handler();
        private boolean mAutoIncrement = false;
        private boolean mAutoDecrement = false;
        Context mContext;
        int mResource;
        List<Friend> mObject;
        int mType;

        public MemberSelectionAdaptorTwo(Context context, int resource, List<Friend> objects, int type){
            super(context, resource, objects);
            mContext = context;
            mResource = resource;
            mObject = objects;
            mType = type;
        }

        private class ViewHolder{
            TextView nameText;
            Button minus;
            Button value;
            Button plus;
            int position;
        }

        private class RepeatUpdater implements Runnable {
            private Button button;

            public RepeatUpdater(View v) { button = (Button) v; }
            public void run() {
                if(mAutoIncrement){
                    Pair<Integer, Button> tag = (Pair) button.getTag();
                    if (counter > 0.009 && mType == SPLIT_UNEQUALLY) {
                        Double temp = Double.valueOf(tag.second.getText().toString()) + 0.01;
                        tag.second.setText(String.format("%.2f", temp));
                        tempResult[tag.first] = temp;
                        counter -= 0.01;
                        if(counter < 0.00 && counter > -0.001){ counter = 0.00; }
                        capacityText.setText(String.format("%.2f", counter));
                    } else if(mType == SPLIT_BY_RATIO){
                        Double temp = Double.valueOf(tag.second.getText().toString()) + 1;
                        counter += 1;
                        tempResult[tag.first] = temp;
                        tag.second.setText(String.format("%.0f", temp));
                    }
                    repeatUpdateHandler.postDelayed(new RepeatUpdater(button), 50);
                } else if(mAutoDecrement){
                    Pair<Integer, Button> tag = (Pair) button.getTag();
                    Double temp = Double.valueOf(tag.second.getText().toString());
                    if (temp > 0.009 && mType == SPLIT_UNEQUALLY) {
                        temp -= 0.01;
                        if(temp < 0.00 && temp > -0.001){ temp = 0.00; }
                        tempResult[tag.first] = temp;
                        tag.second.setText(String.format("%.2f", temp));
                        counter += 0.01;
                        capacityText.setText(String.format("%.2f", counter));
                    } else if(mType == SPLIT_BY_RATIO && temp > 0.99){
                        temp -= 1;
                        counter -= 1;
                        if(counter < 0.00 && counter > -0.01){ counter = 0.00; }
                        if(temp < 0.00 && temp > -0.01){ temp = 0.00; }
                        tempResult[tag.first] = temp;
                        tag.second.setText(String.format("%.0f", temp));
                    }
                    repeatUpdateHandler.postDelayed(new RepeatUpdater(button), 50);
                }
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parentGroup){
            Friend currentItem = mObject.get(position);
            final ViewHolder viewHolder;
            if(convertView == null){
                convertView = parent.getLayoutInflater().inflate(mResource, parentGroup, false);
                viewHolder = new ViewHolder();
                viewHolder.nameText = (TextView) convertView.findViewById(R.id.memberName);
                viewHolder.minus = (Button) convertView.findViewById(R.id.minusButton);
                viewHolder.value = (Button) convertView.findViewById(R.id.valueButton);
                viewHolder.plus = (Button) convertView.findViewById(R.id.plusButton);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.position = position;
            viewHolder.nameText.setText(currentItem.displayName);

            Pair<Integer, Button> tag = new Pair<>(position, viewHolder.value);
            viewHolder.plus.setTag(tag);
            viewHolder.minus.setTag(tag);
            viewHolder.value.setTag(position);

            if(tempResult[position] == null){
                if(mType == SPLIT_UNEQUALLY){ viewHolder.value.setText("0.00"); }
                else{ viewHolder.value.setText("0"); }
            } else {
                if(mType == SPLIT_UNEQUALLY){ viewHolder.value.setText(String.format("%.2f", tempResult[position])); }
                else{ viewHolder.value.setText(String.format("%.0f", tempResult[position])); }
            }

            viewHolder.plus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Pair<Integer, Button> tag = (Pair) v.getTag();
                    if (counter > 0.99 || mType == SPLIT_BY_RATIO) {
                        Double temp = Double.valueOf(tag.second.getText().toString()) + 1.00;
                        tempResult[tag.first] = temp;
                        if (mType == SPLIT_UNEQUALLY) {
                            tag.second.setText(String.format("%.2f", temp));
                            counter -= 1.00;
                            if (counter < 0.00 && counter > -0.01) {
                                counter = 0.00;
                            }
                            capacityText.setText(String.format("%.2f", counter));
                        } else {
                            counter += 1;
                            tag.second.setText(String.format("%.0f", temp));
                        }
                    }
                }
            });
            viewHolder.plus.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mAutoIncrement = true;
                    repeatUpdateHandler.post(new RepeatUpdater(v));
                    return false;
                }
            });
            viewHolder.plus.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if ((event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)
                            && mAutoIncrement) {
                        mAutoIncrement = false;
                        return true;
                    }
                    return false;
                }
            });

            viewHolder.minus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Pair<Integer, Button> tag = (Pair) v.getTag();
                    Double temp = Double.valueOf(tag.second.getText().toString());
                    if (temp > 0.99) {
                        temp -= 1.00;
                        if (temp < 0.00 && temp > -0.01) {
                            temp = 0.00;
                        }
                        tempResult[tag.first] = temp;
                        if (mType == SPLIT_UNEQUALLY) {
                            tag.second.setText(String.format("%.2f", temp));
                            counter += 1.00;
                            capacityText.setText(String.format("%.2f", counter));
                        } else {
                            counter -= 1;
                            if(counter < 0.00 && counter > -0.01){ counter = 0; }
                            tag.second.setText(String.format("%.0f", temp));
                        }
                    }
                }
            });
            viewHolder.minus.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mAutoDecrement = true;
                    repeatUpdateHandler.post(new RepeatUpdater(v));
                    return false;
                }
            });
            viewHolder.minus.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if ((event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)
                            && mAutoDecrement) {
                        mAutoDecrement = false;
                        return true;
                    }
                    return false;
                }
            });

            viewHolder.value.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Button button = (Button) v;
                    final AlertDialog.Builder builder = new AlertDialog.Builder(parent);
                    LinearLayout linearLayout = new LinearLayout(parent);
                    linearLayout.setOrientation(LinearLayout.HORIZONTAL);
                    linearLayout.setPadding(50, 20, 20, 0);
                    TextView textView = new TextView(parent);
                    textView.setText("Set a new value:  ");
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
                    final EditText editText = new EditText(parent);
                    editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
                    editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(7)});
                    linearLayout.addView(textView);
                    linearLayout.addView(editText);
                    builder.setTitle("Set Value");
                    builder.setView(linearLayout);
                    if(mType == SPLIT_UNEQUALLY){
                        editText.setHint(button.getText().toString());
                        editText.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    } else {
                        editText.setHint(button.getText().toString());
                        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                    }

                    builder.setView(linearLayout);
                    builder.setPositiveButton("Set Value", null);
                    builder.setNegativeButton("Cancel", null);
                    final AlertDialog dialog = builder.create();

                    dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(DialogInterface dialogInterface) {
                            final Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                            positiveButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    String newValueInString = editText.getText().toString();
                                    Double oldValue = Double.valueOf(button.getText().toString());
                                    if(mType == SPLIT_BY_RATIO){
                                        button.setText(newValueInString);
                                        counter = counter - oldValue.intValue() + Double.valueOf(newValueInString).intValue();
                                        dialog.dismiss();
                                    } else {
                                        if(newValueInString.contains(".")){
                                            int index = newValueInString.indexOf(".");
                                            if(newValueInString.length() > index + 3){
                                                newValueInString = newValueInString.substring(0, index + 3);
                                            }
                                        }
                                        Double newValue = Double.valueOf(newValueInString);
                                        if(newValue < (counter + oldValue + 0.01)){
                                            button.setText(String.format("%.2f", newValue));
                                            tempResult[(int)button.getTag()] = newValue;
                                            counter = counter + oldValue - newValue;
                                            if(counter < 0.00 && counter > -0.01){ counter = 0.00; }
                                            capacityText.setText(String.format("%.2f", counter));
                                            dialog.dismiss();
                                        } else {
                                            Toast.makeText(parent, "The new value excesses the capacity!", Toast.LENGTH_SHORT).show();
                                            editText.setText("");
                                        }
                                    }
                                }
                            });
                        }
                    });
                    dialog.show();
                }
            });

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

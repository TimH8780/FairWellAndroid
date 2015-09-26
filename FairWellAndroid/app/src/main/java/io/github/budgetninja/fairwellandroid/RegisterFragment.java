package io.github.budgetninja.fairwellandroid;


import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

/**
 * A placeholder fragment containing a simple view.
 */
public class RegisterFragment extends Fragment {

    private View rootView;
    private Button cancelButton, confirmRegButton, uploadButton;
    private EditText firstN, lastN, userN, email, pass, ConfirmPass;
    private TextView termCondition;
    private CheckBox agreement;

    public RegisterFragment() {
    }

    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_register, container, false);

        cancelButton = (Button) rootView.findViewById(R.id.cancelRegistrationButton);
        confirmRegButton = (Button) rootView.findViewById(R.id.confirmRegistrationButton);
        uploadButton = (Button) rootView.findViewById(R.id.uploadPic);
        firstN = (EditText) rootView.findViewById(R.id.firstName);
        lastN = (EditText) rootView.findViewById(R.id.lastName);
        userN = (EditText) rootView.findViewById(R.id.username);
        email = (EditText) rootView.findViewById(R.id.emailAddress);
        pass = (EditText) rootView.findViewById(R.id.password);
        ConfirmPass = (EditText) rootView.findViewById(R.id.confirmPassword);
        termCondition = (TextView) rootView.findViewById(R.id.conditionTerm);
        agreement = (CheckBox) rootView.findViewById(R.id.agreeCheckBox);

        //Function of Cancel Button
        cancelButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "Cancelled", Toast.LENGTH_SHORT).show();
                ((MainActivity) getActivity()).goToRegisterPage(false);
            }
        });

        //Function of Confirm Registration Buttton
        confirmRegButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(pass.getText().toString().equals(ConfirmPass.getText().toString())
                        && firstN.getText().toString().length() > 0 && lastN.getText().toString().length() >0){
                    if(agreement.isChecked()) {
                        ParseUser user = new ParseUser();
                        user.setUsername(userN.getText().toString());
                        user.setPassword(pass.getText().toString());
                        user.setEmail(email.getText().toString());
                        user.put("Last_Name", lastN.getText().toString());
                        user.put("First_Name", firstN.getText().toString());
                        user.signUpInBackground(new SignUpCallback() {
                            @Override
                            public void done(ParseException e) {
                                if (e == null) {
                                    //Successful
                                    Toast.makeText(getActivity().getApplicationContext(), "Registration Success. A verification email was sent to"
                                            + email.getText().toString(), Toast.LENGTH_SHORT).show();
                                    ((MainActivity) getActivity()).goToRegisterPage(false);
                                    return;
                                }
                                //Fail
                                Toast.makeText(getActivity().getApplicationContext(), "Failed to register: "
                                        + e.getMessage(), Toast.LENGTH_SHORT).show();
                                ConfirmPass.setText("");
                                agreement.setChecked(false);
                            }
                        });
                        return;
                    }
                    //No check on agreement
                    Toast.makeText(getActivity().getApplicationContext(), "Please read and agree " +
                            "the 'Term and Condition'", Toast.LENGTH_SHORT).show();
                    ConfirmPass.setText("");
                    return;
                }
                //Missing info or Not match password
                Toast.makeText(getActivity().getApplicationContext(), "Please double check " +
                        "all information", Toast.LENGTH_SHORT).show();
                ConfirmPass.setText("");
            }
        });

        //Function of uploading an image
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //do something
            }
        });

        //The "Term and Condition"
        termCondition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //display something
            }
        });

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_register, menu);
    }

}

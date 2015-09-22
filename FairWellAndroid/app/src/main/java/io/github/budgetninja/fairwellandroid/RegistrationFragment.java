package io.github.budgetninja.fairwellandroid;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;


/**
 * A placeholder fragment containing a simple view.
 */
public class RegistrationFragment extends Fragment {

    EditText registration_username;
    EditText registration_password;
    EditText registration_confirm_password;
    EditText registration_email;
    EditText registration_confirm_email;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_registration, container, false);

        registration_username = (EditText) view.findViewById(R.id.registration_username);
        registration_password = (EditText) view.findViewById(R.id.registration_password);
        registration_confirm_password = (EditText) view.findViewById(R.id.registration_confirm_password);
        registration_email = (EditText) view.findViewById(R.id.registration_email);
        registration_confirm_email = (EditText) view.findViewById(R.id.registration_confirm_email);

        return view;

    }

}

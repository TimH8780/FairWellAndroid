package io.github.budgetninja.fairwellandroid;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;


/**
 * A placeholder fragment containing a simple view.
 */
public class LoginFragment extends Fragment {

    EditText username;
    EditText password;
    Button facebookLogin;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        username = (EditText) view.findViewById(R.id.login_username);
        password = (EditText) view.findViewById(R.id.login_password);
        facebookLogin = (com.facebook.login.widget.LoginButton) view.findViewById(R.id.login_button_facebook);

        return view;
    }

}

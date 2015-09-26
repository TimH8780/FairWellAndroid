package io.github.budgetninja.fairwellandroid;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseInstallation;
import com.parse.ParseUser;
import com.parse.RequestPasswordResetCallback;
import com.parse.SaveCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class LoginFragment extends Fragment {

    private View view;
    private Button loginBut, registerBut, facebookLoginBut;
    private EditText username, password;
    private TextView forgetPass;

    public LoginFragment() {
    }

    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        setHasOptionsMenu(true);        //force to recreate optionMenu
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_login, container, false);

        loginBut = (Button) view.findViewById(R.id.loginButton);
        registerBut = (Button) view.findViewById(R.id.registerButton);
        facebookLoginBut = (Button) view.findViewById(R.id.facebookButton);
        username = (EditText) view.findViewById(R.id.loginUsername);
        password = (EditText) view.findViewById(R.id.loginPassword);
        forgetPass = (TextView) view.findViewById(R.id.forgetPassword);

        //Function of Login Button
        loginBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ParseUser.logInInBackground(username.getText().toString(), password.getText().toString(), new LogInCallback() {
                    @Override
                    public void done(ParseUser parseUser, ParseException e) {
                        if (parseUser != null) {
                            ((MainActivity) getActivity()).goToLoggedInPage();
                            return;
                        }
                        Toast.makeText(getActivity().getApplicationContext(), "Failed to login: invalid information", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        //Function of Register Button
        registerBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).goToRegisterPage(true);
            }
        });

        //Function of Facebook Login
        facebookLoginBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ParseFacebookUtils.logInWithReadPermissionsInBackground(getActivity(), Arrays.asList("public_profile", "email"), new LogInCallback() {
                    @Override
                    public void done(final ParseUser user, ParseException e) {
                        if (e == null) {
                            if (user == null) {
                                Log.d("FairWell", "Uh oh. The user cancelled the Facebook login.");
                            } else if (user.isNew()) {
                                Log.d("FairWell", "User Signed up and logged in through Facebook!");
                                setUpUsernameFacebook(user);
                            } else {
                                Log.d("FairWell", "User logged in through Facebook!");
                                setUpUsernameFacebook(user);

                                //Below is for push notification in the future.
                                ParseInstallation myInstallation = ParseInstallation.getCurrentInstallation();
                                myInstallation.put("User", ParseUser.getCurrentUser());
                                myInstallation.saveInBackground();
                            }
                        } else {
                            Log.d("Facebook Login", e.getMessage());
                            Toast.makeText(getActivity().getApplicationContext(), "Failed to Login with facebook: "
                                    + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        //Function of forget password
        forgetPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                final EditText input = new EditText(getActivity());
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                builder.setTitle("Please Enter Your Email Address");
                builder.setView(input);
                builder.setPositiveButton("Send Reset Link", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final String emailAddress = input.getText().toString();
                        ParseUser.requestPasswordResetInBackground(emailAddress, new RequestPasswordResetCallback() {
                            public void done(ParseException e) {
                                if (e == null) {
                                    Toast.makeText(getActivity().getApplicationContext(), "An email has been sent to "
                                            + emailAddress, Toast.LENGTH_SHORT).show();
                                } else {
                                    Log.d("ResetPW", e.getMessage());
                                    Toast.makeText(getActivity().getApplicationContext(), "Failed to reset password: "
                                            + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
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

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_login, menu);
    }

    public void setUpUsernameFacebook(final ParseUser user){
        GraphRequest request = GraphRequest.newMeRequest(AccessToken.getCurrentAccessToken(),
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(
                            JSONObject object,
                            GraphResponse response) {
                        try {
                            user.fetchIfNeeded().put("usernameFacebook", object.getString("name"));
                            user.fetchIfNeeded().put("Last_Name", object.getString("last_name"));
                            user.fetchIfNeeded().put("First_Name", object.getString("first_name"));
                            user.saveInBackground(new SaveCallback() {
                                @Override
                                public void done(ParseException e) {
                                    if (e == null) {
                                        ((MainActivity) getActivity()).goToLoggedInPage();
                                    } else {
                                        e.printStackTrace();
                                        Log.d("User", e.getMessage());
                                        Toast.makeText(getActivity().getApplicationContext(), "Request Failed," +
                                                " Please Retry.", Toast.LENGTH_SHORT).show();
                                        ParseUser.logOutInBackground();
                                    }
                                }
                            });
                        }
                        catch (JSONException e) { e.printStackTrace(); }
                        catch (ParseException e) { }
                    }
                });
        Bundle parameters = new Bundle();
        parameters.putString("fields", "name,email");
        request.setParameters(parameters);
        request.executeAsync();
    }

}

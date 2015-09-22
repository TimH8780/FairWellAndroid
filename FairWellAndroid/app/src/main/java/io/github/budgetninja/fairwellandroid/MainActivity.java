package io.github.budgetninja.fairwellandroid;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
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
import com.parse.SignUpCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;


public class MainActivity extends AppCompatActivity {

FragmentManager FragmentManger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FragmentManger = getFragmentManager();
        FragmentTransaction fragmentTransaction = FragmentManger.beginTransaction();
        fragmentTransaction.replace(R.id.frag_container, new LoginFragment(), "fragTag");
        fragmentTransaction.commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ParseFacebookUtils.onActivityResult(requestCode, resultCode, data);
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
                            user.saveInBackground(new SaveCallback() {
                                @Override
                                public void done(ParseException e) {
                                    if (e == null) {
                                        goToLoggedInPage();
                                    } else {
                                        e.printStackTrace();
                                        Log.d("User", e.getMessage());
                                        Toast.makeText(getApplicationContext(), "request failed, please re-try.", Toast.LENGTH_SHORT).show();
                                        ParseUser.logOutInBackground();
                                    }
                                }
                            });
                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (ParseException e) {
                        }
                    }
                });
        Bundle parameters = new Bundle();
        parameters.putString("fields", "name,email");
        request.setParameters(parameters);
        request.executeAsync();
    }

    public void goToLoggedInPage(){
        Intent intent = new Intent(MainActivity.this, LoggedInActivity.class);
        startActivity(intent);
    }

    public void switchFragment(View view){
        Fragment frag = new RegistrationFragment();
        if(view == findViewById(R.id.registration_cancel_button)){
            frag = new LoginFragment();
        }
        FragmentManager FragmentManger = getFragmentManager();
        FragmentTransaction fragmentTransaction = FragmentManger.beginTransaction();
        fragmentTransaction.replace(R.id.frag_container, frag, "fragTag");
        fragmentTransaction.commit();
    }

    public void forgetPassword(View view){
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        final EditText input = new EditText(MainActivity.this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        builder.setTitle(getString(R.string.please_enter_your_email_address));
        builder.setView(input);
        builder.setPositiveButton(getString(R.string.send_password_reset_link), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String emailAddress = input.getText().toString();
                ParseUser.requestPasswordResetInBackground(emailAddress, new RequestPasswordResetCallback() {
                    public void done(ParseException e) {
                        if (e == null) {
                            Toast toast = Toast.makeText(getApplicationContext(), "An email has been sent to " + emailAddress, Toast.LENGTH_SHORT);
                            toast.show();
                        } else {
                            Log.d("ResetPW", e.getMessage());
                            Toast.makeText(getApplicationContext(), "Failed to reset password: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    public void facebookLogin(View view) {
        ParseFacebookUtils.logInWithReadPermissionsInBackground(MainActivity.this,
                Arrays.asList("public_profile", "email"), new LogInCallback() {
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
                            Toast.makeText(getApplicationContext(), "Failed to Login with facebook: "
                                    + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    public void login (View view){
        LoginFragment temp = (LoginFragment)FragmentManger.findFragmentByTag("fragTag");
        ParseUser.logInInBackground(temp.username.getText().toString(), temp.password.getText().toString(), new LogInCallback() {
            @Override
            public void done(ParseUser parseUser, ParseException e) {
                if (parseUser != null) {
                    goToLoggedInPage();
                    return;
                }
                Toast.makeText(getApplicationContext(), "Failed to login: invalid username or password", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void registration(View view){
        RegistrationFragment temp = (RegistrationFragment)FragmentManger.findFragmentByTag("fragTag");
        String temp_name = temp.registration_username.getText().toString();
        String temp_pass = temp.registration_password.getText().toString();
        String temp_mail = temp.registration_email.getText().toString();
        String temp_c_mail = temp.registration_confirm_email.getText().toString();
        String temp_c_pass = temp.registration_confirm_password.getText().toString();
        if( temp_mail.equals(temp_c_mail) && temp_pass.equals(temp_c_pass)){
            ParseUser user = new ParseUser();
            user.setUsername(temp_name);
            user.setPassword(temp_pass);
            user.setEmail(temp_mail);
            user.signUpInBackground(new SignUpCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                        Toast.makeText(getApplicationContext(), "Registration Success. Please Login", Toast.LENGTH_SHORT).show();
                        FragmentManager FragmentManger = getFragmentManager();
                        FragmentTransaction fragmentTransaction = FragmentManger.beginTransaction();
                        fragmentTransaction.replace(R.id.frag_container, new LoginFragment(),"fragTag");
                        fragmentTransaction.commit();
                        return;
                    }
                    Log.d("Registration", e.getMessage());
                    Toast.makeText(getApplicationContext(), "Failed to register: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    RegistrationFragment temp = (RegistrationFragment)FragmentManger.findFragmentByTag("fragTag");
                    temp.registration_confirm_password.setText("");
                }
            });
            return;
        }
        Toast.makeText(getApplicationContext(), "Failed to register: information not match", Toast.LENGTH_SHORT).show();
        temp.registration_confirm_password.setText("");
    }


}

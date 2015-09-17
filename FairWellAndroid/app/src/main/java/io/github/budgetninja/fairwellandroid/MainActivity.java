package io.github.budgetninja.fairwellandroid;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
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
import com.parse.SignUpCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;


public class MainActivity extends AppCompatActivity {

    EditText username;
    EditText password;
    EditText registration_username;
    EditText registration_password;
    EditText registration_confirm_password;
    EditText registration_email;
    EditText registration_confirm_email;
    Button login_button;
    Button register_button;
    Button registration_register_button;
    Button registration_cancel_button;
    Button facebookLogin;
    TextView forget_password;
    View registration_page;
    RelativeLayout login_page;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        registration_page = inflater.inflate(R.layout.registration, null, false);
        registration_page.setX(30);
        registration_page.setY(50);

        username = (EditText) findViewById(R.id.login_username);
        password = (EditText) findViewById(R.id.login_password);
        registration_username = (EditText) registration_page.findViewById(R.id.registration_username);
        registration_password = (EditText) registration_page.findViewById(R.id.registration_password);
        registration_confirm_password = (EditText) registration_page.findViewById(R.id.registration_confirm_password);
        registration_email = (EditText) registration_page.findViewById(R.id.registration_email);
        registration_confirm_email = (EditText) registration_page.findViewById(R.id.registration_confirm_email);
        registration_cancel_button = (Button) registration_page.findViewById(R.id.registration_cancel_button);
        registration_register_button = (Button) registration_page.findViewById(R.id.registration_register_button);

        login_button = (Button) findViewById(R.id.login_button);
        register_button = (Button) findViewById(R.id.register_button);
        forget_password = (TextView) findViewById(R.id.forget_password);
        login_page = (RelativeLayout) findViewById(R.id.login_page);
        facebookLogin = (com.facebook.login.widget.LoginButton)findViewById(R.id.login_button_facebook);
        facebookLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
                                    Toast.makeText(getApplicationContext(), "Failed to Login with facebook: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });
        login_page.addView(registration_page);
        registration_page.setVisibility(View.GONE);

        login_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ParseUser.logInInBackground(username.getText().toString(), password.getText().toString(), new LogInCallback() {
                    @Override
                    public void done(ParseUser parseUser, ParseException e) {
                        if (parseUser != null) {
                            goToLoggedInPage();
                            return;
                        }
                        Toast.makeText(getApplicationContext(), "Incorrect Username or Password", Toast.LENGTH_LONG).show();
                        password.setText("");
                    }
                });
            }
        });

        register_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registration_page.setVisibility(View.VISIBLE);
                username.setEnabled(false);
                password.setEnabled(false);
            }
        });

        forget_password.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
        });

        registration_cancel_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registration_page.setVisibility(View.GONE);
                username.setEnabled(true);
                password.setEnabled(true);
                registration_password.setText("");
                registration_confirm_password.setText("");
                registration_email.setText("");
                registration_confirm_email.setText("");
                registration_username.setText("");
            }
        });

        registration_register_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String temp_name = registration_username.getText().toString();
                String temp_pass = registration_password.getText().toString();
                String temp_mail = registration_email.getText().toString();
                String temp_c_mail = registration_confirm_email.getText().toString();
                String temp_c_pass = registration_confirm_password.getText().toString();
                if( temp_mail.equals(temp_c_mail) && temp_pass.equals(temp_c_pass)){
                    ParseUser user = new ParseUser();
                    user.setUsername(temp_name);
                    user.setPassword(temp_pass);
                    user.setEmail(temp_mail);
                    user.signUpInBackground(new SignUpCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                Toast.makeText(getApplicationContext(), "Registered! Please Login.", Toast.LENGTH_LONG).show();
                                registration_page.setVisibility(View.GONE);
                                username.setEnabled(true);
                                password.setEnabled(true);
                                return;
                            }
                            Toast.makeText(getApplicationContext(), "Invalid information or Already used", Toast.LENGTH_LONG).show();
                            registration_password.setText("");
                            registration_confirm_password.setText("");
                        }
                    });
                    return;
                }
                Toast.makeText(getApplicationContext(), "Please enter valid information", Toast.LENGTH_LONG).show();
                registration_password.setText("");
                registration_confirm_password.setText("");
            }
        });

    }

    public void setUpUsernameFacebook(final ParseUser user){
        GraphRequest request = GraphRequest.newMeRequest(
                AccessToken.getCurrentAccessToken(),
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
                                    if(e==null) {
                                        goToLoggedInPage();
                                    }else{
                                        e.printStackTrace();
                                        Log.d("User", e.getMessage());
                                        Toast.makeText(getApplicationContext(),"request failed, please re-try.", Toast.LENGTH_SHORT).show();
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
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ParseFacebookUtils.onActivityResult(requestCode, resultCode, data);
    }
}

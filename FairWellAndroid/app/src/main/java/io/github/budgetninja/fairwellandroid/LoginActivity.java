package io.github.budgetninja.fairwellandroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
import com.parse.ParseTwitterUtils;
import com.parse.ParseUser;
import com.parse.RequestPasswordResetCallback;
import com.parse.SaveCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class LoginActivity extends Activity {

    private View view;
    private Button loginBut, registerBut, facebookLoginBut, twitterLoginBut;
    private EditText username, password;
    private TextView forgetPass;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; goto parent activity.
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        setContentView(R.layout.activity_login);

        loginBut = (Button) findViewById(R.id.loginButton);
        registerBut = (Button) findViewById(R.id.registerButton);
        facebookLoginBut = (Button) findViewById(R.id.facebookButton);
        twitterLoginBut = (Button) findViewById(R.id.twitterButton);
        username = (EditText) findViewById(R.id.loginUsername);
        password = (EditText) findViewById(R.id.loginPassword);
        forgetPass = (TextView) findViewById(R.id.forgetPassword);

        //Function of Login Button
        loginBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ParseUser.logInInBackground(username.getText().toString(), password.getText().toString(), new LogInCallback() {
                    @Override
                    public void done(ParseUser parseUser, ParseException e) {
                        if (e == null) {
                            goToLoggedInPage();
                        } else {
                            Toast.makeText(getApplicationContext(), "Failed to login: invalid information", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        //Function of Register Button
        registerBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToRegisterPage();
            }
        });

        //Function of Facebook Login
        facebookLoginBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ParseFacebookUtils.logInWithReadPermissionsInBackground(LoginActivity.this, Arrays.asList("public_profile", "email"), new LogInCallback() {
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
        });

        //Function of Twitter Login
        twitterLoginBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ParseTwitterUtils.logIn(LoginActivity.this, new LogInCallback() {
                    @Override
                    public void done(ParseUser user, ParseException err) {
                        if (user == null) {
                            Log.d("Fairwell", "Uh oh. The user cancelled the Twitter login.");
                            Toast.makeText(getApplicationContext(),getString(R.string.twitter_login_failed),Toast.LENGTH_SHORT).show();
                        } else if (user.isNew()) {
                            Log.d("Fairwell", "User signed up and logged in through Twitter!");
                            setUpUsernameTwitter(user);
                        } else {
                            Log.d("Fairwell", "User logged in through Twitter!");
                            setUpUsernameTwitter(user);
                            ParseInstallation myInstallation = ParseInstallation.getCurrentInstallation();
                            myInstallation.put("User", ParseUser.getCurrentUser());
                            myInstallation.saveInBackground();
                        }
                    }
                });
            }
        });

        //Function of forget password
        forgetPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                final EditText input = new EditText(LoginActivity.this);
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
                                    Toast.makeText(getApplicationContext(), "An email has been sent to "
                                            + emailAddress, Toast.LENGTH_SHORT).show();
                                } else {
                                    Log.d("ResetPW", e.getMessage());
                                    Toast.makeText(getApplicationContext(), "Failed to reset password: "
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
    }



    public void goToLoggedInPage(){
        Intent intent = new Intent(LoginActivity.this, ContentActivity.class);
        startActivity(intent);
    }

    public void setUpUsernameTwitter(ParseUser user){
        user.put("usernameTwitter",(ParseTwitterUtils.getTwitter().getScreenName()));
        user.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    goToLoggedInPage();
                } else {
                    Log.d("User", e.getMessage());
                    Toast.makeText(getApplicationContext(), "request failed, please re-try.", Toast.LENGTH_SHORT).show();
                    ParseUser.logOutInBackground();
                }
            }
        });
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
                            /*
                            if(object.getString("last_name")!=null) {
                                user.fetchIfNeeded().put("Last_Name", object.getString("last_name"));
                            }
                            if(object.getString("first_name")!=null) {
                                user.fetchIfNeeded().put("First_Name", object.getString("first_name"));
                            }
                            */
                            user.saveInBackground(new SaveCallback() {
                                @Override
                                public void done(ParseException e) {
                                    if (e == null) {
                                        goToLoggedInPage();
                                    } else {
                                        e.printStackTrace();
                                        Log.d("User", e.getMessage());
                                        Toast.makeText(getApplicationContext(), "Request Failed," +
                                                " Please Retry.", Toast.LENGTH_SHORT).show();
                                        ParseUser.logOutInBackground();
                                    }
                                }
                            });
                        }
                        catch (JSONException e) { e.printStackTrace(); }
                        catch (ParseException e) { e.printStackTrace();}
                    }
                });
        Bundle parameters = new Bundle();
        parameters.putString("fields", "name,email");
        request.setParameters(parameters);
        request.executeAsync();
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ParseFacebookUtils.onActivityResult(requestCode, resultCode, data);
    }
    public void goToRegisterPage() {
        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(intent);
    }
}

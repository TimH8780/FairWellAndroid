package io.github.budgetninja.fairwellandroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseTwitterUtils;
import com.parse.ParseUser;
import com.parse.RequestPasswordResetCallback;
import com.parse.SaveCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class LoginActivity extends Activity {

    private EditText username, password;

    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        setContentView(R.layout.activity_login);

        Button loginBut = (Button) findViewById(R.id.loginButton);
        Button registerBut = (Button) findViewById(R.id.registerButton);
        Button facebookLoginBut = (Button) findViewById(R.id.facebookButton);
        Button twitterLoginBut = (Button) findViewById(R.id.twitterButton);
        username = (EditText) findViewById(R.id.loginUsername);
        password = (EditText) findViewById(R.id.loginPassword);

        TextView forgetPass = (TextView) findViewById(R.id.forgetPassword);

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
                final LinearLayout layout = new LinearLayout(LoginActivity.this);
                final TextView message = new TextView(LoginActivity.this);
                final EditText userInput = new EditText(LoginActivity.this);
                layout.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams para = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);
                para.setMargins(20, 20, 20, 0);
                message.setText("Please enter your email address:");
                message.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
                message.setLayoutParams(para);
                userInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                userInput.setLayoutParams(para);
                layout.addView(message);
                layout.addView(userInput);
                builder.setTitle("Reset Password");      //use e-mail for now, may need to change
                builder.setView(layout);
                builder.setPositiveButton("Send Reset Link", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final String emailAddress = userInput.getText().toString();
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

    public void goToLoggedInPage(){
        if(Utility.checkNewEntry()){
            Utility.setChangedRecord();
            Utility.generateFriendList(ParseUser.getCurrentUser());
        }
        Intent intent = new Intent(LoginActivity.this, ContentActivity.class);
        startActivity(intent);
    }

    public void setUpUsernameTwitter(ParseUser user){
        user.put("usernameTwitter", (ParseTwitterUtils.getTwitter().getScreenName()));
        if(user.get("newEntry") == null){
            ParseObject temp = new ParseObject("Friend_update");
            temp.put("newEntry", false);
            temp.saveInBackground();
            user.put("newEntry", temp);
        }
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
                            if(user.get("newEntry") == null){
                                ParseObject temp = new ParseObject("Friend_update");
                                temp.put("newEntry", false);
                                temp.saveInBackground();
                                user.fetchIfNeeded().put("newEntry", temp);
                            }
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
                        catch (JSONException|ParseException e) { e.printStackTrace(); }
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

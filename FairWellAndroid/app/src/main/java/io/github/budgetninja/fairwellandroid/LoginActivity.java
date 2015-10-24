package io.github.budgetninja.fairwellandroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.InputType;
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
import com.parse.FindCallback;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseTwitterUtils;
import com.parse.ParseUser;
import com.parse.RequestPasswordResetCallback;
import com.parse.SaveCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LoginActivity extends Activity {

    private EditText username, password;
    private ConnectivityManager connMgr;

    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        setContentView(R.layout.activity_login);
        connMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

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
                if(!isNetworkConnected()) {
                    Toast.makeText(getApplicationContext(), "Check Internet Connection", Toast.LENGTH_SHORT).show();
                    return;
                }
                signin(username.getText().toString(), password.getText().toString());
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
                if(!isNetworkConnected()) {
                    Toast.makeText(getApplicationContext(), "Check Internet Connection", Toast.LENGTH_SHORT).show();
                    return;
                }
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
                if(!isNetworkConnected()) {
                    Toast.makeText(getApplicationContext(), "Check Internet Connection", Toast.LENGTH_SHORT).show();
                    return;
                }
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
                        if(!isNetworkConnected()) {
                            Toast.makeText(getApplicationContext(), "Check Internet Connection", Toast.LENGTH_SHORT).show();
                            return;
                        }
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
    public void signin(String email_username, final String password){
        //Username are restricted to contain "@" when signing up,
        //So if there are "@" in email_username, then it is a email, otherwise it is a username
        if(email_username.contains("@")){
            ParseQuery<ParseUser> queryUser = ParseUser.getQuery();
            queryUser.whereEqualTo("email",email_username);
            queryUser.findInBackground(new FindCallback<ParseUser>() {
                public void done(List<ParseUser> users, ParseException e) {
                    if (e == null) {
                        if (users.size() == 0) {
                            Toast toast = Toast.makeText(getApplicationContext(), "Signin Failed, email not registered", Toast.LENGTH_SHORT);
                            toast.show();
                        } else if (users.size() > 1) {
                            Toast toast = Toast.makeText(getApplicationContext(), "Signin Failed, Error in database, email is registered with more than 1 user", Toast.LENGTH_SHORT);
                            toast.show();
                        } else {
                            String username = users.get(0).getUsername();
                            signin(username, password);
                        }
                    } else {
                        Log.d("User", e.getMessage());
                        Toast.makeText(getApplicationContext(), "Failed to login: invalid information", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }else {
            ParseUser.logInInBackground(email_username, password, new LogInCallback() {
                public void done(ParseUser user, ParseException e) {
                    if (user != null) {
                        ParseInstallation myInstallation = ParseInstallation.getCurrentInstallation();
                        myInstallation.put("User", ParseUser.getCurrentUser());
                        myInstallation.saveInBackground();
                        goToLoggedInPage();
                    } else {
                        Log.d("User", e.getMessage());
                        Toast.makeText(getApplicationContext(), "Failed to login: invalid information", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ParseFacebookUtils.onActivityResult(requestCode, resultCode, data);
    }

    public void goToLoggedInPage(){
        if(Utility.checkNewEntryField()){
            Utility.setChangedRecord();
            Utility.generateRawFriendList(ParseUser.getCurrentUser());
        }
        Intent intent = new Intent(LoginActivity.this, ContentActivity.class);
        startActivity(intent);
    }

    public void setUpUsernameTwitter(ParseUser user){
        user.put("usernameTwitter", (ParseTwitterUtils.getTwitter().getScreenName()));
        if(user.get("newEntry") == null){
            ParseObject tempA = new ParseObject("Friend_update");
            tempA.put("newEntry", false);
            tempA.saveInBackground();
            user.put("newEntry", tempA);
            ParseObject tempB = ParseUser.getCurrentUser().getParseObject("newEntry");
            tempB.put("list", new ArrayList<ParseObject>());
            tempB.put("offlineFriendList", new ArrayList<String>());
            tempB.pinInBackground();
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
                                ParseObject tempA = new ParseObject("Friend_update");
                                tempA.put("newEntry", false);
                                tempA.saveInBackground();
                                user.fetchIfNeeded().put("newEntry", tempA);
                                ParseObject tempB = ParseUser.getCurrentUser().getParseObject("newEntry");
                                tempB.put("list", new ArrayList<ParseObject>());
                                tempB.put("offlineFriendList", new ArrayList<String>());
                                tempB.pinInBackground();
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

    public void goToRegisterPage() {
        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(intent);
    }

    private boolean isNetworkConnected(){
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

}

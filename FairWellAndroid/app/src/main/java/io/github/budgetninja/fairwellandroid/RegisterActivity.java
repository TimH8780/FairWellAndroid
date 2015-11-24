package io.github.budgetninja.fairwellandroid;


import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.text.Html;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

import java.util.ArrayList;

/**
 * A placeholder fragment containing a simple view.
 */
public class RegisterActivity extends AppCompatActivity {

    private EditText firstN, lastN, userN, email, pass, ConfirmPass;
    private CheckBox agreement;
    private ConnectivityManager connMgr;

    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        setContentView(R.layout.activity_register);

        Button confirmRegButton = (Button) findViewById(R.id.confirmRegistrationButton);
        firstN = (EditText) findViewById(R.id.firstName);
        lastN = (EditText) findViewById(R.id.lastName);
        userN = (EditText) findViewById(R.id.username);
        email = (EditText) findViewById(R.id.emailAddress);
        pass = (EditText) findViewById(R.id.password);
        ConfirmPass = (EditText) findViewById(R.id.confirmPassword);
        TextView termCondition = (TextView) findViewById(R.id.conditionTerm);
        agreement = (CheckBox) findViewById(R.id.agreeCheckBox);
        connMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        if (getSupportActionBar()!=null){
            getSupportActionBar().setTitle("Registration");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            final Drawable upArrow = getResources().getDrawable(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
            upArrow.setColorFilter(getResources().getColor(R.color.coolBackground), PorterDuff.Mode.SRC_ATOP);
            getSupportActionBar().setHomeAsUpIndicator(upArrow);
        }


        //Confirm Registration Button
        confirmRegButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                if(networkInfo == null || !networkInfo.isConnected()){
                    Toast.makeText(getApplicationContext(), "Check Internet Connection", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(pass.getText().toString().equals(ConfirmPass.getText().toString())
                        && firstN.getText().toString().length() > 0 && lastN.getText().toString().length() >0){
                    if(agreement.isChecked()) {
                        final ParseUser user = new ParseUser();
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
                                    ParseObject tempA = new ParseObject("Friend_update");
                                    tempA.put("newEntry", false);
                                    tempA.put("list", new ArrayList<ParseObject>());
                                    tempA.put("offlineFriendList", new ArrayList<String>());
                                    tempA.put("statementList", new ArrayList<ParseObject>());
                                    tempA.saveInBackground();
                                    user.put("newEntry", tempA);
                                    user.saveInBackground();
                                    ParseObject tempB = ParseUser.getCurrentUser().getParseObject("newEntry");
                                    tempB.pinInBackground();
                                    Toast.makeText(getApplicationContext(), "Registration Success. A verification email was sent to "
                                            + email.getText().toString(), Toast.LENGTH_SHORT).show();
                                    finish();
                                    return;
                                }
                                //Fail
                                Toast.makeText(getApplicationContext(), "Failed to register: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                ConfirmPass.setText("");
                                agreement.setChecked(false);
                            }
                        });
                        return;
                    }
                    //No check on agreement
                    Toast.makeText(getApplicationContext(), "Please read and agree the 'Term and Condition'", Toast.LENGTH_SHORT).show();
                    ConfirmPass.setText("");
                    return;
                }
                //Missing info or Not match password
                Toast.makeText(getApplicationContext(), "Please double check all information", Toast.LENGTH_SHORT).show();
                ConfirmPass.setText("");
            }
        });


        //Term and Condition
        termCondition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                    // EULA title
                    String title = getString(R.string.app_name) + " Terms and Privacy";

                    // EULA text
                    String message = getString(R.string.eula_string);

                    AlertDialog.Builder builder = new AlertDialog.Builder(RegisterActivity.this)
                            .setTitle(title)
                            .setMessage(message)
                            .setCancelable(false)
                            .setNegativeButton("OK", null);

                    builder.create().show();










            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}

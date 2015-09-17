package io.github.budgetninja.fairwellandroid;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

import org.w3c.dom.Text;


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
    TextView forget_password;
    View registration_page;
    RelativeLayout login_page;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(getApplicationContext().LAYOUT_INFLATER_SERVICE);
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

        login_page.addView(registration_page);
        registration_page.setVisibility(View.GONE);

        login_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ParseUser.logInInBackground(username.getText().toString(), password.getText().toString(), new LogInCallback() {
                    @Override
                    public void done(ParseUser parseUser, ParseException e) {
                        if (parseUser != null) {
                            //success, next activity
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
                //forget password
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

}

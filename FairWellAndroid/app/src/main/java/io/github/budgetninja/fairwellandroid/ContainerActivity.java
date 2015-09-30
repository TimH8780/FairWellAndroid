package io.github.budgetninja.fairwellandroid;


import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class ContainerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);                 //empty container

        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setElevation(0);
        }
        Bundle bundle = getIntent().getExtras();
        int index = bundle.getInt("Index");
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        switch (index) {                        //fill the container with different fragment based on the index
            case 1:
                ft.replace(R.id.container, new ViewStatementsFragment(), "View");
                break;
            case 2:
                ft.replace(R.id.container, new AddStatementFragment(), "Add");
                break;
            case 3:
                ft.replace(R.id.container, new ResolveStatementsFragment(), "Resolve");
                break;
        }
        ft.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_container, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if(id == android.R.id.home){
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //It will be better to change the image of the icon when clicked
    public void iconClick(View view){
        String clicked = "";
        if(view == findViewById(R.id.icon_1)){ clicked = "Icon 1"; }            //need to change, don't know what some
        else if(view == findViewById(R.id.icon_2)){ clicked = "Icon 2"; }      //icons are representing
        else if(view == findViewById(R.id.icon_3)){ clicked = "Icon 3"; }
        else if(view == findViewById(R.id.icon_4)){ clicked = "Icon 4"; }
        else if(view == findViewById(R.id.icon_5)){ clicked = "Icon 5"; }
        else if(view == findViewById(R.id.icon_6)){ clicked = "Icon 6"; }
        else if(view == findViewById(R.id.icon_7)){ clicked = "Icon 7"; }
        ((AddStatementFragment)getSupportFragmentManager().findFragmentByTag("Add")).setClickedText(clicked);
    }

}

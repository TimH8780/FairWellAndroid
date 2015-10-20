package io.github.budgetninja.fairwellandroid;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class ContainerActivity extends AppCompatActivity {

    private static final int INDEX_VIEW_STATEMENT = 1;
    private static final int INDEX_ADD_STATEMENT = 2;
    private static final int INDEX_RESOLVE_STATEMENT = 3;

    private ConnectivityManager connMgr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);                 //empty container
        connMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

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
            case INDEX_VIEW_STATEMENT:
                ft.replace(R.id.container, new ViewStatementsFragment(), "View");
                break;
            case INDEX_ADD_STATEMENT:
                ft.replace(R.id.container, new AddStatementFragment(), "Add");
                break;
            case INDEX_RESOLVE_STATEMENT:
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
        if(view == findViewById(R.id.icon_1)){ clicked = "Food"; }            //need to change, don't know what some
        else if(view == findViewById(R.id.icon_2)){ clicked = "Gift"; }      //icons are representing
        else if(view == findViewById(R.id.icon_3)){ clicked = "Travel"; }
        else if(view == findViewById(R.id.icon_4)){ clicked = "Grocery"; }
        else if(view == findViewById(R.id.icon_5)){ clicked = "Utility"; }
        else if(view == findViewById(R.id.icon_6)){ clicked = "Entertainment"; }
        else if(view == findViewById(R.id.icon_7)){ clicked = "Other"; }
        ((AddStatementFragment)getSupportFragmentManager().findFragmentByTag("Add")).setClickedIconText(clicked);
    }

    protected boolean isNetworkConnected(){
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }
}

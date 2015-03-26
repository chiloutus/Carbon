package com.garylynam.carbon;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;


public class Login extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
    }


    @Override
    public boolean onCreateOptionsMenu(android.view.Menu Menu) {
        // Inflate the Menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, Menu);
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

        return super.onOptionsItemSelected(item);
    }


    public void login(){
        //TODO create login Script

        Intent i = new Intent(this.getApplicationContext(),MainMenu.class);
        startActivity(i);

    }

}

package com.raja.bluelist1;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.support.v7.app.ActionBarActivity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity implements OnClickListener{
	private TextView mLabel1;
	private ListView mList1;
	private Button mButon1;
	private ArrayList<String> mArrList; 
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLabel1 = (TextView)findViewById(R.id.textView1);
        mList1 = (ListView)findViewById(R.id.listView1);
        mButon1 = (Button)findViewById(R.id.button1);
        mButon1.setOnClickListener(this);
        //populateDevices1();
        //populateDevices2();
        //populateDevices3();
        //populateDevices4();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);  
        String macId = prefs.getString("macid", "00:00:00:00:00");      
        Log.d("CHOOSE", macId);
        mLabel1.setText("MAC: " +macId);        
    }

    private void populateDevices1() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        for(BluetoothDevice bt : pairedDevices) {
           mLabel1.append("(" +bt.getName().trim() +") ");
           mLabel1.append(bt.getAddress() +"\n\n");
        }
    }
    
    private void populateDevices2() {
    	BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    	Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
    	ArrayList<String> lis = new ArrayList<String>();
    	for(BluetoothDevice bt : pairedDevices) {
    		String item = bt.getName().trim() +"\n" +bt.getAddress();
    	   lis.add(item);
    	}
    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.lineitem, lis);
    	mList1.setAdapter(adapter);
    }
    
    private void populateDevices3() {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this); //getBaseContext());
    	builder.setTitle("Choose an animal");
    	// add a list
    	final String[] animals = {"horse", "cow", "camel", "sheep", "goat"};
    	int selectedIndex = 1;
    	builder.setSingleChoiceItems(animals, selectedIndex, new DialogInterface.OnClickListener() {
    	    @Override
    	    public void onClick(DialogInterface dialog, int which) {
    	    	//selectedIndex = which;
    	    	getChoice(animals[which]);
    	    	//getChoice(which);
    	    	dialog.dismiss();
    	    }
    	});
    	// create and show the alert dialog
    	AlertDialog dialog = builder.create();
    	dialog.show();    	
    }
    
    private void populateDevices4() {  	
    	BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    	Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
    	int n = pairedDevices.size();
    	final String[] deviceList = new String[n];
    	int i = 0;
    	for(BluetoothDevice bt : pairedDevices) {
    		String item = bt.getName().trim() +"\n" +bt.getAddress();
    		deviceList[i] = item;
    		i++;
    	}
    	AlertDialog.Builder builder = new AlertDialog.Builder(this); 
    	builder.setTitle("Choose a device");
    	int selectedIndex = 2;
    	builder.setSingleChoiceItems(deviceList, selectedIndex, new DialogInterface.OnClickListener() {
    	    @Override
    	    public void onClick(DialogInterface dialog, int which) {
    	    	//selectedIndex = which;
    	    	//getChoice(which);
    	    	getChoice(deviceList[which]);
    	    	dialog.dismiss();
    	    }
    	});
    	// create and show the alert dialog
    	builder.show();
    }     
    private void populateDevices5() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);  
        String macId = prefs.getString("macid", "00:00:00:00:00");      
        Log.d("CHOOSE", macId);
        mLabel1.setText("MAC: " +macId);
    	int selectedIndex = 0;
    	BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    	Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
    	int n = pairedDevices.size();
    	final String[] deviceList = new String[n];
    	int i = 0;
    	for(BluetoothDevice bt : pairedDevices) {
    		String mac = bt.getAddress();
    		String item = bt.getName().trim() +"\n" +mac;
    		deviceList[i] = item;
    		if (mac.equals(macId))
    			selectedIndex = i;
    		i++;
    	}
    	AlertDialog.Builder builder = new AlertDialog.Builder(this); 
    	builder.setTitle("Choose a device");

    	builder.setSingleChoiceItems(deviceList, selectedIndex, new DialogInterface.OnClickListener() {
    	    @Override
    	    public void onClick(DialogInterface dialog, int which) {
    	    	//selectedIndex = which;
    	    	//getChoice(which);
    	    	getChoice(deviceList[which]);
    	    	dialog.dismiss();
    	    }
    	});
    	// create and show the alert dialog
    	builder.show();
    }    
    
    // call back method from dialog
    public void getChoice(String choice) {
    	Log.d("CHOOSE", "Choice= "+choice);
    	String[] frags = choice.split("\n");
    	mLabel1.setText("MAC: " +frags[1]);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);    
    	prefs.edit().putString("macid", frags[1]).commit();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		populateDevices4();
	}
}

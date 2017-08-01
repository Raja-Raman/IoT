package com.raja.occupancy3;

import java.util.Set;

import android.support.v7.app.ActionBarActivity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

//<uses-permission android:name="android.permission.BLUETOOTH" />
//<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
public class MainActivity extends ActionBarActivity  implements OnClickListener{
	private BlueHelper mBluetoothHelper;
	private TextView mLabel1, mLabel2;
	private ImageView mImage1;
	private char mCurrentStatus;
	private Handler mHandler;
	private int mLineCount;
	private int mOccupancyCount;  // number of times room was occupied
	private int mAlertCount;      // number of times pre-release warning beep was sounded
	private int mVacatedCount;    // number of times room was actually released
	
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ((Button)findViewById(R.id.button1)).setOnClickListener(this);
        ((Button)findViewById(R.id.button2)).setOnClickListener(this);
        ((Button)findViewById(R.id.button3)).setOnClickListener(this);
        mLabel1 = (TextView)findViewById(R.id.textView1);
        mLabel2 = (TextView)findViewById(R.id.textView2);
        mImage1 = (ImageView)findViewById(R.id.imageView1);
        mImage1.setImageResource(R.raw.off);
        
        mCurrentStatus = 'E';
        mLineCount = 0;  // for status messages 
        mOccupancyCount = 0;
        mVacatedCount = 0;
        mAlertCount = 0;
                
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);  
        String macId = prefs.getString("macid", "00:00:00:00:00");      
        G.trace("MAC id: " +macId);
        showLog("MAC id: " +macId);
        Config.mCurrentDeviceMAC = macId; // BTHelper takes the MAC from Config.
        mBluetoothHelper = new BlueHelper();
        initHandler();
        startBT();  
    }

    private void initHandler() {
		mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (msg.what <= Config.MAX_CONTROL_MSG) {
					showLog (Config.displayMessages[msg.what]);
					if (msg.what <= Config.MAX_ERROR_MSG)  { // some error occurred
						G.trace2("BT Error message: "+msg.what);
						mImage1.setImageResource(R.raw.off);
						mCurrentStatus = 'E';
					}
					return;
				}
				switch (msg.what) {
					//case (Config.DATA_BYTEARRAY):
					//case (Config.DATA_STRING):
					case (Config.DATA_CHAR):
						char cmd = (char)msg.arg1;
						if (cmd != '\r' && cmd != '\n')
							displayStatus(cmd);
						break;
					default:
						G.trace("Unexpected data type !");
						break;					
				}
			}
		};
    }

	@Override public void onDestroy(){
		G.trace("---OnDestroy---");
		stopBT();
	    super.onDestroy();		
	}  
	
	private void startBT() {		
		mImage1.setImageResource(R.raw.off);
		mBluetoothHelper.bootStart(mHandler);
	}
	
	private void stopBT() {
		mBluetoothHelper.exit();  
		mImage1.setImageResource(R.raw.off);
	}
	
	private void showLog(String msg) {
		if (!msg.endsWith("\n"))
			msg = msg+"\n";
		mLineCount++;
		if (mLineCount > 15) {
			mLabel1.setText(msg);
			mLineCount = 0;
		}
		else
			mLabel1.append(msg);		
	}
    
    private void displayStatus (char cmdChar) {
    	G.trace("command: " +cmdChar);
    	if (cmdChar==mCurrentStatus)
    		return;    	   	// this prevents initial UI update
    	G.trace("that was a new command !");
		if (cmdChar=='E') {  // error
			mCurrentStatus = cmdChar;
			mImage1.setImageResource(R.raw.off);
		}
		else  if (cmdChar=='0') { // vacant
			mCurrentStatus = cmdChar;
			mImage1.setImageResource(R.raw.green);
	        mVacatedCount++;
		}
		else if (cmdChar=='1') {  // occupied
			mCurrentStatus = cmdChar;
			mImage1.setImageResource(R.raw.red);
	        mOccupancyCount++;
		}
		else if (cmdChar=='2') {  // on the brink of releasing the room
			mAlertCount++;
		}		
		else if (cmdChar=='R') 
			G.trace("Occupancy sensor IoT starting..");
		else {
			G.trace2("Unexpected lamp colour ! (" +cmdChar +")");
			mImage1.setImageResource(R.raw.off);
		}
		StringBuilder builder = new StringBuilder();
		builder.append(mOccupancyCount);
		builder.append("-");
		builder.append(mVacatedCount);
		builder.append("-");
		builder.append(mAlertCount);
		mLabel2.setText(builder.toString());
    }
    
	@Override
	public void onClick(View arg0) {
		G.trace("On Button Click...");
		switch(arg0.getId()) {
		case (R.id.button1):
			mBluetoothHelper.sendMessage("a");  // on
			break;
		case (R.id.button2):
			mBluetoothHelper.sendMessage("b");  // off
			break;		
		case (R.id.button3):
			mBluetoothHelper.sendMessage("c");  // auto mode
			break;				
		}
	}
	
	// TODO: can you move this to BlueHelper ? you have to pass 'this' pointer to it
	private void showDeviceDialog() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);  
        String macId = prefs.getString("macid", "00:00:00:00:00");      
        showLog("MAC: " +macId);
        int selectedIndex = 0;
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        int n = pairedDevices.size();
        final String[] deviceList = new String[n];
        int i = 0;
        for(BluetoothDevice bt : pairedDevices) {
            String mac = bt.getAddress();
            String name = bt.getName().trim();
            if (!name.startsWith("HC-05"))
            	continue;
            name = name +"\n" +mac;
            deviceList[i] = name;
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
        }});
        // create and show the alert dialog
        builder.show();
    }    
    
    // call back method from device selection dialog
    public void getChoice(String choice) {
        G.trace("New device:" +choice);
        String[] frags = choice.split("\n");
        String macId = frags[1];
        mLabel1.setText("MAC id: " +macId );
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);    
        prefs.edit().putString("macid", macId).commit();
        Config.mCurrentDeviceMAC = macId;
//        stopBT();
//        startBT();
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
        	showDeviceDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

package com.raja.occupancy2;

import java.lang.ref.WeakReference;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
        mBluetoothHelper = new BlueHelper(Config.mCurrentDeviceMAC);
        initHandler();
        startBT();  
    }

    private void initHandler() {
		mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (msg.what <= Config.MAX_CONTROL_MSG) {
					showLog (Config.displayMessages[msg.what]);
					if (msg.what > Config.MAX_HEALTHY_MSG)
						mImage1.setImageResource(R.raw.off);
					return;
				}
				switch (msg.what) {
					//case (Config.DATA_BYTEARRAY):
					//case (Config.DATA_STRING):
					case (Config.DATA_CHAR):
						char cmd = (char)msg.arg1;
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
		mLineCount++;
		if (mLineCount > 15) {
			mLabel1.setText(msg);
			mLineCount = 0;
		}
		else
			mLabel1.append(msg);		
	}
    
    private void displayStatus (char cmdChar) {
    	if (cmdChar==mCurrentStatus)
    		return;    	   	

		if (cmdChar=='E') {  // error
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
		else if (cmdChar=='2') {  // on the brink of error
			mAlertCount++;
		}		
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
}

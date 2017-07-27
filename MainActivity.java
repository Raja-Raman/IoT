package com.raja.occupancy1;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.support.v7.app.ActionBarActivity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.os.Environment;
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

/*
	<uses-permission android:name="android.permission.BLUETOOTH" />
	<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
	<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" /> 
	<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
*/
public class MainActivity extends ActionBarActivity implements OnClickListener{
	private TextView mLabel1, mLabel2, mLabel3;
	private Button mButton1, mButton2;
	private BlueHelper mBluetoothHelper;
	private Handler mHandler;
	private int mLineCount;
	private StringBuilder mBuffer;
	private static String START_DELIMITER = "[";
	private static String END_DELIMITER = "]";
	boolean mLogDisabled;
	private FileOutputStream mLogStream;
	private OutputStreamWriter mLogWriter;
    SimpleDateFormat mTimeStampFormatter;
    private boolean mOccupied;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mButton1 = ((Button)findViewById(R.id.button1));
        mButton1.setOnClickListener(this);
        mButton2 = ((Button)findViewById(R.id.button2));
        mButton2.setOnClickListener(this);
        mLabel1 = (TextView)findViewById(R.id.textView1);
        mLabel2 = (TextView)findViewById(R.id.textView2);
        mLabel3 = (TextView)findViewById(R.id.textView3);
   	    mLineCount = 0;
   	    mLogDisabled = false;
		mOccupied = true;
	    mButton1.setBackgroundColor(Color.RED);
	    mButton2.setBackgroundColor(Color.LTGRAY);
	    mLabel2.setText("occu");
   	    initLogFile();
   	    mTimeStampFormatter = new SimpleDateFormat("HH:mm:ss,"); // comma is needed for CSV file
   	    mBuffer = new StringBuilder(512);
        mBluetoothHelper = new BlueHelper(Config.mCurrentDeviceMAC);
        initHandler();
        startBT();         
    }

	@Override public void onDestroy(){
		G.trace("---OnDestroy---");
		try {
			if (mLogWriter != null)
				mLogWriter.close();
			if (mLogStream != null) {
		        mLogStream.flush();
		        mLogStream.close();	
			}
			G.trace("Log File closed");
		}
		catch(IOException e) {
			G.trace("Cannot close log file: " +e.getMessage());
		}
		stopBT();
	    super.onDestroy();		
	}  
	
	private void startBT() {		
		mBluetoothHelper.bootStart(mHandler);
	}
	
	private void stopBT() {
		mBluetoothHelper.exit();  
	}
	
	private void showLog(String msg) {
		mLineCount++;
		if (mLineCount > 20) {
			mLabel1.setText(msg);
			mLineCount = 0;
		}
		else
			mLabel1.append(msg);		
	}
	
    private void initHandler() {
		mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (msg.what < Config.MAX_CONTROL_MSG) {  // command & control message
					showLog(Config.displayMessages[msg.what]);
					return;
				}				
				switch (msg.what) {
					//case (Config.DATA_CHAR):
					//case (Config.DATA_BYTEARRAY):
					case (Config.DATA_STRING):				
						//int numbytes = msg.arg1;
						mBuffer.append((String)msg.obj);
						processPacket();
						break;	
					default:
						G.trace("Unexpected data type: " +msg.what);
						showLog("Unexpected data type");
						break;					
				} // switch
			}
		};
    }
    
//    private void dataError(String errMsg) {
//    	mLabel3.setText("ERR");
//    }
    
    private void processPacket() {
    	if (mBuffer.length()==0) {
    		mLabel3.setText("BLNK");
    		G.trace("Data error: Empty payload");
    		return;
    	}    	
    	G.trace("Processing:" +mBuffer.toString());
    	//showLog("->" +mBuffer.toString()); 
		int opening = mBuffer.indexOf(START_DELIMITER); 
		int closing = mBuffer.lastIndexOf(END_DELIMITER); 
		if (opening < 0 || closing < 0 || opening >= closing) { // it is a partial packet
			G.trace("-Partial packet-");
			return;
		}
		String str = mBuffer.substring(opening+1, closing); // discard both delimiters
		mBuffer.delete(0, closing+1); // discard leading junk chars and the just saved packet
		str = str.trim();
		str = str.replace("][", " "); // needed to avoid double spaces for split()
		str = str.trim();
		str = str.replace("]", " "); // these are needed to handle partial packets
		str = str.trim();
		str = str.replace("[", " ");
		str = str.trim();		
		String[] splitstr = str.split(" "); 
		//G.trace("# of fragments= " +splitstr.length);
		for (int i=0; i<splitstr.length; i++) {
			splitstr[i] = splitstr[i].trim();
			if(splitstr[i].length() > 0)
				processFragment(splitstr[i]);
		}
    }
    
	// A fragment is the string between one pair of start and end delimiters
	private void processFragment (String fragment) {
	    //G.trace("Processing fragment: " +fragment);		
    	if (fragment.startsWith("E") || fragment.startsWith("R")) // || str.startsWith("C") 
				mLabel3.setText(fragment);
		else	 
		if (fragment.startsWith("S") || fragment.startsWith("s"))
			mLabel2.setText(fragment);							
		else
		if (fragment.startsWith("D")) {
			fragment = fragment.substring(1); // remove 'D'	
			if (!mLogDisabled)
				save2File (fragment);
		}
		else {
			mLabel3.setText("ERR");
			G.trace("Fragment Error: " +fragment);
			showLog("+Error: " +fragment);
		}
    }
    
    private void save2File (String str) {
    	try {
       	    String ts = mTimeStampFormatter.format(new Date());
       	    StringBuilder builder = new StringBuilder(40);
       	    builder.append(ts); // this already contains a comma at the end
       	    if (mOccupied)
       	    	builder.append("1,");
       	    else
       	    	builder.append("0,");
       	    builder.append(str); // str from Arduino must be comma separated values
       	    builder.append("\n");
    		mLogWriter.append(builder.toString());
    		showLog(">>" +builder.toString());  // TODO: disable these 
        	//G.trace("Saved: " +builder.toString());
    	}
    	catch (IOException e) {
    		G.trace("Could not write to log file: " +e.getMessage());
    		mLogDisabled = true;
    	}
    }
    
	@Override
	public void onClick(View arg0) {
		G.trace("On Button Click...");
		switch(arg0.getId()) {
		case (R.id.button1):
			//mBluetoothHelper.sendMessage("a");
			mOccupied = true;
		    mButton1.setBackgroundColor(Color.RED);
		    mButton2.setBackgroundColor(Color.LTGRAY);
		    mLabel2.setText("occu");
			break;
		case (R.id.button2):
			//mBluetoothHelper.sendMessage("b");
			mOccupied = false;
			mButton2.setBackgroundColor(Color.RED);
			mButton1.setBackgroundColor(Color.LTGRAY);
			mLabel2.setText("free");
			break;		
		}
	}
	
    private void initLogFile() 
    {
        File sdcardRoot = Environment.getExternalStorageDirectory();
        G.trace("sdcardRoot: "+sdcardRoot);
        G.trace("sdcardRoot-absolute: "+sdcardRoot.getAbsolutePath());
        File dir  = new File(sdcardRoot +File.separator +"Raja" +File.separator +"Occupancy");
		if (!dir.exists())
			dir.mkdir();
		String logDirectory = dir.toString()+File.separator;
        G.trace("Log directory: " +logDirectory); 
        
        SimpleDateFormat formatter = new SimpleDateFormat("d-MMM_HH-mm");
        String fileName = "Log_" + formatter.format(new Date()) +".csv";
	    G.trace("Log file name: " +fileName);
	    
	    File file = new File(logDirectory, fileName);
	    // Save your stream, don't forget to flush() it before closing it.
	    mLogDisabled = false;
	    try
	    {
	        file.createNewFile();
	        this.mLogStream = new FileOutputStream(file);
	        this.mLogWriter = new OutputStreamWriter(mLogStream);
	        if (mLogStream==null || mLogWriter==null)
	        	mLogDisabled = true;
	        else
	        	G.trace("Log file created");
	    }
	    catch (IOException e)
	    {
	        G.trace("Logging failed: " + e.toString());
	        mLogDisabled = true;
	    }
	    finally {
	    	if (mLogDisabled)
	    		showLog("LOG DISABLED");
	    	else
	    		showLog("Logging to: " +fileName +"\n");
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

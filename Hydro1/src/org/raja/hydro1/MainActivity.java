package org.raja.hydro1;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
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

public class MainActivity extends ActionBarActivity implements OnClickListener{
	private TextView mLabel1, mLabel2, mLabel3, mLabel4;
	private BlueHelper mBluetoothHelper;
	private Handler mHandler;
	private int mLineCount;
	private byte[] mRxBuffer;
	private static int RX_BUFFER_SIZE = 512;
	int mReadIndex, mWriteIndex;
	private static String START_DELIMITER = "[";
	private static String END_DELIMITER = "]";
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ((Button)findViewById(R.id.button1)).setOnClickListener(this);
        ((Button)findViewById(R.id.button2)).setOnClickListener(this);
        ((Button)findViewById(R.id.button3)).setOnClickListener(this);
        ((Button)findViewById(R.id.button4)).setOnClickListener(this);
        ((Button)findViewById(R.id.button5)).setOnClickListener(this);
        ((Button)findViewById(R.id.button6)).setOnClickListener(this);  
        ((Button)findViewById(R.id.button7)).setOnClickListener(this); 
        mLabel1 = (TextView)findViewById(R.id.textView1);
        mLabel2 = (TextView)findViewById(R.id.textView2);
        mLabel3 = (TextView)findViewById(R.id.textView4); 
        mLabel4 = (TextView)findViewById(R.id.textView3);
        mRxBuffer = new byte[RX_BUFFER_SIZE];
        mReadIndex = 0;
        mWriteIndex = 0;
    	mLineCount = 0;
        mBluetoothHelper = new BlueHelper(Config.mCurrentDeviceMAC);
        initHandler();
        startBT();           
    }

	@Override public void onDestroy(){
		G.trace("---OnDestroy---");
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
		if (mLineCount>10) {
			mLabel4.setText(msg);
			mLineCount = 0;
		}
		else
			mLabel4.append(msg);		
	}
	
    private void initHandler() {
		mHandler = new Handler() {
			boolean overflow = false;  // buffer folded back and crossed read pointer again
			@Override
			public void handleMessage(Message msg) {
				if (msg.what < Config.MAX_CONTROL_MSG) {  // command & control message
					showLog(Config.displayMessages[msg.what]);
					return;
				}				
				switch (msg.what) {
					//case (Config.DATA_CHAR):
					//case (Config.DATA_STRING):				
					case (Config.DATA_BYTEARRAY):
						int numbytes = msg.arg1;
						if (numbytes > RX_BUFFER_SIZE) {
							G.trace2("--- Rx Buffer inundated ! ---");
							showLog("Rx Buffer inundated !");
							/** comment this out for production: */ 
							throw new RuntimeException("Rx Buffer inundated");
						}
						byte[] rxbytes = (byte[])msg.obj;
						for (int i=0; i<numbytes; i++) {
							mRxBuffer[mWriteIndex] = rxbytes[i];
							mWriteIndex = (mWriteIndex+1)% RX_BUFFER_SIZE;
				        	if (mWriteIndex == mReadIndex) {
				        		overflow = true;
				        		mReadIndex = (mWriteIndex+1) % RX_BUFFER_SIZE; // +1 is needed to differentiate from empty buffer
				        	}
				        } // for
			        	if (overflow){
			        		G.trace("------ PANIC: Buffer overflow !!!-----------");
			        		showLog("Rx Buffer overflow !!");
			        		overflow = false;
			        	}
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
    
    // process the packet (in global Rx buffer) and move the read pointer to the new position 
    private void processPacket() {
		if (mReadIndex==mWriteIndex) {
			G.trace("--- Unexpected behaviour: process() called without fresh data ---");
			return;
		}
		if (mReadIndex < mWriteIndex) {
			String str = new String(mRxBuffer, mReadIndex,(mWriteIndex-mReadIndex));
			G.trace("Input: " +str);
			int offset = parse (str);
			mReadIndex = (mReadIndex +offset +1) % RX_BUFFER_SIZE;
			return;
		}
		G.trace("-Buffer folded back-");
		String str1 = new String(mRxBuffer, mReadIndex,(mRxBuffer.length-mReadIndex));
		//G.trace("Input1: " +str1);
		String str2 = new String(mRxBuffer, 0,mWriteIndex);
		//G.trace("Input2: " +str2);		
		String str = str1.concat(str2);
		G.trace("Combined Input: " +str);
		int offset = parse (str);
		mReadIndex = (mReadIndex +offset +1) % RX_BUFFER_SIZE;
	}
    	
    private int parse (String str) {
		int opening = str.indexOf("["); 
		//G.trace("first [: "+opening);
		if (opening < 0) // no packet or corrupted
			return (-1);
		int closing = str.lastIndexOf("]"); 
		//G.trace("last ]: "+closing);		
		if (closing < 0)  // it is a partial packet
			return (-1);
		String sub = str.substring(opening, closing+1);
		//G.trace("Trimmed: " +sub);		
		sub = sub.replace("][", " ");
		sub = sub.trim();
		sub = sub.replace("[", " ");
		sub = sub.trim();
		sub = sub.replace("]", " ");
		sub = sub.trim();
		//G.trace("before splitting: " +sub);
		String[] splitstr = sub.split(" "); 
		//G.trace("# of fragments= " +splitstr.length);
		for (int i=0; i<splitstr.length; i++) {
			splitstr[i] = splitstr[i].trim();
			if(splitstr[i].length() > 0)
				processFragment(splitstr[i]);
		}
		return closing;
	}
   
    // A fragment is the string between one pair of start and end delimiters
    private void processFragment (String fragment) {
    	//G.trace("Processing fragment: " +fragment);
    	if (fragment.startsWith("C") || fragment.startsWith("E"))
				mLabel2.setText(fragment);
		else	 
		if (fragment.startsWith("S") || fragment.startsWith("s"))
			mLabel3.setText(fragment);							
		else
		if (fragment.startsWith("D"))
		{
			try {
				fragment = fragment.substring(1);
				int lightLevel = Integer.parseInt(fragment);
				mLabel1.setText(fragment);					
				send2Cloud (lightLevel);
			}
			catch(NumberFormatException e)
			{G.trace2(e.getMessage());}
		}
		else {
			mLabel2.setText("ERR");
			G.trace("Fragment Error: " +fragment);
			showLog("Error: " +fragment);
		}
    }
    
    private void send2Cloud (int lightLevel) {
    	G.trace("Sending to cloud (stub): " + lightLevel);  // TODO
    }
    
	@Override
	public void onClick(View arg0) {
		G.trace("On Button Click...");
		switch(arg0.getId()) {
		case (R.id.button1):
			mBluetoothHelper.sendMessage("a");
			break;
		case (R.id.button2):
			mBluetoothHelper.sendMessage("b");
			break;		
		case (R.id.button3):
			mBluetoothHelper.sendMessage("c");
			break;	
		case (R.id.button4):
			mBluetoothHelper.sendMessage("d");
			break;	
		case (R.id.button5):
			mBluetoothHelper.sendMessage("e");
			break;	
		case (R.id.button6):
			mBluetoothHelper.sendMessage("f");
			break;				
		case (R.id.button7):
			mBluetoothHelper.sendMessage("s");
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

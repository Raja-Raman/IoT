package com.raja.occupancy1;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.UUID;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;

public class BlueHelper  {
	// Generic serial device UUID is: "00001101-0000-1000-8000-00805F9B34FB";
	volatile boolean mStopFlag = false;
	volatile boolean mExitFlag = false;
	volatile boolean mWorkerActive = false;
	BluetoothAdapter mBtAdapter; 
	Handler mHandler;
    BluetoothSocket mWorkerSocket = null;
    DataInputStream mInStream=null;
    DataOutputStream mOutStream = null;
    boolean mAllowInsecureConnection = true;  //false; //
    String mCurrentDeviceMAC;
    
    public BlueHelper (String serverMAC) {
    	this.mCurrentDeviceMAC = serverMAC;
    }
    
    public void bootStart(Handler handler) {
    	this.mHandler = handler;    	
    	new ConnectorThread().start();
    }
    
    public void exit() {
    	G.trace2("--EXIT called. Stopping everything--");
    	mExitFlag = true;
    	closeAll();
    	asleep(5000);
    }
    
	public boolean initAdapter () {
    	G.trace("Initializing bluetooth adapter...");
    	mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    	if (mBtAdapter == null) {
    		G.trace("bluetooth adapter NOT found!");
			return false;
    	}
		G.trace("bluetooth adapter is present");
    	// user interaction is needed for this approach:
    	// mParentActivity.startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE));  
    	if (!mBtAdapter.isEnabled()){
    		G.trace("Enabling bluetooth adapter...");  
    		mBtAdapter.enable();
    	}
    	// it takes over 3.5 sec. on Android pad  
    	int count=0;
    	int WAIT=200;
    	for (; count<25; count++) {
    		if (mStopFlag)
    			return false;
    		if (mBtAdapter.isEnabled())
    			break;
    		asleep(WAIT);
    	}
    	if (!mBtAdapter.isEnabled()) {
    		G.trace("Bluetooth adapter could not be enabled !");
    		postMessageToUI(Config.BT_ADAPTER_ERROR);
    		return false;
    	}    	
    	G.trace("Bluetooth adapter is enabled (" +count*WAIT +" mSec)");
    	postMessageToUI(Config.BT_ADAPTER_OK);		
		return true;
	}
	
    public boolean connectToServer()
    {
    	BluetoothSocket tmpsocket = null;
        try {
        	BluetoothDevice remoteDevice = mBtAdapter.getRemoteDevice(this.mCurrentDeviceMAC);  
        	if (mAllowInsecureConnection) {
	    		Method method;
	    		method = remoteDevice.getClass().getMethod("createRfcommSocket", new Class[] { int.class } );
	    		tmpsocket = (BluetoothSocket) method.invoke(remoteDevice, 1);
	    		G.trace("Method invoked for temp socket");
        	}
        	else
        		tmpsocket = remoteDevice.createRfcommSocketToServiceRecord(UUID.fromString(Config.SERVICE_UUID));  
        		G.trace("temp socket created from UUID");
        		//tmpsocket = remoteDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString(Config.SERVICE_UUID)); 
        		//G.trace("temp Insecure socket created from UUID");
        } 
        catch (Exception e) { 
        	G.trace("--Unable to create RFcomm socket--");
        	postMessageToUI(Config.RFCOMM_ERROR);
        	G.trace(e.getMessage());
        	//e.printStackTrace();
        	return false;
        }    	
        if (tmpsocket==null)  // TODO: post error message to UI
        {
        	G.trace("--RFcomm socket in NULL--");
        	postMessageToUI(Config.NULL_SOCKET_ERROR);
        	return false;
        }        
        mWorkerSocket = tmpsocket;
        G.trace("RFcomm socket created");
        postMessageToUI(Config.SOCKET_CREATED);
        
        // do this only if discovery is in progress?
        mBtAdapter.cancelDiscovery(); 
        
        G.trace("Trying to connect to blue tooth server...");
        try {        
        	mWorkerSocket.connect();
        }         
        catch (IOException e) { 
        	G.trace("--Unable to connect to server socket--");   
        	postMessageToUI(Config.SERVER_CONNECTION_ERROR);        	
        	G.trace(e.getMessage());
        	//e.printStackTrace();
        	closeAll();  
            return false;
        }        
        postMessageToUI(Config.SERVER_CONNECTED);        
        G.trace("Connected to server socket.");
        try {
             mOutStream = new DataOutputStream(mWorkerSocket.getOutputStream());        	
             mInStream = new DataInputStream(mWorkerSocket.getInputStream());
             //mInStream.reset(); //flush junk
             while(mInStream.available() > 0)
            	 mInStream.read();
	    } 
        catch (IOException e) {
          G.trace("Cannot get socket I/O stream");
      	  G.trace(e.getMessage());
      	  //e.printStackTrace();
	      postMessageToUI(Config.IO_STREAM_ERROR);
	      closeAll();  
	      return false;
	   }          
        G.trace("Obtained the socket I/O streams");
        postMessageToUI(Config.IO_STREAM_OK);
    	return true;
    }
    	
    private void asleep (int milliSeconds) {
		try { Thread.sleep(milliSeconds); }    
		catch (InterruptedException e){}    	
    }
    
    private void postMessageToUI (int status)
    {
		Message msg = mHandler.obtainMessage(status);
		mHandler.sendMessage(msg);
    }	
    
    private void postMessageToUI (char charData)
    {
		Message msg = mHandler.obtainMessage(Config.DATA_CHAR);
		msg.arg1 = charData;
		mHandler.sendMessage(msg);
    }
    
    private void postMessageToUI (byte[] byteData, int numBytes)
    {
		Message msg = mHandler.obtainMessage(Config.DATA_BYTEARRAY);
		msg.arg1 = numBytes;
		msg.obj = byteData;
		mHandler.sendMessage(msg);
    }
    
    private void postMessageToUI (String strData)
    {
		Message msg = mHandler.obtainMessage(Config.DATA_STRING);
		msg.arg1 = strData.length();
		msg.obj = (Object)strData;
		mHandler.sendMessage(msg);
    }
    
    // send message over the socket to the blue tooth server
    public void sendMessage (char charData) {
    	if (mWorkerSocket==null || mOutStream==null) {
    		G.trace("--Socket or stream is null--");
    		postMessageToUI (Config.CONNECTION_LOST);
    		return;
    	}
    	try {
    		mOutStream.write(charData);
    	} catch (Exception e) {
    		G.trace("--Socket error--");
    		G.trace(e.getMessage());
    		postMessageToUI (Config.CONNECTION_LOST);
    		return;
    	}
    }
    
    // send message over the socket to the blue tooth server
    public void sendMessage (String strData) {
    	if (strData==null || strData.length()==0)
    		return;
    	if (mWorkerSocket==null || mOutStream==null) {
    		G.trace("--Socket or stream is null--");
    		postMessageToUI (Config.CONNECTION_LOST);
    		return;
    	}
    	try {
    		mOutStream.write(strData.getBytes());
    	} catch (Exception e) {
    		G.trace("--Socket error--");
    		G.trace(e.getMessage());
    		postMessageToUI (Config.CONNECTION_LOST);
    		return;
    	}
    }

    public void closeAll () {
    	closeAll(false);
    }
    
	public void closeAll (boolean disableAdapter) {
    	postMessageToUI(Config.QUIT_MSG);
    	stopWorkerThread();
    	closeBluetooth(disableAdapter);
	}
	
    private void stopWorkerThread() {
    	mStopFlag = true; // NOTE: this will end the worker thread loop
    	G.trace("Stopping worker thread...");
    	// this.join();
    	for (int i=0; i<12; i++) {
        	if (!mWorkerActive)
        	   break;
        	asleep(500); 
    	}
    	if (!mWorkerActive)
    	   G.trace("Worker thread has finished");
    	else
    	   G.trace("--ERROR: Worker thread is still active !--");
    }
    
    private void closeBluetooth(boolean disableAdapter) {
    	G.trace("Shutting down sockets...");
    	try
    	{
	    	if (mInStream != null) mInStream.close();   
	    	if (mOutStream != null) mOutStream.close();   
	    	if (mWorkerSocket != null) mWorkerSocket.close(); 
	    	G.trace("Sockets are closed.");
	    	if (mBtAdapter != null && disableAdapter && mBtAdapter.isEnabled()) {
	    		mBtAdapter.disable(); 	  
		    	G.trace("Bluetooth adapter disabled.");
	    	}
    	}  
		catch (IOException e) 
		{ 
	    	G.trace2("--- Unable to finish clean up ---");
	    	G.trace(e.getMessage());
		}
    }
//-------------------------------------------------------------------------  
    
    class ConnectorThread extends Thread {
        public void run() {
        	 G.trace("Connector thread starts...");
			 while (!mExitFlag) {
				 G.trace("<watch dog>");
				 if (!mWorkerActive) 
					 reconnect();
				 for (int i=0; i<10; i++) {
					 if (mExitFlag)
						 break;
					 asleep(1000);
				 }
			 } // while not Exit Flag
			 G.trace("Connector thread exits.");
        }
        
        private void reconnect() {
        	G.trace("Reconnecting...");
        	if (mExitFlag)
        		return;        	
        	postMessageToUI(Config.RECONNECTING);
	    	closeAll();
	    	mStopFlag = false;
	    	if (initAdapter())
	    		if (connectToServer())
	    			new CommunicatorThread().start();
        }
    }
//-------------------------------------------------------------------------    
    
    class CommunicatorThread extends Thread {
        public void run() {
        	mWorkerActive = true;
        	G.trace("Starting worker thread...");
        	byte[] buffer = new byte [255];
        	String msgString;
        	int numBytesReceived = 0;
        	while (!mStopFlag) {
    	    	try {
    		    	//if (mInStream.available() > 0) { // THIS prevented the exception from being thrown!!
    		    		numBytesReceived = mInStream.read(buffer);
    		    		/****
    		    		for (int i=0; i<numBytesReceived; i++)
    		    			postMessageToUI ((char)buffer[i]);
    		    		****/
    		    		//G.trace(buffer);
    		    		/***
    		    		G.trace("<Bytes>: " +numBytesReceived);
    		    		postMessageToUI (buffer, numBytesReceived);
    		    		***/
    		    		msgString = new String(buffer, 0,numBytesReceived); // offset,length
    		    		postMessageToUI(msgString);
    		    	//}
    	    	}
    	    	catch (Exception e) {
    	    		G.trace("--Socket connection lost--");
    	    		postMessageToUI(Config.CONNECTION_LOST);
    	    		break;
    	    	}
        	} // while !stop_flag
        	G.trace("Worker thread exits.");
        	mWorkerActive = false;
        }    	
    }
}

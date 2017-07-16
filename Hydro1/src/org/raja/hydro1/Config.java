package org.raja.hydro1;

public class Config {
	// Well known UUID for serial devices:
	public static String SERVICE_UUID = "00001101-0000-1000-8000-00805F9B34FB";  // generic serial device
	public static String SERVER_MAC_ADDRESS1 = "98:D3:32:70:C6:8E";  // HC-05
	public static String SERVER_MAC_ADDRESS2 = "98:D3:33:80:CE:25";  // HC-05
	public static String SERVER_MAC_ADDRESS3 = "B8:27:EB:AC:45:DB";  //  Raspberry Pi
	
	public static String mCurrentDeviceMAC = SERVER_MAC_ADDRESS1;  // TODO: save this to preferences
	
	public static final int  DATA_CHAR = 90;
	public static final int  DATA_BYTEARRAY = 91;
	public static final int  DATA_STRING = 92;
	
	public static final int  BT_ADAPTER_ERROR = 0;
	public static final int  NULL_SOCKET_ERROR = 1;
	public static final int  RFCOMM_ERROR = 2;
	public static final int  SERVER_CONNECTION_ERROR = 3;
	public static final int  IO_STREAM_ERROR = 4;
	public static final int  CONNECTION_LOST = 5;
	
	public static final int  BT_ADAPTER_OK = 6;
	public static final int  SOCKET_CREATED = 7;
	public static final int  SERVER_CONNECTED = 8;
	public static final int  IO_STREAM_OK = 9;	
	public static final int  RECONNECTING = 10;
	public static final int  QUIT_MSG = 11;	
	public static final int  MAX_CONTROL_MSG = 12; // always 1 more than the last control message
	
	public static final String[] displayMessages = {
		"- Bluetooth adapter error\n",
		"- The BT socket is null\n",
		"- Could not initialize RFCOMM\n",
		"- Could not connect to BT server\n",		
		"- Could not get IO stream\n",
		"- BT connection is lost\n",
		"+ BT adapter initialized\n",
		"+ Socket created\n",
		"+ Connected to BT server\n",
		"+ IO stream obtained\n",
		"* Attempting to reconnect...\n",
		"* Quitting !\n"
	};
}

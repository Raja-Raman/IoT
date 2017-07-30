package com.raja.occupancy2;

import android.util.Log;

public class G {
	public static String TAG="occupy";
    public static boolean debug = true;
    
    public static void trace (Object o) {
        if (!debug) return; 
        if (o==null) o = "(null)";
        Log.i(TAG, o.toString());
    }
    
    public static void trace2 (Object o) {
        if (o==null) o = "(null)";
        Log.i(TAG, o.toString());
    }
}
package org.raja.hydro1;

import android.util.Log;

public class G {
	public static String TAG="hydro";
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
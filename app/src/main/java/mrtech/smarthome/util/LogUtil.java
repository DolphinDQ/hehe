package mrtech.smarthome.util; /**
 * 
 */

import mrtech.smarthome.BuildConfig;
import android.util.Log;


/**
 * 类似android.util.Log的功能，暂时只支持打印到logcat中
 * @author csexttt
 * @date
 * @version 1.0
 */
public class LogUtil
{
	/**
	 * @see Log
	 * @param tag Log's tag
	 * @param message Log's message
	 */
	public static void d(String tag, String message)
	{
		if(BuildConfig.DEBUG)
			Log.d(tag, message);
	}
	
	public static void i(String tag, String message)
	{
		if(BuildConfig.DEBUG)
			Log.i(tag, message);
	}
	
	public static void w(String tag, String message)
	{
		if(BuildConfig.DEBUG)
			Log.w(tag, message);
	}
	
	public static void e(String tag, String message)
	{
		if(BuildConfig.DEBUG)
			Log.e(tag, message);
	}
}

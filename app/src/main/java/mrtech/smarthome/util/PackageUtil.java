package mrtech.smarthome.util; /**
 * 
 */

import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

/**
 * 包辅助类
 * @author CJ
 * @version 1.0
 * @date 2014-12-4 上午10:39:17
 * @function
 */
public class PackageUtil
{
	/**
	 * 获取当前版本
	 * @param context
	 * @return
	 */
	public static int getSystemVersionCode(Context context)
	{
		int versionCode = 0;
		try
		{
			PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_CONFIGURATIONS);
			versionCode = pi.versionCode;
		} catch (NameNotFoundException e)
		{
			e.printStackTrace();
		}
		return versionCode;
	}
	
	/**
	 * 获取当前版本名称
	 * @param context
	 * @return
	 */
	public static String getSystemVersionName(Context context)
	{
		String versionName = "";
		try
		{
			PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_CONFIGURATIONS);
			versionName = pi.versionName;
		} catch (NameNotFoundException e)
		{
			e.printStackTrace();
		}
		return versionName;
	}
	
	/**
	 * 判断app是否在运行，无论是前台还是后台�?
	 * @param context
	 * @param packageName
	 * @return
	 */
	public static boolean isAppRunning(Context context, String packageName)
	{
		ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningTaskInfo> list = am.getRunningTasks(100);
		for (RunningTaskInfo info : list)
		{
			if (info.topActivity.getPackageName().equals(packageName) && info.baseActivity.getPackageName().equals(packageName))
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * �?��某Activity在前台还是在后台
	 */
	public static boolean isTopActivity(Context context, String packageName)
	{
		ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningTaskInfo> tasksInfo = am.getRunningTasks(1);
		if (tasksInfo.size() > 0)
		{
			// 应用程序位于堆栈的顶�?
			if (packageName.equals(tasksInfo.get(0).topActivity.getPackageName()))
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 获取渠道名称
	 * @param context
	 * @return
	 */
	public static String getChannelName(Context context)
	{
		String channelName = "";
		try
		{
			ApplicationInfo applicationInfo = context
					.getPackageManager()
					.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
			channelName = applicationInfo.metaData.getString("UMENG_CHANNEL");
		}
		catch (NameNotFoundException e)
		{
			e.printStackTrace();
		}
		return channelName;
	}
	
	public static String getCurrentActivityName(Context context)
	{
		ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningTaskInfo> tasksInfo = am.getRunningTasks(1);
		if (tasksInfo.size() > 0)
		{
			return tasksInfo.get(0).topActivity.getClassName();
		}
		return "";
	}
}

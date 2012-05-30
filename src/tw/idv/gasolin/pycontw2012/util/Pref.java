package tw.idv.gasolin.pycontw2012.util;

import java.io.File;

import tw.idv.gasolin.pycontw2012.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Pref extends PreferenceActivity {

	public static final String LOCAL_PATH = Environment
	.getExternalStorageDirectory().toString()
	+ File.separator
	+ "course"
	+ File.separator
	+ "testing";
	
	public static final String UACODE = "UA-20095328-2";
	public static final String CONTENT_AUTHORITY = "tw.idv.gasolin.pycontw2012";
	
	public static final String PREF_SERVER_IP="server_ip";
	public static final String PREF_COURSE_ID="course_id";
	
	@Override protected void 
    onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref);
    }
	
	//get url from preference setting
	public static String getCourseId(Context context) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		return pref.getString(PREF_COURSE_ID,"");
	}
	
	public static String getRoomUrl(Context context) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		String server_ip = pref.getString(PREF_SERVER_IP,"");
		if("".equals(server_ip)||"".equals(pref.getString(PREF_COURSE_ID,""))){
			return context.getString(R.string.rooms_url);
		}
		return "http://"+server_ip+"/api/1/"+pref.getString(PREF_COURSE_ID,"")+"/program/rooms/";
	}
	
	public static String getTracksUrl(Context context) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		String server_ip = pref.getString(PREF_SERVER_IP,"");
		if("".equals(server_ip)||"".equals(pref.getString(PREF_COURSE_ID,""))){
			return context.getString(R.string.tracks_url);
		}
		return "http://"+server_ip+"/api/1/program/types/";
	}
	
	public static String getSessionsUrl(Context context) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		String server_ip = pref.getString(PREF_SERVER_IP,"");
		if("".equals(server_ip)||"".equals(pref.getString(PREF_COURSE_ID,""))){
			return context.getString(R.string.sessions_url);
		}
		return "http://"+server_ip+"/api/1/"+pref.getString(PREF_COURSE_ID,"")+"/program/";
	}
	
	public static String getSponsorUrl(Context context) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		String server_ip = pref.getString(PREF_SERVER_IP,"");
		if("".equals(server_ip)||"".equals(pref.getString(PREF_COURSE_ID,""))){
			return context.getString(R.string.sponsors_url);
		}
		return "http://"+server_ip+"/api/1/"+pref.getString(PREF_COURSE_ID,"")+"/program/sponsors/";
	}
}

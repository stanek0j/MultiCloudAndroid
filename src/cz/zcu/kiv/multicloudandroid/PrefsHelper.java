package cz.zcu.kiv.multicloudandroid;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.zcu.kiv.multicloud.json.AccountSettings;
import cz.zcu.kiv.multicloud.json.Json;
import cz.zcu.kiv.multicloud.utils.FileAccountManager;
import cz.zcu.kiv.multicloudandroid.display.SyncData;

/**
 * cz.zcu.kiv.multicloudandroid/PrefsHelper.java			<br /><br />
 *
 * Helper class for getting values from shared preferences.
 *
 * @author Jaromír Staněk
 * @version 1.0
 *
 */
public class PrefsHelper {

	/** Key for getting preference for showing hidden files. */
	public static final String PREFS_SHOW_DELETED = "prefs_show_deleted";
	/** Key for getting preference for showing shared files. */
	public static final String PREFS_SHOW_SHARED = "prefs_show_shared";
	/** Key for getting preference for hiding checksum cache file. */
	public static final String PREFS_HIDE_CACHE = "prefs_hide_cache";
	/** Key for getting preference for showing error dialogs. */
	public static final String PREFS_SHOW_ERR = "prefs_show_err";
	/** Key for getting number of download threads. */
	public static final String PREFS_THREADS = "prefs_threads";
	/** Key for getting synchronization folder. */
	public static final String PREFS_SYNCH_FOLDER = "prefs_synch_folder";
	/** Key for getting selective synchronization preferences. */
	public static final String PREFS_SYNCH_SELECT = "prefs_synch_select";
	/** Key for getting last local folder listed. */
	public static final String PREFS_LAST_FOLDER = "prefs_last_folder";
	/** Selective synchronization data file. */
	public static final String PREFS_SYNCH_FILE = "synchronize.json";

	/** Context. */
	private final Context context;

	/**
	 * Ctor with context as parameter.
	 * @param context Context.
	 */
	public PrefsHelper(Context context) {
		this.context = context;
	}

	/**
	 * Returns list of accounts.
	 * @return List of accounts.
	 */
	public List<String> getAccounts() {
		List<String> accounts = new ArrayList<>();
		ObjectMapper mapper = Json.getInstance().getMapper();
		try {
			Map<String, AccountSettings> data = mapper.readValue(new File(context.getFilesDir(), FileAccountManager.DEFAULT_FILE), new TypeReference<HashMap<String, AccountSettings>>() {});
			if (data != null) {
				accounts.addAll(data.keySet());
			}
		} catch (IOException e) {
			Log.e(MainActivity.MULTICLOUD_NAME, e.getMessage());
		}
		return accounts;
	}

	/**
	 * Returns the last used folder.
	 * @return Last used folder.
	 */
	public String getLastFolder() {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(PREFS_LAST_FOLDER, MainActivity.ROOT_FOLDER);
	}

	/**
	 * Returns the synchronization data.
	 * @return Synchronization data.
	 */
	public SyncData getSyncData() {
		SyncData data = new SyncData();
		ObjectMapper mapper = Json.getInstance().getMapper();
		try {
			data = mapper.readValue(new File(context.getFilesDir(), PREFS_SYNCH_FILE), SyncData.class);
			if (data != null) {
				data.setRoot(true);
			}
		} catch (IOException e) {
			Log.e(MainActivity.MULTICLOUD_NAME, e.getMessage());
		}
		return data;
	}

	/**
	 * Returns the synchronization folder.
	 * @return Synchronization folder.
	 */
	public String getSynchronizationFolder() {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(PREFS_SYNCH_FOLDER, null);
	}

	/**
	 * Returns number of download threads.
	 * @return Number of download threads.
	 */
	public int getThreads() {
		return PreferenceManager.getDefaultSharedPreferences(context).getInt(PREFS_THREADS, 1);
	}

	/**
	 * Returns if checksum cache should be hidden.
	 * @return If checksum cache should be hidden.
	 */
	public boolean isHideCache() {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREFS_HIDE_CACHE, true);
	}

	/**
	 * Returns if deleted files should be displayed.
	 * @return If deleted files should be displayed.
	 */
	public boolean isShowDeleted() {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREFS_SHOW_DELETED, false);
	}

	/**
	 * Returns if error dialogs should be displayed.
	 * @return If error dialogs should be displayed.
	 */
	public boolean isShowErr() {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREFS_SHOW_ERR, true);
	}

	/**
	 * Returns if shared files should be displayed.
	 * @return If shared files should be displayed.
	 */
	public boolean isShowShared() {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREFS_SHOW_SHARED, false);
	}

	/**
	 * Sets the last used folder.
	 * @param folder Last used folder.
	 */
	public void setLastFolder(String folder) {
		Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.putString(PREFS_LAST_FOLDER, folder);
		editor.apply();
	}

	/**
	 * Sets the synchronization data to file.
	 * @param data Synchronization data.
	 */
	public void setSyncData(SyncData data) {
		ObjectMapper mapper = Json.getInstance().getMapper();
		try {
			mapper.writerWithDefaultPrettyPrinter().writeValue(new File(context.getFilesDir(), PREFS_SYNCH_FILE), data);
		} catch (IOException e) {
			Log.e(MainActivity.MULTICLOUD_NAME, e.getMessage());
		}
	}

}

package cz.zcu.kiv.multicloudandroid;

import android.content.Context;
import android.preference.PreferenceManager;

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
	public static final String PREFS_SYNCH_SELECT = "prefs_synch_select";

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

}

package cz.zcu.kiv.multicloudandroid.fragment;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import cz.zcu.kiv.multicloudandroid.PrefsHelper;
import cz.zcu.kiv.multicloudandroid.R;
import cz.zcu.kiv.multicloudandroid.display.FilePreference;
import cz.zcu.kiv.multicloudandroid.display.NumberPreference;

/**
 * cz.zcu.kiv.multicloudandroid.fragment/PrefsFragment.java			<br /><br />
 *
 * Preferences fragment for displaying all application preferences.
 *
 * @author Jaromír Staněk
 * @version 1.0
 *
 */
public class PrefsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

	/** Preference settings for thread number. */
	private NumberPreference threads;
	/** Preference settings for synchronization folder. */
	private FilePreference synchFolder;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		threads = (NumberPreference) getPreferenceManager().findPreference(PrefsHelper.PREFS_THREADS);
		synchFolder = (FilePreference) getPreferenceManager().findPreference(PrefsHelper.PREFS_SYNCH_FOLDER);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onResume() {
		super.onResume();
		threads.setSummary(getText(R.string.prefs_threads_desc) + " " + threads.getSharedPreferences().getInt(PrefsHelper.PREFS_THREADS, 1));
		String folder = synchFolder.getSharedPreferences().getString(PrefsHelper.PREFS_SYNCH_FOLDER, null);
		if (folder == null) {
			synchFolder.setSummary(R.string.prefs_synch_folder_desc);
		} else {
			synchFolder.setSummary(folder);
		}
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(PrefsHelper.PREFS_THREADS)) {
			threads.setSummary(getText(R.string.prefs_threads_desc) + " " + sharedPreferences.getInt(key, 1));
		}
		if (key.equals(PrefsHelper.PREFS_SYNCH_FOLDER)) {
			String folder = sharedPreferences.getString(PrefsHelper.PREFS_SYNCH_FOLDER, null);
			if (folder == null) {
				synchFolder.setSummary(R.string.prefs_synch_folder_desc);
			} else {
				synchFolder.setSummary(folder);
			}
		}
	}

}

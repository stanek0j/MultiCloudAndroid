package cz.zcu.kiv.multicloudandroid;

import android.app.FragmentTransaction;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import cz.zcu.kiv.multicloudandroid.fragment.PrefsFragment;

/**
 * cz.zcu.kiv.multicloudandroid/PrefsActivity.java			<br /><br />
 *
 * Activity for displaying application preferences.
 *
 * @author Jaromír Staněk
 * @version 1.0
 *
 */
public class PrefsActivity extends PreferenceActivity {

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		FragmentTransaction transaction = getFragmentManager().beginTransaction();
		transaction.replace(android.R.id.content, new PrefsFragment());
		transaction.commit();
	}

}

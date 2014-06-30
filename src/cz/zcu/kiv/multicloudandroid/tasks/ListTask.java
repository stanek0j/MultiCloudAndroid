package cz.zcu.kiv.multicloudandroid.tasks;

import cz.zcu.kiv.multicloud.MultiCloudException;
import cz.zcu.kiv.multicloud.json.FileInfo;
import cz.zcu.kiv.multicloud.oauth2.OAuth2SettingsException;
import cz.zcu.kiv.multicloudandroid.MainActivity;
import cz.zcu.kiv.multicloudandroid.PrefsHelper;
import cz.zcu.kiv.multicloudandroid.R;

/**
 * cz.zcu.kiv.multicloudandroid.tasks/ListTask.java			<br /><br />
 *
 * Task for getting the contents of a folder.
 *
 * @author Jaromír Staněk
 * @version 1.0
 *
 */
public class ListTask extends MultiCloudTask {

	/** Folder content. */
	private FileInfo folder;

	/**
	 * Ctor with necessary parameters.
	 * @param activity Activity.
	 */
	public ListTask(MainActivity activity) {
		super(activity, R.string.wait_list);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void doInBackgroundExtended() {
		try {
			PrefsHelper prefs = activity.getPrefsHelper();
			folder = cloud.listFolder(account.getName(), activity.getCurrentFolder(), prefs.isShowDeleted(), prefs.isShowShared());
		} catch (MultiCloudException | OAuth2SettingsException | InterruptedException e) {
			error = e.getMessage();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onPostExecuteExtended() {
		if (folder != null) {
			cache.provideChecksum(account.getName(), folder);
			cache.provideChecksum(account.getName(), folder.getContent());
			activity.actionListItem(folder);
		}
	}

}

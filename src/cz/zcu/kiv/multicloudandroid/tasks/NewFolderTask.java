package cz.zcu.kiv.multicloudandroid.tasks;

import cz.zcu.kiv.multicloud.MultiCloudException;
import cz.zcu.kiv.multicloud.json.FileInfo;
import cz.zcu.kiv.multicloud.oauth2.OAuth2SettingsException;
import cz.zcu.kiv.multicloudandroid.MainActivity;
import cz.zcu.kiv.multicloudandroid.PrefsHelper;
import cz.zcu.kiv.multicloudandroid.R;

/**
 * cz.zcu.kiv.multicloudandroid.tasks/NewFolderTask.java			<br /><br />
 *
 * Task for creating new folder.
 *
 * @author Jaromír Staněk
 * @version 1.0
 *
 */
public class NewFolderTask extends MultiCloudTask {

	/** New folder name. */
	private final String name;
	/** Folder to be listed. */
	private FileInfo folder;

	/**
	 * Ctor with necessary parameters.
	 * @param activity Activity.
	 * @param name New folder name.
	 */
	public NewFolderTask(MainActivity activity, String name) {
		super(activity, activity.getLibrary(), activity.getCurrentAccount(), R.string.wait_new_folder);
		this.name = name;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void doInBackgroundExtended() {
		try {
			cloud.createFolder(account.getName(), name, activity.getCurrentFolder());
			PrefsHelper prefs = activity.getPrefsHelper();
			folder = cloud.listFolder(account.getName(), activity.getCurrentFolder(), prefs.isShowHidden(), prefs.isShowShared());
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
			activity.actionListItem(folder);
		}
	}

}

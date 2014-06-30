package cz.zcu.kiv.multicloudandroid.tasks;

import cz.zcu.kiv.multicloud.MultiCloudException;
import cz.zcu.kiv.multicloud.json.FileInfo;
import cz.zcu.kiv.multicloud.oauth2.OAuth2SettingsException;
import cz.zcu.kiv.multicloudandroid.MainActivity;
import cz.zcu.kiv.multicloudandroid.PrefsHelper;
import cz.zcu.kiv.multicloudandroid.R;

/**
 * cz.zcu.kiv.multicloudandroid.tasks/DeleteTask.java			<br /><br />
 *
 * Task for deleting a file or folder.
 *
 * @author Jaromír Staněk
 * @version 1.0
 *
 */
public class DeleteTask extends MultiCloudTask {

	/** File or folder to be deleted. */
	private final FileInfo file;
	/** Folder to be listed. */
	private FileInfo folder;

	/**
	 * Ctor with necessary parameters.
	 * @param activity Activity.
	 * @param file File or folder to be deleted.
	 */
	public DeleteTask(MainActivity activity, FileInfo file) {
		super(activity, R.string.wait_delete);
		this.file = file;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void doInBackgroundExtended() {
		try {
			cloud.delete(account.getName(), file);
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
			cache.provideChecksum(account.getName(), folder);
			cache.provideChecksum(account.getName(), folder.getContent());
			activity.actionListItem(folder);
		}
	}

}

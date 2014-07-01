package cz.zcu.kiv.multicloudandroid.tasks;

import cz.zcu.kiv.multicloud.MultiCloudException;
import cz.zcu.kiv.multicloud.json.FileInfo;
import cz.zcu.kiv.multicloud.oauth2.OAuth2SettingsException;
import cz.zcu.kiv.multicloudandroid.MainActivity;
import cz.zcu.kiv.multicloudandroid.PrefsHelper;
import cz.zcu.kiv.multicloudandroid.R;

/**
 * cz.zcu.kiv.multicloudandroid.tasks/RenameTask.java			<br /><br />
 *
 * Task for renaming a file or folder.
 *
 * @author Jaromír Staněk
 * @version 1.0
 *
 */
public class RenameTask extends MultiCloudTask {

	/** New file or folder name. */
	private final String name;
	/** File or folder to be renamed. */
	private final FileInfo file;
	/** Folder to be listed. */
	private FileInfo folder;

	/**
	 * Ctor with necessary parameters.
	 * @param activity Activity.
	 * @param file File or folder to be renamed.
	 * @param name New file or folder name.
	 */
	public RenameTask(MainActivity activity, FileInfo file, String name) {
		super(activity, R.string.wait_rename);
		this.file = file;
		this.name = name;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void doInBackgroundExtended() {
		try {
			readRemoteCache();
			FileInfo renamed = cloud.rename(account.getName(), file, name);
			renamed.setChecksum(file.getChecksum());
			cache.update(account.getName(), file, renamed);
			PrefsHelper prefs = activity.getPrefsHelper();
			folder = cloud.listFolder(account.getName(), activity.getCurrentFolder(), prefs.isShowDeleted(), prefs.isShowShared());
			writeRemoteCache();
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

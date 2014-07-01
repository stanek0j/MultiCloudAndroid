package cz.zcu.kiv.multicloudandroid.tasks;

import java.io.File;

import cz.zcu.kiv.multicloud.MultiCloudException;
import cz.zcu.kiv.multicloud.json.FileInfo;
import cz.zcu.kiv.multicloud.oauth2.OAuth2SettingsException;
import cz.zcu.kiv.multicloud.utils.Utils;
import cz.zcu.kiv.multicloud.utils.Utils.UnitsFormat;
import cz.zcu.kiv.multicloudandroid.DialogProgressListener;
import cz.zcu.kiv.multicloudandroid.MainActivity;
import cz.zcu.kiv.multicloudandroid.PrefsHelper;
import cz.zcu.kiv.multicloudandroid.R;
import cz.zcu.kiv.multicloudandroid.display.TaskDialog;

/**
 * cz.zcu.kiv.multicloudandroid.tasks/UploadTask.java			<br /><br />
 *
 * Task for uploading a file.
 *
 * @author Jaromír Staněk
 * @version 1.0
 *
 */
public class UploadTask extends MultiCloudTask {

	/** Local file to be uploaded. */
	private final File source;
	/** File to upload to. */
	private final FileInfo destination;
	/** Folder to upload to. */
	private final FileInfo destinationFolder;
	/** Destination folder. */
	private FileInfo folder;
	/** If existing file should be overwritten. */
	private final boolean overwrite;
	/** Checksum of the local file. */
	private String checksum;

	/**
	 * Ctor with necessary parameters.
	 * @param activity Activity.
	 * @param source Local file to be uploaded.
	 * @param destination File to upload to.
	 * @param destinationFolder Folder to upload to.
	 * @param overwrite If existing file should be overwritten.
	 */
	public UploadTask(MainActivity activity, File source, FileInfo destination, FileInfo destinationFolder, boolean overwrite) {
		super(activity, R.string.wait_upload, false);
		this.source = source;
		this.destination = destination;
		this.destinationFolder = destinationFolder;
		this.overwrite = overwrite;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void doInBackgroundExtended() {
		DialogProgressListener listener = new DialogProgressListener();
		TaskDialog dialog = getDialog();
		dialog.setProgress(0);
		dialog.setMax((int) DialogProgressListener.PROGRESS_SIZE);
		dialog.setProgressNumberFormat(activity.getText(R.string.desc_total_size) + " " + Utils.formatSize(source.length(), UnitsFormat.BINARY));
		listener.setDialog(dialog);
		cloud.setListener(listener);
		try {
			checksum = cache.computeChecksum(source);
			//readRemoteCache();
			if (destination == null) {
				cloud.uploadFile(account.getName(), destinationFolder, source.getName(), overwrite, source);
			} else {
				cloud.updateFile(account.getName(), destinationFolder, destination, source.getName(), source);
			}
			PrefsHelper prefs = activity.getPrefsHelper();
			folder = cloud.listFolder(account.getName(), activity.getCurrentFolder(), prefs.isShowDeleted(), prefs.isShowShared());
		} catch (MultiCloudException | OAuth2SettingsException | InterruptedException e) {
			error = e.getMessage();
		}
		cloud.setListener(null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onPostExecuteExtended() {
		if (folder != null) {
			for (FileInfo content: folder.getContent()) {
				if (!destinationFolder.getContent().contains(content)) {
					/* new file uploaded */
					if (content.getName().equals(source.getName()) && content.getSize() == source.length()) {
						content.setChecksum(checksum);
						cache.add(account.getName(), content);
						break;
					}
				} else {
					if (destination == null) {
						continue;
					}
					/* match ID if present */
					boolean condId = false;
					if ((content.getId() == null) && (destination.getId() == null)) {
						condId = true;
					} else if ((content.getId() != null) && (destination.getId() != null)) {
						condId = content.getId().equals(destination.getId());
					} else {
						continue;
					}
					/* match PATH if present */
					boolean condPath = false;
					if ((content.getPath() == null) && (destination.getPath() == null)) {
						condPath = true;
					} else if ((content.getPath() != null) && (destination.getPath() != null)) {
						condPath = content.getPath().equals(destination.getPath());
					} else {
						continue;
					}
					/* match NAME */
					boolean condName = (destination != null && content.getName().equals(destination.getName()));
					if (condId && condPath && condName) {
						content.setChecksum(checksum);
						cache.add(account.getName(), content);
						break;
					}
				}
			}
			cache.provideChecksum(account.getName(), folder);
			cache.provideChecksum(account.getName(), folder.getContent());
			//writeRemoteCache();
			activity.actionListItem(folder);
		}
	}

}

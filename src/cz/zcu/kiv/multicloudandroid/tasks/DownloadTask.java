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
 * cz.zcu.kiv.multicloudandroid.tasks/DownloadTask.java			<br /><br />
 *
 * Task for downloadinng files.
 *
 * @author Jaromír Staněk
 * @version 1.0
 *
 */
public class DownloadTask extends MultiCloudTask {

	/** File t be downladed. */
	private final FileInfo source;
	/** Local fole to download to. */
	private final File destination;
	/** If existing local file should be overwritten. */
	private final boolean overwrite;

	/**
	 * Ctor with necessary parameters.
	 * @param activity Activity.
	 */
	public DownloadTask(MainActivity activity, FileInfo source, File destination, boolean overwrite) {
		super(activity, R.string.wait_download, false);
		this.source = source;
		this.destination = destination;
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
		dialog.setProgressNumberFormat(activity.getText(R.string.desc_total_size) + " " + Utils.formatSize(source.getSize(), UnitsFormat.BINARY));
		listener.setDialog(dialog);
		cloud.setListener(listener);
		try {
			PrefsHelper prefs = activity.getPrefsHelper();
			if (prefs.getThreads() > 1) {
				for (int i = 0; i < prefs.getThreads(); i++) {
					cloud.addDownloadSource(account.getName(), source);
				}
				cloud.downloadMultiFile(destination, overwrite);
			} else {
				cloud.downloadFile(account.getName(), source, destination, overwrite);
			}
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
		if (error == null) {
			activity.showToast(R.string.desc_downloaded);
		} else {
			if (!overwrite) {
				destination.delete();
			}
		}
	}

}

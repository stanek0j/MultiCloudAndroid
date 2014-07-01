package cz.zcu.kiv.multicloudandroid.tasks;

import java.io.File;
import java.io.IOException;

import android.os.Environment;
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
 * cz.zcu.kiv.multicloudandroid.tasks/ChecksumTask.java			<br /><br />
 *
 * Task for computing checksum of remote file.
 *
 * @author Jaromír Staněk
 * @version 1.0
 *
 */
public class ChecksumTask extends MultiCloudTask {

	/** File to compute checksum for. */
	private final FileInfo source;
	/** Checksum of the local file. */
	private String checksum;

	/**
	 * Ctor with necessary parameters.
	 * @param activity Activity.
	 * @param source File to compute checksum for.
	 */
	public ChecksumTask(MainActivity activity, FileInfo source) {
		super(activity, R.string.wait_checksum, false);
		this.source = source;
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
		File tmp = null;
		try {
			if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
				tmp = File.createTempFile("multicloud", ".tmp", activity.getExternalCacheDir());
			} else {
				tmp = File.createTempFile("multicloud", ".tmp", activity.getCacheDir());
			}
		} catch (IOException e) {
			error = e.getMessage();
		}
		if (tmp != null) {
			try {
				readRemoteCache();
				cloud.setListener(listener);
				PrefsHelper prefs = activity.getPrefsHelper();
				if (prefs.getThreads() > 1) {
					for (int i = 0; i < prefs.getThreads(); i++) {
						cloud.addDownloadSource(account.getName(), source);
					}
					cloud.downloadMultiFile(tmp, true);
				} else {
					cloud.downloadFile(account.getName(), source, tmp, true);
				}
				dialog.setIndeterminate(true);
				checksum = cache.computeChecksum(tmp);
				source.setChecksum(checksum);
				cache.add(account.getName(), source);
				cloud.setListener(null);
				writeRemoteCache();
			} catch (MultiCloudException | OAuth2SettingsException | InterruptedException e) {
				error = e.getMessage();
			}
			tmp.delete();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onPostExecuteExtended() { }


}

package cz.zcu.kiv.multicloudandroid.tasks;

import java.io.File;
import java.io.IOException;

import android.os.AsyncTask;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.zcu.kiv.multicloud.MultiCloud;
import cz.zcu.kiv.multicloud.MultiCloudException;
import cz.zcu.kiv.multicloud.filesystem.ProgressListener;
import cz.zcu.kiv.multicloud.json.FileInfo;
import cz.zcu.kiv.multicloud.json.Json;
import cz.zcu.kiv.multicloud.oauth2.OAuth2SettingsException;
import cz.zcu.kiv.multicloudandroid.ChecksumCache;
import cz.zcu.kiv.multicloudandroid.ChecksumProvider;
import cz.zcu.kiv.multicloudandroid.MainActivity;
import cz.zcu.kiv.multicloudandroid.R;
import cz.zcu.kiv.multicloudandroid.display.Account;
import cz.zcu.kiv.multicloudandroid.display.TaskDialog;

/**
 * cz.zcu.kiv.multicloudandroid.tasks/MultiCloudTask.java			<br /><br />
 *
 * Abstract asynchronous task with supplied common parameters.
 *
 * @author Jaromír Staněk
 * @version 1.0
 *
 */
public abstract class MultiCloudTask extends AsyncTask<Void, Void, Void> {

	/** Activity. */
	protected final MainActivity activity;
	/** MultiCloud library. */
	protected final MultiCloud cloud;
	/** Account. */
	protected final Account account;
	/** Checksum provider. */
	protected final ChecksumProvider cache;
	/** Progress dialog showed. */
	private TaskDialog dialog;
	/** Identifier of the text shown in progress dialog. */
	private final int dialogText;
	/** Error message to be displayed. */
	protected String error;
	/** If the progress of the task could be determined. */
	private final boolean indeterminate;

	/**
	 * Ctor with necessary parameters.
	 * @param activity Activity.
	 * @param dialogText Text for the displayed dialog.
	 */
	public MultiCloudTask(MainActivity activity, int dialogText) {
		this(activity, dialogText, true);
	}

	/**
	 * Ctor with necessary parameters.
	 * @param activity Activity.
	 * @param dialogText Text for the displayed dialog.
	 * @param indeterminate If the progress of the task could be determined.
	 */
	public MultiCloudTask(MainActivity activity, int dialogText, boolean indeterminate) {
		this.activity = activity;
		this.cloud = activity.getLibrary();
		this.account = activity.getCurrentAccount();
		this.cache = activity.getCache();
		this.dialogText = dialogText;
		this.indeterminate = indeterminate;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Void doInBackground(Void... params) {
		doInBackgroundExtended();
		return null;
	}

	/**
	 * Execution of the task in the background.
	 */
	protected abstract void doInBackgroundExtended();

	/**
	 * Returns the progress dialog.
	 * @return Progress dialog.
	 */
	protected TaskDialog getDialog() {
		return dialog;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onPostExecute(Void result) {
		if (dialog.isShowing()) {
			dialog.dismiss();
		}
		onPostExecuteExtended();
		if (error != null) {
			activity.showError(R.string.err_generic, error);
		}
	}

	/**
	 * Method to be executed after the task finishes.
	 */
	protected abstract void onPostExecuteExtended();

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onPreExecute() {
		dialog = new TaskDialog(cloud, activity, dialogText, indeterminate);
		dialog.show();
	}

	/**
	 * Read remote checksum caches into local cache.
	 * @throws MultiCloudException If the operation failed.
	 * @throws OAuth2SettingsException If the authorization failed.
	 * @throws InterruptedException If the process was interrupted.
	 */
	protected void readRemoteCache() throws MultiCloudException, OAuth2SettingsException, InterruptedException {
		File tmpFile = null;
		ObjectMapper mapper = Json.getInstance().getMapper();
		try {
			tmpFile = File.createTempFile("multicloud", ".tmp", activity.getCacheDir());
		} catch (IOException e) {
			Log.e(MainActivity.MULTICLOUD_NAME, e.getMessage());
		}
		if (tmpFile != null) {
			cloud.setListener(new ProgressListener() {
				/**
				 * {@inheritDoc}
				 */
				@Override
				protected void onProgress() {
					/* do nothing */
				}
			});
			for (String accountName: cache.getRemoteAccounts()) {
				if (cache.getRemote(accountName) != null) {
					FileInfo metadata = cloud.metadata(accountName, cache.getRemote(accountName));
					if (cache.getRemoteDate(accountName) == null || (metadata != null && metadata.getModified().after(cache.getRemoteDate(accountName)))) {
						cloud.downloadFile(accountName, metadata, tmpFile, true);
						try {
							ChecksumCache remote = mapper.readValue(tmpFile, ChecksumCache.class);
							cache.merge(remote);
						} catch (IOException e) {
							Log.e(MainActivity.MULTICLOUD_NAME, e.getMessage());
						}
					}
					if (metadata == null) {
						cache.putRemote(accountName, cache.getRemoteRoot(accountName), null);
					}
				}
			}
			tmpFile.delete();
			cloud.setListener(null);
		}
	}


	/**
	 * Writes local checksum cache to remote destinations.
	 * @throws MultiCloudException If the operation failed.
	 * @throws OAuth2SettingsException If the authorization failed.
	 * @throws InterruptedException If the process was interrupted.
	 */
	protected void writeRemoteCache() throws MultiCloudException, OAuth2SettingsException, InterruptedException {
		cloud.setListener(new ProgressListener() {
			/**
			 * {@inheritDoc}
			 */
			@Override
			protected void onProgress() {
				/* do nothing */
			}
		});
		for (String accountName: cache.getRemoteAccounts()) {
			FileInfo remoteRoot = cache.getRemoteRoot(accountName);
			FileInfo remote = cache.getRemote(accountName);
			if (remoteRoot != null) {
				if (remote != null) {
					/* update existing metadata */
					cloud.updateFile(accountName, remoteRoot, remote, ChecksumProvider.CHECKSUM_FILE, cache.getChecksumFile());
					FileInfo metadata = cloud.metadata(accountName, remote);
					if (metadata != null) {
						cache.putRemote(accountName, remoteRoot, metadata);
					}
				} else {
					/* write new metadata */
					cloud.uploadFile(account.getName(), remoteRoot, ChecksumProvider.CHECKSUM_FILE, true, cache.getChecksumFile());
					FileInfo r = cloud.listFolder(account.getName(), remoteRoot);
					if (r != null) {
						for (FileInfo f: r.getContent()) {
							if (f.getName().equals(ChecksumProvider.CHECKSUM_FILE)) {
								cache.putRemote(account.getName(), remoteRoot, f);
								break;
							}
						}
					}
				}
			}
		}
		cloud.setListener(null);
	}

}

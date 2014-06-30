package cz.zcu.kiv.multicloudandroid.tasks;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import cz.zcu.kiv.multicloud.MultiCloud;
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
	private ProgressDialog dialog;
	/** Identifier of the text shown in progress dialog. */
	private final int dialogText;
	/** Error message to be displayed. */
	protected String error;

	/**
	 * Ctor with necessary parameters.
	 * @param activity Activity.
	 * @param dialogText Text for the displayed dialog.
	 */
	public MultiCloudTask(MainActivity activity, int dialogText) {
		this.activity = activity;
		this.cloud = activity.getLibrary();
		this.account = activity.getCurrentAccount();
		this.cache = activity.getCache();
		this.dialogText = dialogText;
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
		dialog = new TaskDialog(cloud, activity, dialogText);
		dialog.show();
	}

}

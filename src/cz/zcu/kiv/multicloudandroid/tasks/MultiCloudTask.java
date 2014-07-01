package cz.zcu.kiv.multicloudandroid.tasks;

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
public abstract class MultiCloudTask extends AsyncTask<Void, Long, Void> {

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

}

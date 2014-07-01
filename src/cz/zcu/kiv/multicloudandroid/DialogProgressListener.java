package cz.zcu.kiv.multicloudandroid;

import cz.zcu.kiv.multicloud.filesystem.ProgressListener;
import cz.zcu.kiv.multicloudandroid.display.TaskDialog;

/**
 * cz.zcu.kiv.multicloudandroid/DialogProgressListener.java			<br /><br />
 *
 * Progress listener for showing current operation progress in progress dialog.
 *
 * @author Jaromír Staněk
 * @version 1.0
 *
 */
public class DialogProgressListener extends ProgressListener {

	/** Size of the progress for scaling. */
	public static final double PROGRESS_SIZE = 10000.0d;

	/** Dialog displayed. */
	private TaskDialog dialog;
	/** If progress should be reported. */
	private boolean reporting;

	/**
	 * Empty ctor.
	 */
	public DialogProgressListener() {
		this(ProgressListener.DEFAULT_REFRESH_INTERVAL);
	}

	/**
	 * Ctor with refresh interval.
	 * @param refreshInterval Refresh interval.
	 */
	public DialogProgressListener(long refreshInterval) {
		super(refreshInterval);
		reporting = true;
	}

	/**
	 * Return the dialog.
	 * @return Progress dialog.
	 */
	public TaskDialog getDialog() {
		return dialog;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onProgress() {
		if (reporting) {
			int progress = (int) (PROGRESS_SIZE * ((double) getTransferred() / getDivisor()) / getTotalSize());
			dialog.setProgress(progress);
		}
	}

	/**
	 * Sets the progress dialog.
	 * @param dialog Progress dialog.
	 */
	public void setDialog(TaskDialog dialog) {
		this.dialog = dialog;
	}

	/**
	 * Sets if the progress should be reported.
	 * @param reporting If progress should be reported.
	 */
	public void setReporting(boolean reporting) {
		this.reporting = reporting;
	}

}

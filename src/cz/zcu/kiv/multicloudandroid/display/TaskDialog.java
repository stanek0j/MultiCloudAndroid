package cz.zcu.kiv.multicloudandroid.display;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import cz.zcu.kiv.multicloud.MultiCloud;
import cz.zcu.kiv.multicloudandroid.MainActivity;
import cz.zcu.kiv.multicloudandroid.R;

/**
 * cz.zcu.kiv.multicloudandroid.display/TaskDialog.java			<br /><br />
 *
 * Progress dialog with abort button for displaying operation progress.
 *
 * @author Jaromír Staněk
 * @version 1.0
 *
 */
public class TaskDialog extends ProgressDialog {

	/** MultiCloud library. */
	private final MultiCloud cloud;
	/** Context. */
	private final Context context;
	/** Message to be displayed. */
	private final int message;
	/** If indeterminate progress dialog should be created. */
	private final boolean indeterminate;
	/** If the dialog was aborted. */
	private boolean aborted;

	/**
	 * Ctor with necessary parameters.
	 * @param cloud MultiCloud library.
	 * @param context Context.
	 * @param message Message to be displayed.
	 */
	public TaskDialog(MultiCloud cloud, Context context, int message, boolean indeterminate) {
		super(context);
		this.cloud = cloud;
		this.context = context;
		this.message = message;
		this.indeterminate = indeterminate;
		this.aborted = false;
		prepareDialog();
	}

	/**
	 * Ctor with necessary parameters.
	 * @param cloud MultiCloud library.
	 * @param context Context.
	 * @param theme Theme.
	 * @param message Message to be displayed.
	 */
	public TaskDialog(MultiCloud cloud, Context context, int theme, int message, boolean indeterminate) {
		super(context, theme);
		this.cloud = cloud;
		this.context = context;
		this.message = message;
		this.indeterminate = indeterminate;
		this.aborted = false;
		prepareDialog();
	}

	/**
	 * Returns if the dialog was aborted.
	 * @return If the dialog was aborted.
	 */
	public boolean isAborted() {
		return aborted;
	}

	/**
	 * Method for putting the dialog together.
	 */
	private void prepareDialog() {
		setCancelable(false);
		setIndeterminate(indeterminate);
		if (indeterminate) {
			setProgressStyle(ProgressDialog.STYLE_SPINNER);
		} else {
			setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		}
		setMessage(context.getText(message));
		setButton(ProgressDialog.BUTTON_NEGATIVE, context.getText(R.string.button_abort), new DialogInterface.OnClickListener() {
			/**
			 * {@inheritDoc}
			 */
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Thread thread = new Thread() {
					/**
					 * {@inheritDoc}
					 */
					@Override
					public void run() {
						cloud.abortAuthorization();
						cloud.abortOperation();
					}
				};
				thread.start();
				try {
					thread.join();
				} catch (InterruptedException e) {
					Log.e(MainActivity.MULTICLOUD_NAME, e.getMessage());
				}
				aborted = true;
				cancel();
			}
		});
	}

}

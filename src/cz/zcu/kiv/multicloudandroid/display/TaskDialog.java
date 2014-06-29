package cz.zcu.kiv.multicloudandroid.display;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import cz.zcu.kiv.multicloud.MultiCloud;
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

	/**
	 * Ctor with necessary parameters.
	 * @param cloud MultiCloud library.
	 * @param context Context.
	 * @param message Message to be displayed.
	 */
	public TaskDialog(MultiCloud cloud, Context context, int message) {
		super(context);
		this.cloud = cloud;
		this.context = context;
		this.message = message;
		prepareDialog();
	}

	/**
	 * Ctor with necessary parameters.
	 * @param cloud MultiCloud library.
	 * @param context Context.
	 * @param theme Theme.
	 * @param message Message to be displayed.
	 */
	public TaskDialog(MultiCloud cloud, Context context, int theme, int message) {
		super(context, theme);
		this.cloud = cloud;
		this.context = context;
		this.message = message;
		prepareDialog();
	}

	/**
	 * Method for putting the dialog together.
	 */
	private void prepareDialog() {
		setCancelable(false);
		setIndeterminate(true);
		setMessage(context.getText(message));
		setButton(ProgressDialog.BUTTON_NEGATIVE, context.getText(R.string.button_abort), new DialogInterface.OnClickListener() {
			/**
			 * {@inheritDoc}
			 */
			@Override
			public void onClick(DialogInterface dialog, int which) {
				cloud.abortAuthorization();
				cloud.abortOperation();
				cancel();
			}
		});
	}

}

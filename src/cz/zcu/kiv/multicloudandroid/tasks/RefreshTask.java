package cz.zcu.kiv.multicloudandroid.tasks;

import cz.zcu.kiv.multicloudandroid.MainActivity;
import cz.zcu.kiv.multicloudandroid.R;

/**
 * cz.zcu.kiv.multicloudandroid.tasks/RefreshTask.java			<br /><br />
 *
 * Task for refreshing user accounts.
 *
 * @author Jaromír Staněk
 * @version 1.0
 *
 */
public class RefreshTask extends MultiCloudTask {

	/**
	 * Ctor with necessary parameters.
	 * @param activity Activity.
	 */
	public RefreshTask(MainActivity activity) {
		super(activity, R.string.wait_refresh);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void doInBackgroundExtended() {

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onPostExecuteExtended() { }

}

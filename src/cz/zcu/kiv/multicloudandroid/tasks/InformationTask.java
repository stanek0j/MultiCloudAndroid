package cz.zcu.kiv.multicloudandroid.tasks;

import cz.zcu.kiv.multicloud.MultiCloudException;
import cz.zcu.kiv.multicloud.json.AccountInfo;
import cz.zcu.kiv.multicloud.json.AccountQuota;
import cz.zcu.kiv.multicloud.oauth2.OAuth2SettingsException;
import cz.zcu.kiv.multicloudandroid.MainActivity;
import cz.zcu.kiv.multicloudandroid.R;

/**
 * cz.zcu.kiv.multicloudandroid.tasks/InformationTask.java			<br /><br />
 *
 * Task for getting information about user account.
 *
 * @author Jaromír Staněk
 * @version 1.0
 *
 */
public class InformationTask extends MultiCloudTask {

	/** Account information. */
	private AccountInfo info;
	/** Account quota. */
	private AccountQuota quota;

	/**
	 * Ctor with necessary parameters.
	 * @param activity Activity.
	 */
	public InformationTask(MainActivity activity) {
		super(activity, R.string.wait_info);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void doInBackgroundExtended() {
		try {
			info = cloud.accountInfo(account.getName());
			quota = cloud.accountQuota(account.getName());
			account.setTotalSpace(quota.getTotalBytes());
			account.setFreeSpace(quota.getFreeBytes());
			account.setUsedSpace(quota.getUsedBytes());
		} catch (MultiCloudException | OAuth2SettingsException | InterruptedException e) {
			error = e.getMessage();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onPostExecuteExtended() {
		activity.actionUpdateAccount(account);
		if (info != null && quota != null) {
			activity.actionInformationAccount(info, quota);
		}
	}

}

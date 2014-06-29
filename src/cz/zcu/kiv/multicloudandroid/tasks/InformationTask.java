package cz.zcu.kiv.multicloudandroid.tasks;

import android.util.Log;
import cz.zcu.kiv.multicloud.MultiCloud;
import cz.zcu.kiv.multicloud.MultiCloudException;
import cz.zcu.kiv.multicloud.json.AccountInfo;
import cz.zcu.kiv.multicloud.json.AccountQuota;
import cz.zcu.kiv.multicloud.oauth2.OAuth2SettingsException;
import cz.zcu.kiv.multicloudandroid.MainActivity;
import cz.zcu.kiv.multicloudandroid.R;
import cz.zcu.kiv.multicloudandroid.display.Account;

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
	 * @param cloud MultiCloud library.
	 * @param account Account to be authorized.
	 */
	public InformationTask(MainActivity activity, MultiCloud cloud, Account account) {
		super(activity, cloud, account, R.string.wait_info);
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
			Log.e(MainActivity.MULTICLOUD_NAME, e.getMessage());
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

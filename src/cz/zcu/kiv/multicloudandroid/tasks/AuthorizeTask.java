package cz.zcu.kiv.multicloudandroid.tasks;

import cz.zcu.kiv.multicloud.MultiCloud;
import cz.zcu.kiv.multicloud.MultiCloudException;
import cz.zcu.kiv.multicloud.json.AccountQuota;
import cz.zcu.kiv.multicloud.oauth2.OAuth2SettingsException;
import cz.zcu.kiv.multicloudandroid.BrowserCallback;
import cz.zcu.kiv.multicloudandroid.MainActivity;
import cz.zcu.kiv.multicloudandroid.R;
import cz.zcu.kiv.multicloudandroid.display.Account;

/**
 * cz.zcu.kiv.multicloudandroid.tasks/AuthorizeTask.java			<br /><br />
 *
 * Task for authorization of the user account.
 *
 * @author Jaromír Staněk
 * @version 1.0
 *
 */
public class AuthorizeTask extends MultiCloudTask {

	/**
	 * Ctor with necessary parameters.
	 * @param activity Activity.
	 * @param cloud MultiCloud library.
	 * @param account Account to be authorized.
	 */
	public AuthorizeTask(MainActivity activity, MultiCloud cloud, Account account) {
		super(activity, cloud, account, R.string.wait_authorize);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void doInBackgroundExtended() {
		try {
			cloud.authorizeAccount(account.getName(), new BrowserCallback(activity));
			account.setAuthorized(true);
			AccountQuota quota = cloud.accountQuota(account.getName());
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
	}

}

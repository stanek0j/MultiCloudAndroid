package cz.zcu.kiv.multicloudandroid.tasks;

import cz.zcu.kiv.multicloud.MultiCloud;
import cz.zcu.kiv.multicloudandroid.MainActivity;
import cz.zcu.kiv.multicloudandroid.R;
import cz.zcu.kiv.multicloudandroid.display.Account;

public class LoadTask extends MultiCloudTask {

	public LoadTask(MainActivity activity, MultiCloud cloud, Account account) {
		super(activity, cloud, account, R.string.wait_load);
	}

	@Override
	protected void doInBackgroundExtended() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onPostExecuteExtended() {
		// TODO Auto-generated method stub

	}

}

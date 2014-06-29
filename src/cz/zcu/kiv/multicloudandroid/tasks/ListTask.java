package cz.zcu.kiv.multicloudandroid.tasks;

import android.util.Log;
import cz.zcu.kiv.multicloud.MultiCloud;
import cz.zcu.kiv.multicloud.MultiCloudException;
import cz.zcu.kiv.multicloud.json.FileInfo;
import cz.zcu.kiv.multicloud.oauth2.OAuth2SettingsException;
import cz.zcu.kiv.multicloudandroid.MainActivity;
import cz.zcu.kiv.multicloudandroid.R;
import cz.zcu.kiv.multicloudandroid.display.Account;

public class ListTask extends MultiCloudTask {

	private FileInfo folder;

	public ListTask(MainActivity activity, MultiCloud cloud, Account account) {
		super(activity, cloud, account, R.string.wait_list);
	}

	@Override
	protected void doInBackgroundExtended() {
		try {
			folder = cloud.listFolder(account.getName(), null);
		} catch (MultiCloudException | OAuth2SettingsException | InterruptedException e) {
			Log.e(MainActivity.MULTICLOUD_NAME, e.getMessage());
		}
	}

	@Override
	protected void onPostExecuteExtended() {
		if (folder != null) {
			activity.actionListItem(folder);
		}
	}

}

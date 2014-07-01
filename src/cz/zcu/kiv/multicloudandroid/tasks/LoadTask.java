package cz.zcu.kiv.multicloudandroid.tasks;

import java.io.File;
import java.io.IOException;
import java.util.List;

import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.zcu.kiv.multicloud.MultiCloudException;
import cz.zcu.kiv.multicloud.filesystem.ProgressListener;
import cz.zcu.kiv.multicloud.json.AccountInfo;
import cz.zcu.kiv.multicloud.json.AccountQuota;
import cz.zcu.kiv.multicloud.json.FileInfo;
import cz.zcu.kiv.multicloud.json.Json;
import cz.zcu.kiv.multicloud.oauth2.OAuth2SettingsException;
import cz.zcu.kiv.multicloudandroid.ChecksumCache;
import cz.zcu.kiv.multicloudandroid.ChecksumProvider;
import cz.zcu.kiv.multicloudandroid.MainActivity;
import cz.zcu.kiv.multicloudandroid.R;
import cz.zcu.kiv.multicloudandroid.display.Account;

/**
 * cz.zcu.kiv.multicloudandroid.tasks/LoadTask.java			<br /><br />
 *
 * Task for loading account information and remote checksum caches.
 *
 * @author Jaromír Staněk
 * @version 1.0
 *
 */
public class LoadTask extends MultiCloudTask {

	/** List of accounts. */
	private final List<Account> accounts;

	/**
	 * Ctor with necessary parameters.
	 * @param activity Activity.
	 * @param accounts List of accounts.
	 */
	public LoadTask(MainActivity activity, List<Account> accounts) {
		super(activity, R.string.wait_load);
		this.accounts = accounts;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void doInBackgroundExtended() {
		ObjectMapper mapper = Json.getInstance().getMapper();
		File tmpFile = null;
		try {
			tmpFile = File.createTempFile("multicloud", ".tmp", activity.getCacheDir());
		} catch (IOException e) {
			Log.e(MainActivity.MULTICLOUD_NAME, e.getMessage());
		}
		boolean skip = false;
		cloud.setListener(new ProgressListener() {
			/**
			 * {@inheritDoc}
			 */
			@Override
			protected void onProgress() {
				/* do nothing */
			}
		});
		for (Account account: accounts) {
			if (skip) {
				break;
			}
			if (account.isAuthorized()) {
				try {
					AccountInfo info = cloud.accountInfo(account.getName());
					AccountQuota quota = cloud.accountQuota(account.getName());
					account.setTotalSpace(quota.getTotalBytes());
					account.setFreeSpace(quota.getFreeBytes());
					account.setUsedSpace(quota.getUsedBytes());
					cache.addAccount(account.getName(), info.getId());
					FileInfo root = cloud.listFolder(account.getName(), null);
					if (root != null) {
						for (FileInfo f: root.getContent()) {
							if (f.getName().equals(ChecksumProvider.CHECKSUM_FILE)) {
								if (cache.getRemoteDate(account.getName()) == null || f.getModified().after(cache.getRemoteDate(account.getName()))) {
									if (tmpFile != null) {
										cloud.downloadFile(account.getName(), f, tmpFile, true);
										try {
											ChecksumCache remote = mapper.readValue(tmpFile, ChecksumCache.class);
											cache.merge(remote);
										} catch (IOException e) {
											/* ignore file exceptions */
										}
									}
								}
								cache.putRemote(account.getName(), root, f);
								break;
							} else {
								cache.putRemote(account.getName(), root, null);
							}
						}
					}
				} catch (MultiCloudException | OAuth2SettingsException | InterruptedException e) {
					if (getDialog().isAborted()) {
						error = activity.getText(R.string.err_aborted).toString();
						skip = true;
					}
				}
			}
		}
		cloud.setListener(null);
		if (!skip) {
			try {
				writeRemoteCache();
			} catch (MultiCloudException | OAuth2SettingsException | InterruptedException e) {
				if (getDialog().isAborted()) {
					error = activity.getText(R.string.err_aborted).toString();
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onPostExecuteExtended() {
		for (Account account: accounts) {
			activity.actionUpdateAccount(account);
		}
	}

}

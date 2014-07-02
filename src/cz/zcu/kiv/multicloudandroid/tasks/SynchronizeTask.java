package cz.zcu.kiv.multicloudandroid.tasks;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.os.Environment;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.zcu.kiv.multicloud.MultiCloudException;
import cz.zcu.kiv.multicloud.filesystem.FileType;
import cz.zcu.kiv.multicloud.json.AccountInfo;
import cz.zcu.kiv.multicloud.json.AccountQuota;
import cz.zcu.kiv.multicloud.json.FileInfo;
import cz.zcu.kiv.multicloud.json.Json;
import cz.zcu.kiv.multicloud.oauth2.OAuth2SettingsException;
import cz.zcu.kiv.multicloud.utils.Utils;
import cz.zcu.kiv.multicloud.utils.Utils.UnitsFormat;
import cz.zcu.kiv.multicloudandroid.ChecksumCache;
import cz.zcu.kiv.multicloudandroid.ChecksumProvider;
import cz.zcu.kiv.multicloudandroid.DialogProgressListener;
import cz.zcu.kiv.multicloudandroid.MainActivity;
import cz.zcu.kiv.multicloudandroid.PrefsHelper;
import cz.zcu.kiv.multicloudandroid.R;
import cz.zcu.kiv.multicloudandroid.display.Account;
import cz.zcu.kiv.multicloudandroid.display.SyncData;
import cz.zcu.kiv.multicloudandroid.display.TaskDialog;

/**
 * cz.zcu.kiv.multicloudandroid.tasks/SynchronizeTask.java			<br /><br />
 *
 * Task for synchronizing local folder with remote folders.
 *
 * @author Jaromír Staněk
 * @version 1.0
 *
 */
public class SynchronizeTask extends MultiCloudTask {

	/** List of accounts. */
	private final List<Account> accounts;
	/** Synchronization folder. */
	private File syncFolder;
	/** Temporary file. */
	private File tmpFile;
	/** Remote synchronization folder. */
	private FileInfo remote;
	/** List of conflicted files. */
	private final List<String> report;

	/**
	 * Ctor with necessary parameters.
	 * @param activity Activity.
	 * @param accounts List of accounts.
	 */
	public SynchronizeTask(MainActivity activity, List<Account> accounts) {
		super(activity, R.string.wait_synchronize, false);
		this.accounts = accounts;
		this.report = new ArrayList<>();
	}

	/**
	 * Clears the synchronization data.
	 * @param node Synchronization data.
	 */
	private void clearLocalStructure(SyncData node) {
		if (node == null) {
			return;
		}
		node.setOrigChecksum(node.getChecksum());
		node.setChecksum(null);
		for (String key: node.getAccounts().keySet()) {
			node.getAccounts().put(key, null);
		}
		for (SyncData content: node.getNodes()) {
			clearLocalStructure(content);
		}
	}

	/**
	 * Creates all folders along a path supplied.
	 * @param account Account name.
	 * @param structure Path to be created.
	 * @param root Root folder to start creating folders in.
	 * @return The folder at the end of the path.
	 * @throws InterruptedException If the process was interrupted.
	 */
	private FileInfo createFolderStructure(String account, List<SyncData> structure, FileInfo root) throws InterruptedException {
		if (account == null || root == null) {
			return null;
		}
		if (structure.isEmpty()) {
			return root;
		}
		List<SyncData> s = new ArrayList<>();
		s.addAll(structure);
		FileInfo destination = null;
		FileInfo list = null;
		FileInfo folder = root;
		do {
			try {
				list = cloud.listFolder(account, folder);
				if (list != null) {
					SyncData find = s.get(0);
					boolean found = false;
					for (FileInfo f: list.getContent()) {
						if (find.getName().equals(f.getName()) && f.getFileType() == FileType.FOLDER) {
							s.remove(0);
							list = f;
							found = true;
							break;
						}
					}
					if (!found) {
						cloud.createFolder(account, find.getName(), folder);
						list = cloud.listFolder(account, folder);
						if (list != null) {
							for (FileInfo f: list.getContent()) {
								if (find.getName().equals(f.getName()) && f.getFileType() == FileType.FOLDER) {
									s.remove(0);
									list = f;
									break;
								}
							}
						}
					}
				} else {
					break;
				}
				folder = list;
			} catch (MultiCloudException | OAuth2SettingsException e) {
				break;
			}
		} while (!s.isEmpty());
		if (s.isEmpty()) {
			destination = folder;
		}
		return destination;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void doInBackgroundExtended() {
		ObjectMapper mapper = Json.getInstance().getMapper();
		PrefsHelper prefs = activity.getPrefsHelper();
		syncFolder = new File(prefs.getSynchronizationFolder());
		tmpFile = null;
		try {
			if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
				tmpFile = File.createTempFile("multicloud", ".tmp", activity.getExternalCacheDir());
			} else {
				tmpFile = File.createTempFile("multicloud", ".tmp", activity.getCacheDir());
			}
		} catch (IOException e) {
			error = e.getMessage();
		}
		DialogProgressListener listener = new DialogProgressListener();
		TaskDialog dialog = getDialog();
		listener.setDialog(dialog);
		if (tmpFile != null) {
			boolean failed = false;
			/* analyze local folder */
			dialog.setProgressNumberFormat(activity.getText(R.string.wait_synch_local).toString());
			dialog.setIndeterminate(true);
			SyncData data = activity.getPrefsHelper().getSyncData();
			clearLocalStructure(data);
			readLocalStructure(syncFolder, data);
			prefs.setSyncData(data);
			/* analyze remote folders */
			dialog.setProgressNumberFormat(activity.getText(R.string.wait_synch_remote).toString());
			dialog.setIndeterminate(true);
			Map<String, FileInfo> syncRoot = new HashMap<>();
			try {
				/* get remote accounts info */
				for (Account account: accounts) {
					AccountInfo info = cloud.accountInfo(account.getName());
					cache.addAccount(account.getName(), info.getId());
				}
				/* get remote sync roots and caches */
				readRemoteCache();
				cloud.setListener(listener);
				for (Account account: accounts) {
					FileInfo list = cloud.listFolder(account.getName(), null);
					if (list != null) {
						FileInfo remoteCache = null;
						/* find sync folder and cache */
						for (FileInfo f: list.getContent()) {
							if (f.getName().equals(MainActivity.SYNC_FOLDER)) {
								syncRoot.put(account.getName(), f);
							}
							if (f.getName().equals(ChecksumProvider.CHECKSUM_FILE)) {
								if (cache.getRemoteDate(account.getName()) == null || f.getModified().after(cache.getRemoteDate(account.getName()))) {
									cloud.downloadFile(account.getName(), f, tmpFile, true);
									try {
										ChecksumCache remote = mapper.readValue(tmpFile, ChecksumCache.class);
										cache.merge(remote);
									} catch (IOException e) {
										/* ignore file exceptions */
									}
								}
								remoteCache = f;
								cache.putRemote(account.getName(), list, f);
							}
						}
						if (remoteCache != null) {
							/* update existing checksum cache file */
							cloud.updateFile(account.getName(), list, remoteCache, ChecksumProvider.CHECKSUM_FILE, cache.getChecksumFile());
							FileInfo metadata = cloud.metadata(account.getName(), remoteCache);
							if (metadata != null) {
								cache.putRemote(account.getName(), list, metadata);
							}
						} else {
							/* upload new checksum cache file */
							cloud.uploadFile(account.getName(), list, ChecksumProvider.CHECKSUM_FILE, true, cache.getChecksumFile());
							FileInfo r = cloud.listFolder(account.getName(), list);
							if (r != null) {
								for (FileInfo f: r.getContent()) {
									if (f.getName().equals(ChecksumProvider.CHECKSUM_FILE)) {
										cache.putRemote(account.getName(), list, f);
										break;
									}
								}
							}
						}
					}
					/* create sync folder, if not found */
					if (syncRoot.get(account.getName()) == null) {
						cloud.createFolder(account.getName(), MainActivity.SYNC_FOLDER, list);
					}
					if (syncRoot.get(account.getName()) == null) {
						list = cloud.listFolder(account.getName(), null);
						if (list != null) {
							for (FileInfo f: list.getContent()) {
								if (f.getName().equals(MainActivity.SYNC_FOLDER)) {
									syncRoot.put(account.getName(), f);
									break;
								}
							}
						}
					}
				}
				/* traverse through remote folders */
				for (Account account: accounts) {
					remote = syncRoot.get(account.getName());
					readRemoteStructure(account.getName(), remote, data);
				}
				/* compute missing checksums */
				checksumStructure(data);
				writeRemoteCache();
				prefs.setSyncData(data);
			} catch (MultiCloudException | OAuth2SettingsException | InterruptedException e) {
				failed = true;
			}
			/* synchronize files */
			if (!failed) {
				try {
					dialog.setIndeterminate(false);
					cloud.setListener(listener);
					synchronizeStructure(data, new ArrayList<SyncData>(), syncRoot);
					dialog.setIndeterminate(true);
					for (Account account: accounts) {
						AccountQuota quota = cloud.accountQuota(account.getName());
						account.setTotalSpace(quota.getTotalBytes());
						account.setFreeSpace(quota.getFreeBytes());
						account.setUsedSpace(quota.getUsedBytes());
					}
					writeRemoteCache();
					prefs.setSyncData(data);
				} catch (MultiCloudException | OAuth2SettingsException | InterruptedException e) {
					error = e.getMessage();
				}
			}
			cloud.setListener(null);
			tmpFile.delete();
		}
	}

	/**
	 * Compute missing checksums.
	 * @param node Synchronization data.
	 * @throws MultiCloudException If the operation failed.
	 * @throws OAuth2SettingsException If the authorization failed.
	 * @throws InterruptedException If the process was interrupted.
	 */
	private void checksumStructure(SyncData node) throws MultiCloudException, OAuth2SettingsException, InterruptedException {
		if (node == null) {
			return;
		}
		for (Entry<String, FileInfo> remote: node.getAccounts().entrySet()) {
			FileInfo remoteFile = remote.getValue();
			if (remoteFile != null && remoteFile.getChecksum() == null) {
				try {
					for (int i = 0; i < activity.getPrefsHelper().getThreads(); i++) {
						cloud.addDownloadSource(remote.getKey(), remoteFile);
					}
					cloud.downloadMultiFile(tmpFile, true);
					String checksum = cache.computeChecksum(tmpFile);
					remoteFile.setChecksum(checksum);
					cache.add(remote.getKey(), remoteFile);
				} catch (MultiCloudException | OAuth2SettingsException e) {
					if (getDialog().isAborted()) {
						throw e;
					}
				}
			}
		}
		for (SyncData content: node.getNodes()) {
			checksumStructure(content);
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
		activity.actionSynchronize(report);
	}

	/**
	 * Reads local file structure and computes checksums for files.
	 * @param file Local file.
	 * @param node Synchronization data.
	 */
	private void readLocalStructure(File file, SyncData node) {
		if (file == null || node == null) {
			return;
		}
		if (file.getName().equals(node.getName()) || file.equals(syncFolder)) {
			node.setLocalFile(file);
			if (node.getNodes().isEmpty() && file.isFile()) {
				node.setChecksum(cache.computeChecksum(file));
			} else {
				for (SyncData content: node.getNodes()) {
					for (File inner: file.listFiles()) {
						if (inner.getName().equals(content.getName())) {
							readLocalStructure(inner, content);
						}
					}
				}
			}
		}
	}

	/**
	 * Read remote file structure.
	 * @param account Account name.
	 * @param file Remote file.
	 * @param node Synchronization data.
	 * @throws MultiCloudException If the operation failed.
	 * @throws OAuth2SettingsException If the authorization failed.
	 * @throws InterruptedException If the process was interrupted.
	 */
	private void readRemoteStructure(String account, FileInfo file, SyncData node) throws MultiCloudException, OAuth2SettingsException, InterruptedException {
		if (file == null || node == null) {
			return;
		}
		if (file.getName().equals(node.getName()) || file == remote) {
			if (node.getNodes().isEmpty() && file.getFileType() == FileType.FILE) {
				if (node.getAccounts().containsKey(account)) {
					node.getAccounts().put(account, file);
				}
			} else {
				FileInfo list = cloud.listFolder(account, file);
				cache.provideChecksum(account, list);
				cache.provideChecksum(account, list.getContent());
				for (SyncData content: node.getNodes()) {
					for (FileInfo inner: list.getContent()) {
						if (inner.getName().equals(content.getName())) {
							readRemoteStructure(account, inner, content);
						}
					}
				}
			}
		}
	}

	/**
	 * Recursive method for synchronizing content of remote folders.
	 * @param node Synchronization data.
	 * @param folderStructure Path to current location.
	 * @param syncRoot Root folder for synchronization.
	 * @throws MultiCloudException If the operation failed.
	 * @throws OAuth2SettingsException If the authorization failed.
	 * @throws InterruptedException If the process was interrupted.
	 */
	private void synchronizeStructure(SyncData node, List<SyncData> folderStructure, Map<String, FileInfo> syncRoot) throws MultiCloudException, OAuth2SettingsException, InterruptedException {
		boolean exists = true;
		if (node.getChecksum() == null) {
			/* folder or no local file */
			exists = false;
		}
		if (exists) {
			Map<String, List<Entry<String, FileInfo>>> conflicted = new HashMap<>();
			Map<String, FileInfo> downloadList = new HashMap<>();
			Map<String, FileInfo> uploadList = new HashMap<>();
			boolean skip = false;
			for (Entry<String, FileInfo> remote: node.getAccounts().entrySet()) {
				FileInfo remoteFile = remote.getValue();
				if (remoteFile == null) {
					/* no remote file */
					uploadList.put(remote.getKey(), remoteFile);
				} else {
					if (remoteFile.getChecksum() != null) {
						if (!remoteFile.getChecksum().equals(node.getChecksum())) {
							/* remote file not matching local */
							if (!remoteFile.getChecksum().equals(node.getOrigChecksum())) {
								/* conflicted file */
								if (conflicted.containsKey(remoteFile.getChecksum())) {
									conflicted.get(remoteFile.getChecksum()).add(remote);
								} else {
									List<Entry<String, FileInfo>> list = new ArrayList<>();
									list.add(remote);
									conflicted.put(remoteFile.getChecksum(), list);
								}
							} else {
								/* old version */
								uploadList.put(remote.getKey(), remoteFile);
							}
						}
					} else {
						/* no remote checksum - skip file */
					}
				}
			}
			/* resolve conflict */
			if (!conflicted.isEmpty()) {
				StringBuilder sbl = new StringBuilder();
				sbl.append("[Sync folder]/");
				for (SyncData folder: folderStructure) {
					sbl.append(folder.getName() + "/");
				}
				sbl.append(node.getName());
				report.add(sbl.toString());
				skip = true;
			}
			if (!skip) {
				try {
					/* download remote file */
					if (!downloadList.isEmpty()) {
						long size = 0;
						for (int i = 0; i < activity.getPrefsHelper().getThreads(); i++) {
							for (Entry<String, FileInfo> entry: downloadList.entrySet()) {
								size = entry.getValue().getSize();
								cloud.addDownloadSource(entry.getKey(), entry.getValue());
							}
						}
						getDialog().setProgress(0);
						getDialog().setMax((int) DialogProgressListener.PROGRESS_SIZE);
						getDialog().setProgressNumberFormat(activity.getText(R.string.desc_total_size) + " " + Utils.formatSize(size, UnitsFormat.BINARY));
						cloud.downloadMultiFile(node.getLocalFile(), true);
					}
					/* upload local file */
					Map<String, FileInfo> destinations = new HashMap<>();
					for (Entry<String, FileInfo> entry: uploadList.entrySet()) {
						FileInfo destination = createFolderStructure(entry.getKey(), folderStructure, syncRoot.get(entry.getKey()));
						destinations.put(entry.getKey(), destination);
						if (entry.getValue() != null) {
							cloud.addUpdateDestination(entry.getKey(), destination, entry.getValue(), node.getName());
						} else {
							cloud.addUploadDestination(entry.getKey(), destination, node.getName());
						}
					}
					if (!uploadList.isEmpty()) {
						getDialog().setProgress(0);
						getDialog().setMax((int) DialogProgressListener.PROGRESS_SIZE);
						getDialog().setProgressNumberFormat(activity.getText(R.string.desc_total_size) + " " + Utils.formatSize(node.getLocalFile().length(), UnitsFormat.BINARY));
						cloud.updateMultiFile(node.getLocalFile());
					}
					/* update cache */
					FileInfo list = null;
					for (Entry<String, FileInfo> entry: destinations.entrySet()) {
						if (entry.getValue() != null) {
							list = cloud.listFolder(entry.getKey(), entry.getValue());
							for (FileInfo content: list.getContent()) {
								if (!entry.getValue().getContent().contains(content)) {
									/* new file uploaded */
									if (content.getName().equals(node.getLocalFile().getName()) && content.getSize() == node.getLocalFile().length()) {
										content.setChecksum(node.getChecksum());
										cache.add(entry.getKey(), content);
										break;
									}
								} else {
									FileInfo dst = uploadList.get(entry.getKey());
									if (dst == null) {
										continue;
									}
									/* match ID if present */
									boolean condId = false;
									if ((content.getId() == null) && (dst.getId() == null)) {
										condId = true;
									} else if ((content.getId() != null) && (dst.getId() != null)) {
										condId = content.getId().equals(dst.getId());
									} else {
										continue;
									}
									/* match PATH if present */
									boolean condPath = false;
									if ((content.getPath() == null) && (dst.getPath() == null)) {
										condPath = true;
									} else if ((content.getPath() != null) && (dst.getPath() != null)) {
										condPath = content.getPath().equals(dst.getPath());
									} else {
										continue;
									}
									/* match NAME */
									boolean condName = (dst != null && content.getName().equals(dst.getName()));
									if (condId && condPath && condName) {
										content.setChecksum(node.getChecksum());
										cache.add(entry.getKey(), content);
										break;
									}
								}
							}
						}
					}
				} catch (MultiCloudException | OAuth2SettingsException e) {
					Log.e(MainActivity.MULTICLOUD_NAME, e.getMessage());
					if (getDialog().isAborted()) {
						throw e;
					}
				}
			}
		}
		List<SyncData> structure = new ArrayList<>();
		structure.addAll(folderStructure);
		if (!node.isRoot()) {
			structure.add(node);
		}
		for (SyncData content: node.getNodes()) {
			synchronizeStructure(content, structure, syncRoot);
		}
	}

}

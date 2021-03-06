package cz.zcu.kiv.multicloudandroid;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import cz.zcu.kiv.multicloud.MultiCloud;
import cz.zcu.kiv.multicloud.MultiCloudException;
import cz.zcu.kiv.multicloud.MultiCloudSettings;
import cz.zcu.kiv.multicloud.json.AccountInfo;
import cz.zcu.kiv.multicloud.json.AccountQuota;
import cz.zcu.kiv.multicloud.json.AccountSettings;
import cz.zcu.kiv.multicloud.json.FileInfo;
import cz.zcu.kiv.multicloud.utils.FileAccountManager;
import cz.zcu.kiv.multicloud.utils.FileCloudManager;
import cz.zcu.kiv.multicloud.utils.FileCredentialStore;
import cz.zcu.kiv.multicloud.utils.SecureFileCredentialStore;
import cz.zcu.kiv.multicloud.utils.Utils;
import cz.zcu.kiv.multicloudandroid.display.Account;
import cz.zcu.kiv.multicloudandroid.display.AccountAction;
import cz.zcu.kiv.multicloudandroid.display.AccountAdapter;
import cz.zcu.kiv.multicloudandroid.display.AccountSelectedHandler;
import cz.zcu.kiv.multicloudandroid.display.DialogCreator;
import cz.zcu.kiv.multicloudandroid.display.ItemAction;
import cz.zcu.kiv.multicloudandroid.display.ItemAdapter;
import cz.zcu.kiv.multicloudandroid.display.ItemSelectedHandler;
import cz.zcu.kiv.multicloudandroid.display.SyncData;
import cz.zcu.kiv.multicloudandroid.fragment.AccountFragment;
import cz.zcu.kiv.multicloudandroid.fragment.ItemFragment;
import cz.zcu.kiv.multicloudandroid.tasks.AuthorizeTask;
import cz.zcu.kiv.multicloudandroid.tasks.ChecksumTask;
import cz.zcu.kiv.multicloudandroid.tasks.InformationTask;
import cz.zcu.kiv.multicloudandroid.tasks.ListTask;
import cz.zcu.kiv.multicloudandroid.tasks.LoadTask;
import cz.zcu.kiv.multicloudandroid.tasks.SynchronizeTask;

/**
 * cz.zcu.kiv.multicloudandroid/MainActivity.java			<br /><br />
 *
 * Main activity of the Android MultiCloud application.
 *
 * @author Jaromír Staněk
 * @version 1.0
 *
 */
public class MainActivity extends FragmentActivity implements AccountSelectedHandler, ItemSelectedHandler {

	/** Tag for identifying account fragment. */
	public static final String ACCOUNT_FRAGMENT_TAG = "ACCOUNTS";
	/** Tag for identifying item fragment. */
	public static final String ITEM_FRAGMENT_TAG = "ITEMS";
	/** Tag for identifying preferences fragment. */
	public static final String PREFS_FRAGMENT_TAG = "PREFS";
	/** Name of the application for logging purposes. */
	public static final String MULTICLOUD_NAME = "MultiCloud";
	/** Format for displaying dates. */
	public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
	/** Root folder path. */
	public static final String ROOT_FOLDER = "/";
	/** Remote synchronization folder. */
	public static final String SYNC_FOLDER = "multicloud";

	/** Preferences helper. */
	private PrefsHelper prefs;
	/** Dialog creator. */
	private DialogCreator dialogs;
	/** MultiCloud library. */
	private MultiCloud cloud;
	/** Stack of folders for holding current path. */
	private final LinkedList<FileInfo> currentPath;
	/** Current account. */
	private Account currentAccount;
	/** Current folder. */
	private FileInfo currentFolder;
	/** Checksum cache for remote files. */
	private ChecksumProvider cache;

	/** If folder should be added to folder stack. */
	private boolean addToStack = false;
	/** If saved account should be loaded. */
	private boolean load = true;

	/**
	 * Empty ctor.
	 */
	public MainActivity() {
		currentPath = new LinkedList<>();
	}

	/**
	 * Callback for adding account.
	 * @param account Account to be added.
	 */
	public void actionAddAccount(Account account) {
		AccountFragment fragment = (AccountFragment) getSupportFragmentManager().findFragmentByTag(ACCOUNT_FRAGMENT_TAG);
		if (fragment != null) {
			fragment.accountAdd(account);
		}
	}

	/**
	 * Callback for starting web browser.
	 * @param requestUri Uri to be loaded.
	 */
	public void actionAuthorize(Uri requestUri) {
		Intent browserIntent = new Intent(Intent.ACTION_VIEW, requestUri);
		startActivity(browserIntent);
	}

	/**
	 * Callback for displaying account information.
	 * @param info Account information.
	 * @param quota Account quota.
	 */
	public void actionInformationAccount(AccountInfo info, AccountQuota quota) {
		dialogs.dialogAccountInfo(info, quota);
	}

	/**
	 * Callback for listing folder content.
	 * @param folder Folder content.
	 */
	public void actionListItem(FileInfo folder) {
		ItemFragment fragment = (ItemFragment) getSupportFragmentManager().findFragmentByTag(ITEM_FRAGMENT_TAG);
		if (fragment != null) {
			if (folder != null) {
				currentFolder = folder;
				if (addToStack) {
					currentPath.add(folder);
				}
				getActionBar().setDisplayHomeAsUpEnabled(currentPath.size() > 1);
				if (folder.getName() == null || currentFolder.getName().isEmpty()) {
					getActionBar().setTitle("[" + currentAccount.getName() + " - root]");
				} else {
					getActionBar().setTitle(folder.getName());
				}
				addToStack = false;
			}
			fragment.displayFolderContent(folder);
		}
	}

	/**
	 * Callback for removing account.
	 * @param account Account to be removed.
	 */
	public void actionRemoveAccount(Account account) {
		cache.removeAccount(account.getName());
		SyncData data = prefs.getSyncData();
		removeAccount(data, account.getName());
		prefs.setSyncData(data);
		AccountFragment fragment = (AccountFragment) getSupportFragmentManager().findFragmentByTag(ACCOUNT_FRAGMENT_TAG);
		if (fragment != null) {
			fragment.accountRemove(account);
		}
	}

	/**
	 * Callback for renaming account.
	 * @param account Account to be renamed.
	 * @param name New account name.
	 */
	public void actionRenameAccount(Account account, String name) {
		cache.renameAccount(account.getName(), name);
		SyncData data = prefs.getSyncData();
		renameAccount(data, account.getName(), name);
		prefs.setSyncData(data);
		AccountFragment fragment = (AccountFragment) getSupportFragmentManager().findFragmentByTag(ACCOUNT_FRAGMENT_TAG);
		if (fragment != null) {
			fragment.accountRename(account, name);
		}
	}

	/**
	 * Callback for displaying synchronization problems.
	 * @param report Synchronization report.
	 */
	public void actionSynchronize(List<String> report) {
		if (!report.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			sb.append(getText(R.string.desc_synch_failed).toString());
			sb.append("\n");
			for (String s: report) {
				sb.append("\n");
				sb.append(s);
			}
			AlertDialog dialog = new AlertDialog.Builder(this)
			.setTitle(R.string.action_synchronize)
			.setMessage(sb.toString())
			.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
				/**
				 * {@inheritDoc}
				 */
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			})
			.create();
			dialog.show();
		} else {
			showToast(R.string.desc_synch_done);
		}
	}

	/**
	 * Callback for updating account information.
	 * @param account Account.
	 */
	public void actionUpdateAccount(Account account) {
		AccountFragment fragment = (AccountFragment) getSupportFragmentManager().findFragmentByTag(ACCOUNT_FRAGMENT_TAG);
		if (fragment != null) {
			fragment.accountUpdate(account);
		}
	}

	/**
	 * Returns the checksum provider.
	 * @return Checksum provider.
	 */
	public synchronized ChecksumProvider getCache() {
		return cache;
	}

	/**
	 * Returns current account.
	 * @return Current account.
	 */
	public synchronized Account getCurrentAccount() {
		return currentAccount;
	}

	/**
	 * Returns current folder.
	 * @return Current folder.
	 */
	public synchronized FileInfo getCurrentFolder() {
		return currentFolder;
	}

	/**
	 * Returns the MultiCloud library.
	 * @return MultiCloud library.
	 */
	public synchronized MultiCloud getLibrary() {
		return cloud;
	}

	/**
	 * Returns the preference helper.
	 * @return Preference helper.
	 */
	public synchronized PrefsHelper getPrefsHelper() {
		return prefs;
	}

	/**
	 * Method for updating the list of accounts.
	 */
	public void loadAccounts() {
		ListFragment fragment = (ListFragment) getSupportFragmentManager().findFragmentByTag(ACCOUNT_FRAGMENT_TAG);
		if (fragment != null) {
			List<Account> accountList = new ArrayList<>();
			AccountAdapter accounts = (AccountAdapter) fragment.getListAdapter();
			accounts.clear();
			for (AccountSettings as: cloud.getSettings().getAccountManager().getAllAccountSettings()) {
				Account a = new Account();
				a.setAuthorized(as.isAuthorized());
				a.setCloud(as.getSettingsId());
				a.setName(as.getAccountId());
				accounts.add(a);
				accountList.add(a);
			}
			if (accounts.isEmpty()) {
				Account a = new Account();
				a.setAuthorized(false);
				a.setCloud(null);
				a.setName(getString(R.string.action_add_account));
				accounts.add(a);
			} else {
				LoadTask load = new LoadTask(this, accountList);
				load.execute();
			}
			accounts.notifyDataSetChanged();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onAccountSelected(int position, AccountAction action) {
		ListFragment fragment = (ListFragment) getSupportFragmentManager().findFragmentByTag(ACCOUNT_FRAGMENT_TAG);
		if (fragment != null) {
			AccountAdapter accounts = (AccountAdapter) fragment.getListAdapter();
			Account prevAccount = currentAccount;
			currentAccount = accounts.getItem(position);
			boolean persistAccount = false;
			switch (action) {
			case ADD:
				dialogs.dialogAccountName(null);
				break;
			case AUTHORIZE:
				if (!currentAccount.isAuthorized()) {
					AuthorizeTask authTask = new AuthorizeTask(this);
					authTask.execute();
				} else {
					showToast(R.string.err_authorized);
				}
				break;
			case INFORMATION:
				if (currentAccount.isAuthorized()) {
					InformationTask infoTask = new InformationTask(this);
					infoTask.execute();
				} else {
					showToast(R.string.err_not_authorized);
				}
				break;
			case LIST:
				final ActionBar actionBar = getActionBar();
				if (!currentAccount.equals(prevAccount)) {
					currentFolder = null;
					currentPath.clear();
					actionBar.setDisplayHomeAsUpEnabled(false);
					actionBar.setTitle("[" + currentAccount.getName() + " - root]");
					addToStack = true;
				} else {
					actionBar.setDisplayHomeAsUpEnabled(currentPath.size() > 1);
					if (currentFolder.getName() == null || currentFolder.getName().isEmpty()) {
						actionBar.setTitle("[" + currentAccount.getName() + " - root]");
					} else {
						actionBar.setTitle(currentFolder.getName());
					}
				}
				persistAccount = true;
				ItemFragment itemFragment = new ItemFragment();
				FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
				transaction.replace(R.id.container, itemFragment, ITEM_FRAGMENT_TAG);
				transaction.addToBackStack(ITEM_FRAGMENT_TAG);
				transaction.commit();
				ListTask listTask = new ListTask(this);
				listTask.execute();
				break;
			case REMOVE:
				try {
					cloud.deleteAccount(currentAccount.getName());
					actionRemoveAccount(currentAccount);
				} catch (MultiCloudException e) {
					Log.e(MULTICLOUD_NAME, e.getMessage());
				}
				break;
			case RENAME:
				dialogs.dialogAccountName(currentAccount);
				break;
			default:
				break;
			}
			if (!persistAccount) {
				currentAccount = prevAccount;
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		getActionBar().setDisplayHomeAsUpEnabled(false);
		getActionBar().setTitle(R.string.app_name);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		prefs = new PrefsHelper(this);
		dialogs = new DialogCreator(this);
		cache = new ChecksumProvider(new File(getFilesDir(), ChecksumProvider.CHECKSUM_FILE));

		if (cloud == null) {
			MultiCloudSettings settings = new MultiCloudSettings();
			FileAccountManager accountManager = FileAccountManager.getInstance();
			accountManager.setSettingsFile(new File(getFilesDir(), FileAccountManager.DEFAULT_FILE));
			try {
				accountManager.loadAccountSettings();
			} catch (IOException e) {
				Log.e(MULTICLOUD_NAME, e.getMessage());
			}
			settings.setAccountManager(accountManager);
			FileCloudManager cloudManager = FileCloudManager.getInstance();
			try {
				File tmp = File.createTempFile("multicloud", ".tmp");
				for (String f: getAssets().list(FileCloudManager.DEFAULT_FOLDER)) {
					InputStream is = getAssets().open(FileCloudManager.DEFAULT_FOLDER + File.separator + f);
					FileOutputStream fos = new FileOutputStream(tmp);
					byte[] buffer = new byte[4096];
					while (is.read(buffer) != -1) {
						fos.write(buffer);
					}
					is.close();
					fos.flush();
					fos.close();
					cloudManager.loadCloudSettings(tmp);
				}
				tmp.delete();
			} catch (IOException e) {
				Log.e(MULTICLOUD_NAME, e.getMessage());
			}
			settings.setCloudManager(cloudManager);
			settings.setCredentialStore(new SecureFileCredentialStore(new File(getFilesDir(), FileCredentialStore.DEFAULT_STORE_FILE)));
			cloud = new MultiCloud(settings);
			cloud.validateAccounts();
		}

		if (findViewById(R.id.container) != null) {
			if (savedInstanceState != null) {
				return;
			}
			AccountFragment accounts = new AccountFragment();
			accounts.setArguments(getIntent().getExtras());
			getSupportFragmentManager().beginTransaction().add(R.id.container, accounts, ACCOUNT_FRAGMENT_TAG).commit();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.options_menu, menu);
		MenuItem itemAddAccount = menu.findItem(R.id.item_add_account);
		MenuItem itemNewFolder = menu.findItem(R.id.item_new_folder);
		MenuItem itemUpload = menu.findItem(R.id.item_upload);
		if (findViewById(R.id.container) != null) {
			Fragment accounts = getSupportFragmentManager().findFragmentByTag(ACCOUNT_FRAGMENT_TAG);
			Fragment items = getSupportFragmentManager().findFragmentByTag(ITEM_FRAGMENT_TAG);
			if (accounts != null && accounts.isVisible()) {
				itemAddAccount.setVisible(true);
				itemNewFolder.setVisible(false);
				itemUpload.setVisible(false);
			}
			if (items != null && items.isVisible()) {
				itemAddAccount.setVisible(false);
				itemNewFolder.setVisible(true);
				itemUpload.setVisible(true);
			}
		}
		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onItemSelected(int position, ItemAction action) {
		ListFragment fragment = (ListFragment) getSupportFragmentManager().findFragmentByTag(ITEM_FRAGMENT_TAG);
		if (fragment != null) {
			ItemAdapter items = (ItemAdapter) fragment.getListAdapter();
			FileInfo item = items.getItem(position);
			switch (action) {
			case LIST:
				currentFolder = item;
				addToStack = true;
				ListTask listTask = new ListTask(this);
				listTask.execute();
				break;
			case DOWNLOAD:
				dialogs.dialogFileDownload(item);
				break;
			case RENAME:
				dialogs.dialogFileRename(item);
				break;
			case DELETE:
				dialogs.dialogFileDelete(item);
				break;
			case CHECKSUM:
				ChecksumTask checksum = new ChecksumTask(this, item);
				checksum.execute();
				break;
			case PROPERTIES:
				dialogs.dialogFileProperties(item);
				break;
			default:
				break;
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_add_account:
			dialogs.dialogAccountName(null);
			break;
		case R.id.item_new_folder:
			dialogs.dialogFileNewFolder();
			break;
		case R.id.item_upload:
			dialogs.dialogFileUpload(currentFolder);
			break;
		case R.id.item_refresh:
			if (findViewById(R.id.container) != null) {
				Fragment accounts = getSupportFragmentManager().findFragmentByTag(ACCOUNT_FRAGMENT_TAG);
				Fragment items = getSupportFragmentManager().findFragmentByTag(ITEM_FRAGMENT_TAG);
				if (accounts != null && accounts.isVisible()) {
					loadAccounts();
				}
				if (items != null && items.isVisible()) {
					ListTask list = new ListTask(this);
					list.execute();
				}
			}
			break;
		case R.id.item_synchronize:
			if (prefs.getSynchronizationFolder() == null) {
				showToast(R.string.desc_synch_select_folder);
				break;
			}
			List<Account> accountList = new ArrayList<>();
			ListFragment fragment = (ListFragment) getSupportFragmentManager().findFragmentByTag(ACCOUNT_FRAGMENT_TAG);
			if (fragment != null) {
				AccountAdapter accounts = (AccountAdapter) fragment.getListAdapter();
				for (int i = 0; i < accounts.getCount(); i++) {
					accountList.add(accounts.getItem(i));
				}
			}
			SynchronizeTask synchronize = new SynchronizeTask(this, accountList);
			synchronize.execute();
			break;
		case R.id.item_preferences:
			Intent intent = new Intent(this, PrefsActivity.class);
			startActivity(intent);
			break;
		case android.R.id.home:
			ItemFragment itemFragment = (ItemFragment) getSupportFragmentManager().findFragmentByTag(ITEM_FRAGMENT_TAG);
			if (itemFragment != null) {
				currentPath.removeLast();
				currentFolder = currentPath.peekLast();
				ListTask listTask = new ListTask(this);
				listTask.execute();
			}
			break;
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onRestart() {
		super.onRestart();
		load = false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onResume() {
		super.onResume();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onStart() {
		super.onStart();
		if (load) {
			loadAccounts();
		}
		load = true;
	}

	/**
	 * Removes account from synchronization data.
	 * @param node Root of the tree structure.
	 * @param oldName Account to be removed.
	 */
	private void removeAccount(SyncData node, String oldName) {
		if (node == null || Utils.isNullOrEmpty(oldName)) {
			return;
		}
		if (node.getAccounts().containsKey(oldName)) {
			node.getAccounts().remove(oldName);
		}
		for (SyncData inner: node.getNodes()) {
			removeAccount(inner, oldName);
		}
	}

	/**
	 * Renames account in synchronization data.
	 * @param node Root of the tree structure.
	 * @param oldName Old name to rename from.
	 * @param newName New name to rename to.
	 */
	private void renameAccount(SyncData node, String oldName, String newName) {
		if (node == null || Utils.isNullOrEmpty(oldName) || Utils.isNullOrEmpty(newName)) {
			return;
		}
		if (node.getAccounts().containsKey(oldName)) {
			FileInfo value = node.getAccounts().get(oldName);
			node.getAccounts().remove(oldName);
			node.getAccounts().put(newName, value);
		}
		for (SyncData inner: node.getNodes()) {
			renameAccount(inner, oldName, newName);
		}
	}

	/**
	 * Method for displaying error message in a dialog.
	 * @param title Dialog title.
	 * @param message Error message.
	 */
	public void showError(int title, String message) {
		Log.e(MULTICLOUD_NAME, message);
		if (prefs.isShowErr()) {
			AlertDialog dialog = new AlertDialog.Builder(this)
			.setTitle(title)
			.setMessage(message)
			.setPositiveButton(R.string.button_ok, null)
			.create();
			dialog.show();
		} else {
			Toast.makeText(this, message, Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Method for notifying the user via toast.
	 * @param text Message to be displayed.
	 */
	public void showToast(int text) {
		Toast.makeText(this, text, Toast.LENGTH_LONG).show();
	}

}

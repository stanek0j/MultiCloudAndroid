package cz.zcu.kiv.multicloudandroid;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Intent;
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
import cz.zcu.kiv.multicloud.utils.SecureFileCredentialStore;
import cz.zcu.kiv.multicloudandroid.display.Account;
import cz.zcu.kiv.multicloudandroid.display.AccountAction;
import cz.zcu.kiv.multicloudandroid.display.AccountAdapter;
import cz.zcu.kiv.multicloudandroid.display.AccountSelectedHandler;
import cz.zcu.kiv.multicloudandroid.display.DialogCreator;
import cz.zcu.kiv.multicloudandroid.display.ItemAction;
import cz.zcu.kiv.multicloudandroid.display.ItemAdapter;
import cz.zcu.kiv.multicloudandroid.display.ItemSelectedHandler;
import cz.zcu.kiv.multicloudandroid.fragment.AccountFragment;
import cz.zcu.kiv.multicloudandroid.fragment.ItemFragment;
import cz.zcu.kiv.multicloudandroid.tasks.AuthorizeTask;
import cz.zcu.kiv.multicloudandroid.tasks.InformationTask;
import cz.zcu.kiv.multicloudandroid.tasks.ListTask;
import cz.zcu.kiv.multicloudandroid.tasks.RefreshTask;

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

	public static final String ACCOUNT_FRAGMENT_TAG = "ACCOUNTS";
	public static final String ITEM_FRAGMENT_TAG = "ITEMS";
	public static final String PREFS_FRAGMENT_TAG = "PREFS";
	public static final String KEY_ACCOUNT = "account";
	public static final String MULTICLOUD_NAME = "MultiCLoud";
	public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

	/** Preferences helper. */
	private PrefsHelper prefs;
	/** Dialog creator. */
	private DialogCreator dialogs;
	/** MultiCloud library. */
	private MultiCloud cloud;
	/** Current path. */
	private final LinkedList<FileInfo> currentPath;
	/** Current account. */
	private Account currentAccount;
	/** Current folder. */
	private FileInfo currentFolder;
	/** Checksum cache for remote files. */
	private final ChecksumProvider cache;

	private boolean addToStack = false;
	private boolean load = true;

	public MainActivity() {
		currentPath = new LinkedList<>();
		cache = new ChecksumProvider();
	}

	public void actionAddAccount(Account account) {
		AccountFragment fragment = (AccountFragment) getSupportFragmentManager().findFragmentByTag(ACCOUNT_FRAGMENT_TAG);
		if (fragment != null) {
			fragment.accountAdd(account);
		}
	}

	public void actionInformationAccount(AccountInfo info, AccountQuota quota) {
		dialogs.dialogAccountInfo(info, quota);
	}

	public void actionListItem(FileInfo folder) {
		ItemFragment fragment = (ItemFragment) getSupportFragmentManager().findFragmentByTag(ITEM_FRAGMENT_TAG);
		if (fragment != null) {
			if (folder != null) {
				currentFolder = folder;
				if (addToStack) {
					currentPath.add(folder);
				}
				for (FileInfo f: currentPath) {
					Log.wtf("test", "path: " + f.getName());
				}
				getActionBar().setDisplayHomeAsUpEnabled(currentPath.size() > 1);
				if (folder.getName() == null) {
					getActionBar().setTitle("[" + currentAccount.getName() + " - root]");
				} else {
					getActionBar().setTitle(folder.getName());
				}
				addToStack = false;
			}
			fragment.displayFolderContent(folder);
		}
	}

	public void actionRemoveAccount(Account account) {
		AccountFragment fragment = (AccountFragment) getSupportFragmentManager().findFragmentByTag(ACCOUNT_FRAGMENT_TAG);
		if (fragment != null) {
			fragment.accountRemove(account);
		}
	}

	public void actionRenameAccount(Account account, String name) {
		AccountFragment fragment = (AccountFragment) getSupportFragmentManager().findFragmentByTag(ACCOUNT_FRAGMENT_TAG);
		if (fragment != null) {
			fragment.accountRename(account, name);
		}
	}

	public void actionUpdateAccount(Account account) {
		AccountFragment fragment = (AccountFragment) getSupportFragmentManager().findFragmentByTag(ACCOUNT_FRAGMENT_TAG);
		if (fragment != null) {
			fragment.accountUpdate(account);
		}
	}

	public synchronized ChecksumProvider getCache() {
		return cache;
	}

	public synchronized Account getCurrentAccount() {
		return currentAccount;
	}

	public synchronized FileInfo getCurrentFolder() {
		return currentFolder;
	}

	public synchronized MultiCloud getLibrary() {
		return cloud;
	}

	public synchronized PrefsHelper getPrefsHelper() {
		return prefs;
	}

	/**
	 * Method for updating the list of accounts.
	 */
	public void loadAccounts() {
		ListFragment fragment = (ListFragment) getSupportFragmentManager().findFragmentByTag(ACCOUNT_FRAGMENT_TAG);
		if (fragment != null) {
			AccountAdapter accounts = (AccountAdapter) fragment.getListAdapter();
			accounts.clear();
			for (AccountSettings as: cloud.getSettings().getAccountManager().getAllAccountSettings()) {
				Account a = new Account();
				a.setAuthorized(as.isAuthorized());
				a.setCloud(as.getSettingsId());
				a.setName(as.getAccountId());
				accounts.add(a);
			}
			if (accounts.isEmpty()) {
				Account a = new Account();
				a.setAuthorized(false);
				a.setCloud(null);
				a.setName(getString(R.string.action_add_account));
				accounts.add(a);
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
			Account account = accounts.getItem(position);
			switch (action) {
			case ADD:
				dialogs.dialogAccountName(null);
				break;
			case AUTHORIZE:
				if (!account.isAuthorized()) {
					AuthorizeTask authTask = new AuthorizeTask(this);
					authTask.execute();
				} else {
					showToast(R.string.err_authorized);
				}
				break;
			case INFORMATION:
				if (account.isAuthorized()) {
					InformationTask infoTask = new InformationTask(this);
					infoTask.execute();
				} else {
					showToast(R.string.err_not_authorized);
				}
				break;
			case LIST:
				final ActionBar actionBar = getActionBar();
				if (!account.equals(currentAccount)) {
					currentAccount = account;
					currentFolder = null;
					currentPath.clear();
					actionBar.setDisplayHomeAsUpEnabled(false);
					actionBar.setTitle("[" + account.getName() + " - root]");
					addToStack = true;
				} else {
					actionBar.setDisplayHomeAsUpEnabled(currentPath.size() > 1);
					actionBar.setTitle(currentFolder.getName());
				}
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
					cloud.deleteAccount(account.getName());
					actionRemoveAccount(account);
				} catch (MultiCloudException e) {
					Log.e(MULTICLOUD_NAME, e.getMessage());
				}
				break;
			case RENAME:
				dialogs.dialogAccountName(account);
				break;
			default:
				break;
			}
		}
	}

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

		if (cloud == null) {
			MultiCloudSettings settings = new MultiCloudSettings();
			FileAccountManager accountManager = FileAccountManager.getInstance();
			accountManager.setSettingsFile(new File(getFilesDir(), FileAccountManager.DEFAULT_FILE));
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
			settings.setCredentialStore(new SecureFileCredentialStore(new File(getFilesDir(), SecureFileCredentialStore.DEFAULT_STORE_FILE)));
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
				Log.wtf("test", "showing accounts");
			}
			if (items != null && items.isVisible()) {
				itemAddAccount.setVisible(false);
				itemNewFolder.setVisible(true);
				itemUpload.setVisible(true);
				Log.wtf("test", "showing items");
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
				break;
			case RENAME:
				dialogs.dialogFileRename(item);
				break;
			case DELETE:
				dialogs.dialogFileDelete(item);
				break;
			case CHECKSUM:
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
			Toast.makeText(this, "upload", Toast.LENGTH_SHORT).show();
			break;
		case R.id.item_refresh:
			if (findViewById(R.id.container) != null) {
				Fragment accounts = getSupportFragmentManager().findFragmentByTag(ACCOUNT_FRAGMENT_TAG);
				Fragment items = getSupportFragmentManager().findFragmentByTag(ITEM_FRAGMENT_TAG);
				if (accounts != null && accounts.isVisible()) {
					RefreshTask refreshTask = new RefreshTask(this);
					refreshTask.execute();
				}
				if (items != null && items.isVisible()) {
					ListTask listTask = new ListTask(this);
					listTask.execute();
				}
			}
			break;
		case R.id.item_synchronize:
			Toast.makeText(this, "synchronize", Toast.LENGTH_SHORT).show();
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

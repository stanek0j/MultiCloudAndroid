package cz.zcu.kiv.multicloudandroid;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import cz.zcu.kiv.multicloud.MultiCloud;
import cz.zcu.kiv.multicloud.MultiCloudException;
import cz.zcu.kiv.multicloud.MultiCloudSettings;
import cz.zcu.kiv.multicloud.json.AccountInfo;
import cz.zcu.kiv.multicloud.json.AccountQuota;
import cz.zcu.kiv.multicloud.json.AccountSettings;
import cz.zcu.kiv.multicloud.json.CloudSettings;
import cz.zcu.kiv.multicloud.json.FileInfo;
import cz.zcu.kiv.multicloud.utils.FileAccountManager;
import cz.zcu.kiv.multicloud.utils.FileCloudManager;
import cz.zcu.kiv.multicloud.utils.SecureFileCredentialStore;
import cz.zcu.kiv.multicloud.utils.Utils;
import cz.zcu.kiv.multicloud.utils.Utils.UnitsFormat;
import cz.zcu.kiv.multicloudandroid.display.Account;
import cz.zcu.kiv.multicloudandroid.display.AccountAction;
import cz.zcu.kiv.multicloudandroid.display.AccountAdapter;
import cz.zcu.kiv.multicloudandroid.display.AccountSelectedHandler;
import cz.zcu.kiv.multicloudandroid.display.ItemAction;
import cz.zcu.kiv.multicloudandroid.display.ItemAdapter;
import cz.zcu.kiv.multicloudandroid.display.ItemSelectedHandler;
import cz.zcu.kiv.multicloudandroid.fragment.AccountFragment;
import cz.zcu.kiv.multicloudandroid.fragment.ItemFragment;
import cz.zcu.kiv.multicloudandroid.tasks.AuthorizeTask;
import cz.zcu.kiv.multicloudandroid.tasks.InformationTask;
import cz.zcu.kiv.multicloudandroid.tasks.ListTask;

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
	public static final String KEY_ACCOUNT = "account";
	public static final String MULTICLOUD_NAME = "MultiCLoud";
	public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

	private MultiCloud cloud;
	private Account selectedAccount;

	private boolean load = true;

	public void actionAddAccount(Account account) {
		AccountFragment fragment = (AccountFragment) getSupportFragmentManager().findFragmentByTag(ACCOUNT_FRAGMENT_TAG);
		if (fragment != null) {
			fragment.accountAdd(account);
		}
	}

	public void actionInformationAccount(AccountInfo info, AccountQuota quota) {
		dialogAccountInfo(info, quota);
	}

	public void actionListItem(FileInfo folder) {
		ItemFragment fragment = (ItemFragment) getSupportFragmentManager().findFragmentByTag(ITEM_FRAGMENT_TAG);
		if (fragment != null) {
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

	private void dialogAccountInfo(final AccountInfo info, final AccountQuota quota) {
		final View view = getLayoutInflater().inflate(R.layout.account_info_dialog, null);
		AlertDialog dialog = new AlertDialog.Builder(this)
		.setTitle(R.string.action_info_account)
		.setView(view)
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
		TextView userName = (TextView) dialog.findViewById(R.id.textView_user_name);
		userName.setText(info.getName());
		TextView userId = (TextView) dialog.findViewById(R.id.textView_user_id);
		userId.setText(info.getId());
		TextView quotaTotal = (TextView) dialog.findViewById(R.id.textView_quota_total);
		quotaTotal.setText(Utils.formatSize(quota.getTotalBytes(), UnitsFormat.BINARY) + " (" + quota.getTotalBytes() + "B)");
		TextView quotaFree = (TextView) dialog.findViewById(R.id.textView_quota_free);
		quotaFree.setText(Utils.formatSize(quota.getFreeBytes(), UnitsFormat.BINARY) + " (" + quota.getFreeBytes() + "B)");
		TextView quotaUsed = (TextView) dialog.findViewById(R.id.textView_quota_used);
		quotaUsed.setText(Utils.formatSize(quota.getUsedBytes(), UnitsFormat.BINARY) + " (" + quota.getUsedBytes() + "B)");
	}

	private void dialogAccountName(final Account account) {
		final View view = getLayoutInflater().inflate(R.layout.account_name_dialog, null);
		AlertDialog dialog = new AlertDialog.Builder(this)
		.setTitle(R.string.action_add_account)
		.setView(view)
		.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
			/**
			 * {@inheritDoc}
			 */
			@Override
			public void onClick(DialogInterface dialog, int which) {
				EditText text = (EditText) view.findViewById(R.id.editText_name);
				Spinner clouds = (Spinner) view.findViewById(R.id.spinner_clouds);
				try {
					if (account == null) {
						Account a = new Account();
						a.setAuthorized(false);
						a.setCloud((String) clouds.getSelectedItem());
						a.setName(text.getText().toString());
						cloud.createAccount(a.getName(), a.getCloud());
						actionAddAccount(a);
					} else {
						cloud.renameAccount(account.getName(), text.getText().toString());
						actionRenameAccount(account, text.getText().toString());
					}
				} catch (MultiCloudException e) {
					showError(R.string.action_add_account, e.getMessage());
				}
			}
		})
		.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
			/**
			 * {@inheritDoc}
			 */
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		})
		.create();
		dialog.show();
		Spinner clouds = (Spinner) dialog.findViewById(R.id.spinner_clouds);
		if (clouds != null) {
			ArrayList<String> cloudTypes = new ArrayList<>();
			for (CloudSettings cs: cloud.getSettings().getCloudManager().getAllCloudSettings()) {
				cloudTypes.add(cs.getSettingsId());
			}
			ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, cloudTypes);
			clouds.setAdapter(adapter);
		}
		if (account != null) {
			clouds.setEnabled(false);
			EditText text = (EditText) dialog.findViewById(R.id.editText_name);
			text.setText(account.getName());
			text.setSelection(text.getText().length());
		}
	}

	public MultiCloud getLibrary() {
		return cloud;
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
				dialogAccountName(null);
				break;
			case AUTHORIZE:
				if (!account.isAuthorized()) {
					AuthorizeTask authTask = new AuthorizeTask(this, cloud, account);
					authTask.execute();
				} else {
					showToast(R.string.err_authorized);
				}
				break;
			case INFORMATION:
				if (account.isAuthorized()) {
					InformationTask infoTask = new InformationTask(this, cloud, account);
					infoTask.execute();
				} else {
					showToast(R.string.err_not_authorized);
				}
				break;
			case LIST:
				ItemFragment data = (ItemFragment) getSupportFragmentManager().findFragmentByTag(ITEM_FRAGMENT_TAG);
				selectedAccount = account;
				if (data != null) {

				} else {
					final ActionBar actionBar = getActionBar();
					actionBar.setDisplayHomeAsUpEnabled(true);
					actionBar.setTitle("[root]");
					ItemFragment itemFragment = new ItemFragment();
					FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
					transaction.replace(R.id.container, itemFragment, ITEM_FRAGMENT_TAG);
					transaction.addToBackStack(ITEM_FRAGMENT_TAG);
					transaction.commit();
					invalidateOptionsMenu();
				}
				ListTask listTask = new ListTask(this, cloud, selectedAccount);
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
				dialogAccountName(account);
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
			if (accounts == null || items == null) {
				if (accounts != null) {
					itemAddAccount.setVisible(true);
					itemNewFolder.setVisible(false);
					itemUpload.setVisible(false);
					Log.wtf("test", "showing accounts");
				}
				if (items != null) {
					itemAddAccount.setVisible(false);
					itemNewFolder.setVisible(true);
					itemUpload.setVisible(true);
					Log.wtf("test", "showing items");
				}
			} else {
				Log.wtf("test", "showing both");
			}
		} else {
			Log.wtf("test", "container not found");
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
				ListTask listTask = new ListTask(this, cloud, selectedAccount);
				listTask.execute();
				break;
			case DOWNLOAD:
				break;
			case RENAME:
				break;
			case DELETE:
				break;
			case CHECKSUM:
				break;
			case PROPERTIES:
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
			dialogAccountName(null);
			break;
		case R.id.item_synchronize:
			Toast.makeText(this, "synchronize", Toast.LENGTH_SHORT).show();
			break;
		case R.id.item_preferences:
			Toast.makeText(this, "preferences", Toast.LENGTH_SHORT).show();
			break;
		case android.R.id.home:
			Toast.makeText(this, "back", Toast.LENGTH_SHORT).show();
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
		AlertDialog dialog = new AlertDialog.Builder(this)
		.setTitle(title)
		.setMessage(message)
		.setPositiveButton(R.string.button_ok, null)
		.create();
		dialog.show();
	}

	public void showToast(int text) {
		Toast.makeText(this, text, Toast.LENGTH_LONG).show();
	}

}

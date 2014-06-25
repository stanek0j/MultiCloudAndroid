package cz.zcu.kiv.multicloudandroid;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
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
import android.widget.Toast;
import cz.zcu.kiv.multicloud.MultiCloud;
import cz.zcu.kiv.multicloud.MultiCloudException;
import cz.zcu.kiv.multicloud.MultiCloudSettings;
import cz.zcu.kiv.multicloud.json.AccountSettings;
import cz.zcu.kiv.multicloud.json.CloudSettings;
import cz.zcu.kiv.multicloud.utils.FileAccountManager;
import cz.zcu.kiv.multicloud.utils.FileCloudManager;
import cz.zcu.kiv.multicloud.utils.SecureFileCredentialStore;
import cz.zcu.kiv.multicloudandroid.display.Account;
import cz.zcu.kiv.multicloudandroid.display.AccountAdapter;
import cz.zcu.kiv.multicloudandroid.display.AccountSelectedHandler;
import cz.zcu.kiv.multicloudandroid.fragment.AccountFragment;
import cz.zcu.kiv.multicloudandroid.fragment.ItemFragment;

/**
 * cz.zcu.kiv.multicloudandroid/MainActivity.java			<br /><br />
 *
 * Main activity of the Android MultiCloud application.
 *
 * @author Jaromír Staněk
 * @version 1.0
 *
 */
public class MainActivity extends FragmentActivity implements AccountSelectedHandler {

	private MultiCloud cloud;

	public MultiCloud getLibrary() {
		return cloud;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onAccountSelected(int position) {
		ItemFragment data = (ItemFragment) getSupportFragmentManager().findFragmentByTag("ITEMS");
		if (data != null) {

		} else {
			ItemFragment itemFragment = new ItemFragment();
			Bundle args = new Bundle();
			itemFragment.setArguments(args);
			FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
			transaction.replace(R.id.container, itemFragment, "ITEMS");
			transaction.addToBackStack("ITEMS");
			transaction.commit();
		}
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
				Log.e("MultiCloud", e.getMessage());
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
			getSupportFragmentManager().beginTransaction().add(R.id.container, accounts, "ACCOUNTS").commit();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.options_menu, menu);
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_add_account:
			final View view = getLayoutInflater().inflate(R.layout.account_dialog, null);
			AlertDialog dialog = new AlertDialog.Builder(this)
			.setTitle(R.string.action_add_account_options)
			.setView(view)
			.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
				/**
				 * {@inheritDoc}
				 */
				@Override
				public void onClick(DialogInterface dialog, int which) {
					EditText account = (EditText) view.findViewById(R.id.editText_name);
					Spinner clouds = (Spinner) view.findViewById(R.id.spinner_clouds);
					try {
						cloud.createAccount(account.getText().toString(), (String) clouds.getSelectedItem());
						updateAccountFragment();
					} catch (MultiCloudException e) {
						showError(R.string.action_add_account_options, e.getMessage());
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
			break;
		case R.id.item_synchronize:
			Toast.makeText(this, "synchronize", Toast.LENGTH_SHORT).show();
			break;
		case R.id.item_preferences:
			Toast.makeText(this, "preferences", Toast.LENGTH_SHORT).show();
			break;
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onStart() {
		super.onStart();
		updateAccountFragment();
	}

	/**
	 * Method for displaying error message in a dialog.
	 * @param title Dialog title.
	 * @param message Error message.
	 */
	public void showError(int title, String message) {
		Log.e("MultiCloud", message);
		AlertDialog dialog = new AlertDialog.Builder(this)
		.setTitle(title)
		.setMessage(message)
		.setPositiveButton(R.string.button_ok, null)
		.create();
		dialog.show();
	}

	/**
	 * Method for updating the list of accounts.
	 */
	private void updateAccountFragment() {
		ListFragment fragment = (ListFragment) getSupportFragmentManager().findFragmentByTag("ACCOUNTS");
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
				a.setName(getString(R.string.action_add_account_options));
				accounts.add(a);
			}
		}
	}

}

package cz.zcu.kiv.multicloudandroid.fragment;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import cz.zcu.kiv.multicloudandroid.MainActivity;
import cz.zcu.kiv.multicloudandroid.R;
import cz.zcu.kiv.multicloudandroid.display.Account;
import cz.zcu.kiv.multicloudandroid.display.AccountAction;
import cz.zcu.kiv.multicloudandroid.display.AccountAdapter;
import cz.zcu.kiv.multicloudandroid.display.AccountSelectedHandler;

/**
 * cz.zcu.kiv.multicloudandroid.fragment/AccountFragment.java			<br /><br />
 *
 * Fragment for displaying user accounts.
 *
 * @author Jaromír Staněk
 * @version 1.0
 *
 */
public class AccountFragment extends ListFragment {

	/** Handler for the onSelected event. */
	private AccountSelectedHandler handler;

	/**
	 * Adds account to the list.
	 * @param account Account to be added.
	 */
	public void accountAdd(Account account) {
		AccountAdapter accounts = (AccountAdapter) getListAdapter();
		if (accounts.getCount() == 1) {
			Account first = accounts.getItem(0);
			if (first.getCloud() == null) {
				accounts.remove(first);
			}
		}
		accounts.add(account);
		accounts.notifyDataSetChanged();
	}

	/**
	 * Removes account from the list.
	 * @param account Account to be removed.
	 */
	public void accountRemove(Account account) {
		AccountAdapter accounts = (AccountAdapter) getListAdapter();
		accounts.remove(account);
		if (accounts.isEmpty()) {
			Account a = new Account();
			a.setAuthorized(false);
			a.setCloud(null);
			a.setName(getString(R.string.action_add_account));
			accounts.add(a);
		}
		accounts.notifyDataSetChanged();
	}

	/**
	 * Renames account in the list.
	 * @param account Account to be renamed.
	 * @param name New name for the account.
	 */
	public void accountRename(Account account, String name) {
		AccountAdapter accounts = (AccountAdapter) getListAdapter();
		account.setName(name);
		accounts.notifyDataSetChanged();
	}

	/**
	 * Updated account in the list.
	 * @param account Account to be updated.
	 */
	public void accountUpdate(Account account) {
		AccountAdapter accounts = (AccountAdapter) getListAdapter();
		for (int i = 0; i < accounts.getCount(); i++) {
			Account a = accounts.getItem(i);
			if (a.getName().equals(account.getName())) {
				a.setAuthorized(account.isAuthorized());
				a.setTotalSpace(account.getTotalSpace());
				a.setFreeSpace(account.getFreeSpace());
				a.setUsedSpace(account.getUsedSpace());
				break;
			}
		}
		accounts.notifyDataSetChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		registerForContextMenu(getListView());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			handler = (AccountSelectedHandler) activity;
		} catch (ClassCastException e) {
			/* handling of account selection not supported */
			Log.e(MainActivity.MULTICLOUD_NAME, e.getMessage());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		AccountAction action = AccountAction.NONE;
		switch (item.getItemId()) {
		case R.id.item_authorize:
			action = AccountAction.AUTHORIZE;
			break;
		case R.id.item_info:
			action = AccountAction.INFORMATION;
			break;
		case R.id.item_rename:
			action = AccountAction.RENAME;
			break;
		case R.id.item_remove:
			action = AccountAction.REMOVE;
			break;
		default:
			return super.onContextItemSelected(item);
		}
		handler.onAccountSelected(info.position, action);
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setListAdapter(new AccountAdapter(getActivity(), android.R.layout.simple_list_item_activated_1, new ArrayList<Account>()));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		getActivity().getMenuInflater().inflate(R.menu.account_menu, menu);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Account account = (Account) getListAdapter().getItem(position);
		AccountAction action = AccountAction.NONE;
		if (account.getCloud() == null) {
			action = AccountAction.ADD;
		} else {
			if (!account.isAuthorized()) {
				action = AccountAction.AUTHORIZE;
			} else {
				action = AccountAction.LIST;
			}
		}
		handler.onAccountSelected(position, action);
		getListView().setItemChecked(position, true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onStart() {
		super.onStart();
		if (getFragmentManager().findFragmentById(R.id.data_fragment) != null) {
			getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		} else {
			getListView().setChoiceMode(ListView.CHOICE_MODE_NONE);
		}
	}

}

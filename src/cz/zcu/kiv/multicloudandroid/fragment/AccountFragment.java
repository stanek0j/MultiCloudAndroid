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
import android.widget.ListView;
import android.widget.Toast;
import cz.zcu.kiv.multicloudandroid.R;
import cz.zcu.kiv.multicloudandroid.display.Account;
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
	AccountSelectedHandler handler;

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
			Log.e("MultiCloud", e.getMessage());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_add:
			Toast.makeText(getActivity(), "add", Toast.LENGTH_SHORT).show();
			break;
		case R.id.item_authorize:
			Toast.makeText(getActivity(), "auth", Toast.LENGTH_SHORT).show();
			break;
		case R.id.item_info:
			Toast.makeText(getActivity(), "info", Toast.LENGTH_SHORT).show();
			break;
		case R.id.item_remove:
			Toast.makeText(getActivity(), "remove", Toast.LENGTH_SHORT).show();
			break;
		}
		// TODO Auto-generated method stub
		return super.onContextItemSelected(item);
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
		handler.onAccountSelected(position);
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

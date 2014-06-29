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
import cz.zcu.kiv.multicloud.filesystem.FileType;
import cz.zcu.kiv.multicloud.json.FileInfo;
import cz.zcu.kiv.multicloudandroid.MainActivity;
import cz.zcu.kiv.multicloudandroid.R;
import cz.zcu.kiv.multicloudandroid.display.ItemAction;
import cz.zcu.kiv.multicloudandroid.display.ItemAdapter;
import cz.zcu.kiv.multicloudandroid.display.ItemSelectedHandler;

/**
 * cz.zcu.kiv.multicloudandroid.fragment/ItemFragment.java			<br /><br />
 *
 * Fragment for displaying folder contents.
 *
 * @author Jaromír Staněk
 * @version 1.0
 *
 */
public class ItemFragment extends ListFragment {

	/** Handler for the onSelected event. */
	private ItemSelectedHandler handler;

	/**
	 * Method for displaying folder content.
	 * @param folder Folder content.
	 */
	public void displayFolderContent(FileInfo folder) {
		ItemAdapter adapter = (ItemAdapter) getListAdapter();
		adapter.clear();
		if (folder != null) {
			/* loop twice - first for folders, then for files */
			for (FileInfo f: folder.getContent()) {
				if (f.getFileType() == FileType.FOLDER) {
					adapter.add(f);
				}
			}
			for (FileInfo f: folder.getContent()) {
				if (f.getFileType() == FileType.FILE) {
					//if (parent.getPreferences().isHideMetadata() && f.getName().equals(ChecksumProvider.CHECKSUM_FILE)) {
					//	continue;
					//}
					adapter.add(f);
				}
			}
		}
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
			handler = (ItemSelectedHandler) activity;
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
		ItemAction action = ItemAction.NONE;
		switch (item.getItemId()) {
		case R.id.item_download:
			action = ItemAction.DOWNLOAD;
			break;
		case R.id.item_rename:
			action = ItemAction.RENAME;
			break;
		case R.id.item_delete:
			action = ItemAction.DELETE;
			break;
		case R.id.item_checksum:
			action = ItemAction.CHECKSUM;
			break;
		case R.id.item_properties:
			action = ItemAction.PROPERTIES;
			break;
		default:
			return super.onContextItemSelected(item);
		}
		handler.onItemSelected(info.position, action);
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setListAdapter(new ItemAdapter(getActivity(), android.R.layout.simple_list_item_activated_1, new ArrayList<FileInfo>()));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		ItemAdapter adapter = (ItemAdapter) getListAdapter();
		FileInfo item = adapter.getItem(info.position);
		if (item.getFileType() == FileType.FILE) {
			getActivity().getMenuInflater().inflate(R.menu.item_file_menu, menu);
		} else {
			getActivity().getMenuInflater().inflate(R.menu.item_folder_menu, menu);
		}
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
		ItemAdapter adapter = (ItemAdapter) getListAdapter();
		FileInfo item = adapter.getItem(position);
		ItemAction action = ItemAction.NONE;
		if (item.getFileType() == FileType.FOLDER) {
			action = ItemAction.LIST;
			handler.onItemSelected(position, action);
		}
		getListView().setItemChecked(position, true);
	}

}

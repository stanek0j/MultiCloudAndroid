package cz.zcu.kiv.multicloudandroid.display;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import cz.zcu.kiv.multicloudandroid.PrefsHelper;
import cz.zcu.kiv.multicloudandroid.R;

/**
 * cz.zcu.kiv.multicloudandroid.display/SynchronizePreference.java			<br /><br />
 *
 * Preference dialog for selecting accounts to synchronize files to.
 *
 * @author Jaromír Staněk
 * @version 1.0
 *
 */
public class SynchronizePreference extends DialogPreference {

	/** Preference helper. */
	private final PrefsHelper prefs;
	/** Selected file node. */
	private SyncData fileNode;
	/** Selected folder node. */
	private SyncData folderNode;
	/** Root folder. */
	private File rootFolder;
	/** Synchronization data. */
	private SyncData data;
	/** Checkable account settings. */
	private final List<CheckBox> checkAccounts;
	/** Layout for checkboxes. */
	private LinearLayout layout;

	/**
	 * Ctor with necessary parameters.
	 * @param context Context.
	 * @param attrs Attributes.
	 */
	public SynchronizePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setDialogLayoutResource(R.layout.synch_chooser_dialog);
		prefs = new PrefsHelper(context);
		data = prefs.getSyncData();
		checkAccounts = new ArrayList<>();
		for (String account: prefs.getAccounts()) {
			CheckBox check = new CheckBox(context, attrs);
			check.setText(account);
			checkAccounts.add(check);
		}
	}

	/**
	 * Clears checked accounts.
	 */
	private void clearFileSynch() {
		for (CheckBox check: checkAccounts) {
			check.setChecked(false);
		}
	}

	/**
	 * Lists the supplied folder and filters out files.
	 * @param file Folder to be filtered.
	 * @return List of files.
	 */
	private List<SyncData> filterFolderContent(SyncData file) {
		List<SyncData> show = new ArrayList<>();
		if (file == null) {
			return show;
		}
		if (file.getNodes() == null) {
			return show;
		}
		for (SyncData f: file.getNodes()) {
			if (f.getLocalFile().isDirectory()) {
				show.add(f);
			}
		}
		for (SyncData f: file.getNodes()) {
			if (f.getLocalFile().isFile()) {
				show.add(f);
			}
		}
		return show;
	}

	/**
	 * Loads local file into the structure.
	 * @param folder Folder to be loaded.
	 * @param parent Parent node to add children to.
	 * @return Created node.
	 */
	private SyncData loadLocalFiles(File folder, SyncData parent) {
		SyncData node = null;
		if (parent == null) {
			node = new SyncData();
			node.setName("root");
			node.setLocalFile(folder);
			node.setRoot(true);
		} else {
			node = new SyncData();
			node.setName(folder.getName());
			node.setLocalFile(folder);
			node.setParent(parent);
		}
		if (folder.isDirectory()) {
			for (File content: folder.listFiles()) {
				loadLocalFiles(content, node);
			}
		}
		if (parent != null) {
			parent.getNodes().add(node);
		}
		return node;
	}

	/**
	 * Loads local file structure.
	 * @return Local file structure.
	 */
	private SyncData loadLocalFileStructure() {
		SyncData saved = prefs.getSyncData();
		SyncData actual = loadLocalFiles(rootFolder, null);
		matchStructure(saved, actual);
		return actual;
	}

	/**
	 * Match saved data to actual structure.
	 * @param data Saved data.
	 * @param structure Actual structure.
	 */
	private void matchStructure(SyncData data, SyncData structure) {
		if (data == null || structure == null) {
			return;
		}
		if (data.getName() != null && data.getName().equals(structure.getName())) {
			if (data.getNodes().isEmpty() && structure.getNodes().isEmpty()) {
				structure.getAccounts().putAll(data.getAccounts());
				structure.setChecksum(data.getChecksum());
			} else {
				for (SyncData content: data.getNodes()) {
					for (SyncData inner: structure.getNodes()) {
						if (content.getName().equals(inner.getName())) {
							matchStructure(content, inner);
						}
					}
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onBindDialogView(View view) {
		super.onBindView(view);
		String rootPath = getSharedPreferences().getString(PrefsHelper.PREFS_SYNCH_FOLDER, null);
		rootFolder = null;
		if (rootPath != null) {
			rootFolder = new File(rootPath);
			if (rootFolder.exists()) {
				if (!rootFolder.isDirectory()) {
					rootFolder = null;
				}
			} else {
				rootFolder = null;
			}
		}
		if (rootFolder == null) {
			return;
		}
		data = loadLocalFileStructure();
		folderNode = data;
		final TextView synchPath = (TextView) view.findViewById(R.id.textView_synch_path);
		synchPath.setText(replaceRoot(folderNode.getLocalFile(), rootFolder));
		final TextView empty = (TextView) view.findViewById(R.id.textView_synch_emtpy_folder);
		final ListView list = (ListView) view.findViewById(R.id.listView_synch_files);
		final SynchAdapter adapter = new SynchAdapter(getContext(), android.R.layout.simple_list_item_activated_1, filterFolderContent(folderNode));
		list.setAdapter(adapter);
		list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			/**
			 * {@inheritDoc}
			 */
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				SyncData selectedNode = (SyncData) list.getItemAtPosition(position);
				File selected = selectedNode.getLocalFile();
				retainFileSynch(fileNode);
				if (selected.isDirectory()) {
					fileNode = null;
					folderNode = selectedNode;
					synchPath.setText(replaceRoot(folderNode.getLocalFile(), rootFolder));
					clearFileSynch();
				} else {
					fileNode = selectedNode;
					folderNode = fileNode.getParent();
					synchPath.setText(replaceRoot(fileNode.getLocalFile(), rootFolder));
					showFileSynch(fileNode);
				}
				adapter.clear();
				adapter.addAll(filterFolderContent(folderNode));
				adapter.notifyDataSetChanged();
				if (adapter.isEmpty()) {
					empty.setVisibility(View.VISIBLE);
				} else {
					empty.setVisibility(View.GONE);
				}
			}
		});
		if (adapter.isEmpty()) {
			empty.setVisibility(View.VISIBLE);
		} else {
			empty.setVisibility(View.GONE);
		}
		Button btnSave = (Button) view.findViewById(R.id.button_synch_save);
		btnSave.setOnClickListener(new View.OnClickListener() {
			/**
			 * {@inheritDoc}
			 */
			@Override
			public void onClick(View v) {
				retainFileSynch(fileNode);
				adapter.notifyDataSetChanged();
				prefs.setSyncData(data);
			}
		});
		Button btnParent = (Button) view.findViewById(R.id.button_synch_parent);
		btnParent.setOnClickListener(new View.OnClickListener() {
			/**
			 * {@inheritDoc}
			 */
			@Override
			public void onClick(View v) {
				if (!folderNode.isRoot()) {
					retainFileSynch(fileNode);
					fileNode = null;
					folderNode = folderNode.getParent();
					adapter.clear();
					adapter.addAll(filterFolderContent(folderNode));
					adapter.notifyDataSetChanged();
					if (adapter.isEmpty()) {
						empty.setVisibility(View.VISIBLE);
					} else {
						empty.setVisibility(View.GONE);
					}
					synchPath.setText(replaceRoot(folderNode.getLocalFile(), rootFolder));
					clearFileSynch();
				}
			}
		});
		layout = (LinearLayout) view.findViewById(R.id.list_clouds);
		for (CheckBox check: checkAccounts) {
			layout.addView(check);
		}
		clearFileSynch();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (positiveResult) {
			prefs.setSyncData(data);
		}
		if (layout != null) {
			layout.removeAllViews();
		}
	}

	/**
	 * Replaces the root path with substitution string.
	 * @param file File to get path from.
	 * @param root Root folder.
	 * @return Path with substituted root.
	 */
	private String replaceRoot(File file, File root) {
		String path = file.getAbsolutePath();
		return path.replaceFirst(root.getAbsolutePath(), getContext().getText(R.string.desc_synch_root).toString());
	}

	/**
	 * Saving checked accounts to selected file.
	 * @param data Selected file.
	 */
	private void retainFileSynch(SyncData data) {
		if (data == null) {
			return;
		}
		data.getAccounts().clear();
		for (CheckBox check: checkAccounts) {
			if (check.isChecked()) {
				data.getAccounts().put(check.getText().toString(), null);
			}
		}
	}

	/**
	 * Marks accounts that are chosen for the selected file.
	 * @param data Selected file.
	 */
	private void showFileSynch(SyncData data) {
		for (CheckBox check: checkAccounts) {
			check.setChecked(data.getAccounts().containsKey(check.getText().toString()));
		}
	}

}

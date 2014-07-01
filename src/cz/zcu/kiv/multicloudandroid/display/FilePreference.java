package cz.zcu.kiv.multicloudandroid.display;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import cz.zcu.kiv.multicloudandroid.MainActivity;
import cz.zcu.kiv.multicloudandroid.R;

/**
 * cz.zcu.kiv.multicloudandroid.display/FilePreference.java			<br /><br />
 *
 * Preference settings with dialog containing file chooser.
 *
 * @author Jaromír Staněk
 * @version 1.0
 *
 */
public class FilePreference extends DialogPreference {

	/** Selected file. */
	private File file;
	/** If only folders should be showed. */
	private final boolean foldersOnly;

	/**
	 * Ctor with necessary parameters.
	 * @param context Context.
	 * @param attrs Attributes.
	 */
	public FilePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setDialogLayoutResource(R.layout.file_chooser_dialog);
		TypedArray filePrefs = context.obtainStyledAttributes(attrs, R.styleable.FilePreference, 0, 0);
		foldersOnly = filePrefs.getBoolean(R.styleable.FilePreference_folders_only, false);
		filePrefs.recycle();
	}

	/**
	 * Lists the supplied folder and filters out files.
	 * @param file Folder to be filtered.
	 * @return List of files.
	 */
	private List<File> filterFolderContent(File file) {
		List<File> show = new ArrayList<>();
		if (!file.isDirectory()) {
			return show;
		}
		if (file.listFiles() == null) {
			return show;
		}
		for (File f: file.listFiles()) {
			if (foldersOnly) {
				if (f.isDirectory()) {
					show.add(f);
				}
			} else {
				show.add(f);
			}
		}
		return show;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);
		String savedPath = getSharedPreferences().getString(getKey(), null);
		if (savedPath == null) {
			file = new File(MainActivity.ROOT_FOLDER);
		} else {
			file = new File(savedPath);
			if (!file.exists()) {
				file = new File(MainActivity.ROOT_FOLDER);
			} else {
				if (foldersOnly && !file.isDirectory()) {
					file = new File(MainActivity.ROOT_FOLDER);
				}
			}
		}
		final TextView path = (TextView) view.findViewById(R.id.textView_path);
		path.setText(file.getAbsolutePath());
		final TextView empty = (TextView) view.findViewById(R.id.textView_emtpy_folder);
		final ListView list = (ListView) view.findViewById(R.id.listView_files);
		final FileAdapter adapter = new FileAdapter(getContext(), android.R.layout.simple_list_item_activated_1, filterFolderContent(file));
		list.setAdapter(adapter);
		list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			/**
			 * {@inheritDoc}
			 */
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				File selected = (File) list.getItemAtPosition(position);
				file = selected;
				adapter.clear();
				adapter.addAll(filterFolderContent(file));
				adapter.notifyDataSetChanged();
				if (adapter.isEmpty()) {
					empty.setVisibility(View.VISIBLE);
				} else {
					empty.setVisibility(View.GONE);
				}
				path.setText(file.getAbsolutePath());
			}
		});
		if (adapter.isEmpty()) {
			empty.setVisibility(View.VISIBLE);
		} else {
			empty.setVisibility(View.GONE);
		}
		Button btnClear = (Button) view.findViewById(R.id.button_clear);
		btnClear.setOnClickListener(new View.OnClickListener() {
			/**
			 * {@inheritDoc}
			 */
			@Override
			public void onClick(View v) {
				file = null;
				persistString(null);
				getDialog().dismiss();
			}
		});
		Button btnNewFolder = (Button) view.findViewById(R.id.button_folder);
		btnNewFolder.setOnClickListener(new View.OnClickListener() {
			/**
			 * {@inheritDoc}
			 */
			@Override
			public void onClick(View v) {
				final View view = LayoutInflater.from(getContext()).inflate(R.layout.file_name_dialog, null);
				AlertDialog dialog = new AlertDialog.Builder(getContext())
				.setTitle(R.string.action_new_folder)
				.setView(view)
				.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
					/**
					 * {@inheritDoc}
					 */
					@Override
					public void onClick(DialogInterface dialog, int which) {
						EditText text = (EditText) view.findViewById(R.id.editText_file_folder_name);
						File folder = new File(file, text.getText().toString());
						if (folder.mkdir()) {
							adapter.clear();
							adapter.addAll(filterFolderContent(file));
							adapter.notifyDataSetChanged();
							if (adapter.isEmpty()) {
								empty.setVisibility(View.VISIBLE);
							} else {
								empty.setVisibility(View.GONE);
							}
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
				EditText text = (EditText) dialog.findViewById(R.id.editText_file_folder_name);
				text.setHint(R.string.desc_enter_folder);
			}
		});
		Button btnParent = (Button) view.findViewById(R.id.button_parent);
		btnParent.setOnClickListener(new View.OnClickListener() {
			/**
			 * {@inheritDoc}
			 */
			@Override
			public void onClick(View v) {
				if (file.getParentFile() != null) {
					file = file.getParentFile();
					adapter.clear();
					adapter.addAll(filterFolderContent(file));
					adapter.notifyDataSetChanged();
					if (adapter.isEmpty()) {
						empty.setVisibility(View.VISIBLE);
					} else {
						empty.setVisibility(View.GONE);
					}
					path.setText(file.getAbsolutePath());
				}
			}
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (positiveResult) {
			if (file != null) {
				persistString(file.getAbsolutePath());
			} else {
				persistString(null);
			}
		}
	}

}

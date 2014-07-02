package cz.zcu.kiv.multicloudandroid.display;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import cz.zcu.kiv.multicloud.MultiCloudException;
import cz.zcu.kiv.multicloud.filesystem.FileType;
import cz.zcu.kiv.multicloud.json.AccountInfo;
import cz.zcu.kiv.multicloud.json.AccountQuota;
import cz.zcu.kiv.multicloud.json.CloudSettings;
import cz.zcu.kiv.multicloud.json.FileInfo;
import cz.zcu.kiv.multicloud.utils.Utils;
import cz.zcu.kiv.multicloud.utils.Utils.UnitsFormat;
import cz.zcu.kiv.multicloudandroid.MainActivity;
import cz.zcu.kiv.multicloudandroid.R;
import cz.zcu.kiv.multicloudandroid.tasks.DeleteTask;
import cz.zcu.kiv.multicloudandroid.tasks.DownloadTask;
import cz.zcu.kiv.multicloudandroid.tasks.NewFolderTask;
import cz.zcu.kiv.multicloudandroid.tasks.RenameTask;
import cz.zcu.kiv.multicloudandroid.tasks.UploadTask;

/**
 * cz.zcu.kiv.multicloudandroid.display/DialogCreator.java			<br /><br />
 *
 * Class for creating simple dialogs.
 *
 * @author Jaromír Staněk
 * @version 1.0
 *
 */
public class DialogCreator {

	/** Activity. */
	private final MainActivity activity;
	/** Selected file. */
	private File file;
	/** Selected folder. */
	private File folder;
	/** Destination file. */
	private FileInfo dst;

	/**
	 * Ctor with activity as parameter.
	 * @param activity Activity.
	 */
	public DialogCreator(MainActivity activity) {
		this.activity = activity;
	}

	/**
	 * Dialog for showing account information.
	 * @param info Account information.
	 * @param quota Account quota.
	 */
	public void dialogAccountInfo(final AccountInfo info, final AccountQuota quota) {
		final View view = activity.getLayoutInflater().inflate(R.layout.account_info_dialog, null);
		AlertDialog dialog = new AlertDialog.Builder(activity)
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

	/**
	 * Dialog for entering account name.
	 * @param account Account name for renaming.
	 */
	public void dialogAccountName(final Account account) {
		final View view = activity.getLayoutInflater().inflate(R.layout.account_name_dialog, null);
		AlertDialog dialog = new AlertDialog.Builder(activity)
		.setTitle(R.string.action_add_account)
		.setView(view)
		.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
			/**
			 * {@inheritDoc}
			 */
			@Override
			public void onClick(DialogInterface dialog, int which) {
				EditText text = (EditText) view.findViewById(R.id.editText_user_name);
				Spinner clouds = (Spinner) view.findViewById(R.id.spinner_clouds);
				try {
					if (account == null) {
						Account a = new Account();
						a.setAuthorized(false);
						a.setCloud((String) clouds.getSelectedItem());
						a.setName(text.getText().toString());
						activity.getLibrary().createAccount(a.getName(), a.getCloud());
						activity.actionAddAccount(a);
					} else {
						activity.getLibrary().renameAccount(account.getName(), text.getText().toString());
						activity.actionRenameAccount(account, text.getText().toString());
					}
				} catch (MultiCloudException e) {
					activity.showError(R.string.action_add_account, e.getMessage());
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
			for (CloudSettings cs: activity.getLibrary().getSettings().getCloudManager().getAllCloudSettings()) {
				cloudTypes.add(cs.getSettingsId());
			}
			ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, cloudTypes);
			clouds.setAdapter(adapter);
			if (account != null) {
				for (int i = 0; i < adapter.getCount(); i++) {
					if (adapter.getItem(i).equals(account.getCloud())) {
						clouds.setSelection(i);
						break;
					}
				}
			}
		}
		if (account != null) {
			clouds.setEnabled(false);
			EditText text = (EditText) dialog.findViewById(R.id.editText_user_name);
			text.setText(account.getName());
			text.setSelection(text.getText().length());
		}
	}

	/**
	 * Confirmation dialog for deleting a file or folder.
	 * @param file File or folder to be deleted.
	 */
	public void dialogFileDelete(final FileInfo file) {
		AlertDialog dialog = new AlertDialog.Builder(activity)
		.setTitle(R.string.action_delete)
		.setMessage(R.string.desc_delete)
		.setPositiveButton(R.string.button_yes, new DialogInterface.OnClickListener() {
			/**
			 * {@inheritDoc}
			 */
			@Override
			public void onClick(DialogInterface dialog, int which) {
				DeleteTask delete = new DeleteTask(activity, file);
				delete.execute();
			}
		})
		.setNegativeButton(R.string.button_no, new DialogInterface.OnClickListener() {
			/**
			 * {@inheritDoc}
			 */
			@Override
			public void onClick(DialogInterface dialog, int which) {
				activity.showToast(R.string.err_op_cancel);
				dialog.cancel();
			}
		})
		.create();
		dialog.show();
	}

	/**
	 * Dialog for downloading file.
	 */
	public void dialogFileDownload(final FileInfo source) {
		folder = new File(activity.getPrefsHelper().getLastFolder());
		file = null;
		final View view = activity.getLayoutInflater().inflate(R.layout.file_chooser_dialog, null);
		AlertDialog dialog = new AlertDialog.Builder(activity)
		.setTitle(R.string.action_download)
		.setView(view)
		.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
			/**
			 * {@inheritDoc}
			 */
			@Override
			public void onClick(DialogInterface dialog, int which) {
				final View view = activity.getLayoutInflater().inflate(R.layout.file_name_dialog, null);
				AlertDialog nameDialog = new AlertDialog.Builder(activity)
				.setTitle(R.string.desc_target_name)
				.setView(view)
				.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
					/**
					 * {@inheritDoc}
					 */
					@Override
					public void onClick(DialogInterface dialog, int which) {
						EditText text = (EditText) view.findViewById(R.id.editText_file_folder_name);
						if (text.getText().toString().trim().isEmpty()) {
							activity.showToast(R.string.err_no_local_name);
						} else {
							file = new File(folder, text.getText().toString().trim());
							if (file.exists()) {
								if (file.isDirectory()) {
									activity.showToast(R.string.err_folder);
								} else {
									AlertDialog confirmDialog = new AlertDialog.Builder(activity)
									.setTitle(R.string.desc_overwrite)
									.setMessage(R.string.desc_overwrite_msg)
									.setPositiveButton(R.string.button_yes, new DialogInterface.OnClickListener() {
										/**
										 * {@inheritDoc}
										 */
										@Override
										public void onClick(DialogInterface dialog, int which) {
											DownloadTask download = new DownloadTask(activity, source, file, true);
											download.execute();
										}
									})
									.setNegativeButton(R.string.button_no, new DialogInterface.OnClickListener() {
										/**
										 * {@inheritDoc}
										 */
										@Override
										public void onClick(DialogInterface dialog, int which) {
											activity.showToast(R.string.err_no_overwrite);
											dialog.cancel();
										}
									})
									.create();
									confirmDialog.show();
								}
							} else {
								DownloadTask download = new DownloadTask(activity, source, file, false);
								download.execute();
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
						activity.showToast(R.string.err_op_cancel);
						dialog.cancel();
					}
				})
				.create();
				nameDialog.show();
				EditText text = (EditText) nameDialog.findViewById(R.id.editText_file_folder_name);
				text.setHint(R.string.desc_enter_file);
				if (file != null) {
					text.setText(file.getName());
				} else {
					text.setText(source.getName());
				}
				text.setSelection(text.getText().length());
			}
		})
		.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
			/**
			 * {@inheritDoc}
			 */
			@Override
			public void onClick(DialogInterface dialog, int which) {
				activity.showToast(R.string.err_op_cancel);
				dialog.cancel();
			}
		})
		.create();
		dialog.show();
		final TextView path = (TextView) view.findViewById(R.id.textView_path);
		path.setText(folder.getAbsolutePath());
		final TextView empty = (TextView) view.findViewById(R.id.textView_emtpy_folder);
		final ListView list = (ListView) view.findViewById(R.id.listView_files);
		final FileAdapter adapter = new FileAdapter(activity, android.R.layout.simple_list_item_activated_1, filterFolderContent(folder));
		list.setAdapter(adapter);
		list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			/**
			 * {@inheritDoc}
			 */
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				File selected = (File) list.getItemAtPosition(position);
				if (selected.isDirectory()) {
					file = null;
					folder = selected;
					path.setText(folder.getAbsolutePath());
				} else {
					file = selected;
					folder = file.getParentFile();
					path.setText(file.getAbsolutePath());
				}
				activity.getPrefsHelper().setLastFolder(folder.getAbsolutePath());
				adapter.clear();
				adapter.addAll(filterFolderContent(folder));
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
		Button btnClear = (Button) view.findViewById(R.id.button_clear);
		btnClear.setVisibility(View.GONE);
		Button btnNewFolder = (Button) view.findViewById(R.id.button_save);
		btnNewFolder.setOnClickListener(new View.OnClickListener() {
			/**
			 * {@inheritDoc}
			 */
			@Override
			public void onClick(View v) {
				final View view = LayoutInflater.from(activity).inflate(R.layout.file_name_dialog, null);
				AlertDialog dialog = new AlertDialog.Builder(activity)
				.setTitle(R.string.action_new_folder)
				.setView(view)
				.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
					/**
					 * {@inheritDoc}
					 */
					@Override
					public void onClick(DialogInterface dialog, int which) {
						EditText text = (EditText) view.findViewById(R.id.editText_file_folder_name);
						File newFolder = new File(folder, text.getText().toString().trim());
						if (newFolder.mkdir()) {
							adapter.clear();
							adapter.addAll(filterFolderContent(folder));
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
				if (folder.getParentFile() != null) {
					folder = folder.getParentFile();
					adapter.clear();
					adapter.addAll(filterFolderContent(folder));
					adapter.notifyDataSetChanged();
					if (adapter.isEmpty()) {
						empty.setVisibility(View.VISIBLE);
					} else {
						empty.setVisibility(View.GONE);
					}
					path.setText(folder.getAbsolutePath());
				}
			}
		});
	}

	/**
	 * Dialog for creating new folder.
	 */
	public void dialogFileNewFolder() {
		final View view = activity.getLayoutInflater().inflate(R.layout.file_name_dialog, null);
		AlertDialog dialog = new AlertDialog.Builder(activity)
		.setTitle(R.string.action_new_folder)
		.setView(view)
		.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
			/**
			 * {@inheritDoc}
			 */
			@Override
			public void onClick(DialogInterface dialog, int which) {
				EditText text = (EditText) view.findViewById(R.id.editText_file_folder_name);
				if (text.getText().toString().trim().isEmpty()) {
					activity.showToast(R.string.err_no_name);
				} else {
					NewFolderTask newFolder = new NewFolderTask(activity, text.getText().toString().trim());
					newFolder.execute();
				}
			}
		})
		.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
			/**
			 * {@inheritDoc}
			 */
			@Override
			public void onClick(DialogInterface dialog, int which) {
				activity.showToast(R.string.err_op_cancel);
				dialog.cancel();
			}
		})
		.create();
		dialog.show();
		EditText text = (EditText) dialog.findViewById(R.id.editText_file_folder_name);
		text.setHint(R.string.desc_enter_folder);
	}

	/**
	 * Dialog for displaying file or folder properties.
	 * @param file File or folder to show properties of.
	 */
	public void dialogFileProperties(final FileInfo file) {
		final View view = activity.getLayoutInflater().inflate(R.layout.file_properties_dialog, null);
		AlertDialog dialog = new AlertDialog.Builder(activity)
		.setTitle(R.string.action_properties)
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
		TextView fileName = (TextView) dialog.findViewById(R.id.textView_file_name);
		fileName.setText(file.getName());
		TextView fileId = (TextView) dialog.findViewById(R.id.textView_file_id);
		if (file.getId() != null) {
			fileId.setText(file.getId());
		} else {
			fileId.setText("-");
		}
		TextView fileSize = (TextView) dialog.findViewById(R.id.textView_file_size);
		fileSize.setText(Utils.formatSize(file.getSize(), UnitsFormat.BINARY) + " (" + file.getSize() + "B)");
		SimpleDateFormat dt = new SimpleDateFormat(MainActivity.DATE_FORMAT, Locale.getDefault());
		TextView created = (TextView) dialog.findViewById(R.id.textView_created);
		if (file.getCreated() != null) {
			created.setText(dt.format(file.getCreated()));
		} else {
			created.setText("-");
		}
		TextView modified = (TextView) dialog.findViewById(R.id.textView_modified);
		if (file.getModified() != null) {
			modified.setText(dt.format(file.getModified()));
		} else {
			modified.setText("-");
		}
		TextView checksum = (TextView) dialog.findViewById(R.id.textView_checksum);
		if (file.getChecksum() != null) {
			checksum.setText(file.getChecksum());
		} else {
			checksum.setText("-");
		}
	}

	/**
	 * Dialog for renaming file or folder.
	 * @param file File or folder to be renamed.
	 */
	public void dialogFileRename(final FileInfo file) {
		final View view = activity.getLayoutInflater().inflate(R.layout.file_name_dialog, null);
		AlertDialog dialog = new AlertDialog.Builder(activity)
		.setTitle(R.string.action_rename)
		.setView(view)
		.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
			/**
			 * {@inheritDoc}
			 */
			@Override
			public void onClick(DialogInterface dialog, int which) {
				EditText text = (EditText) view.findViewById(R.id.editText_file_folder_name);
				if (text.getText().toString().trim().isEmpty()) {
					activity.showToast(R.string.err_no_name);
				} else {
					RenameTask rename = new RenameTask(activity, file, text.getText().toString().trim());
					rename.execute();
				}
			}
		})
		.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
			/**
			 * {@inheritDoc}
			 */
			@Override
			public void onClick(DialogInterface dialog, int which) {
				activity.showToast(R.string.err_op_cancel);
				dialog.cancel();
			}
		})
		.create();
		dialog.show();
		EditText text = (EditText) dialog.findViewById(R.id.editText_file_folder_name);
		if (file.getFileType() == FileType.FILE) {
			text.setHint(R.string.desc_enter_file);
		} else {
			text.setHint(R.string.desc_enter_folder);
		}
		text.setText(file.getName());
		text.setSelection(text.getText().length());
	}

	/**
	 * Dialog for uploading file.
	 * @param destination Destination folder.
	 */
	public void dialogFileUpload(final FileInfo destination) {
		folder = new File(activity.getPrefsHelper().getLastFolder());
		file = null;
		final View view = activity.getLayoutInflater().inflate(R.layout.file_chooser_dialog, null);
		AlertDialog dialog = new AlertDialog.Builder(activity)
		.setTitle(R.string.action_upload)
		.setView(view)
		.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
			/**
			 * {@inheritDoc}
			 */
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (file == null) {
					activity.showToast(R.string.err_no_local_file);
				} else {
					boolean found = false;
					for (FileInfo f: destination.getContent()) {
						if (file.getName().equals(f.getName())) {
							dst = f;
							found = true;
							break;
						}
					}
					if (found) {
						AlertDialog confirmDialog = new AlertDialog.Builder(activity)
						.setTitle(R.string.desc_overwrite)
						.setMessage(R.string.desc_overwrite_msg)
						.setPositiveButton(R.string.button_yes, new DialogInterface.OnClickListener() {
							/**
							 * {@inheritDoc}
							 */
							@Override
							public void onClick(DialogInterface dialog, int which) {
								UploadTask upload = new UploadTask(activity, file, dst, destination, true);
								upload.execute();
							}
						})
						.setNegativeButton(R.string.button_no, new DialogInterface.OnClickListener() {
							/**
							 * {@inheritDoc}
							 */
							@Override
							public void onClick(DialogInterface dialog, int which) {
								activity.showToast(R.string.err_no_overwrite);
								dialog.cancel();
							}
						})
						.create();
						confirmDialog.show();
					} else {
						UploadTask upload = new UploadTask(activity, file, null, destination, false);
						upload.execute();
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
				activity.showToast(R.string.err_op_cancel);
				dialog.cancel();
			}
		})
		.create();
		dialog.show();
		final TextView path = (TextView) view.findViewById(R.id.textView_path);
		path.setText(folder.getAbsolutePath());
		final TextView empty = (TextView) view.findViewById(R.id.textView_emtpy_folder);
		final ListView list = (ListView) view.findViewById(R.id.listView_files);
		final FileAdapter adapter = new FileAdapter(activity, android.R.layout.simple_list_item_activated_1, filterFolderContent(folder));
		list.setAdapter(adapter);
		list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			/**
			 * {@inheritDoc}
			 */
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				File selected = (File) list.getItemAtPosition(position);
				if (selected.isDirectory()) {
					file = null;
					folder = selected;
					path.setText(folder.getAbsolutePath());
				} else {
					file = selected;
					folder = file.getParentFile();
					path.setText(file.getAbsolutePath());
				}
				activity.getPrefsHelper().setLastFolder(folder.getAbsolutePath());
				adapter.clear();
				adapter.addAll(filterFolderContent(folder));
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
		Button btnClear = (Button) view.findViewById(R.id.button_clear);
		btnClear.setVisibility(View.GONE);
		Button btnNewFolder = (Button) view.findViewById(R.id.button_save);
		btnNewFolder.setOnClickListener(new View.OnClickListener() {
			/**
			 * {@inheritDoc}
			 */
			@Override
			public void onClick(View v) {
				final View view = LayoutInflater.from(activity).inflate(R.layout.file_name_dialog, null);
				AlertDialog dialog = new AlertDialog.Builder(activity)
				.setTitle(R.string.action_new_folder)
				.setView(view)
				.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
					/**
					 * {@inheritDoc}
					 */
					@Override
					public void onClick(DialogInterface dialog, int which) {
						EditText text = (EditText) view.findViewById(R.id.editText_file_folder_name);
						File newFolder = new File(folder, text.getText().toString().trim());
						if (newFolder.mkdir()) {
							adapter.clear();
							adapter.addAll(filterFolderContent(folder));
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
				if (folder.getParentFile() != null) {
					folder = folder.getParentFile();
					adapter.clear();
					adapter.addAll(filterFolderContent(folder));
					adapter.notifyDataSetChanged();
					if (adapter.isEmpty()) {
						empty.setVisibility(View.VISIBLE);
					} else {
						empty.setVisibility(View.GONE);
					}
					path.setText(folder.getAbsolutePath());
				}
			}
		});
	}

	/**
	 * Lists the supplied folder and filters out files.
	 * @param file Folder to be filtered.
	 * @return List of files.
	 */
	private List<File> filterFolderContent(File file) {
		List<File> show = new ArrayList<>();
		if (file == null) {
			return show;
		}
		if (!file.isDirectory()) {
			return show;
		}
		if (file.listFiles() == null) {
			return show;
		}
		for (File f: file.listFiles()) {
			if (f.isDirectory()) {
				show.add(f);
			}
		}
		for (File f: file.listFiles()) {
			if (f.isFile()) {
				show.add(f);
			}
		}
		return show;
	}

}

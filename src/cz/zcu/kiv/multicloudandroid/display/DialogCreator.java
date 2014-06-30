package cz.zcu.kiv.multicloudandroid.display;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
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
import cz.zcu.kiv.multicloudandroid.tasks.NewFolderTask;
import cz.zcu.kiv.multicloudandroid.tasks.RenameTask;

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
				dialog.cancel();
			}
		})
		.create();
		dialog.show();
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
				NewFolderTask newFolder = new NewFolderTask(activity, text.getText().toString());
				newFolder.execute();
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
				RenameTask rename = new RenameTask(activity, file, text.getText().toString());
				rename.execute();
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
		if (file.getFileType() == FileType.FILE) {
			text.setHint(R.string.desc_enter_file);
		} else {
			text.setHint(R.string.desc_enter_folder);
		}
		text.setText(file.getName());
		text.setSelection(text.getText().length());
	}

}

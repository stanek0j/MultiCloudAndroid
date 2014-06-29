package cz.zcu.kiv.multicloudandroid.display;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import cz.zcu.kiv.multicloud.filesystem.FileType;
import cz.zcu.kiv.multicloud.json.FileInfo;
import cz.zcu.kiv.multicloud.utils.Utils;
import cz.zcu.kiv.multicloud.utils.Utils.UnitsFormat;
import cz.zcu.kiv.multicloudandroid.MainActivity;
import cz.zcu.kiv.multicloudandroid.R;

/**
 * cz.zcu.kiv.multicloudandroid.display/ItemAdapter.java			<br /><br />
 *
 * Adapter for holding and displaying items.
 *
 * @author Jaromír Staněk
 * @version 1.0
 *
 */
public class ItemAdapter extends ArrayAdapter<FileInfo> {

	/**
	 * Ctor with necessary parameters.
	 * @param context Context.
	 * @param resource Resource ID.
	 * @param objects List of items.
	 */
	public ItemAdapter(Context context, int resource, List<FileInfo> objects) {
		super(context, resource, objects);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.item_item, parent, false);
		FileInfo value = getItem(position);
		ImageView icon = (ImageView) view.findViewById(R.id.imageView_icon);
		TextView name = (TextView) view.findViewById(R.id.textView_filename);
		TextView size = (TextView) view.findViewById(R.id.textView_size);
		TextView modified = (TextView) view.findViewById(R.id.textView_modified);
		name.setText(value.getName());
		if (value.getFileType() == FileType.FILE) {
			icon.setImageResource(R.drawable.file_small);
			size.setText(Utils.formatSize(value.getSize(), UnitsFormat.BINARY));
		} else {
			icon.setImageResource(R.drawable.folder_small);
		}
		SimpleDateFormat dt = new SimpleDateFormat(MainActivity.DATE_FORMAT, Locale.getDefault());
		if (value.getModified() != null) {
			modified.setText(dt.format(value.getModified()));
		} else if (value.getCreated() != null) {
			modified.setText(dt.format(value.getCreated()));
		}
		return view;
	}

}

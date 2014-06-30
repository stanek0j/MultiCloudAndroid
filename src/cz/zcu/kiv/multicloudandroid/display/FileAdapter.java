package cz.zcu.kiv.multicloudandroid.display;

import java.io.File;
import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import cz.zcu.kiv.multicloudandroid.R;

/**
 * cz.zcu.kiv.multicloudandroid.display/FileAdapter.java			<br /><br />
 *
 * Adapter for holding and displaying local files.
 *
 * @author Jaromír Staněk
 * @version 1.0
 *
 */
public class FileAdapter extends ArrayAdapter<File> {

	/**
	 * Ctor with necessary parameters.
	 * @param context Context.
	 * @param resource Resource ID.
	 * @param objects List of items.
	 */
	public FileAdapter(Context context, int resource, List<File> objects) {
		super(context, resource, objects);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TextView item = new TextView(getContext());
		File file = getItem(position);
		item.setText(file.getName());
		if (file.isDirectory()) {
			item.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_folder, 0, 0, 0);
		} else {
			item.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_file, 0, 0, 0);
		}
		item.setCompoundDrawablePadding(4);
		return item;
	}

}

package cz.zcu.kiv.multicloudandroid.display;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import cz.zcu.kiv.multicloudandroid.R;

/**
 * cz.zcu.kiv.multicloudandroid.display/SynchAdapter.java			<br /><br />
 *
 * Adapter for holding and displaying local files with synchronization preferences.
 *
 * @author Jaromír Staněk
 * @version 1.0
 *
 */
public class SynchAdapter extends ArrayAdapter<SyncData> {

	/**
	 * Ctor with necessary parameters.
	 * @param context Context.
	 * @param resource Resource ID.
	 * @param objects List of items.
	 */
	public SynchAdapter(Context context, int resource, List<SyncData> objects) {
		super(context, resource, objects);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.item_item, parent, false);
		SyncData file = getItem(position);
		ImageView icon = (ImageView) view.findViewById(R.id.imageView_icon);
		TextView name = (TextView) view.findViewById(R.id.textView_filename);
		TextView size = (TextView) view.findViewById(R.id.textView_size);
		TextView modified = (TextView) view.findViewById(R.id.textView_modified);
		name.setText(file.getName());
		if (file.getLocalFile().isFile()) {
			icon.setImageResource(R.drawable.ic_file);
		} else {
			icon.setImageResource(R.drawable.ic_folder);
		}
		size.setVisibility(View.GONE);
		if (file.getLocalFile().isFile()) {
			if (file.getAccounts().isEmpty()) {
				modified.setText(R.string.prefs_synch_select_none);
			} else {
				StringBuilder sb = new StringBuilder();
				for (String account: file.getAccounts().keySet()) {
					if (sb.length() > 0) {
						sb.append(", ");
					}
					sb.append(account);
				}
				modified.setText(sb.toString());
			}
		} else {
			modified.setText("");
		}
		return view;
	}

}

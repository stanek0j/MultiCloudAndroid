package cz.zcu.kiv.multicloudandroid.display;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import cz.zcu.kiv.multicloud.utils.Utils;
import cz.zcu.kiv.multicloud.utils.Utils.UnitsFormat;
import cz.zcu.kiv.multicloudandroid.R;

/**
 * cz.zcu.kiv.multicloudandroid.display/AccountAdapter.java			<br /><br />
 *
 * Adapter for holding and displaying accounts.
 *
 * @author Jaromír Staněk
 * @version 1.0
 *
 */
public class AccountAdapter extends ArrayAdapter<Account> {

	/** Context. */
	private final Context context;

	/**
	 * Ctor with necessary parameters.
	 * @param context Context.
	 * @param resource Resource ID.
	 * @param objects List of accounts.
	 */
	public AccountAdapter(Context context, int resource, List<Account> objects) {
		super(context, resource, objects);
		this.context = context;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.account_item, parent, false);
		Account value = getItem(position);
		TextView name = (TextView) view.findViewById(R.id.textView_name);
		TextView cloud = (TextView) view.findViewById(R.id.textView_cloud);
		TextView total = (TextView) view.findViewById(R.id.textView_total);
		TextView quota = (TextView) view.findViewById(R.id.textView_quota);
		name.setText(value.getName());
		if (value.isListed()) {
			cloud.setText(value.getCloud() + " (" + context.getText(R.string.desc_listed) + ")");
		} else {
			cloud.setText(value.getCloud());
		}
		StringBuilder sb = new StringBuilder();
		if (value.isAuthorized()) {
			total.setText(context.getText(R.string.desc_total_space) + " " + Utils.formatSize(value.getTotalSpace(), UnitsFormat.BINARY));
			sb.append(context.getText(R.string.desc_free_used_space) + " ");
			sb.append(Utils.formatSize(value.getFreeSpace(), UnitsFormat.BINARY));
			sb.append(" / ");
			sb.append(Utils.formatSize(value.getUsedSpace(), UnitsFormat.BINARY));
		} else {
			if (value.getCloud() != null) {
				total.setText(R.string.desc_not_authorized);
			} else {
				total.setText(R.string.desc_add_account);
			}
		}
		quota.setText(sb.toString());
		return view;
	}

}

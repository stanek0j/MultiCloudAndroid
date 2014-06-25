package cz.zcu.kiv.multicloudandroid.display;

import java.util.List;

import android.content.Context;
import android.widget.ArrayAdapter;
import cz.zcu.kiv.multicloud.json.FileInfo;

public class ItemAdapter extends ArrayAdapter<FileInfo> {

	public ItemAdapter(Context context, int resource, List<FileInfo> objects) {
		super(context, resource);
	}

}

package cz.zcu.kiv.multicloudandroid.display;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;
import cz.zcu.kiv.multicloudandroid.R;

/**
 * cz.zcu.kiv.multicloudandroid.display/NumberPreference.java			<br /><br />
 *
 * Preference settings with dialog containing number picker.
 *
 * @author Jaromír Staněk
 * @version 1.0
 *
 */
public class NumberPreference extends DialogPreference {

	/** Default minimum value. */
	public static final int DEF_MIN = 1;
	/** Default maximum value. */
	public static final int DEF_MAX = 10;

	/** Minimum value. */
	private final int min;
	/** Maximum value. */
	private final int max;
	/** Default value. */
	private int def;
	/** Number picker. */
	private NumberPicker numPicker;

	/**
	 * Ctor with necessary parameters.
	 * @param context Context.
	 * @param attrs Attributes.
	 */
	public NumberPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setDialogLayoutResource(R.layout.number_picker_dialog);
		TypedArray numPrefs = context.obtainStyledAttributes(attrs, R.styleable.NumberPreference, 0, 0);
		min = numPrefs.getInt(R.styleable.NumberPreference_min, DEF_MIN);
		max = numPrefs.getInt(R.styleable.NumberPreference_max, DEF_MAX);
		numPrefs.recycle();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);
		numPicker = (NumberPicker) view.findViewById(R.id.numberPicker_num);
		numPicker.setMinValue(min);
		numPicker.setMaxValue(max);
		def = getSharedPreferences().getInt(getKey(), min);
		numPicker.setValue(def);
		numPicker.setWrapSelectorWheel(false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (positiveResult) {
			persistInt(numPicker.getValue());
		}
	}

}

package cz.zcu.kiv.multicloudandroid.display;

import android.content.Context;
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

	/** Minimum value. */
	public static final int MIN_VAL = 1;
	/** Maximum value. */
	public static final int MAX_VAL = 10;

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
		setDialogLayoutResource(R.layout.number_pref);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);
		numPicker = (NumberPicker) view.findViewById(R.id.numberPicker_num);
		numPicker.setMinValue(MIN_VAL);
		numPicker.setMaxValue(MAX_VAL);
		def = getSharedPreferences().getInt(getKey(), MIN_VAL);
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

package cs.umass.edu.myactivitiestoolkit.view.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;

import cs.umass.edu.myactivitiestoolkit.R;

/**
 * A boolean preference with an on/off switch widget. After API 24, you may (and should) use
 * {@link android.preference.SwitchPreference} instead.
 *
 * @author CS390MB
 */
public class SwitchPreference extends Preference {

    /**
     * the default preference value in the case that it is not defined in the XML attributes
     */
    private static final boolean DEFAULT_VALUE = false;

    /**
     * The toggle switch associated with the preference
     */
    private Switch toggleSwitch;

    /**
     * The default preference value.
     */
    private boolean defaultValue;

    public SwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        setWidgetLayoutResource(R.layout.switch_toggle_service);
        return super.onCreateView(parent);
    }

    @Override
    protected void onBindView(View rootView) {
        super.onBindView(rootView);

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        toggleSwitch = (Switch) rootView.findViewById(R.id.toggleSwitch);
        toggleSwitch.setChecked(preferences.getBoolean(getKey(), defaultValue));
        toggleSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean enabled) {
                preferences.edit().putBoolean(getKey(), enabled).apply();
            }
        });
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        defaultValue = a.getBoolean(index, DEFAULT_VALUE);
        return defaultValue;
    }

    /**
     * Retrieves the handle to the switch view
     * @return {@link Switch} object
     */
    public Switch getSwitch(){
        return toggleSwitch;
    }

}
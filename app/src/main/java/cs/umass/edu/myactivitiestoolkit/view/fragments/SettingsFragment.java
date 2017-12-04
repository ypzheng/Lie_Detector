package cs.umass.edu.myactivitiestoolkit.view.fragments;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import cs.umass.edu.myactivitiestoolkit.R;

/**
 * The Settings fragment allows the user to modify all shared applications preferences.
 * You will not be required to make any changes to this class.
 *
 * @author CS390MB
 */
public class SettingsFragment extends PreferenceFragment {
    @SuppressWarnings("unused")
    /** used for debugging purposes */
    private static final String TAG = SettingsFragment.class.getName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preference);
    }
}
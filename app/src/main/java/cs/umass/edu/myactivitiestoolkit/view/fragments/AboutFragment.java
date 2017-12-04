package cs.umass.edu.myactivitiestoolkit.view.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Locale;

import cs.umass.edu.myactivitiestoolkit.R;
import cs.umass.edu.myactivitiestoolkit.util.Metadata;

/**
 * Describes the application and displays the current version.
 *
 * @author Sean Noran
 */
public class AboutFragment extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false);
        TextView txtVersion = (TextView) view.findViewById(R.id.txtVersion);
        txtVersion.setText(String.format(Locale.getDefault(), getString(R.string.about_app_version), Metadata.getVersionName(getActivity())));

        return view;
    }
}
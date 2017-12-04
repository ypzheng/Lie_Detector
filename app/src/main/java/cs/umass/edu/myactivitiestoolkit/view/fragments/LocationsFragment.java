package cs.umass.edu.myactivitiestoolkit.view.fragments;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cs.umass.edu.myactivitiestoolkit.R;
import cs.umass.edu.myactivitiestoolkit.clustering.Cluster;
import cs.umass.edu.myactivitiestoolkit.clustering.ClusteringRequest;
import cs.umass.edu.myactivitiestoolkit.clustering.DBScan;
import cs.umass.edu.myactivitiestoolkit.constants.Constants;
import cs.umass.edu.myactivitiestoolkit.location.FastConvexHull;
import cs.umass.edu.myactivitiestoolkit.location.GPSLocation;
import cs.umass.edu.myactivitiestoolkit.location.LocationDAO;
import cs.umass.edu.myactivitiestoolkit.services.LocationService;
import cs.umass.edu.myactivitiestoolkit.services.AccelerometerService;
import cs.umass.edu.myactivitiestoolkit.services.ServiceManager;
import cs.umass.edu.myactivitiestoolkit.util.PermissionsUtil;
import edu.umass.cs.MHLClient.client.MessageReceiver;
import edu.umass.cs.MHLClient.client.MobileIOClient;

/**
 * Fragment which visualizes the stored locations along with their clusters and allows
 * the user to select a clustering algorithm and change its parameters. The locations
 * are visualized using Google Maps API.
 * <br><br>
 *
 * @author CS390MB
 *
 * @see GoogleMap
 * @see MapView
 * @see LocationService
 * @see Fragment
 */
public class LocationsFragment extends Fragment {

    @SuppressWarnings("unused")
    /** Used during debugging to identify logs by class */
    private static final String TAG = LocationsFragment.class.getName();

    /** Request code required for obtaining location permission. **/
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 4;

    /** The view which contains the {@link #map} **/
    MapView mapView;

    /** The map object. **/
    private GoogleMap map;

    /** The list of visual map markers representing saved locations. */
    private final List<Marker> locationMarkers;

    /** The list of visual map markers representing cluster centers. */
    private final List<Marker> clusterMarkers;

    /** Indicates whether map markers, excluding cluster centers, should be displayed. **/
    private boolean hideMarkers = false;

    /** The location services icon which functions as a button to toggle the {@link LocationService}. **/
    private View btnToggleLocationService;

    /** Allows the user to define the epsilon parameter for DBScan. **/
    private EditText txtEps;

    /** Allows the user to define the minimum points parameter for DBScan. **/
    private EditText txtMinPts;

    /** Allows the user to define the number of clusters for k-means clustering. **/
    private EditText txtKClusters;

    /** Reference to the service manager which communicates to the {@link LocationService}. **/
    private ServiceManager serviceManager;

    /** Responsible for communicating with the data collection server. */
    protected MobileIOClient client;

    /** The user ID required to authenticate the server connection. */
    protected String userID;

    /**
     * We listen for text input, so that we can decide how the UI is modified when the keyboard appears.
     */
    private final View.OnFocusChangeListener textLostFocusListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus){
            if(!hasFocus) {
                InputMethodManager imm =  (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        }
    };

    public LocationsFragment(){
        locationMarkers = new ArrayList<>();
        clusterMarkers = new ArrayList<>();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        serviceManager = ServiceManager.getInstance(getActivity());
        userID = getString(R.string.mobile_health_client_user_id);
        client = MobileIOClient.getInstance(getActivity(), userID);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_location, container, false);

        final RadioGroup clusterAlgorithmOpts = (RadioGroup) rootView.findViewById(R.id.radioGroupClusteringAlgorithm);
        final View parametersDBScan = rootView.findViewById(R.id.parameters_dbscan);
        final View parametersKMeans = rootView.findViewById(R.id.parameters_kmeans);
        clusterAlgorithmOpts.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                switch (i) {
                    case R.id.radioButtonDBScan:
                        parametersDBScan.setVisibility(View.VISIBLE);
                        parametersKMeans.setVisibility(View.INVISIBLE);
                        break;
                    case R.id.radioButtonKMeans:
                        parametersDBScan.setVisibility(View.INVISIBLE);
                        parametersKMeans.setVisibility(View.VISIBLE);
                        break;
                    case R.id.radioButtonMeanShift:
                        parametersDBScan.setVisibility(View.INVISIBLE);
                        parametersKMeans.setVisibility(View.INVISIBLE);
                        break;
                }
            }
        });

        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        txtEps = (EditText) rootView.findViewById(R.id.txtEps);
        txtEps.setOnFocusChangeListener(textLostFocusListener);

        txtKClusters = (EditText) rootView.findViewById(R.id.txtKClusters);
        txtKClusters.setOnFocusChangeListener(textLostFocusListener);

        txtMinPts = (EditText) rootView.findViewById(R.id.txtMinPts);
        txtMinPts.setOnFocusChangeListener(textLostFocusListener);

        mapView = (MapView) rootView.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        mapView.onResume(); // needed to get the map to display immediately

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
            System.gc();
        } catch (Exception e) {
            e.printStackTrace();
        }

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap mMap) {
                LocationsFragment.this.map = mMap;
            }
        });

        View btnUpdate = rootView.findViewById(R.id.btnUpdateMap);
        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GPSLocation[] locations = getSavedLocations();
                if (locations.length == 0){
                    Toast.makeText(getActivity(), "No locations to cluster.", Toast.LENGTH_LONG).show();
                    return;
                }
                //Place a marker at each point and also adds it to the global list of markers
                map.clear();
                locationMarkers.clear();
                if (!hideMarkers) {
                    for (GPSLocation loc : locations) {
                        Marker marker = map.addMarker(new MarkerOptions()
                                .position(new LatLng(loc.latitude, loc.longitude)) //sets the latitude & longitude
                                .title("At " + LocationDAO.getISOTimeString(loc.timestamp))); //display the time it occurred when clicked
                        locationMarkers.add(marker);
                    }
                }
                switch (clusterAlgorithmOpts.getCheckedRadioButtonId()){
                    case R.id.radioButtonDBScan:
                        float eps = Float.parseFloat(txtEps.getText().toString());
                        int minPts = Integer.parseInt(txtMinPts.getText().toString());
                        runDBScan(locations, eps, minPts);
                        break;
                    case R.id.radioButtonKMeans:
                        int k = Integer.parseInt(txtKClusters.getText().toString());
                        runKMeans(locations, k);
                        break;
                    case R.id.radioButtonMeanShift:
                        runMeanShift(locations);
                        break;
                }
                zoomInOnMarkers(100); // zoom to clusters automatically
            }
        });

        View btnSettings = rootView.findViewById(R.id.btnMapsSettings);
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popup = new PopupMenu(getActivity(), view);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.menu_maps, popup.getMenu());
                popup.show();
                popup.getMenu().getItem(0).setTitle(hideMarkers ? "Show Markers" : "Hide Markers");
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (item.getItemId() == R.id.action_hide_markers) {
                            hideMarkers = !hideMarkers;
                            for (Marker marker : locationMarkers){
                                marker.setVisible(!hideMarkers);
                            }
                            return true;
                        } else {
                            return false;
                        }
                    }
                });
            }
        });

        btnToggleLocationService = rootView.findViewById(R.id.btnToggleLocation);
        if (serviceManager.isServiceRunning(LocationService.class)) {
            btnToggleLocationService.setBackgroundResource(R.drawable.ic_location_on_black_48dp);
        } else {
            btnToggleLocationService.setBackgroundResource(R.drawable.ic_location_off_black_48dp);
        }
        btnToggleLocationService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!serviceManager.isServiceRunning(LocationService.class)) {
                    requestPermissions();
                }else{
                    serviceManager.stopSensorService(LocationService.class);
                }
            }
        });

        return rootView;
    }

    /**
     * When the fragment starts, register a {@link #receiver} to receive messages from the
     * {@link AccelerometerService}. The intent filter defines messages
     * we are interested in receiving. We would like to receive notifications and sensor data, so
     * we specify both {@link Constants.ACTION#BROADCAST_MESSAGE}
     * and {@link Constants.ACTION#BROADCAST_ACCELEROMETER_DATA}. We would also like to detect
     * step count updates, so include {@link Constants.ACTION#BROADCAST_ANDROID_STEP_COUNT},
     * {@link Constants.ACTION#BROADCAST_LOCAL_STEP_COUNT} and {@link Constants.ACTION#BROADCAST_SERVER_STEP_COUNT}
     */
    @Override
    public void onStart() {
        super.onStart();

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getActivity());
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION.BROADCAST_MESSAGE);
        broadcastManager.registerReceiver(receiver, filter);
    }

    /**
     * When the fragment stops, e.g. the user closes the application or opens a new activity,
     * then we should unregister the {@link #receiver}.
     */
    @Override
    public void onStop() {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getActivity());
        try {
            broadcastManager.unregisterReceiver(receiver);
        }catch (IllegalArgumentException e){
            e.printStackTrace();
        }
        super.onStop();
    }

    /**
     * Retrieves all locations saved in the local database.
     * @return a list of {@link GPSLocation}s.
     */
    private GPSLocation[] getSavedLocations(){
        LocationDAO dao = new LocationDAO(getActivity());
        try {
            dao.openRead();
            return dao.getAllLocations();
        } finally {
            dao.close();
        }
    }

    /**
     * Here you should draw clusters on the map. We have given you {@link #drawHullFromPoints(GPSLocation[], int)},
     * which draws a convex hull around the specified points, in the given color. For each cluster,
     * you should draw the convex hull in a unique color (it's OK if it's not unique after several
     * clusters, as long as we can distinguish clusters that are close or overlap). We provided you
     * with an array of colors you can index into. Make sure if you have more clusters than the size
     * of the list to handle an {@link ArrayIndexOutOfBoundsException}, e.g. by using the modulus
     * operator (%).
     * <br><br>
     * EXTRA CREDIT: You may optionally display the cluster centers as a marker. You may approximate
     * the geographic cluster center by averaging the latitudes and longitudes separately, or
     * you may go above and beyond and account for the spherical nature of the earth.
     * See <a href="http://www.geomidpoint.com/calculation.html">geomidpoint.com</a> for details.
     */
    private void drawClusters(final Collection<Cluster<GPSLocation>> clusters){
        final int[] colors = new int[]{Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.CYAN, Color.WHITE};
        // TODO: For each cluster, draw a convex hull around the points in a maximally distinct color
        int index = 0;
        for (Cluster<GPSLocation> cluster : clusters){
            drawHullFromPoints(cluster.getPoints().toArray(new GPSLocation[cluster.getPoints().size()]), colors[index++ % colors.length]);
            double sumOfLatitudes = 0, sumOfLongitudes = 0;
            for (int i = 0; i < cluster.getPoints().size(); i++){
                sumOfLatitudes += cluster.getPoints().get(i).latitude;
                sumOfLongitudes += cluster.getPoints().get(i).longitude;
            }
            GPSLocation loc = new GPSLocation(-1, sumOfLatitudes / cluster.getPoints().size(), sumOfLongitudes / cluster.getPoints().size(), -1);
            Marker marker = map.addMarker(new MarkerOptions()
                    .position(new LatLng(loc.latitude, loc.longitude)) //sets the latitude & longitude
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    .title("Cluster " + index)); //display the time it occurred when clicked
            clusterMarkers.add(marker);
        }
    }

    /**
     * Here you will call your DBScan algorithm, with the given parameters. Then
     * call {@link #drawClusters(Collection)} in order to visualize your output.
     * @param locations the list of locations to be clustered.
     * @param eps the neighborhood radius parameter.
     * @param minPts the minimum number of points in a neighborhood.
     */
    private void runDBScan(GPSLocation[] locations, float eps, int minPts){
        //TODO: Cluster the locations by calling DBScan.
        DBScan<GPSLocation> dbScan = new DBScan<>(eps, minPts);
        List<Cluster<GPSLocation>> clusters = dbScan.cluster(Arrays.asList(locations));
        drawClusters(clusters);
    }

    /**
     * Here you will request to cluster your n locations using k-means clustering. We have
     * registered a listener for you and have parsed the JSON string that comes back
     * from the server. The result is a list of n integers associating each location with
     * one of the k clusters. Generate a list of k clusters and then call {@link #drawClusters(Collection)}.
     *
     * @param locations the list of locations to be clustered.
     * @param k the number of clusters.
     */
    private void runKMeans(final GPSLocation[] locations, final int k){
        client.registerMessageReceiver(new MessageReceiver() {
            @Override
            protected void onMessageReceived(JSONObject json) {
                final Map<Integer, Cluster<GPSLocation>> clusters = new ArrayMap<>();
                try {
                    json = json.getJSONObject("data");
                    JSONArray indexes = json.getJSONArray("indexes");
                    for (int i = 0; i < indexes.length(); i++) {
                        int index = indexes.getInt(i);
                        if (clusters.containsKey(index)) {
                            clusters.get(index).addPoint(locations[i]);
                        }else{
                            Cluster<GPSLocation> c = new Cluster<>();
                            c.addPoint(locations[i]);
                            clusters.put(index, c);
                        }
                        //TODO: Using the index of each location, generate k clusters, then call drawClusters()
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            drawClusters(clusters.values());
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    client.unregisterMessageReceiver(this);
                }
            }
        });
        client.connect();
        client.sendSensorReading(new ClusteringRequest(userID, "", "", System.currentTimeMillis(), locations, "k_means", k));

        //TODO: Send the data off to the server.
        //TODO: Register a message receiver. In the onMessageReceived(String) method, decode the JSON object and update the map
        //TODO: Send a k-means clustering request, specifying the value of k
    }

    /**
     * Here you will request to cluster your n locations using mean-shift clustering.
     * The only difference between this and {@link #runKMeans(GPSLocation[], int)} with handling
     * the cluster indexes is that here we no longer know the number of clusters. Using a {@link List},
     * this shouldn't be a problem.
     *
     * @param locations the list of locations to be clustered.
     */
    private void runMeanShift(final GPSLocation[] locations){
        client.registerMessageReceiver(new MessageReceiver() {
            @Override
            protected void onMessageReceived(JSONObject json) {
                final Map<Integer, Cluster<GPSLocation>> clusters = new ArrayMap<>();
                try {
                    json = json.getJSONObject("data");
                    JSONArray indexes = json.getJSONArray("indexes");
                    for (int i = 0; i < indexes.length(); i++) {
                        int index = indexes.getInt(i);
                        if (clusters.containsKey(index)) {
                            clusters.get(index).addPoint(locations[i]);
                        }else{
                            Cluster<GPSLocation> c = new Cluster<>();
                            c.addPoint(locations[i]);
                            clusters.put(index, c);
                        }
                        //TODO: Using the index of each location, generate clusters, then call drawClusters()
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            drawClusters(clusters.values());
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    client.unregisterMessageReceiver(this);
                }
            }
        });
        client.connect();
        client.sendSensorReading(new ClusteringRequest(userID, "", "", System.currentTimeMillis(), locations, "mean_shift", -1));
    }

    /**
     * Zooms in as much as possible such that all markers are visible on the map
     * Thanks to andr at http://stackoverflow.com/questions/14828217/android-map-v2-zoom-to-show-all-the-markers
     * for this clean solution.
     * @param padding the number of pixels padding the ege of the map layout between markers
     */
    public void zoomInOnMarkers(int padding){
        if (locationMarkers.size() + clusterMarkers.size() == 0) return;
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Marker marker : locationMarkers) {
            builder.include(marker.getPosition());
        }
        for (Marker marker : clusterMarkers) {
            builder.include(marker.getPosition());
        }
        LatLngBounds bounds = builder.build();
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        map.animateCamera(cu);
    }

    /**
     * Draws a convex hull around a set of points - this will be great for visualizing clusters
     * @param locations the set of locations contained in the convex hull
     */
    private void drawHullFromPoints(GPSLocation[] locations, int color){
        if (locations.length <= 2) return;
        ArrayList<GPSLocation> hull = FastConvexHull.execute(locations);
        PolygonOptions options = new PolygonOptions();
        for(GPSLocation loc : hull){
            options.add(new LatLng(loc.latitude,loc.longitude));
        }
        options = options.strokeColor(color).fillColor(color);
        map.addPolygon(options); // draw a polygon
    }

    /**
     * The receiver listens for messages from the {@link AccelerometerService}, e.g. was the
     * service started/stopped, and updates the status views accordingly. It also
     * listens for sensor data and displays the sensor readings to the user.
     */
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                if (intent.getAction().equals(Constants.ACTION.BROADCAST_MESSAGE)){
                    int message = intent.getIntExtra(Constants.KEY.MESSAGE, -1);
                    if (message == Constants.MESSAGE.LOCATION_SERVICE_STARTED){
                        btnToggleLocationService.setBackgroundResource(R.drawable.ic_location_on_black_48dp);
                    } else if (message == Constants.MESSAGE.LOCATION_SERVICE_STOPPED){
                        btnToggleLocationService.setBackgroundResource(R.drawable.ic_location_off_black_48dp);
                    }
                }
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        System.gc();
        mapView.onLowMemory();
    }

    /**
     * Request permissions required for video recording. These include
     * {@link android.Manifest.permission#WRITE_EXTERNAL_STORAGE WRITE_EXTERNAL_STORAGE},
     * and {@link android.Manifest.permission#CAMERA CAMERA}. If audio is enabled, then
     * the {@link android.Manifest.permission#RECORD_AUDIO RECORD_AUDIO} permission is
     * additionally required.
     */
    @TargetApi(Build.VERSION_CODES.M)
    public void requestPermissions(){
        String[] permissions = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};

        if (!PermissionsUtil.hasPermissionsGranted(getActivity(), permissions)) {
            requestPermissions(permissions, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        onLocationPermissionGranted();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                //If the request is cancelled, the result array is empty.
                if (grantResults.length == 0) return;

                for (int i = 0; i < permissions.length; i++){
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED){
                        switch (permissions[i]) {
                            case Manifest.permission.ACCESS_COARSE_LOCATION:
                                //TODO: Show status
                                return;
                            case Manifest.permission.ACCESS_FINE_LOCATION:
                                //TODO: Show status
                                return;
                            default:
                                return;
                        }
                    }
                }
                onLocationPermissionGranted();
            }
        }
    }

    /**
     * Called when location permissions have been granted by the user.
     */
    public void onLocationPermissionGranted(){
        serviceManager.startSensorService(LocationService.class);
    }
}
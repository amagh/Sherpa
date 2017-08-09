package project.hikerguide.utilities;

import android.content.Context;
import android.widget.Toast;

import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.mapboxsdk.offline.OfflineRegion;
import com.mapbox.mapboxsdk.offline.OfflineRegionError;
import com.mapbox.mapboxsdk.offline.OfflineRegionStatus;
import com.mapbox.mapboxsdk.offline.OfflineTilePyramidRegionDefinition;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.UnsupportedEncodingException;

import project.hikerguide.R;
import project.hikerguide.data.GuideContract;
import project.hikerguide.models.datamodels.Guide;
import timber.log.Timber;

/**
 * Created by Alvin on 7/27/2017.
 */

public class MapUtils {
    // ** Constants ** //
    private static final String JSON_CHARSET = "UTF-8";

    /**
     * Adds a PolylineOptions representing the track from a .gpx file to a MapboxMap
     *
     * @param gpxFile      .gpx file containing a track to be plotted on the MapboxMap
     * @param mapboxMap    MapboxMap where the PolylineOptions will be drawn on
     */
    public static void addMapOptionsToMap(File gpxFile, final MapboxMap mapboxMap) {

        // Parse the GPX File to get the Mapbox PolyLine and Marker
        GpxUtils.getMapboxOptions(gpxFile, new GpxUtils.MapboxOptionsListener() {
            @Override
            public void onOptionReady(MarkerOptions markerOptions, PolylineOptions polylineOptions) {
                // Set the Marker for the start of the trail
                mapboxMap.addMarker(markerOptions);

                // Set the Polyline representing the trail
                mapboxMap.addPolyline(polylineOptions
                        .width(3));

                // Position the camera such that it fits all the points in the PolyLine
                CameraPosition position = mapboxMap.getCameraForLatLngBounds(
                        new LatLngBounds.Builder().includes(polylineOptions.getPoints()).build(),
                        new int[] {50, 100, 50, 100});

                mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position));
            }
        });
    }

    /**
     * Saves Mapbox tiles corresponding to a Guide so that it can be viewed while offline
     *
     * @param context     Interface to global Context
     * @param guide       Guide whose region is to be saved
     * @param callback    Callback to notify of download progress and completion
     */
    public static void saveMapboxOffline(final Context context, final Guide guide,
                                         final MapboxDownloadCallback callback) {

        // Setup the variables required for downloading Mapbox tiles
        GpxUtils.getMapboxOptions(guide.getGpxFile(), new GpxUtils.MapboxOptionsListener() {
            @Override
            public void onOptionReady(MarkerOptions markerOptions, PolylineOptions polylineOptions) {

                // Get the bounds to download based on the bounds required for the Polyline
                LatLngBounds bounds = new LatLngBounds.Builder()
                        .includes(polylineOptions.getPoints())
                        .build();

                // Setup the definition, passing in the LatLngBounds
                OfflineTilePyramidRegionDefinition definition = new OfflineTilePyramidRegionDefinition(
                        context.getString(R.string.outdoors_style),
                        bounds,
                        13, // Min zoom
                        16, // Max zoom
                        context.getResources().getDisplayMetrics().density);

                // Metadata for the offline region
                byte[] metadata;

                try {

                    JSONObject jsonObject = new JSONObject();

                    // Use the trail as the name of the region
                    jsonObject.put(GuideContract.GuideEntry.TRAIL_NAME, guide.trailName);

                    // Add FirebaseId of the Guide
                    jsonObject.put(GuideContract.GuideEntry.FIREBASE_ID, guide.firebaseId);

                    // Convert to String and then to byte array
                    String json = jsonObject.toString();
                    metadata = json.getBytes(JSON_CHARSET);
                } catch (JSONException | UnsupportedEncodingException e) {
                    e.printStackTrace();

                    metadata = null;
                }

                // Check that metadata was successfully generated
                if (metadata != null) {
                    saveMapboxTiles(context, definition, metadata, callback);
                }
            }
        });
    }

    /**
     * Saves Mapbox Tiles for a definition and metadata
     *
     * @param context       Interface to global Context
     * @param definition    Definition for the region to be downloaded
     * @param metadata      Metadata for the region to be downloaded
     * @param callback      Callback to notify of download progress and completion
     */
    private static void saveMapboxTiles(final Context context,
                                        OfflineTilePyramidRegionDefinition definition,
                                        byte[] metadata, final MapboxDownloadCallback callback) {

        // Init the OfflineManager
        OfflineManager manager = OfflineManager.getInstance(context);

        // Create the offline region
        manager.createOfflineRegion(definition, metadata,
                new OfflineManager.CreateOfflineRegionCallback() {

                    @Override
                    public void onCreate(OfflineRegion offlineRegion) {

                        // Set download to Active
                        offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE);

                        // Init the obsever to keep track of the download progress
                        offlineRegion.setObserver(new OfflineRegion.OfflineRegionObserver() {

                            @Override
                            public void onStatusChanged(OfflineRegionStatus status) {

                                // Calculate the percentage progress
                                double progress = status.getRequiredResourceCount() >= 0
                                        ? (100.0 * status.getCompletedResourceCount() / status.getRequiredResourceCount())
                                        : 0.0;

                                if (status.isComplete()) {

                                    // Download complete
                                    callback.onDownloadComplete();

                                } else if (status.isRequiredResourceCountPrecise()) {

                                    // Update ProgressBar
                                    callback.onUpdateProgress(progress);
                                }
                            }

                            @Override
                            public void onError(OfflineRegionError error) {

                                // Show a Toast to the user to let them know of the failure to save
                                Toast.makeText(context,
                                        context.getString(R.string.error_downloading_mapbox),
                                        Toast.LENGTH_LONG)
                                        .show();

                                Timber.e(error.getMessage());
                            }

                            @Override
                            public void mapboxTileCountLimitExceeded(long limit) {

                                // Notify user of reaching max tile count
                                Toast.makeText(context,
                                        context.getString(R.string.error_tile_count_exceeded),
                                        Toast.LENGTH_LONG)
                                        .show();

                                Timber.i("Exceeded TileCountLimit");
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        Timber.e("Error: " + error);
                    }
                });

    }

    /**
     * Deletes Mapbox tiles that have been saved offline for a given Guide
     *
     * @param context    Interface to global Context
     * @param guide      Guide whose associated Mapbox tiles should be deleted
     */
    public static void deleteMapboxOffline(final Context context, final Guide guide,
                                           final MapboxDeleteCallback callback) {

        // Init the OfflineManager
        OfflineManager manager = OfflineManager.getInstance(context);

        // List the downloaded regions
        manager.listOfflineRegions(new OfflineManager.ListOfflineRegionsCallback() {
            @Override
            public void onList(OfflineRegion[] offlineRegions) {

                // Iterate through the regions and check the metadata for a match
                for (OfflineRegion region : offlineRegions) {
                    try {

                        // Convert the metadata to a json String
                        String json = new String(region.getMetadata(), JSON_CHARSET);

                        // Convert the String to a JSONObject
                        JSONObject jsonObject = new JSONObject(json);

                        // Check if the firebaseId in the metadata matches the Guide
                        String firebaseId = jsonObject.getString(GuideContract.GuideEntry.FIREBASE_ID);

                        if (guide.firebaseId.equals(firebaseId)) {

                            // Delete the tiles for the OfflineRegion
                            deleteTiles(context, region, callback);
                        }
                    } catch (UnsupportedEncodingException | JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onError(String error) {
                Timber.e("Error: " + error);
            }
        });
    }

    /**
     * Deletes an OfflineRegion for Mapbox
     *
     * @param context          Interface to global Context
     * @param offlineRegion    OfflineRegion to be deleted
     */
    private static void deleteTiles(final Context context, OfflineRegion offlineRegion,
                                    final MapboxDeleteCallback callback) {

        // Delete the region
        offlineRegion.delete(new OfflineRegion.OfflineRegionDeleteCallback() {
            @Override
            public void onDelete() {

                // Trigger callback
                callback.onComplete();

                Timber.d("Map deleted!");
            }

            @Override
            public void onError(String error) {

                // Show a Toast to the user to let them know of the failure to delete
                Toast.makeText(context,
                        context.getString(R.string.error_downloading_mapbox),
                        Toast.LENGTH_LONG)
                        .show();

                Timber.e("Error: " + error);
            }
        });
    }

    public interface MapboxDownloadCallback {
        void onDownloadComplete();
        void onUpdateProgress(double progress);
    }

    public interface MapboxDeleteCallback {
        void onComplete();
    }
}

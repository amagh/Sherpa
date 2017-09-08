package project.sherpa.utilities;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.List;

import project.sherpa.models.datamodels.Area;
import project.sherpa.models.datamodels.PlaceModel;

/**
 * Created by Alvin on 8/17/2017.
 */

public class GooglePlacesApiUtils {

    /**
     * Initializes the GoogleApiClient to use the Places API
     *
     * @param context                     Interface to global Context
     * @param connectionCallbacks         Callback Interface for connection status to GoogleApi
     * @param connectionFailedListener    Callback Interface for a failed connection to GoogleApi
     * @return A GoogleApiClient set to access Google Places API
     */
    public static GoogleApiClient initGoogleApiClient(@NonNull Context context,
                                                      @NonNull GoogleApiClient.ConnectionCallbacks connectionCallbacks,
                                                      @NonNull GoogleApiClient.OnConnectionFailedListener connectionFailedListener) {

        GoogleApiClient client = new GoogleApiClient.Builder(context)
                .addApi(Places.GEO_DATA_API)
                .addConnectionCallbacks(connectionCallbacks)
                .addOnConnectionFailedListener(connectionFailedListener)
                .build();

        client.connect();

        return client;
    }

    /**
     * Queries Google Places API for a given query and returns the results in the form of a List of
     * PlaceModels
     *
     * @param googleApiClient    GoogleApiClient that will be used to access the API
     * @param query              Query for Google Places API
     * @param callback           Callback Interface that will be used to pass the results back to
     *                           the calling Object
     */
    public static void queryGooglePlaces(GoogleApiClient googleApiClient, String query,
                                         final GooglePlacesQueryListener callback) {

        // Max LatLngBounds possible
        LatLngBounds bounds = new LatLngBounds(
                new LatLng(-85, 180),   // Max NE bound
                new LatLng(85, -180));  // Max SW bound

        // Query Places API
        PendingResult<AutocompletePredictionBuffer> result = Places.GeoDataApi
                .getAutocompletePredictions(googleApiClient, query, bounds, null);

        result.setResultCallback(new ResultCallback<AutocompletePredictionBuffer>() {
            @Override
            public void onResult(@NonNull AutocompletePredictionBuffer autocompletePredictions) {
                if (autocompletePredictions.getStatus().isSuccess() && autocompletePredictions.getCount() > 0) {
                    List<PlaceModel> placeList = new ArrayList<>();

                    for (AutocompletePrediction prediction : autocompletePredictions) {

                        // Create a new PlaceModel from the List and populate it with info
                        PlaceModel placeModel = new PlaceModel();
                        placeModel.primaryText = prediction.getPrimaryText(null).toString();
                        placeModel.secondaryText = prediction.getSecondaryText(null).toString();
                        placeModel.placeId = prediction.getPlaceId();

                        placeList.add(placeModel);
                    }

                    autocompletePredictions.release();

                    // Pass the results to the calling Object
                    callback.onQueryReady(placeList);
                }
            }
        });
    }

    /**
     * Retrieves a Mapbox LatLng that can be used to position the camera from a PlaceId
     *
     * @param googleApiClient    GoogleApiClient that will be used to access the API
     * @param placeId            PlaceId to query for coordinates
     * @param callback           Callback Interface that will be used to pass teh results back to
     *                           the calling Object
     */
    public static void getMapboxLatLngForPlaceId(GoogleApiClient googleApiClient, String placeId, final CoordinateListener callback) {

        // Query Places API to get the Place
        Places.GeoDataApi.getPlaceById(googleApiClient, placeId).setResultCallback(new ResultCallback<PlaceBuffer>() {
            @Override
            public void onResult(@NonNull PlaceBuffer places) {

                // Check to ensure results are valid
                if (places.getStatus().isSuccess() && places.getCount() > 0) {

                    // Get the Place, generate the LatLng and pass the coordinates to the calling
                    // Object
                    Place place = places.get(0);
                    LatLng location = place.getLatLng();
                    callback.onCoordinatesReady(new com.mapbox.mapboxsdk.geometry.LatLng(
                            location.latitude,
                            location.longitude));

                    // Release the resources
                    places.release();
                }
            }
        });
    }

    /**
     * Converts a PlaceModel to an Area
     *
     * @param googleApiClient    For connecting to Google Places API to pull the required
     *                           information to create the Area
     * @param placeModel         The PlaceModel to be converted to an Area
     * @param listener           Listener for informing the calling Object that the conversion has
     *                           completed
     */
    public static void convertPlaceModelToArea(@NonNull GoogleApiClient googleApiClient,
                                               PlaceModel placeModel, final ConversionListener listener) {

        // Get the detailed information by querying Google Places with the PlaceId
        Places.GeoDataApi.getPlaceById(googleApiClient, placeModel.placeId).setResultCallback(new ResultCallback<PlaceBuffer>() {
            @Override
            public void onResult(@NonNull PlaceBuffer places) {

                if (places.getStatus().isSuccess() && places.getCount() > 0) {

                    // Init a new Area
                    Area area = new Area();

                    // Get the Place
                    Place place = places.get(0);

                    // Add the details to the new Area and pass it to the Listener
                    LatLng latLng   = place.getLatLng();
                    area.name       = place.getName().toString();
                    area.location   = place.getAddress().toString();

                    area.latitude = latLng.latitude;
                    area.longitude = latLng.longitude;

                    listener.onPlaceConvertedToArea(area);

                    places.release();
                }
            }
        });
    }

    /**
     * Callback Interface for passing search results from a Google Places query to the calling
     * Object
     **/
    public interface GooglePlacesQueryListener {
        void onQueryReady(List<PlaceModel> placeModelList);
    }

    /**
     * Callback Interface for passing coordinates to the calling Object
     */
    public interface CoordinateListener {
        void onCoordinatesReady(com.mapbox.mapboxsdk.geometry.LatLng latLng);
    }

    /**
     * Listener for passing a converted PlaceModel to Area to the receiver
     */
    public interface ConversionListener {
        void onPlaceConvertedToArea(Area area);
    }
}

package project.sherpa.services.firebaseservice;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import project.sherpa.models.datamodels.abstractmodels.BaseModel;
import timber.log.Timber;

/**
 * A background service that is used as a server for data from Firebase Database. By keeping the
 * connections to the data alive in this service, it allows for fewer calls to Firebase Database
 * and for fresh data to be immediately be delivered to any Activities/Fragments that call for it.
 *
 * Created by Alvin on 9/30/2017.
 */

public class FirebaseProviderService extends Service {

    // ** Constants ** //
    private static final int DELAY = 1000;

    // ** Member Variables ** //
    private IBinder mBinder = new FirebaseProviderBinder();

    private Map<String, SmartValueEventListener> mSmartListenerMap = new HashMap<>();
    private Map<SmartValueEventListener, List<ModelChangeListener>> mModelListenerMap = new HashMap<>();

    private Map<String, SmartQueryValueListener> mSmartQueryMap = new HashMap<>();
    private Map<SmartQueryValueListener, List<QueryChangeListener>> mQueryListenerMap = new HashMap<>();

    private Handler mCleanUpHandler = new Handler();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class FirebaseProviderBinder extends Binder {
        public FirebaseProviderService getService() {
            return FirebaseProviderService.this;
        }
    }

    /**
     * Registers a ModelChangeListener to a SmartValueEventListener. If the SmartValueEventListener
     * corresponding to the data the the ModelChangeListener is requesting does not exist, it
     * starts a new SmartValueEventListener for it and attaches the ModelChangeListener to it.
     *
     * @param modelChangeListener    The ModelChangeListener to be registered
     */
    public synchronized void registerModelChangeListener(ModelChangeListener modelChangeListener) {

        // Check to see if a corresponding SmartValueEventListener exists for the data being
        // requested
        String firebaseId = modelChangeListener.getFirebaseId();
        SmartValueEventListener smartValueEventListener;

        if (!mSmartListenerMap.keySet().contains(firebaseId)) {

            Timber.d("Starting SmartValueEventListener for: " + firebaseId);

            // No corresponding SmartValueEventListener. Start a new one
            smartValueEventListener = new SmartValueEventListener(
                    modelChangeListener.getType(),
                    modelChangeListener.getFirebaseId()) {

                @Override
                public void onModelChange(BaseModel model) {
                    // To be deprecated
                }

                @Override
                public void onModelChange() {

                    // Notify any registered ModelChangeListeners that its data has changed so its
                    // UI can be updated to reflect the changes
                    for (ModelChangeListener listedModelChangeListener : mModelListenerMap.get(this)) {
                        listedModelChangeListener.updateModel();
                    }
                }
            };

            // Start listening to changes in the data on Firebase
            smartValueEventListener.start();

            // Add the SmartValueEventListener to the Map
            mSmartListenerMap.put(firebaseId, smartValueEventListener);

            if (mModelListenerMap.get(smartValueEventListener) == null) {
                mModelListenerMap.put(smartValueEventListener, new ArrayList<ModelChangeListener>());
            }

            mModelListenerMap.get(smartValueEventListener).add(modelChangeListener);

        } else {

            // SmartValueEventListener corresponding to the sought data exists, register the
            // ModelChangeListener to it
            smartValueEventListener = mSmartListenerMap.get(firebaseId);

            if (mModelListenerMap.get(smartValueEventListener) == null) {
                mModelListenerMap.put(smartValueEventListener, new ArrayList<ModelChangeListener>());
            }

            if (!mModelListenerMap.get(smartValueEventListener).contains(modelChangeListener)) {
                mModelListenerMap.get(smartValueEventListener).add(modelChangeListener);
            }

            // Immediately deliver the existing data to the ModelChangeListener
            if (smartValueEventListener.getModel() != null) modelChangeListener.updateModel();
        }
    }

    /**
     * Unregisters a ModelChangeListener from a corresponding SmartValueEventListener
     *
     * @param modelChangeListener    The ModelChangeListener to be unregistered
     */
    public synchronized void unregisterModelChangeListener(ModelChangeListener modelChangeListener) {

        SmartValueEventListener smartValueEventListener = mSmartListenerMap.get(modelChangeListener.getFirebaseId());
        List<ModelChangeListener> modelChangeListenerList = mModelListenerMap.get(smartValueEventListener);

        if (modelChangeListenerList != null && modelChangeListenerList.contains(modelChangeListener)) {
            modelChangeListenerList.remove(modelChangeListener);
        }

        cleanUp();
    }

    /**
     * Registers a QueryChangeListener to being observing for changes in the corresponding
     * SmartQueryValueListener. If the SmartQueryValueListener for the QueryChangeListener does not
     * exist, then it is started.
     *
     * @param queryChangeListener    The QueryChangeListener to register for changes in data
     */
    public synchronized void registerQueryChangeListener(QueryChangeListener queryChangeListener) {

        // Get the corresponding SmartQueryValueListener from mSmartQueryMap
        SmartQueryValueListener smartQueryValueListener = mSmartQueryMap.get(queryChangeListener.getQueryKey());

        if (smartQueryValueListener == null) {

            Timber.d("Starting SmartQueryValueListener for: " + queryChangeListener.getQueryKey());

            // SmartQueryValueListener is not in mSmartQueryMap, init it and put it in
            smartQueryValueListener = new SmartQueryValueListener(
                    queryChangeListener.getType(),
                    queryChangeListener.getQuery()) {

                @Override
                public void onQueryChanged(BaseModel[] models) {
                    for (QueryChangeListener listener : mQueryListenerMap.get(this)) {
                        listener.updateModels(models);
                    }
                }
            };

            // Start listening for changes
            smartQueryValueListener.start();
            mSmartQueryMap.put(queryChangeListener.getQueryKey(), smartQueryValueListener);
        }

        // Init the List of attached QueryChangeListeners for the SmartQueryValueListener if it
        // hasn't been initialized yet
        if (mQueryListenerMap.get(smartQueryValueListener) == null) {
            mQueryListenerMap.put(smartQueryValueListener, new ArrayList<QueryChangeListener>());
        }

        // Add the QueryChangeListener to the List of attached QueryChangeListeners
        if (!mQueryListenerMap.get(smartQueryValueListener).contains(queryChangeListener)) {
            mQueryListenerMap.get(smartQueryValueListener).add(queryChangeListener);
        }

        // Return the data returned by the Query if it isn't null
        if (smartQueryValueListener.getData() != null) {
            queryChangeListener.updateModels(smartQueryValueListener.getData());
        }
    }

    public void unregisterQueryChangeListener(QueryChangeListener queryChangeListener) {

        SmartQueryValueListener smartQueryValueListener = mSmartQueryMap.get(queryChangeListener.getQueryKey());
        List<QueryChangeListener> queryListenerList = mQueryListenerMap.get(smartQueryValueListener);

        if (queryListenerList != null && queryListenerList.contains(queryChangeListener)) {
            mQueryListenerMap.get(smartQueryValueListener).remove(queryChangeListener);
        }

        cleanUp();
    }

    /**
     * Cleans up mSmartListenerMap by stopping and removing any SmartValueEventListeners that no
     * longer have any ModelChangeListeners attached to it.
     */
    private void cleanUp() {
        mCleanUpHandler.removeCallbacksAndMessages(null);

        mCleanUpHandler.postDelayed(new Runnable() {
            @Override
            public void run() {

                // Iterate through each SmartValueEventListener and stop any that don't have any
                // registered ModelChangeListeners attached to it
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                List<String> keyList = new ArrayList<>(mSmartListenerMap.keySet());

                for (int i = mSmartListenerMap.size() - 1; i >= 0; i--) {

                    // Do not close the ValueEventListener for the logged in user
                    String firebaseId = keyList.get(i);
                    if (user != null && firebaseId.equals(user.getUid())) continue;

                    SmartValueEventListener listener = mSmartListenerMap.get(firebaseId);

                    if (mModelListenerMap.get(listener).size() == 0) {

                        // SmartValueEventListener does not have any ModelChangeListeners attached.
                        // Stop it and remove the SmartValueEventListener from the Map
                        listener.stop();
                        mModelListenerMap.remove(listener);
                        mSmartListenerMap.remove(firebaseId);

                        Timber.d("Stopped SmartValueEventListener: " + firebaseId);
                    }
                }

                // Set the keyList to the keySet for mSmartQueryMap
                keyList = new ArrayList<>(mSmartQueryMap.keySet());

                // Iterate through each SmartQueryValueListener and stop any that don't have any
                // registered QueryChangeListeners attached to it
                for (int i = mSmartQueryMap.size() - 1; i >= 0; i--) {

                    String queryKey = keyList.get(i);
                    SmartQueryValueListener listener = mSmartQueryMap.get(queryKey);

                    if (mQueryListenerMap.get(listener).size() == 0) {

                        // SmartQueryValueListener does not have any QueryChangeListeners attached.
                        // Stop it and remove the SmartQueryValueListener from the Map
                        listener.stop();
                        mQueryListenerMap.remove(listener);
                        mSmartQueryMap.remove(queryKey);

                        Timber.d("Stopped SmartQueryValueListener: " + queryKey);
                    }
                }
            }
        }, DELAY);
    }
}

package project.sherpa.utilities;

import java.lang.ref.SoftReference;
import java.util.HashMap;

import project.sherpa.models.datamodels.Author;
import project.sherpa.models.datamodels.Section;
import project.sherpa.models.datamodels.abstractmodels.BaseModel;

/**
 * Created by Alvin on 8/26/2017.
 */

public class DataCache {

    // ** Member Variables ** //
    private static DataCache sDataCache = new DataCache();
    private HashMap<String, SoftReference<BaseModel>> mDataMap;
    private HashMap<String, SoftReference<Section[]>> mArrayMap;

    private DataCache() {
        mDataMap = new HashMap<>();
        mArrayMap = new HashMap<>();
    }

    /**
     * Enforces Singleton Pattern.
     *
     * @return The Singleton instance of the DataCache
     */
    public static DataCache getInstance() {
        return sDataCache;
    }

    /**
     * Stores a data model in the DataCache
     *
     * @param model    The data model to be cached
     */
    public void store(BaseModel model) {

        // Create a SoftReference to the data
        SoftReference<BaseModel> dataReference = new SoftReference<>(model);

        lock(model);

        // Check to see if model being added to the cache is already in the cache
        if (get(model.firebaseId) != null) {

            // Update the model with the new values instead of replacing it
            BaseModel cachedModel = get(model.firebaseId);
            cachedModel.updateValues(model);

        } else {
            // Add the SoftReference to the DataCache
            mDataMap.put(model.firebaseId, dataReference);
        }
    }

    /**
     * Helper method to keep the Object cached for a short period of time to allow the cache to
     * be able to pass Objects from one Activity to another
     *
     * @param model    Model to be locked
     */
    private void lock(BaseModel model) {

        // Start a new Thread for a short period of time. Due to the hard-reference to the Object
        // in the signature, this will keep the Object from being GC'd for however long the Thread
        // is alive.
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        t.start();
    }

    /**
     * Stores an Array of Sections in the DataCache
     *
     * @param sections    The Array of Sections to be cached
     */
    public void store(Section[] sections) {

        // Get a reference to one of the Sections for the guide's firebaseId
        Section section = sections[0];

        // Create a SoftReference to the Array
        SoftReference<Section[]> arrayReference = new SoftReference<>(sections);

        // Cache the Array
        mArrayMap.put(section.guideId, arrayReference);
    }

    /**
     * Retrieves a data model from the cache
     *
     * @param firebaseId    The FirebaseId used as the key to the data model from the cache
     * @return The data model associated with the FirebaseId
     */
    public BaseModel get(String firebaseId) {

        // Attempt to retrieve the data from cache
        SoftReference<BaseModel> dataReference = mDataMap.get(firebaseId);

        // Check to ensure the data is valid
        if (dataReference != null && dataReference.get() != null) {

            // Return the data from the SoftReference
            return dataReference.get();
        } else {

            // Does not exist in the cache. Return null
            return null;
        }
    }

    /**
     * Retrieves an Array of Sections from the cache
     *
     * @param guideId    The FirebaseId of the Guide associated with the Sections used as the key
     * @return An Array of Sections corresponding to the FirebaseId of the Guide they belong to
     */
    public Section[] getSections(String guideId) {

        // Attempt to retrieve the data from cache
        SoftReference<Section[]> sectionReference = mArrayMap.get(guideId);

        // Check to ensure the data is valid
        if (sectionReference != null && sectionReference.get() != null) {

            // Return the data from the SoftReference
            return sectionReference.get();
        } else {

            // Does not exist in the cache. Return null
            return null;
        }
    }
}

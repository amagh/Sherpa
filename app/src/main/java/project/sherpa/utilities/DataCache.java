package project.sherpa.utilities;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.HashMap;

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

        // Create a WeakReference to the data
        SoftReference<BaseModel> dataReference = new SoftReference<>(model);

        lock(model);

        // Add the WeakReference to the DataCache
        mDataMap.put(model.firebaseId, dataReference);
    }

    private void lock(BaseModel model) {
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

        // Create a WeakReference to the Array
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

            // Return the data from the WeakReference
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

            // Return the data from the WeakReference
            return sectionReference.get();
        } else {

            // Does not exist in the cache. Return null
            return null;
        }
    }
}

package project.hikerguide.utilities;

import com.google.firebase.database.DataSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import project.hikerguide.data.GuideDatabase;
import project.hikerguide.firebasedatabase.FirebaseProvider;
import project.hikerguide.models.Area;
import project.hikerguide.models.Author;
import project.hikerguide.models.BaseModel;
import project.hikerguide.models.Guide;
import project.hikerguide.models.Section;
import project.hikerguide.models.Trail;

import static junit.framework.Assert.assertNotNull;
import static project.hikerguide.firebasedatabase.FirebaseProvider.FirebaseType.AREA;
import static project.hikerguide.firebasedatabase.FirebaseProvider.FirebaseType.AUTHOR;
import static project.hikerguide.firebasedatabase.FirebaseProvider.FirebaseType.GUIDE;
import static project.hikerguide.firebasedatabase.FirebaseProvider.FirebaseType.SECTION;
import static project.hikerguide.firebasedatabase.FirebaseProvider.FirebaseType.TRAIL;

/**
 * Created by Alvin on 7/18/2017.
 */

public class FirebaseProviderUtils {

    /**
     * Helper method for getting the directory of a type
     *
     * @param type    FirebaseType
     * @return The directory that corresponds to the type
     */
    public static String getDirectoryFromType(@FirebaseProvider.FirebaseType int type) {
        switch (type) {
            case GUIDE:
                return GuideDatabase.GUIDES;

            case TRAIL:
                return GuideDatabase.TRAILS;

            case AUTHOR:
                return GuideDatabase.AUTHORS;

            case SECTION:
                return GuideDatabase.SECTIONS;

            case AREA:
                return GuideDatabase.AREAS;

            default: throw new UnsupportedOperationException("Unknown Firebase type " + type);
        }
    }

    /**
     * Helper method for getting the directory corresponding to the class of a BaseModel
     *
     * @param model    The BaseModel extended Class
     * @return The directory corresponding to the Class of the BaseModel parameter
     */
    public static String getDirectoryFromModel(BaseModel model) {

        // Resolve the directory based on the Class
        if (model instanceof Guide) {
            return GuideDatabase.GUIDES;
        } else if (model instanceof Trail) {
            return GuideDatabase.TRAILS;
        } else if (model instanceof Author) {
            return GuideDatabase.AUTHORS;
        } else if (model instanceof Section) {
            return GuideDatabase.SECTIONS;
        } else if (model instanceof Area) {
            return GuideDatabase.AREAS;
        } else {
            throw new UnsupportedOperationException("Unknown model:" + model.getClass());
        }
    }

    /**
     * Retrieves an Array of BaseModels from a DataSnapShot containing children of those types
     *
     * @param type            The FirebaseType. This will correspond to the type of BaseModel that
     *                        will be returned
     * @param dataSnapshot    The DataSnapShot containing children of the FirebaseType
     * @return An Array of BaseModels corresponding to the FirebaseType parameter
     */
    public static BaseModel[] getModelsFromSnapshot(@FirebaseProvider.FirebaseType int type, DataSnapshot dataSnapshot) {
        // Initialize the List that will store all the BaseModels created from the DataSnapshots
        List<BaseModel> modelList = new ArrayList<>();

        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
            // Cast the data to the correct BaseModel based on the type
            BaseModel model = getModelFromSnapshot(type, snapshot);

            // Add it to the List to be returned by the listener
            modelList.add(model);
        }

        // Create a new Array to return
        BaseModel[] models;

        switch (type) {
            case GUIDE:
                models = new Guide[modelList.size()];
                break;

            case TRAIL:
                models = new Trail[modelList.size()];
                break;

            case AUTHOR:
                models = new Author[modelList.size()];
                break;

            case SECTION:
                models = new Section[modelList.size()];
                break;

            case AREA:
                models = new Area[modelList.size()];
                break;

            default: throw new UnsupportedOperationException("Unknown Firebase type " + type);
        }

//        for (int i = 0; i < models.length; i++) {
//            models[i] = modelList.get(i);
//            System.out.println(models[i].getClass());
//        }

        // Copy the data to the created Array
        return modelList.toArray(models);
    }

    /**
     * Converts a DataSnapshot to a single BaseModel
     *
     * @param type            The FirebaseType corresponding to the BaseModel to be returned
     * @param dataSnapshot    The DataSnapshot describing a BaseModel
     * @return A BaseModel with the information contained within the DataSnapshot
     */
    public static BaseModel getModelFromSnapshot(@FirebaseProvider.FirebaseType int type, DataSnapshot dataSnapshot) {
        BaseModel model;

        switch (type) {
            case GUIDE:
                model = dataSnapshot.getValue(Guide.class);
                break;

            case TRAIL:
                model = dataSnapshot.getValue(Trail.class);
                break;

            case AUTHOR:
                model = dataSnapshot.getValue(Author.class);
                break;

            case SECTION:
                model = dataSnapshot.getValue(Section.class);
                break;

            case AREA:
                model = dataSnapshot.getValue(Area.class);
                break;

            default: throw new UnsupportedOperationException("Unknown Firebase type " + type);
        }

        // Set the model's firebaseId
        assertNotNull(model);
        model.firebaseId = dataSnapshot.getKey();

        return model;
    }
}

package project.hikerguide;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.firebase.geofire.GeoLocation;
import com.google.firebase.database.DatabaseError;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import project.hikerguide.files.GpxFile;
import project.hikerguide.files.ImageFile;
import project.hikerguide.firebasedatabase.DatabaseProvider;
import project.hikerguide.firebasestorage.StorageProvider;
import project.hikerguide.models.Area;
import project.hikerguide.models.Author;
import project.hikerguide.models.Guide;
import project.hikerguide.models.Section;
import project.hikerguide.models.Trail;
import project.hikerguide.models.abstractmodels.BaseModel;
import project.hikerguide.models.abstractmodels.BaseModelWithImage;
import project.hikerguide.utilities.SaveUtils;

import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;
import static project.hikerguide.firebasedatabase.DatabaseProvider.FirebaseType.AREA;
import static project.hikerguide.firebasedatabase.DatabaseProvider.FirebaseType.AUTHOR;
import static project.hikerguide.firebasedatabase.DatabaseProvider.FirebaseType.GUIDE;
import static project.hikerguide.firebasedatabase.DatabaseProvider.FirebaseType.TRAIL;

/**
 * Tests that storage and database are properly linked
 */
@RunWith(AndroidJUnit4.class)
public class FirebaseTest {
    // ** Constants ** //

    // ** Member Variables ** //
    private Context mContext;
    private DatabaseProvider mDatabase;
    private StorageProvider mStorage;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
        mDatabase = DatabaseProvider.getInstance();
        mStorage = StorageProvider.getInstance();
    }

    @Test
    public void testSaveNewGuide() {
        // Generate data models to insert into Firebase
        final Area area = TestUtilities.getArea();
        final Author author = TestUtilities.getAuthor(mContext);
        final Trail trail = TestUtilities.getTrail();
        final Guide guide = TestUtilities.getGuide(mContext);
        Section[] sections = TestUtilities.getSections(mContext);

        // Insert the author into the database and their ImageFile into the Storage
        mDatabase.insertRecord(author);
        mStorage.uploadFile(author.getImageFile());

        // Insert the rest of the data models and their images into the database/storage
        SaveUtils.saveGuide(area, author, trail, guide, sections);

        // Check to ensure the GeoQuery returns the inserted guide
        mDatabase.geoQuery(new GeoLocation(guide.latitude, guide.longitude), 5.0, new DatabaseProvider.GeofireListener() {
            @Override
            public void onKeyEntered(String guideId) {
                String errorWrongId = "Id of the Guide retrieved does not match the Id of the guide added";
                assertEquals(errorWrongId, guide.firebaseId, guideId);
            }

            @Override
            public void onFailure(DatabaseError databaseError) {

            }
        });

        // Create an Array of Types and Models to iterate through
        int[] types = new int[] {AREA, AUTHOR, TRAIL, GUIDE};
        final BaseModel[] models = new BaseModel[] {area, author, trail, guide};

        // Validate the database models were properly inserted
        for (int i = 0; i < types.length; i++) {
            final BaseModel expected = models[i];
            mDatabase.getRecord(types[i], expected.firebaseId, new DatabaseProvider.FirebaseSingleListener() {
                @Override
                public void onDataReady(BaseModel model) {
                    TestUtilities.validateModelValues(expected, model);
                }

                @Override
                public void onFailure(DatabaseError databaseError) {

                }
            });
        }

        // Validate all the sections
        for (final Section section : sections) {
            mDatabase.getRecord(DatabaseProvider.FirebaseType.SECTION, section.firebaseId, new DatabaseProvider.FirebaseSingleListener() {
                @Override
                public void onDataReady(BaseModel model) {
                    TestUtilities.validateModelValues(section, model);
                }

                @Override
                public void onFailure(DatabaseError databaseError) {

                }
            });
        }

        // Check to ensure the uploaded GpxFile exists
        String errorGpxFileDoesNotExist = "GPX file for guide cannot be found on the Firebase Storage";
        assertNotNull(errorGpxFileDoesNotExist, mStorage.getDownloadUrl(guide.getGpxFile()));

        // Check to ensure the uploaded ImageFiles exists
        String errorImageFileDoesNotExist = "Image file for %s (%s) cannot be found on Firebase Storage";

        BaseModelWithImage[] modelsWithImages = new BaseModelWithImage[] {author, guide};

        for (BaseModelWithImage model : modelsWithImages) {
            assertNotNull(String.format(errorImageFileDoesNotExist, model.getClass(), model.firebaseId), mStorage.getDownloadUrl(model.getImageFile()));
        }

        for (Section section : sections) {
            if (section.hasImage) {
                assertNotNull(String.format(errorImageFileDoesNotExist, section.getClass(), section.firebaseId), mStorage.getDownloadUrl(section.getImageFile()));
            }
        }

        // Clean up
        mDatabase.deleteAllRecords();
        mStorage.deleteFile(guide.getGpxFile());

        for (BaseModelWithImage model : modelsWithImages) {
            mStorage.deleteFile(model.getImageFile());
        }

        for (Section section : sections) {
            if (section.hasImage) {
                mStorage.deleteFile(section.getImageFile());
            }
        }
    }

    @Test
    public void testDownloadFilesForGuide() {
        // Generate data models to insert
        final Area area = TestUtilities.getArea();
        final Author author = TestUtilities.getAuthor(mContext);
        final Trail trail = TestUtilities.getTrail();
        final Guide guide = TestUtilities.getGuide(mContext);
        Section[] sections = TestUtilities.getSections(mContext);

        // Insert data into the database and storage
        mDatabase.insertRecord(author);
        mStorage.uploadFile(author.getImageFile());

        SaveUtils.saveGuide(area, author, trail, guide, sections);

        // Check to ensure that a new Guide can be properly created from the database record
        final Guide returnedGuide = new Guide();
        mDatabase.geoQuery(new GeoLocation(guide.latitude, guide.longitude), 6, new DatabaseProvider.GeofireListener() {
            @Override
            public void onKeyEntered(String guideId) {
                returnedGuide.firebaseId = guideId;
            }

            @Override
            public void onFailure(DatabaseError databaseError) {

            }
        });

        // Wait to ensure the query returns a Guide prior to moving on
        while (returnedGuide.firebaseId == null) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Generate Gpx and Image Files on the Internal Storage
        GpxFile gpxFile = returnedGuide.generateGpxFileForDownload(mContext);
        ImageFile imageFile = returnedGuide.generateImageFileForDownload(mContext);

        // Download the respective Files from Storage
        String errorDownloading = "Unable to download the File. Check logs for details.";
        assertTrue(errorDownloading, mStorage.downloadFile(gpxFile));
        assertTrue(errorDownloading, mStorage.downloadFile(imageFile));

        // Check that the file sizes of the downloaded files match the uploaded files
        String errorFileSizeMisMatch = "Downloaded file does not match the uploaded file.";
        assertEquals(errorFileSizeMisMatch, guide.getGpxFile().length(), gpxFile.length());
        assertEquals(errorFileSizeMisMatch, guide.getImageFile().length(), imageFile.length());

        // Delete the downloaded Files
        gpxFile.delete();
        imageFile.delete();

        // Query the Database for all the Sections for the Guide
        final List<Section> sectionList = new ArrayList<>();
        mDatabase.getSectionsForGuide(returnedGuide, new DatabaseProvider.FirebaseListener() {
            @Override
            public void onDataReady(BaseModel[] models) {
                assertThat(models.length, not(0));

                for (Section section : (Section[]) models) {
                    sectionList.add(section);
                }
            }

            @Override
            public void onFailure(DatabaseError databaseError) {
                assertNull(databaseError);
            }
        });

        // Wait to ensure that the query returns all the Sections
        while(sectionList.size() == 0) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Download and check the file sizes of the images associated with each Section
        for (Section returnedSection : sectionList) {
            if (!returnedSection.hasImage) {
                continue;
            }

            imageFile = returnedSection.generateImageFileForDownload(mContext);
            System.out.println(imageFile.firebaseId + " - " + imageFile.getAbsolutePath());
            assertTrue(errorDownloading, mStorage.downloadFile(imageFile));

            for (Section section : sections) {
                if (section.firebaseId.equals(returnedSection.firebaseId)) {
                    assertEquals(errorFileSizeMisMatch, section.getImageFile().length(), imageFile.length());
                }
            }

            // Delete the ImageFiles from the storage and device
            mStorage.deleteFile(returnedSection.getImageFile());
            imageFile.delete();
        }

        // Clean up
        mDatabase.deleteAllRecords();
        mStorage.deleteFile(returnedGuide.getGpxFile());
        mStorage.deleteFile(returnedGuide.getImageFile());
        mStorage.deleteFile(author.getImageFile());
    }
}

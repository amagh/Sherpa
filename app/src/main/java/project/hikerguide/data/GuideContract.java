package project.hikerguide.data;

import net.simonvt.schematic.annotation.AutoIncrement;
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.ForeignKeyConstraint;
import net.simonvt.schematic.annotation.NotNull;
import net.simonvt.schematic.annotation.PrimaryKey;
import net.simonvt.schematic.annotation.References;

/**
 * Defines the tables and columns to be created for the database
 */

public class GuideContract {
    // Order of insertion into Firebase Database: AUTHOR = AREA > TRAIL > GUIDE > SECTION

    public interface GuideEntry {
        @DataType(DataType.Type.INTEGER) @PrimaryKey @AutoIncrement String _ID      = "_id";
        @DataType(DataType.Type.TEXT) String FIREBASE_ID                            = "firebaseId";

        @DataType(DataType.Type.TEXT)
        @References(table = GuideDatabase.TRAILS, column = TrailEntry.FIREBASE_ID)
        String TRAIL_ID                                                             = "trailId";

        @DataType(DataType.Type.TEXT) String TRAIL_NAME                             = "trailName";

        @DataType(DataType.Type.TEXT)
        @References(table = GuideDatabase.AUTHORS, column = AuthorEntry.FIREBASE_ID)
        String AUTHOR_ID                                                            = "authorId";

        @DataType(DataType.Type.TEXT) String AUTHOR_NAME                            = "authorName";
        @DataType(DataType.Type.TEXT) String DATE_ADDED                             = "dateAdded";
        @DataType(DataType.Type.REAL) String RATING                                 = "rating";
        @DataType(DataType.Type.INTEGER) String REVIEWS                             = "reviews";

        @DataType(DataType.Type.REAL) String LATITUDE                               = "latitude";
        @DataType(DataType.Type.REAL) String LONGITUDE                              = "longitude";
        @DataType(DataType.Type.REAL) String DISTANCE                               = "distance";
        @DataType(DataType.Type.REAL) String ELEVATION                              = "elevation";

        @DataType(DataType.Type.INTEGER) String DIFFICULTY                          = "difficulty";
        @DataType(DataType.Type.TEXT) String IMAGE_URI                              = "imageUri";
        @DataType(DataType.Type.TEXT) String GPX_URI                                = "gpxUri";
    }

    public interface TrailEntry {
        @DataType(DataType.Type.INTEGER) @PrimaryKey @AutoIncrement String _ID      = "_id";
        @DataType(DataType.Type.TEXT) @NotNull String FIREBASE_ID                   = "firebaseId";

        @DataType(DataType.Type.TEXT)
        @References(table = GuideDatabase.AREAS, column = AreaEntry.FIREBASE_ID)
        @NotNull String AREA_ID                                                     = "areaId";

        @DataType(DataType.Type.TEXT) @NotNull String NAME                          = "name";
        @DataType(DataType.Type.TEXT) @NotNull String LOWER_CASE_NAME               = "lowerCaseName";
        @DataType(DataType.Type.TEXT) String NOTES                                  = "notes";
    }

    public interface AuthorEntry {
        @DataType(DataType.Type.INTEGER) @PrimaryKey @AutoIncrement String _ID      = "_id";
        @DataType(DataType.Type.TEXT) @NotNull String FIREBASE_ID                   = "firebaseId";
        @DataType(DataType.Type.TEXT) @NotNull String NAME                          = "name";
        @DataType(DataType.Type.TEXT) @NotNull String LOWER_CASE_NAME               = "lowerCaseName";
        @DataType(DataType.Type.TEXT) String DESCRIPTION                            = "description";
        @DataType(DataType.Type.INTEGER) String SCORE                               = "score";
        @DataType(DataType.Type.TEXT) String IMAGE_URI                              = "imageUri";
    }

    public interface SectionEntry {
        @DataType(DataType.Type.INTEGER) @PrimaryKey @AutoIncrement String _ID      = "_id";
        @DataType(DataType.Type.TEXT) String FIREBASE_ID                            = "firebaseId";

        @DataType(DataType.Type.TEXT)
        @References(table = GuideDatabase.GUIDES, column = GuideEntry.FIREBASE_ID)
        @NotNull String GUIDE_ID                                                    = "guideId";
        
        @DataType(DataType.Type.INTEGER) @NotNull String SECTION                    = "section";
        @DataType(DataType.Type.TEXT) String CONTENT                                = "content";
        @DataType(DataType.Type.TEXT) String IMAGE_URI                              = "imageUri";
    }

    public interface AreaEntry {
        @DataType(DataType.Type.INTEGER) @PrimaryKey @AutoIncrement String _ID      = "_id";
        @DataType(DataType.Type.TEXT) @NotNull String FIREBASE_ID                   = "firebaseId";
        @DataType(DataType.Type.TEXT) @NotNull String NAME                          = "name";
        @DataType(DataType.Type.TEXT) @NotNull String LOWER_CASE_NAME               = "lowerCaseName";
        @DataType(DataType.Type.REAL) String LATITUDE                               = "latitude";
        @DataType(DataType.Type.REAL) String LONGITUDE                              = "longitude";
        @DataType(DataType.Type.TEXT) @NotNull String STATE                         = "state";
    }
}

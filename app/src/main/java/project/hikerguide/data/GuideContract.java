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
        @DataType(DataType.Type.TEXT) String FIREBASE_ID                            = "firebase_id";

        @DataType(DataType.Type.INTEGER)
        @References(table = GuideDatabase.TRAILS, column = TrailEntry.FIREBASE_ID)
        @NotNull String TRAIL_ID                                                    = "trail_id";

        @DataType(DataType.Type.INTEGER)
        @References(table = GuideDatabase.AUTHORS, column = AuthorEntry.FIREBASE_ID)
        @NotNull String AUTHOR_ID                                                   = "author_id";

        @DataType(DataType.Type.TEXT) @NotNull String DATE_ADDED                    = "date_added";
        @DataType(DataType.Type.REAL) String RATING                                 = "rating";
        @DataType(DataType.Type.INTEGER) String REVIEWS                             = "reviews";
//        @DataType(DataType.Type.TEXT) @NotNull String GPX                           = "gpx";
        @DataType(DataType.Type.REAL) @NotNull String LATITUDE                      = "latitude";
        @DataType(DataType.Type.REAL) @NotNull String LONGITUDE                     = "longitude";
        @DataType(DataType.Type.INTEGER) @NotNull String HAS_IMAGE                  = "has_image";
    }

    public interface TrailEntry {
        @DataType(DataType.Type.INTEGER) @PrimaryKey @AutoIncrement String _ID      = "_id";
        @DataType(DataType.Type.TEXT) String FIREBASE_ID                            = "firebase_id";

        @DataType(DataType.Type.INTEGER)
        @References(table = GuideDatabase.AUTHORS, column = AreaEntry.FIREBASE_ID)
        @NotNull String AREA_ID                                                     = "area_id";

        @DataType(DataType.Type.TEXT) @NotNull String NAME                          = "name";
        @DataType(DataType.Type.TEXT) String NOTES                                  = "notes";
    }

    public interface AuthorEntry {
        @DataType(DataType.Type.INTEGER) @PrimaryKey @AutoIncrement String _ID      = "_id";
        @DataType(DataType.Type.TEXT) String FIREBASE_ID                            = "firebase_id";
        @DataType(DataType.Type.TEXT) @NotNull String NAME                          = "name";
        @DataType(DataType.Type.INTEGER) @NotNull String HAS_IMAGE                  = "has_image";
        @DataType(DataType.Type.INTEGER) String SCORE                               = "score";
    }

    public interface SectionEntry {
        @DataType(DataType.Type.INTEGER) @PrimaryKey @AutoIncrement String _ID      = "_id";
        @DataType(DataType.Type.TEXT) String FIREBASE_ID                            = "firebase_id";

        @DataType(DataType.Type.INTEGER)
        @References(table = GuideDatabase.GUIDES, column = GuideEntry.FIREBASE_ID)
        @NotNull String GUIDE_ID                                                    = "guide_id";
        
        @DataType(DataType.Type.INTEGER) @NotNull String SECTION                    = "section";
        @DataType(DataType.Type.TEXT) @NotNull String CONTENT                       = "content";
        @DataType(DataType.Type.INTEGER) @NotNull String HAS_IMAGE                  = "has_image";
    }

    public interface AreaEntry {
        @DataType(DataType.Type.INTEGER) @PrimaryKey @AutoIncrement String _ID      = "_id";
        @DataType(DataType.Type.TEXT) String FIREBASE_ID                            = "firebase_id";
        @DataType(DataType.Type.TEXT) @NotNull String NAME                          = "name";
    }
}

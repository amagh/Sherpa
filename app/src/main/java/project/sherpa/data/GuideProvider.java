package project.sherpa.data;

import android.net.Uri;

import net.simonvt.schematic.annotation.ContentProvider;
import net.simonvt.schematic.annotation.ContentUri;
import net.simonvt.schematic.annotation.InexactContentUri;
import net.simonvt.schematic.annotation.TableEndpoint;

/**
 * ContentProvider framework to be used by Schematic to generate the ContentProvider
 */

@ContentProvider(
        authority = GuideProvider.AUTHORITY,
        database = GuideDatabase.class)
public class GuideProvider {
    // ** Constants ** //
    public static final String AUTHORITY = "project.sherpa.data";
    static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    /**
     * Builds a URI from the BASE_CONTENT_URI by appending paths to it
     *
     * @param paths    Array of paths to append
     * @return URI with the paths appended to the BASE_CONTENT_URI
     */
    private static Uri buildUri(String... paths) {
        // Get a reference to the Uri.Builder
        Uri.Builder builder = BASE_CONTENT_URI.buildUpon();

        // For each path in the parameters, append the path to the BASE_CONTENT_URI
        for (String path : paths) {
            builder.appendPath(path);
        }

        // Build and return the URI
        return builder.build();
    }

    public static String getIdFromUri(Uri uri) {
        return uri.getLastPathSegment();
    }

    /**
     * Defines the paths used to access the data in the database
     */
    interface Path {
        String GUIDES       = "guides";
        String TRAILS       = "trails";
        String AUTHORS      = "authors";
        String SECTIONS     = "sections";
        String AREAS        = "areas";
    }

    /**
     * URIs for accessing guide data
     */
    @TableEndpoint(table = GuideDatabase.GUIDES)
    public static class Guides {

        @ContentUri(
                path = Path.GUIDES,
                type = "vnd.android.cursor.dir/guides",
                defaultSort = GuideContract.GuideEntry.DATE_ADDED + " DESC")
        public static final Uri CONTENT_URI = buildUri(Path.GUIDES);

        @InexactContentUri(
                path = Path.GUIDES + "/*",
                name = "GUIDE_DETAILS",
                type = "vnd.android.cursor.item/guides",
                whereColumn = GuideDatabase.GUIDES + "." + GuideContract.GuideEntry.FIREBASE_ID,
                pathSegment = 1,
                join = "JOIN " + GuideDatabase.TRAILS + " ON " +
                        GuideDatabase.GUIDES + "." + GuideContract.GuideEntry.TRAIL_ID + " = "  +
                        GuideDatabase.TRAILS + "." + GuideContract.TrailEntry.FIREBASE_ID               +
                        " JOIN " + GuideDatabase.AUTHORS + " ON "                               +
                        GuideDatabase.GUIDES + "." + GuideContract.GuideEntry.AUTHOR_ID + " = " +
                        GuideDatabase.AUTHORS + "." + GuideContract.AuthorEntry.FIREBASE_ID             +
                        " JOIN " + GuideDatabase.SECTIONS + " ON "                              +
                        GuideDatabase.GUIDES + "." + GuideContract.GuideEntry.FIREBASE_ID + " = "       +
                        GuideDatabase.SECTIONS + "." + GuideContract.SectionEntry.GUIDE_ID
        )
        public static Uri withId(String firebaseId) {
            return CONTENT_URI.buildUpon().appendPath(firebaseId).build();
        }

        @InexactContentUri(
                path = Path.GUIDES + "/" + Path.TRAILS + "/#",
                name = "GUIDES_FOR_TRAIL",
                type = "vnd.android.cursor.dir/guides",
                whereColumn = GuideContract.GuideEntry.TRAIL_ID,
                pathSegment = 2)
        public static Uri forTrail(long id) {
            return CONTENT_URI.buildUpon()
                    .appendPath(Path.TRAILS)
                    .appendPath(Long.toString(id))
                    .build();
        }

        @InexactContentUri(
                path = Path.GUIDES + "/" + Path.AUTHORS + "/#",
                name = "GUIDE_WITH_AUTHOR_ID",
                type = "vnd.android.cursor.dir/guides",
                whereColumn = GuideContract.GuideEntry.AUTHOR_ID,
                pathSegment = 2)
        public static Uri fromAuthor(long id) {
            return CONTENT_URI.buildUpon()
                    .appendPath(Path.AUTHORS)
                    .appendPath(Long.toString(id))
                    .build();
        }
    }

    /**
     * URIs for accessing trail data
     */
    @TableEndpoint(table = GuideDatabase.TRAILS)
    public static class Trails {

        @ContentUri(
                path = Path.TRAILS,
                type = "vnd.android.cursor.dir/trails")
        public static final Uri CONTENT_URI = buildUri(Path.TRAILS);

        @InexactContentUri(
                path = Path.TRAILS + "/" + Path.AREAS + "/#",
                name = "PATHS_FOR_AREA",
                type = "vnd.android.cursor.dir/trails",
                whereColumn = GuideContract.TrailEntry.AREA_ID,
                pathSegment = 2)
        public static Uri forArea(long id) {
            return CONTENT_URI.buildUpon()
                    .appendPath(Path.AREAS)
                    .appendPath(Long.toString(id))
                    .build();
        }
    }

    /**
     * URIs for accessing author data
     */
    @TableEndpoint(table = GuideDatabase.AUTHORS)
    public static class Authors {

        @ContentUri(
                path = Path.AUTHORS,
                type = "vnd.android.cursor.dir/authors")
        public static final Uri CONTENT_URI = buildUri(Path.AUTHORS);
    }

    /**
     * URIs for accessing section data
     */
    @TableEndpoint(table = GuideDatabase.SECTIONS)
    public static class Sections {

        @ContentUri(
                path = Path.SECTIONS,
                type = "vnd.android.cursor.dir/sections")
        public static final Uri CONTENT_URI = buildUri(Path.SECTIONS);

        @InexactContentUri(
                path = Path.SECTIONS + "/" + Path.GUIDES + "/#",
                name = "SECTIONS_FOR_GUIDE",
                type = "vnd.android.cursor.dir/sections",
                whereColumn = GuideContract.SectionEntry.GUIDE_ID,
                pathSegment = 3)
        public static Uri forGuide(long id) {
            return CONTENT_URI.buildUpon()
                    .appendPath(Path.GUIDES)
                    .appendPath(Long.toString(id))
                    .build();
        }
    }

    /**
     * URIs for accessing area data
     */
    @TableEndpoint(table = GuideDatabase.AREAS)
    public static class Areas {

        @ContentUri(
                path = Path.AREAS,
                type = "vnd.android.cursor.dir/areas")
        public static final Uri CONTENT_URI = buildUri(Path.AREAS);
    }
}

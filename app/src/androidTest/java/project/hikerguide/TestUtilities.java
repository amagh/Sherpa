package project.hikerguide;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Set;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import project.hikerguide.data.GuideContract;
import project.hikerguide.models.datamodels.Area;
import project.hikerguide.models.datamodels.Author;
import project.hikerguide.files.ImageFile;
import project.hikerguide.models.datamodels.abstractmodels.BaseModel;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.models.datamodels.Section;
import project.hikerguide.models.datamodels.Trail;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;

/**
 * Created by Alvin on 7/17/2017.
 */

class TestUtilities {

    /**
     * Checks values from a Cursor to match the values in the ContentValues
     *
     * @param cursor    Cursor containing database values
     * @param values    ContentValues from which the database values are to be matched against
     */
    static void validateCursorValues(Cursor cursor, ContentValues values) {
        String nullCursorError = "Cursor is null. Is ContentProvider registered in " +
                "AndroidManifest.xml?";
        assertNotNull(nullCursorError, cursor);

        Set<String> keySet = values.keySet();

        for (String key : keySet) {
            int columnIndex = cursor.getColumnIndex(key);

            String columnNotFoundError = key + " column not found";
            assertFalse(columnNotFoundError, columnIndex == -1);

            String expectedValue = values.getAsString(key);
            String cursorValue = cursor.getString(columnIndex);

            String matchError = "Expected value: " + expectedValue +
                    " does not match actual value: " + cursorValue;

            assertEquals(matchError, expectedValue, cursorValue);
        }
    }

    /**
     * Checks that the values of the data model returned has the same values as the data model that
     * was inserted in the Firebase Database
     *
     * @param expected    The data model inserted into the Firebase Database
     * @param returned    The data model that was returned from the Firebase Database
     */
    static void validateModelValues(BaseModel expected, BaseModel returned) {
        // Get all the Fields of the data model
        Field[] fields = expected.getClass().getFields();

        // Iterate through and ensure each Field value is equal
        for (Field field : fields) {
            String errorValueDifferent = "Values in the returned data model (" + returned.firebaseId + ") do not match the values inserted (" + expected.firebaseId + "). " + field.toString();
            try {
                System.out.println("Field: " + field.toString() + " | Expected: " + field.get(expected) + " | Actual: " + field.get(returned));
                assertEquals(errorValueDifferent, field.get(expected), field.get(returned));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    // Getters for dummy values to insert into the database

    static ContentValues getAreaValues() {
        ContentValues values = new ContentValues();
        values.put(GuideContract.AreaEntry.NAME, "Yosemite");

        return values;
    }

    static ContentValues getAuthorValues() {
        ContentValues values = new ContentValues();
        values.put(GuideContract.AuthorEntry.NAME, "John Muir");
        values.put(GuideContract.AuthorEntry.HAS_IMAGE, 1);
        values.put(GuideContract.AuthorEntry.SCORE, 100);

        return values;
    }

    static ContentValues getTrailValues() {
        ContentValues values = new ContentValues();
        values.put(GuideContract.TrailEntry.AREA_ID, 1);
        values.put(GuideContract.TrailEntry.NAME, "Four Mile Trail");
        values.put(GuideContract.TrailEntry.NOTES, "Temporarily Closed");

        return values;
    }

    static ContentValues getGuideValues() {
        ContentValues values = new ContentValues();
        values.put(GuideContract.GuideEntry.AUTHOR_ID, 1);
        values.put(GuideContract.GuideEntry.TRAIL_ID, 1);
        values.put(GuideContract.GuideEntry.DATE_ADDED, System.currentTimeMillis());
        values.put(GuideContract.GuideEntry.HAS_IMAGE, 1);
        values.put(GuideContract.GuideEntry.LATITUDE, 37.734);
        values.put(GuideContract.GuideEntry.LONGITUDE, -119.602);
        values.put(GuideContract.GuideEntry.RATING, 5);
        values.put(GuideContract.GuideEntry.REVIEWS, 1);

        return values;
    }

    static ContentValues getSectionValues() {
        ContentValues values = new ContentValues();
        values.put(GuideContract.SectionEntry.GUIDE_ID, 1);
        values.put(GuideContract.SectionEntry.SECTION, 1);
        values.put(GuideContract.SectionEntry.CONTENT, "Description of the hike");
        values.put(GuideContract.SectionEntry.HAS_IMAGE, 1);

        return values;
    }

    static Guide getGuide(Context context) {
        Guide guide = new Guide(1, System.currentTimeMillis(), 37.734, 119.602);
        guide.setImageUri(downloadFile(context, "http://www.norcalhiker.com/wp-content/uploads/2014/09/09.jpg"));
        guide.setGpxUri(downloadFile(context, "http://www.norcalhiker.com/maps/FourMileTrail.gpx"));

        return guide;
    }

    static Guide[] getGuides() {
        Guide guide1 = new Guide(1, "a", "1", System.currentTimeMillis(), 37.734, -119.602);
        Guide guide2 = new Guide(2, "b", "1", System.currentTimeMillis(), 37.734, -119.602);
        Guide guide3 = new Guide(3, "c", "1", System.currentTimeMillis(), 37.734, -119.602);
        Guide guide4 = new Guide(4, "d", "1", System.currentTimeMillis(), 37.734, -119.602);
        Guide guide5 = new Guide(5, "f", "1", System.currentTimeMillis(), 37.734, -119.602);
        Guide guide6 = new Guide(6, "g", "1", System.currentTimeMillis(), 37.734, -119.602);
        Guide guide7 = new Guide(7, "h", "1", System.currentTimeMillis(), 37.734, -119.602);
        Guide guide8 = new Guide(8, "i", "1", System.currentTimeMillis(), 37.734, -119.602);
        Guide guide9 = new Guide(9, "j", "1", System.currentTimeMillis(), 37.734, -119.602);

        Guide[] guides = {guide1, guide2, guide3, guide4, guide5, guide6, guide7, guide8, guide9};
        return guides;
    }

    static Trail getTrail() {
        return new Trail(1, "Four Mile Trail", null);
    }

    static Author getAuthor(Context context) {
        Author author = new Author(1, "John Muir");
        author.setImageUri(downloadFile(context, "http://www.norcalhiker.com/JMT/Images/170.jpg"));
        return author;
    }

    static Section getSection() {
        return new Section(1, "1", 1, "Description of hike");
    }

    public static Section[] getSections(Context context) {
        Section section1 = new Section(1, 1, "The Four Mile Trail (actually 4.8 miles) is one of Yosemite Valley’s most strenuous trails. It climbs up to Glacier Point – an ascent of nearly 3300-ft in just under five miles. After the grueling uphill slog, hikers are rewarded with unparalleled views of Half Dome and the rest of Yosemite Valley. Glacier Point can also be reached by car or shuttle, but the view feels much more rewarding when you walk. We hiked this on a rainy Saturday in late September. Though summer is over, there were still plenty of people out and about. You can’t expect much solitude on this trail, but you can look forward to a great workout and some good people watching!");
        Section section2 = new Section(2, 2, "Red Tape:  The park entrance fee is $20.  If you plan to stay in the Valley, reserve accommodations well in advance!  There is limited parking at the trailhead, but it is along the El Capitan shuttle route.  The shuttle runs from June through October, 9am to 6pm. When you enter the park, they will hand you a fairly decent map at the kiosk. The trail is on this map. There are no junctions, so it’s basically impossible to get lost.");
        Section section3 = new Section(3, 3, "Trail Description: The hike described here is actually a repeat of the beginning portion of the 20-mile John Muir hike we completed last October. On that trip we hiked half this trail in the dark, then continued on towards Sentinel Dome, the Panorama Trail, Liberty Cap and the Mist Trail. This time around we decided to “take it easy” and enjoy the Four Mile Trail on its own.");
        Section section4 = new Section(4, 4, "Getting ready to hike! Saturday morning in Curry Village.");
        section4.setImageUri(downloadFile(context, "http://www.norcalhiker.com/wp-content/uploads/2014/09/01.jpg"));
        Section section5 = new Section(5, 5, "We slept late, ate breakfast at the Curry Village Coffee Corner, then drove the loop around Yosemite Valley to reach the trailhead.  The trail has a huge sign, so it’s difficult to miss.  There was no parking (at 10:00 am on a Saturday), so we drove a little further up the road and parked at the Swinging Bridge Picnic Area.");
        Section section6 = new Section(6, 6, "Somewhere near the beginning of the trail…");
        section6.setImageUri(downloadFile(context, "http://www.norcalhiker.com/wp-content/uploads/2014/09/02.jpg"));
        Section section7 = new Section(7, 7, "We started up the trail and were quickly passed by several people walking much faster.  I think we are just slow, but I will blame our heavy packs.  We carried lots of food, a couple of beers, warm clothing and rain gear.  All proved to be very useful and/or delicious.  Many of the other hikers were wearing shorts (or jeans!!) and not carrying jackets.  It was cold and rainy – seems like poor preparation (but who am I to judge?)");
        Section section8 = new Section(8, 8, "Moss on all the rocks.");
        section8.setImageUri(downloadFile(context, "http://www.norcalhiker.com/wp-content/uploads/2014/09/03.jpg"));
        Section section9 = new Section(9, 9, "The trail climbs at a steady, relentless grade with many switchbacks.  Soon we had glimpses of El Cap and the Valley through the trees.  About halfway up, we started catching views of Half Dome.  We also began encountering quite a few hikers coming down the trail. Some had taken the shuttle up and were now hiking down.  Others had already hiked to the top and were on their way back.  I’m not used to this because we are usually the first people to hit a trail!");

        return new Section[] {section1, section2, section3, section4, section5, section6, section7, section8, section9};
    }

    static File downloadFile(Context context, String imageUrl) {
        // Use OkHttp to get a connection to the URL
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(imageUrl)
                .build();

        try {
            Response response = client.newCall(request).execute();

            // Create an InputStream from the Response
            InputStream inStream = response.body().byteStream();

            // Create the output File where it will be saved
            File outputFile = new File(context.getFilesDir(), Uri.parse(imageUrl).getLastPathSegment());

            // Open an OutputStream to the File
            FileOutputStream outStream = new FileOutputStream(outputFile);

            // Init the buffer to be used to transfer the File
            byte[] buffer = new byte[1024];
            int length;

            // Write the File
            while ((length = inStream.read(buffer)) > 0) {
                outStream.write(buffer, 0, length);
            }

            outStream.flush();
            inStream.close();
            outStream.close();

            return outputFile;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    static Area getArea() {
        return new Area(1, "Yosemite");
    }

    static Area[] getAreas() {
        Area area1 = new Area(1, "Yosemite");
        Area area2 = new Area(2, "Grand Canyon");
        Area area3 = new Area(3, "Red Wood Forest");
        Area area4 = new Area(4, "Yellowstone");
        Area area5 = new Area(5, "Acadia");
        Area area6 = new Area(6, "Glacier");
        Area area7 = new Area(7, "Zion");
        Area area8 = new Area(8, "Arches");
        Area area9 = new Area(9, "Grand Teton");

        return new Area[] {area1, area2, area3, area4, area5, area6, area7, area8, area9};
    }

    static ImageFile getImageFile(Context context) {
        ImageFile file = new ImageFile("TestId", context.getFilesDir(), "test.jpg");

        return file;
    }
}

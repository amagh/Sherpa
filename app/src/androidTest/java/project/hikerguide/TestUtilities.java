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
                assertEquals(errorValueDifferent, field.get(expected), field.get(returned));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    // Getters for dummy values to insert into the database

    static ContentValues getAreaValues() {
        ContentValues values = new ContentValues();
        values.put(GuideContract.AreaEntry.FIREBASE_ID, "testAreaId");
        values.put(GuideContract.AreaEntry.NAME, "Yosemite");
        values.put(GuideContract.AreaEntry.LOWER_CASE_NAME, "yosemite");
        values.put(GuideContract.AreaEntry.LOCATION, "California");

        return values;
    }

    static ContentValues getAuthorValues() {
        ContentValues values = new ContentValues();
        values.put(GuideContract.AuthorEntry.FIREBASE_ID, "testAuthorId");
        values.put(GuideContract.AuthorEntry.NAME, "John Muir");
        values.put(GuideContract.AuthorEntry.LOWER_CASE_NAME, "john muir");
        values.put(GuideContract.AuthorEntry.DESCRIPTION, "Test description");
        values.put(GuideContract.AuthorEntry.SCORE, 100);
        values.put(GuideContract.AuthorEntry.IMAGE_URI, "TestImageUri");

        return values;
    }

    static ContentValues getTrailValues() {
        ContentValues values = new ContentValues();
        values.put(GuideContract.TrailEntry.FIREBASE_ID, "testTrailId");
        values.put(GuideContract.TrailEntry.AREA_ID, "testAreaId");
        values.put(GuideContract.TrailEntry.NAME, "Four Mile Trail");
        values.put(GuideContract.TrailEntry.LOWER_CASE_NAME, "four mile trail");
        values.put(GuideContract.TrailEntry.NOTES, "Temporarily Closed");

        return values;
    }

    static ContentValues getGuideValues() {
        ContentValues values = new ContentValues();
        values.put(GuideContract.GuideEntry.FIREBASE_ID, "testGuideId");
        values.put(GuideContract.GuideEntry.AUTHOR_ID, "testAuthorId");
        values.put(GuideContract.GuideEntry.TRAIL_ID, "testTrailId");
        values.put(GuideContract.GuideEntry.DATE_ADDED, System.currentTimeMillis());
//        values.put(GuideContract.GuideEntry.HAS_IMAGE, 1);
        values.put(GuideContract.GuideEntry.LATITUDE, 37.734);
        values.put(GuideContract.GuideEntry.LONGITUDE, -119.602);
        values.put(GuideContract.GuideEntry.RATING, 5);
        values.put(GuideContract.GuideEntry.REVIEWS, 1);

        return values;
    }

    static ContentValues getSectionValues() {
        ContentValues values = new ContentValues();
        values.put(GuideContract.SectionEntry.FIREBASE_ID, "testSectionId");
        values.put(GuideContract.SectionEntry.GUIDE_ID, "testGuideId");
        values.put(GuideContract.SectionEntry.SECTION, 1);
        values.put(GuideContract.SectionEntry.CONTENT, "Description of the hike");
//        values.put(GuideContract.SectionEntry.HAS_IMAGE, 1);

        return values;
    }

    static Guide getGuide1(Context context) {
        Guide guide = new Guide(System.currentTimeMillis());
        guide.firebaseId = "testFirebaseId";
        guide.authorId = "testAuthorId";
        guide.authorName = "testAuthor";
        guide.trailId = "testTrailId";
        guide.trailName = "testTrailName";
        guide.difficulty = 3;
        guide.setImageUri(downloadFile(context, "http://www.norcalhiker.com/wp-content/uploads/2014/09/09.jpg"));
        guide.setGpxUri(downloadFile(context, "http://www.norcalhiker.com/maps/FourMileTrail.gpx"));

        return guide;
    }

    static Guide getGuide2(Context context) {
        Guide guide = new Guide(System.currentTimeMillis());
        guide.difficulty = 2;
        guide.setImageUri(downloadFile(context, "http://www.norcalhiker.com/wp-content/uploads/2015/03/000.jpg"));
        guide.setGpxUri(downloadFile(context, "http://www.norcalhiker.com/maps/Snow_Mtn.gpx"));

        return guide;
    }

    static Guide getGuide3(Context context) {
        Guide guide = new Guide(System.currentTimeMillis());
        guide.difficulty = 3;
        guide.setImageUri(downloadFile(context, "http://www.norcalhiker.com/wp-content/uploads/2016/01/00-1.jpg"));
        guide.setGpxUri(downloadFile(context, "http://www.norcalhiker.com/maps/Falls_Trail.gpx"));

        return guide;
    }

    static Guide getGuide4(Context context) {
        Guide guide = new Guide(System.currentTimeMillis());
        guide.difficulty = 4;
        guide.setImageUri(downloadFile(context, "http://www.norcalhiker.com/wp-content/uploads/2013/10/032.jpg"));
        guide.setGpxUri(downloadFile(context, "http://www.norcalhiker.com/maps/Muir_Hike.gpx"));

        return guide;
    }

    static Guide[] getGuides() {
        Guide guide1 = new Guide("a", "1", System.currentTimeMillis(), 37.734, -119.602);
        Guide guide2 = new Guide("b", "1", System.currentTimeMillis(), 37.734, -119.602);
        Guide guide3 = new Guide("c", "1", System.currentTimeMillis(), 37.734, -119.602);
        Guide guide4 = new Guide("d", "1", System.currentTimeMillis(), 37.734, -119.602);
        Guide guide5 = new Guide("f", "1", System.currentTimeMillis(), 37.734, -119.602);
        Guide guide6 = new Guide("g", "1", System.currentTimeMillis(), 37.734, -119.602);
        Guide guide7 = new Guide("h", "1", System.currentTimeMillis(), 37.734, -119.602);
        Guide guide8 = new Guide("i", "1", System.currentTimeMillis(), 37.734, -119.602);
        Guide guide9 = new Guide("j", "1", System.currentTimeMillis(), 37.734, -119.602);

        Guide[] guides = {guide1, guide2, guide3, guide4, guide5, guide6, guide7, guide8, guide9};
        return guides;
    }

    static Trail getTrail1() {
        Trail trail = new Trail("Four Mile Trail", null);
        trail.firebaseId = "testFirebaseId";
        trail.areaId = "testAreaId";
        return trail;
    }

    static Trail getTrail2() {
        return new Trail("Snow Mountain", null);
    }

    static Trail getTrail3() {
        return new Trail("Falls Trail Loop", null);
    }

    static Trail getTrail4() {
        return new Trail("Four Mile Trail, Sentinel Dome, Panorama Trail, Liberty Cap and Mist Trail", null);
    }

    static Author getAuthor1(Context context) {
        Author author = new Author("John Muir");
        author.firebaseId = "testAuthorId";
        author.setImageUri(downloadFile(context, "http://www.norcalhiker.com/JMT/Images/170.jpg"));
        return author;
    }

    static Section getSection() {
        return new Section("testGuideId", 1, "Description of hike");
    }

    public static Section[] getSections1(Context context) {
        Section section1 = new Section(1, "The Four Mile Trail (actually 4.8 miles) is one of Yosemite Valley’s most strenuous trails. It climbs up to Glacier Point – an ascent of nearly 3300-ft in just under five miles. After the grueling uphill slog, hikers are rewarded with unparalleled views of Half Dome and the rest of Yosemite Valley. Glacier Point can also be reached by car or shuttle, but the view feels much more rewarding when you walk. We hiked this on a rainy Saturday in late September. Though summer is over, there were still plenty of people out and about. You can’t expect much solitude on this trail, but you can look forward to a great workout and some good people watching!");
        section1.firebaseId = "testSectionId1";
        section1.guideId = "testGuideId";

        Section section2 = new Section(2, "Red Tape:  The park entrance fee is $20.  If you plan to stay in the Valley, reserve accommodations well in advance!  There is limited parking at the trailhead, but it is along the El Capitan shuttle route.  The shuttle runs from June through October, 9am to 6pm. When you enter the park, they will hand you a fairly decent map at the kiosk. The trail is on this map. There are no junctions, so it’s basically impossible to get lost.");
        section2.firebaseId = "testSectionId2";
        section2.guideId = "testGuideId";

        Section section3 = new Section(3, "Trail Description: The hike described here is actually a repeat of the beginning portion of the 20-mile John Muir hike we completed last October. On that trip we hiked half this trail in the dark, then continued on towards Sentinel Dome, the Panorama Trail, Liberty Cap and the Mist Trail. This time around we decided to “take it easy” and enjoy the Four Mile Trail on its own.");
        section3.firebaseId = "testSectionId3";
        section3.guideId = "testGuideId";

        Section section4 = new Section(4, "Getting ready to hike! Saturday morning in Curry Village.");
        section4.firebaseId = "testSectionId4";
        section4.guideId = "testGuideId";
        section4.setImageUri(downloadFile(context, "http://www.norcalhiker.com/wp-content/uploads/2014/09/01.jpg"));

        Section section5 = new Section(5, "We slept late, ate breakfast at the Curry Village Coffee Corner, then drove the loop around Yosemite Valley to reach the trailhead.  The trail has a huge sign, so it’s difficult to miss.  There was no parking (at 10:00 am on a Saturday), so we drove a little further up the road and parked at the Swinging Bridge Picnic Area.");
        section5.firebaseId = "testSectionId5";
        section5.guideId = "testGuideId";

        Section section6 = new Section(6, "Somewhere near the beginning of the trail…");
        section6.firebaseId = "testSectionId6";
        section6.guideId = "testGuideId";
        section6.setImageUri(downloadFile(context, "http://www.norcalhiker.com/wp-content/uploads/2014/09/02.jpg"));

        Section section7 = new Section(7, "We started up the trail and were quickly passed by several people walking much faster.  I think we are just slow, but I will blame our heavy packs.  We carried lots of food, a couple of beers, warm clothing and rain gear.  All proved to be very useful and/or delicious.  Many of the other hikers were wearing shorts (or jeans!!) and not carrying jackets.  It was cold and rainy – seems like poor preparation (but who am I to judge?)");
        section7.firebaseId = "testSectionId7";
        section7.guideId = "testGuideId";

        Section section8 = new Section(8, "Moss on all the rocks.");
        section8.firebaseId = "testSectionId8";
        section8.guideId = "testGuideId";
        section8.setImageUri(downloadFile(context, "http://www.norcalhiker.com/wp-content/uploads/2014/09/03.jpg"));

        Section section9 = new Section(9, "The trail climbs at a steady, relentless grade with many switchbacks.  Soon we had glimpses of El Cap and the Valley through the trees.  About halfway up, we started catching views of Half Dome.  We also began encountering quite a few hikers coming down the trail. Some had taken the shuttle up and were now hiking down.  Others had already hiked to the top and were on their way back.  I’m not used to this because we are usually the first people to hit a trail!");
        section9.firebaseId = "testSectionId9";
        section9.guideId = "testGuideId";

        return new Section[] {section1, section2, section3, section4, section5, section6, section7, section8, section9};
    }

    static Section[] getSections2(Context context) {
        Section section1 = new Section(1, "Last week, as I was making plans to visit the Snow Mountain Wilderness, I discovered that the vast majority of people I talked to had never heard of Snow Mountain. This is too bad because efforts are underway to create a Berryessa Snow Mountain National Monument. This would protect the land for future generations while improving coordination between the agencies who plan for fires, manage recreation and clear out invasive species and/or marijuana grow sites. There is already a lot of support for the proposed National Monument, but if more people knew about the area, there might be greater momentum behind the cause.");
        Section section2 = new Section(2, "Basics: This hike begins at Deafy Glade (pronounced “Deefee”) and climbs 4000-ft to the east summit of Snow Mountain (7056′). It’s 14 miles, roundtrip. The steepest portion of the trail is in the first 3.5 miles – between Deafy Glade and Summit Spring. Alternatively, it’s possible to start at the Summit Spring trailhead. This shorter route is approximately 8 miles roundtrip but requires an extra 30 to 45 minutes of bumpy dirt-road driving. (The pavement ends just after the Deafy Glade trailhead.) Snow Mountain can be hiked or snowshoed year-round, but heavy snow in the winter will close the last few miles of the road to Summit Springs. This hike is dog friendly and great for backpacking.");
        Section section3 = new Section(3, "Red Tape, Reservations and Camping: There are no permits, quotas or fees for hiking this peak. A campfire permit is required if you plan to use a stove while backpacking – check the Forest Service website. Bear cans are recommended but not required.");
        Section section4 = new Section(4, "If you plan to car camp, Dixie Glade campground is located right next to the trailhead. It’s comprised of 8 sites that are first come, first served. There are also a handful of OHV campgrounds several miles before the trailhead including: Fouts, Mill Creek, and Gray Pine Group. These are outside of the wilderness area and in the heart of the OHV territory.");
        Section section5 = new Section(5, "Directions to Trailhead: Exit I-5 at the town of Maxwell in Colusa County and head west along Maxwell Sites Road. Follow signs for Stonyford (making right turns onto Sites Lodoga Road then Lodoga Stonyford Road). Once you reach the town of Stonyford, turn left onto Market Street. One block later, turn left again on to Fouts Spring Road (aka M10). Follow Fouts Spring Road for the next 12 or 13 miles. The pavement ends at the Deafy Glade trailhead, shortly after the Dixie Glade campground.");
        Section section6 = new Section(6, "Trail Description: Rob has wanted to climb Snow Mountain for quite some time. He always said “we should hike it when there’s snow!” Unfortunately, there is no snow this winter. Some friends invited us to go camping in the area, so we decided to join them and hike the peak – snow or no snow. We camped at the Gray Pine Group site in the heart of OHV territory. After a windy night, we woke up around sunrise and made the short drive to Deafy Glade. We signed the register and hit the trail by 8:30am.");
        Section section7 = new Section(7, null);
        section7.setImageUri(downloadFile(context, "http://www.norcalhiker.com/wp-content/uploads/2015/03/01.jpg"));
        Section section8 = new Section(8, null);
        section8.setImageUri(downloadFile(context, "http://www.norcalhiker.com/wp-content/uploads/2015/03/02.jpg"));
        Section section9 = new Section(9, "The trail starts out deceivingly flat. It follows an old road as it contours around the hillside then eventually descends to South Fork Stony Creek (about 1 mile in). There is no bridge at this creek crossing. SummitPost describes the crossing as crotch-deep on a 6-ft person in mid-May. This would be challenging but we are in a drought. We hopped on a couple of rocks and were quickly across.");
        Section section10 = new Section(10, null);
        section10.setImageUri(downloadFile(context, "http://www.norcalhiker.com/wp-content/uploads/2015/03/03.jpg"));
        Section section11 = new Section(11, null);
        section11.setImageUri(downloadFile(context, "http://www.norcalhiker.com/wp-content/uploads/2015/03/03.jpg"));

        return new Section[] {section1, section2, section3, section4, section5, section6, section7, section8, section9, section10, section11};
    }

    static Section[] getSections3(Context context) {
        Section section1 = new Section(1, "For anyone who has hiked in Mount Diablo’s blistering summer heat, it seems almost impossible that there could be even a single waterfall on that mountain, much less an entire trail dedicated to numerous falls.  The Falls Trail is a 1.15 mile point-to-point trail located somewhere amongst the maze of paths on Mount Diablo’s north slope.  We hiked the Falls Trail as part of a 7-mile loop, beginning and ending at the Mitchell Canyon Visitor Center.");
        Section section2 = new Section(2, "Trailhead Directions and Red Tape: The visitor center parking area is located at the south end of Mitchell Canyon Road in Clayton, CA. Google Map link to trailhead. Parking is $6 and the gate opens at 8:00 am.  It’s also possible to hike from the trailhead at the Regency Gate.");
        Section section3 = new Section(3, "Trail Description: It rained for most of last week (thanks El Niño), making Saturday the perfect time for a waterfall hike.  I always heard there were waterfalls at Mount Diablo, but I had never actually seen any of them before.");
        Section section4 = new Section(4, null);
        section4.setImageUri(downloadFile(context, "http://www.norcalhiker.com/wp-content/uploads/2016/01/01-1-768x576.jpg"));
        Section section5 = new Section(5, "Flowers near the Donner Cabin site.");
        section5.setImageUri(downloadFile(context, "http://www.norcalhiker.com/wp-content/uploads/2016/01/03-1-768x500.jpg"));
        Section section6 = new Section(6, "In stark contrast to the wider dirt roads, the Falls Trail is a narrow single track path with steep drop-offs to the side.  It was fortunately far less muddy so we had good traction.  We could see several falls as we contoured around the hillside.");
        Section section7 = new Section(7, null);
        section7.setImageUri(downloadFile(context, "http://www.norcalhiker.com/wp-content/uploads/2016/01/04-768x576.jpg"));
        Section section8 = new Section(8, null);
        section8.setImageUri(downloadFile(context, "http://www.norcalhiker.com/wp-content/uploads/2016/01/05-1-768x576.jpg"));
        Section section9 = new Section(9, null);
        section9.setImageUri(downloadFile(context, "http://www.norcalhiker.com/wp-content/uploads/2016/01/06-1-768x576.jpg"));
        Section section10 = new Section(10, "Somewhere halfway down the trail, we came around a bend and found a woman precariously clinging to an unstable slope about 20 feet above us.  We asked if she was okay and mentioned that maybe she had gotten off the trail a little…?  Her friend quickly appeared and explained that we could reach an even better waterfall if we scrambled up the slide.  With little hesitation, we made our way up the same stupid slope.  It was definitely worth it.  A narrow and somewhat unstable path took us to the best waterfall on the entire trail.  We would have completely walked past this had we not run into these people.  Rob thanked them on our way down.");
        Section section11 = new Section(11, null);
        section11.setImageUri(downloadFile(context, "http://www.norcalhiker.com/wp-content/uploads/2016/01/07-1.jpg"));

        return new Section[] {section1, section2, section3, section4, section5, section6, section7, section8, section9, section10, section11};
    }

    static Section[] getSections4(Context context) {
        Section section1 = new Section(1, "In John Muir’s book The Yosemite, there is a chapter near the end entitled “How Best to Spend One’s Yosemite Time”. The chapter describes several hikes, just as a modern blog would. He describes two single day excursions, two 2-day excursions, a 3-day excursion, and a grand several week excursion (not the JMT). This post focuses on the first listed single day excursion – the quintessential day hike in Yosemite. It’s about 20 miles total, with about 6,500 feet of elevation gain – longer and more difficult than a round trip up Half Dome.");
        Section section2 = new Section(2, "We first attempted this hike several months ago in August.  We failed.  Even though we made it to the trailhead before sunrise, Christa was carsick (after the drive in from the Wawona Hotel) and it was very hot, humid and buggy.  We made another attempt in October – this time we succeeded!  Completing this hike requires an early start, a bit of luck, and a lot of determination.  The following text is John Muir’s description (in its entirety) annotated with my comments and photos.");
        Section section3 = new Section(3, "\"If I were so time poor as to have only one day to spend in Yosemite I should start at daybreak, say at three o clock in midsummer, with a pocketful of any sort of dry breakfast stuff, for Glacier Point, Sentinel Dome, the head of Illilouette Fall, Nevada Fall, the top of Liberty Cap, Vernal Fall, and the wild boulder choked River Canõn.\"");
        Section section4 = new Section(4, "Wow.  All day with just a pocket full of dry breakfast stuff?  We’re going to need a little more food than that.  Also, on the longest day of the year, sunrise or “daybreak” is at 5:35am. So 3:00am would be a bit before daybreak in my book.  Nonetheless, we began our hike at 5:50am in October – about an hour and a half before sunrise, just as Muir is suggesting.");
        Section section5 = new Section(5, "Lunch for the day.");
        section5.setImageUri(downloadFile(context, "http://www.norcalhiker.com/wp-content/uploads/2013/10/012.jpg"));
        Section section6 = new Section(6, "For the record, we ate all of the following on our hike: three dried fruit leathers, two bagel sandwiches, two cans of Hop ‘Ottin IPA, one Superfood ProBar, half a pound of dark chocolate covered pretzels, a handful of coffee almonds, and one package of ProBar Bolt energy chews (they taste like gummy bears). Additionally, before we left in the morning, we each ate a bagel with cream cheese and drank a Starbucks Doubleshot®. A little more than a pocket full of dry breakfast stuff.");
        Section section7 = new Section(7, "We are not used to hiking with headlamps. Christa kept blinding me with her light.  I considered hiking with just the light of the moon and stars, but it was pretty dark around our feet. The lights kept us from tripping over the uneven trail surface. Despite the blinding headlamps, El Capitan and other valley features were impressive in the half-moon light.  We heard owls hooting, but otherwise the valley was silent. It actually spooked me a bit – I expected to see the eyes of an unidentified animal (bear) shining back at me, but all was quiet this morning.  We got a little lost between the Lodge and the Four Mile Trail, but we managed to find the trailhead in the dark.");
        Section section8 = new Section(8, "\"The trail leaves the Valley at the base of the Sentinel Rock, and as you slowly saunter from point to point along its many accommodating zigzags nearly all the Valley rocks and falls are seen in striking, ever changing combinations. At an elevation of about five hundred feet a particularly fine, wide sweeping view down the Valley is obtained, past the sheer face of the Sentinel and between the Cathedral Rocks and El Capitan.\"");
        section8.setImageUri(downloadFile(context, "http://www.norcalhiker.com/wp-content/uploads/2016/01/05-1-768x576.jpg"));
        Section section9 = new Section(9, null);
        section9.setImageUri(downloadFile(context, "http://www.norcalhiker.com/wp-content/uploads/2013/10/032.jpg"));
        Section section10 = new Section(10, "\"At a height of about 1500 feet the great Half Dome comes full in sight, overshadowing every other feature of the Valley to the eastward.\"");
        Section section11 = new Section(11, "The view from the Four Mile Trail is epic. If you have a decent set of legs and you’re in Yosemite, I advise you to hike up to Glacier Point and Sentinal Dome via the Four Mile Trail.");

        return new Section[] {section1, section2, section3, section4, section5, section6, section7, section8, section9, section10, section11};
    }

    static File downloadFile(Context context, String url) {
        // Use OkHttp to get a connection to the URL
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();

        try {
            Response response = client.newCall(request).execute();

            // Create an InputStream from the Response
            InputStream inStream = response.body().byteStream();

            // Create the output File where it will be saved
            File outputFile = new File(context.getFilesDir(), Uri.parse(url).getLastPathSegment());

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

    static Area getArea1() {
        Area area = new Area("Yosemite Valley");
        area.firebaseId = "testAreaId";
        area.location   = "testLocation";

        return area;
    }

    static Area getArea2() {
        return new Area("Mendocino National Forest");
    }

    static Area getArea3() {
        return new Area("Mount Diablo State Park");
    }

    static Area[] getAreas() {
        Area area1 = new Area("Yosemite");
        Area area2 = new Area("Grand Canyon");
        Area area3 = new Area("Red Wood Forest");
        Area area4 = new Area("Yellowstone");
        Area area5 = new Area("Acadia");
        Area area6 = new Area("Glacier");
        Area area7 = new Area("Zion");
        Area area8 = new Area("Arches");
        Area area9 = new Area("Grand Teton");

        return new Area[] {area1, area2, area3, area4, area5, area6, area7, area8, area9};
    }

    static ImageFile getImageFile(Context context) {
        ImageFile file = new ImageFile("TestId", context.getFilesDir(), "test.jpg");

        return file;
    }
}

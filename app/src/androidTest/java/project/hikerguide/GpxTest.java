package project.hikerguide;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import project.hikerguide.utilities.GpxStats;
import project.hikerguide.utilities.GpxUtils;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * Created by Alvin on 7/21/2017.
 */

@RunWith(AndroidJUnit4.class)
public class GpxTest {
    // ** Constants ** //
    private static final String GPX_URL = "http://www.gpsvisualizer.com/download/convert/20170722025200-45110-data.gpx";

    @Test
    public void testGetGpxStats() {
        // Get the Gpx File
        File file = TestUtilities.downloadFile(InstrumentationRegistry.getTargetContext(), GPX_URL);

        // Generate the GpxStats
        GpxStats stats = GpxUtils.getGpxStats(file);

        String errorCalculatingStats = "GpxUtils was unable to calculate GpxStats for the Gpx File downloaded";
        assertNotNull(errorCalculatingStats, stats);

        // Convert results to Imperial measurements
        double distance = stats.distance / 1609.34;
        double elevation = stats.elevation * 3.28084;

        // Check stats are within margins of error compared to known distance and elevation
        String errorDistanceInaccurate = "Distance calculated is greater than 0.1 mile different than the expected distance.";
        assertTrue(errorDistanceInaccurate, distance > 9.5 && distance < 9.7);

        String errorElevationInaccurate = "Elevation calculated is greater than 100ft different than the expected elevation";
        assertTrue(errorElevationInaccurate, elevation < 3300 && elevation > 3100);
    }
}

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
    static final String GPX_URL = "http://www.norcalhiker.com/maps/FourMileTrail.gpx";

    @Test
    public void testGetGpxDistance() {
        // Get the Gpx File
        File file = TestUtilities.downloadFile(InstrumentationRegistry.getTargetContext(), GPX_URL);

        // Generate the GpxStats
        GpxStats stats = GpxUtils.getGpxStats(file);

        String errorCalculatingStats = "GpxUtils was unable to calculate GpxStats for the Gpx File downloaded";
        assertNotNull(errorCalculatingStats, stats);

        String errorDistanceInaccurate = "Distance calculated is greater than 0.1 mile different than the expected distance.";
        assertTrue(errorDistanceInaccurate, stats.distance > 9.5 && stats.distance < 9.7);
    }
}

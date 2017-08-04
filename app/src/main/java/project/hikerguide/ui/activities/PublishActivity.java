package project.hikerguide.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

import java.io.File;

import project.hikerguide.models.datamodels.Area;
import project.hikerguide.models.datamodels.Author;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.models.datamodels.Section;
import project.hikerguide.models.datamodels.Trail;
import project.hikerguide.utilities.SaveUtils;
import timber.log.Timber;

import static project.hikerguide.utilities.IntentKeys.AREA_KEY;
import static project.hikerguide.utilities.IntentKeys.AUTHOR_KEY;
import static project.hikerguide.utilities.IntentKeys.GUIDE_KEY;
import static project.hikerguide.utilities.IntentKeys.SECTION_KEY;
import static project.hikerguide.utilities.IntentKeys.TRAIL_KEY;

/**
 * Created by Alvin on 8/3/2017.
 */

public class PublishActivity extends MapboxActivity {
    // ** Member Variables ** //
    private Author mAuthor;
    private Guide mGuide;
    private Area mArea;
    private Trail mTrail;
    private Section[] mSections;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the data objects passed from the Intent
        Intent intent = getIntent();

        mAuthor = intent.getParcelableExtra(AUTHOR_KEY);
        mGuide = intent.getParcelableExtra(GUIDE_KEY);
        mArea = intent.getParcelableExtra(AREA_KEY);
        mTrail = intent.getParcelableExtra(TRAIL_KEY);

        // Get the Parcelable[] for the Sections
        Parcelable[] parcelables = intent.getParcelableArrayExtra(SECTION_KEY);
        mSections = new Section[parcelables.length];

        // Copy the elements from parcelables to mSections as it cannot be directly cast to Section[]
        System.arraycopy(parcelables, 0, mSections, 0, parcelables.length);

        // Resize any images associated with the models
        resizeImages();
    }

    /**
     * Resize the images of any data models that have associated image Files
     */
    private void resizeImages() {

        // Resize the image for the Guide
        SaveUtils.resizeImageForModel(mGuide);

        // Resize the image for any Sections that have images
        for (Section section : mSections) {
            if (section.hasImage) {
                SaveUtils.resizeImageForModel(section);
            }
        }
    }
}

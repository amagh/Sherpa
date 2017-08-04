package project.hikerguide.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import project.hikerguide.models.datamodels.Area;
import project.hikerguide.models.datamodels.Author;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.models.datamodels.Section;
import project.hikerguide.models.datamodels.Trail;

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

        Intent intent = getIntent();

        mAuthor = intent.getParcelableExtra(AUTHOR_KEY);
        mGuide = intent.getParcelableExtra(GUIDE_KEY);
        mArea = intent.getParcelableExtra(AREA_KEY);
        mTrail = intent.getParcelableExtra(TRAIL_KEY);
        mSections = (Section[]) intent.getParcelableArrayExtra(SECTION_KEY);


    }

    private boolean validateGuide() {
        if (!mGuide.hasImage) {
            return false;
        }

        if (mGuide.getGpxUri() == null || mGuide.distance == 0) {
            return false;
        }

        return true;
    }
}

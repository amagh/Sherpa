package project.sherpa.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import project.sherpa.R;
import project.sherpa.models.datamodels.Message;
import project.sherpa.ui.fragments.FavoritesFragment;

import static project.sherpa.models.datamodels.Message.ATTACHMENT_TYPE;
import static project.sherpa.models.datamodels.Message.AttachmentType.GUIDE_TYPE;
import static project.sherpa.models.datamodels.Message.AttachmentType.NONE;

/**
 * Created by Alvin on 9/18/2017.
 */

public class AttachActivity extends ConnectivityActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attach);

        // Start the correct Fragment based on the AttachmentType
        @Message.AttachmentType
        int attachmentType = getIntent().getIntExtra(ATTACHMENT_TYPE, NONE);

        Fragment fragment = null;

        switch (attachmentType) {
            case NONE: finish();
                break;
            case GUIDE_TYPE:
                fragment = new FavoritesFragment();
                break;
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    /**
     * Finishes the Activity and returns the result to the calling Activity/Fragment
     *
     * @param returnIntent    Intent containing the data for the attachment
     */
    public void finishWithAttachment(Intent returnIntent) {
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }
}

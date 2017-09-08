package project.sherpa.models.datamodels.abstractmodels;

import com.google.firebase.database.Exclude;

import java.util.Map;

/**
 * Base data model type
 */

public abstract class BaseModel {
    // ** Member Variables ** //
    public String firebaseId;
    private boolean draft;

    public abstract Map<String, Object> toMap();

    @Exclude
    public boolean isDraft() {
        return draft;
    }

    @Exclude
    public void setDraft(boolean draft) {
        this.draft = draft;
    }
}

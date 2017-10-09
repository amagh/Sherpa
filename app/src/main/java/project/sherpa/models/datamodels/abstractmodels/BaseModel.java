package project.sherpa.models.datamodels.abstractmodels;

import com.google.firebase.database.Exclude;

import java.util.Map;

/**
 * Base data model type
 */

public abstract class BaseModel {

    // ** Constants ** //
    public static final String FIREBASE_ID = "firebaseId";

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

    /**
     * Replaces the values of the BaseModel with new values
     *
     * @param newModelValues    BaseModel containing the new Values
     */
    @Exclude
    public abstract void updateValues(BaseModel newModelValues);
}

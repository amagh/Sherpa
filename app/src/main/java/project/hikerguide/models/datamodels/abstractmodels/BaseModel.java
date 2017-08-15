package project.hikerguide.models.datamodels.abstractmodels;

import java.util.Map;

/**
 * Base data model type
 */

public abstract class BaseModel {
    // ** Member Variables ** //
    public String firebaseId;
    private boolean draft;

    public abstract Map<String, Object> toMap();

    public boolean isDraft() {
        return draft;
    }

    public void setDraft(boolean draft) {
        this.draft = draft;
    }
}

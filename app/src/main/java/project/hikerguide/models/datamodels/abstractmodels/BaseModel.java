package project.hikerguide.models.datamodels.abstractmodels;

import java.util.Map;

/**
 * Base data model type
 */

public abstract class BaseModel {
    // ** Member Variables ** //
    public String firebaseId;

    public abstract Map<String, Object> toMap();
}

package project.hikerguide.models.abstractmodels;

import java.util.Map;

/**
 * Base data model type
 */

public abstract class BaseModel {
    // ** Constants ** //
    public static final String ID = "id";

    // ** Member Variables ** //
    public long id;
    public String firebaseId;

    public abstract Map<String, Object> toMap();
}

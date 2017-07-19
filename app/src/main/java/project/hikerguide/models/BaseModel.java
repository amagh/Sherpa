package project.hikerguide.models;

import java.util.Map;

/**
 * Base data model type
 */

public abstract class BaseModel {
    // ** Constants ** //
    static final String ID = "id";

    // ** Member Variables ** //
    public long id;
    public String firebaseId;

    public abstract Map<String, Object> toMap();
}

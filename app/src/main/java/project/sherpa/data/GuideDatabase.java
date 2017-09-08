package project.sherpa.data;

import net.simonvt.schematic.annotation.Database;
import net.simonvt.schematic.annotation.Table;

/**
 * Created by Alvin on 7/17/2017.
 */

@Database(fileName = GuideDatabase.DATABASE_NAME,
        version = GuideDatabase.VERSION)
public class GuideDatabase {
    // ** Constants ** //
    public static final int VERSION = 1;
    public static final String DATABASE_NAME = "guides.db";

    @Table(GuideContract.GuideEntry.class)
    public static final String GUIDES = "guides";

    @Table(GuideContract.TrailEntry.class)
    public static final String TRAILS = "trails";

    @Table(GuideContract.AuthorEntry.class)
    public static final String AUTHORS = "authors";

    @Table(GuideContract.SectionEntry.class)
    public static final String SECTIONS = "sections";

    @Table(GuideContract.AreaEntry.class)
    public static final String AREAS = "areas";
}

package project.sherpa.data;

import android.database.sqlite.SQLiteDatabase;

import net.simonvt.schematic.annotation.Database;
import net.simonvt.schematic.annotation.OnUpgrade;
import net.simonvt.schematic.annotation.Table;

import project.sherpa.data.generated.GuideDatabase.*;


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

    @Table(GuideContract.MessageEntry.class)
    public static final String MESSAGES = "messages";

    @Table(GuideContract.ChatEntry.class)
    public static final String CHATS = "chats";

//    @OnUpgrade
//    public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//        switch (oldVersion) {
//            case 1:
//                db.beginTransaction();
//                try {
//
//                    // Create MESSAGES and CHATS tables
//                    db.execSQL(project.sherpa.data.generated.GuideDatabase.MESSAGES);
//                    db.execSQL(project.sherpa.data.generated.GuideDatabase.CHATS);
//                    db.setTransactionSuccessful();
//                } finally {
//                    db.endTransaction();
//                }
//        }
//    }
}

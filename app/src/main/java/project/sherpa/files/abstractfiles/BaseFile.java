package project.sherpa.files.abstractfiles;

import android.support.annotation.NonNull;

import java.io.File;
import java.net.URI;

/**
 * Created by Alvin on 7/19/2017.
 */

public abstract class BaseFile extends File {
    // ** Member Variables ** //
    public String firebaseId;

    public BaseFile(String pathname) {
        super(pathname);
    }

    public BaseFile(String parent, String child) {
        super(parent, child);
    }

    public BaseFile(File parent, String child) {
        super(parent, child);
    }

    public BaseFile(@NonNull URI uri, String firebaseId) {
        super(uri);
    }
}

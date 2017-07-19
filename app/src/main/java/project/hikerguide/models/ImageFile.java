package project.hikerguide.models;

import android.support.annotation.NonNull;

import java.io.File;
import java.net.URI;

/**
 * Created by Alvin on 7/19/2017.
 */

public class ImageFile extends File {
    // ** Member Variables ** //
    public String firebaseId;

    public ImageFile(String pathname) {
        super(pathname);
    }

    public ImageFile(String parent, String child) {
        super(parent, child);
    }

    public ImageFile(File parent, String child) {
        super(parent, child);
    }

    public ImageFile(@NonNull URI uri) {
        super(uri);
    }
}

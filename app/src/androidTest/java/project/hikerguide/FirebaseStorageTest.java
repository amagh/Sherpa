package project.hikerguide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import project.hikerguide.firebasestorage.*;
import project.hikerguide.files.ImageFile;

import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by Alvin on 7/19/2017.
 */

@RunWith(AndroidJUnit4.class)
public class FirebaseStorageTest {
    // ** Constants ** //
    private static final String IMAGE_URL = "http://www.norcalhiker.com/wp-content/uploads/2014/02/031.jpg";

    // ** Member Variables ** //
    private Context mContext;
    private StorageProvider mProvider;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getTargetContext();
        mProvider = StorageProvider.getInstance();

        ImageFile image = TestUtilities.getImageFile(mContext);

        mProvider.deleteFile(image);

        if (image.exists()) {
            image.delete();
        }
    }

    @Test
    public void testUploadFile() {
        // Get the dummy ImageFile to upload
        ImageFile file = downloadTestImage();

        String errorUploading = "Failed to upload file to Firebase Storage";
        assertTrue(errorUploading, mProvider.uploadFile(file));
    }

    @Test
    public void testDownloadFile() {
        // Upload the File to Storage
        ImageFile srcFile = downloadTestImage();
        mProvider.uploadFile(srcFile);

        // Create a new File to be downloaded
        ImageFile destFile = ImageFile.getDestinationFile(mContext, srcFile.firebaseId);

        String errorDownloading = "Download could not be completed. Check logs for error message";
        assertTrue(errorDownloading, mProvider.downloadFile(destFile));

        String errorImageIncomplete = "Size of the image does not match the expected size of the image";
        assertEquals(errorImageIncomplete, 151244, destFile.length());

        // Delete the image
        destFile.delete();
    }

    @Test
    public void testDeleteFile() {
        // Upload the File to Storage
        ImageFile srcFile = downloadTestImage();
        mProvider.uploadFile(srcFile);

        // Check to ensure the File was properly uploaded
        Uri downloadUri = mProvider.getDownloadUrl(srcFile);

        String errorNoUri = "No download URL was returned from Firebase Storage. Image was not properly uploaded.";
        assertNotNull(errorNoUri, downloadUri);

        // Delete the File
        String errorDeleting = "There was an error deleting the File. Check logs for details.";
        assertTrue(errorDeleting, mProvider.deleteFile(srcFile));
        downloadUri = mProvider.getDownloadUrl(srcFile);

        // Check to ensure the File was properly deleted
        String errorUriExists = "A download URL was returned from Firebase Storage. Image was not properly deleted.";
        assertNull(errorUriExists, downloadUri);
    }

    /**
     * Helper method for downloading an image from the Internet to be used as the test image
     *
     * @return The ImageFile corresponding to the downloaded image
     */
    public ImageFile downloadTestImage() {
        // Get the dummy ImageFile to upload
        ImageFile file = TestUtilities.getImageFile(mContext);

        // Create a Request for the image using the URL
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(IMAGE_URL)
                .build();

        try {
            // Get a response from the Request
            Response response = client.newCall(request).execute();

            // Check that the response is valid
            String errorNoResponse = "Received no response from the URL";
            assertNotNull(errorNoResponse, response);

            // Get the InputStream from the Response
            InputStream inStream = response.body().byteStream();

            // Create a Bitmap from the InputStream
            Bitmap bitmap = BitmapFactory.decodeStream(inStream);

            // Create a File from the Bitmap's InputStream
            FileOutputStream outStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outStream);
            outStream.flush();
            outStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return file;
    }

    @After
    public void cleanup() {
        ImageFile image = TestUtilities.getImageFile(mContext);

        mProvider.deleteFile(image);

        if (image.exists()) {
            image.delete();
        }
    }
}

package project.hikerguide.filepicker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.v4.app.Fragment;

import java.util.ArrayList;

import droidninja.filepicker.FilePickerActivity;
import droidninja.filepicker.FilePickerBuilder;
import droidninja.filepicker.FilePickerConst;
import droidninja.filepicker.PickerManager;
import droidninja.filepicker.models.FileType;
import droidninja.filepicker.utils.Orientation;

/**
 * Created by Alvin on 8/23/2017.
 */

public class CustomFilePickerBuilder  {
    private final Bundle mPickerOptionsBundle;
    private int mRequestCode = -1;

    public CustomFilePickerBuilder() {
        mPickerOptionsBundle = new Bundle();
    }

    public static CustomFilePickerBuilder getInstance() {
        return new CustomFilePickerBuilder();
    }

    public CustomFilePickerBuilder setMaxCount(int maxCount) {
        PickerManager.getInstance().setMaxCount(maxCount);
        return this;
    }

    public CustomFilePickerBuilder setActivityTheme(int theme) {
        PickerManager.getInstance().setTheme(theme);
        return this;
    }

    public CustomFilePickerBuilder setSelectedFiles(ArrayList<String> selectedPhotos) {
        mPickerOptionsBundle.putStringArrayList(FilePickerConst.KEY_SELECTED_MEDIA, selectedPhotos);
        return this;
    }

    public CustomFilePickerBuilder enableVideoPicker(boolean status) {
        PickerManager.getInstance().setShowVideos(status);
        return this;
    }

    public CustomFilePickerBuilder enableImagePicker(boolean status) {
        PickerManager.getInstance().setShowImages(status);
        return this;
    }

    public CustomFilePickerBuilder showGifs(boolean status) {
        PickerManager.getInstance().setShowGif(status);
        return this;
    }

    public CustomFilePickerBuilder showFolderView(boolean status) {
        PickerManager.getInstance().setShowFolderView(status);
        return this;
    }

    public CustomFilePickerBuilder enableDocSupport(boolean status) {
        PickerManager.getInstance().setDocSupport(status);
        return this;
    }

    public CustomFilePickerBuilder enableCameraSupport(boolean status) {
        PickerManager.getInstance().setEnableCamera(status);
        return this;
    }

    public CustomFilePickerBuilder withOrientation(Orientation orientation) {
        PickerManager.getInstance().setOrientation(orientation);
        return this;
    }

    public CustomFilePickerBuilder addFileSupport(String title, String[] extensions, @DrawableRes int drawable) {
        PickerManager.getInstance().addFileType(new FileType(title,extensions,drawable));
        return this;
    }

    public CustomFilePickerBuilder addFileSupport(String title, String[] extensions) {
        PickerManager.getInstance().addFileType(new FileType(title,extensions,0));
        return this;
    }

    public CustomFilePickerBuilder setRequestCode(int requestCode) {
        mRequestCode = requestCode;
        return this;
    }

    public void pickPhoto(Activity context) {
        mPickerOptionsBundle.putInt(FilePickerConst.EXTRA_PICKER_TYPE,FilePickerConst.MEDIA_PICKER);
        start(context,FilePickerConst.MEDIA_PICKER);
    }

    public void pickPhoto(Fragment context) {
        mPickerOptionsBundle.putInt(FilePickerConst.EXTRA_PICKER_TYPE,FilePickerConst.MEDIA_PICKER);
        start(context,FilePickerConst.MEDIA_PICKER);
    }

    public void pickFile(Activity context) {
        mPickerOptionsBundle.putInt(FilePickerConst.EXTRA_PICKER_TYPE,FilePickerConst.DOC_PICKER);
        start(context,FilePickerConst.DOC_PICKER);
    }

    public void pickFile(Fragment context) {
        mPickerOptionsBundle.putInt(FilePickerConst.EXTRA_PICKER_TYPE,FilePickerConst.DOC_PICKER);
        start(context,FilePickerConst.DOC_PICKER);
    }

    private void start(Activity context, int pickerType) {
        PickerManager.getInstance().setProviderAuthorities(context.getApplicationContext().getPackageName() + ".droidninja.filepicker.provider");

        Intent intent = new Intent(context, FilePickerActivity.class);
        intent.putExtras(mPickerOptionsBundle);

        if(pickerType==FilePickerConst.MEDIA_PICKER) {
            int requestCode = mRequestCode != -1
                    ? mRequestCode
                    : FilePickerConst.REQUEST_CODE_PHOTO;

            context.startActivityForResult(intent, requestCode);
        } else {
            int requestCode = mRequestCode != -1
                    ? mRequestCode
                    : FilePickerConst.REQUEST_CODE_DOC;

            context.startActivityForResult(intent, requestCode);
        }
    }

    private void start(Fragment fragment, int pickerType) {
        PickerManager.getInstance().setProviderAuthorities(fragment.getContext().getApplicationContext().getPackageName() + ".droidninja.filepicker.provider");

        Intent intent = new Intent(fragment.getActivity(), FilePickerActivity.class);
        intent.putExtras(mPickerOptionsBundle);
        if(pickerType==FilePickerConst.MEDIA_PICKER) {
            int requestCode = mRequestCode != -1
                    ? mRequestCode
                    : FilePickerConst.REQUEST_CODE_PHOTO;

            fragment.getActivity().startActivityForResult(intent, requestCode);
        } else {
            int requestCode = mRequestCode != -1
                    ? mRequestCode
                    : FilePickerConst.REQUEST_CODE_DOC;

            fragment.getActivity().startActivityForResult(intent, requestCode);
        }
    }
}

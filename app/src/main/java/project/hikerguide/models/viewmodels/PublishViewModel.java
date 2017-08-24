package project.hikerguide.models.viewmodels;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import project.hikerguide.BR;
import project.hikerguide.R;

/**
 * Created by Alvin on 8/23/2017.
 */

public class PublishViewModel extends BaseObservable {

    // ** Member Variables ** //
    private double mUploadProgress;
    private int mTotalUploads;
    private int mCurrentUpload = 1;

    @Bindable
    public int getTotalUploads() {
        return mTotalUploads;
    }

    public void setTotalUploads(int totalUploads) {
        mTotalUploads = totalUploads;

        notifyPropertyChanged(BR.totalUploads);
    }

    @Bindable
    public int getCurrentUpload() {
        return mCurrentUpload;
    }

    public void setCurrentUpload(int currentUpload) {
        mCurrentUpload = currentUpload;

        notifyPropertyChanged(BR.currentUpload);
    }

    @BindingAdapter({"currentUpload", "totalUploads"})
    public static void updateTextProgress(TextView textView, int currentUpload, int totalUploads) {
        String uploadProgress = textView.getContext().getString(R.string.publish_progress_text, currentUpload, totalUploads);

        textView.setText(uploadProgress);
    }

    @Bindable
    public double getUploadProgress() {
        return mUploadProgress;
    }

    public void setUploadProgress(double progress) {
        mUploadProgress = progress;

        notifyPropertyChanged(BR.uploadProgress);
    }

    @BindingAdapter("uploadProgress")
    public static void updateProgressBar(ProgressBar progressBar, double uploadProgress) {
        progressBar.setIndeterminate(false);
        progressBar.setProgress((int) uploadProgress);
    }
}

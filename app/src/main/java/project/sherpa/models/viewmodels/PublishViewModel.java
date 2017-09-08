package project.sherpa.models.viewmodels;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.widget.TextView;

import project.sherpa.BR;
import project.sherpa.R;

/**
 * Created by Alvin on 8/23/2017.
 */

public class PublishViewModel extends BaseObservable {

    // ** Member Variables ** //
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
}

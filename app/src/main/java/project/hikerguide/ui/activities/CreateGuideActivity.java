package project.hikerguide.ui.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.internal.NavigationMenu;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import droidninja.filepicker.FilePickerBuilder;
import droidninja.filepicker.FilePickerConst;
import io.github.yavski.fabspeeddial.FabSpeedDial;
import project.hikerguide.BR;
import project.hikerguide.R;
import project.hikerguide.databinding.ActivityCreateGuideBinding;
import project.hikerguide.databinding.ListItemGuideDetailsBinding;
import project.hikerguide.models.datamodels.Area;
import project.hikerguide.models.datamodels.Author;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.models.datamodels.Section;
import project.hikerguide.models.datamodels.Trail;
import project.hikerguide.models.datamodels.abstractmodels.BaseModel;
import project.hikerguide.models.datamodels.abstractmodels.BaseModelWithImage;
import project.hikerguide.models.viewmodels.GuideViewModel;
import project.hikerguide.ui.adapters.EditGuideDetailsAdapter;
import timber.log.Timber;

import static android.support.v7.widget.helper.ItemTouchHelper.DOWN;
import static android.support.v7.widget.helper.ItemTouchHelper.LEFT;
import static android.support.v7.widget.helper.ItemTouchHelper.RIGHT;
import static android.support.v7.widget.helper.ItemTouchHelper.UP;
import static project.hikerguide.utilities.IntentKeys.AREA_KEY;
import static project.hikerguide.utilities.IntentKeys.AUTHOR_KEY;
import static project.hikerguide.utilities.IntentKeys.GUIDE_KEY;
import static project.hikerguide.utilities.IntentKeys.SECTION_KEY;
import static project.hikerguide.utilities.IntentKeys.TRAIL_KEY;
import static project.hikerguide.utilities.FirebaseProviderUtils.GPX_EXT;

/**
 * Created by Alvin on 7/27/2017.
 */

public class CreateGuideActivity extends MapboxActivity implements FabSpeedDial.MenuListener {
    // ** Constants ** //
    public static final int PERMISSION_REQUEST_EXT_STORAGE = 9687;

    // ** Member Variables ** //
    private Guide mGuide;
    private Area mArea;
    private Trail mTrail;
    private Author mAuthor;
    private Section[] mSections;

    private ActivityCreateGuideBinding mBinding;
    private EditGuideDetailsAdapter mAdapter;
    private List<BaseModel> mModelList;
    private ItemTouchHelper mItemTouchHelper;
    private int mFilePickerModelPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.d("onCreate");
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_create_guide);

        initRecyclerView();

        if (getIntent().getData() != null) {

            // Load saved data from database
        } else {

            // Retrieve the passed objects from the Intent
            mAuthor = getIntent().getParcelableExtra(AUTHOR_KEY);
            mArea = getIntent().getParcelableExtra(AREA_KEY);
            mTrail = getIntent().getParcelableExtra(TRAIL_KEY);

            // Start a new Guide
            mGuide = new Guide();

            // Add the information from the passed objects to the Guide
            mGuide.authorName = mAuthor.name;
            mGuide.area = mArea.name;
            mGuide.trailName = mTrail.name;

            // Setup the Adapter
            mModelList = new ArrayList<>();
            mAdapter.setModelList(mModelList);
            mAdapter.addModel(mGuide);
            mBinding.setVm(new GuideViewModel(this,mGuide));
        }

        setSupportActionBar(mBinding.guideDetailsTb);

        mBinding.fabDial.setMenuListener(this);
    }

    /**
     * Initializes the components required for the RecyclerView
     */
    private void initRecyclerView() {

        // Init the Adapter
        mAdapter = new EditGuideDetailsAdapter(this);

        // Set the LayoutManager and Adapter to the RecyclerView
        mBinding.guideDetailsRv.setLayoutManager(new LinearLayoutManager(this));
        mBinding.guideDetailsRv.setAdapter(mAdapter);

        // Init and set the ItemTouchHelper
        mItemTouchHelper = new ItemTouchHelper(mItemTouchCallback);
        mItemTouchHelper.attachToRecyclerView(mBinding.guideDetailsRv);
    }

    /**
     * Opens Android-FilePicker Activity so the user may select a File to add to the Guide
     *
     * @param type    Type of file to be selected. Either document or media.
     */
    public void openFilePicker(int type) {

        // Check to ensure the app has the required permissions first
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // Request permission to read external storage
            requestPermission(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_EXT_STORAGE);
        } else {

            // Launch the FilePicker based on the type of file to be added to the Guide
            switch (type) {
                case FilePickerConst.FILE_TYPE_DOCUMENT: {

                    FilePickerBuilder.getInstance()
                            .setMaxCount(1)
                            .addFileSupport(getString(R.string.gpx_label), new String[]{GPX_EXT})
                            .enableDocSupport(false)
                            .pickFile(this);

                    break;
            }

                case FilePickerConst.FILE_TYPE_MEDIA: {

                    FilePickerBuilder.getInstance()
                            .setMaxCount(1)
                            .enableVideoPicker(false)
                            .pickPhoto(this);

                    break;
                }
            }
        }

    }

    /**
     * Requests Android permission for the application
     *
     * @param permissions    The Permission(s) to request
     * @param requestCode    Code to request it with. For identifying the result.
     */
    public void requestPermission(String[] permissions, int requestCode) {

        ActivityCompat.requestPermissions(this,
                permissions,
                requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // Process the selected File based on the request code
        switch (requestCode) {
            case FilePickerConst.REQUEST_CODE_DOC: {
                if (resultCode == RESULT_OK) {

                    // Retrieve the path of the selected File
                    List<String> dataArray = data.getStringArrayListExtra(FilePickerConst.KEY_SELECTED_DOCS);

                    // Check to ensure the user selected  File
                    if (dataArray == null || dataArray.size() == 0) {
                        return;
                    }

                    String filePath = dataArray.get(0);

                    // Check to ensure the first item in the List is a Guide
                    if (mModelList.get(0) instanceof Guide) {

                        Guide guide = (Guide) mModelList.get(0);

                        // Set the gpxUri for the Guide based on the File at the selected path
                        guide.setGpxUri(new File(filePath));

                        EditGuideDetailsAdapter.EditViewHolder viewHolder =
                                ((EditGuideDetailsAdapter.EditViewHolder)mBinding.guideDetailsRv.findViewHolderForAdapterPosition(0));

                        if (viewHolder != null) {
                            ViewDataBinding binding = viewHolder.getBinding();

                            ((ListItemGuideDetailsBinding) binding).getVm().notifyPropertyChanged(BR.gpx);
                            ((ListItemGuideDetailsBinding) binding).getVm().notifyPropertyChanged(BR.distance);
                        }

                    }
                }

                break;
            }

            case FilePickerConst.REQUEST_CODE_PHOTO: {
                if (resultCode == RESULT_OK) {

                    // Retrieve the path of the selected Image
                    List<String> dataArray = data.getStringArrayListExtra(FilePickerConst.KEY_SELECTED_MEDIA);

                    // If nothing is selected, do nothing
                    if (dataArray == null || dataArray.size() == 0) {
                        mFilePickerModelPosition = -1;
                        return;
                    }

                    String imagePath = dataArray.get(0);

                    // mFilePickerModelPosition is the same as the size of the list, that means
                    // that the user has chosen to add a new Section with image
                    if (mFilePickerModelPosition == mModelList.size()) {

                        // Add the Section to mModel List
                        mModelList.add(new Section());
                    }

                    // Set the imageUri of the model selected
                    ((BaseModelWithImage) mModelList.get(mFilePickerModelPosition)).setImageUri(new File(imagePath));

                    // Notify the item based on the position of mFilePickerModelPosition of the change
                    if (mFilePickerModelPosition == 0) {
                        mBinding.getVm().notifyPropertyChanged(BR.imageUri);
                        mBinding.getVm().notifyPropertyChanged(BR.iconVisibility);

                        // Remove error icon from the ActionBar
                        getSupportActionBar().setIcon(null);
                    } else {
                        mAdapter.notifyItemChanged(mFilePickerModelPosition);
                    }

                    // Reset mFilePickerPosition to -1
                    mFilePickerModelPosition = -1;
                }

                break;
            }
        }
    }

    /**
     * For adding the hero image to the Guide
     */
    public void onHeroImageClick() {

        // Set the position to the first position as that is the only position the Guide should
        // exist in
        mFilePickerModelPosition = 0;

        // Open the FilePicker to allow the user to select the image they want to use
        openFilePicker(FilePickerConst.FILE_TYPE_MEDIA);
    }

    /**
     * Handles the clicking of an image of a Section by triggering the replacement of the image
     *
     * @param section    Section whose image is to be changed
     */
    public void onSectionImageClick(Section section) {

        // Set the mem var to the position of the Model in the Adapter
        mFilePickerModelPosition = mModelList.indexOf(section);

        if (mFilePickerModelPosition != -1) {
            // Open the FilePicker
            openFilePicker(FilePickerConst.FILE_TYPE_MEDIA);
        }
    }

    @Override
    public boolean onPrepareMenu(NavigationMenu navigationMenu) {
        return true;
    }

    @Override
    public boolean onMenuItemSelected(MenuItem menuItem) {

        switch (menuItem.getItemId()) {
            case R.id.fab_add_gpx:

                // Open FilePicker to allow GPX selection
                openFilePicker(FilePickerConst.FILE_TYPE_DOCUMENT);
                return true;

            case R.id.fab_add_section_text:

                // Add another blank section to mModelList
                mAdapter.addModel(new Section());
                return true;

            case R.id.fab_add_section_image:

                // Set mFilePickerPosition to the size of mModelList because that is the position
                // that the Section will exist in once it is added
                mFilePickerModelPosition = mModelList.size();

                // Open the FilePicker to allow selection of a photo to include
                openFilePicker(FilePickerConst.FILE_TYPE_MEDIA);
                return true;
        }

        return false;
    }

    @Override
    public void onMenuClosed() {

    }

    ItemTouchHelper.Callback mItemTouchCallback = new ItemTouchHelper.SimpleCallback(UP|DOWN, LEFT|RIGHT) {
        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            // Get the start/end positions
            int start = viewHolder.getAdapterPosition();
            int end = target.getAdapterPosition();

            // If the re-order involves the Guide, disallow it
            if (end == 0) {
                mAdapter.notifyItemMoved(end, start);
                return false;
            }

            // Move the Model within the Adapter
            if (start < end) {
                for (int i = start; i < end; i++) {
                    Collections.swap(mModelList, i, i + 1);
                }
            } else {
                for (int i = start; i > end; i--) {
                    Collections.swap(mModelList, i, i - 1);
                }
            }

            mAdapter.notifyItemMoved(start, end);
            return true;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

        }

        @Override
        public boolean isLongPressDragEnabled() {
            // Drag is handled by touch pads
            return false;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            // Delete is handled by delete icon
            return false;
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_create_guide, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_publish:
                boolean valid = true;

                boolean sectionsValid = validateSections();
                boolean guideValid = validateGuide();

                if (!sectionsValid || !guideValid) {
                    valid = false;
                }

                if (valid) {
                    Intent intent = new Intent(this, PublishActivity.class);
                    intent.putExtra(AUTHOR_KEY, mAuthor);
                    intent.putExtra(AREA_KEY, mArea);
                    intent.putExtra(TRAIL_KEY, mTrail);
                    intent.putExtra(GUIDE_KEY, mGuide);
                    intent.putExtra(SECTION_KEY, mSections);

                    startActivity(intent);
                }

                return true;

            case R.id.menu_save:

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Triggers the re-ordering function of the ItemTouchHelper
     *
     * @param model    Model that was selected to be re-ordered
     */
    public void reorderModel(BaseModel model) {

        // Get the position of the Model in the Adapter
        int position = mModelList.indexOf(model);

        // Trigger the startDrag() of the ItemTouchHelper, passing in the ViewHolder for the position
        mItemTouchHelper.startDrag(mBinding.guideDetailsRv.findViewHolderForAdapterPosition(position));
    }

    /**
     * Removes a model from the Adapter
     *
     * @param model    Model to be removed
     */
    public void removeModel(BaseModel model) {
        mAdapter.removeModel(model);
    }

    /**
     * Checks to ensure that all required items for the Guide model exists
     */
    private boolean validateGuide() {

        boolean valid = true;

        // Check that the hero image for the Guide has been set
        if (!mGuide.hasImage) {

            // Display an error icon on the ActionBar to indicate the missing image in case the
            // hero image is not visible due to CollapsingToolbar
            getSupportActionBar().setIcon(ContextCompat.getDrawable(this, R.drawable.ic_error_outline));

            // Show an error icon over the missing hero image
            mBinding.getVm().setShowImageError(true);

            valid = false;
        }

        // Check that the .gpx file has been set
        if (mGuide.getGpxUri() == null || mGuide.distance == 0) {

            // Scroll to the top so the user can see the missing gpx file error
            mBinding.guideDetailsRv.scrollToPosition(0);

            // Must be delayed slightly because the scroll operation takes time or else the
            // ViewHolder will not have been created by the Adapter
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {

                    // Get the ViewHolder for the Guide
                    EditGuideDetailsAdapter.EditViewHolder viewHolder =
                            (EditGuideDetailsAdapter.EditViewHolder) mBinding.guideDetailsRv.findViewHolderForAdapterPosition(0);

                    // Show the missing GPX error
                    ListItemGuideDetailsBinding binding = (ListItemGuideDetailsBinding) viewHolder.getBinding();
                    binding.getVm().setShowGpxError(true);
                }
            }, 100);

            valid = false;
        }

        return valid;
    }

    /**
     * Checks that all Sections have been filled out and removes any empty Sections
     */
    private boolean validateSections() {

        // Used to set the Section ordering
        int order = 0;

        // List of empty Sections with no content to be removed
        List<Section> emptySections = new ArrayList<>();

        // Iterate through each Section and check
        for (int i = 1; i < mModelList.size(); i++) {

            // Get reference to Section
            Section section = (Section) mModelList.get(i);

            // Check that Section is not empty
            if (section.content != null || section.hasImage) {

                // Add the ordering to the Section
                section.section = order;

                // Increment the ordering for the next Section
                order++;
            } else {

                // Empty Section, add it to the List of Section to be removed
                emptySections.add(section);
            }
        }

        // Remove each empty Section from the Adapter
        for (Section section : emptySections) {
            mAdapter.removeModel(section);
        }

        // Check that there is at least one valid Section
        if (mModelList.size() == 1) {

            // Show a Toast to indicate to the user to add a Section before publishing
            Toast.makeText(this, getString(R.string.create_guide_no_sections_error), Toast.LENGTH_LONG).show();
            return false;
        }

        // Initialize the memvar for Sections to be passed through
        mSections = new Section[mModelList.size() - 1];

        // Add all valid Sections to mSections
        for (int i = 1; i < mModelList.size(); i++) {
            mSections[i - 1] = (Section) mModelList.get(i);
        }

        return true;
    }
}

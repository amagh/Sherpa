package project.sherpa.ui.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import droidninja.filepicker.FilePickerConst;
import project.sherpa.BR;
import project.sherpa.R;
import project.sherpa.data.GuideContract;
import project.sherpa.data.GuideDatabase;
import project.sherpa.data.GuideProvider;
import project.sherpa.databinding.ActivityCreateGuideBinding;
import project.sherpa.databinding.ListItemGuideDetailsEditBinding;
import project.sherpa.databinding.ListItemSectionImageEditBinding;
import project.sherpa.databinding.ListItemSectionTextEditBinding;
import project.sherpa.models.datamodels.Area;
import project.sherpa.models.datamodels.Author;
import project.sherpa.models.datamodels.Guide;
import project.sherpa.models.datamodels.Section;
import project.sherpa.models.datamodels.Trail;
import project.sherpa.models.datamodels.abstractmodels.BaseModel;
import project.sherpa.models.datamodels.abstractmodels.BaseModelWithImage;
import project.sherpa.models.viewmodels.GuideViewModel;
import project.sherpa.ui.adapters.EditGuideDetailsAdapter;
import project.sherpa.ui.dialogs.DeleteDialog;
import project.sherpa.ui.dialogs.PublishDialog;
import project.sherpa.ui.dialogs.SaveDialog;
import project.sherpa.utilities.ContentProviderUtils;
import project.sherpa.utilities.DataCache;
import project.sherpa.utilities.GeneralUtils;

import static android.support.v7.widget.helper.ItemTouchHelper.DOWN;
import static android.support.v7.widget.helper.ItemTouchHelper.LEFT;
import static android.support.v7.widget.helper.ItemTouchHelper.RIGHT;
import static android.support.v7.widget.helper.ItemTouchHelper.UP;
import static project.sherpa.ui.activities.CreateGuideActivity.BUNDLE_KEYS.FIREBASE_ID_KEY;
import static project.sherpa.utilities.Constants.IntentKeys.AREA_KEY;
import static project.sherpa.utilities.Constants.IntentKeys.AUTHOR_KEY;
import static project.sherpa.utilities.Constants.IntentKeys.GUIDE_KEY;
import static project.sherpa.utilities.Constants.IntentKeys.TRAIL_KEY;
import static project.sherpa.utilities.Constants.RequestCodes.REQUEST_CODE_PUBLISH;

/**
 * Created by Alvin on 7/27/2017.
 */

public class CreateGuideActivity extends MapboxActivity implements ConnectivityActivity.ConnectivityCallback,
        LoaderManager.LoaderCallbacks<Cursor> {

    // ** Constants ** //
    private static final int LOADER_GUIDE_DRAFT             = 7912;
    private static final int LOADER_AREA_DRAFT              = 9519;
    private static final int LOADER_TRAIL_DRAFT             = 7269;
    private static final int LOADER_SECTION_DRAFT           = 6385;
    private static final int LOADER_AUTHOR_DRAFT            = 2614;

    interface BUNDLE_KEYS {
        String FIREBASE_ID_KEY = "firebaseId";
    }

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
    private MenuItem mPublishMenuItem;

    private boolean mSaved = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_create_guide);

        initRecyclerView();

        if (savedInstanceState != null) {

            // Retrieve the FirebaseIds of the items to restore
            String guideId = savedInstanceState.getString(GUIDE_KEY);
            String authorId = savedInstanceState.getString(AUTHOR_KEY);
            String areaId = savedInstanceState.getString(AREA_KEY);
            String trailId = savedInstanceState.getString(TRAIL_KEY);

            // Retrieve items from cache
            mGuide = (Guide) DataCache.getInstance().get(guideId);
            mAuthor = (Author) DataCache.getInstance().get(authorId);
            mArea = (Area) DataCache.getInstance().get(areaId);
            mTrail = (Trail) DataCache.getInstance().get(trailId);
            mSections = DataCache.getInstance().getSections(guideId);

            // Set the ViewModel
            GuideViewModel vm = new GuideViewModel(this, mGuide);
            mBinding.setVm(vm);

            // Add Guide and Sections to Adapter
            mAdapter.addModel(mGuide);

            if (mSections != null) {
                for (Section section : mSections) {
                    mAdapter.addModel(section);
                }
            }
        } else {
            if (getIntent().getData() != null) {

                // Load mSaved data from database
                Bundle guideBundle = new Bundle();
                guideBundle.putString(FIREBASE_ID_KEY, GuideProvider.getIdFromUri(getIntent().getData()));
                getSupportLoaderManager().initLoader(LOADER_GUIDE_DRAFT, guideBundle, this);

            } else {

                // Retrieve the passed objects from the Intent
                mAuthor = getIntent().getParcelableExtra(AUTHOR_KEY);
                mArea = getIntent().getParcelableExtra(AREA_KEY);
                mTrail = getIntent().getParcelableExtra(TRAIL_KEY);

                // Start a new Guide
                mGuide = new Guide();

                // Add the information from the passed objects to the Guide
                mGuide.authorId = mAuthor.firebaseId;
                mGuide.authorName = mAuthor.name;
                mGuide.area = mArea.name;

                if (mTrail.firebaseId != null) {
                    mGuide.trailId = mTrail.firebaseId;
                }

                mGuide.trailName = mTrail.name;

                // Setup the Adapter
                mAdapter.addModel(mGuide);
                mBinding.setVm(new GuideViewModel(this, mGuide));
            }
        }

        setSupportActionBar(mBinding.guideDetailsTb);
    }

    @Override
    public void onConnected() {

        // Enable option to publish when connected to network
        mPublishMenuItem.setVisible(true);
    }

    @Override
    public void onDisconnected() {

        // Disable option to publish if there is no network connection
        mPublishMenuItem.setVisible(false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Cache data
        cacheData();

        // Add the FirebaseIds to the SavedInstanceState so they can be restored
        outState.putString(GUIDE_KEY, mGuide.firebaseId);
        outState.putString(AUTHOR_KEY, mAuthor.firebaseId);
        outState.putString(AREA_KEY, mArea.firebaseId);
        outState.putString(TRAIL_KEY, mTrail.firebaseId);
    }

    /**
     * Initializes the components required for the RecyclerView
     */
    private void initRecyclerView() {

        // Init the Adapter
        mAdapter = new EditGuideDetailsAdapter(this);

        // Set the List used to populate the Adapter
        mModelList = new ArrayList<>();
        mAdapter.setModelList(mModelList);

        // Set the LayoutManager and Adapter to the RecyclerView
        mBinding.guideDetailsRv.setLayoutManager(new LinearLayoutManager(this));
        mBinding.guideDetailsRv.setAdapter(mAdapter);

        // Init and set the ItemTouchHelper
        mItemTouchHelper = new ItemTouchHelper(mItemTouchCallback);
        mItemTouchHelper.attachToRecyclerView(mBinding.guideDetailsRv);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        mSaved = false;

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

                            ((ListItemGuideDetailsEditBinding) binding).getVm().notifyPropertyChanged(BR.gpx);
                            ((ListItemGuideDetailsEditBinding) binding).getVm().notifyPropertyChanged(BR.distance);
                            ((ListItemGuideDetailsEditBinding) binding).getVm().notifyPropertyChanged(BR.elevationVisibility);
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
                        mBinding.getVm().notifyPropertyChanged(BR.image);
                        mBinding.getVm().notifyPropertyChanged(BR.iconVisibility);

                        // Remove error icon from the ActionBar
                        getSupportActionBar().setIcon(null);
                    } else {
                        mAdapter.notifyItemChanged(mFilePickerModelPosition);
                    }

                    // Check if the image is being selected for the GuideDetails or a Section
                    if (mFilePickerModelPosition != 0) {

                        // Image is for a Section. Scroll to the bottom so the new Section is
                        // visible to the Author
                        mBinding.guideDetailsRv.scrollToPosition(mModelList.size() - 1);

                        // Get the position to be scrolled to
                        final int position = mFilePickerModelPosition;

                        // Set the focus on the Caption TextView. Needs to be delayed to prevent
                        // crashing due to attempting to selecting a ViewHolder before it has been
                        // created.
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                EditGuideDetailsAdapter.EditViewHolder viewHolder =
                                        (EditGuideDetailsAdapter.EditViewHolder) mBinding.guideDetailsRv
                                                .findViewHolderForAdapterPosition(position);

                                ((ListItemSectionImageEditBinding) viewHolder.getBinding()).listSectionImageCaptionTv.requestFocus();
                            }
                        }, 50);
                    }

                    // Reset mFilePickerPosition to -1
                    mFilePickerModelPosition = -1;
                }

                break;
            }

            case REQUEST_CODE_PUBLISH:
                if (resultCode == RESULT_OK) {

                    // Retrieve Guide that was just published
                    mGuide = data.getParcelableExtra(GUIDE_KEY);

                    // Start GuideDetailsActivity for mGuide
                    Intent intent = new Intent(this, GuideDetailsActivity.class);
                    intent.putExtra(GUIDE_KEY, mGuide);

                    startActivity(intent);

                    // Close this Activity
                    finish();
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
        GeneralUtils.openFilePicker(this, GeneralUtils.DEFAULT_REQUEST_CODE, FilePickerConst.FILE_TYPE_MEDIA);
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
            GeneralUtils.openFilePicker(this, GeneralUtils.DEFAULT_REQUEST_CODE, FilePickerConst.FILE_TYPE_MEDIA);
        }
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

            mSaved = false;
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

        mPublishMenuItem = menu.getItem(0);

        // Begin listening to network status
        addConnectivityCallback(this);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_publish:

                // Show Dialog confirming user wants to publish
                PublishDialog publishDialog = new PublishDialog();
                publishDialog.setOnClickListener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        // Validate the Sections and Guide to ensure all required elements are present
                        boolean sectionsValid = validateSections();
                        boolean guideValid = validateGuide();

                        if (sectionsValid && guideValid) {

                            // Save the items to the DataCache
                            cacheData();

                            // Start the PublishActivity and send all elements through the Intent
                            Intent intent = new Intent(CreateGuideActivity.this, PublishActivity.class);
                            intent.putExtra(AUTHOR_KEY, mAuthor.firebaseId);
                            intent.putExtra(AREA_KEY, mArea.firebaseId);
                            intent.putExtra(TRAIL_KEY, mTrail.firebaseId);
                            intent.putExtra(GUIDE_KEY, mGuide.firebaseId);

                            startActivityForResult(intent, REQUEST_CODE_PUBLISH);
                        }
                    }
                });

                publishDialog.show(getSupportFragmentManager(), null);

                return true;

            case R.id.menu_save_draft:
                saveGuide();

                return true;

            case R.id.menu_delete_draft:

                // Show a Dialog confirming the user wants to delete the Guide
                DeleteDialog deleteDialog = new DeleteDialog();
                deleteDialog.setOnClickListener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        deleteDraft();
                    }
                });

                deleteDialog.show(getSupportFragmentManager(), null);

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        // Variables for building CursorLoader
        Uri uri = null;
        String selection = null;
        String[] selectionArgs = null;
        String sortOrder = null;

        // Set the variables according to the LoaderID
        switch (id) {
            case LOADER_GUIDE_DRAFT:
                uri = GuideProvider.Guides.CONTENT_URI;
                selection = GuideContract.GuideEntry.FIREBASE_ID + " = ?";
                selectionArgs = new String[] {args.getString(FIREBASE_ID_KEY, null)};

                break;

            case LOADER_AREA_DRAFT:
                uri = GuideProvider.Areas.CONTENT_URI;
                selection = GuideContract.AreaEntry.FIREBASE_ID + " = ?";
                selectionArgs = new String[] {args.getString(FIREBASE_ID_KEY, null)};

                break;

            case LOADER_TRAIL_DRAFT:
                uri = GuideProvider.Trails.CONTENT_URI;
                selection = GuideContract.TrailEntry.FIREBASE_ID + " = ?";
                selectionArgs = new String[] {args.getString(FIREBASE_ID_KEY, null)};

                break;

            case LOADER_SECTION_DRAFT:
                uri = GuideProvider.Sections.CONTENT_URI;
                selection = GuideContract.SectionEntry.GUIDE_ID + " = ?";
                selectionArgs = new String[] {args.getString(FIREBASE_ID_KEY, null)};
                sortOrder = GuideContract.SectionEntry.SECTION + " ASC";

                break;

            case LOADER_AUTHOR_DRAFT:
                uri = GuideProvider.Authors.CONTENT_URI;
                selection = GuideContract.AuthorEntry.FIREBASE_ID + " = ?";
                selectionArgs = new String[] {args.getString(FIREBASE_ID_KEY, null)};

                break;
        }

        return new CursorLoader(
                this,
                uri,
                null,
                selection,
                selectionArgs,
                sortOrder
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (data != null) {
            switch (loader.getId()) {
                case LOADER_GUIDE_DRAFT:

                    if (mModelList.size() == 0 && data.moveToFirst()) {

                        // Create a Guide from the Cursor
                        mGuide = Guide.createGuideFromCursor(data);

                        // Set the Guide to the Adapter and the DataBinding ViewModel
                        mBinding.setVm(new GuideViewModel(this, mGuide));
                        mAdapter.addModel(mGuide);

                        // Init the CursorLoaders for the associated Sections, Trail, and Author
                        Bundle sectionBundle = new Bundle();
                        sectionBundle.putString(FIREBASE_ID_KEY, GuideProvider.getIdFromUri(getIntent().getData()));
                        getSupportLoaderManager().initLoader(LOADER_SECTION_DRAFT, sectionBundle, this);

                        Bundle trailBundle = new Bundle();
                        trailBundle.putString(FIREBASE_ID_KEY, mGuide.trailId);
                        getSupportLoaderManager().initLoader(LOADER_TRAIL_DRAFT, trailBundle, this);

                        Bundle authorBundle = new Bundle();
                        authorBundle.putString(FIREBASE_ID_KEY, mGuide.authorId);
                        getSupportLoaderManager().initLoader(LOADER_AUTHOR_DRAFT, authorBundle, this);
                    }

                    break;

                case LOADER_AREA_DRAFT:
                    if (data.moveToFirst()) {

                        // Set the memvar to the Area created from the Cursor
                        mArea = Area.createAreaFromCursor(data);
                    }

                    break;

                case LOADER_TRAIL_DRAFT:
                    if (data.moveToFirst()) {

                        // Set the memvar to the Trail created by the Cursor
                        mTrail = Trail.createTrailFromCursor(data);

                        // Init the CursorLoader for the Area
                        Bundle areaBundle = new Bundle();
                        areaBundle.putString(FIREBASE_ID_KEY, mTrail.areaId);
                        getSupportLoaderManager().initLoader(LOADER_AREA_DRAFT, areaBundle, this);
                    }

                    break;

                case LOADER_SECTION_DRAFT:

                    // Add each Section created from the Cursor to the Adapter
                    if (mModelList.size() == 1 && data.moveToFirst()) {

                        do {
                            Section section = Section.createSectionFromCursor(data);
                            mAdapter.addModel(section);
                        } while (data.moveToNext());
                    }

                    break;

                case LOADER_AUTHOR_DRAFT:
                    if (data.moveToFirst()) {

                        // Set the memvar to the Author created from the Cursor
                        mAuthor = Author.createAuthorFromCursor(data);
                    }

                    break;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    /**
     * Click response for bottom Toolbar for adding an additional Section
     *
     * @param view    View that was clicked
     */
    public void onClickAddSection(View view) {
        // Add another blank section to mModelList
        mAdapter.addModel(new Section());

        mBinding.guideDetailsRv.scrollToPosition(mModelList.size() - 1);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                EditGuideDetailsAdapter.EditViewHolder viewHolder =
                        (EditGuideDetailsAdapter.EditViewHolder) mBinding.guideDetailsRv
                                .findViewHolderForAdapterPosition(mModelList.size() - 1);

                ((ListItemSectionTextEditBinding) viewHolder.getBinding()).listSectionTextTv.requestFocus();
            }
        }, 50);

        mSaved = false;
    }

    /**
     * Click response for bottom Toolbar for adding an additional Section with Image
     *
     * @param view    View that was clicked
     */
    public void onClickAddSectionWithImage(View view) {

        // Set mFilePickerPosition to the size of mModelList because that is the position
        // that the Section will exist in once it is added
        mFilePickerModelPosition = mModelList.size();

        // Open the FilePicker to allow selection of a photo to include
        GeneralUtils.openFilePicker(this, GeneralUtils.DEFAULT_REQUEST_CODE, FilePickerConst.FILE_TYPE_MEDIA);

        mSaved = false;
    }

    /**
     * Click response for bottom Toolbar for adding a GPX to the Guide
     *
     * @param view    View that was clicked
     */
    public void onClickAddGpx(View view) {

        // Open FilePicker to allow GPX selection
        GeneralUtils.openFilePicker(this, GeneralUtils.DEFAULT_REQUEST_CODE, FilePickerConst.FILE_TYPE_DOCUMENT);
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

        mSaved = false;
    }

    /**
     * Checks to ensure that all required items for the Guide model exists
     */
    private boolean validateGuide() {

        boolean valid = true;

        if (mGuide.difficulty == 0) {
            Toast.makeText(
                    this,
                    getString(R.string.create_guide_difficulty_missing_error_text),
                    Toast.LENGTH_LONG)
                    .show();

            valid = false;
        }

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
                    ListItemGuideDetailsEditBinding binding = (ListItemGuideDetailsEditBinding) viewHolder.getBinding();
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

    /**
     * Saves the elements for the Guide to the local database so that they can be accessed and
     * edited at a later time.
     */
    private void saveGuide() {

        // Remove previous entries from the database
        ContentProviderUtils.deleteModel(this, mGuide);
        ContentProviderUtils.deleteModel(this, mArea);
        ContentProviderUtils.deleteModel(this, mTrail);

        ContentProviderUtils.deleteSectionsForGuide(this, mGuide);

        // Get a FirebaseId for each element that does not already have one and set the FirebaseId
        // of each element that depends on another element's FirebaseId.
        // e.g. Set the trailId of the Guide to the trail's FirebaseId
        if (mArea != null) {
            if (mArea.firebaseId == null || mArea.firebaseId.equals(AREA_KEY)) {
                mArea.firebaseId = FirebaseDatabase.getInstance().getReference()
                        .child(GuideDatabase.AREAS)
                        .push()
                        .getKey();

                mArea.setDraft(true);
            }

            ContentProviderUtils.insertModel(this, mArea);
        }


        if (mTrail != null) {
            if (mTrail.firebaseId == null || mTrail.firebaseId.equals(TRAIL_KEY)) {
                mTrail.firebaseId = FirebaseDatabase.getInstance().getReference()
                        .child(GuideDatabase.TRAILS)
                        .push()
                        .getKey();

                mTrail.setDraft(true);
            }

            mTrail.areaId = mArea.firebaseId;

            ContentProviderUtils.insertModel(this, mTrail);
        }

        if (mGuide != null) {
            if (mGuide.firebaseId == null || mGuide.firebaseId.equals(GUIDE_KEY)) {
                mGuide.firebaseId = FirebaseDatabase.getInstance().getReference()
                        .child(GuideDatabase.GUIDES)
                        .push()
                        .getKey();

                mGuide.setDraft(true);
            }

            mGuide.trailId = mTrail.firebaseId;
            mGuide.authorId = mAuthor.firebaseId;

            ContentProviderUtils.insertModel(this, mGuide);
        }

        if (mAuthor != null) {

            if (!ContentProviderUtils.isModelInDatabase(this, mAuthor)) {

                ContentProviderUtils.insertModel(this, mAuthor);
            }
        }

        // Add the Section numbering to each Section
        validateSections();

        if (mSections != null) {
            for (Section section : mSections) {
                section.guideId = mGuide.firebaseId;
                section.setDraft(true);
            }

            ContentProviderUtils.bulkInsertSections(this, mSections);
        }

        // Notify the user of the save operation completion
        Toast.makeText(
                this,
                getString(R.string.draft_saved),
                Toast.LENGTH_LONG)
                .show();;

        mSaved = true;
    }

    /**
     * Deletes the draft and all associated entries from the database
     */
    private void deleteDraft() {
        // Remove entries from the database
        ContentProviderUtils.deleteModel(this, mGuide);
        ContentProviderUtils.deleteModel(this, mTrail);

        if (ContentProviderUtils.getTrailCountForArea(this, mArea) == 0) {
            ContentProviderUtils.deleteModel(this, mArea);
        }

        if (ContentProviderUtils.getGuideCountForAuthor(this, mAuthor) == 0) {
            ContentProviderUtils.deleteModel(this, mAuthor);
        }

        ContentProviderUtils.deleteSectionsForGuide(this, mGuide);

        // Notify the user of the deletion
        Toast.makeText(
                this,
                getString(R.string.draft_deleted),
                Toast.LENGTH_LONG)
                .show();

        // Close the Activity
        finish();
    }

    /**
     * Stores the data to be published in the DataCache
     */
    private void cacheData() {

        // Add temporary FirebaseId to any data model that doesn't have one yet
        if (mGuide.firebaseId == null) {
            mGuide.firebaseId = GUIDE_KEY;
        }

        // Instantiate mSections
        validateSections();

        if (mSections != null) {
            for (Section section : mSections) {
                section.guideId = mGuide.firebaseId;
            }

            DataCache.getInstance().store(mSections);
        }

        if (mTrail.firebaseId == null) {
            mTrail.firebaseId = TRAIL_KEY;
        }

        if (mArea.firebaseId == null) {
            mArea.firebaseId = AREA_KEY;
        }

        // Cache the data
        DataCache.getInstance().store(mGuide);
        DataCache.getInstance().store(mTrail);
        DataCache.getInstance().store(mArea);
        DataCache.getInstance().store(mAuthor);
    }

    @Override
    public void onBackPressed() {

        // Check whether the Guide has been saved as a draft yet
        if (mSaved) {

            // Proceed to exit normally
            super.onBackPressed();
        } else {

            // Show a Dialog to confirm the user wants to exit without saving progress
            SaveDialog dialog = new SaveDialog();
            dialog.setOnClickListener(new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                    // User selected to exit without saving
                    mSaved = true;
                    onBackPressed();
                }
            });

            dialog.show(getSupportFragmentManager(), null);
        }
    }
}

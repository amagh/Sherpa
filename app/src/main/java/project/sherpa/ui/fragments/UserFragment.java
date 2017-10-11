package project.sherpa.ui.fragments;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.internal.NavigationMenu;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.databinding.library.baseAdapters.BR;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import droidninja.filepicker.FilePickerConst;
import io.github.yavski.fabspeeddial.FabSpeedDial;
import project.sherpa.R;
import project.sherpa.data.GuideContract;
import project.sherpa.data.GuideDatabase;
import project.sherpa.databinding.FragmentUserBinding;
import project.sherpa.models.datamodels.Author;
import project.sherpa.models.datamodels.Chat;
import project.sherpa.models.datamodels.Guide;
import project.sherpa.models.datamodels.abstractmodels.BaseModel;
import project.sherpa.models.viewmodels.AuthorViewModel;
import project.sherpa.models.viewmodels.UserFragmentViewModel;
import project.sherpa.services.firebaseservice.FirebaseProviderService;
import project.sherpa.services.firebaseservice.ModelChangeListener;
import project.sherpa.services.firebaseservice.QueryChangeListener;
import project.sherpa.ui.activities.AccountActivity;
import project.sherpa.ui.activities.FriendFollowActivity;
import project.sherpa.ui.activities.GuideDetailsActivity;
import project.sherpa.ui.activities.MainActivity;
import project.sherpa.ui.activities.MessageActivity;
import project.sherpa.ui.activities.OpenDraftActivity;
import project.sherpa.ui.activities.SelectAreaTrailActivity;
import project.sherpa.ui.adapters.AuthorDetailsAdapter;
import project.sherpa.ui.adapters.GuideAdapter;
import project.sherpa.ui.adapters.interfaces.ClickHandler;
import project.sherpa.ui.behaviors.FabSpeedDialScrollBehavior;
import project.sherpa.ui.dialogs.ProgressDialog;
import project.sherpa.ui.fragments.abstractfragments.ConnectivityFragment;
import project.sherpa.utilities.ContentProviderUtils;
import project.sherpa.utilities.DataCache;
import project.sherpa.utilities.FirebaseProviderUtils;
import project.sherpa.utilities.SaveUtils;
import project.sherpa.widgets.FavoritesWidgetUpdateService;
import project.sherpa.services.firebaseservice.FirebaseProviderService.*;
import timber.log.Timber;

import static android.app.Activity.RESULT_OK;
import static junit.framework.Assert.assertNotNull;
import static project.sherpa.utilities.Constants.IntentKeys.AUTHOR_KEY;
import static project.sherpa.utilities.Constants.IntentKeys.CHAT_KEY;
import static project.sherpa.utilities.Constants.IntentKeys.GUIDE_KEY;
import static project.sherpa.utilities.Constants.RequestCodes.REQUEST_CODE_BACKDROP;
import static project.sherpa.utilities.Constants.RequestCodes.REQUEST_CODE_PROFILE_PIC;
import static project.sherpa.utilities.FirebaseProviderUtils.BACKDROP_SUFFIX;
import static project.sherpa.utilities.FirebaseProviderUtils.FirebaseType.AUTHOR;
import static project.sherpa.utilities.FirebaseProviderUtils.FirebaseType.CHAT;
import static project.sherpa.utilities.FirebaseProviderUtils.FirebaseType.GUIDE;
import static project.sherpa.utilities.FirebaseProviderUtils.IMAGE_PATH;
import static project.sherpa.utilities.FirebaseProviderUtils.JPEG_EXT;

/**
 * Created by Alvin on 10/2/2017.
 */

public class UserFragment extends ConnectivityFragment implements FabSpeedDial.MenuListener {

    // ** Constants ** //
    public static final int ACCOUNT_ACTIVITY_REQUEST_CODE = 7219;
    private static final String MODEL_LIST_KEY = "models";

    // ** Member Variables ** //
    private FragmentUserBinding mBinding;
    private Author mAuthor;
    private AuthorDetailsAdapter mAdapter;
    private List<BaseModel> mModelList;

    private Map<String, ModelChangeListener> mListenerMap = new HashMap<>();
    private QueryChangeListener<Guide> mGuideQueryListener;

    /**
     * Factory pattern for creating a new instance of the Fragment
     *
     * @param userId    The FirebaseId of the user whose details are to be loaded by the Fragment
     * @return A UserFragment set to load the Firebase profile for the user in the signature
     */
    public static UserFragment newInstance(String userId) {

        // Init a Bundle and pass the FirebaseId of the user to be loaded
        Bundle args = new Bundle();
        args.putString(AUTHOR_KEY, userId);

        // Create a new Fragment and attach the Bundle
        UserFragment fragment = new UserFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_user, container, false);
        bindFirebaseProviderService(true);

        initFabSpeedDialer();
        initRecyclerView();

        if (savedInstanceState != null) {

            // Restore mModelList from SavedInstanceState
            ArrayList<String> modelIdList = savedInstanceState.getStringArrayList(MODEL_LIST_KEY);

            if (modelIdList != null && mModelList.size() == 0) {
                for (String modelId : modelIdList) {
                    if (mAuthor == null) {
                        Author author = (Author) DataCache.getInstance().get(modelId);

                        setAuthor(author);
                        continue;
                    }

                    Guide guide = (Guide) DataCache.getInstance().get(modelId);
                    mAdapter.addModel(guide);
                }
            }

            // Hide the ProgressBar because items will be loaded from SavedInstanceState
            mBinding.userPb.setVisibility(View.GONE);
        }

        setHasOptionsMenu(true);
        loadAdViewModel(mBinding);

        return mBinding.getRoot();
    }

    /**
     * Initializes the FABSpeedDialer and its components
     */
    private void initFabSpeedDialer() {
        mBinding.setVm(new AuthorViewModel((AppCompatActivity) getActivity(), new Author()));
        mBinding.fabDial.setMenuListener(this);
        mBinding.fabDial.getChildAt(0).setContentDescription(getString(R.string.content_description_create_fab));
    }

    /**
     * Initializes components for RecyclerView to function
     */
    private void initRecyclerView() {

        mAdapter = new AuthorDetailsAdapter(new ClickHandler<Guide>() {
            @Override
            public void onClick(Guide guide) {

                // Start the Activity to display Guide details
                Intent intent = new Intent(getActivity(), GuideDetailsActivity.class);
                intent.putExtra(GUIDE_KEY, guide.firebaseId);
                intent.putExtra(AUTHOR_KEY, guide.authorId);
                startActivity(intent);
            }
        });

        // Set the LayoutManager and Adapter
        mBinding.userRv.setAdapter(mAdapter);
        mBinding.userRv.setLayoutManager(new StaggeredGridLayoutManager(
                getResources().getInteger(R.integer.guide_columns),
                StaggeredGridLayoutManager.VERTICAL));

        // Init the List of Models
        mModelList = new ArrayList<>();

        // Set the List in the Adapter
        mAdapter.setModelList(mModelList);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Start listening for changes in the data
        for (ModelChangeListener listener : mListenerMap.values()) {
            mService.registerModelChangeListener(listener);
        }

        if (mGuideQueryListener != null) mService.registerQueryChangeListener(mGuideQueryListener);

        // Reset the message icon
        if (mBinding.getUfvm() != null) mBinding.getUfvm().setHasNewMessages(false);
        mBinding.getVm().notifyPropertyChanged(BR._all);
    }

    @Override
    public void onPause() {
        super.onPause();

        // Stop listening for changes in the data
        for (ModelChangeListener listener : mListenerMap.values()) {
            mService.unregisterModelChangeListener(listener);
        }

        if (mGuideQueryListener != null) mService.unregisterQueryChangeListener(mGuideQueryListener);
    }

    @Override
    protected void onServiceConnected() {

        Bundle args = UserFragment.this.getArguments();
        String userId = null;

        if (args == null) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) userId = user.getUid();

        } else {
            userId = args.getString(AUTHOR_KEY, null);
        }

        if (userId == null) {
            Intent intent = new Intent(getActivity(), AccountActivity.class);
            startActivityForResult(intent, ACCOUNT_ACTIVITY_REQUEST_CODE);
        } else {
            loadUserSelfProfile();
            loadUserProfile(userId);
        }
    }

    /**
     * Loads the logged in user's Firebase profile to check if the Guides created by mAuthor have
     * been favorite'd by the user.
     */
    private void loadUserSelfProfile() {

        // Check to ensure user is logged int
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) return;

        // Get the data for the logged in user
        ModelChangeListener<Author> listener = new ModelChangeListener<Author>(AUTHOR, user.getUid()) {
            @Override
            public void onModelReady(Author model) {

                // Set the user to the Adapter so the favorites can be indicated correctly
                mAdapter.setUser(model);
            }

            @Override
            public void onModelChanged() {

            }
        };

        mService.registerModelChangeListener(listener);
        mListenerMap.put(user.getUid(), listener);
    }

    /**
     * Loads a user's Firebase profile
     *
     * @param userId    FirebaseId of the user to load
     */
    private void loadUserProfile(final String userId) {

        // Only load the user's profile if it hasn't already been loaded
        if (mListenerMap.get(userId)!= null && mAuthor != null) return;

        ModelChangeListener<Author> listener = new ModelChangeListener<Author>(AUTHOR, userId) {
            @Override
            public void onModelReady(Author author) {

                if (mAuthor == null) setAuthor(author);

                // Load the Guides that the Author has created
                loadGuidesForAuthor(mAuthor);

                if (mAuthor.firebaseId.equals(getFirebaseId())) {
                    setChatListeners();
                }

                mBinding.userPb.setVisibility(View.GONE);
            }

            @Override
            public void onModelChanged() {
                updateAuthor();
            }
        };

        mService.registerModelChangeListener(listener);
        mListenerMap.put(userId, listener);
    }

    /**
     * Sets the Author's details to be displayed by the Fragment
     *
     * @param author    Author whose details are to be displayed
     */
    private void setAuthor(Author author) {

        // Set memvar
        mAuthor = author;

        // Build the ViewModel that will be shared between the Fragment and the Adapter
        AuthorViewModel vm = new AuthorViewModel((AppCompatActivity) getActivity(), mAuthor);
        UserFragmentViewModel ufvm = new UserFragmentViewModel(mAuthor, this);
        mBinding.setVm(vm);
        mBinding.setUfvm(ufvm);
        mAdapter.setAuthorViewModel(vm);

        // Add the author to the Adapter
        mAdapter.addModel(mAuthor);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (mAuthor != null && user != null && user.getUid().equals(mAuthor.firebaseId)) {
            setupForSelfProfile();
        }
    }

    /**
     * Forces the binding to refresh its Views
     */
    private void updateAuthor() {
        mBinding.getVm().notifyPropertyChanged(BR._all);
    }

    /**
     * Loads the guides for the Author whose profile is being shown
     *
     * @param author    Author to load the guides for
     */
    private void loadGuidesForAuthor(Author author) {

        // Skip loading the guides if they have already been set
        if (mGuideQueryListener != null) return;

        // Build a query to find all guides Authored by the author
        Query guideQuery = FirebaseDatabase.getInstance().getReference()
                .child(GuideDatabase.GUIDES)
                .orderByChild(GuideContract.GuideEntry.AUTHOR_ID)
                .equalTo(author.firebaseId);

        mGuideQueryListener = new QueryChangeListener<Guide>(GUIDE, guideQuery, author.firebaseId) {
            @Override
            public void onQueryChanged(Guide[] guides) {
                mBinding.userPb.setVisibility(View.GONE);

                if (guides == null) return;
                for (Guide guide : guides) {
                    mAdapter.addModel(guide);
                }
            }
        };

        mService.registerQueryChangeListener(mGuideQueryListener);
    }

    /**
     * Sets up the layouts to be appropriate for someone viewing their own profile
     */
    private void setupForSelfProfile() {

        // Add the SupportActionBar so the menu items can be created, but remove the title
        if (getActivity() != null) {
            ((AppCompatActivity) getActivity()).setSupportActionBar(mBinding.toolbar);
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(null);
        }

        // Set the layout behavior for the FAB
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) mBinding.fabDial.getLayoutParams();
        params.setBehavior(new FabSpeedDialScrollBehavior());
    }

    /**
     * Sets ModelChangeListeners for each of the Chats that the user is a part of to notify the
     * user if they have a new message
     */
    private void setChatListeners() {

        for (String chatId : mAuthor.getChats()) {
            if (mListenerMap.get(chatId) != null) return;

            mListenerMap.put(chatId, new ModelChangeListener<Chat>(CHAT, chatId) {
                @Override
                public void onModelReady(Chat chat) {
                    int localMessageCount = ContentProviderUtils.getMessageCount(getActivity(), chat.firebaseId);
                    int firebaseMessageCount = chat.getMessageCount();

                    if (firebaseMessageCount > localMessageCount) {
                        mBinding.getUfvm().setHasNewMessages(true);
                    }
                }

                @Override
                public void onModelChanged() {
                    int localMessageCount = ContentProviderUtils.getMessageCount(getActivity(), getModel().firebaseId);
                    int firebaseMessageCount = getModel().getMessageCount();

                    if (firebaseMessageCount > localMessageCount) {
                        mBinding.getUfvm().setHasNewMessages(true);
                    }
                }
            });

            mService.registerModelChangeListener(mListenerMap.get(chatId));
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_user, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_user_log_off:
                // Clean the database to start fresh
                ContentProviderUtils.cleanDatabase(getActivity(), new Author());

                FavoritesWidgetUpdateService.updateWidgets(getActivity());

                // Log the User out
                FirebaseAuth.getInstance().signOut();

                // Start the AccountActivity
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new UserFragment())
                        .commit();

                return true;

            case R.id.menu_user_edit:
                switchAuthorLayout();
        }

        return false;
    }

    @Override
    public void onActivityResult(final int requestCode, int resultCode, Intent data) {

        if (requestCode == ACCOUNT_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data.getBooleanExtra(AUTHOR_KEY, false)) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    loadUserSelfProfile();
                    loadUserProfile(user.getUid());
                }
            } else {
                // Handle the log out based on the Activity the Fragment is in
                if (getActivity() instanceof MainActivity) {

                    // Switch Fragments
                    ((MainActivity) getActivity()).switchFragments(R.id.navigation_home);
                } else {

                    // Close the Activity
                    getActivity().finish();
                }
            }

        } else if (requestCode == REQUEST_CODE_PROFILE_PIC || requestCode == REQUEST_CODE_BACKDROP) {

            if (resultCode != RESULT_OK) {
                return;
            }

            // Retrieve the path of the selected Image
            List<String> dataArray = data.getStringArrayListExtra(FilePickerConst.KEY_SELECTED_MEDIA);

            if (dataArray == null) {
                return;
            }

            // Get the path for the image selected
            String imagePath = dataArray.get(0);

            // Resize the image for upload
            File imageFile = new File(imagePath);
            imageFile = SaveUtils.resizeImage(imageFile);

            // Get StorageReference for location to upload the image to
            StorageReference imageRef = FirebaseStorage.getInstance().getReference()
                    .child(IMAGE_PATH);

            // Set the sub-directory depending on requestCode
            if (requestCode == REQUEST_CODE_PROFILE_PIC) {
                imageRef = imageRef.child(mAuthor.firebaseId + JPEG_EXT);
            } else {
                imageRef = imageRef.child(mAuthor.firebaseId + BACKDROP_SUFFIX + JPEG_EXT);
            }

            // Show a dialog to the User to indicate the upload action
            final ProgressDialog dialog = new ProgressDialog();
            dialog.setTitle(getString(R.string.progress_upload_files_title));
            dialog.setIndeterminate(true);

            dialog.show(getActivity().getSupportFragmentManager(), null);

            // Upload the Image
            imageRef.putFile(Uri.fromFile(imageFile))
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot snapshot) {
                            dialog.dismiss();

                            boolean updateAuthor = false;

                            // Update the image
                            if (requestCode == REQUEST_CODE_PROFILE_PIC) {
                                mBinding.getVm().notifyPropertyChanged(BR.authorImage);

                                if (!mAuthor.hasImage) {
                                    mAuthor.hasImage = true;
                                    updateAuthor = true;
                                }
                            } else {
                                mBinding.getVm().notifyPropertyChanged(BR.backdrop);

                                if (!mAuthor.isHasBackdrop()) {
                                    mAuthor.setHasBackdrop(true);
                                    updateAuthor = true;
                                }
                            }

                            if (updateAuthor) mAuthor.updateFirebase();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            dialog.dismiss();

                            // Inform the user of the failure
                            Toast.makeText(
                                    getActivity(),
                                    getString(R.string.failed_image_upload_text),
                                    Toast.LENGTH_LONG)
                                    .show();

                            Timber.e(e, e.getMessage());
                        }
                    });
        }
    }

    /**
     * Switches the layout used by the AuthorDetailsAdapter to one with EditText so the user can
     * change the info on their profile
     */
    public void switchAuthorLayout() {
        mAdapter.switchAuthorLayout();
        mBinding.getUfvm().setInEditMode(!mBinding.getUfvm().getInEditMode());
    }

    /**
     * Starts the MessageActivity with the User whose profile the user is viewing
     */
    public void startMessageActivityToUser() {

        // Show ProgressBar
        mBinding.userMessagePb.setVisibility(View.VISIBLE);

        // Add the members that would be in the Chat to a List and check if there is a duplicate Chat
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        final List<String> chatMembers = new ArrayList<>();
        chatMembers.add(user.getUid());
        chatMembers.add(mAuthor.firebaseId);

        Chat.checkDuplicateChats(chatMembers, new FirebaseProviderUtils.FirebaseListener() {
            @Override
            public void onModelReady(BaseModel model) {

                Chat chat;

                if (model == null) {
                    // No duplicate Chat. Start a new Chat with the user

                    chat = new Chat();
                    chat.generateFirebaseId();
                    chat.setActiveMembers(chatMembers);
                    chat.setAllMembers(chatMembers);
                    chat.setGroup(chatMembers.size() > 2);
                } else {

                    // Start the duplicate Chat
                    chat = (Chat) model;
                }

                Intent intent = new Intent(getActivity(), MessageActivity.class);
                intent.putExtra(CHAT_KEY, chat.firebaseId);

                startActivity(intent);

                mBinding.userMessagePb.setVisibility(View.GONE);
            }
        });
    }

    public void startFriendFollowActivity() {
        Intent intent = new Intent(getActivity(), FriendFollowActivity.class);
        intent.putExtra(AUTHOR_KEY, mAuthor.firebaseId);

//        DataCache.getInstance().store(mAuthor);

        startActivity(intent);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Check that the List used by the Adapter is populated
        if (mModelList != null && mModelList.size() > 0) {

            // Create a List of FirebaseIds of all the models that will need be to retrieved
            ArrayList<String> modelIdList = new ArrayList<>();

            // Add the FirebaseId of each model to the List
            for (BaseModel model : mModelList) {
                modelIdList.add(model.firebaseId);
            }

            // Add the List to the Bundle
            outState.putStringArrayList(MODEL_LIST_KEY, modelIdList);
        }
    }

    @Override
    public boolean onPrepareMenu(NavigationMenu navigationMenu) {
        return true;
    }

    @Override
    public boolean onMenuItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.fab_new_guide:

                // Launch Activity flow to start a new Guide
                Intent intent = new Intent(getActivity(), SelectAreaTrailActivity.class);
                intent.putExtra(AUTHOR_KEY, mAuthor);
                startActivity(intent);

                return true;

            case R.id.fab_open_draft:

                // Launch Activity for opening a saved draft
                startActivity(new Intent(getActivity(), OpenDraftActivity.class));
                return true;
        }

        return false;
    }

    @Override
    public void onMenuClosed() {

    }

    @Override
    public void onConnected() {
        super.onConnected();

        if (mModelList == null || mModelList.size() == 0) {

            // Show the ProgressBar
            mBinding.userPb.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDisconnected() {
        super.onDisconnected();

        // Hide the ProgressBar as nothing is actually loading
        mBinding.userPb.setVisibility(View.GONE);
    }

}

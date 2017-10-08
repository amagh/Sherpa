package project.sherpa.ui.activities;

import android.databinding.ViewDataBinding;
import android.os.Bundle;
import android.view.View;

import project.sherpa.R;
import project.sherpa.ui.activities.abstractactivities.ConnectivityActivity;
import project.sherpa.ui.fragments.ChatFragment;

/**
 * Created by Alvin on 9/15/2017.
 */

public class ChatActivity extends ConnectivityActivity {

    // ** Constants ** //
    private ViewDataBinding mBinding;
    private ChatFragment mFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chat);

        mFragment = new ChatFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, mFragment)
                .commit();
    }

    /**
     * Click response for the FAB that starts a new Chat
     * @param view
     */
    public void onClickNewChat(View view) {
        addNewChat();
    }

    private void addNewChat() {
        mFragment.addNewChat();
    }
}

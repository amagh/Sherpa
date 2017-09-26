package project.sherpa.ui.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;

import project.sherpa.R;
import project.sherpa.ui.fragments.MessageFragment;

import static project.sherpa.utilities.Constants.FragmentTags.FRAG_TAG_MESSAGES;
import static project.sherpa.utilities.Constants.IntentKeys.CHAT_KEY;

/**
 * Created by Alvin on 9/15/2017.
 */

public class MessageActivity extends ConnectivityActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_message);

        String chatId = getIntent().getStringExtra(CHAT_KEY);

        MessageFragment fragment = MessageFragment.newInstance(chatId);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment, FRAG_TAG_MESSAGES)
                .commit();
    }
}

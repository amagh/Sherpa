package project.hikerguide.ui;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import project.hikerguide.R;
import project.hikerguide.data.GuideDatabase;
import project.hikerguide.databinding.ActivityUserBinding;
import project.hikerguide.firebasedatabase.DatabaseProvider;
import project.hikerguide.models.datamodels.Author;
import project.hikerguide.utilities.FirebaseProviderUtils;
import timber.log.Timber;

/**
 * Created by Alvin on 7/31/2017.
 */

public class UserActivity extends AppCompatActivity {
    // ** Constants ** //

    // ** Member Variables ** //
    ActivityUserBinding mBinding;
    Author mAuthor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_user);
    }

    @Override
    protected void onStart() {
        super.onStart();

        checkUser();
    }

    /**
     * Checks that the Firebase User has been added to the Firebase Database
     */
    private void checkUser() {
        // Get an instance of the FirebaseUser
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            // No valid user, send to the AccountActivity to sign in
            startActivity(new Intent(this, AccountActivity.class));
            return;
        }

        // Get a reference to the DatabaseReference corresponding to the user
        final DatabaseReference authorRef = FirebaseDatabase.getInstance().getReference()
                .child(GuideDatabase.AUTHORS)
                .child(user.getUid());

        authorRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                // Convert the DataSnapshot to an Author Object
                mAuthor = (Author) FirebaseProviderUtils
                        .getModelFromSnapshot(DatabaseProvider.FirebaseType.AUTHOR, dataSnapshot);

                // Remove Listener
                authorRef.removeEventListener(this);

                if (mAuthor == null || mAuthor.name == null) {

                    // Author has not set a display name at minimum yet
                    // TODO: Start profile Activity
                }


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

                // Remove Listener
                authorRef.removeEventListener(this);
            }
        });
    }
}

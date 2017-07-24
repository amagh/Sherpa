package project.hikerguide.ui;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import project.hikerguide.R;
import project.hikerguide.data.GuideDatabase;
import project.hikerguide.firebasedatabase.DatabaseProvider;
import project.hikerguide.models.datamodels.Author;
import project.hikerguide.models.datamodels.Guide;
import project.hikerguide.models.datamodels.Section;
import project.hikerguide.models.datamodels.abstractmodels.BaseModel;
import project.hikerguide.ui.adapters.GuideDetailsAdapter;
import project.hikerguide.utilities.FirebaseProviderUtils;
import timber.log.Timber;

import static project.hikerguide.ui.GuideDetailsActivity.IntentKeys.GUIDE_KEY;

public class GuideDetailsActivity extends AppCompatActivity {
    // ** Constants ** //
    public interface IntentKeys {
        String GUIDE_KEY = "guides";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide_details);
    }
}

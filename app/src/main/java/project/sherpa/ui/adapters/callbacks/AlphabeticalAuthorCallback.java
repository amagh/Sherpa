package project.sherpa.ui.adapters.callbacks;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.util.SortedListAdapterCallback;

import project.sherpa.models.datamodels.Author;

/**
 * Created by Alvin on 9/26/2017.
 */

public class AlphabeticalAuthorCallback extends SortedListAdapterCallback<Author> {

    public AlphabeticalAuthorCallback(RecyclerView.Adapter adapter) {
        super(adapter);
    }

    @Override
    public int compare(Author o1, Author o2) {
        return o1.name.compareTo(o2.name);
    }

    @Override
    public boolean areContentsTheSame(Author oldItem, Author newItem) {
        return oldItem.firebaseId.equals(newItem.firebaseId);
    }

    @Override
    public boolean areItemsTheSame(Author item1, Author item2) {
        return item1.firebaseId.equals(item2.firebaseId);
    }
}

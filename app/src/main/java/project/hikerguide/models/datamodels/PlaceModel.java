package project.hikerguide.models.datamodels;

import java.util.Comparator;

import project.hikerguide.models.AreaAdapterSortable;

/**
 * Created by Alvin on 8/2/2017.
 */

public class PlaceModel implements AreaAdapterSortable {
    public String primaryText;
    public String secondaryText;
    public String placeId;

    @Override
    public String getName() {
        return primaryText;
    }
}

package project.sherpa.models.datamodels;

import project.sherpa.models.AreaAdapterSortable;

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

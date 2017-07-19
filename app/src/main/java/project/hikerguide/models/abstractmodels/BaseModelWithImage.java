package project.hikerguide.models.abstractmodels;

import project.hikerguide.models.ImageFile;

/**
 * Created by Alvin on 7/19/2017.
 */

public abstract class BaseModelWithImage extends BaseModel {
    private ImageFile imageFile;

    public void setImageFile(ImageFile imageFile) {
        this.imageFile = imageFile;
    }

    public ImageFile getImageFile() {
        return imageFile;
    }
}

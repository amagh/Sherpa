package project.hikerguide.ui.adapters;

/**
 * Generic ClickHandler for passing the clicked item from an Adapter
 */

public interface ClickHandler<T> {
    void onClick(T clickedItem);
}

package project.sherpa.ui.adapters.interfaces;

/**
 * Generic ClickHandler for passing the clicked item from an Adapter
 */

public interface ClickHandler<T> {
    void onClick(T clickedItem);
}

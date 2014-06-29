package cz.zcu.kiv.multicloudandroid.display;

/**
 * cz.zcu.kiv.multicloudandroid.display/ItemSelectedHandler.java			<br /><br />
 *
 * Interface for handling selection of an item from the item list.
 *
 * @author Jaromír Staněk
 * @version 1.0
 *
 */
public interface ItemSelectedHandler {

	/**
	 * Event fired when an item was selected.
	 * @param position Position of the selected item.
	 * @param action Action that should be performed.
	 */
	void onItemSelected(int position, ItemAction action);

}

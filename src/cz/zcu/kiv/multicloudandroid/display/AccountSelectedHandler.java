package cz.zcu.kiv.multicloudandroid.display;

/**
 * cz.zcu.kiv.multicloudandroid.display/AccountSelectedHandler.java			<br /><br />
 *
 * Interface for handling selection of an account from the account list.
 *
 * @author Jaromír Staněk
 * @version 1.0
 *
 */
public interface AccountSelectedHandler {

	/**
	 * Event fired when an account was selected.
	 * @param position Position of the selected account.
	 * @param action Action that should be performed.
	 */
	void onAccountSelected(int position, AccountAction action);

}

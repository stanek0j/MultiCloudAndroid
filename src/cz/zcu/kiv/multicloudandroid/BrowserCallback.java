package cz.zcu.kiv.multicloudandroid;

import android.net.Uri;
import cz.zcu.kiv.multicloud.oauth2.AuthorizationCallback;
import cz.zcu.kiv.multicloud.oauth2.AuthorizationRequest;

/**
 * cz.zcu.kiv.multicloudandroid/BrowserCallback.java			<br /><br />
 *
 * Implementation of the {@link cz.zcu.kiv.multicloud.oauth2.AuthorizationCallback} for opening default browser.
 *
 * @author Jaromír Staněk
 * @version 1.0
 *
 */
public class BrowserCallback implements AuthorizationCallback {

	/** Context. */
	private final MainActivity context;

	/**
	 * Ctor with context as parameter.
	 * @param context Context.
	 */
	public BrowserCallback(MainActivity context) {
		this.context = context;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onAuthorizationRequest(AuthorizationRequest request) {
		context.actionAuthorize(Uri.parse(request.getRequestUri()));
	}

}

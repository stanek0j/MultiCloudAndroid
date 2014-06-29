package cz.zcu.kiv.multicloudandroid;

import android.content.Context;
import android.content.Intent;
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
	private final Context context;

	/**
	 * Ctor with context as parameter.
	 * @param context Context.
	 */
	public BrowserCallback(Context context) {
		this.context = context;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onAuthorizationRequest(AuthorizationRequest request) {
		Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(request.getRequestUri()));
		context.startActivity(browserIntent);
	}

}

package org.eclipse.e4.ui.bindings.keys;

import org.eclipse.osgi.util.NLS;

/**
 *
 */
public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.e4.ui.bindings.keys.messages"; //$NON-NLS-1$
	public static String KeyBindingDispatcher_Command;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}

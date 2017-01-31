/*******************************************************************************
 * Copyright (c) 2017 Fabio Zadrozny and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fabio Zadrozny - initial API and implementation - https://bugs.eclipse.org/bugs/show_bug.cgi?id=8519
 *******************************************************************************/
package org.eclipse.e4.core.macros;

import java.io.File;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.core.macros.internal.MacroManager;
import org.osgi.framework.BundleContext;

/**
 * The activator which creates the default macro manager instance.
 */
public class Activator extends Plugin {

	private static Activator plugin;

	public Activator() {
		super();
		plugin = this;
	}

	public static Activator getDefault() {
		return plugin;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);

		// user.home/.eclipse is already used by oomph and recommenders, so, it
		// seems a good place to read additional macros which should be
		// persisted for the user who wants to store macros across workspaces.
		try {
			IPath stateLocation = this.getStateLocation();
			stateLocation.append("macros"); //$NON-NLS-1$
			File userHome = new File(System.getProperty("user.home")); //$NON-NLS-1$
			File eclipseUserHome = new File(userHome, ".eclipse"); //$NON-NLS-1$
			File eclipseUserHomeMacros = new File(eclipseUserHome, "org.eclipse.e4.core.macros"); //$NON-NLS-1$
			File eclipseUserHomeMacrosLoadDir = new File(eclipseUserHomeMacros, "macros"); //$NON-NLS-1$
			if (!eclipseUserHomeMacrosLoadDir.exists()) {
				eclipseUserHomeMacrosLoadDir.mkdirs();
			}
			// By default macros are saved/loaded under the workspace, but can
			// also be loaded from the
			// user.home/.eclipse/org.eclipse.e4.macros/macros
			// directory.
			MacroManager.createDefaultInstance(stateLocation.toFile(), eclipseUserHomeMacrosLoadDir);
		} catch (Exception e) {
			log(e);
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		MacroManager.disposeDefaultInstance();
	}

	public static void log(Throwable exception) {
		try {
			if (plugin != null) {
				plugin.getLog().log(new Status(IStatus.ERROR, plugin.getBundle().getSymbolicName(),
						exception.getMessage(), exception));
			} else {
				// The plugin is not available. Just print to stderr.
				exception.printStackTrace();
			}
		} catch (Exception e) {
			// Print the original error if something happened, not the one
			// related to the log not working.
			exception.printStackTrace();
		}
	}

}

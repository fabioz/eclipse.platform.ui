/*******************************************************************************
 * Copyright (c) 2017 Fabio Zadrozny and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fabio Zadrozny - initial API and implementation - http://eclip.se/8519
 *******************************************************************************/
package org.eclipse.e4.ui.macros.internal.keybindings;

import javax.inject.Inject;
import org.eclipse.e4.core.macros.EMacroService;
import org.eclipse.e4.core.macros.IMacroStateListener;
import org.eclipse.e4.ui.bindings.keys.KeyBindingDispatcher;

/**
 * A macro listener that will install the KeyBindingDispatcherInterceptor when
 * in a record/playback context.
 */
public class KeyBindingDispatcherInterceptorInstaller implements IMacroStateListener {

	@Inject
	private KeyBindingDispatcher fDispatcher;

	/**
	 * The interceptor for keybinding commands (created only when actually in a
	 * record/playback context, null otherwise).
	 */
	private KeyBindingDispatcherInterceptor fInterceptor;

	@Override
	public void macroStateChanged(EMacroService macroService) {
		if (macroService.isRecording() || macroService.isPlayingBack()) {
			if (fInterceptor == null) {
				fInterceptor = new KeyBindingDispatcherInterceptor(macroService, fDispatcher);
				fDispatcher.addInterceptor(fInterceptor);
			}
		} else {
			if (fInterceptor != null) {
				fDispatcher.removeInterceptor(fInterceptor);
				fInterceptor = null;
			}
		}
	}
}

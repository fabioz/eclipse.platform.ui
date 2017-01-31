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
package org.eclipse.e4.ui.macros.internal.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.e4.core.macros.EMacroService;
import org.eclipse.ui.PlatformUI;

/**
 * Activates the playback of the last macro.
 */
public class MacroPlaybackAction extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) {
		PlatformUI.getWorkbench().getService(EMacroService.class).playbackLastMacro();
		return null;
	}
}

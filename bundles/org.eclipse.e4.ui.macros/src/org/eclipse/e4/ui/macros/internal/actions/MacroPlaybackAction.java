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
package org.eclipse.e4.ui.macros.internal.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;

/**
 * An e3-wrapper to call the e4 action.
 *
 * Needed because defining a toolbar with an e4 fragment is not placed properly
 * when there are e3-compat toolbars.
 */
public class MacroPlaybackAction extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) {
		DIWrapper.execute(MacroPlaybackActionImpl.class);
		return null;
	}
}

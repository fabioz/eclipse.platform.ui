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

import javax.inject.Inject;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.macros.EMacroContext;
import org.eclipse.swt.widgets.Shell;

/**
 *
 */
public class ToogleMacroRecordAction {

	@Inject
	private EMacroContext fMacroContext;


	/**
	 * @param shell
	 */
	@Execute
	public void execute(Shell shell) {
		fMacroContext.toggleMacroRecord();
	}

}

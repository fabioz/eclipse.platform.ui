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
package org.eclipse.e4.core.macros.internal;

import org.eclipse.e4.core.macros.IMacroCommand;

/**
 * A macro which is composed of multiple macro commands.
 */
/* default */ interface IComposableMacro extends IMacro {

	/**
	 * Adds a new macro command to this macro.
	 *
	 * @param macroCommand
	 *            the macro command to be appended to this macro.
	 */
	public void addMacroCommand(IMacroCommand macroCommand);

}

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

import java.util.Map;

/**
 * The basic abstraction of a macro command (i.e.: something that can be stored
 * when recording a macro and is later executed when playing it back).
 */
public interface IMacroCommand {

	/**
	 * @return the id for the macro command.
	 */
	String getId();

	/**
	 * Executes the macro command in the given context.
	 *
	 * @param macroPlaybackContext
	 *            the context used to playback the macro.
	 * @throws Exception
	 *             if something didn't work when executing the macro.
	 */
	void execute(IMacroPlaybackContext macroPlaybackContext) throws Exception;

	/**
	 * Should be overridden to provide a user-representation of the macro
	 * command.
	 *
	 * @return a user-representation for the macro command.
	 */
	String getUserRepresentation();

	/**
	 * Convert the macro command into a map (which may be later dumped to the
	 * disk) and recreated with an
	 * {@link org.eclipse.e4.core.macros.IMacroCreator} registered through the
	 * org.eclipse.e4.core.macros.macro_command extension point.
	 *
	 * @return a map which may be dumped to the disk and can be used to recreate
	 *         the macro later on.
	 */
	Map<String, String> toMap();

}

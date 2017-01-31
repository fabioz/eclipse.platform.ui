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

/**
 * A context passed when playing back a macro.
 */
public interface IMacroPlaybackContext {

	/**
	 * To be called to give some information while recording or running a macro.
	 *
	 * @param string
	 *            a string to be logged.
	 */
	void log(String string);

	/**
	 * To be called when some error happens when playing back a macro.
	 *
	 * @param exception
	 *            the exception that happened when playing back a macro.
	 */
	void error(Throwable exception);

}

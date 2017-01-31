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

import java.io.IOException;
import org.eclipse.e4.core.macros.Activator;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;

/**
 *
 */
public class MacroConsole {

	private static final Object lock = new Object();

	private static boolean fShowMacroConsole = true;

	private static IOConsoleOutputStream fOutputStream;

	private static IOConsoleOutputStream getConsoleOutputStream() {
		synchronized (lock) {
			if (fOutputStream == null) {
				MessageConsole console = new MessageConsole(Messages.MacroConsole_MacroRecordPlayback, null);
				fOutputStream = console.newOutputStream();
			}
			return fOutputStream;
		}
	}

	/**
	 * @param msg
	 */
	public static void logToMacroConsole(String msg){
		if(fShowMacroConsole){
			IOConsoleOutputStream consoleOutputStream = getConsoleOutputStream();
			try {
				consoleOutputStream.write(msg);
				consoleOutputStream.write("\n"); //$NON-NLS-1$
			} catch (IOException e) {
				Activator.log(e);
			}
		}
	}

}

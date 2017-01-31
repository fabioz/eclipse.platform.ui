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
package org.eclipse.e4.ui.bindings.internal.macro;

import java.util.Map;
import javax.inject.Inject;
import org.eclipse.core.commands.CommandManager;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.macros.IMacroCommand;
import org.eclipse.e4.core.macros.IMacroCreator;
import org.eclipse.e4.ui.bindings.keys.KeyBindingDispatcher;
import org.eclipse.ui.PlatformUI;

/**
 *
 */
public class MacroForParameterizedCommandCreator implements IMacroCreator {

	/**
	 * Helper class to get the needed dependencies using DI.
	 */
	private static class DIContext {

		@Inject
		private CommandManager fCommandManager;
		@Inject
		private KeyBindingDispatcher fDispatcher;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.e4.core.macros.IMacroCreator#create(java.util.Map)
	 */
	@Override
	public IMacroCommand create(Map<String, String> stringMap) throws Exception {
		// As MacroForParameterizedCommandCreator is registered through an
		// extension point, @Inject won't work directly in it, so, we create
		// a helper class to get what'd be needed for the macro command.
		//
		// Reference:
		// http://eclipsesource.com/blogs/tutorials/eclipse-4-e4-tutorial-soft-migration-from-3-x-to-eclipse-4-e4/
		IEclipseContext eclipseContext = PlatformUI.getWorkbench().getService(IEclipseContext.class).getActiveLeaf();
		DIContext diContext = ContextInjectionFactory.make(DIContext.class, eclipseContext);
		return MacroForParameterizedCommand.fromMap(stringMap, diContext.fCommandManager, diContext.fDispatcher);
	}

}

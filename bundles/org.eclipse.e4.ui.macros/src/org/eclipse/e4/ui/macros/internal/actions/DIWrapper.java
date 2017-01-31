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

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.ui.PlatformUI;

/**
 * Helper to create/execute some class from e4.
 */
/* default */ class DIWrapper {

	private static IEclipseContext getActiveContext() {
		IEclipseContext parentContext = PlatformUI.getWorkbench().getService(IEclipseContext.class);
		return parentContext.getActiveLeaf();
	}

	/**
	 * Creates and executes the given class.
	 *
	 * @param clazz
	 *            the class whose "@Execute" should be called.
	 */
	public static <T> void execute(Class<T> clazz) {
		IEclipseContext context = getActiveContext();
		T impl = ContextInjectionFactory.make(clazz, context);
		ContextInjectionFactory.invoke(impl, Execute.class, context);
	}

}

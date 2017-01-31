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

import org.eclipse.e4.core.contexts.ContextFunction;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;

/**
 * Creates an MacroContextImplementation (to be bound to EMacroContext).
 */
public class MacroContextServiceCreationFunction extends ContextFunction {

	@Override
	public Object compute(IEclipseContext context, String contextKey) {
		return ContextInjectionFactory.make(MacroContextImplementation.class, context);
	}
}

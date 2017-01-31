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

import javax.annotation.PostConstruct;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.macros.internal.MacroContextServiceCreationFunction;

/**
 * Provide the macro services (EMacroContext) as an add-on. Must be instantiated
 * against the application level context.
 */
public final class MacroServiceAddon {

	@PostConstruct
	public void init(IEclipseContext context) {
		context.set(EMacroContext.class.getName(), new MacroContextServiceCreationFunction());
	}

}

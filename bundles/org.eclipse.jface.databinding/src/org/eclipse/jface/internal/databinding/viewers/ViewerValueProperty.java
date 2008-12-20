/*******************************************************************************
 * Copyright (c) 2008 Matthew Hall and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Hall - initial API and implementation (bug 194734)
 ******************************************************************************/

package org.eclipse.jface.internal.databinding.viewers;

import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.property.value.SimpleValueProperty;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.viewers.Viewer;

/**
 * @since 3.3
 * 
 */
public abstract class ViewerValueProperty extends SimpleValueProperty {
	protected Realm getPreferredRealm(Object source) {
		if (source instanceof Viewer) {
			return SWTObservables.getRealm(((Viewer) source).getControl()
					.getDisplay());
		}
		return super.getPreferredRealm(source);
	}
}

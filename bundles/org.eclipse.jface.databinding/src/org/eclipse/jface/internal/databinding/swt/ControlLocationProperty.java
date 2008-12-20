/*******************************************************************************
 * Copyright (c) 2008 Matthew Hall and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Hall - initial API and implementation
 *     Tom Schindl - initial API and implementation
 ******************************************************************************/

package org.eclipse.jface.internal.databinding.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;

/**
 * @since 3.3
 * 
 */
public class ControlLocationProperty extends WidgetValueProperty {
	/**
	 * 
	 */
	public ControlLocationProperty() {
		super(SWT.Move);
	}

	protected Object getValueType() {
		return Point.class;
	}

	public Object getValue(Object source) {
		return ((Control) source).getLocation();
	}

	public boolean setValue(Object source, Object value) {
		if (source == null)
			return false;
		((Control) source).setLocation((Point) value);
		return true;
	}

	public String toString() {
		return "Control.location <Point>"; //$NON-NLS-1$
	}
}

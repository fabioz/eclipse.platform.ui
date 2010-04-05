package org.eclipse.e4.workbench.ui.internal;

import org.eclipse.e4.core.contexts.ContextFunction;
import org.eclipse.e4.core.contexts.IContextConstants;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.MContext;
import org.eclipse.e4.ui.model.application.MPart;
import org.eclipse.e4.ui.model.application.MWindow;

/**
 *
 */
public class ActivePartLookupFunction extends ContextFunction {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.e4.core.services.context.spi.ContextFunction#compute(org.
	 * eclipse.e4.core.services.context.IEclipseContext, java.lang.Object[])
	 */
	@Override
	public Object compute(IEclipseContext context, Object[] arguments) {
		/**
		 * This is the specific implementation. TODO: generalize it
		 */
		MContext window = (MContext) context.get(MWindow.class.getName());
		if (window == null) {
			window = (MContext) context.get(MApplication.class.getName());
			if (window == null) {
				return null;
			}
		}
		IEclipseContext current = window.getContext();
		if (current == null) {
			return null;
		}
		IEclipseContext next = (IEclipseContext) current.getLocal(IContextConstants.ACTIVE_CHILD);
		while (next != null) {
			current = next;
			next = (IEclipseContext) current.getLocal(IContextConstants.ACTIVE_CHILD);
		}
		return current.get(MPart.class.getName());
	}
}

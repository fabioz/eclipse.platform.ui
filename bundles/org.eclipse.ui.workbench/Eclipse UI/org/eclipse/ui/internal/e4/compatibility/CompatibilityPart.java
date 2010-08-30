/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.ui.internal.e4.compatibility;

import org.eclipse.e4.ui.workbench.UIEvents;

import org.eclipse.e4.ui.di.Persist;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.model.application.ui.MDirtyable;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.ISaveablePart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPart2;
import org.eclipse.ui.IWorkbenchPartConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.internal.PartSite;
import org.eclipse.ui.internal.WorkbenchPage;
import org.eclipse.ui.internal.WorkbenchPartReference;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.util.Util;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

public abstract class CompatibilityPart {

	public static final String COMPATIBILITY_EDITOR_URI = "platform:/plugin/org.eclipse.ui.workbench/org.eclipse.ui.internal.e4.compatibility.CompatibilityEditor"; //$NON-NLS-1$

	public static final String COMPATIBILITY_VIEW_URI = "platform:/plugin/org.eclipse.ui.workbench/org.eclipse.ui.internal.e4.compatibility.CompatibilityView"; //$NON-NLS-1$

	@Inject
	Composite composite;

	@Inject
	Logger logger;

	IWorkbenchPart wrapped;

	MPart part;

	@Inject
	private IEventBroker eventBroker;

	private boolean beingDisposed = false;

	private boolean alreadyDisposed = false;

	/**
	 * This handler will be notified when the part's contribution has been
	 * un/set.
	 */
	private EventHandler objectSetHandler = new EventHandler() {
		public void handleEvent(Event event) {
			// check that we're looking at our own part and that the object is
			// being unset
			if (event.getProperty(UIEvents.EventTags.ELEMENT) == part
					&& event.getProperty(UIEvents.EventTags.NEW_VALUE) == null) {
				invalidate();
			}
		}
	};

	/**
	 * This handler will be notified when the part's widget has been un/set.
	 */
	private EventHandler widgetSetHandler = new EventHandler() {
		public void handleEvent(Event event) {
			// check that we're looking at our own part and that the widget is
			// being unset
			if (event.getProperty(UIEvents.EventTags.ELEMENT) == part
					&& event.getProperty(UIEvents.EventTags.NEW_VALUE) == null) {
				 Assert.isTrue(!composite.isDisposed(),
										"The widget should not have been disposed at this point"); //$NON-NLS-1$
				beingDisposed = true;
				WorkbenchPartReference reference = getReference();
				// notify the workbench we're being closed
				((WorkbenchPage) reference.getPage()).firePartClosed(CompatibilityPart.this);
			}
		}
	};

	CompatibilityPart(MPart part) {
		this.part = part;
	}

	public abstract WorkbenchPartReference getReference();

	protected void createPartControl(final IWorkbenchPart legacyPart, Composite parent) {
		try {
			legacyPart.createPartControl(parent);
		} catch (RuntimeException e) {
			logger.error(e);
		}
	}

	@Focus
	void delegateSetFocus() {
		wrapped.setFocus();
	}

	private void invalidate() {
		WorkbenchPartReference reference = getReference();
		reference.invalidate();

		if (wrapped != null) {
			try {
				wrapped.dispose();
			} catch (Exception e) {
				// client code may have errors so we need to catch it
				logger.error(e);
			}
		}

		disposeSite();
		alreadyDisposed = true;
	}

	private String computeLabel() {
		if (wrapped instanceof IWorkbenchPart2) {
			String label = ((IWorkbenchPart2) wrapped).getPartName();
			return Util.safeString(label);
		}

		IWorkbenchPartSite site = wrapped.getSite();
		if (site != null) {
			return Util.safeString(site.getRegisteredName());
		}
		return Util.safeString(wrapped.getTitle());
	}

	/**
	 * Returns whether this part is being disposed. This is used for
	 * invalidating this part so that it is not returned when a method expects a
	 * "working" part.
	 * <p>
	 * See bug 308492.
	 * </p>
	 * 
	 * @return if the part is currently being disposed
	 */
	public boolean isBeingDisposed() {
		return beingDisposed;
	}

	@PostConstruct
	public void create() {
		eventBroker.subscribe(UIEvents.buildTopic(UIEvents.Contribution.TOPIC,
				UIEvents.Contribution.OBJECT), objectSetHandler);

		eventBroker.subscribe(UIEvents.buildTopic(UIEvents.UIElement.TOPIC,
				UIEvents.UIElement.WIDGET), widgetSetHandler);

		WorkbenchPartReference reference = getReference();

		try {
			// ask our reference to instantiate the part through the registry
			wrapped = reference.createPart();
			// invoke init methods
			reference.initialize(wrapped);
		} catch (PartInitException e) {
			reference.invalidate();
			if (wrapped instanceof IEditorPart) {
				try {
					wrapped.dispose();
				} catch (Exception ex) {
					// client code may have errors so we need to catch it
					logger.error(ex);
				}
			}
			disposeSite();

			alreadyDisposed = false;
			WorkbenchPlugin.log("Unable to create part", e.getStatus()); //$NON-NLS-1$

			wrapped = reference.createErrorPart(e.getStatus());
			try {
				reference.initialize(wrapped);
			} catch (PartInitException ex) {
				WorkbenchPlugin.log("Unable to initialize error part", ex.getStatus()); //$NON-NLS-1$
				return;
			}
		}

		// hook reference listeners to the part
		// reference.hookPropertyListeners();

		createPartControl(wrapped, composite);

		part.setLabel(computeLabel());
		part.setTooltip(wrapped.getTitleToolTip());

		wrapped.addPropertyListener(new IPropertyListener() {
			public void propertyChanged(Object source, int propId) {
				switch (propId) {
				case IWorkbenchPartConstants.PROP_TITLE:
					part.setLabel(computeLabel());
					break;
				case IWorkbenchPartConstants.PROP_DIRTY:
					if (wrapped instanceof ISaveablePart) {
						((MDirtyable) part).setDirty(((ISaveablePart) wrapped).isDirty());
					}
					break;
				}
			}
		});

		// notify the workbench we've been opened
		((WorkbenchPage) reference.getPage()).firePartOpened(CompatibilityPart.this);
	}

	@PreDestroy
	void destroy() {
		if (!alreadyDisposed) {
			invalidate();
		}

		eventBroker.unsubscribe(objectSetHandler);
		eventBroker.unsubscribe(widgetSetHandler);
	}

	/**
	 * Disposes of the 3.x part's site if it has one. Subclasses may override
	 * but must call <code>super.disposeSite()</code> in its implementation.
	 */
	void disposeSite() {
		PartSite site = getReference().getSite();
		if (site != null) {
			site.dispose();
		}
	}

	@Persist
	void doSave(@Optional IProgressMonitor monitor) {
		monitor = SubMonitor.convert(monitor);
		if (wrapped instanceof ISaveablePart) {
			((ISaveablePart) wrapped).doSave(monitor);
		}
	}

	public IWorkbenchPart getPart() {
		return wrapped;
	}

	public MPart getModel() {
		return part;
	}
}
/*******************************************************************************
 * Copyright (c) 2009. 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.e4.ui.workbench.swt.internal;

import java.io.IOException;
import org.eclipse.core.commands.contexts.ContextManager;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.e4.core.services.IContributionFactory;
import org.eclipse.e4.core.services.Logger;
import org.eclipse.e4.core.services.context.ContextChangeEvent;
import org.eclipse.e4.core.services.context.EclipseContextFactory;
import org.eclipse.e4.core.services.context.IEclipseContext;
import org.eclipse.e4.core.services.context.IRunAndTrack;
import org.eclipse.e4.core.services.context.spi.ContextInjectionFactory;
import org.eclipse.e4.core.services.context.spi.IContextConstants;
import org.eclipse.e4.core.services.context.spi.IEclipseContextStrategy;
import org.eclipse.e4.ui.internal.services.ActiveContextsFunction;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.e4.ui.workbench.swt.Activator;
import org.eclipse.e4.workbench.ui.IExceptionHandler;
import org.eclipse.e4.workbench.ui.internal.ActiveChildLookupFunction;
import org.eclipse.e4.workbench.ui.internal.ActivePartLookupFunction;
import org.eclipse.e4.workbench.ui.internal.E4Workbench;
import org.eclipse.e4.workbench.ui.internal.ExceptionHandler;
import org.eclipse.e4.workbench.ui.internal.ReflectionContributionFactory;
import org.eclipse.e4.workbench.ui.internal.ResourceHandler;
import org.eclipse.e4.workbench.ui.internal.UISchedulerStrategy;
import org.eclipse.e4.workbench.ui.internal.WorkbenchLogger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.widgets.Display;

/**
 *
 */
public class E4Application implements IApplication {
	private String[] args;

	private ResourceHandler handler;

	public Object start(IApplicationContext applicationContext)
			throws Exception {

		Display display = new Display();

		args = (String[]) applicationContext.getArguments().get(
				IApplicationContext.APPLICATION_ARGS);
		IEclipseContext appContext = createDefaultContext();

		// Create the app model and its context
		MApplication appModel = loadApplicationModel(applicationContext,
				appContext);
		// for compatibility layer: set the application in the OSGi service
		// context (see Workbench#getInstance())
		if (!E4Workbench.getServiceContext().containsKey(
				MApplication.class.getName())) {
			// first one wins.
			E4Workbench.getServiceContext().set(MApplication.class.getName(),
					appModel);
		}

		// Set the app's context after adding itself
		appContext.set(MApplication.class.getName(), appModel);
		appModel.setContext(appContext);

		// Parse out parameters from both the command line and/or the product
		// definition (if any) and put them in the context
		String xmiURI = getArgValue(E4Workbench.XMI_URI_ARG, applicationContext);
		appContext.set(E4Workbench.XMI_URI_ARG, xmiURI);
		String cssURI = getArgValue(E4Workbench.CSS_URI_ARG, applicationContext);
		appContext.set(E4Workbench.CSS_URI_ARG, cssURI);
		String cssResourcesURI = getArgValue(E4Workbench.CSS_RESOURCE_URI_ARG,
				applicationContext);
		appContext.set(E4Workbench.CSS_RESOURCE_URI_ARG, cssResourcesURI);

		// This is a default arg, if missing we use the default rendering engine
		String presentationURI = getArgValue(E4Workbench.PRESENTATION_URI_ARG,
				applicationContext);
		if (presentationURI == null) {
			presentationURI = PartRenderingEngine.engineURI;
			appContext.set(E4Workbench.PRESENTATION_URI_ARG, presentationURI);
		}

		// Instantiate the Workbench (which is responsible for
		// 'running' the UI (if any)...
		E4Workbench workbench = new E4Workbench(appModel, appContext);

		// Create and run the UI (if any)
		workbench.createAndRunUI(appModel);

		// Save the model into the targetURI
		saveModel();

		workbench.close();

		return 0;
	}

	private void saveModel() {
		try {
			handler.save();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private MApplication loadApplicationModel(IApplicationContext appContext,
			IEclipseContext eclipseContext) {
		MApplication theApp = null;

		Location instanceLocation = Activator.getDefault()
				.getInstanceLocation();

		String appModelPath = getArgValue(E4Workbench.XMI_URI_ARG, appContext);
		Assert.isNotNull(appModelPath, E4Workbench.XMI_URI_ARG
				+ " argument missing"); //$NON-NLS-1$
		final URI initialWorkbenchDefinitionInstance = URI
				.createPlatformPluginURI(appModelPath, true);

		boolean saveAndRestore = true;
		handler = new ResourceHandler(instanceLocation,
				initialWorkbenchDefinitionInstance, saveAndRestore,
				(Logger) eclipseContext.get(Logger.class.getName()));
		Resource resource = handler.loadMostRecentModel();
		theApp = (MApplication) resource.getContents().get(0);

		return theApp;
	}

	private String getArgValue(String argName, IApplicationContext appContext) {
		// Is it in the arg list ?
		if (argName == null || argName.length() == 0)
			return null;

		for (int i = 0; i < args.length; i += 2) {
			if (argName.equals(args[i]))
				return args[i + 1];
		}

		return appContext.getBrandingProperty(argName);
	}

	public void stop() {
	}

	public static IEclipseContext createDefaultContext() {
		return createDefaultContext(UISchedulerStrategy.getInstance());
	}

	public static IEclipseContext createDefaultContext(
			IEclipseContextStrategy strategy) {
		// FROM: WorkbenchApplication
		// parent of the global workbench context is an OSGi service
		// context that can provide OSGi services
		IEclipseContext serviceContext = E4Workbench.getServiceContext();
		final IEclipseContext appContext = EclipseContextFactory.create(
				serviceContext, strategy);
		appContext.set(IContextConstants.DEBUG_STRING, "WorkbenchAppContext"); //$NON-NLS-1$

		// FROM: Workbench#createWorkbenchContext
		IExtensionRegistry registry = RegistryFactory.getRegistry();
		ExceptionHandler exceptionHandler = new ExceptionHandler();
		ReflectionContributionFactory contributionFactory = new ReflectionContributionFactory(
				registry);
		appContext.set(IContributionFactory.class.getName(),
				contributionFactory);

		appContext.set(Logger.class.getName(), ContextInjectionFactory.inject(
				new WorkbenchLogger(), appContext));
		appContext.set(IContextConstants.DEBUG_STRING, "WorkbenchContext"); //$NON-NLS-1$

		// setup for commands and handlers
		appContext.set(ContextManager.class.getName(), new ContextManager());

		// FROM: Workbench#createWorkbenchContext
		appContext.set(IServiceConstants.ACTIVE_CONTEXTS,
				new ActiveContextsFunction());
		appContext.set(IServiceConstants.ACTIVE_PART,
				new ActivePartLookupFunction());
		appContext.runAndTrack(new IRunAndTrack() {
			public boolean notify(ContextChangeEvent event) {
				Object o = appContext.get(IServiceConstants.ACTIVE_PART);
				if (o instanceof MPart) {
					appContext.set(IServiceConstants.ACTIVE_PART_ID,
							((MPart) o).getId());
				}
				return true;
			}

			/*
			 * For debugging purposes only
			 */
			@Override
			public String toString() {
				return IServiceConstants.ACTIVE_PART_ID;
			}
		}, null);
		// EHandlerService comes from a ContextFunction
		// EContextService comes from a ContextFunction
		appContext.set(IExceptionHandler.class.getName(), exceptionHandler);
		appContext.set(IExtensionRegistry.class.getName(), registry);
		// appContext.set(IServiceConstants.SELECTION,
		// new ActiveChildOutputFunction(IServiceConstants.SELECTION));

		// appContext.set(IServiceConstants.INPUT, new ContextFunction() {
		// public Object compute(IEclipseContext context, Object[] arguments) {
		// Class adapterType = null;
		// if (arguments.length > 0 && arguments[0] instanceof Class) {
		// adapterType = (Class) arguments[0];
		// }
		// Object newInput = null;
		// Object newValue = context.get(IServiceConstants.SELECTION);
		// if (adapterType == null || adapterType.isInstance(newValue)) {
		// newInput = newValue;
		// } else if (newValue != null && adapterType != null) {
		// IAdapterManager adapters = (IAdapterManager) appContext
		// .get(IAdapterManager.class.getName());
		// if (adapters != null) {
		// Object adapted = adapters.loadAdapter(newValue,
		// adapterType.getName());
		// if (adapted != null) {
		// newInput = adapted;
		// }
		// }
		// }
		// return newInput;
		// }
		// });
		appContext.set(IServiceConstants.ACTIVE_SHELL,
				new ActiveChildLookupFunction(IServiceConstants.ACTIVE_SHELL,
						E4Workbench.LOCAL_ACTIVE_SHELL));

		// FROM: Workbench#initializeNullStyling
		appContext.set(IStylingEngine.SERVICE_NAME, new IStylingEngine() {
			public void setClassname(Object widget, String classname) {
			}

			public void setId(Object widget, String id) {
			}

			public void style(Object widget) {
			}
		});

		// FROM: Workbench constructor
		// workbenchContext.set(Workbench.class.getName(), this);
		// workbenchContext.set(IWorkbench.class.getName(), this);
		appContext.set(IExtensionRegistry.class.getName(), registry);
		appContext.set(IContributionFactory.class.getName(),
				contributionFactory);
		appContext.set(IEclipseContext.class.getName(), appContext);

		return appContext;
	}
}

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

import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.macros.Activator;
import org.eclipse.e4.core.macros.EMacroContext;
import org.eclipse.e4.core.macros.IMacroCommand;
import org.eclipse.e4.core.macros.IMacroCreator;
import org.eclipse.e4.core.macros.IMacroListener;
import org.eclipse.e4.core.macros.IMacroPlaybackContext;

/**
 * An implementation of the public API for dealing with macros (mostly passes
 * things to the default MacroManager instance).
 */
public class MacroContextImplementation implements EMacroContext {

	public static final String MACRO_COMMAND_EXTENSION_POINT = "org.eclipse.e4.core.macros.macro_command"; //$NON-NLS-1$
	public static final String MACRO_COMMAND_ID = "macro_command_id"; //$NON-NLS-1$
	public static final String MACRO_COMMAND_CLASS = "creator_class"; //$NON-NLS-1$

	private Map<String, IMacroCreator> fCommandIdToMacroCreator;

	@Inject
	public MacroContextImplementation(IEclipseContext eclipseContext) {
		if (eclipseContext != null) {
			IExtensionRegistry registry = eclipseContext.get(IExtensionRegistry.class);

			Map<String, IMacroCreator> validCommandIds = new HashMap<>();
			for (IConfigurationElement ce : registry.getConfigurationElementsFor(MACRO_COMMAND_EXTENSION_POINT)) {
				String macroCommandId = ce.getAttribute(MACRO_COMMAND_ID);
				String macroCommandClass = ce.getAttribute(MACRO_COMMAND_CLASS);
				if (macroCommandId != null && macroCommandClass != null) {
					try {
						validCommandIds.put(macroCommandId,
								(IMacroCreator) ce.createExecutableExtension(MACRO_COMMAND_CLASS));
					} catch (CoreException e) {
						Activator.log(e);
					}
				} else {
					Activator.log(new RuntimeException(
							"Wrong definition for extension: " + MACRO_COMMAND_EXTENSION_POINT + ": " + ce)); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			fCommandIdToMacroCreator = validCommandIds;
		}
	}

	private MacroManager getMacroManager() {
		return MacroManager.getDefaultInstance();
	}

	@Override
	public boolean isRecording() {
		return getMacroManager().isRecording();
	}

	@Override
	public boolean isPlayingBack() {
		return getMacroManager().isPlayingBack();
	}

	@Override
	public void addCommand(IMacroCommand macroCommand) {
		getMacroManager().addMacroCommand(macroCommand);
	}

	@Override
	public void toggleMacroRecord() {
		getMacroManager().toggleMacroRecord(this, fCommandIdToMacroCreator);
	}

	@Override
	public void playbackLastMacro(IMacroPlaybackContext macroPlaybackContext) {
		getMacroManager().playbackLastMacro(this, macroPlaybackContext, fCommandIdToMacroCreator);
	}

	@Override
	public void addMacroListener(IMacroListener listener) {
		getMacroManager().addMacroListener(listener);
	}

	@Override
	public void removeMacroListener(IMacroListener listener) {
		getMacroManager().removeMacroListener(listener);

	}

}

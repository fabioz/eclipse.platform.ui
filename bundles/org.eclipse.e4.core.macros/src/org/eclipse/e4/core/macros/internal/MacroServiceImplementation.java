/*******************************************************************************
 * Copyright (c) 2017 Fabio Zadrozny and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fabio Zadrozny - initial API and implementation - http://eclip.se/8519
 *******************************************************************************/
package org.eclipse.e4.core.macros.internal;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.macros.Activator;
import org.eclipse.e4.core.macros.EMacroService;
import org.eclipse.e4.core.macros.IMacroInstruction;
import org.eclipse.e4.core.macros.IMacroInstructionFactory;
import org.eclipse.e4.core.macros.IMacroPlaybackContext;
import org.eclipse.e4.core.macros.IMacroStateListener;

/**
 * An implementation of the public API for dealing with macros (mostly passes
 * things to an internal MacroManager instance and sets it up properly, dealing
 * with the eclipse context and extension points).
 */
public class MacroServiceImplementation implements EMacroService {

	/**
	 * The instance of the macro manager.
	 */
	private MacroManager fMacroManager;

	/**
	 * Gets the macro manager (lazily creates it if needed).
	 *
	 * @return the macro manager responsible for managing macros.
	 */
	public MacroManager getMacroManager() {
		if (fMacroManager == null) {
			// user.home/.eclipse is already used by oomph and recommenders, so,
			// it
			// seems a good place to read additional macros which should be
			// persisted for the user who wants to store macros across
			// workspaces.
			Activator plugin = Activator.getDefault();
			File[] macrosDirectory;
			if (plugin != null) {
				IPath stateLocation = plugin.getStateLocation();
				stateLocation.append("macros"); //$NON-NLS-1$
				File userHome = new File(System.getProperty("user.home")); //$NON-NLS-1$
				File eclipseUserHome = new File(userHome, ".eclipse"); //$NON-NLS-1$
				File eclipseUserHomeMacros = new File(eclipseUserHome, "org.eclipse.e4.core.macros"); //$NON-NLS-1$
				File eclipseUserHomeMacrosLoadDir = new File(eclipseUserHomeMacros, "macros"); //$NON-NLS-1$
				if (!eclipseUserHomeMacrosLoadDir.exists()) {
					eclipseUserHomeMacrosLoadDir.mkdirs();
				}
				macrosDirectory = new File[] { stateLocation.toFile(), eclipseUserHomeMacrosLoadDir };
				// By default macros are saved/loaded under the workspace, but
				// can also be loaded from the
				//
				// user.home/.eclipse/org.eclipse.e4.macros/macros
				//
				// directory.
			} else {
				macrosDirectory = new File[] {};
			}
			fMacroManager = new MacroManager(macrosDirectory);
		}
		return fMacroManager;
	}

	public static final String MACRO_INSTRUCTION_FACTORY_EXTENSION_POINT = "org.eclipse.e4.core.macros.macroInstructionsFactory"; //$NON-NLS-1$
	public static final String MACRO_INSTRUCTION_ID = "macroInstructionId"; //$NON-NLS-1$
	public static final String MACRO_INSTRUCTION_FACTORY_CLASS = "class"; //$NON-NLS-1$

	private static Map<String, IMacroInstructionFactory> fMacroInstructionIdToFactory;

	public static final String MACRO_LISTENERS_EXTENSION_POINT = "org.eclipse.e4.core.macros.macroStateListeners"; //$NON-NLS-1$
	public static final String MACRO_LISTENER_CLASS = "class"; //$NON-NLS-1$

	private boolean fLoadedExtensionListeners = false;

	private IEclipseContext fEclipseContext;

	private IExtensionRegistry fExtensionRegistry;

	@Inject
	public MacroServiceImplementation(IEclipseContext eclipseContext, IExtensionRegistry extensionRegistry) {
		this.fEclipseContext = eclipseContext;
		this.fExtensionRegistry = extensionRegistry;
	}

	/**
	 * Loads the macro listeners provided through extension points.
	 */
	private void loadExtensionPointsmacroStateListeners() {
		if (!fLoadedExtensionListeners && fExtensionRegistry != null) {
			fLoadedExtensionListeners = true;

			MacroManager macroManager = getMacroManager();
			for (IConfigurationElement ce : fExtensionRegistry
					.getConfigurationElementsFor(MACRO_LISTENERS_EXTENSION_POINT)) {
				String macroStateListenerClass = ce.getAttribute(MACRO_LISTENER_CLASS);
				if (macroStateListenerClass != null) {
					try {
						IMacroStateListener macroStateListener = (IMacroStateListener) ce
								.createExecutableExtension(MACRO_LISTENER_CLASS);
						// Make sure that it has the proper eclipse context.
						ContextInjectionFactory.inject(macroStateListener, fEclipseContext);
						macroManager.addMacroStateListener(macroStateListener);
					} catch (CoreException e) {
						Activator.log(e);
					}
				} else {
					Activator.log(new RuntimeException(
							"Wrong definition for extension: " + MACRO_LISTENERS_EXTENSION_POINT + ": " + ce)); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
	}

	/**
	 * @return Returns the fMacroInstructionIdToFactory (creates it lazily if it
	 *         still wasn't created).
	 */
	private Map<String, IMacroInstructionFactory> getMacroInstructionIdToFactory() {
		if (fMacroInstructionIdToFactory == null && fEclipseContext != null && fExtensionRegistry != null) {
			Map<String, IMacroInstructionFactory> validMacroInstructionIds = new HashMap<>();
			for (IConfigurationElement ce : fExtensionRegistry
					.getConfigurationElementsFor(MACRO_INSTRUCTION_FACTORY_EXTENSION_POINT)) {
				String macroInstructionId = ce.getAttribute(MACRO_INSTRUCTION_ID);
				String macroInstructionFactoryClass = ce.getAttribute(MACRO_INSTRUCTION_FACTORY_CLASS);
				if (macroInstructionId != null && macroInstructionFactoryClass != null) {
					try {
						IMacroInstructionFactory macroInstructionFactory = (IMacroInstructionFactory) ce
								.createExecutableExtension(MACRO_INSTRUCTION_FACTORY_CLASS);

						// Make sure that it has the proper eclipse context.
						ContextInjectionFactory.inject(macroInstructionFactory, fEclipseContext);
						validMacroInstructionIds.put(macroInstructionId, macroInstructionFactory);
					} catch (CoreException e) {
						Activator.log(e);
					}
				} else {
					Activator.log(new RuntimeException("Wrong definition for extension: " //$NON-NLS-1$
							+ MACRO_INSTRUCTION_FACTORY_EXTENSION_POINT + ": " + ce)); //$NON-NLS-1$
				}
			}
			fMacroInstructionIdToFactory = validMacroInstructionIds;
		}
		return fMacroInstructionIdToFactory;
	}

	@Override
	public boolean isRecording() {
		if (fMacroManager == null) {
			// Avoid creating if possible
			return false;
		}
		return getMacroManager().isRecording();
	}

	@Override
	public boolean isPlayingBack() {
		if (fMacroManager == null) {
			// Avoid creating if possible
			return false;
		}
		return getMacroManager().isPlayingBack();
	}

	@Override
	public void addMacroInstruction(IMacroInstruction macroInstruction) {
		getMacroManager().addMacroInstruction(macroInstruction);
	}

	@Override
	public void addMacroInstruction(IMacroInstruction macroInstruction, Object event, int priority) {
		getMacroManager().addMacroInstruction(macroInstruction, event, priority);
	}

	@Override
	public void toggleMacroRecord() {
		loadExtensionPointsmacroStateListeners();
		getMacroManager().toggleMacroRecord(this, getMacroInstructionIdToFactory());
	}

	@Override
	public void playbackLastMacro() {
		loadExtensionPointsmacroStateListeners();
		IMacroPlaybackContext macroPlaybackContext = new MacroPlaybackContextImpl(getMacroInstructionIdToFactory());
		getMacroManager().playbackLastMacro(this, macroPlaybackContext);
	}

	@Override
	public void addMacroStateListener(IMacroStateListener listener) {
		getMacroManager().addMacroStateListener(listener);
	}

	@Override
	public void removeMacroStateListener(IMacroStateListener listener) {
		getMacroManager().removeMacroStateListener(listener);
	}

	/**
	 * Note that this is only available in this implementation, not on the
	 * public API (EMacroService). Needed for testing.
	 *
	 * @return the currently registered listeners.
	 */
	public IMacroStateListener[] getMacroStateListeners() {
		return getMacroManager().getMacroStateListeners();
	}

	/**
	 * A map which maps accepted command ids when recording a macro to whether
	 * they should be recorded as a macro instruction to be played back later
	 * on.
	 */
	private Map<String, Boolean> fWhitelistedCommandIds;

	/**
	 * @return a set with the commands that are accepted when macro recording.
	 */
	private Map<String, Boolean> getInternalCommandsWhitelisted() {
		if (fWhitelistedCommandIds == null) {
			fWhitelistedCommandIds = new HashMap<>();
			if (fEclipseContext != null) {
				IExtensionRegistry registry = fEclipseContext.get(IExtensionRegistry.class);
				if (registry != null) {
					for (IConfigurationElement ce : registry
							.getConfigurationElementsFor("org.eclipse.e4.core.macros.whitelistedCommands")) { //$NON-NLS-1$
						if ("whitelistedCommand".equals(ce.getName()) && ce.getAttribute("id") != null //$NON-NLS-1$ //$NON-NLS-2$
								&& ce.getAttribute("recordMacroInstruction") != null) { //$NON-NLS-1$
							Boolean recordMacroInstruction = Boolean
									.parseBoolean(ce.getAttribute("recordMacroInstruction")) //$NON-NLS-1$
											? Boolean.TRUE
											: Boolean.FALSE;
							fWhitelistedCommandIds.put(ce.getAttribute("id"), recordMacroInstruction); //$NON-NLS-1$
						}
					}
				}
			}
		}
		return fWhitelistedCommandIds;
	}

	@Override
	public boolean isCommandWhitelisted(String commandId) {
		return getInternalCommandsWhitelisted().containsKey(commandId);
	}

	@Override
	public boolean getRecordMacroInstruction(String commandId) {
		Map<String, Boolean> macroWhitelistedCommands = getInternalCommandsWhitelisted();
		return macroWhitelistedCommands.get(commandId);
	}

	@Override
	public void setCommandWhitelisted(String commandId, boolean whitelistCommand, boolean recordMacroInstruction) {
		if (whitelistCommand) {
			getInternalCommandsWhitelisted().put(commandId, recordMacroInstruction);
		} else {
			getInternalCommandsWhitelisted().remove(commandId);
		}
	}

	@Override
	public Set<String> getCommandsWhitelisted() {
		Map<String, Boolean> internalWhitelistedCommands = getInternalCommandsWhitelisted();
		// Create a copy
		return new HashSet<>(internalWhitelistedCommands.keySet());
	}

}

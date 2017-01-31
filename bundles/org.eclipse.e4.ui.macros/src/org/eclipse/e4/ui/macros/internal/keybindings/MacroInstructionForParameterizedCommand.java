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
package org.eclipse.e4.ui.macros.internal.keybindings;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.commands.CommandManager;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.CommandException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.runtime.Assert;
import org.eclipse.e4.core.macros.IMacroInstruction;
import org.eclipse.e4.core.macros.IMacroPlaybackContext;
import org.eclipse.e4.ui.bindings.keys.KeyBindingDispatcher;
import org.eclipse.swt.widgets.Event;

/**
 * A macro instruction for parameterized commands.
 */
public class MacroInstructionForParameterizedCommand implements IMacroInstruction {

	private static final String ID = "org.eclipse.e4.ui.bindings.keys.macroInstructionForParameterizedCommand"; //$NON-NLS-1$

	private static final String CHARACTER = "character"; //$NON-NLS-1$

	private static final String TYPE = "type"; //$NON-NLS-1$

	private static final String STATE_MASK = "stateMask"; //$NON-NLS-1$

	private static final String KEY_CODE = "keyCode"; //$NON-NLS-1$

	private static final String COMMAND = "command"; //$NON-NLS-1$

	private KeyBindingDispatcher fDispatcher;

	private ParameterizedCommand fCmd;

	private Event fEvent;

	/**
	 * @param cmd
	 *            the command recorded.
	 * @param event
	 *            the related event.
	 * @param keybindingDispatcher
	 *            the dispatcher to be used to execute commands.
	 */
	public MacroInstructionForParameterizedCommand(ParameterizedCommand cmd, Event event,
			KeyBindingDispatcher keybindingDispatcher) {
		this.fCmd = cmd;

		// Create a new event (we want to make sure that only the given info is
		// really needed on playback and don't want to keep a reference to the
		// original widget).
		Event newEvent = new Event();
		newEvent.keyCode = event.keyCode;
		newEvent.stateMask = event.stateMask;
		newEvent.type = event.type;
		newEvent.character = event.character;

		this.fEvent = newEvent;
		this.fDispatcher = keybindingDispatcher;
	}

	@Override
	public void execute(IMacroPlaybackContext macroPlaybackContext) throws Exception {
		ParameterizedCommand cmd = fCmd;
		if (cmd == null) {
			throw new RuntimeException("Parameterized command not set."); //$NON-NLS-1$
		}
		try {
			fDispatcher.executeCommand(cmd, this.fEvent);
		} catch (final CommandException e) {
			throw e;
		}
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String toString() {
		try {
			return Messages.MacroInstructionForParameterizedCommand_0 + this.fCmd.getName();
		} catch (NotDefinedException e) {
			return Messages.MacroInstructionForParameterizedCommand_0 + "Undefined"; //$NON-NLS-1$
		}
	}

	@Override
	public Map<String, String> toMap() {
		Map<String, String> map = new HashMap<>();
		String serialized = fCmd.serialize();
		Assert.isNotNull(serialized);
		map.put(COMMAND, serialized);
		map.put(KEY_CODE, Integer.toString(fEvent.keyCode));
		map.put(STATE_MASK, Integer.toString(fEvent.stateMask));
		map.put(TYPE, Integer.toString(fEvent.type));
		map.put(CHARACTER, Character.toString(fEvent.character));

		return map;
	}

	/**
	 * @param map
	 *            a map (created from {@link #toMap()}.
	 * @param commandManager
	 *            the command manager used to deserialize commands.
	 * @param keybindingDispatcher
	 *            the dispatcher for commands.
	 * @return a macro instruction created from the map (created from
	 *         {@link #toMap()}.
	 * @throws Exception
	 *             if it was not possible to recreate the macro instruction.
	 */
	/* default */ static MacroInstructionForParameterizedCommand fromMap(Map<String, String> map, CommandManager commandManager,
			KeyBindingDispatcher keybindingDispatcher)
			throws Exception {
		Assert.isNotNull(commandManager);
		Assert.isNotNull(map);
		Assert.isNotNull(keybindingDispatcher);
		ParameterizedCommand cmd = commandManager.deserialize(map.get(COMMAND));
		Event event = new Event();
		event.keyCode = Integer.parseInt(map.get(KEY_CODE));
		event.stateMask = Integer.parseInt(map.get(STATE_MASK));
		event.type = Integer.parseInt(map.get(TYPE));
		event.character = map.get(CHARACTER).charAt(0);
		MacroInstructionForParameterizedCommand macroInstruction = new MacroInstructionForParameterizedCommand(cmd,
				event, keybindingDispatcher);
		return macroInstruction;
	}
}


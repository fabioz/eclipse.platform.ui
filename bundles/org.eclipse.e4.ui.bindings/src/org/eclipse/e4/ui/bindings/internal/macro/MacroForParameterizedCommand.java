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
package org.eclipse.e4.ui.bindings.internal.macro;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import org.eclipse.core.commands.CommandManager;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.CommandException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.runtime.Assert;
import org.eclipse.e4.core.macros.IMacroCommand;
import org.eclipse.e4.core.macros.IMacroPlaybackContext;
import org.eclipse.e4.ui.bindings.keys.KeyBindingDispatcher;
import org.eclipse.e4.ui.bindings.keys.Messages;
import org.eclipse.swt.widgets.Event;

/**
 * A macro command for parameterized commands.
 */
public class MacroForParameterizedCommand implements IMacroCommand {

	private static final String ID = "org.eclipse.e4.ui.bindings.keys.parameterized_macro_command"; //$NON-NLS-1$

	private static final String CHARACTER = "character"; //$NON-NLS-1$

	private static final String TYPE = "type"; //$NON-NLS-1$

	private static final String STATE_MASK = "stateMask"; //$NON-NLS-1$

	private static final String KEY_CODE = "keyCode"; //$NON-NLS-1$

	private static final String COMMAND = "command"; //$NON-NLS-1$

	@Inject
	private KeyBindingDispatcher fDispatcher;

	private ParameterizedCommand fCmd;

	private Event fEvent;

	/**
	 * @param cmd
	 * @param event
	 */
	public MacroForParameterizedCommand(ParameterizedCommand cmd, Event event) {
		this.fCmd = cmd;
		this.fEvent = event;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.e4.core.macros.IMacroCommand#execute()
	 */
	@Override
	public void execute(IMacroPlaybackContext macroPlaybackContext) throws Exception {
		ParameterizedCommand cmd = fCmd;
		if (cmd == null) {
			throw new RuntimeException("Macro command not set."); //$NON-NLS-1$
		}
		try {
			macroPlaybackContext.log("Executing recorded command: " + cmd); //$NON-NLS-1$
			fDispatcher.executeCommand(cmd, this.fEvent);
		} catch (final CommandException e) {
			throw e;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.e4.core.macros.IMacroCommand#getId()
	 */
	@Override
	public String getId() {
		return ID;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.e4.core.macros.IMacroCommand#getUserRepresentation()
	 */
	@Override
	public String getUserRepresentation() {
		try {
			return Messages.KeyBindingDispatcher_Command + this.fCmd.getName();
		} catch (NotDefinedException e) {
			return "Undefined"; //$NON-NLS-1$
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.e4.core.macros.IMacroCommand#toMap()
	 */
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
	 * @param commandManager
	 * @param keybindingDispatcher
	 * @return a command created from the map (created from {@link #toMap()}.
	 * @throws Exception
	 */
	/* default */ static MacroForParameterizedCommand fromMap(Map<String, String> map, CommandManager commandManager,
			KeyBindingDispatcher keybindingDispatcher)
			throws Exception {
		Assert.isNotNull(commandManager);
		Assert.isNotNull(map);
		ParameterizedCommand cmd = commandManager.deserialize(map.get(COMMAND));
		Event event = new Event();
		event.keyCode = Integer.parseInt(map.get(KEY_CODE));
		event.stateMask = Integer.parseInt(map.get(STATE_MASK));
		event.type = Integer.parseInt(map.get(TYPE));
		event.character = map.get(CHARACTER).charAt(0);
		MacroForParameterizedCommand command = new MacroForParameterizedCommand(cmd, event);
		command.fDispatcher = keybindingDispatcher;
		return command;
	}
}
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
package org.eclipse.e4.ui.macros.styled_text;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.e4.core.macros.IMacroInstruction;
import org.eclipse.e4.core.macros.IMacroPlaybackContext;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * A macro instruction to replay a key down (always followed by a key up).
 *
 * @since 3.11
 */
/* default */ class StyledTextKeyDownMacroInstruction implements IMacroInstruction {

	private static final String ID = "org.eclipse.ui.texteditor.macro.styledTextKeyDownMacroInstruction"; //$NON-NLS-1$

	private static final String CHARACTER = "character"; //$NON-NLS-1$

	private static final String TYPE = "type"; //$NON-NLS-1$

	private static final String STATE_MASK = "stateMask"; //$NON-NLS-1$

	private static final String KEY_CODE = "keyCode"; //$NON-NLS-1$

	private Event fEvent;

	public StyledTextKeyDownMacroInstruction(Event event) {
		// Create a new event (we want to make sure that only the given info is
		// really needed on playback and don't want to keep a reference to the
		// original widget).
		Event newEvent = new Event();
		newEvent.keyCode = event.keyCode;
		newEvent.stateMask = event.stateMask;
		newEvent.type = event.type;
		newEvent.character = event.character;

		this.fEvent = newEvent;
	}

	@Override
	public void execute(IMacroPlaybackContext macroPlaybackContext) {
		IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (activeWorkbenchWindow == null) {
			return;
		}
		IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
		if (activePage == null) {
			return;
		}
		IEditorPart activeEditor = activePage.getActiveEditor();
		if (activeEditor == null) {
			return;
		}
		Control control = activeEditor.getAdapter(Control.class);
		if (control instanceof StyledText) {
			StyledText styledText = (StyledText) control;
			styledText.notifyListeners(SWT.KeyDown, fEvent);

			// Key up is also needed to update the clipboard.
			Event keyUpEvent = new Event();
			keyUpEvent.keyCode = fEvent.keyCode;
			keyUpEvent.stateMask = fEvent.stateMask;
			keyUpEvent.type = SWT.KeyUp;
			keyUpEvent.character = fEvent.character;
			styledText.notifyListeners(SWT.KeyUp, keyUpEvent);
		}
	}

	@Override
	public String getId() {
		return ID;
	}

	@SuppressWarnings("boxing")
	@Override
	public String toString() {
		if (this.fEvent == null) {
			return String.format(Messages.StyledTextKeyDownMacroInstruction_Key, "null"); //$NON-NLS-1$
		}
		return String.format(Messages.StyledTextKeyDownMacroInstruction_Key, this.fEvent.character);
	}

	@Override
	public Map<String, String> toMap() {
		Map<String, String> map = new HashMap<>();
		map.put(KEY_CODE, Integer.toString(fEvent.keyCode));
		map.put(STATE_MASK, Integer.toString(fEvent.stateMask));
		map.put(TYPE, Integer.toString(fEvent.type));
		map.put(CHARACTER, Character.toString(fEvent.character));
		return map;
	}

	/* default */ static StyledTextKeyDownMacroInstruction fromMap(Map<String, String> map) {
		Event event = new Event();
		event.keyCode = Integer.parseInt(map.get(KEY_CODE));
		event.stateMask = Integer.parseInt(map.get(STATE_MASK));
		event.type = Integer.parseInt(map.get(TYPE));
		event.character = map.get(CHARACTER).charAt(0);
		return new StyledTextKeyDownMacroInstruction(event);
	}
}
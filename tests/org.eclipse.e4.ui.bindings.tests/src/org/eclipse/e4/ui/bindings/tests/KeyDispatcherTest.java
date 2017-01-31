/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Lars Vogel <Lars.Vogel@gmail.com> - Bug 440893
 *     Simon Scholz <simon.scholz@vogella.com> - Bug 436344
 *******************************************************************************/
package org.eclipse.e4.ui.bindings.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.Closeable;
import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.core.commands.Category;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.contexts.Context;
import org.eclipse.core.commands.contexts.ContextManager;
import org.eclipse.e4.core.commands.CommandServiceAddon;
import org.eclipse.e4.core.commands.ECommandService;
import org.eclipse.e4.core.commands.EHandlerService;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.macros.EMacroContext;
import org.eclipse.e4.core.macros.IMacroPlaybackContext;
import org.eclipse.e4.core.macros.MacroServiceAddon;
import org.eclipse.e4.core.macros.internal.MacroManager;
import org.eclipse.e4.ui.bindings.BindingServiceAddon;
import org.eclipse.e4.ui.bindings.EBindingService;
import org.eclipse.e4.ui.bindings.internal.BindingTable;
import org.eclipse.e4.ui.bindings.internal.BindingTableManager;
import org.eclipse.e4.ui.bindings.keys.KeyBindingDispatcher;
import org.eclipse.e4.ui.services.ContextServiceAddon;
import org.eclipse.e4.ui.services.EContextService;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class KeyDispatcherTest {
	private static final String ID_DIALOG = "org.eclipse.ui.contexts.dialog";
	private static final String ID_DIALOG_AND_WINDOW = "org.eclipse.ui.contexts.dialogAndWindow";
	private static final String ID_WINDOW = "org.eclipse.ui.contexts.window";

	final static String[] CONTEXTS = { ID_DIALOG_AND_WINDOW, "DAW", null,
			ID_DIALOG, "Dialog", ID_DIALOG_AND_WINDOW, ID_WINDOW, "Window",
			ID_DIALOG_AND_WINDOW, };

	private static final String TEST_CAT1 = "test.cat1";
	private static final String TEST_ID1 = "test.id1";
	private static final String TEST_ID2 = "test.id2";

	static class CallHandler {
		public boolean q1;
		public boolean q2;

		@CanExecute
		public boolean canExecute() {
			q1 = true;
			return true;
		}

		@Execute
		public Object execute() {
			q2 = true;
			if (q1) {
				return Boolean.TRUE;
			}
			return Boolean.FALSE;
		}
	}

	private Display display;
	private IEclipseContext workbenchContext;
	private CallHandler handler;
	private CallHandler twoStrokeHandler;

	private void defineCommands(IEclipseContext context) {
		ECommandService cs = workbenchContext
				.get(ECommandService.class);
		Category category = cs.defineCategory(TEST_CAT1, "CAT1", null);
		cs.defineCommand(TEST_ID1, "ID1", null, category, null);
		cs.defineCommand(TEST_ID2, "ID2", null, category, null);
		ParameterizedCommand cmd = cs.createCommand(TEST_ID1, null);
		EHandlerService hs = workbenchContext
				.get(EHandlerService.class);
		handler = new CallHandler();
		hs.activateHandler(TEST_ID1, handler);
		EBindingService bs = workbenchContext
				.get(EBindingService.class);
		TriggerSequence seq = bs.createSequence("CTRL+A");
		Binding db = createDefaultBinding(bs, seq, cmd);
		bs.activateBinding(db);

		ParameterizedCommand cmd2 = cs.createCommand(TEST_ID2, null);
		twoStrokeHandler = new CallHandler();
		hs.activateHandler(TEST_ID2, twoStrokeHandler);
		TriggerSequence twoKeys = bs.createSequence("CTRL+5 CTRL+A");
		db = createDefaultBinding(bs, twoKeys, cmd2);
		bs.activateBinding(db);
	}

	private Binding createDefaultBinding(EBindingService bs,
			TriggerSequence sequence, ParameterizedCommand command) {

		Map<String, String> attrs = new HashMap<>();
		attrs.put("schemeId", "org.eclipse.ui.defaultAcceleratorConfiguration");

		return bs.createBinding(sequence, command, ID_WINDOW, attrs);
	}

	@Before
	public void setUp() {
		display = Display.getDefault();
		IEclipseContext globalContext = Activator.getDefault().getGlobalContext();
		workbenchContext = globalContext.createChild("workbenchContext");
		ContextInjectionFactory.make(CommandServiceAddon.class, workbenchContext);
		ContextInjectionFactory.make(ContextServiceAddon.class, workbenchContext);
		ContextInjectionFactory.make(BindingServiceAddon.class, workbenchContext);
		ContextInjectionFactory.make(MacroServiceAddon.class, workbenchContext);
		defineContexts(workbenchContext);
		defineBindingTables(workbenchContext);
		defineCommands(workbenchContext);
	}

	private void defineContexts(IEclipseContext context) {
		ContextManager contextManager = context.get(ContextManager.class);
		for (int i = 0; i < CONTEXTS.length; i += 3) {
			Context c = contextManager.getContext(CONTEXTS[i]);
			c.define(CONTEXTS[i + 1], null, CONTEXTS[i + 2]);
		}

		EContextService cs = context
				.get(EContextService.class);
		cs.activateContext(ID_DIALOG_AND_WINDOW);
		cs.activateContext(ID_WINDOW);
	}

	private void defineBindingTables(IEclipseContext context) {
		BindingTableManager btm = context.get(BindingTableManager.class);
		ContextManager cm =  context
				.get(ContextManager.class);
		btm.addTable(new BindingTable(cm.getContext(ID_DIALOG_AND_WINDOW)));
		btm.addTable(new BindingTable(cm.getContext(ID_WINDOW)));
		btm.addTable(new BindingTable(cm.getContext(ID_DIALOG)));
	}

	@After
	public void tearDown() {
		workbenchContext.dispose();
		workbenchContext = null;
		display.dispose();
		display = null;
	}

	@Test
	public void testExecuteOneCommand() {
		KeyBindingDispatcher dispatcher = new KeyBindingDispatcher();
		ContextInjectionFactory.inject(dispatcher, workbenchContext);
		final Listener listener = dispatcher.getKeyDownFilter();
		display.addFilter(SWT.KeyDown, listener);
		display.addFilter(SWT.Traverse, listener);

		assertFalse(handler.q2);

		Shell shell = new Shell(display, SWT.NONE);

		notifyCtrlA(shell);

		assertTrue(handler.q2);
	}

	@Test
	public void testExecuteMultiStrokeBinding() {
		KeyBindingDispatcher dispatcher = new KeyBindingDispatcher();
		ContextInjectionFactory.inject(dispatcher, workbenchContext);
		final Listener listener = dispatcher.getKeyDownFilter();
		display.addFilter(SWT.KeyDown, listener);
		display.addFilter(SWT.Traverse, listener);

		assertFalse(twoStrokeHandler.q2);

		Shell shell = new Shell(display, SWT.NONE);

		Event event = new Event();
		event.type = SWT.KeyDown;
		event.keyCode = SWT.CTRL;
		shell.notifyListeners(SWT.KeyDown, event);

		event = new Event();
		event.type = SWT.KeyDown;
		event.stateMask = SWT.CTRL;
		event.keyCode = '5';
		shell.notifyListeners(SWT.KeyDown, event);

		assertFalse(twoStrokeHandler.q2);

		event = new Event();
		event.type = SWT.KeyDown;
		event.keyCode = SWT.CTRL;
		shell.notifyListeners(SWT.KeyDown, event);

		event = new Event();
		event.type = SWT.KeyDown;
		event.stateMask = SWT.CTRL;
		event.keyCode = 'A';
		shell.notifyListeners(SWT.KeyDown, event);

		processEvents();

		assertTrue(twoStrokeHandler.q2);
		assertFalse(handler.q2);
	}

	@Test
	@Ignore
	public void TODOtestKeyDispatcherReset() throws Exception {
		KeyBindingDispatcher dispatcher = new KeyBindingDispatcher();
		ContextInjectionFactory.inject(dispatcher, workbenchContext);
		final Listener listener = dispatcher.getKeyDownFilter();
		display.addFilter(SWT.KeyDown, listener);
		display.addFilter(SWT.Traverse, listener);

		assertFalse(twoStrokeHandler.q2);

		Shell shell = new Shell(display, SWT.NONE);

		Event event = new Event();
		event.type = SWT.KeyDown;
		event.keyCode = SWT.CTRL;
		shell.notifyListeners(SWT.KeyDown, event);

		event = new Event();
		event.type = SWT.KeyDown;
		event.stateMask = SWT.CTRL;
		event.keyCode = '5';
		shell.notifyListeners(SWT.KeyDown, event);

		assertFalse(twoStrokeHandler.q2);
		Thread.sleep(2000L);
		processEvents();

		event = new Event();
		event.type = SWT.KeyDown;
		event.keyCode = SWT.CTRL;
		shell.notifyListeners(SWT.KeyDown, event);

		event = new Event();
		event.type = SWT.KeyDown;
		event.stateMask = SWT.CTRL;
		event.keyCode = 'A';
		shell.notifyListeners(SWT.KeyDown, event);

		processEvents();

		assertFalse(twoStrokeHandler.q2);
		assertTrue(handler.q2);
	}

	@Test
	public void testSendKeyStroke() {
		KeyBindingDispatcher dispatcher = ContextInjectionFactory
				.make(KeyBindingDispatcher.class, workbenchContext);
		final Listener listener = dispatcher.getKeyDownFilter();
		display.addFilter(SWT.KeyDown, listener);
		display.addFilter(SWT.Traverse, listener);
		Shell shell = new Shell(display, SWT.NONE);
		shell.setLayout(new FillLayout());
		StyledText text = new StyledText(shell, SWT.WRAP | SWT.MULTI);
		shell.setBounds(100, 100, 100, 100);
		shell.layout();
		processEvents();
		assertEquals("", text.getText());

		Event event = new Event();
		event.type = SWT.KeyDown;
		event.stateMask = SWT.SHIFT;
		event.keyCode = '(';
		event.character = '(';
		text.notifyListeners(SWT.KeyDown, event);

		event = new Event();
		event.type = SWT.KeyUp;
		event.stateMask = SWT.SHIFT;
		event.keyCode = '(';
		event.character = '(';
		text.notifyListeners(SWT.KeyUp, event);

		processEvents();

		assertEquals("(", text.getText());
	}


	@Test
	public void testMacroIntegration() throws Exception {
		KeyBindingDispatcher dispatcher = new KeyBindingDispatcher();
		workbenchContext.set(KeyBindingDispatcher.class, dispatcher);
		ContextInjectionFactory.inject(dispatcher, workbenchContext);
		EMacroContext macroContext = workbenchContext.get(EMacroContext.class);
		macroContext.toggleMacroRecord();
		assertTrue(macroContext.isRecording());

		// Usually whitelisted commands are gotten through an extension point,
		// but for testing
		// purposes, let's change the field directly.
		Field whitelistedCommandIdsField = dispatcher.getClass().getDeclaredField("fWhitelistedCommandIds");
		whitelistedCommandIdsField.setAccessible(true);

		final Listener listener = dispatcher.getKeyDownFilter();
		display.addFilter(SWT.KeyDown, listener);
		display.addFilter(SWT.Traverse, listener);

		assertFalse(handler.q2);

		Shell shell = new Shell(display, SWT.NONE);

		// Whitelist command
		whitelistedCommandIdsField.set(dispatcher, new HashSet<>(Arrays.asList(TEST_ID1)));
		notifyCtrlA(shell);

		assertTrue(handler.q2);

		macroContext.toggleMacroRecord();
		assertFalse(macroContext.isRecording());

		handler.q2 = false;
		macroContext.playbackLastMacro(new IMacroPlaybackContext() {

			@Override
			public void log(String string) {
			}

			@Override
			public void error(Throwable exception) {
			}
		});
		assertTrue(handler.q2);

		// Blacklist command
		handler.q2 = false;
		whitelistedCommandIdsField.set(dispatcher, new HashSet<>());
		notifyCtrlA(shell);
		assertFalse(handler.q2);
	}

	private void notifyCtrlA(Shell shell) {
		Event event = new Event();
		event.type = SWT.KeyDown;
		event.keyCode = SWT.CTRL;
		shell.notifyListeners(SWT.KeyDown, event);

		event = new Event();
		event.type = SWT.KeyDown;
		event.stateMask = SWT.CTRL;
		event.keyCode = 'A';
		shell.notifyListeners(SWT.KeyDown, event);
	}

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@SuppressWarnings("restriction")
	@Test
	public void testMacroIntegrationSaveRestore() throws Exception {
		MacroManager macroManager = new MacroManager();
		try (Closeable closeable = MacroManager.withTemporaryDefaultInstance(macroManager)) {
			File macrosDirectory = folder.getRoot();
			FilenameFilter macrosFilter = new FilenameFilter() {

				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".js");
				}
			};
			assertEquals(0, macrosDirectory.list(macrosFilter).length);

			KeyBindingDispatcher dispatcher = new KeyBindingDispatcher();
			workbenchContext.set(KeyBindingDispatcher.class, dispatcher);
			ContextInjectionFactory.inject(dispatcher, workbenchContext);
			EMacroContext macroContext = workbenchContext.get(EMacroContext.class);
			macroContext.toggleMacroRecord();

			// Usually whitelisted commands are gotten through an extension
			// point, but for testing purposes, let's change the field directly.
			Field whitelistedCommandIdsField = dispatcher.getClass().getDeclaredField("fWhitelistedCommandIds");
			whitelistedCommandIdsField.setAccessible(true);

			final Listener listener = dispatcher.getKeyDownFilter();
			display.addFilter(SWT.KeyDown, listener);
			display.addFilter(SWT.Traverse, listener);

			assertFalse(handler.q2);

			Shell shell = new Shell(display, SWT.NONE);

			// Whitelist command
			whitelistedCommandIdsField.set(dispatcher, new HashSet<>(Arrays.asList(TEST_ID1)));
			notifyCtrlA(shell);

			assertTrue(handler.q2);

			macroContext.toggleMacroRecord();
			// Macro was saved in the dir.
			assertEquals(1, macrosDirectory.list(macrosFilter).length);

			macroManager.reloadMacros();
			throw new AssertionError("Finish this");
		}
	}


	private void processEvents() {
		while (display.readAndDispatch())
			;
	}

	// KEYS >>> Listener.handleEvent(type = KeyDown, stateMask = 0x0, keyCode =
	// 0x40000, time = 2986140, character = 0x0)
	// KEYS >>> Listener.handleEvent(type = KeyDown, stateMask = 0x40000,
	// keyCode = 0x10000, time = 2986218, character = 0x0)
	// KEYS >>> Listener.handleEvent(type = KeyDown, stateMask = 0x50000,
	// keyCode = 0x6c, time = 2986515, character = 0xc)

}

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
package org.eclipse.e4.core.macros.tests;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.e4.core.macros.IMacroCommand;
import org.eclipse.e4.core.macros.IMacroCreator;
import org.eclipse.e4.core.macros.IMacroPlaybackContext;
import org.eclipse.e4.core.macros.internal.MacroManager;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MacroTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private static class PlaybackContext implements IMacroPlaybackContext {

		public StringBuffer buffer = new StringBuffer();

		public void recordPlayback(String name) {
			if (buffer.length() > 0) {
				buffer.append("\n");
			}
			buffer.append(name);
		}
	}

	private static class DummyMacroCommand implements IMacroCommand {

		private String fName;

		public DummyMacroCommand(String name) {
			this.fName = name;
		}

		@Override
		public void execute(IMacroPlaybackContext macroPlaybackContext) {
			((PlaybackContext) macroPlaybackContext).recordPlayback(this.fName);
		}

		@Override
		public String getId() {
			return "dummy";
		}

		@Override
		public String getUserRepresentation() {
			return "Dummy command";
		}

		@Override
		public Map<String, String> toMap() {
			HashMap<String, String> map = new HashMap<>();
			map.put("dummyKey", "dummyValue");
			map.put("name", this.fName);
			return map;
		}

	}

	@Test
	public void testRecordingState() throws Exception {
		MacroManager macroManager = new MacroManager();
		Assert.assertFalse(macroManager.isRecording());
		macroManager.toggleMacroRecord(null, new HashMap<>());
		Assert.assertTrue(macroManager.isRecording());
		macroManager.toggleMacroRecord(null, new HashMap<>());
		Assert.assertFalse(macroManager.isRecording());
	}

	@Test
	public void testAddMacroCommands() throws Exception {
		PlaybackContext playbackContext = new PlaybackContext();
		MacroManager macroManager = new MacroManager();
		Map<String, IMacroCreator> commandIdToCreator = makeCommandIdToCreator();
		macroManager.toggleMacroRecord(null, commandIdToCreator);
		Assert.assertTrue(macroManager.isRecording());
		macroManager.addMacroCommand(new DummyMacroCommand("macro1"));
		macroManager.addMacroCommand(new DummyMacroCommand("macro2"));
		macroManager.toggleMacroRecord(null, commandIdToCreator);
		Assert.assertFalse(macroManager.isRecording());

		macroManager.playbackLastMacro(null, playbackContext, commandIdToCreator);
		Assert.assertEquals("macro1\nmacro2", playbackContext.buffer.toString());
	}

	@Test
	public void testMacroManagerSaveRestore() throws Exception {
		File root = folder.getRoot();
		MacroManager macroManager = new MacroManager(root);
		Map<String, IMacroCreator> commandIdToCreator = makeCommandIdToCreator();
		createMacroWithOneDummyCommand(macroManager, commandIdToCreator);
		String[] macroNames = listTemporaryMacros(root);
		Assert.assertEquals(1, macroNames.length);

		// Create a new macroManager (to force getting from the disk).
		macroManager = new MacroManager(root);
		PlaybackContext playbackContext = new PlaybackContext();
		macroManager.playbackLastMacro(null, playbackContext, commandIdToCreator);
		Assert.assertEquals("macro1", playbackContext.buffer.toString());
	}

	@Test
	public void testMacroManagerMaxNumberOfMacros() throws Exception {
		File root = folder.getRoot();
		MacroManager macroManager = new MacroManager(root);
		macroManager.setmaxNumberOfTemporaryMacros(2);
		Map<String, IMacroCreator> commandIdToCreator = makeCommandIdToCreator();

		createMacroWithOneDummyCommand(macroManager, commandIdToCreator);

		String[] macroNames1 = listTemporaryMacros(root);
		Assert.assertEquals(macroNames1.length, 1);

		// Sleep to make sure that the time of the file will be different
		// (otherwise, if it runs too fast, the same time could be applied to
		// the first, second and last file, in which case we may remove the
		// wrong one).
		sleepABit();
		createMacroWithOneDummyCommand(macroManager, commandIdToCreator);

		String[] macroNames2 = listTemporaryMacros(root);
		Assert.assertEquals(2, macroNames2.length);

		// Now, creating a new one removes the old one
		sleepABit();
		createMacroWithOneDummyCommand(macroManager, commandIdToCreator);

		String[] macroNames3 = listTemporaryMacros(root);
		Assert.assertEquals(2, macroNames3.length);

		Assert.assertTrue(!Arrays.asList(macroNames3).contains(macroNames1[0]));
	}

	private void sleepABit() {
		synchronized (this) {
			try {
				this.wait(2); // 2 millis should be enough for a new timestamp
								// on files.
			} catch (InterruptedException e) {
				// Ignore in this case
			}
		}

	}

	protected String[] listTemporaryMacros(File root) {
		String[] files = root.list(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".js") && name.startsWith("temp_macro");
			}
		});
		return files;
	}

	protected void createMacroWithOneDummyCommand(MacroManager macroManager,
			Map<String, IMacroCreator> commandIdToCreator) {
		macroManager.toggleMacroRecord(null, commandIdToCreator);
		macroManager.addMacroCommand(new DummyMacroCommand("macro1"));
		macroManager.toggleMacroRecord(null, commandIdToCreator);
	}

	private Map<String, IMacroCreator> makeCommandIdToCreator() {
		Map<String, IMacroCreator> commandIdToCreator = new HashMap<>();
		commandIdToCreator.put("dummy", new IMacroCreator() {

			@Override
			public IMacroCommand create(Map<String, String> stringMap) {
				if (stringMap.size() != 2) {
					throw new AssertionError("Expected map size to be 2. Found: " + stringMap.size());
				}
				if (!stringMap.get("dummyKey").equals("dummyValue")) {
					throw new AssertionError("Did not find dummyKey->dummyValue mapping.");
				}
				if (stringMap.get("name") == null) {
					throw new AssertionError("Expected name to be defined.");
				}

				return new DummyMacroCommand(stringMap.get("name"));
			}
		});
		return commandIdToCreator;
	}

}

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
package org.eclipse.e4.ui.macros.tests;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.e4.core.macros.EMacroService;
import org.eclipse.e4.core.macros.IMacroInstruction;
import org.eclipse.e4.core.macros.IMacroInstructionFactory;
import org.eclipse.e4.core.macros.IMacroPlaybackContext;
import org.eclipse.e4.core.macros.IMacroStateListener;
import org.eclipse.e4.core.macros.internal.MacroManager;
import org.eclipse.e4.core.macros.internal.MacroServiceImplementation;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MacroTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private static class PlaybackContext implements IMacroPlaybackContext {

		public StringBuffer buffer = new StringBuffer();

		private Map<String, IMacroInstructionFactory> fMacroInstructionIdToFactory;

		public PlaybackContext(Map<String, IMacroInstructionFactory> macroInstructionIdToFactory) {
			this.fMacroInstructionIdToFactory = macroInstructionIdToFactory;
		}

		public void recordPlayback(String name) {
			if (buffer.length() > 0) {
				buffer.append("\n");
			}
			buffer.append(name);
		}

		@Override
		public IMacroInstruction createMacroInstruction(String macroInstructionId, Map<String, String> stringMap)
				throws Exception {
			return fMacroInstructionIdToFactory.get(macroInstructionId).create(stringMap);
		}
	}

	private static class DummyMacroInstruction implements IMacroInstruction {

		private String fName;

		public DummyMacroInstruction(String name) {
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
		public String toString() {
			return "Dummy macro instruction";
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
		MacroServiceImplementation macroService = new MacroServiceImplementation(null, null);
		File root = folder.getRoot();
		macroService.getMacroManager().setMacrosDirectories(root);
		final StringBuilder buf = new StringBuilder();
		macroService.getMacroManager().addMacroStateListener(new IMacroStateListener() {

			@Override
			public void macroStateChanged(EMacroService macroService) {
				buf.append("rec: ").append(macroService.isRecording()).append(" play: ")
						.append(macroService.isPlayingBack()).append("\n");
			}
		});

		Assert.assertFalse(macroService.isRecording());
		Assert.assertEquals("", buf.toString());

		macroService.toggleMacroRecord();
		Assert.assertEquals("rec: true play: false\n", buf.toString());
		buf.setLength(0);
		Assert.assertTrue(macroService.isRecording());

		macroService.toggleMacroRecord();
		Assert.assertEquals("rec: false play: false\n", buf.toString());
		buf.setLength(0);
		Assert.assertFalse(macroService.isRecording());

		macroService.playbackLastMacro();
		Assert.assertEquals("rec: false play: true\n"
				+ "rec: false play: false\n", buf.toString());
		buf.setLength(0);

		macroService.toggleMacroRecord();
		macroService.playbackLastMacro();
		macroService.toggleMacroRecord();
		Assert.assertEquals(
				"rec: true play: false\n"
				+ "rec: true play: true\n"
				+ "rec: true play: false\n"
				+ "rec: false play: false\n",
				buf.toString());
	}

	@Test
	public void testAddMacroInstructions() throws Exception {
		MacroManager macroManager = new MacroManager();
		Map<String, IMacroInstructionFactory> macroInstructionIdToFactory = makeMacroInstructionIdToFactory();
		PlaybackContext playbackContext = new PlaybackContext(macroInstructionIdToFactory);
		macroManager.toggleMacroRecord(null, macroInstructionIdToFactory);
		Assert.assertTrue(macroManager.isRecording());
		macroManager.addMacroInstruction(new DummyMacroInstruction("macro1"));
		macroManager.addMacroInstruction(new DummyMacroInstruction("macro2"));
		macroManager.toggleMacroRecord(null, macroInstructionIdToFactory);
		Assert.assertFalse(macroManager.isRecording());

		macroManager.playbackLastMacro(null, playbackContext);
		Assert.assertEquals("macro1\nmacro2", playbackContext.buffer.toString());
	}

	@Test
	public void testMacroManagerSaveRestore() throws Exception {
		File root = folder.getRoot();
		MacroManager macroManager = new MacroManager(root);
		Map<String, IMacroInstructionFactory> macroInstructionIdToFactory = makeMacroInstructionIdToFactory();
		createMacroWithOneDummyMacroInstruction(macroManager, macroInstructionIdToFactory);
		String[] macroNames = listTemporaryMacros(root);
		Assert.assertEquals(1, macroNames.length);

		// Create a new macroManager (to force getting from the disk).
		macroManager = new MacroManager(root);
		PlaybackContext playbackContext = new PlaybackContext(macroInstructionIdToFactory);
		macroManager.playbackLastMacro(null, playbackContext);
		Assert.assertEquals("macro1", playbackContext.buffer.toString());
	}

	@Test
	public void testMacroManagerMaxNumberOfMacros() throws Exception {
		File root = folder.getRoot();
		MacroManager macroManager = new MacroManager(root);
		macroManager.setMaxNumberOfTemporaryMacros(2);
		Map<String, IMacroInstructionFactory> macroInstructionIdToFactory = makeMacroInstructionIdToFactory();

		createMacroWithOneDummyMacroInstruction(macroManager, macroInstructionIdToFactory);

		String[] macroNames1 = listTemporaryMacros(root);
		Assert.assertEquals(macroNames1.length, 1);

		// Sleep to make sure that the time of the file will be different
		// (otherwise, if it runs too fast, the same time could be applied to
		// the first, second and last file, in which case we may remove the
		// wrong one).
		sleepABit();
		createMacroWithOneDummyMacroInstruction(macroManager, macroInstructionIdToFactory);

		String[] macroNames2 = listTemporaryMacros(root);
		Assert.assertEquals(2, macroNames2.length);

		// Now, creating a new one removes the old one
		sleepABit();
		createMacroWithOneDummyMacroInstruction(macroManager, macroInstructionIdToFactory);

		String[] macroNames3 = listTemporaryMacros(root);
		Assert.assertEquals(2, macroNames3.length);

		Assert.assertTrue(!Arrays.asList(macroNames3).contains(macroNames1[0]));
	}

	@Test
	public void testMacroInstructionWithRelatedEventAndPriority() {
		File root = folder.getRoot();
		MacroManager macroManager = new MacroManager(root);
		Map<String, IMacroInstructionFactory> macroInstructionIdToFactory = makeMacroInstructionIdToFactory();

		macroManager.toggleMacroRecord(null, macroInstructionIdToFactory);

		// macro1 is added
		macroManager.addMacroInstruction(new DummyMacroInstruction("macro1"));

		// macro replace will be replaced when another event of the same
		// priority is added
		macroManager.addMacroInstruction(new DummyMacroInstruction("macro replace"), "event1", 1);
		macroManager.addMacroInstruction(new DummyMacroInstruction("macro2"), "event1", 1);

		// macro replace 2 will be replaced when another event of higher
		// priority is added
		macroManager.addMacroInstruction(new DummyMacroInstruction("macro replace 2"), "event2", 1);
		macroManager.addMacroInstruction(new DummyMacroInstruction("macro3"), "event2", 2);

		// macro keep will not be replaced when another event of lower priority
		// is added
		macroManager.addMacroInstruction(new DummyMacroInstruction("macro keep"), "event3", 1);
		macroManager.addMacroInstruction(new DummyMacroInstruction("macro4"), "event3", 0);

		macroManager.toggleMacroRecord(null, macroInstructionIdToFactory);

		PlaybackContext playbackContext = new PlaybackContext(macroInstructionIdToFactory);
		macroManager.playbackLastMacro(null, playbackContext);
		Assert.assertEquals("macro1\nmacro2\nmacro3\nmacro keep", playbackContext.buffer.toString());

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

	protected void createMacroWithOneDummyMacroInstruction(MacroManager macroManager,
			Map<String, IMacroInstructionFactory> macroInstructionIdToFactory) {
		macroManager.toggleMacroRecord(null, macroInstructionIdToFactory);
		macroManager.addMacroInstruction(new DummyMacroInstruction("macro1"));
		macroManager.toggleMacroRecord(null, macroInstructionIdToFactory);
	}

	private Map<String, IMacroInstructionFactory> makeMacroInstructionIdToFactory() {
		Map<String, IMacroInstructionFactory> macroInstructionIdToFactory = new HashMap<>();
		macroInstructionIdToFactory.put("dummy", new IMacroInstructionFactory() {

			@Override
			public IMacroInstruction create(Map<String, String> stringMap) {
				if (stringMap.size() != 2) {
					throw new AssertionError("Expected map size to be 2. Found: " + stringMap.size());
				}
				if (!stringMap.get("dummyKey").equals("dummyValue")) {
					throw new AssertionError("Did not find dummyKey->dummyValue mapping.");
				}
				if (stringMap.get("name") == null) {
					throw new AssertionError("Expected name to be defined.");
				}

				return new DummyMacroInstruction(stringMap.get("name"));
			}
		});
		return macroInstructionIdToFactory;
	}

}

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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.e4.core.macros.Activator;
import org.eclipse.e4.core.macros.EMacroContext;
import org.eclipse.e4.core.macros.IMacroCommand;
import org.eclipse.e4.core.macros.IMacroCreator;
import org.eclipse.e4.core.macros.IMacroListener;
import org.eclipse.e4.core.macros.IMacroPlaybackContext;

/**
 * This is the private API for dealing with macros (implementation detail: a
 * single, static manager is used to deal with macros).
 */
public class MacroManager {

	/*
	 * Static methods/fields.
	 */

	/**
	 * A static, shared instance of the macro manager.
	 */
	private static MacroManager fDefaultMacroManager;

	public static void createDefaultInstance(File... macrosDirectory) {
		fDefaultMacroManager = new MacroManager(macrosDirectory);
	}

	public static MacroManager getDefaultInstance() {
		return fDefaultMacroManager;
	}

	public static void disposeDefaultInstance() {
		fDefaultMacroManager = null;
	}

	/**
	 * Helper to temporarily set a different default macro manager (to be used
	 * when testing).
	 *
	 * <code>
	 * try (Closeable closeable = MacroManager.withTemporaryDefaultInstance(macroManager)) {
	 *    ...
	 * }
	 * </code>
	 *
	 * @param temporaryMacroManager
	 *            a macro manager to be set temporarily.
	 * @return a Closeable which will restore the previous macro manager later
	 *         on.
	 */
	public static Closeable withTemporaryDefaultInstance(MacroManager temporaryMacroManager) {
		final MacroManager initial = fDefaultMacroManager;
		fDefaultMacroManager = temporaryMacroManager;
		return new Closeable() {

			@Override
			public void close() {
				fDefaultMacroManager = initial;
			}
		};
	}

	private static final String JS_EXT = ".js"; //$NON-NLS-1$

	private static final String TEMP_MACRO_PREFIX = "temp_macro_"; //$NON-NLS-1$

	/*
	 * Methods from the instance.
	 */

	/**
	 * The max number of temporary macros to be kept (can't be lower than 1).
	 */
	private int fMaxNumberOfTemporaryMacros = 5;

	/**
	 * @param maxNumberOfTemporaryMacros
	 *            The max number of temporary macros to be kept.
	 */
	public void setmaxNumberOfTemporaryMacros(int maxNumberOfTemporaryMacros) {
		Assert.isTrue(maxNumberOfTemporaryMacros >= 1);
		this.fMaxNumberOfTemporaryMacros = maxNumberOfTemporaryMacros;
	}

	/**
	 * @return Returns the max number of temporary macros to be kept.
	 */
	public int getMaxNumberOfTemporaryMacros() {
		return fMaxNumberOfTemporaryMacros;
	}

	/**
	 * The directories where macros should be looked up. The first directory is
	 * the one where macros are persisted.
	 */
	private final File[] fMacrosDirectories;

	/**
	 * Holds the macro currently being recorded (if we're in record mode). If
	 * not in record mode, should be null.
	 */
	private ComposableMacro fMacroBeingRecorded;

	/**
	 * Holds the last recorded or played back macro.
	 */
	private IMacro fLastMacro;

	/**
	 * Flag indicating whether we're playing back a macro.
	 */
	private boolean fIsPlayingBack = false;

	/**
	 * Creates a manager for macros which will read macros from the given
	 * directory.
	 *
	 * @param macrosDirectories
	 *            the directories where macros should be looked up. The first
	 *            directory is the one where macros are persisted. If there are
	 *            2 macros which would end up having the same name, the one in
	 *            the directory that appears last is the one which is used.
	 */
	public MacroManager(File... macrosDirectories) {
		Assert.isNotNull(macrosDirectories);
		for (File file : macrosDirectories) {
			Assert.isNotNull(file);
		}
		this.fMacrosDirectories = macrosDirectories;
		reloadMacros();
	}

	/**
	 * @return whether a macro is currently being recorded.
	 */
	public boolean isRecording() {
		return fMacroBeingRecorded != null;
	}

	/**
	 * @return whether a macro is currently being played back.
	 */
	public boolean isPlayingBack() {
		return fIsPlayingBack;
	}

	/**
	 * Adds a macro command to the macro currently being recorded. Does nothing
	 * if no macro is being recorded.
	 *
	 * @param macroCommand
	 *            the macro command to be recorded.
	 */
	public void addMacroCommand(IMacroCommand macroCommand) {
		IComposableMacro macroBeingRecorded = fMacroBeingRecorded;
		if (macroBeingRecorded != null) {
			macroBeingRecorded.addMacroCommand(macroCommand);
		}
	}

	/**
	 * Toggles the macro record (either starts recording or stops an existing
	 * record).
	 *
	 * @param macroContext
	 *            the context to record macros.
	 * @param commandIdToCreator
	 *            a mapping of the available command ids to the factory method
	 *            to create such macro commands.
	 */
	public void toggleMacroRecord(final EMacroContext macroContext, Map<String, IMacroCreator> commandIdToCreator) {
		if (fIsPlayingBack) {
			// Can't toggle the macro record mode while playing back.
			return;
		}
		try {
			if (fMacroBeingRecorded == null) {
				fMacroBeingRecorded = new ComposableMacro(commandIdToCreator);
			} else {
				try {
					saveTemporaryMacro(fMacroBeingRecorded);
				} finally {
					fLastMacro = fMacroBeingRecorded;
					fMacroBeingRecorded = null;
				}
			}
		} finally {
			notifyMacroStateChange(macroContext);
		}

	}

	/**
	 * Helper class to store a path an a time.
	 */
	private static final class PathAndTime {

		private Path fPath;
		private long fLastModified;

		public PathAndTime(Path p, long lastModified) {
			this.fPath = p;
			this.fLastModified = lastModified;
		}

	}

	/**
	 * @param macro
	 *            the macro to be recorded as a temporary macro.
	 */
	private void saveTemporaryMacro(ComposableMacro macro) {
		if (fMacrosDirectories == null || this.fMacrosDirectories.length == 0) {
			return;
		}
		// The first one is the one we use as a working directory to store
		// temporary macros.
		File macroDirectory = this.fMacrosDirectories[0];
		if (!macroDirectory.isDirectory()) {
			return;
		}

		ArrayList<PathAndTime> pathAndTime = listTemporaryMacrosPathAndTime(macroDirectory);

		try {
			Path tempFile = Files.createTempFile(Paths.get(macroDirectory.toURI()), TEMP_MACRO_PREFIX, JS_EXT);
			Files.write(tempFile, macro.toJSBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE,
					StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e1) {
			Activator.log(e1);
			return; // Can't create file at expected place;
		}

		// Remove older files
		while (pathAndTime.size() >= fMaxNumberOfTemporaryMacros) {
			PathAndTime removeFile = pathAndTime.remove(pathAndTime.size() - 1);
			try {
				Files.deleteIfExists(removeFile.fPath);
			} catch (Exception e) {
				Activator.log(e);
			}
		}
	}

	/**
	 * @param macroDirectory
	 *            the directory from where we should get the temporary macros.
	 *
	 * @return The path/time for the temporary macros at a given directory as an
	 *         array list sorted such that the last element is the oldest one
	 *         and the first is the newest.
	 */
	private ArrayList<PathAndTime> listTemporaryMacrosPathAndTime(File macroDirectory) {
		// It's a sorted list and not a tree map to deal with the case of
		// multiple times pointing to the same file (although hard to happen,
		// it's not impossible).
		ArrayList<PathAndTime> pathAndTime = new ArrayList<>();

		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(macroDirectory.toURI()),
				new DirectoryStream.Filter<Path>() {

					@Override
					public boolean accept(Path entry) {
						String name = entry.getFileName().toString().toLowerCase();
						return name.startsWith(TEMP_MACRO_PREFIX) && name.endsWith(JS_EXT);
					}
				})) {
			for (Path p : directoryStream) {
				pathAndTime.add(new PathAndTime(p, Files.getLastModifiedTime(p).to(TimeUnit.NANOSECONDS)));
			}
		} catch (IOException e1) {
			Activator.log(e1);
		}

		// Sort by reversed modified time (because it's faster to remove the
		// last element from an ArrayList later on).
		Collections.sort(pathAndTime, new Comparator<PathAndTime>() {

			@Override
			public int compare(PathAndTime o1, PathAndTime o2) {
				return Long.compare(o2.fLastModified, o1.fLastModified);
			}
		});
		return pathAndTime;
	}

	/**
	 * Notifies that a macro state change occurred (see
	 * {@link org.eclipse.e4.core.macros.IMacroListener}).
	 *
	 * @param macroContext
	 *            the macro context where such change took place.
	 */
	private void notifyMacroStateChange(final EMacroContext macroContext) {
		for (final IMacroListener listener : fListeners) {
			SafeRunner.run(new ISafeRunnable() {

				@Override
				public void run() throws Exception {
					listener.onMacroStateChanged(macroContext);
				}

				@Override
				public void handleException(Throwable exception) {
					Activator.log(exception);
				}
			});
		}
	}

	/**
	 * Plays back the last recorded macro.
	 *
	 * @param macroContext
	 *            the macro context (used to notify listeners of the change.
	 * @param macroPlaybackContext
	 *            a context to be used to playback the macro (passed to the
	 *            macro to be played back).
	 * @param commandIdToCreator
	 *            map with the command id to the creator of the macro with that
	 *            command id.
	 */
	public void playbackLastMacro(EMacroContext macroContext, final IMacroPlaybackContext macroPlaybackContext,
			final Map<String, IMacroCreator> commandIdToCreator) {
		if (fLastMacro != null && !fIsPlayingBack) {
			// Note that we can play back while recording, but we can't change
			// the recording mode while playing back.
			fIsPlayingBack = true;
			try {
				notifyMacroStateChange(macroContext);
				SafeRunner.run(new ISafeRunnable() {

					@Override
					public void run() throws Exception {
						fLastMacro.setCommandIdToCreator(commandIdToCreator);
						fLastMacro.playback(macroPlaybackContext);
					}

					@Override
					public void handleException(Throwable exception) {
						Activator.log(exception);
					}
				});
			} finally {
				fIsPlayingBack = false;
			}
		}
	}

	/**
	 * A list with the listeners to be notified of changes in the macro context.
	 */
	private final ListenerList<IMacroListener> fListeners = new ListenerList<>();

	/**
	 * Adds a macro listener to be notified on changes in the macro
	 * record/playback state.
	 *
	 * @param listener
	 *            the listener to be added.
	 */
	public void addMacroListener(IMacroListener listener) {
		fListeners.add(listener);
	}

	/**
	 * @param listener
	 *            the macro listener which should no longer be notified of
	 *            changes.
	 */
	public void removeMacroListener(IMacroListener listener) {
		fListeners.remove(listener);
	}

	/**
	 * Reloads the macros available.
	 */
	public void reloadMacros() {
		for (File macroDirectory : this.fMacrosDirectories) {
			if (macroDirectory.isDirectory()) {
				ArrayList<PathAndTime> listPathsAndTimes = listTemporaryMacrosPathAndTime(macroDirectory);
				if (listPathsAndTimes.size() > 0) {
					this.fLastMacro = new SavedJSMacro(listPathsAndTimes.get(0).fPath.toFile());
				}
			} else {
				Activator.log(new RuntimeException(String.format("Expected: %s to be a directory.", macroDirectory))); //$NON-NLS-1$
			}
		}
	}
}

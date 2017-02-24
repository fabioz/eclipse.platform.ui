package org.eclipse.e4.ui.macros.internal.editors;

import org.eclipse.e4.core.macros.EMacroService;
import org.eclipse.e4.core.macros.IMacroStateListener;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 *
 */
public class EditorsMacroInstructionGenerator implements IMacroStateListener {


	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.e4.core.macros.IMacroStateListener#macroStateChanged(org.
	 * eclipse.e4.core.macros.EMacroService)
	 */
	@Override
	public void macroStateChanged(EMacroService macroService) {
		IWorkbenchWindow[] workbenchWindows = PlatformUI.getWorkbench().getWorkbenchWindows();
		for (IWorkbenchWindow iWorkbenchWindow : workbenchWindows) {

		}
		IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if(activeWorkbenchWindow == null) {
			return;
		}
		IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
		if(activePage == null) {
			return;
		}

		IEditorPart activeEditor = activePage.getActiveEditor();
		if(activeEditor == null) {
			return;
		}

		activePage.addPartListener(listener);

		if (macroService.isRecording() || macroService.isPlayingBack()) {
			platform
		}
	}

}

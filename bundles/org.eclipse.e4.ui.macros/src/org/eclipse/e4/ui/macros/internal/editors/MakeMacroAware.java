package org.eclipse.e4.ui.macros.internal.editors;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 *
 */
public class MakeMacroAware {

	/**
	 *
	 */
	private static class MacroPartListener implements IPartListener2 {
		@Override
		public void partVisible(IWorkbenchPartReference partRef) {
			activateMacro(partRef);
		}

		@Override
		public void partOpened(IWorkbenchPartReference partRef) {
			activateMacro(partRef);
		}

		@Override
		public void partInputChanged(IWorkbenchPartReference partRef) {

		}

		@Override
		public void partHidden(IWorkbenchPartReference partRef) {

		}

		@Override
		public void partDeactivated(IWorkbenchPartReference partRef) {

		}

		@Override
		public void partClosed(IWorkbenchPartReference partRef) {

		}

		@Override
		public void partBroughtToTop(IWorkbenchPartReference partRef) {

		}

		@Override
		public void partActivated(IWorkbenchPartReference partRef) {
			activateMacro(partRef);
		}

		protected void activateMacro(IWorkbenchPartReference partRef) {
		}
	}

	IPartListener2 partListener = new MacroPartListener();

	private void addListeners(IWorkbenchWindow window) {
		window.getPartService().addPartListener(partListener);
	}


	public void install() {
		// Fix future windows (add listeners)
		for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			addListeners(window);
		}

		// Start listening current editors
		IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
		for (IWorkbenchWindow window : windows) {
			for (IWorkbenchPage page : window.getPages()) {
				for (IEditorReference ref : page.getEditorReferences()) {
					IEditorPart part = ref.getEditor(false);
					if (part != null) {
						partListener.partOpened(ref);
					}
				}
			}
		}

		PlatformUI.getWorkbench().addWindowListener(new IWindowListener() {
			@Override
			public void windowOpened(IWorkbenchWindow window) {
				addListeners(window);
			}

			@Override
			public void windowClosed(IWorkbenchWindow window) {
			}

			@Override
			public void windowActivated(IWorkbenchWindow window) {
				addListeners(window);
			}

			@Override
			public void windowDeactivated(IWorkbenchWindow window) {
			}
		});
	}

}

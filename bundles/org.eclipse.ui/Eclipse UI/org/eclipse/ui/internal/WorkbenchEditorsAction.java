package org.eclipse.ui.internal;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.ui.internal.dialogs.WorkbenchEditorsDialog;

/**
 * Implements a action to open a dialog showing all open editors
 * and the recent closed editors.
 */
public class WorkbenchEditorsAction extends Action {

	WorkbenchWindow window;

	/**
	 * Constructor for NavigateWorkbenchAction.
	 * @param text
	 */
	public WorkbenchEditorsAction(WorkbenchWindow window) {
		super(WorkbenchMessages.getString("WorkbenchEditorsAction.label")); //$NON-NLS-1$
		setAccelerator(SWT.CTRL | SWT.SHIFT | 'W');
		this.window = window;

	}
	public void run() {
		new WorkbenchEditorsDialog(window).open();
	}
}
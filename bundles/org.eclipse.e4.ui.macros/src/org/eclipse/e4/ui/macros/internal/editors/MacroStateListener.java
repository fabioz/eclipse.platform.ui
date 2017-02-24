package org.eclipse.e4.ui.macros.internal.editors;

import org.eclipse.e4.core.macros.EMacroService;
import org.eclipse.e4.core.macros.IMacroStateListener;
import org.eclipse.e4.ui.macros.styled_text.StyledTextMacroRecorder;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;

/**
 * Helper class to deal with entering/exiting macro record/playback.
 *
 * @since 3.11
 */
public final class MacroStateListener implements IMacroStateListener {

	/**
	 * null when not in macro record/playback. Used to check the current
	 * state and store information to be restored when exiting macro mode.
	 *
	 */
	private IMemento fMementoStateBeforeMacro;
	/**
	 * Helper class to record keystrokes on the StyledText.
	 */
	private StyledTextMacroRecorder fStyledTextMacroRecorder;
	/**
	 * Constant used to save whether the content assist was enabled before
	 * being disabled in disableCodeCompletion.
	 */
	private static final String CONTENT_ASSIST_ENABLED = "contentAssistEnabled";//$NON-NLS-1$

	/**
	 * Re-enables the content assist based on the state of the key
	 * {@link #CONTENT_ASSIST_ENABLED} in the passed memento.
	 *
	 * @param memento
	 *            the memento where a key with
	 *            {@link #CONTENT_ASSIST_ENABLED} with the enabled state of
	 *            the content assist to be restored.
	 */
	private void restoreContentAssist(IMemento memento) {
		ITextOperationTarget textOperationTarget = AbstractTextEditor.this.getAdapter(ITextOperationTarget.class);
		if (textOperationTarget instanceof ITextOperationTargetExtension) {
			ITextOperationTargetExtension targetExtension = (ITextOperationTargetExtension) textOperationTarget;
			if (textOperationTarget instanceof ITextOperationTargetExtension) {
				Boolean contentAssistProposalsBeforMacroMode = memento.getBoolean(CONTENT_ASSIST_ENABLED);
				if (contentAssistProposalsBeforMacroMode != null) {
					if ((contentAssistProposalsBeforMacroMode).booleanValue()) {
						targetExtension.enableOperation(ISourceViewer.CONTENTASSIST_PROPOSALS, true);
					} else {
						targetExtension.enableOperation(ISourceViewer.CONTENTASSIST_PROPOSALS, false);
					}
				}
			}
		}
	}

	/**
	 * Disables the content assist and saves the previous state on the
	 * passed memento (note that it's only saved if it is actually disabled
	 * here).
	 *
	 * @param memento
	 *            memento where the previous state should be saved, to be
	 *            properly restored later on in
	 *            {@link #restoreContentAssist(IMemento)}.
	 */
	private void disableContentAssist(IMemento memento) {
		ITextOperationTarget textOperationTarget = AbstractTextEditor.this.getAdapter(ITextOperationTarget.class);
		if (textOperationTarget instanceof ITextOperationTargetExtension) {
			ITextOperationTargetExtension targetExtension = (ITextOperationTargetExtension) textOperationTarget;

			// Disable content assist and mark it to be restored
			// later on
			if (textOperationTarget.canDoOperation(ISourceViewer.CONTENTASSIST_PROPOSALS)) {
				memento.putBoolean(CONTENT_ASSIST_ENABLED, true);
				targetExtension.enableOperation(ISourceViewer.CONTENTASSIST_PROPOSALS, false);
			}
		}
	}

	/**
	 * Implemented to properly deal with macro recording/playback (i.e.: the
	 * editor may need to disable content assist during macro recording and
	 * it needs to record keystrokes to be played back afterwards).
	 *
	 * @see org.eclipse.e4.core.macros.IMacroStateListener#macroStateChanged(org.eclipse.e4.core.macros.EMacroService)
	 */
	@Override
	public void macroStateChanged(EMacroService macroService) {
		if (macroService.isPlayingBack()
				|| (macroService.isRecording() && getDisableContentAssistOnMacroRecord())) {
			// We always need to disable content assist on playback and we
			// selectively disable it on record based on
			// the return of getDisableContentAssistOnMacroRecord(), which
			// subclasses may override.
			if (fMementoStateBeforeMacro == null) {
				fMementoStateBeforeMacro = XMLMemento.createWriteRoot("AbstractTextEditorXmlMemento"); //$NON-NLS-1$
				disableContentAssist(fMementoStateBeforeMacro);
			}
		} else {
			if (fMementoStateBeforeMacro != null) {
				// Restores content assist if it was disabled (based on the
				// memento)
				restoreContentAssist(fMementoStateBeforeMacro);
				fMementoStateBeforeMacro = null;
			}
		}

		// When recording install a recorder for key events (and uninstall
		// if not recording).
		if (macroService.isRecording()) {
			if (fStyledTextMacroRecorder == null) {
				ISourceViewer viewer = fSourceViewer;
				if (viewer != null && macroService.isRecording()) {
					StyledText textWidget = viewer.getTextWidget();
					if (textWidget != null && !textWidget.isDisposed()) {
						fStyledTextMacroRecorder = new StyledTextMacroRecorder(macroService);
						fStyledTextMacroRecorder.install(textWidget);
					}
				}
			}
		} else { // !macroService.isRecording()
			if (fStyledTextMacroRecorder != null) {
				ISourceViewer viewer = fSourceViewer;
				if (viewer != null && !macroService.isRecording()) {
					StyledText textWidget = viewer.getTextWidget();
					if (textWidget != null && !textWidget.isDisposed()) {
						fStyledTextMacroRecorder.uninstall(textWidget);
						fStyledTextMacroRecorder = null;
					}
				}
			}
		}
	}
}
package org.eclipse.e4.ui.bindings.keys;

import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.swt.widgets.Event;

/**
 * Interface for an contribution to the KeyBindingDispatcher for listening and
 * handling command dispatch.
 * <p>
 * This interface is provisional and not yet considered API.
 *
 * @noreference
 * @noimplement
 *
 * @since 0.13
 */
public interface IKeyBindingInterceptor {

	/**
	 * Called when a keybinding has been successfully matched to a command,
	 * which should be dispatched. The interceptor may handle the dispatch.
	 *
	 * @param parameterizedCommand
	 *            the command to check for interception.
	 * @param event
	 *            the event that triggered the command.
	 * @return true if this interceptor will handle the dispatching of this
	 *         command, in which case all handling by the KeyBindingDispatcher
	 *         will be stopped and false otherwise.
	 */
	boolean executeCommand(ParameterizedCommand parameterizedCommand, Event event);

	/**
	 * Called after a given command is executed.
	 *
	 * @param trigger
	 *            the event that triggered the command.
	 * @param parameterizedCommand
	 *            the command that was executed.
	 * @param commandDefined
	 *            whether the command was actually defined.
	 * @param commandHandled
	 *            whether the command was actually handled.
	 */
	void postExecuteCommand(ParameterizedCommand parameterizedCommand, Event trigger, boolean commandDefined,
			boolean commandHandled);

}

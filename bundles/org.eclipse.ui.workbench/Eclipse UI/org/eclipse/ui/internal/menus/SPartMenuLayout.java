/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.ui.internal.menus;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.commands.common.NotDefinedException;

/**
 * <p>
 * A menu layout that includes the bars and the context menus.
 * </p>
 * <p>
 * Clients must not instantiate, and must not extend.
 * </p>
 * <p>
 * <strong>PROVISIONAL</strong>. This class or interface has been added as
 * part of a work in progress. There is a guarantee neither that this API will
 * work nor that it will remain the same. Please do not use this API without
 * consulting with the Platform/UI team.
 * </p>
 * <p>
 * This class will eventually exist in <code>org.eclipse.jface.menus</code>.
 * </p>
 * 
 * @since 3.2
 */
class SPartMenuLayout {

	/**
	 * <p>
	 * Each character in this string represents a path delimiter that is
	 * understood by this layout class when parsing paths. This can be passed
	 * directly to a string tokenizer for parsing, or the first character can be
	 * used for programmatically generating paths.
	 * </p>
	 * <p>
	 * The character "<code>/</code>" is guaranteed to be a path delimiter.
	 * </p>
	 */
	public static final String PATH_DELIMITERS = "/"; //$NON-NLS-1$

	/**
	 * Inserts a particular path into a map. The layout is represented by a
	 * nesting structure of maps.
	 * 
	 * @param element
	 *            The element to insert into the node; must not be
	 *            <code>null</code>.
	 * @param location
	 *            The location at which the element is to be inserted; may be
	 *            <code>null</code>.
	 * @param locationElement
	 *            The element containining the path at which element should be
	 *            inserted; must not be <code>null</code>.
	 * @param node
	 *            The top-level node representing the layout; must not be
	 *            <code>null</code>.
	 * @throws NotDefinedException
	 *             If the given menu element is not defined.
	 */
	protected static final void insertElementIntoNode(
			final MenuElement element, final SLocation location,
			final LeafLocationElement locationElement, LayoutNode node)
			throws NotDefinedException {
		final ILocationElementTokenizer tokenizer = locationElement
				.getTokenizer();
		while (tokenizer.hasMoreTokens()) {
			final LocationElementToken token = tokenizer.nextToken();
			node = node.getChildNode(token);
		}

		node.createChildNode(element, location);
	}

	/**
	 * The top-level bars for this menu layout, indexed by type ({@link String}).
	 * Each bar is represented by a top-level {@link LayoutNode} with no menu
	 * element. Never <code>null</code>.
	 */
	private final Map barsByType;

	/**
	 * The menu layout for the context menus, indexed by the context menu
	 * identifiers ({@link String}). Each menu is represented by a top-level
	 * {@link LayoutNode} with no menu element. Never <code>null</code>.
	 */
	private final Map popupsById;

	/**
	 * Constructs a new instance of <code>SMenuLayout</code>.
	 */
	protected SPartMenuLayout() {
		this.barsByType = new HashMap();
		this.popupsById = new HashMap();
	}

	/**
	 * Adds an entry into one of the top-level bars. A bar can be a menu bar, a
	 * tool bar, or a status line.
	 * 
	 * @param element
	 *            The element to insert into the node; must not be
	 *            <code>null</code>.
	 * @param location
	 *            The location at which the element is to be inserted; may be
	 *            <code>null</code>.
	 * @param bar
	 *            The location in a bar; must not be <code>null</code>.
	 * @throws NotDefinedException
	 *             If the given menu element is not defined.
	 */
	protected final void addBar(final MenuElement element,
			final SLocation location, final SBar bar)
			throws NotDefinedException {
		final String type = bar.getType();
		LayoutNode node = (LayoutNode) barsByType.get(type);
		if (node == null) {
			node = new LayoutNode();
			barsByType.put(type, node);
		}
		insertElementIntoNode(element, location, bar, node);
	}

	/**
	 * Adds an entry into a popup menu.
	 * 
	 * @param element
	 *            The element to insert into the popup; must not be
	 *            <code>null</code>.
	 * @param location
	 *            The location at which the element is to be inserted; may be
	 *            <code>null</code>.
	 * @param popup
	 *            The popup indicator; must not be <code>null</code>.
	 * @throws NotDefinedException
	 *             If the given menu element is not defined.
	 */
	protected final void addPopup(final MenuElement element,
			final SLocation location, final SPopup popup)
			throws NotDefinedException {
		final String popupId = popup.getId();
		LayoutNode node = (LayoutNode) popupsById.get(popupId);
		if (node == null) {
			node = new LayoutNode();
			popupsById.put(popupId, node);
		}
		insertElementIntoNode(element, location, popup, node);
	}

	/**
	 * <p>
	 * Returns the layout node representing the root of the bar of the given
	 * type.
	 * </p>
	 * 
	 * @param type
	 *            The type of the bar to retrieve; should not be
	 *            <code>null</code>.
	 * @return The layout node corresponding to the type; <code>null</code> if
	 *         there are no bars of that type, or if that bar is currently
	 *         empty.
	 * @see {@link #getMenuBar()}
	 */
	public final ILayoutNode getBar(final String type) {
		return (ILayoutNode) barsByType.get(type);
	}

	/**
	 * <p>
	 * Returns the layout node representing the root of the menu bar.
	 * </p>
	 * 
	 * @return The root menu bar layout node; may be <code>null</code> if
	 *         there are no items in the menu bar.
	 */
	public final ILayoutNode getMenuBar() {
		return (ILayoutNode) barsByType.get(SBar.TYPE_MENU);
	}

	/**
	 * Returns the bars indexed by type. Generally, a bar will be a menu bar, a
	 * tool bar or a status line.
	 * 
	 * @return The map of bar nodes ({@link LayoutNode} indexed by type; never
	 *         <code>null</code>;
	 */
	protected final Map getBarsByType() {
		return barsByType;
	}

	/**
	 * Returns the popup menus indexed by identifiers.
	 * 
	 * @return The map of context menu nodes ({@link LayoutNode}) indexed by
	 *         context menu identifier.
	 */
	protected final Map getPopupsById() {
		return popupsById;
	}

	/**
	 * This is a debugging method for printing node information. The method is
	 * recursive.
	 * 
	 * @param node
	 *            The node which should be printed (along with its children) to
	 *            the buffer; must not be <code>null</code>
	 * @param buffer
	 *            The buffer to which to print; must not be <code>null</code>.
	 * @param indent
	 *            The identation level for the node; must be a whole number.
	 */
	protected final void printNode(final LayoutNode node,
			final StringBuffer buffer, final int indent) {
		for (int i = 0; i < indent; i++) {
			buffer.append(' ');
		}
		buffer.append(node.getMenuElement());
		buffer.append('\n');
		final Collection children = node.getChildrenSorted();
		final Iterator childItr = children.iterator();
		while (childItr.hasNext()) {
			final LayoutNode childNode = (LayoutNode) childItr.next();
			printNode(childNode, buffer, indent + 2);
		}
	}

	/**
	 * Prints out some debugging information about the entire menu layout. This
	 * information is quite large.
	 */
	public String toString() {
		final StringBuffer buffer = new StringBuffer();
		Iterator entryItr;

		// Print out the bars.
		buffer.append("   ___ top-level bars ___ \n"); //$NON-NLS-1$
		entryItr = getBarsByType().entrySet().iterator();
		while (entryItr.hasNext()) {
			final Map.Entry entry = (Map.Entry) entryItr.next();
			final String type = (String) entry.getKey();
			buffer.append(' ');
			buffer.append(' ');
			buffer.append(type);
			buffer.append('\n');
			LayoutNode node = (LayoutNode) entry.getValue();
			printNode(node, buffer, 4);
		}

		// Print out the context menus.
		buffer.append("   ___ context menus ___ \n"); //$NON-NLS-1$
		entryItr = getPopupsById().entrySet().iterator();
		while (entryItr.hasNext()) {
			final Map.Entry entry = (Map.Entry) entryItr.next();
			final String id = (String) entry.getKey();
			buffer.append(' ');
			buffer.append(' ');
			buffer.append(id);
			buffer.append('\n');
			LayoutNode node = (LayoutNode) entry.getValue();
			printNode(node, buffer, 4);
		}

		return buffer.toString();
	}
}


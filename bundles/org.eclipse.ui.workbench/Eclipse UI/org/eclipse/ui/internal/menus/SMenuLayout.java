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
 * The layout of various menu elements. A layout includes all menus, groups,
 * items and widgets -- regardless of whether they are currently visible.
 * </p>
 * <p>
 * There are three basic types of menu elements: part, popup and bar. Each of
 * these fundamental types has a map of subelements -- indexed by some string.
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
public final class SMenuLayout extends SPartMenuLayout {

	/**
	 * Generates a menu layout based on a collection of menu elements. This
	 * layout will take into account ordering constraints.
	 * 
	 * @param menuElements
	 *            The menu elements to be sorted into a menu layout; must not
	 *            <code>null</code>.
	 * @return The menu layout corresponding to the menu elements; never
	 *         <code>null</code>.
	 */
	static final SMenuLayout computeLayout(final Collection menuElements) {
		final SMenuLayout layout = new SMenuLayout();

		final Iterator elementItr = menuElements.iterator();
		while (elementItr.hasNext()) {
			final MenuElement element = (MenuElement) elementItr.next();
			try {
				final SLocation[] locations = element.getLocations();
				for (int i = 0; i < locations.length; i++) {
					final SLocation location = locations[i];
					final LocationElement locationElement = location.getPath();
					if (locationElement instanceof SBar) {
						final SBar bar = (SBar) locationElement;
						layout.addBar(element, location, bar);

					} else if (locationElement instanceof SPopup) {
						final SPopup popup = (SPopup) locationElement;
						layout.addPopup(element, location, popup);

					} else if (locationElement instanceof SPart) {
						final SPart part = (SPart) locationElement;
						layout.addPart(element, location, part);

					}
				}
			} catch (final NotDefinedException e) {
				// This menu element is not defined. Just skip it.
			}
		}

		return layout;
	}

	/**
	 * The menu layouts for the various parts, indexed by identifier and type ({@link String}).
	 * TODO Figure out the real structure for this item.
	 */
	private final Map partsById;

	/**
	 * Constructs a new instance of <code>SMenuLayout</code>.
	 */
	SMenuLayout() {
		this.partsById = new HashMap();
	}

	/**
	 * Adds an entry into a part-level menu layout. A part can contain a menu
	 * bar or a tool bar.
	 * 
	 * @param element
	 *            The element to insert into the node; must not be
	 *            <code>null</code>.
	 * @param location
	 *            The location at which the element is to be inserted; may be
	 *            <code>null</code>.
	 * @param part
	 *            The part indicator; must not be <code>null</code>.
	 * @throws NotDefinedException
	 *             If the given menu element is not defined.
	 */
	private final void addPart(final MenuElement element,
			final SLocation location, final SPart part)
			throws NotDefinedException {
		final String partIdOrType = part.getPart();
		SPartMenuLayout layout = (SPartMenuLayout) partsById.get(partIdOrType);
		if (layout == null) {
			layout = new SPartMenuLayout();
			partsById.put(partIdOrType, layout);
		}
		final LeafLocationElement locationElement = part.getLocation();
		if (locationElement instanceof SBar) {
			final SBar bar = (SBar) locationElement;
			layout.addBar(element, location, bar);

		} else if (locationElement instanceof SPopup) {
			final SPopup popup = (SPopup) locationElement;
			layout.addPopup(element, location, popup);

		}
	}

	/**
	 * Prints out some debugging information about the entire menu layout. This
	 * information is quite large.
	 */
	public final String toString() {
		final StringBuffer buffer = new StringBuffer();
		Iterator entryItr;

		// Print out the bars.
		buffer.append(" ***** TOP-LEVEL BARS ***** \n"); //$NON-NLS-1$
		entryItr = getBarsByType().entrySet().iterator();
		while (entryItr.hasNext()) {
			final Map.Entry entry = (Map.Entry) entryItr.next();
			final String type = (String) entry.getKey();
			buffer.append(type);
			buffer.append('\n');
			LayoutNode node = (LayoutNode) entry.getValue();
			printNode(node, buffer, 2);
		}

		// Print out the parts.
		buffer.append(" ***** PART-SPECIFIC BARS ***** \n"); //$NON-NLS-1$
		entryItr = partsById.entrySet().iterator();
		while (entryItr.hasNext()) {
			final Map.Entry entry = (Map.Entry) entryItr.next();
			final String partId = (String) entry.getKey();
			buffer.append(partId);
			buffer.append('\n');
			final SPartMenuLayout layout = (SPartMenuLayout) entry.getValue();
			buffer.append(layout.toString());
		}

		// Print out the context menus.
		buffer.append(" ***** CONTEXT MENUS ***** \n"); //$NON-NLS-1$
		entryItr = getPopupsById().entrySet().iterator();
		while (entryItr.hasNext()) {
			final Map.Entry entry = (Map.Entry) entryItr.next();
			final String id = (String) entry.getKey();
			buffer.append(id);
			buffer.append('\n');
			LayoutNode node = (LayoutNode) entry.getValue();
			printNode(node, buffer, 2);
		}

		return buffer.toString();
	}
}


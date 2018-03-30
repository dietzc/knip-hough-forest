/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2017
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * --------------------------------------------------------------------- *
 *
 */
package org.knime.knip.hough.forest.training;

import org.knime.knip.hough.forest.node.Node;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;

/**
 * Holds the class and offset of a patch to provide a fast access during learning.
 * 
 * @author Simon Schmid, University of Konstanz
 */
public abstract class PatchObject<T> {
	private final RandomAccessibleInterval<T> m_patch;
	private final RandomAccess<T>[] m_randomAccess;
	private final PatchObject<T>[][] m_grid;
	private final int[] m_position;
	private final Node[][][] m_nodeGrid;
	private final int m_numFeatures;

	/**
	 * Creates a new object conatining all relevant parameters.
	 * 
	 * @param patch a {@link RandomAccessibleInterval}
	 */
	@SuppressWarnings("unchecked")
	public PatchObject(final RandomAccessibleInterval<T> patch, final PatchObject<T>[][] grid, final int[] position,
			final Node[][][] nodeGrid) {
		m_patch = patch;
		m_randomAccess = new RandomAccess[nodeGrid.length];
		for (int i = 0; i < m_randomAccess.length; i++) {
			m_randomAccess[i] = patch.randomAccess();
		}
		m_grid = grid;
		m_position = position;
		m_nodeGrid = nodeGrid;
		m_numFeatures = (int) patch.dimension(2);
	}

	/**
	 * @return the patch of this training object
	 */
	public RandomAccessibleInterval<T> getPatch() {
		return m_patch;
	}

	public RandomAccess<T> getRandomAccess(final int i) {
		return m_randomAccess[i];
	}

	/**
	 * @return the grid
	 */
	@SuppressWarnings("rawtypes")
	public PatchObject[][] getGrid() {
		return m_grid;
	}

	/**
	 * @return the position
	 */
	public int[] getPosition() {
		return m_position;
	}

	/**
	 * @return the nodeGrid
	 */
	public Node[][][] getNodeGrid() {
		return m_nodeGrid;
	}

	public boolean isPosInGridInterval(final int[] pos) {
		if (pos[0] < 0 || pos[1] < 0) {
			return false;
		}
		if ((pos[0] >= m_nodeGrid[0].length) || (pos[1] >= m_nodeGrid[0][0].length)) {
			return false;
		}
		return true;
	}

	public void setNodeGrid(final int idx, final Node node) {
		m_nodeGrid[idx][m_position[0]][m_position[1]] = node;
	}

	/**
	 * @return the numFeatures
	 */
	public int getNumFeatures() {
		return m_numFeatures;
	}
}

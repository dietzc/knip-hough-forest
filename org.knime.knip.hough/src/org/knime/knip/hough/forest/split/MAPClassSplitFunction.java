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
package org.knime.knip.hough.forest.split;

import java.util.Random;

import org.knime.knip.hough.forest.node.Node;
import org.knime.knip.hough.forest.training.PatchObject;
import org.knime.knip.hough.nodes.learner.HoughForestLearnerConfig;

import net.imglib2.type.numeric.RealType;

/**
 * TODO doc Holds parameters of a split function of a Hough tree.
 * 
 * @author Simon Schmid, University of Konstanz
 */
public final class MAPClassSplitFunction extends EntangledSplitFunction {

	private static final long serialVersionUID = 1L;

	private final int[] m_offset;
	private final int m_clazz;

	/**
	 * Creates a new {@link MAPClassSplitFunction}.
	 * 
	 * @param indices the indices
	 * @param threshold the threshold
	 */
	public MAPClassSplitFunction(final int[] offset, final int clazz, final int[] stride) {
		super(stride);
		assert clazz == 0 || clazz == 1;
		m_offset = offset;
		m_clazz = clazz;
	}

	@Override
	public <T extends RealType<T>> Split apply(final PatchObject<T> pObj, final int treeIdx, final int[] stride) {
		final int[] position = pObj.getPosition();
		final int[] position_probe = getPos(position, m_offset, stride);
		final int clazz;
		if (pObj.isPosInGridInterval(position_probe)) {
			final Node node = pObj.getNodeGrid()[treeIdx][position_probe[0]][position_probe[1]];
			if (node.getProbability(0) > node.getProbability(1)) {
				clazz = 0;
			} else {
				clazz = 1;
			}
		} else {
			clazz = 0;
		}
		if (m_clazz == clazz) {
			return Split.LEFT;
		}
		return Split.RIGHT;
	}

	public static MAPClassSplitFunction createRandom(final HoughForestLearnerConfig config, final Random random) {
		return new MAPClassSplitFunction(config.createRandomOffset(random), random.nextInt(2),
				new int[] { config.getPatchGapX(), config.getPatchGapY() });
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + m_clazz;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		MAPClassSplitFunction other = (MAPClassSplitFunction) obj;
		if (m_clazz != other.m_clazz)
			return false;
		return true;
	}

	@Override
	public String getName() {
		return "MAP";
	}

	/**
	 * @return the offset
	 */
	public int[] getOffset() {
		return m_offset;
	}

	/**
	 * @return the clazz
	 */
	public int getClazz() {
		return m_clazz;
	}

}

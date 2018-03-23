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
public final class NodeDescendantSplitFunction extends EntangledSplitFunction {

	private static final long serialVersionUID = 1L;

	private final int[] m_offset;

	private final int m_nodeIdx;

	/**
	 * Creates a new {@link NodeDescendantSplitFunction}.
	 * 
	 * @param indices the indices
	 * @param threshold the threshold
	 */
	public NodeDescendantSplitFunction(final int[] offset, final int nodeIdx, final int[] stride) {
		super(stride);
		m_offset = offset;
		m_nodeIdx = nodeIdx;
	}

	@Override
	public <T extends RealType<T>> Split apply(final PatchObject<T> pObj, final int treeIdx, final int[] stride) {
		final int[] position = pObj.getPosition();
		final int[] position_probe = getPos(position, m_offset, stride);
		if (pObj.isPosInGridInterval(position_probe)) {
			Node node = pObj.getNodeGrid()[treeIdx][position_probe[0]][position_probe[1]];
			while (node.getParent() != null) {
				node = node.getParent();
				if (node.getNodeIdx() == m_nodeIdx) {
					return Split.LEFT;
				}
			}
		} else {
			// TODO how to handle?
		}
		return Split.RIGHT;
	}

	public static NodeDescendantSplitFunction createRandom(final HoughForestLearnerConfig config, final int depth,
			final Random random) {
		// all nodes can be chosen above the current depth level except the root node
		final int nodeIdx = random.nextInt((int) Math.pow(2, depth) - 2) + 1;
		return new NodeDescendantSplitFunction(config.createRandomOffset(random), nodeIdx, config.getStride());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + m_nodeIdx;
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
		NodeDescendantSplitFunction other = (NodeDescendantSplitFunction) obj;
		if (m_nodeIdx != other.m_nodeIdx)
			return false;
		return true;
	}

	@Override
	public String getName() {
		return "NodeDesc";
	}

	/**
	 * @return the offset
	 */
	public int[] getOffset() {
		return m_offset;
	}

	/**
	 * @return the nodeIdx
	 */
	public int getNodeIdx() {
		return m_nodeIdx;
	}

}

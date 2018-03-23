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
public final class AncestorNodePairSplitFunction extends EntangledSplitFunction {

	private static final long serialVersionUID = 1L;

	protected final int[] m_offset1;
	protected final int[] m_offset2;
	private final int m_threshold;

	/**
	 * Creates a new {@link AncestorNodePairSplitFunction}.
	 * 
	 * @param indices the indices
	 * @param threshold the threshold
	 */
	public AncestorNodePairSplitFunction(final int[] offset1, final int[] offset2, final int threshold,
			final int[] stride) {
		super(stride);
		m_offset1 = offset1;
		m_offset2 = offset2;
		m_threshold = threshold;
	}

	@Override
	public <T extends RealType<T>> Split apply(final PatchObject<T> pObj, final int treeIdx, final int[] stride) {
		final int[] position = pObj.getPosition();
		final int[] positionProbe1 = getPos(position, m_offset1, stride);
		final int[] positionProbe2 = getPos(position, m_offset2, stride);
		if (pObj.isPosInGridInterval(positionProbe1) && pObj.isPosInGridInterval(positionProbe2)) {
			Node nodeProbe1 = pObj.getNodeGrid()[treeIdx][positionProbe1[0]][positionProbe1[1]];
			Node nodeProbe2 = pObj.getNodeGrid()[treeIdx][positionProbe2[0]][positionProbe2[1]];
			int counter = 0;
			// take both nodes onto the same depth level (may be different, if one of the nodes is a leaf)
			int d = nodeProbe1.getDepth() - nodeProbe2.getDepth();
			if (d > 0) {
				// nodeProbe2 is leaf node
				counter = d;
				for (; d != 0; d--) {
					nodeProbe1 = nodeProbe1.getParent();
				}
			} else if (d < 0) {
				// nodeProbe1 is leaf node
				counter = d * (-1);
				for (; d != 0; d++) {
					nodeProbe2 = nodeProbe2.getParent();
				}
			}
			// go upwards until both nodes meet
			while (nodeProbe1.getParent() != null) {
				nodeProbe1 = nodeProbe1.getParent();
				nodeProbe2 = nodeProbe2.getParent();
				if (nodeProbe1 == nodeProbe2) {
					return Split.LEFT;
				}
				counter++;
				if (counter > m_threshold) {
					return Split.RIGHT;
				}
			}
		} else {
			// TODO how to handle?
		}
		return Split.RIGHT;
	}

	public static AncestorNodePairSplitFunction createRandom(final HoughForestLearnerConfig config, final int depth,
			final Random random) {
		return new AncestorNodePairSplitFunction(config.createRandomOffset(random), config.createRandomOffset(random),
				random.nextInt(Math.min(depth - 1, config.getAncestorNodePairThreshold())) + 1, config.getStride());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + m_threshold;
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
		AncestorNodePairSplitFunction other = (AncestorNodePairSplitFunction) obj;
		if (m_threshold != other.m_threshold)
			return false;
		return true;
	}

	@Override
	public String getName() {
		return "Ancestor";
	}

	/**
	 * @return the threshold
	 */
	public int getThreshold() {
		return m_threshold;
	}

}

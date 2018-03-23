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

import java.util.Arrays;
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
public final class OffsetSimilarityNodePairSplitFunction extends EntangledSplitFunction {

	private static final long serialVersionUID = 1L;

	protected final int[] m_offset1;
	protected final int[] m_offset2;
	private final double m_threshold;
	private final double m_sigma;

	/**
	 * Creates a new {@link OffsetSimilarityNodePairSplitFunction}.
	 * 
	 * @param indices the indices
	 * @param threshold the threshold
	 */
	public OffsetSimilarityNodePairSplitFunction(final int[] offset1, final int[] offset2, final double threshold,
			final int[] stride, final double sigma) {
		super(stride);
		m_offset1 = offset1;
		m_offset2 = offset2;
		m_threshold = threshold;
		m_sigma = sigma;
	}

	@Override
	public <T extends RealType<T>> Split apply(final PatchObject<T> pObj, final int treeIdx, final int[] stride) {
		final int[] position = pObj.getPosition();
		final int[] position_probe1 = getPos(position, m_offset1, stride);
		final int[] position_probe2 = getPos(position, m_offset2, stride);
		if (pObj.isPosInGridInterval(position_probe1) && pObj.isPosInGridInterval(position_probe2)) {
			Node node1 = pObj.getNodeGrid()[treeIdx][position_probe1[0]][position_probe1[1]];
			Node node2 = pObj.getNodeGrid()[treeIdx][position_probe2[0]][position_probe2[1]];
			if (node1.getProbability(0) > 0.5 || node2.getProbability(0) > 0.5) {
				return Split.LEFT;
			}
			final double[] d1 = node1.getOffsetMean();
			final double[] d2 = node2.getOffsetMean();
			double v = 0;
			for (int i = 0; i < d1.length; i++) {
				final double diff = d1[i] - d2[i];
				v += diff * diff;
			}
			v /= m_sigma * m_sigma;
			double exp = Math.exp(0 - v);
			if (exp < m_threshold) {
				return Split.LEFT;
			}
		} else {
			// TODO how to handle?
			return Split.LEFT;
		}
		return Split.RIGHT;
	}

	public static OffsetSimilarityNodePairSplitFunction createRandom(final HoughForestLearnerConfig config,
			final Random random) {
		return new OffsetSimilarityNodePairSplitFunction(config.createRandomOffset(random),
				config.createRandomOffset(random), random.nextDouble(), config.getStride(),
				config.getOffsetSimilarityNodePairSigma());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(m_offset1);
		result = prime * result + Arrays.hashCode(m_offset2);
		long temp;
		temp = Double.doubleToLongBits(m_threshold);
		result = prime * result + (int) (temp ^ (temp >>> 32));
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
		OffsetSimilarityNodePairSplitFunction other = (OffsetSimilarityNodePairSplitFunction) obj;
		if (!Arrays.equals(m_offset1, other.m_offset1))
			return false;
		if (!Arrays.equals(m_offset2, other.m_offset2))
			return false;
		if (Double.doubleToLongBits(m_threshold) != Double.doubleToLongBits(other.m_threshold))
			return false;
		return true;
	}

	@Override
	public String getName() {
		return "Offset";
	}

}

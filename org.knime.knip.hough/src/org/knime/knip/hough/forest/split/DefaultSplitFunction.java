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

import org.knime.knip.hough.forest.training.PatchObject;
import org.knime.knip.hough.forest.training.TrainingObject;
import org.knime.knip.hough.nodes.learner.HoughForestLearnerConfig;

import net.imglib2.RandomAccess;
import net.imglib2.type.numeric.RealType;

/**
 * Holds parameters of a split function of a Hough tree.
 * 
 * @author Simon Schmid, University of Konstanz
 */
public final class DefaultSplitFunction implements SplitFunction {

	private static final long serialVersionUID = 1L;

	private final int[][] m_indices;
	private final double m_threshold;

	/**
	 * Creates a new {@link DefaultSplitFunction}.
	 * 
	 * @param indices the indices
	 * @param threshold the threshold
	 */
	public DefaultSplitFunction(final int[][] indices, final double threshold) {
		m_indices = indices;
		m_threshold = threshold;
	}

	public static <T extends RealType<T>> DefaultSplitFunction createRandom(final TrainingObject<?> sample,
			final HoughForestLearnerConfig config, Random random) {
		final int channel = random.nextInt(sample.getNumFeatures());
		return new DefaultSplitFunction(
				new int[][] {
						{ random.nextInt(config.getPatchWidth()), random.nextInt(config.getPatchHeight()), channel },
						{ random.nextInt(config.getPatchWidth()), random.nextInt(config.getPatchHeight()), channel } },
				random.nextDouble() * config.getThresholds()[channel]);
	}

	@Override
	public <T extends RealType<T>> Split apply(final PatchObject<T> pObj, final int treeIdx, final int[] stride) {
		final RandomAccess<T> raPatch = pObj.getRandomAccess(treeIdx);
		final int[] min = pObj.getMin();
		raPatch.setPosition(new int[] { min[0] + m_indices[0][0], min[1] + m_indices[0][1], m_indices[0][2] });
		final float value1 = raPatch.get().getRealFloat();
		raPatch.setPosition(new int[] { min[0] + m_indices[1][0], min[1] + m_indices[1][1], m_indices[1][2] });
		final float value2 = raPatch.get().getRealFloat();
		if (value1 - value2 < m_threshold)
			return Split.LEFT;
		return Split.RIGHT;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.deepHashCode(m_indices);
		long temp;
		temp = Double.doubleToLongBits(m_threshold);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DefaultSplitFunction)) {
			return false;
		}
		DefaultSplitFunction other = (DefaultSplitFunction) obj;
		if (!Arrays.deepEquals(m_indices, other.m_indices)) {
			return false;
		}
		if (Double.doubleToLongBits(m_threshold) != Double.doubleToLongBits(other.m_threshold)) {
			return false;
		}
		return true;
	}

	@Override
	public String getName() {
		return "Default";
	}

}

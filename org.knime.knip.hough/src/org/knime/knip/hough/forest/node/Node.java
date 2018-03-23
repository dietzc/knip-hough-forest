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
package org.knime.knip.hough.forest.node;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * TODO Interface for a node of a hough tree.
 * 
 * @author Simon Schmid, University of Konstanz
 */
public abstract class Node implements Serializable {

	private static final long serialVersionUID = 1L;

	private int m_depth;
	private int m_nodeIdx;
	private double[] m_probabilities;
	private List<int[]> m_offsets;
	private SplitNode m_parent;

	private double[] m_offsetMean;

	public Node(final int depth, final int nodeIdx, final double[] classProbabilities, final List<int[]> offsets,
			final SplitNode parent) {
		m_depth = depth;
		m_nodeIdx = nodeIdx;
		m_probabilities = classProbabilities;
		m_offsets = offsets;
		m_parent = parent;
		m_offsetMean = offsetMean(offsets);
	}

	/**
	 * Empty no-arg constructor used for deserialization.
	 */
	protected Node() {
	}

	public List<int[]> getOffsetVectors() {
		return m_offsets;
	}

	private double[] offsetMean(final List<int[]> offsets) {
		final double[] offsetMean = new double[] { 0, 0 };
		if (offsets.size() == 0) {
			return offsetMean;
		}
		for (final int[] o : offsets) {
			offsetMean[0] += o[0];
			offsetMean[1] += o[1];
		}
		offsetMean[0] /= offsets.size();
		offsetMean[1] /= offsets.size();

		return offsetMean;
	}

	public double[] getOffsetMean() {
		return m_offsetMean;

	}

	public double getProbability(final int idx) {
		return getProbabilities()[idx];
	}

	/**
	 * @return the probabilities
	 */
	public double[] getProbabilities() {
		return m_probabilities;
	}

	/**
	 * @return the nodeIdx
	 */
	public int getNodeIdx() {
		return m_nodeIdx;
	}

	/**
	 * @return the depth
	 */
	public int getDepth() {
		return m_depth;
	}

	/**
	 * @return the parent
	 */
	public SplitNode getParent() {
		return m_parent;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + m_depth;
		result = prime * result + m_nodeIdx;
		result = prime * result + Arrays.hashCode(m_offsetMean);
		result = prime * result + ((m_offsets == null) ? 0 : m_offsets.hashCode());
		result = prime * result + ((m_parent == null) ? 0 : m_parent.hashCode());
		result = prime * result + Arrays.hashCode(m_probabilities);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Node other = (Node) obj;
		if (m_depth != other.m_depth)
			return false;
		if (m_nodeIdx != other.m_nodeIdx)
			return false;
		if (!Arrays.equals(m_offsetMean, other.m_offsetMean))
			return false;
		if (m_offsets == null) {
			if (other.m_offsets != null)
				return false;
		} else if (!Arrays.deepEquals(m_offsets.toArray(), other.m_offsets.toArray()))
			return false;
		// TODO parent cannot be compared, because it leads to a recurrent loop
		// if (m_parent == null) {
		// if (other.m_parent != null)
		// return false;
		// } else if (!m_parent.equals(other.m_parent))
		// return false;
		if (!Arrays.equals(m_probabilities, other.m_probabilities))
			return false;
		return true;
	}

}

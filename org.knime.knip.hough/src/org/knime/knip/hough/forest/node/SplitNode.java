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

import java.util.List;

import org.knime.knip.hough.forest.split.SplitFunction;

/**
 * Represents a split node of a Hough tree.
 * 
 * @author Simon Schmid, University of Konstanz
 */
public final class SplitNode extends Node {

	private static final long serialVersionUID = 1L;

	private SplitFunction m_splitFunction;
	private Node m_leftChild;
	private Node m_rightChild;

	/**
	 * Creates an object of this class with all relevant parameters.
	 * 
	 * @param splitFunction its {@link SplitFunction}
	 * @param leftChild its left child {@link Node}
	 * @param rightChild its right child {@link Node}
	 */
	public SplitNode(final SplitFunction splitFunction, final int depth, final int nodeIdx,
			final double[] classProbabilities, final List<int[]> offsets, final Node leftChild, final Node rightChild,
			final SplitNode parent) {
		super(depth, nodeIdx, classProbabilities, offsets, parent);
		m_splitFunction = splitFunction;
		setLeftChild(leftChild);
		setRightChild(rightChild);
	}

	/**
	 * Empty no-arg constructor used for deserialization.
	 */
	protected SplitNode() {
	}

	/**
	 * Creates an object of this class with children to be added.
	 * 
	 * @param splitFunction its {@link SplitFunction}
	 * @param leftChild its left child {@link Node}
	 * @param rightChild its right child {@link Node}
	 */
	public SplitNode(final SplitFunction splitFunction, final int depth, final int nodeIdx, final List<int[]> offsets,
			final double[] classProbabilities, final SplitNode parent) {
		super(depth, nodeIdx, classProbabilities, offsets, parent);
		m_splitFunction = splitFunction;
	}

	/**
	 * @return the left child {@link Node}
	 */
	public Node getLeftChild() {
		return m_leftChild;
	}

	/**
	 * @return the right child {@link Node}
	 */
	public Node getRightChild() {
		return m_rightChild;
	}

	/**
	 * @return the {@link SplitFunction}
	 */
	public SplitFunction getSplitFunction() {
		return m_splitFunction;
	}

	/**
	 * @param leftChild the leftChild to set
	 */
	public void setLeftChild(Node leftChild) {
		m_leftChild = leftChild;
	}

	/**
	 * @param rightChild the rightChild to set
	 */
	public void setRightChild(Node rightChild) {
		m_rightChild = rightChild;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((m_leftChild == null) ? 0 : m_leftChild.hashCode());
		result = prime * result + ((m_rightChild == null) ? 0 : m_rightChild.hashCode());
		result = prime * result + ((m_splitFunction == null) ? 0 : m_splitFunction.hashCode());
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
		SplitNode other = (SplitNode) obj;
		if (m_leftChild == null) {
			if (other.m_leftChild != null)
				return false;
		} else if (!m_leftChild.equals(other.m_leftChild))
			return false;
		if (m_rightChild == null) {
			if (other.m_rightChild != null)
				return false;
		} else if (!m_rightChild.equals(other.m_rightChild))
			return false;
		if (m_splitFunction == null) {
			if (other.m_splitFunction != null)
				return false;
		} else if (!m_splitFunction.equals(other.m_splitFunction))
			return false;
		return true;
	}

}

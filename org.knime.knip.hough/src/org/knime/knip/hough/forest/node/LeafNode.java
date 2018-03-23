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

import org.knime.knip.hough.forest.training.SampleTrainingObject;

/**
 * Represents a leaf node of a Hough tree.
 * 
 * @author Simon Schmid, University of Konstanz
 */
public class LeafNode extends Node {

	private static final long serialVersionUID = 1L;

	private final int m_numElementsOfClazz0;
	private final int m_numElementsOfClazz1;

	/**
	 * Creates an object of this class with all relevant parameters.
	 * 
	 * @param sample the {@link SampleTrainingObject}
	 * @param classProbabilities the class probabilities
	 * @param depth depth of the node
	 */
	public LeafNode(final SampleTrainingObject<?> sample, final double[] classProbabilities, final int depth,
			final int nodeIdx, final SplitNode parent) {
		super(depth, nodeIdx, classProbabilities, sample.getOffsets(), parent);
		m_numElementsOfClazz0 = sample.getNumberElementsOfClazz0();
		m_numElementsOfClazz1 = sample.getNumberElementsOfClazz1();
	}

	/**
	 * Creates a dummy object of this class.
	 */
	public LeafNode() {
		super(0, 0, null, null, null);
		m_numElementsOfClazz0 = 0;
		m_numElementsOfClazz1 = 0;
	}

	public int getNumElementsOfClazz0() {
		return m_numElementsOfClazz0;
	}

	public int getNumElementsOfClazz1() {
		return m_numElementsOfClazz1;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + m_numElementsOfClazz0;
		result = prime * result + m_numElementsOfClazz1;
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
		LeafNode other = (LeafNode) obj;
		if (m_numElementsOfClazz0 != other.m_numElementsOfClazz0)
			return false;
		if (m_numElementsOfClazz1 != other.m_numElementsOfClazz1)
			return false;
		return true;
	}

}

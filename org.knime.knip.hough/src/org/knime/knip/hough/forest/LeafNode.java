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
package org.knime.knip.hough.forest;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.List;

import net.imglib2.type.numeric.RealType;

/**
 * Represents a leaf node of a Hough tree.
 * 
 * @author Simon Schmid, University of Konstanz
 */
public final class LeafNode<T extends RealType<T>> implements Node {

	private static final long serialVersionUID = -6317737563377710160L;

	private double[] m_probability;
	private int m_depth;
	private List<int[]> m_offsets;
	private int m_numElementsOfClazz0;
	private int m_numElementsOfClazz1;

	/**
	 * Creates an object of this class with all relevant parameters.
	 * 
	 * @param sample the {@link PatchSample}
	 * @param classProbabilities the class probabilities
	 * @param depth depth of the node
	 */
	public LeafNode(final PatchSample<T> sample, final double[] classProbabilities, final int depth) {
		this.m_offsets = sample.getOffsetVectors();
		this.m_numElementsOfClazz0 = sample.getNumberElementsOfClazz0();
		this.m_numElementsOfClazz1 = sample.getNumberElementsOfClazz1();
		this.m_depth = depth;
		// compute probabilities
		this.m_probability = classProbabilities;
	}

	/**
	 * Creates an empty object of this class which needs to be filled by invoking {@link #readExternal(ObjectInput)}.
	 */
	public LeafNode() {
	}

	public List<int[]> getOffsetVectors() {
		return m_offsets;
	}

	public double getProbability(final int idx) {
		return m_probability[idx];
	}

	public int getNumElementsOfClazz0() {
		return m_numElementsOfClazz0;
	}

	public int getNumElementsOfClazz1() {
		return m_numElementsOfClazz1;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		m_offsets = (List<int[]>) in.readObject();
		m_probability = (double[]) in.readObject();
		m_depth = in.readInt();
		m_numElementsOfClazz0 = in.readInt();
		m_numElementsOfClazz1 = in.readInt();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(m_offsets);
		out.writeObject(m_probability);
		out.writeInt(m_depth);
		out.writeInt(m_numElementsOfClazz0);
		out.writeInt(m_numElementsOfClazz1);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + m_depth;
		result = prime * result + m_numElementsOfClazz0;
		result = prime * result + m_numElementsOfClazz1;
		result = prime * result + ((m_offsets == null) ? 0 : m_offsets.hashCode());
		result = prime * result + Arrays.hashCode(m_probability);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof LeafNode)) {
			return false;
		}
		LeafNode<?> other = (LeafNode<?>) obj;
		if (m_depth != other.m_depth) {
			return false;
		}
		if (m_numElementsOfClazz0 != other.m_numElementsOfClazz0) {
			return false;
		}
		if (m_numElementsOfClazz1 != other.m_numElementsOfClazz1) {
			return false;
		}
		if (m_offsets == null) {
			if (other.m_offsets != null) {
				return false;
			}
		} else if (!Arrays.deepEquals(m_offsets.toArray(), other.m_offsets.toArray())) {
			return false;
		}
		if (!Arrays.equals(m_probability, other.m_probability)) {
			return false;
		}
		return true;
	}

}

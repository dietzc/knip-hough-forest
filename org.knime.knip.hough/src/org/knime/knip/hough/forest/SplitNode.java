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

/**
 * Represents a split node of a Hough tree.
 * 
 * @author Simon Schmid, University of Konstanz
 */
public final class SplitNode implements Node {

	private static final long serialVersionUID = -5493651343996719345L;

	private SplitFunction splitFunction;
	private Node leftChild;
	private Node rightChild;

	/**
	 * Creates an object of this class with all relevant parameters.
	 * 
	 * @param splitFunction its {@link SplitFunction}
	 * @param leftChild its left child {@link Node}
	 * @param rightChild its right child {@link Node}
	 */
	public SplitNode(final SplitFunction splitFunction, final Node leftChild, final Node rightChild) {
		this.splitFunction = splitFunction;
		this.leftChild = leftChild;
		this.rightChild = rightChild;
	}

	/**
	 * Creates an empty object of this class which needs to be filled by invoking {@link #readExternal(ObjectInput)}.
	 */
	public SplitNode() {
	}

	/**
	 * @return the left child {@link Node}
	 */
	public Node getLeftChild() {
		return leftChild;
	}

	/**
	 * @return the right child {@link Node}
	 */
	public Node getRightChild() {
		return rightChild;
	}

	/**
	 * @return the {@link SplitFunction}
	 */
	public SplitFunction getSplitFunction() {
		return splitFunction;
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		leftChild = (Node) in.readObject();
		rightChild = (Node) in.readObject();
		int[][] indices = (int[][]) in.readObject();
		double threshold = in.readDouble();
		splitFunction = new SplitFunction(indices, threshold);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(leftChild);
		out.writeObject(rightChild);
		out.writeObject(splitFunction.getIndices());
		out.writeDouble(splitFunction.getThreshold());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((leftChild == null) ? 0 : leftChild.hashCode());
		result = prime * result + ((rightChild == null) ? 0 : rightChild.hashCode());
		result = prime * result + ((splitFunction == null) ? 0 : splitFunction.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof SplitNode)) {
			return false;
		}
		SplitNode other = (SplitNode) obj;
		if (leftChild == null) {
			if (other.leftChild != null) {
				return false;
			}
		} else if (!leftChild.equals(other.leftChild)) {
			return false;
		}
		if (rightChild == null) {
			if (other.rightChild != null) {
				return false;
			}
		} else if (!rightChild.equals(other.rightChild)) {
			return false;
		}
		if (splitFunction == null) {
			if (other.splitFunction != null) {
				return false;
			}
		} else if (!splitFunction.equals(other.splitFunction)) {
			return false;
		}
		return true;
	}

}

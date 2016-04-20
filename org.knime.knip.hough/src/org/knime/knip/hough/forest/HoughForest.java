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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.knime.knip.hough.features.FeatureDescriptor;

/**
 * This objects holds all relevant parameters of a hough forest.
 * 
 * @author Simon Schmid, University of Konstanz
 */
public final class HoughForest implements Externalizable {

	private List<SplitNode> m_listTrees;
	private long[] m_patchSize;
	private FeatureDescriptor<?> m_featureDescriptor;

	/**
	 * Creates an empty object of this class which needs to be filled by invoking {@link #readExternal(ObjectInput)}.
	 */
	public HoughForest() {
		this.m_listTrees = new ArrayList<>();
		this.m_patchSize = new long[2];
	}

	/**
	 * Creates an object of this class with all relevant parameters.
	 * 
	 * @param listTrees list of trees
	 * @param patchSize size of the patches
	 * @param featureDescriptor the used {@link FeatureDescriptor}
	 */
	public HoughForest(final List<SplitNode> listTrees, final long[] patchSize,
			final FeatureDescriptor<?> featureDescriptor) {
		this.m_listTrees = listTrees;
		this.m_patchSize = patchSize;
		this.m_featureDescriptor = featureDescriptor;
	}

	/**
	 * @return {@link List} of all trees
	 */
	public List<SplitNode> getListOfTrees() {
		return m_listTrees;
	}

	/**
	 * @return the size of the patches which has been used for learning
	 */
	public long[] getPatchSize() {
		return m_patchSize;
	}

	/**
	 * @return the {@link FeatureDescriptor} used for learning
	 */
	public FeatureDescriptor<?> getFeatureDescriptor() {
		return m_featureDescriptor;
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		m_patchSize = (long[]) in.readObject();
		int numTrees = in.readInt();
		for (int i = 0; i < numTrees; i++) {
			SplitNode tree = new SplitNode();
			tree.readExternal(in);
			m_listTrees.add(tree);
		}
		m_featureDescriptor = new FeatureDescriptor<>();
		m_featureDescriptor.readExternal(in);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(m_patchSize);
		out.writeInt(m_listTrees.size());
		for (final SplitNode root : m_listTrees) {
			root.writeExternal(out);
		}
		m_featureDescriptor.writeExternal(out);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((m_featureDescriptor == null) ? 0 : m_featureDescriptor.hashCode());
		result = prime * result + ((m_listTrees == null) ? 0 : m_listTrees.hashCode());
		result = prime * result + Arrays.hashCode(m_patchSize);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof HoughForest)) {
			return false;
		}
		HoughForest other = (HoughForest) obj;
		if (m_featureDescriptor == null) {
			if (other.m_featureDescriptor != null) {
				return false;
			}
		} else if (!m_featureDescriptor.equals(other.m_featureDescriptor)) {
			return false;
		}
		if (m_listTrees == null) {
			if (other.m_listTrees != null) {
				return false;
			}
		} else if (!Arrays.deepEquals(m_listTrees.toArray(), other.m_listTrees.toArray())) {
			return false;
		}
		if (!Arrays.equals(m_patchSize, other.m_patchSize)) {
			return false;
		}
		return true;
	}

}

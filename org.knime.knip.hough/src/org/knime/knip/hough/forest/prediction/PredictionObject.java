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
package org.knime.knip.hough.forest.prediction;

import java.util.ArrayList;
import java.util.List;

import org.knime.knip.hough.forest.node.LeafNode;
import org.knime.knip.hough.forest.node.Node;
import org.knime.knip.hough.forest.training.PatchObject;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

/**
 * The predictions of a patch are written into this object to provide a fast access during back projection.
 * 
 * @author Simon Schmid, University of Konstanz
 */
public final class PredictionObject<T extends RealType<T>> extends PatchObject<T> {

	// collectes predicted leaf nodes of different trees
	private final List<LeafNode> m_predictions;

	private final int[] m_patchMid;

	/**
	 * Creates a new {@link PredictionObject} with the given patch. Its predictions will be empty on initialization and
	 * need to be added during the prediction.
	 * 
	 * @param patch a {@link RandomAccessibleInterval}
	 */
	public PredictionObject(final RandomAccessibleInterval<T> patch, final RandomAccess<T>[] randomAccess,
			final int[] patchMid, final PatchObject<T>[][] grid, final int[] position, final Node[][][] nodeGrid) {
		super(patch, randomAccess, grid, position, nodeGrid);
		m_patchMid = patchMid;
		m_predictions = new ArrayList<LeafNode>();
	}

	/**
	 * adds the prediction to the {@link PredictionObject}
	 */
	public void addPrediction(final LeafNode prediction) {
		m_predictions.add(prediction);
	}

	/**
	 * @return all predictions of the {@link PredictionObject}
	 */
	public List<LeafNode> getPredictions() {
		return m_predictions;
	}

	/**
	 * @return the patchMid
	 */
	public int[] getPatchMid() {
		return m_patchMid;
	}
}

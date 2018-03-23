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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.knip.hough.forest.HoughForest;
import org.knime.knip.hough.forest.node.LeafNode;
import org.knime.knip.hough.forest.node.Node;
import org.knime.knip.hough.forest.node.SplitNode;
import org.knime.knip.hough.forest.split.SplitFunction.Split;
import org.knime.knip.hough.nodes.predictor.HoughForestPredictorConfig;

import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.Localizable;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

/**
 * Several methods used for prediction, voting and back projection.
 * 
 * @author Simon Schmid, University of Konstanz
 */
public final class PredictorEntangled {

	private static <T extends RealType<T>> void predictTree(final Map<PredictionObject<T>, Node> mapPredObjNode,
			final int treeIdx, final HoughForestPredictorConfig config) {
		boolean again = false;
		final int[] stride = new int[] { config.getPatchGapX(), config.getPatchGapY() };
		for (final PredictionObject<T> predObj : mapPredObjNode.keySet()) {
			final Node node = mapPredObjNode.get(predObj);
			if (node instanceof LeafNode) {
				predObj.addPrediction((LeafNode) node);
				continue;
			}
			again = true;
			if (!(node instanceof SplitNode)) {
				throw new IllegalArgumentException("Unexpected node type: " + node.getClass());
			}
			final Split splitDecision = ((SplitNode) node).getSplitFunction().apply(predObj, treeIdx, stride);
			// and call recursively
			if (splitDecision == Split.LEFT) {
				final Node leftChild = ((SplitNode) node).getLeftChild();
				mapPredObjNode.put(predObj, leftChild);
				predObj.setNodeGrid(treeIdx, leftChild);
			} else {
				final Node rightChild = ((SplitNode) node).getRightChild();
				mapPredObjNode.put(predObj, rightChild);
				predObj.setNodeGrid(treeIdx, rightChild);
			}
		}
		if (again) {
			predictTree(mapPredObjNode, treeIdx, config);
		}
	}

	/**
	 * Allows to predict the class of the patch of an {@link PredictionObject} given an trained Hough forest, stores the
	 * prediction into the object and writes a vote out.
	 * 
	 * @param forest forest to predict on
	 * @param predObject object to predict
	 * @param raVotes {@link RandomAccess} of the {@link RandomAccessibleInterval} in which the votes are written
	 * @param scaledInterval scaled interval according to the scale of the input image
	 * @param scale scale of the input image compared to the original scale of learning
	 */
	public static <T extends RealType<T>> void predictForest(final HoughForest forest,
			final List<PredictionObject<T>> predObjects, final RandomAccess<FloatType> raVotes,
			final FinalInterval scaledInterval, final double scale, final HoughForestPredictorConfig config) {
		for (int i = 0; i < forest.getListOfTrees().size(); i++) {
			final SplitNode root = forest.getListOfTrees().get(i);
			setNodeGrid(i, predObjects, root);
			// map needed to store current node during prediction (breadth first)
			final Map<PredictionObject<T>, Node> mapPredObjNode = new HashMap<PredictionObject<T>, Node>();
			initMap(mapPredObjNode, predObjects, root);
			predictTree(mapPredObjNode, i, config);
			for (final PredictionObject<T> predObj : predObjects) {
				final Node node = mapPredObjNode.get(predObj);
				if (!(node instanceof LeafNode)) {
					throw new IllegalStateException("Unexpected type of node predicted: " + node.getClass());
				}
				final LeafNode predictedLeafNode = (LeafNode) node;
				// predObj.addPrediction(predictedLeafNode); TODO check (l. 84)
				for (final int[] offset : ((LeafNode) node).getOffsetVectors()) {
					if (offset.length > 0) {
						final int patchX = predObj.getPatchMid()[0];
						final int patchY = predObj.getPatchMid()[1];
						final int[] pos = new int[] { patchX + offset[0], patchY + offset[1] };
						if (contains2D(scaledInterval, pos)) {
							double angle = getAngle(offset[0], offset[1]);
							double magnitude = getMagnitude(offset[0], offset[1]);

							raVotes.setPosition((int) (pos[0] / scale), 0);
							raVotes.setPosition((int) (pos[1] / scale), 1);
							raVotes.get().setReal(
									raVotes.get().getRealDouble() + (1.0 / scale) * (predictedLeafNode.getProbability(1)
											/ predictedLeafNode.getNumElementsOfClazz1())); // TODO
							// check
							// if
							// scale
							// weighting
							// is
							// correct
						}
					}
				}
			}
		}
	}

	// returns the signed angle of a vector
	private static double getAngle(final double x, final double y) {
		float angle = (float) Math.toDegrees(Math.atan2(x, y));
		if (angle < 0) {
			angle += 360;
		}
		return angle;
	}

	// returns the magnitude of a vector
	private static double getMagnitude(final double x, final double y) {
		return Math.sqrt(x * x + y * y);
	}

	private static <T extends RealType<T>> void initMap(final Map<PredictionObject<T>, Node> sample,
			final List<PredictionObject<T>> predObjects, final Node node) {
		for (final PredictionObject<T> key : predObjects) {
			sample.put(key, node);
		}
	}

	private static boolean contains2D(final Interval containing, final int[] pos) {
		if (pos[0] < containing.min(0) || pos[0] > containing.max(0))
			return false;
		if (pos[1] < containing.min(1) || pos[1] > containing.max(1))
			return false;
		return true;
	}

	/**
	 * Get the vertices of the back projection.
	 * 
	 * @param predObject {@link PredictionObject} containing the predictions
	 * @param scaledMaxInterval scaled interval containing the point of which had the most votes
	 * @param scale scale
	 * @return list of vertices
	 */
	public static <T extends RealType<T>> List<Localizable> getVertices(final PredictionObject<T> predObject,
			final FinalInterval scaledMaxInterval, final double scale) {
		final List<Localizable> vertices = new ArrayList<>();
		int counter = 0;
		final int patchX = predObject.getPatchMid()[0];
		final int patchY = predObject.getPatchMid()[1];
		for (final LeafNode prediction : predObject.getPredictions()) {
			if (prediction.getProbability(1) > 0.5) { // TODO was originally set to 0.5, good idea?
				for (final int[] offset : prediction.getOffsetVectors()) {
					if (offset.length > 0) {
						if (contains2D(scaledMaxInterval, new int[] { patchX + offset[0], patchY + offset[1] })) {
							counter++;
						}
					}
				}
			}
		}
		// only add the patch mid point to vertices, if it satisfies the criteria more
		// than 5 times
		if (counter > 5) {
			vertices.add(new Point((int) (patchX / scale), (int) (patchY / scale)));
		}
		return vertices;
	}

	private static <T extends RealType<T>> void setNodeGrid(final int treeIdx,
			final List<PredictionObject<T>> predObjects, final Node node) {
		for (final PredictionObject<?> tObj : predObjects) {
			tObj.setNodeGrid(treeIdx, node);
		}
	}
}

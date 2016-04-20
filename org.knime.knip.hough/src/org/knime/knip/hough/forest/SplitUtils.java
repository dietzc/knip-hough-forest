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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

/**
 * Utils class for creating, evaluating and predicting {@link SplitFunction}s.
 * 
 * @author Simon Schmid, University of Konstanz
 */
public final class SplitUtils {

	final static String LEFT = "left";
	final static String RIGHT = "right";

	/**
	 * Creates several random {@link SplitFunction}s.
	 * 
	 * @param patch the patch
	 * @param numFunctions the number of functions to create
	 * @param threshold the maximum threshold
	 * @return random array of {@link SplitFunction}s
	 */
	public static <T> SplitFunction[] createSplitFunctions(final RandomAccessibleInterval<T> patch,
			final int numFunctions, final double threshold, final long seed) {
		final SplitFunction[] splitFunctions = new SplitFunction[numFunctions];
		final Random random = new Random(seed);
		// TODO: better finding of thresholds
		for (int i = 0; i < numFunctions; i++) {
			final int channel = random.nextInt((int) patch.max(2));
			splitFunctions[i] = new SplitFunction(
					new int[][] { { random.nextInt((int) patch.max(0)), random.nextInt((int) patch.max(1)), channel },
							{ random.nextInt((int) patch.max(0)), random.nextInt((int) patch.max(1)), channel } },
					random.nextDouble() * threshold);
		}
		return splitFunctions;
	}

	/**
	 * Splits a sample of {@link TrainingObject}s by definition of a {@link SplitFunction}.
	 * 
	 * @param sample the sample to split
	 * @param splitFunction the {@link SplitFunction}
	 * @return a 2d array of {@link PatchSample}, containing the left split in the first and the right split in the
	 *         second dimension
	 */
	@SuppressWarnings("unchecked")
	public static <T extends RealType<T>> PatchSample<T>[] learnSplit(final List<TrainingObject<T>> sample,
			final SplitFunction splitFunction) {

		final List<TrainingObject<T>> left = new ArrayList<>();
		final List<TrainingObject<T>> right = new ArrayList<>();
		// decide left <-> right
		for (int i = 0; i < sample.size(); i++) {
			final String predictSplit = predictSplit(sample.get(i).getPatch().randomAccess(), splitFunction);
			if (predictSplit.equals(LEFT)) {
				left.add(sample.get(i));
			} else {
				right.add(sample.get(i));
			}
		}
		return new PatchSample[] { new PatchSample<>(left), new PatchSample<>(right) };
	}

	/**
	 * Evaluates a split either by entropy or similarity.
	 * 
	 * @param split the splitted {@link PatchSample}s
	 * @param trainingSet the training set
	 * @param byEntropy if true, evaluation is done by entropy, otherwise by similarity
	 * @return the evaluation value
	 */
	public static <T extends RealType<T>> double evaluateSplit(final PatchSample<T>[] split,
			final PatchSample<T> trainingSet, final boolean byEntropy) {
		if (byEntropy) {
			// compute entropy-based
			return (split[0].size() / (split[0].size() + split[1].size())) * split[0].getEntropy(trainingSet)
					+ (split[1].size() / (split[0].size() + split[1].size())) * split[1].getEntropy(trainingSet);
		}
		// compute similarity-based
		final List<int[]> vectors0 = split[0].getOffsetVectors();
		int[] sum0 = new int[2];
		for (int[] vector : vectors0) {
			sum0[0] += vector[0];
			sum0[1] += vector[1];
		}

		int u0 = 0;
		for (int i = 0; i < vectors0.size(); i++) {
			final int v0 = vectors0.get(i)[0] - sum0[0] / split[0].size();
			final int v1 = vectors0.get(i)[1] - sum0[1] / split[0].size();
			u0 += v0 * v0 + v1 * v1;
		}

		final List<int[]> vectors1 = split[1].getOffsetVectors();
		int[] sum1 = new int[2];
		for (int[] vector : vectors1) {
			sum1[0] += vector[0];
			sum1[1] += vector[1];
		}

		int u1 = 0;
		for (int i = 0; i < vectors1.size(); i++) {
			final int v0 = vectors1.get(i)[0] - sum1[0] / split[1].size();
			final int v1 = vectors1.get(i)[1] - sum1[1] / split[1].size();
			u1 += v0 * v0 + v1 * v1;
		}
		return u0 + u1;

	}

	/**
	 * Applies a split function onto a patch and decides between left and right split.
	 * 
	 * @param raPatch
	 * @param splitFunction
	 * @return
	 */
	public static <T extends RealType<T>> String predictSplit(final RandomAccess<T> raPatch,
			final SplitFunction splitFunction) {
		// get positions
		final int[][] indices = splitFunction.getIndices();
		// decide left <-> right
		raPatch.setPosition(indices[0]);
		final float value1 = raPatch.get().getRealFloat();
		raPatch.setPosition(indices[1]);
		final float value2 = raPatch.get().getRealFloat();
		if (value1 - value2 < splitFunction.getThreshold())
			return LEFT;
		return RIGHT;
	}
}

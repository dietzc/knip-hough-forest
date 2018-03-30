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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.knime.core.node.CanceledExecutionException;
import org.knime.knip.hough.forest.split.SplitFunction.Split;
import org.knime.knip.hough.forest.training.SampleTrainingObject;
import org.knime.knip.hough.forest.training.TrainingObject;
import org.knime.knip.hough.nodes.learner.HoughForestLearnerConfig;

import net.imglib2.type.numeric.RealType;

/**
 * Utils class for creating and evaluating {@link SplitFunction}s.
 * 
 * @author Simon Schmid, University of Konstanz
 */
public final class SplitUtils {

	/**
	 * Creates several random {@link SplitFunction}s.
	 * 
	 * @param patch the patch
	 * @param numFunctions the number of functions to create
	 * @param threshold the maximum threshold
	 * @return random array of {@link SplitFunction}s
	 */
	@Deprecated
	public static SplitFunction[] createSplitFunctions(final TrainingObject<?> trObject,
			final HoughForestLearnerConfig config, final long seed) {
		// TODO take max values instead of training object
		final SplitFunction[] splitFunctions = new SplitFunction[config.getNumSplitFunctions()];
		final Random random = new Random(seed);
		for (int i = 0; i < config.getNumSplitFunctions(); i++) {
			splitFunctions[i] = DefaultSplitFunction.createRandom(trObject, config, random);
		}
		return splitFunctions;
	}

	/**
	 * Creates several random {@link SplitFunction}s.
	 * 
	 * @param <T>
	 * 
	 * @param patch the patch
	 * @param numFunctions the number of functions to create
	 * @param threshold the maximum threshold
	 * @return random array of {@link SplitFunction}s
	 */
	public static <T extends RealType<T>> SplitFunction[] createEntangledSplitFunctions(
			final SampleTrainingObject<T> sample, final int depth, final HoughForestLearnerConfig config,
			final long seed) {
		final SplitFunction[] splitFunctions = new SplitFunction[config.getNumSplitFunctions()];
		final Random random = new Random(seed);
		for (int i = 0; i < config.getNumSplitFunctions(); i++) {
			if (depth < 1 || random.nextDouble() > config.getRatioEntanglement() || !config.getEntanglement()) {
				splitFunctions[i] = DefaultSplitFunction.createRandom(sample.getElementsOfSample().get(0), config,
						random);
			} else {
				if (depth < 2) {
					if (config.getUseMapClassSplitFunction()) {
						splitFunctions[i] = MAPClassSplitFunction.createRandom(config, random);
					} else {
						splitFunctions[i] = DefaultSplitFunction.createRandom(sample.getElementsOfSample().get(0),
								config, random);
					}
				} else {
					final List<String> enabledSFs = config.getEnabledSFs();
					final String sf = enabledSFs.get(random.nextInt(enabledSFs.size()));
					if (sf == HoughForestLearnerConfig.MAP_CLASS_SF) {
						splitFunctions[i] = MAPClassSplitFunction.createRandom(config, random);
					} else if (sf == HoughForestLearnerConfig.NODE_DESCENDANT_SF) {
						splitFunctions[i] = NodeDescendantSplitFunction.createRandom(config, depth, random);
					} else if (sf == HoughForestLearnerConfig.ANCESTOR_NODE_PAIR_SF) {
						splitFunctions[i] = AncestorNodePairSplitFunction.createRandom(config, depth, random);
					} else if (sf == HoughForestLearnerConfig.OFFSET_SIMILARITY_NODE_PAIR_SF) {
						splitFunctions[i] = OffsetSimilarityNodePairSplitFunction.createRandom(config, random);
					} else {
						throw new IllegalStateException("Unknow split function: '" + sf + "'");
					}
				}
			}
		}
		return splitFunctions;
	}

	/**
	 * Splits a sample of {@link TrainingObject}s by definition of a {@link SplitFunction}.
	 * 
	 * @param sample the sample to split
	 * @param splitFunction the {@link SplitFunction}
	 * @return a 2d array of {@link SampleTrainingObject}, containing the left split in the first and the right split in
	 *         the second dimension
	 * @throws CanceledExecutionException
	 */
	@SuppressWarnings("unchecked")
	public static <T extends RealType<T>> SampleTrainingObject<T>[] split(final List<TrainingObject<T>> sample,
			final SplitFunction splitFunction, final int treeIdx) throws CanceledExecutionException {

		final List<TrainingObject<T>> left = new ArrayList<>();
		final List<TrainingObject<T>> right = new ArrayList<>();
		// decide left <-> right
		for (int i = 0; i < sample.size(); i++) {
			final Split splitDecision = splitFunction.apply(sample.get(i), treeIdx, null);
			if (splitDecision == Split.LEFT) {
				left.add(sample.get(i));
			} else {
				right.add(sample.get(i));
			}
		}
		return new SampleTrainingObject[] { new SampleTrainingObject<>(left), new SampleTrainingObject<>(right) };
	}

	/**
	 * Evaluates a split either by entropy or similarity.
	 * 
	 * @param split the splitted {@link SampleTrainingObject}s
	 * @param trainingSet the training set
	 * @param byEntropy if true, evaluation is done by entropy, otherwise by similarity
	 * @return the evaluation value
	 */
	public static <T extends RealType<T>> double evaluateSplit(final SampleTrainingObject<T>[] split,
			final SampleTrainingObject<T> trainingSet, final boolean byEntropy) {
		if (byEntropy) {
			// compute entropy-based
			return ((double) split[0].size() / (split[0].size() + split[1].size())) * split[0].getEntropy(trainingSet)
					+ ((double) split[1].size() / (split[0].size() + split[1].size()))
							* split[1].getEntropy(trainingSet);
		}
		// compute similarity-based
		final List<int[]> vectors0 = split[0].getOffsets();
		double[] mean0 = new double[2];
		for (int[] vector : vectors0) {
			mean0[0] += vector[0];
			mean0[1] += vector[1];
		}
		mean0[0] /= split[0].size();
		mean0[1] /= split[0].size();

		double u0 = 0;
		for (int i = 0; i < vectors0.size(); i++) {
			final double v0 = vectors0.get(i)[0] - mean0[0];
			final double v1 = vectors0.get(i)[1] - mean0[1];
			u0 += v0 * v0 + v1 * v1;
		}

		final List<int[]> vectors1 = split[1].getOffsets();
		double[] mean1 = new double[2];
		for (int[] vector : vectors1) {
			mean1[0] += vector[0];
			mean1[1] += vector[1];
		}
		mean1[0] /= split[1].size();
		mean1[1] /= split[1].size();

		double u1 = 0;
		for (int i = 0; i < vectors1.size(); i++) {
			final double v0 = vectors1.get(i)[0] - mean1[0];
			final double v1 = vectors1.get(i)[1] - mean1[1];
			u1 += v0 * v0 + v1 * v1;
		}
		return u0 + u1;
	}

}

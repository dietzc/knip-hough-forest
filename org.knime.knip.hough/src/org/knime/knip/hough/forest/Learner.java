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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;
import org.knime.knip.core.KNIPGateway;

import net.imglib2.type.numeric.RealType;

/**
 * Several methods used for learning.
 * 
 * @author Simon Schmid, University of Konstanz
 */
public final class Learner {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(Learner.class);

	/**
	 * Trains a tree recursively.
	 * 
	 * @param sample actual sample
	 * @param depth actual tree depth
	 * @param maxDepth max depth of tree
	 * @param minSizeSample min size of the sample
	 * @param numSplitFunctions number of split functions to create
	 * @param threshold threshold value
	 * @param trainingSet whole training set
	 * @param exec execution context
	 * @return leaf oder split node
	 * @throws CanceledExecutionException
	 */
	static <T extends RealType<T>> Node trainDepthFirst(final PatchSample<T> sample, final int depth,
			final int maxDepth, final int minSizeSample, final int numSplitFunctions, final double threshold,
			final PatchSample<T> trainingSet, final ExecutionContext exec, final long seed)
			throws CanceledExecutionException {
		exec.checkCanceled();
		final Random random = new Random(seed);
		// check stop conditions
		if ((depth > maxDepth) || (sample.size() < minSizeSample)) {
			final double[] classProbabilities = HoughForestUtils.computeClassProbabilities(
					sample.getNumberElementsOfClazz0(), sample.getNumberElementsOfClazz1(), trainingSet);
			return new LeafNode<>(sample, classProbabilities, depth);
		}

		// check if sample is pure
		if (sample.getNumberElementsOfClazz0() == 0) {
			return new LeafNode<>(sample, new double[] { 0.0, 1.0 }, depth);
		}
		if (sample.getNumberElementsOfClazz1() == 0) {
			return new LeafNode<>(sample, new double[] { 1.0, 0.0 }, depth);
		}

		// get best Split Function
		final SplitFunction[] splitFunctions = SplitUtils.createSplitFunctions(
				sample.getElementsOfSample().get(0).getPatch(), numSplitFunctions, threshold, random.nextLong());
		SplitFunction bestSplitFunction = splitFunctions[0];
		double minInformationGain = Integer.MAX_VALUE;

		for (int i = 0; i < splitFunctions.length; i++) {
			// split
			final PatchSample<T>[] split = SplitUtils.learnSplit(sample.getElementsOfSample(), splitFunctions[i]);
			// compute information gain
			final double informationGain;
			if (sample.getNumberElementsOfClazz0() / sample.size() < 0.05)
				informationGain = SplitUtils.evaluateSplit(split, trainingSet, false);
			else {
				informationGain = SplitUtils.evaluateSplit(split, trainingSet, random.nextBoolean());
			}
			// if information gain is higher, new information gain and split
			// function are set
			if (informationGain < minInformationGain) {
				minInformationGain = informationGain;
				bestSplitFunction = splitFunctions[i];
			}
		}
		// split with the best split function
		final PatchSample<T>[] bestSplit = SplitUtils.learnSplit(sample.getElementsOfSample(), bestSplitFunction);
		if (bestSplit[0].size() == 0) {
			final double[] classProbabilities = HoughForestUtils.computeClassProbabilities(
					sample.getNumberElementsOfClazz0(), sample.getNumberElementsOfClazz1(), trainingSet);
			return new LeafNode<>(bestSplit[0], classProbabilities, depth);
		}
		if (bestSplit[1].size() == 0) {
			final double[] classProbabilities = HoughForestUtils.computeClassProbabilities(
					sample.getNumberElementsOfClazz0(), sample.getNumberElementsOfClazz1(), trainingSet);
			return new LeafNode<>(bestSplit[1], classProbabilities, depth);
		}

		final Node leftChild = trainDepthFirst(bestSplit[0], depth + 1, maxDepth, minSizeSample, numSplitFunctions,
				threshold, trainingSet, exec, random.nextLong());
		final Node rightChild = trainDepthFirst(bestSplit[1], depth + 1, maxDepth, minSizeSample, numSplitFunctions,
				threshold, trainingSet, exec, random.nextLong());
		return new SplitNode(bestSplitFunction, leftChild, rightChild);
	}

	/**
	 * Trains a tree breadth first.
	 */
	static <T extends RealType<T>> Node trainBreadthFirst(final PatchSample<T> sample, final int depth,
			final int maxDepth, final int minSizeSample, final int numSplitFunctions, final double threshold,
			final PatchSample<T> trainingSet, final ExecutionContext exec, final long seed)
			throws CanceledExecutionException {
		exec.checkCanceled();
		return null;
	}

	/**
	 * Trains a forest with the given parameters.
	 * 
	 * @param trainingSet whole training set
	 * @param maxDepth max depth of trees
	 * @param minSizeSample min size of the samples of a leaf
	 * @param sizeOfSample size of the sample for each tree to learn on
	 * @param numTrees number of trees
	 * @param numSplitFunctions number of splitfunctions to create
	 * @param threshold threshold value
	 * @param exec execution context
	 * @param m_progress
	 * @return list of trees
	 * @throws Exception
	 */
	public static <T extends RealType<T>> List<SplitNode> trainForest(final PatchSample<T> trainingSet,
			final int maxDepth, final int minSizeSample, final int sizeOfSample, final int numTrees,
			final int numSplitFunctions, final double threshold, final ExecutionContext exec, final long seed)
			throws Exception {
		final ExecutorService es = KNIPGateway.threads().getExecutorService();
		final List<TrainParallel<T>> threads = new ArrayList<>(numTrees);
		final List<SplitNode> trees = new ArrayList<>();
		LOGGER.info("Learning " + numTrees + " hough trees...");
		final Random random = new Random(seed);
		for (int i = 1; i <= numTrees; i++) {
			threads.add(new TrainParallel<>(new PatchSample<>(trainingSet.getElementsOfSample()), sizeOfSample,
					maxDepth, minSizeSample, numSplitFunctions, threshold, exec, 0.5 / numTrees, i, random.nextLong()));
		}
		try {
			final List<Future<SplitNode>> invokeAll = es.invokeAll(threads);
			for (final Future<SplitNode> future : invokeAll)
				trees.add(future.get());
		} catch (final InterruptedException e) {
			throw new RuntimeException(e); // TODO exception handling
		}
		es.shutdown();
		LOGGER.info("Learning successfully finished.");
		return trees;
	}
}

final class TrainParallel<T extends RealType<T>> implements Callable<SplitNode> {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(Learner.class);

	private final PatchSample<T> m_trainingSet;
	private final int m_numSamples;
	private final int m_maxDepth;
	private final int m_minSizeSample;
	private final int m_numSplitFunctions;
	private final double m_threshold;
	private final ExecutionContext m_exec;
	private final double m_progress;
	private final int m_idx;
	private final long m_seed;

	TrainParallel(final PatchSample<T> trainingSet, final int numSamples, final int maxDepth, final int minSizeSample,
			final int numSplitFunctions, final double threshold, final ExecutionContext exec, final double progress,
			final int idx, final long seed) {
		m_trainingSet = trainingSet;
		m_numSamples = numSamples;
		m_maxDepth = maxDepth;
		m_minSizeSample = minSizeSample;
		m_numSplitFunctions = numSplitFunctions;
		m_threshold = threshold;
		m_exec = exec;
		m_progress = progress;
		m_idx = idx;
		m_seed = seed;
	}

	@Override
	public SplitNode call() throws Exception {
		LOGGER.info("Learning hough tree no. " + m_idx + "...");
		final PatchSample<T> randomSample = HoughForestUtils.randomSample(m_trainingSet, m_numSamples, m_seed); // TODO
		final Node root = Learner.trainDepthFirst(randomSample, 0, m_maxDepth, m_minSizeSample, m_numSplitFunctions,
				m_threshold, m_trainingSet, m_exec, m_seed);
		if (root instanceof SplitNode) {
			LOGGER.info("Hough tree no. " + m_idx + " learned succesfully.");
			m_exec.setProgress(m_exec.getProgressMonitor().getProgress() + m_progress);
			return (SplitNode) root;
		} else {
			LOGGER.info("Learning of hough tree no. " + m_idx + " failed. Trying again..."); // FIXME
			return call();
		}
	}

}

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
package org.knime.knip.hough.forest.training;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;
import org.knime.knip.core.KNIPGateway;
import org.knime.knip.hough.forest.HoughForestUtils;
import org.knime.knip.hough.forest.node.LeafNode;
import org.knime.knip.hough.forest.node.Node;
import org.knime.knip.hough.forest.node.SplitNode;
import org.knime.knip.hough.forest.split.SplitFunction;
import org.knime.knip.hough.forest.split.SplitUtils;
import org.knime.knip.hough.nodes.learner.HoughForestLearnerConfig;

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
	 * @param seed the seed
	 * @return leaf oder split node
	 * @throws CanceledExecutionException
	 */
	private static <T extends RealType<T>> Node trainDepthFirst(final SampleTrainingObject<T> sample, final int depth,
			final HoughForestLearnerConfig config, final SampleTrainingObject<T> trainingSet,
			final ExecutionContext exec, final long seed) throws CanceledExecutionException {
		exec.checkCanceled();
		final Random random = new Random(seed);

		// check stop conditions
		final Optional<LeafNode> leafNode = LearnerUtils.checkStopConditions(sample, depth, trainingSet, config);
		if (leafNode.isPresent()) {
			return leafNode.get();
		}

		// get best Split Function
		final SplitFunction bestSplitFunction = LearnerUtils.findBestSplitFunction(sample, config, trainingSet, random);

		// split with the best split function
		final SampleTrainingObject<T>[] bestSplit = SplitUtils.split(sample.getElementsOfSample(), bestSplitFunction,
				0);

		// check if split is pure
		if (bestSplit[0].size() == 0) {
			final double[] classProbabilities = HoughForestUtils.computeClassProbabilities(
					bestSplit[1].getNumberElementsOfClazz0(), bestSplit[1].getNumberElementsOfClazz1(), trainingSet);
			return new LeafNode(bestSplit[1], classProbabilities, depth, -1, null);
		}
		if (bestSplit[1].size() == 0) {
			final double[] classProbabilities = HoughForestUtils.computeClassProbabilities(
					bestSplit[0].getNumberElementsOfClazz0(), bestSplit[0].getNumberElementsOfClazz1(), trainingSet);
			return new LeafNode(bestSplit[0], classProbabilities, depth, -1, null);
		}

		// train children recursively
		final Node leftChild = trainDepthFirst(bestSplit[0], depth + 1, config, trainingSet, exec, random.nextLong());
		final Node rightChild = trainDepthFirst(bestSplit[1], depth + 1, config, trainingSet, exec, random.nextLong());
		return new SplitNode(bestSplitFunction, depth, -1, null, null, leftChild, rightChild, null);
	}

	/**
	 * Trains a forest with the given parameters.
	 * 
	 * @param trainingSet whole training set
	 * @param maxDepth max depth of trees
	 * @param minSizeSample min size of the samples of a leaf
	 * @param sizeOfSample size of the sample for each tree to learn on
	 * @param numTrees number of trees
	 * @param numSplitFunctions number of split functions to create
	 * @param threshold threshold value
	 * @param exec execution context
	 * @param m_progress
	 * @return list of trees
	 * @throws Exception
	 */
	public static <T extends RealType<T>> List<SplitNode> trainForest(final SampleTrainingObject<T> trainingSet,
			final HoughForestLearnerConfig config, final ExecutionContext exec, final long seed) throws Exception {
		final ExecutorService es = KNIPGateway.threads().getExecutorService();
		final List<TrainParallel<T>> threads = new ArrayList<>(config.getNumTrees());
		final List<SplitNode> trees = new ArrayList<>();
		LOGGER.info("Learning " + config.getNumTrees() + " hough trees...");
		final Random random = new Random(seed);
		for (int i = 0; i < config.getNumTrees(); i++) {
			threads.add(
					new TrainParallel<>(trainingSet, config, exec, 0.5 / config.getNumTrees(), i, random.nextLong()));
		}
		try {
			final List<Future<SplitNode>> invokeAll = es.invokeAll(threads);
			for (final Future<SplitNode> future : invokeAll)
				trees.add(future.get());
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}
		es.shutdown();
		LOGGER.info("Learning successfully finished.");
		return trees;
	}

	private static final class TrainParallel<T extends RealType<T>> implements Callable<SplitNode> {

		private final SampleTrainingObject<T> m_trainingSet;
		private final HoughForestLearnerConfig m_config;
		private final ExecutionContext m_exec;
		private final double m_progress;
		private final int m_idx;
		private final long m_seed;

		private int m_numFails;

		TrainParallel(final SampleTrainingObject<T> trainingSet, final HoughForestLearnerConfig config,
				final ExecutionContext exec, final double progress, final int idx, final long seed) {
			m_trainingSet = trainingSet;
			m_config = config;
			m_exec = exec;
			m_progress = progress;
			m_idx = idx;
			m_seed = seed;
			m_numFails = 0;
		}

		@Override
		public SplitNode call() throws Exception {
			LOGGER.info("Learning hough tree no. " + m_idx + "...");
			final SampleTrainingObject<T> randomSample = HoughForestUtils.randomSample(m_trainingSet,
					m_config.getNumSamples(), m_seed);
			final Node root;
			root = Learner.trainDepthFirst(randomSample, 0, m_config, m_trainingSet, m_exec, m_seed);
			if (root instanceof SplitNode) {
				LOGGER.info("Hough tree no. " + m_idx + " learned succesfully.");
				m_exec.setProgress(m_exec.getProgressMonitor().getProgress() + m_progress);
				return (SplitNode) root;
			} else {
				if (m_numFails < 3) {
					m_numFails++;
					LOGGER.info("Learning of hough tree no. " + m_idx + " failed " + m_numFails
							+ " times. Trying again...");
					return call();
				} else {
					final String msg = "Learning of hough tree no. " + m_idx
							+ " failed three times. Check training data and parameter settings.";
					LOGGER.error(msg);
					throw new IllegalStateException(msg);
				}
			}
		}
	}
}

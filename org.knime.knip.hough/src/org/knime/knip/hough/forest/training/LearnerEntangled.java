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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;
import org.knime.knip.core.KNIPGateway;
import org.knime.knip.hough.forest.HoughForestUtils;
import org.knime.knip.hough.forest.node.EntangledLeafNode;
import org.knime.knip.hough.forest.node.FrontierNode;
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
public final class LearnerEntangled {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(LearnerEntangled.class);

	/**
	 * Trains a tree breadth first.
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
	private static <T extends RealType<T>> Node trainBreadthFirst(final SampleTrainingObject<T> sample, final int depth,
			final HoughForestLearnerConfig config, final SampleTrainingObject<T> trainingSet, final int treeIdx,
			final ExecutionContext exec, final long seed) throws CanceledExecutionException {
		// learn root
		final double[] classProbabilities = HoughForestUtils.computeClassProbabilities(
				sample.getNumberElementsOfClazz0(), sample.getNumberElementsOfClazz1(), trainingSet);
		final FrontierNode<T> rootNode = new FrontierNode<>(sample, trainingSet, depth, 0, null, true,
				classProbabilities, new Random(seed).nextLong());
		List<FrontierNode<T>> frontierNodes = new ArrayList<>();
		frontierNodes.add(rootNode);
		frontierNodes = trainBreadthLevel(frontierNodes, trainingSet, config, treeIdx, exec);
		final Node root;
		if (!frontierNodes.isEmpty()) {
			root = frontierNodes.get(0).getParent();
		} else {
			// if empty, no split was learned
			// return a dummy leaf node to trigger a retraining of the tree
			root = new LeafNode();
		}

		// learn until only leafs are left
		while (!frontierNodes.isEmpty()) {
			frontierNodes = trainBreadthLevel(frontierNodes, trainingSet, config, treeIdx, exec);
		}
		return root;
	}

	private static <T extends RealType<T>> List<FrontierNode<T>> trainBreadthLevel(
			final List<FrontierNode<T>> frontierNodes, final SampleTrainingObject<T> trainingSet,
			final HoughForestLearnerConfig config, final int treeIdx, final ExecutionContext exec)
			throws CanceledExecutionException {
		final List<FrontierNode<T>> newList = new ArrayList<>();
		final Map<SampleTrainingObject<?>, Node> newNodesToSet = new HashMap<>();
		for (final FrontierNode<T> frontierNode : frontierNodes) {
			exec.checkCanceled();
			final SampleTrainingObject<T> sampledTObjects = frontierNode.getSampledTrainingObjects();
			final Random random = new Random(frontierNode.getSeed());
			final int depth = frontierNode.getDepth();
			final int nodeIdx = frontierNode.getNodeIdx();

			// check stop conditions
			final Optional<EntangledLeafNode> leafNodeOpt = LearnerUtils.checkEntangledStopConditions(frontierNode,
					trainingSet, config);
			if (leafNodeOpt.isPresent()) {
				final EntangledLeafNode leafNode = leafNodeOpt.get();
				LearnerUtils.convertToLeafNode(frontierNode, leafNode);
				newNodesToSet.put(leafNode.getAllTrainingObjects(), leafNode);
				continue;
			}

			// get best split function
			final SplitFunction bestSplitFunction = LearnerUtils.findEntangledBestSplitFunction(sampledTObjects, config,
					trainingSet, treeIdx, depth, random, exec);

			// split with the best split function
			final SampleTrainingObject<T>[] bestSplit = SplitUtils.split(sampledTObjects.getElementsOfSample(),
					bestSplitFunction, treeIdx);

			// check if split is pure
			if (bestSplit[0].size() == 0) {
				final EntangledLeafNode ln = new EntangledLeafNode(frontierNode.getSampledTrainingObjects(),
						frontierNode.getProbabilities(), depth, nodeIdx, frontierNode.getAllTrainingObjects(),
						frontierNode.getParent());
				LearnerUtils.convertToLeafNode(frontierNode, ln);
				newNodesToSet.put(frontierNode.getAllTrainingObjects(), ln);
				continue;
			}
			if (bestSplit[1].size() == 0) {
				final EntangledLeafNode ln = new EntangledLeafNode(frontierNode.getSampledTrainingObjects(),
						frontierNode.getProbabilities(), depth, nodeIdx, frontierNode.getAllTrainingObjects(),
						frontierNode.getParent());
				LearnerUtils.convertToLeafNode(frontierNode, ln);
				newNodesToSet.put(frontierNode.getAllTrainingObjects(), ln);
				continue;
			}

			// compute class probabilities
			final double[] classProbabilities0 = HoughForestUtils.computeClassProbabilities(
					bestSplit[0].getNumberElementsOfClazz0(), bestSplit[0].getNumberElementsOfClazz1(), trainingSet);
			final double[] classProbabilities1 = HoughForestUtils.computeClassProbabilities(
					bestSplit[1].getNumberElementsOfClazz0(), bestSplit[1].getNumberElementsOfClazz1(), trainingSet);

			// split all training objects
			final SampleTrainingObject<T>[] splitAllTObjects = SplitUtils
					.split(frontierNode.getAllTrainingObjects().getElementsOfSample(), bestSplitFunction, treeIdx);

			// train children recursively
			final SplitNode splitNode = new SplitNode(bestSplitFunction, depth, nodeIdx,
					frontierNode.getOffsetVectors(), frontierNode.getProbabilities(), frontierNode.getParent());
			final FrontierNode<T> leftChild = new FrontierNode<>(bestSplit[0], splitAllTObjects[0], depth + 1,
					(nodeIdx * 2) + 1, splitNode, true, classProbabilities0, random.nextLong());
			newNodesToSet.put(splitAllTObjects[0], leftChild);
			final FrontierNode<T> rightChild = new FrontierNode<>(bestSplit[1], splitAllTObjects[1], depth + 1,
					(nodeIdx * 2) + 2, splitNode, false, classProbabilities1, random.nextLong());
			newNodesToSet.put(splitAllTObjects[1], rightChild);
			splitNode.setLeftChild(leftChild);
			splitNode.setRightChild(rightChild);
			LearnerUtils.convertToSplitNode(frontierNode, splitNode);
			newList.add(leftChild);
			newList.add(rightChild);
		}
		// update node grid
		for (final SampleTrainingObject<?> tObjs : newNodesToSet.keySet()) {
			LearnerUtils.setNodeGrid(treeIdx, tObjs, newNodesToSet.get(tObjs));
		}
		return newList;
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
	 * @throws ExecutionException
	 */
	public static <T extends RealType<T>> List<SplitNode> trainForest(final SampleTrainingObject<T> trainingSet,
			final HoughForestLearnerConfig config, final ExecutionContext exec, final long seed)
			throws ExecutionException {
		final ExecutorService es = KNIPGateway.threads().getExecutorService();
		final List<TrainParallel<T>> threads = new ArrayList<>(config.getNumTrees());
		final List<SplitNode> trees = new ArrayList<>();
		LOGGER.info("Learning " + config.getNumTrees() + " hough trees...");
		final Random random = new Random(seed);
		for (int i = 0; i < config.getNumTrees(); i++) {
			threads.add(
					new TrainParallel<>(trainingSet, config, exec, 0.5 / config.getNumTrees(), i, random.nextLong()));
		}
		long start = System.currentTimeMillis();
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
		long stop = System.currentTimeMillis();
		LOGGER.debug("Execution time for learning: " + (stop - start) + "ms");
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
			m_exec = exec;
			m_config = config;
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
			root = LearnerEntangled.trainBreadthFirst(randomSample, 0, m_config, m_trainingSet, m_idx, m_exec, m_seed);
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

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

import java.util.Optional;
import java.util.Random;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
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
public final class LearnerUtils {

	static <T extends RealType<T>> Optional<LeafNode> checkStopConditions(final SampleTrainingObject<?> sample,
			final int depth, final SampleTrainingObject<T> trainingSet, final HoughForestLearnerConfig config) {
		// check stop conditions
		if ((depth > config.getDepth()) || (sample.size() < config.getMinSizeSample())) {
			final double[] classProbabilities = HoughForestUtils.computeClassProbabilities(
					sample.getNumberElementsOfClazz0(), sample.getNumberElementsOfClazz1(), trainingSet);
			return Optional.of(new LeafNode(sample, classProbabilities, depth, -1, null));
		}

		// check if sample is pure
		if (sample.getNumberElementsOfClazz0() == 0) {
			return Optional.of(new LeafNode(sample, new double[] { 0.0, 1.0 }, depth, -1, null));
		}
		if (sample.getNumberElementsOfClazz1() == 0) {
			return Optional.of(new LeafNode(sample, new double[] { 1.0, 0.0 }, depth, -1, null));
		}
		return Optional.empty();
	}

	static <T extends RealType<T>> SplitFunction findBestSplitFunction(final SampleTrainingObject<T> sample,
			final HoughForestLearnerConfig config, final SampleTrainingObject<T> trainingSet, final Random random)
			throws CanceledExecutionException {
		final SplitFunction[] splitFunctions = SplitUtils.createSplitFunctions(sample.getElementsOfSample().get(0),
				config, random.nextLong());
		SplitFunction bestSplitFunction = splitFunctions[0];
		double minInformationGain = Integer.MAX_VALUE;

		for (int i = 0; i < splitFunctions.length; i++) {
			// split
			final SampleTrainingObject<T>[] split = SplitUtils.split(sample.getElementsOfSample(), splitFunctions[i],
					0);
			// compute information gain
			final double informationGain;
			if (sample.getNumberElementsOfClazz0() / sample.size() < 0.05)
				informationGain = SplitUtils.evaluateSplit(split, trainingSet, false);
			else {
				informationGain = SplitUtils.evaluateSplit(split, trainingSet, random.nextBoolean());
			}
			// if information gain is higher, new information gain and split function are set
			if (informationGain < minInformationGain) {
				minInformationGain = informationGain;
				bestSplitFunction = splitFunctions[i];
			}
		}
		return bestSplitFunction;
	}

	static <T extends RealType<T>> Optional<EntangledLeafNode> checkEntangledStopConditions(
			final FrontierNode<T> frNode, final SampleTrainingObject<T> trainingSet,
			final HoughForestLearnerConfig config) {
		// check stop conditions
		final SampleTrainingObject<T> sample = frNode.getSampledTrainingObjects();
		final SampleTrainingObject<T> allTrainingObjects = frNode.getAllTrainingObjects();
		if ((frNode.getDepth() >= config.getDepth()) || (sample.size() < config.getMinSizeSample())) {
			return Optional.of(new EntangledLeafNode(sample, frNode.getProbabilities(), frNode.getDepth(),
					frNode.getNodeIdx(), allTrainingObjects, frNode.getParent()));
		}

		// check if sample is pure
		if (sample.getNumberElementsOfClazz0() == 0) {
			return Optional.of(new EntangledLeafNode(sample, new double[] { 0.0, 1.0 }, frNode.getDepth(),
					frNode.getNodeIdx(), allTrainingObjects, frNode.getParent()));
		}
		if (sample.getNumberElementsOfClazz1() == 0) {
			return Optional.of(new EntangledLeafNode(sample, new double[] { 1.0, 0.0 }, frNode.getDepth(),
					frNode.getNodeIdx(), allTrainingObjects, frNode.getParent()));
		}
		return Optional.empty();
	}

	static <T extends RealType<T>> SplitFunction findEntangledBestSplitFunction(final SampleTrainingObject<T> sample,
			final HoughForestLearnerConfig config, final SampleTrainingObject<T> trainingSet, final int treeIdx,
			final int depth, final Random random, final ExecutionContext exec) throws CanceledExecutionException {
		final SplitFunction[] splitFunctions = SplitUtils.createEntangledSplitFunctions(sample, depth, config,
				random.nextLong());
		SplitFunction bestSplitFunction = splitFunctions[0];
		double minInformationGain = Integer.MAX_VALUE;

		for (int i = 0; i < splitFunctions.length; i++) {
			exec.checkCanceled();
			// split
			final SampleTrainingObject<T>[] split = SplitUtils.split(sample.getElementsOfSample(), splitFunctions[i],
					treeIdx);
			// compute information gain
			final double informationGain;
			if ((double) sample.getNumberElementsOfClazz0() / sample.size() < 0.05)
				informationGain = SplitUtils.evaluateSplit(split, trainingSet, false);
			else {
				informationGain = SplitUtils.evaluateSplit(split, trainingSet, random.nextBoolean());
			}
			// if information gain is higher, new information gain and split function are set
			if (informationGain < minInformationGain) {
				minInformationGain = informationGain;
				bestSplitFunction = splitFunctions[i];
			}
		}
		return bestSplitFunction;
	}

	static <T extends RealType<T>> void convertToLeafNode(final FrontierNode<T> frontierNode, final LeafNode leafNode) {
		final SplitNode parent = frontierNode.getParent();
		if (parent != null) {
			if (frontierNode.isLeftChild()) {
				parent.setLeftChild(leafNode);
			} else {
				parent.setRightChild(leafNode);
			}
		}
	}

	static <T extends RealType<T>> void convertToSplitNode(final FrontierNode<T> frontierNode,
			final SplitNode splitNode) {
		final SplitNode parent = frontierNode.getParent();
		if (parent != null) {
			if (frontierNode.isLeftChild()) {
				parent.setLeftChild(splitNode);
			} else {
				parent.setRightChild(splitNode);
			}
		}
	}

	static void setNodeGrid(final int treeIdx, final SampleTrainingObject<?> sample, final Node node) {
		for (final TrainingObject<?> tObj : sample.getElementsOfSample()) {
			tObj.setNodeGrid(treeIdx, node);
		}
	}

}

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

import org.knime.knip.hough.forest.training.SampleTrainingObject;
import org.knime.knip.hough.forest.training.TrainingObject;

import net.imglib2.type.numeric.RealType;

/**
 * Utils class for hough forests.
 * 
 * @author Simon Schmid, University of Konstanz
 */
public final class HoughForestUtils {

	/**
	 * Computes class probabilities.
	 * 
	 * @param size0 number of elements with class 0
	 * @param size1 number of elements with class 1
	 * @param trainingSet the whole training set
	 * @return class probabilities
	 */
	public static <T extends RealType<T>> double[] computeClassProbabilities(final int size0, final int size1,
			final SampleTrainingObject<T> trainingSet) {
		final double[] probability = new double[2];
		if (size0 == 0) {
			if (size1 == 0) {
				probability[0] = 0.5;
				probability[1] = 0.5;
			} else {
				probability[0] = 0;
				probability[1] = 1;
			}
		} else {
			if (size1 == 0) {
				probability[0] = 1;
				probability[1] = 0;
			}

			else {
				double rc0 = (double) trainingSet.size() / trainingSet.getNumberElementsOfClazz0();
				double rc1 = (double) trainingSet.size() / trainingSet.getNumberElementsOfClazz1();
				probability[0] = (size0 * rc0) / (size0 * rc0 + size1 * rc1);
				probability[1] = 1 - probability[0];
			}
		}
		return probability;
	}

	/**
	 * Creates a random sample of patches.
	 * 
	 * @param sample the {@link SampleTrainingObject} to sample from
	 * @param size the size of the smaple to create
	 * @return the random sample
	 */
	public static <T extends RealType<T>> SampleTrainingObject<T> randomSample(final SampleTrainingObject<T> sample,
			final int size, final long seed) {
		final List<TrainingObject<T>> listOfSample = sample.getElementsOfSample();
		final List<TrainingObject<T>> randomSample = new ArrayList<>();
		final Random randomGenerator = new Random(seed);
		for (int i = 0; i < size; i++) {
			int index = randomGenerator.nextInt(listOfSample.size());
			randomSample.add(listOfSample.get(index));
		}
		return new SampleTrainingObject<>(randomSample);
	}

}

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

import net.imglib2.type.numeric.RealType;

/**
 * This object holds a list of {@link TrainingObject}s and offers some
 * calculation methods.
 * 
 * @author Simon Schmid, University of Konstanz
 */
public final class PatchSample<T extends RealType<T>> {

	private final List<TrainingObject<T>> listOfElements;
	private int[] numberElementsOfClazzes;
	private double entropy;

	/**
	 * @param listOfElements a list of {@link TrainingObject}s which shall be
	 *            contained in the sample
	 */
	public PatchSample(final List<TrainingObject<T>> listOfElements) {
		this.listOfElements = listOfElements;
		numberElementsOfClazzes = null;
		entropy = 0;
	}

	/**
	 * @return a list of all {@link TrainingObject} in this sample
	 */
	public List<TrainingObject<T>> getElementsOfSample() {
		return listOfElements;
	}

	/**
	 * @return size of this sample
	 */
	public int size() {
		return listOfElements.size();
	}

	/**
	 * @param trainingSet whole training set, used for computation
	 * @return entropy of this sample
	 */
	public double getEntropy(final PatchSample<T> trainingSet) {
		if (entropy == 0 && size() > 0) {
			final double[] probabilities = HoughForestUtils.computeClassProbabilities(getNumberElementsOfClazz0(),
					getNumberElementsOfClazz1(), trainingSet);
			entropy = -Math.log(probabilities[0]) * probabilities[0] - Math.log(probabilities[1]) * probabilities[1];
		}
		return entropy;
	}

	/**
	 * @return number of elements which are class 0
	 */
	public int getNumberElementsOfClazz0() {
		if (numberElementsOfClazzes == null) {
			computeNumberElemtentsOfClazzes();
		}
		return numberElementsOfClazzes[0];
	}

	/**
	 * @return number of elements which are class 1
	 */
	public int getNumberElementsOfClazz1() {
		if (numberElementsOfClazzes == null) {
			computeNumberElemtentsOfClazzes();
		}
		return numberElementsOfClazzes[1];
	}

	private void computeNumberElemtentsOfClazzes() {
		numberElementsOfClazzes = new int[2];
		int num = 0;
		for (final TrainingObject<T> element : listOfElements) {
			if (element.getClazz() == 0)
				num++;
		}
		numberElementsOfClazzes[0] = num;
		numberElementsOfClazzes[1] = size() - num;
	}

	/**
	 * @return the offset vectors of all elements with class 1 in this sample
	 */
	public List<int[]> getOffsetVectors() {
		List<int[]> vectors = new ArrayList<>();
		for (TrainingObject<T> element : listOfElements) {
			if (element.getClazz() == 1)
				vectors.add(element.getOffset());
		}
		return vectors;
	}
}

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
import java.util.Arrays;
import java.util.List;

import org.knime.knip.hough.forest.HoughForestUtils;

import net.imglib2.type.numeric.RealType;

/**
 * This object holds a list of {@link TrainingObject}s and offers some calculation methods.
 * 
 * @author Simon Schmid, University of Konstanz
 */
public final class SampleTrainingObject<T extends RealType<T>> {

	private final List<TrainingObject<T>> m_listOfElements;
	private final int[] m_numberElementsOfClazzes;
	private final List<int[]> m_offsets;
	private double m_entropy;

	/**
	 * @param listOfElements a list of {@link TrainingObject}s which shall be contained in the sample
	 */
	public SampleTrainingObject(final List<TrainingObject<T>> listOfElements) {
		m_listOfElements = listOfElements;
		m_numberElementsOfClazzes = computeNumberElemtentsOfClazzes();
		m_offsets = collectOffsets();
		m_entropy = 0;
	}

	/**
	 * @return a list of all {@link TrainingObject} in this sample
	 */
	public List<TrainingObject<T>> getElementsOfSample() {
		return m_listOfElements;
	}

	/**
	 * @return size of this sample
	 */
	public int size() {
		return m_listOfElements.size();
	}

	/**
	 * @param trainingSet whole training set, used for computation
	 * @return entropy of this sample
	 */
	public synchronized double getEntropy(final SampleTrainingObject<T> trainingSet) {
		if (m_entropy == 0 && size() > 0) {
			final double[] probabilities = HoughForestUtils.computeClassProbabilities(getNumberElementsOfClazz0(),
					getNumberElementsOfClazz1(), trainingSet);
			m_entropy = -Math.log(probabilities[0]) * probabilities[0] - Math.log(probabilities[1]) * probabilities[1];
		}
		return m_entropy;
	}

	/**
	 * @return number of elements which are class 0
	 */
	public int getNumberElementsOfClazz0() {
		return m_numberElementsOfClazzes[0];
	}

	/**
	 * @return number of elements which are class 1
	 */
	public int getNumberElementsOfClazz1() {
		return m_numberElementsOfClazzes[1];
	}

	private int[] computeNumberElemtentsOfClazzes() {
		int num = 0;
		for (final TrainingObject<T> element : m_listOfElements) {
			if (element.getClazz() == 0)
				num++;
		}
		return new int[] { num, size() - num };
	}

	/**
	 * @return the offset vectors of all elements with class 1 in this sample
	 */
	public List<int[]> getOffsets() {
		return m_offsets;
	}

	private List<int[]> collectOffsets() {
		final List<int[]> vectors = new ArrayList<>();
		for (final TrainingObject<T> element : m_listOfElements) {
			if (element.getClazz() == 1)
				vectors.add(element.getOffset());
		}
		return vectors;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(m_entropy);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((m_listOfElements == null) ? 0 : m_listOfElements.hashCode());
		result = prime * result + Arrays.hashCode(m_numberElementsOfClazzes);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SampleTrainingObject<?> other = (SampleTrainingObject<?>) obj;
		if (Double.doubleToLongBits(m_entropy) != Double.doubleToLongBits(other.m_entropy))
			return false;
		if (m_listOfElements == null) {
			if (other.m_listOfElements != null)
				return false;
		} else if (!m_listOfElements.equals(other.m_listOfElements))
			return false;
		if (!Arrays.equals(m_numberElementsOfClazzes, other.m_numberElementsOfClazzes))
			return false;
		return true;
	}
}

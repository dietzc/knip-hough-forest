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
package org.knime.knip.hough.features;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

import org.knime.scijava.core.ResourceAwareClassLoader;
import org.scijava.Context;
import org.scijava.plugin.DefaultPluginFinder;
import org.scijava.plugin.PluginIndex;

import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

/**
 * This class allows to create a feature descriptor out of an image in form of a {@link RandomAccessibleInterval}. It
 * can be set which features shall be extracted.
 * 
 * @author Simon Schmid, University of Konstanz
 */
public class FeatureDescriptor<T extends RealType<T>> implements Externalizable {

	private final OpService m_ops;

	private final List<Feature> m_feaures;

	private boolean m_isColorImage;

	private boolean m_convertToLab;

	private boolean m_addFirstDerivative;

	private boolean m_useAbsoluteFirstDerivative;

	private boolean m_addSecondDerivative;

	private boolean m_useAbsoluteSecondDerivative;

	private boolean m_addHoG;

	private int m_hogNumBins;

	private boolean m_applyMinMax;

	private boolean m_useAbsoluteValues;

	private final Converter<T, FloatType> m_converterToFloatType;

	private final Converter<FloatType, FloatType> m_converterToAbsoluteValues;

	/**
	 * Creates a feature descriptor which can be applied onto an image. All parameters take the default value, i.e. no
	 * feature will be extracted.
	 */
	public FeatureDescriptor(final boolean numInputChannels) {
		m_ops = new Context(new PluginIndex(
				new DefaultPluginFinder(new ResourceAwareClassLoader(getClass().getClassLoader(), getClass()))))
						.getService(OpService.class);

		m_feaures = new ArrayList<>();

		m_isColorImage = numInputChannels;

		m_converterToFloatType = new Converter<T, FloatType>() {
			@Override
			public void convert(final T arg0, final FloatType arg1) {
				arg1.setReal(arg0.getRealFloat());
			}
		};
		m_converterToAbsoluteValues = new Converter<FloatType, FloatType>() {
			@Override
			public void convert(final FloatType arg0, final FloatType arg1) {
				final float v = arg0.getRealFloat();
				if (v >= 0)
					arg1.setReal(arg0.getRealFloat());
				else
					arg1.setReal(-1 * arg0.getRealFloat());
			}
		};
	}

	/**
	 * Creates a feature descriptor which can be applied onto an image. All parameters can be set to determine which
	 * features shall be extracted.
	 * 
	 * @param convertToLab
	 * @param firstDerivative
	 * @param useAbsoluteFirstDerivative
	 * @param secondDerivative
	 * @param useAbsoluteSecondDerivative
	 * @param hog
	 * @param numberHog
	 * @param applyMinMax
	 * @param useAbsoluteValues
	 */
	public FeatureDescriptor(final boolean numInputChannels, final boolean convertToLab, final boolean firstDerivative,
			final boolean useAbsoluteFirstDerivative, final boolean secondDerivative,
			final boolean useAbsoluteSecondDerivative, final boolean hog, final int numberHog,
			final boolean applyMinMax, final boolean useAbsoluteValues) {
		this(numInputChannels);
		this.setConvertToLab(convertToLab);
		this.setAddFirstDerivative(firstDerivative);
		this.setUseAbsoluteFirstDerivative(useAbsoluteFirstDerivative);
		this.setAddSecondDerivative(secondDerivative);
		this.setUseAbsoluteSecondDerivative(useAbsoluteSecondDerivative);
		this.setAddHoG(hog);
		this.setHogNumBins(numberHog);
		this.setApplyMinMax(applyMinMax);
		this.setUseAbsoluteValues(useAbsoluteValues);

		fillFeatures();
	}

	private void fillFeatures() {
		if (isAddFirstDerivative()) {
			m_feaures.add(new DerivativesFeature(isAddSecondDerivative(), isUseAbsoluteFirstDerivative(),
					isUseAbsoluteSecondDerivative()));
		}
		if (isAddHoG()) {
			m_feaures.add(new HoGFeature(getHogNumBins()));
		}
		if (isApplyMinMax()) {
			m_feaures.add(new MinMaxFilterFeature());
		}
	}

	/**
	 * Creates a 3D feature descriptor where each feature is 2D and is stored in the 3rd dimension. The descriptor
	 * content depends on the settings set in the constructor.
	 * 
	 * @param in input image (can contain one or three color channels, e.g. grayscale or RGB)
	 * @return 3D feature descriptor
	 */
	@SuppressWarnings("unchecked")
	public RandomAccessibleInterval<FloatType> apply(RandomAccessibleInterval<T> in) {
		if (!(in.numDimensions() == 2 && !m_isColorImage)
				&& !(in.numDimensions() == 3 && in.dimension(2) == 3 && m_isColorImage)) {
			throw new IllegalArgumentException("Input image has wrong dimensionality!");
		}
		List<RandomAccessibleInterval<FloatType>> list = new ArrayList<>();
		final RandomAccessibleInterval<FloatType> inFloatType;
		if (in.numDimensions() == 3 && in.dimension(2) == 3 && isConvertToLab()) {
			// convert from RGB to LAB space
			final RandomAccessibleInterval<FloatType>[] inArray = new RandomAccessibleInterval[in.numDimensions()];
			final RandomAccessibleInterval<FloatType>[] outArray = new RandomAccessibleInterval[in.numDimensions()];
			for (int i = 0; i < inArray.length; i++) {
				inArray[i] = Views.hyperSlice(Converters.convert(in, m_converterToFloatType, new FloatType()), 2, i);
				outArray[i] = Views.hyperSlice(m_ops.create().img(in, new FloatType()), 2, i);
			}
			convertRGBtoLAB(outArray, inArray);

			// add LAB channels to the list
			for (int i = 0; i < in.dimension(2); i++) {
				list.add(outArray[i]);
			}

			inFloatType = Views.stack(outArray);
		} else {
			// use RGB for computations
			inFloatType = Converters.convert(in, m_converterToFloatType, new FloatType());
			if (inFloatType.numDimensions() == 2)
				list.add(inFloatType);
			else {
				for (int i = 0; i < inFloatType.dimension(2); i++) {
					list.add(Views.hyperSlice(inFloatType, 2, i));
				}
			}
		}

		// apply features
		for (final Feature feature : m_feaures) {
			list = feature.apply(inFloatType, list, m_ops);
		}

		final RandomAccessibleInterval<FloatType> stackedOuput = Views.stack(list);
		if (isUseAbsoluteValues())
			return Converters.convert(stackedOuput, m_converterToAbsoluteValues, new FloatType());
		return stackedOuput;
	}

	/**
	 * Converts a RGB to a Lab image.
	 * 
	 * @param out a Lab {@link RandomAccessibleInterval}
	 * @param in a RGB {@link RandomAccessibleInterval}
	 */
	@SuppressWarnings("unchecked")
	private static void convertRGBtoLAB(final RandomAccessibleInterval<FloatType>[] out,
			final RandomAccessibleInterval<FloatType>[] in) {
		final ImageJColorSpaceConverter m_conv = new ImageJColorSpaceConverter();

		final Cursor<FloatType>[] inC = new Cursor[3];
		final Cursor<FloatType>[] outC = new Cursor[3];
		for (int i = 0; i < inC.length; i++) {
			inC[i] = Views.flatIterable(in[i]).cursor();
			outC[i] = Views.flatIterable(out[i]).cursor();
		}
		while (inC[0].hasNext()) {
			for (int i = 0; i < outC.length; i++) {
				inC[i].fwd();
				outC[i].fwd();
			}
			final int r = (int) (inC[0].get()).getRealDouble();
			final int g = (int) (inC[1].get()).getRealDouble();
			final int b = (int) (inC[2].get()).getRealDouble();
			final double[] res = m_conv.XYZtoLAB(m_conv.RGBtoXYZ(r, g, b));
			for (int i = 0; i < res.length; i++) {
				outC[i].get().setReal(res[i]);
			}
		}
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		m_isColorImage = in.readBoolean();
		setConvertToLab(in.readBoolean());
		setAddFirstDerivative(in.readBoolean());
		setAddSecondDerivative(in.readBoolean());
		setAddHoG(in.readBoolean());
		setHogNumBins(in.readInt());
		setApplyMinMax(in.readBoolean());
		setUseAbsoluteValues(in.readBoolean());

		fillFeatures();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeBoolean(m_isColorImage);
		out.writeBoolean(isConvertToLab());
		out.writeBoolean(isAddFirstDerivative());
		out.writeBoolean(isAddSecondDerivative());
		out.writeBoolean(isAddHoG());
		out.writeInt(getHogNumBins());
		out.writeBoolean(isApplyMinMax());
		out.writeBoolean(isUseAbsoluteValues());
	}

	/**
	 * @return the convertToLab
	 */
	public boolean isConvertToLab() {
		return m_convertToLab;
	}

	/**
	 * @return the addFirstDerivative
	 */
	public boolean isAddFirstDerivative() {
		return m_addFirstDerivative;
	}

	/**
	 * @return the useAbsoluteFirstDerivative
	 */
	public boolean isUseAbsoluteFirstDerivative() {
		return m_useAbsoluteFirstDerivative;
	}

	/**
	 * @return the addSecondDerivative
	 */
	public boolean isAddSecondDerivative() {
		return m_addSecondDerivative;
	}

	/**
	 * @return the useAbsoluteSecondDerivative
	 */
	public boolean isUseAbsoluteSecondDerivative() {
		return m_useAbsoluteSecondDerivative;
	}

	/**
	 * @return the addHoG
	 */
	public boolean isAddHoG() {
		return m_addHoG;
	}

	/**
	 * @return the hogNumBins
	 */
	public int getHogNumBins() {
		return m_hogNumBins;
	}

	/**
	 * @return the applyMinMax
	 */
	public boolean isApplyMinMax() {
		return m_applyMinMax;
	}

	/**
	 * @return the useAbsoluteValues
	 */
	public boolean isUseAbsoluteValues() {
		return m_useAbsoluteValues;
	}

	/**
	 * @param convertToLab the convertToLab to set
	 */
	public void setConvertToLab(boolean convertToLab) {
		this.m_convertToLab = convertToLab;
	}

	/**
	 * @param addFirstDerivative the addFirstDerivative to set
	 */
	public void setAddFirstDerivative(boolean addFirstDerivative) {
		this.m_addFirstDerivative = addFirstDerivative;
	}

	/**
	 * @param useAbsoluteFirstDerivative the useAbsoluteFirstDerivative to set
	 */
	public void setUseAbsoluteFirstDerivative(boolean useAbsoluteFirstDerivative) {
		this.m_useAbsoluteFirstDerivative = useAbsoluteFirstDerivative;
	}

	/**
	 * @param addSecondDerivative the addSecondDerivative to set
	 */
	public void setAddSecondDerivative(boolean addSecondDerivative) {
		this.m_addSecondDerivative = addSecondDerivative;
	}

	/**
	 * @param useAbsoluteSecondDerivative the useAbsoluteSecondDerivative to set
	 */
	public void setUseAbsoluteSecondDerivative(boolean useAbsoluteSecondDerivative) {
		this.m_useAbsoluteSecondDerivative = useAbsoluteSecondDerivative;
	}

	/**
	 * @param addHoG the addHoG to set
	 */
	public void setAddHoG(boolean addHoG) {
		this.m_addHoG = addHoG;
	}

	/**
	 * @param hogNumBins the hogNumBins to set
	 */
	public void setHogNumBins(int hogNumBins) {
		this.m_hogNumBins = hogNumBins;
	}

	/**
	 * @param applyMinMax the applyMinMax to set
	 */
	public void setApplyMinMax(boolean applyMinMax) {
		this.m_applyMinMax = applyMinMax;
	}

	/**
	 * @param useAbsoluteValues the useAbsoluteValues to set
	 */
	public void setUseAbsoluteValues(boolean useAbsoluteValues) {
		this.m_useAbsoluteValues = useAbsoluteValues;
	}

	public boolean isColorImage() {
		return m_isColorImage;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (m_addFirstDerivative ? 1231 : 1237);
		result = prime * result + (m_addHoG ? 1231 : 1237);
		result = prime * result + (m_addSecondDerivative ? 1231 : 1237);
		result = prime * result + (m_applyMinMax ? 1231 : 1237);
		result = prime * result + (m_convertToLab ? 1231 : 1237);
		result = prime * result + m_hogNumBins;
		result = prime * result + (m_useAbsoluteFirstDerivative ? 1231 : 1237);
		result = prime * result + (m_useAbsoluteSecondDerivative ? 1231 : 1237);
		result = prime * result + (m_useAbsoluteValues ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof FeatureDescriptor)) {
			return false;
		}
		FeatureDescriptor<?> other = (FeatureDescriptor<?>) obj;
		if (m_addFirstDerivative != other.m_addFirstDerivative) {
			return false;
		}
		if (m_addHoG != other.m_addHoG) {
			return false;
		}
		if (m_addSecondDerivative != other.m_addSecondDerivative) {
			return false;
		}
		if (m_applyMinMax != other.m_applyMinMax) {
			return false;
		}
		if (m_convertToLab != other.m_convertToLab) {
			return false;
		}
		if (m_hogNumBins != other.m_hogNumBins) {
			return false;
		}
		if (m_useAbsoluteFirstDerivative != other.m_useAbsoluteFirstDerivative) {
			return false;
		}
		if (m_useAbsoluteSecondDerivative != other.m_useAbsoluteSecondDerivative) {
			return false;
		}
		if (m_useAbsoluteValues != other.m_useAbsoluteValues) {
			return false;
		}
		return true;
	}

}

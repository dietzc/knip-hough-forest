package org.knime.knip.hough.features;

import java.util.List;

import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.gradient.PartialDerivative;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

/**
 * Feature calculating first and second derivatives.
 * 
 * @author Simon Schmid, University of Konstanz
 */
final class DerivativesFeature implements Feature {

	private final boolean m_addSecondDerivative;

	private final boolean m_useAbsoluteValueFirst;

	private final boolean m_useAbsoluteValueSecond;

	private final Converter<FloatType, FloatType> m_converterToAbsoluteValues;

	public DerivativesFeature(final boolean addSecondDerivative, final boolean useAbsoluteValueFirst,
			final boolean useAbsoluteValueSecond) {
		m_addSecondDerivative = addSecondDerivative;
		m_useAbsoluteValueFirst = useAbsoluteValueFirst;
		m_useAbsoluteValueSecond = useAbsoluteValueSecond;
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

	@Override
	public List<RandomAccessibleInterval<FloatType>> apply(RandomAccessibleInterval<FloatType> in,
			List<RandomAccessibleInterval<FloatType>> featureList, final OpService ops) {
		final RandomAccessibleInterval<FloatType> in2D;
		if (in.numDimensions() > 2) {
			in2D = Views
					.dropSingletonDimensions(Views.interval(in, new long[3], new long[] { in.max(0), in.max(1), 0 }));
		} else {
			in2D = in;
		}
		// add first derivatives to the list
		final RandomAccessibleInterval<FloatType> firstDerivative0 = ops.create().img(in2D, new FloatType());
		PartialDerivative.gradientCentralDifference(Views.extendMirrorSingle(in2D), firstDerivative0, 0);
		final RandomAccessibleInterval<FloatType> firstDerivative1 = ops.create().img(in2D, new FloatType());
		PartialDerivative.gradientCentralDifference(Views.extendMirrorSingle(in2D), firstDerivative1, 1);

		if (m_useAbsoluteValueFirst) {
			featureList.add(Converters.convert(firstDerivative0, m_converterToAbsoluteValues, new FloatType()));
			featureList.add(Converters.convert(firstDerivative1, m_converterToAbsoluteValues, new FloatType()));
		} else {
			featureList.add(firstDerivative0);
			featureList.add(firstDerivative1);
		}

		if (m_addSecondDerivative) {
			// add second derivatives to the list
			final RandomAccessibleInterval<FloatType> secondDerivative0 = ops.create().img(in2D, new FloatType());
			PartialDerivative.gradientCentralDifference(Views.extendMirrorDouble(firstDerivative0), secondDerivative0,
					0);
			final RandomAccessibleInterval<FloatType> secondDerivative1 = ops.create().img(in2D, new FloatType());
			PartialDerivative.gradientCentralDifference(Views.extendMirrorDouble(firstDerivative1), secondDerivative1,
					1);

			if (m_useAbsoluteValueSecond) {
				featureList.add(Converters.convert(secondDerivative0, m_converterToAbsoluteValues, new FloatType()));
				featureList.add(Converters.convert(secondDerivative1, m_converterToAbsoluteValues, new FloatType()));
			} else {
				featureList.add(secondDerivative0);
				featureList.add(secondDerivative1);
			}
		}
		return featureList;
	}

}

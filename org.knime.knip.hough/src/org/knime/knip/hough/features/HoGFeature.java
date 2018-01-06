package org.knime.knip.hough.features;

import java.util.List;

import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

/**
 * Feature calculating a Histogram of Oriented Gradients.
 * 
 * @author Simon Schmid, University of Konstanz
 */
final class HoGFeature implements Feature {

	private final int m_numBins;

	public HoGFeature(final int numBins) {
		m_numBins = numBins;
	}

	@Override
	public List<RandomAccessibleInterval<FloatType>> apply(RandomAccessibleInterval<FloatType> in,
			List<RandomAccessibleInterval<FloatType>> featureList, OpService ops) {
		// compute hog features and add them to the list
		@SuppressWarnings("unchecked")
		final RandomAccessibleInterval<FloatType> hog = (RandomAccessibleInterval<FloatType>) ops
				.op(HistogramOfOrientedGradients2D.class, null, in, m_numBins, 2).calculate();
		for (int i = 0; i < hog.dimension(2); i++) {
			featureList.add(Views.hyperSlice(hog, 2, i));
		}
		return featureList;
	}

}

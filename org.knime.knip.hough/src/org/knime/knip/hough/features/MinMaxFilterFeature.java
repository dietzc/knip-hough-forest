package org.knime.knip.hough.features;

import java.util.ArrayList;
import java.util.List;

import net.imagej.ops.OpService;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

/**
 * Feature applying a min and max filter.
 * 
 * @author Simon Schmid, University of Konstanz
 */
final class MinMaxFilterFeature implements Feature {

	@Override
	public List<RandomAccessibleInterval<FloatType>> apply(RandomAccessibleInterval<FloatType> in,
			List<RandomAccessibleInterval<FloatType>> featureList, OpService ops) {
		// apply a min and max filter
		final List<RandomAccessibleInterval<FloatType>> listMaxMin = new ArrayList<>();
		final RectangleShape shape = new RectangleShape(2, false);
		for (final RandomAccessibleInterval<FloatType> element : featureList) {
			// min
			final RandomAccessibleInterval<FloatType> min = ops.create()
					.img(new FinalInterval(in.dimension(0), in.dimension(1)), new FloatType());
			ops.filter().min(Views.flatIterable(min), element, shape);
			listMaxMin.add(min);
			// max
			final RandomAccessibleInterval<FloatType> max = ops.create()
					.img(new FinalInterval(in.dimension(0), in.dimension(1)), new FloatType());
			ops.filter().max(Views.flatIterable(max), element, shape);
			listMaxMin.add(max);
		}
		return listMaxMin;
	}

}

package org.knime.knip.hough.features;

import java.util.List;

import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;

/**
 * Feature which can be added to the feature descriptor.
 * 
 * @author Simon Schmid, University of Konstanz
 */
interface Feature {

	List<RandomAccessibleInterval<FloatType>> apply(RandomAccessibleInterval<FloatType> in,
			List<RandomAccessibleInterval<FloatType>> featureList, OpService ops);
}

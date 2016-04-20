/*
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2015 Tobias Pietzsch, Stephan Preibisch, Barry DeZonia,
 * Stephan Saalfeld, Curtis Rueden, Albert Cardona, Christian Dietz, Jean-Yves
 * Tinevez, Johannes Schindelin, Jonathan Hale, Lee Kamentsky, Larry Lindsey, Mark
 * Hiner, Michael Zinsmaier, Martin Horn, Grant Harris, Aivar Grislis, John
 * Bogovic, Steffen Jaensch, Stefan Helfrich, Jan Funke, Nick Perry, Mark Longair,
 * Melissa Linkert and Dimiter Prodanov.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.knime.knip.hough.grid;

import net.imglib2.RandomAccessibleInterval;

/**
 * Creates a grid of patches of a {@link RandomAccessibleInterval}.
 * 
 * @author Simon Schmid, University of Konstanz
 */
public final class Grids {

	/**
	 * Creates a grid of patches. It's origin is placed at the min of the source image, i.e. at the left top of a 2D
	 * image with a distance of half a patch size (rounded down) to the borders.
	 * 
	 * @param srcImage input/source image
	 * @param gap distance/gap between the start of two neighbored patches, must be non-negative
	 * @param patchSize size of each patch in each dimension. If <= 0, the srcImage will not be divided in this
	 *            dimension, but taken completely, e.g. if [5,5,-1], patch dimensions will be [5,5,z] where z is
	 *            srcImage.dimension(2).
	 */
	public static <T> Grid<T> createGrid(RandomAccessibleInterval<T> srcImage, long[] gap, long[] patchSize) {
		final int srcImgNumDims = srcImage.numDimensions();
		assert gap.length == srcImgNumDims;
		assert patchSize.length == srcImgNumDims;

		final long[] gridDims = new long[srcImgNumDims];
		final long[] origin = new long[srcImgNumDims];

		long[] span = new long[patchSize.length];
		boolean[] skipCenter = new boolean[patchSize.length];
		for (int i = 0; i < patchSize.length; i++) {
			skipCenter[i] = patchSize[i] % 2 == 1;
			span[i] = patchSize[i] <= 0 ? -1 : patchSize[i] / 2;
		}

		for (int i = 0; i < srcImgNumDims; i++) {
			origin[i] = span[i];
			if (span[i] < 0) {
				gridDims[i] = 1;
			} else if (gap[i] == 0) {
				gridDims[i] = 1;
			} else {
				if (gap[i] < 0) {
					throw new IllegalArgumentException("Gap must not be negative!");
				}
				gridDims[i] = Math.max(1, ((srcImage.dimension(i) - patchSize[i]) / gap[i]) + 1);
			}

		}
		return new Grid<>(srcImage, gap, span, skipCenter, origin, gridDims);
	}
}

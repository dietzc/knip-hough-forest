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

import net.imglib2.AbstractInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;

/**
 * A grid of patches. Its patches can be accessed by a {@link GridRandomAccess}.
 * 
 * @author Simon Schmid, University of Konstanz
 */
public final class Grid<T> extends AbstractInterval implements RandomAccessibleInterval<RandomAccessibleInterval<T>> {

	private final RandomAccessibleInterval<T> srcImage;
	private final long[] gap;
	private final long[] span;
	private final long[] origin;
	private final boolean[] skipCenter;

	/**
	 * 
	 * @param srcImage input/source image as {@link RandomAccessibleInterval}
	 * @param gap distance/gap between the mid point of two patches, must be non-negative
	 * @param span span of the neighborhood of each patch, i.e. if skipCenter false, <code>size[d] of patch =
	 *            2 * span[d]</code> or size[d], and if skipCenter true, <code>size[d] of patch = 2 * span[d] + 1</code>
	 * @param skipCenter defines whether the center pixel of the patch should be skipped during calculation of patch
	 *            size or nor
	 * @param origin origin of the grid is at the left top (in 2D case)
	 * @param gridDims dimensions of the grid
	 */
	public Grid(RandomAccessibleInterval<T> srcImage, long[] gap, long[] span, boolean[] skipCenter, long[] origin,
			long[] gridDims) {
		super(gridDims);
		for (long g : gap) {
			assert g >= 0;
		}
		this.srcImage = srcImage;
		this.gap = gap;
		this.span = span;
		this.skipCenter = skipCenter;
		this.origin = origin;
	}

	@Override
	public RandomAccess<RandomAccessibleInterval<T>> randomAccess() {
		return new GridRandomAccess<>(this);
	}

	@Override
	public RandomAccess<RandomAccessibleInterval<T>> randomAccess(Interval arg0) {
		return randomAccess();
	}

	/**
	 * @return the srcImage
	 */
	public RandomAccessibleInterval<T> getSrcImage() {
		return srcImage;
	}

	/**
	 * @return the gap
	 */
	public long[] getGap() {
		return gap;
	}

	/**
	 * @return the span
	 */
	public long[] getSpan() {
		return span;
	}

	/**
	 * @return the origin
	 */
	public long[] getOrigin() {
		return origin;
	}

	/**
	 * @return the skipCenter
	 */
	public boolean[] skipCenter() {
		return skipCenter;
	}

}

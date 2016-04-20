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

import net.imglib2.FinalInterval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Sampler;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

/**
 * The {@link RandomAccess} to access a {@link Grid}.
 * 
 * @author Simon Schmid, University of Konstanz
 */
public final class GridRandomAccess<T> extends Point implements RandomAccess<RandomAccessibleInterval<T>> {

	private final Grid<T> grid;
	private final long[] gridDims; // dimensions of the grid

	private GridRandomAccess(final GridRandomAccess<T> gridRA) {
		super(new long[gridRA.grid.getOrigin().length]);
		grid = gridRA.grid;
		gridDims = gridRA.gridDims.clone();
	}

	/**
	 * Creates a {@link RandomAccess} for a {@link Grid}.
	 * 
	 * @param grid the {@link Grid}
	 */
	public GridRandomAccess(final Grid<T> grid) {
		super(new long[grid.getOrigin().length]);
		gridDims = new long[grid.getSrcImage().numDimensions()];
		grid.dimensions(gridDims);
		this.grid = grid;
	}

	@Override
	public Sampler<RandomAccessibleInterval<T>> copy() {
		return this.copy();
	}

	@Override
	public RandomAccessibleInterval<T> get() {
		// check if position is inside of the interval
		for (int i = 0; i < grid.getOrigin().length; i++) {
			if ((getLongPosition(i) < 0) || ((grid.getGap()[i] > 0) && (getLongPosition(i) >= gridDims[i])))
				throw new IndexOutOfBoundsException("Position is out of bounds!");
		}

		// compute the actual position of the patch in the coordinates of the
		// source image
		final long[] patchPos = new long[grid.getOrigin().length];
		for (int i = 0; i < grid.getOrigin().length; i++) {
			patchPos[i] = grid.getOrigin()[i] + getLongPosition(i) * grid.getGap()[i];
		}

		// compute min and max of the interval of the patch
		final long[] min = new long[patchPos.length];
		final long[] max = new long[patchPos.length];
		for (int i = 0; i < patchPos.length; i++) {
			if (grid.getSpan()[i] < 0) {
				min[i] = grid.getSrcImage().min(i);
				max[i] = grid.getSrcImage().max(i);
			} else {
				min[i] = patchPos[i] - grid.getSpan()[i];
				if (grid.skipCenter()[i]) {
					max[i] = patchPos[i] + grid.getSpan()[i];
				} else {
					max[i] = patchPos[i] + grid.getSpan()[i] - 1;
				}
			}
		}

		// only extend if necessary
		if (Intervals.contains(grid.getSrcImage(), new FinalInterval(min, max)))
			return Views.interval(grid.getSrcImage(), min, max);
		return Views.interval(Views.extendMirrorSingle(grid.getSrcImage()), min, max);
	}

	@Override
	public RandomAccess<RandomAccessibleInterval<T>> copyRandomAccess() {
		return new GridRandomAccess<>(this);
	}

}

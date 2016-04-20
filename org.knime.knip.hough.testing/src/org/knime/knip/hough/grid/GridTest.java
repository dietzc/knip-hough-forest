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
package org.knime.knip.hough.grid;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Random;

import org.junit.Test;
import org.knime.knip.core.KNIPGateway;

import net.imagej.ops.OpService;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.DoubleType;

/**
 * Testcases for {@link Grids}, {@link Grid} and {@link GridRandomAccess}.
 *
 * @author Simon Schmid, University of Konstanz
 */
public class GridTest {

    /**
     * Test for 2D.
     */
    @Test
    public void test2D() {

        final OpService ops = KNIPGateway.ops();
        final Random random = new Random();
        for (int h = 0; h < 100; h++) {
            final RandomAccessibleInterval<DoubleType> img =
                ops.create().img(new int[]{random.nextInt(300) + 100, random.nextInt(30) + 1});

            long[] gap = new long[]{random.nextInt(20) + 1, random.nextInt(5)};
            long[] patchSize = new long[]{random.nextInt(20) - 2, random.nextInt(8) - 2};
            //                        final RandomAccessibleInterval<DoubleType> img = ops.create().img(new int[]{226, 24});
            //
            //                        long[] gap = new long[]{17, 3};
            //                        long[] patchSize = new long[]{-1, 4};
            //            System.out.println(img.dimension(0) + ", " + img.dimension(1) + ", " + gap[0] + ", " + gap[1] + ", "
            //                + patchSize[0] + ", " + patchSize[1]);

            final Grid<DoubleType> grid = Grids.createGrid(img, gap, patchSize);

            for (int i = 0; i < grid.numDimensions(); i++) {
                assertThat(grid.min(i), is(0L));
                if (patchSize[i] > 0) {
                    if (gap[i] == 0) {
                        assertThat(grid.dimension(i), is(1L));
                    } else {
                        assertThat(grid.dimension(i), is(Math.max(1, (img.dimension(i) - patchSize[i]) / gap[i] + 1)));
                    }
                } else {
                    assertThat(grid.dimension(i), is(1L));
                }
            }

            final RandomAccess<RandomAccessibleInterval<DoubleType>> randomAccess = grid.randomAccess();
            for (int x = 0; x < grid.dimension(0); x++) {
                for (int y = 0; y < grid.dimension(1); y++) {
                    randomAccess.setPosition(new int[]{x, y});
                    RandomAccessibleInterval<DoubleType> patch = randomAccess.get();
                    // x
                    if (patchSize[0] > 0) {
                        assertThat(patch.min(0), is(x * gap[0]));
                        assertThat(patch.max(0), is(patch.min(0) + patchSize[0] - 1));
                    } else {
                        assertThat(patch.min(0), is(img.min(0)));
                        assertThat(patch.max(0), is(img.max(0)));
                    }
                    // y
                    if (patchSize[1] > 0) {
                        assertThat(patch.min(1), is(y * gap[1]));
                        assertThat(patch.max(1), is(patch.min(1) + patchSize[1] - 1));
                    } else {
                        assertThat(patch.min(1), is(img.min(1)));
                        assertThat(patch.max(1), is(img.max(1)));
                    }
                }
            }
        }
    }
}

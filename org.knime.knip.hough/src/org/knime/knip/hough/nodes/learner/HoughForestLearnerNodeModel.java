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
package org.knime.knip.hough.nodes.learner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.core.KNIPGateway;
import org.knime.knip.hough.features.FeatureDescriptor;
import org.knime.knip.hough.forest.HoughForest;
import org.knime.knip.hough.forest.node.Node;
import org.knime.knip.hough.forest.node.SplitNode;
import org.knime.knip.hough.forest.training.LearnerEntangled;
import org.knime.knip.hough.forest.training.SampleTrainingObject;
import org.knime.knip.hough.forest.training.TrainingObject;
import org.knime.knip.hough.grid.Grid;
import org.knime.knip.hough.grid.Grids;
import org.knime.knip.hough.nodes.evaluator.HoughForestEvaluator;
import org.knime.knip.hough.ports.HoughForestModelPortObject;
import org.knime.knip.hough.ports.HoughForestModelPortObjectSpec;

import net.imagej.ImgPlus;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * The node model of the node which learns a hough forest.
 * 
 * @author Simon Schmid, University of Konstanz
 */
final class HoughForestLearnerNodeModel<T extends RealType<T>, L> extends NodeModel {
	private HoughForestLearnerConfig m_config;

	private double[] m_thresholds;

	/**
	 * Table in, model out.
	 */
	public HoughForestLearnerNodeModel() {
		super(new PortType[] { BufferedDataTable.TYPE }, new PortType[] { HoughForestModelPortObject.TYPE });
	}

	private int fetchImgColIdx(final DataTableSpec spec) throws InvalidSettingsException {
		if (m_config.getColImage() != null && !m_config.getColImage().isEmpty()) {
			final int imgIdx = spec.findColumnIndex(m_config.getColImage());
			if (imgIdx < 0) {
				throw new InvalidSettingsException(
						"Image column '" + m_config.getColImage() + "' not found in the input table!");
			}
			return imgIdx;
		} else {
			throw new InvalidSettingsException("An image column must be selected!");
		}
	}

	private int fetchLabelingColIdx(final DataTableSpec spec) throws InvalidSettingsException {
		if (m_config.getColLabel() != null && !m_config.getColLabel().isEmpty()) {
			final int labelIdx = spec.findColumnIndex(m_config.getColLabel());
			if (labelIdx < 0) {
				throw new InvalidSettingsException(
						"Labeling column '" + m_config.getColLabel() + "' not found in the input table!");
			}
			return labelIdx;
		} else {
			throw new InvalidSettingsException("A labeling column must be selected!");
		}
	}

	/** {@inheritDoc} */
	@SuppressWarnings("unchecked")
	@Override
	protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
		final BufferedDataTable table = (BufferedDataTable) inObjects[0];
		if (table.size() < 1) {
			throw new IllegalArgumentException("The table must not be empty!");
		}
		// Get the image and labeling column
		final DataTableSpec spec = ((BufferedDataTable) inObjects[0]).getSpec();
		final int imgIdx = fetchImgColIdx(spec);
		final int labelIdx = fetchLabelingColIdx(spec);

		// Set progress to zero, so that in the parallel threads the progress can still be added
		exec.setProgress(0);
		exec.setProgress("Extracting patches...");

		// Parallelization stuff
		final ExecutorService es = KNIPGateway.threads().getExecutorService();
		final List<ExtractParallel> threads = new ArrayList<>((int) table.size());

		/*
		 * Patch Extraction
		 */
		getLogger().infoWithFormat("Extract patches of %d images...", table.size());
		final long[] patchGap = new long[] { m_config.getPatchGapX(), m_config.getPatchGapY(), 0 };
		final long[] patchsize = new long[] { m_config.getPatchWidth(), m_config.getPatchHeight(), -1 };
		final List<TrainingObject<FloatType>> trainingObjects = new ArrayList<>();

		// check dimensionality of the image in the first row
		final CloseableRowIterator rowIterator = table.iterator();
		boolean isColorImage = false;
		if (rowIterator.hasNext()) {
			final DataRow row = rowIterator.next();
			if (row.getCell(imgIdx).isMissing()) {
				throw new IllegalArgumentException("The first row contains a missing cell!");
			}
			final ImgPlus<T> image = ((ImgPlusValue<T>) row.getCell(imgIdx)).getImgPlus();
			if (image.numDimensions() != 2 && !(image.numDimensions() == 3 && image.dimension(2) == 3)) {
				throw new IllegalArgumentException("The images must be either 2D or 3D with three channels!");
			}
			isColorImage = image.numDimensions() == 3;
		}
		rowIterator.close();

		final FeatureDescriptor<T> featureDescriptor = new FeatureDescriptor<>(isColorImage, m_config.getConvertToLab(),
				m_config.getFirstDerivative(), m_config.getUseAbsoluteFirstDerivative(), m_config.getSecondDerivative(),
				m_config.getUseAbsoluteSecondDerivative(), m_config.getHog(), m_config.getHogNumBins(),
				m_config.getApplyMinMax(), m_config.getUseAbsolute());

		final int batchSize = (int) (table.size() / Runtime.getRuntime().availableProcessors()) + 1;
		final double progressStepSize = 0.5 / table.size();
		final ArrayList<ImgPlus<T>> listImg = new ArrayList<>(batchSize);
		final ArrayList<RandomAccessibleInterval<LabelingType<L>>> listLabelings = new ArrayList<>(batchSize);
		for (final DataRow row : table) {
			if (row.getCell(imgIdx).isMissing() || row.getCell(labelIdx).isMissing()) {
				throw new IllegalArgumentException("Row '" + row.getKey() + "' contains a missing cell!");
			}
			final ImgPlus<T> imgPlus = ((ImgPlusValue<T>) row.getCell(imgIdx)).getImgPlus();
			listImg.add(imgPlus);
			listLabelings.add(((LabelingValue<L>) row.getCell(labelIdx)).getLabeling());
			if (listImg.size() >= batchSize) {
				threads.add(new ExtractParallel((List<ImgPlus<T>>) listImg.clone(),
						(List<RandomAccessibleInterval<LabelingType<L>>>) listLabelings.clone(), featureDescriptor,
						patchGap, patchsize, exec, progressStepSize));
				listImg.clear();
				listLabelings.clear();
			}
		}
		if (listImg.size() > 0) {
			threads.add(new ExtractParallel(listImg, listLabelings, featureDescriptor, patchGap, patchsize, exec,
					progressStepSize));
		}
		try {
			final List<Future<List<TrainingObject<FloatType>>>> invokeAll = es.invokeAll(threads);
			for (final Future<List<TrainingObject<FloatType>>> future : invokeAll)
				trainingObjects.addAll(future.get());
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}
		es.shutdown();
		m_config.setThresholds(m_thresholds);
		getLogger().infoWithFormat("%d patches extracted.", trainingObjects.size());

		/*
		 * Forest Training
		 */
		exec.setProgress("Learning trees...");
		getLogger().info("Train hough forest...");
		final long seed = m_config.getUseSeed() ? m_config.getSeed() : System.currentTimeMillis();
		final List<SplitNode> trees;
		// if (m_config.getEntanglement()) {
		trees = LearnerEntangled.trainForest(new SampleTrainingObject<>(trainingObjects), m_config, exec, seed);
		// } else {
		// trees = Learner.trainForest(new SampleTrainingObject<>(trainingObjects), m_config, exec, seed);
		// }
		final HoughForest forest = new HoughForest(trees, patchsize, featureDescriptor);

		if (forest.getListOfTrees().isEmpty()) {
			throw new IllegalStateException("Learned Hough Forest has no trees!");
		}

		Node[][] nodes = trainingObjects.get(0).getNodeGrid()[0];
		StringBuilder stringBuilder = new StringBuilder();
		for (int i = 0; i < nodes[0].length; i++) {
			for (int j = 0; j < nodes.length; j++) {
				stringBuilder.append(String.format("%2d ", nodes[j][i].getNodeIdx()));
			}
			stringBuilder.append("\n");
		}
		System.out.println(stringBuilder.toString());
		HoughForestEvaluator.printTree(trees.get(0), "", true);
		return new PortObject[] { new HoughForestModelPortObject(forest, seed) };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
		if (m_config == null) {
			m_config = new HoughForestLearnerConfig();
		}
		fetchImgColIdx((DataTableSpec) inSpecs[0]);
		fetchLabelingColIdx((DataTableSpec) inSpecs[0]);
		return new PortObjectSpec[] { new HoughForestModelPortObjectSpec() };
	}

	@Override
	protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// no internals

	}

	@Override
	protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// no internals
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		if (m_config != null) {
			m_config.saveSettingsTo(settings);
		}
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		if (m_config != null) {
			m_config.validateSettings(settings);
		}
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		if (m_config == null) {
			m_config = new HoughForestLearnerConfig();
		}
		m_config.loadValidatedSettingsFrom(settings);
	}

	@Override
	protected void reset() {
		m_thresholds = null;
	}

	private double[] getMins(final RandomAccessibleInterval<FloatType> apply) {
		final double[] mins = new double[(int) apply.dimension(2)];
		for (int i = 0; i < apply.dimension(2); i++) {
			final IntervalView<FloatType> slice = Views.hyperSlice(apply, 2, i);
			final Cursor<FloatType> cursor = slice.cursor();
			double min = Long.MAX_VALUE;
			while (cursor.hasNext()) {
				cursor.fwd();
				final double currentValue = cursor.get().getRealDouble();
				if (currentValue < min) {
					min = currentValue;
				}
			}
			mins[i] = min;
		}
		return mins;
	}

	private double[] getMaxs(final RandomAccessibleInterval<FloatType> img) {
		final double[] maxs = new double[(int) img.dimension(2)];
		for (int i = 0; i < img.dimension(2); i++) {
			final IntervalView<FloatType> slice = Views.hyperSlice(img, 2, i);
			final Cursor<FloatType> cursor = slice.cursor();
			double max = Long.MIN_VALUE;
			while (cursor.hasNext()) {
				cursor.fwd();
				final double currentValue = cursor.get().getRealDouble();
				if (currentValue > max) {
					max = currentValue;
				}
			}
			maxs[i] = max;
		}
		return maxs;
	}

	private synchronized void setThresholds(final double[] mins, final double[] maxs) {
		assert mins.length == maxs.length;
		if (m_thresholds == null) {
			m_thresholds = new double[maxs.length];
			for (int i = 0; i < maxs.length; i++) {
				m_thresholds[i] = Long.MIN_VALUE;
			}
		}
		assert m_thresholds.length == maxs.length;
		for (int i = 0; i < m_thresholds.length; i++) {
			final double d = maxs[i] - mins[i];
			if (d > m_thresholds[i]) {
				m_thresholds[i] = d;
			}
		}
	}

	private final class ExtractParallel implements Callable<List<TrainingObject<FloatType>>> {

		private final List<ImgPlus<T>> m_images;
		private final List<RandomAccessibleInterval<LabelingType<L>>> m_labelings;
		private final long[] m_patchGap;
		private final long[] m_patchSize;
		private final ExecutionContext m_exec;
		private final double m_progress;
		private final FeatureDescriptor<T> m_featureDescriptor;

		private ExtractParallel(final List<ImgPlus<T>> images,
				final List<RandomAccessibleInterval<LabelingType<L>>> labelings,
				final FeatureDescriptor<T> featureDescriptor, final long[] patchGap, final long[] patchSize,
				final ExecutionContext exec, final double progressStepSize) {
			assert images.size() == labelings.size();
			m_images = images;
			m_labelings = labelings;
			this.m_featureDescriptor = featureDescriptor;
			this.m_patchGap = patchGap;
			this.m_patchSize = patchSize;
			this.m_exec = exec;
			this.m_progress = progressStepSize;
		}

		@Override
		public List<TrainingObject<FloatType>> call() throws Exception {
			final List<TrainingObject<FloatType>> allTObjects = new ArrayList<>();
			while (!m_images.isEmpty()) {
				final ImgPlus<T> img = m_images.remove(0);
				final RandomAccessibleInterval<LabelingType<L>> labeling = m_labelings.remove(0);
				if (img.numDimensions() != 2 && !(img.numDimensions() == 3 && img.dimension(2) == 3)) {
					throw new IllegalArgumentException("The images must be either 2D or 3D with three channels!");
				}
				final boolean isColorImage = img.numDimensions() == 3;
				if (!(isColorImage && m_featureDescriptor.isColorImage())
						&& !(!isColorImage && !m_featureDescriptor.isColorImage())) {
					throw new IllegalArgumentException("All input images must be either color or grayscale!");
				}
				if (img.dimension(0) != labeling.dimension(0) || img.dimension(1) != labeling.dimension(1)) {
					throw new IllegalArgumentException(
							"The image and labeling must have the same size in the first two dimensions!");
				}
				m_exec.checkCanceled();
				final RandomAccessibleInterval<FloatType> featureImg = m_featureDescriptor.apply(img);
				setThresholds(getMins(featureImg), getMaxs(featureImg));
				final Grid<FloatType> grid = Grids.createGrid(featureImg, m_patchGap, m_patchSize);
				@SuppressWarnings("rawtypes")
				final TrainingObject[][] trainingObjectGrid = new TrainingObject[(int) grid.dimension(0)][(int) grid
						.dimension(1)];
				final Node[][][] nodeGrid = new Node[m_config
						.getNumTrees()][(int) grid.dimension(0)][(int) grid.dimension(1)];
				final RandomAccess<RandomAccessibleInterval<FloatType>> raGrid = grid.randomAccess();
				final LabelRegions<L> labelRegions = new LabelRegions<>(labeling);
				final int numLabels = labelRegions.getExistingLabels().size();
				if (numLabels > 1) {
					throw new IllegalArgumentException("The labeling must contain maximum one label!");
				}
				for (int i = 0; i < grid.dimension(0); i++) {
					for (int j = 0; j < grid.dimension(1); j++) {
						final int[] pos = new int[] { i, j, 0 };
						raGrid.setPosition(pos);
						final RandomAccessibleInterval<FloatType> patch = raGrid.get();
						final Point midOfPatch = new Point(patch.min(0) + patch.dimension(0) / 2,
								patch.min(1) + patch.dimension(1) / 2);
						if (numLabels > 0) {
							final LabelRegion<L> labelRegion = labelRegions
									.getLabelRegion(labelRegions.getExistingLabels().iterator().next());
							// force interval to be 2d
							final FinalInterval interval2D = new FinalInterval(
									new long[] { labelRegion.min(0), labelRegion.min(1) },
									new long[] { labelRegion.max(0), labelRegion.max(1) });
							if (Intervals.contains(interval2D, midOfPatch)) {
								final TrainingObject<FloatType> tObj = new TrainingObject<>(Views.zeroMin(patch), 1,
										new int[] {
												(int) (labelRegion.getCenterOfMass().getFloatPosition(0)
														- midOfPatch.getFloatPosition(0)),
												(int) (labelRegion.getCenterOfMass().getFloatPosition(1)
														- midOfPatch.getFloatPosition(1)) },
										trainingObjectGrid, pos, nodeGrid);
								allTObjects.add(tObj);
								trainingObjectGrid[i][j] = tObj;
								continue;
							}
						}
						final TrainingObject<FloatType> tObj = new TrainingObject<>(Views.zeroMin(patch), 0,
								new int[] {}, trainingObjectGrid, pos, nodeGrid);
						allTObjects.add(tObj);
						trainingObjectGrid[i][j] = tObj;
					}
				}
				m_exec.setProgress(m_exec.getProgressMonitor().getProgress() + m_progress);
			}
			return allTObjects;
		}

	}
}
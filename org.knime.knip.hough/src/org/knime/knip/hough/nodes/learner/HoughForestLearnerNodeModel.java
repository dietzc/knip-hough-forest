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
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelLong;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.core.KNIPGateway;
import org.knime.knip.hough.features.FeatureDescriptor;
import org.knime.knip.hough.forest.HoughForest;
import org.knime.knip.hough.forest.Learner;
import org.knime.knip.hough.forest.PatchSample;
import org.knime.knip.hough.forest.SplitNode;
import org.knime.knip.hough.forest.TrainingObject;
import org.knime.knip.hough.grid.Grid;
import org.knime.knip.hough.grid.Grids;
import org.knime.knip.hough.ports.HoughForestModelPortObject;
import org.knime.knip.hough.ports.HoughForestModelPortObjectSpec;

import net.imagej.ImgPlus;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

/**
 * The node model of the node which learns a hough forest.
 * 
 * @author Simon Schmid, University of Konstanz
 */
final class HoughForestLearnerNodeModel<T extends RealType<T>, L> extends NodeModel {

	// Input
	private final SettingsModelString m_colImage = createColSelectionModel();
	private final SettingsModelString m_colLabel = createLabelSelectionModel();
	// Tree Options
	private final SettingsModelIntegerBounded m_numSamples = createNumSamplesModel();
	private final SettingsModelIntegerBounded m_numSplitFunctions = createNumSplitFunctionsModel();
	private final SettingsModelDoubleBounded m_threshold = createThresholdModel();
	private final SettingsModelIntegerBounded m_depth = createDepthModel();
	private final SettingsModelIntegerBounded m_minSizeSample = createMinSizeSampleModel();
	// Forest Options
	private final SettingsModelIntegerBounded m_numTrees = createNumTreesModel();
	private final SettingsModelBoolean m_useSeed = createUseSeedBoolModel();
	private final SettingsModelLong m_seed = createSeedLongModel();
	// Feature Selection
	private final SettingsModelBoolean m_convertToLab = createConvertToLabModel();
	private final SettingsModelBoolean m_firstDerivative = createFistDerivModel();
	private final SettingsModelBoolean m_useAbsoluteFirstDerivative = createUseAbsoluteFistDerivModel();
	private final SettingsModelBoolean m_secondDerivative = createSecondDerivModel();
	private final SettingsModelBoolean m_useAbsoluteSecondDerivative = createUseAbsoluteSecondDerivModel();
	private final SettingsModelBoolean m_hog = createHogBoolModel();
	private final SettingsModelInteger m_hogNumBins = createHogNumbBinsModel();
	private final SettingsModelBoolean m_applyMinMax = createApplyMinMaxModel();
	private final SettingsModelBoolean m_useAbsolute = createUseAbsoluteModel();
	// Patch Extraction
	private final SettingsModelIntegerBounded m_patchGapX = createPatchGapXModel();
	private final SettingsModelIntegerBounded m_patchGapY = createPatchGapYModel();
	private final SettingsModelIntegerBounded m_patchWidth = createPatchWidthModel();
	private final SettingsModelIntegerBounded m_patchHeight = createPatchHeightModel();

	private final SettingsModel[] m_listSettingsModels = { m_colImage, m_colLabel, m_numSamples, m_numSplitFunctions,
			m_threshold, m_depth, m_minSizeSample, m_numTrees, m_useSeed, m_seed, m_convertToLab, m_firstDerivative,
			m_useAbsoluteFirstDerivative, m_secondDerivative, m_useAbsoluteSecondDerivative, m_hog, m_hogNumBins,
			m_applyMinMax, m_useAbsolute, m_patchWidth, m_patchHeight, m_patchGapX, m_patchGapY };

	static SettingsModelString createColSelectionModel() {
		return new SettingsModelString("image_column", "");
	}

	static SettingsModelString createLabelSelectionModel() {
		return new SettingsModelString("label_column", "");
	}

	static SettingsModelIntegerBounded createNumTreesModel() {
		return new SettingsModelIntegerBounded("num_trees", 8, 1, Integer.MAX_VALUE);
	}

	static SettingsModelBoolean createUseSeedBoolModel() {
		return new SettingsModelBoolean("use_seed", true);
	}

	static SettingsModelLong createSeedLongModel() {
		return new SettingsModelLong("seed", System.currentTimeMillis());
	}

	static SettingsModelIntegerBounded createNumSamplesModel() {
		return new SettingsModelIntegerBounded("num_samples", 10000, 1, Integer.MAX_VALUE);
	}

	static SettingsModelIntegerBounded createNumSplitFunctionsModel() {
		return new SettingsModelIntegerBounded("num_split_functions", 10000, 1, Integer.MAX_VALUE);
	}

	static SettingsModelDoubleBounded createThresholdModel() {
		return new SettingsModelDoubleBounded("threshold", 5.0, 0, Integer.MAX_VALUE);
	}

	static SettingsModelIntegerBounded createDepthModel() {
		return new SettingsModelIntegerBounded("tree_depth", 15, 1, Integer.MAX_VALUE);
	}

	static SettingsModelIntegerBounded createMinSizeSampleModel() {
		return new SettingsModelIntegerBounded("min_size_sample", 20, 1, Integer.MAX_VALUE);
	}

	static SettingsModelBoolean createConvertToLabModel() {
		return new SettingsModelBoolean("convert_to_lab", true);
	}

	static SettingsModelBoolean createFistDerivModel() {
		return new SettingsModelBoolean("add_first_deriv", true);
	}

	static SettingsModelBoolean createUseAbsoluteFistDerivModel() {
		return new SettingsModelBoolean("use_absolute_first_deriv", true);
	}

	static SettingsModelBoolean createSecondDerivModel() {
		return new SettingsModelBoolean("add_second_deriv", true);
	}

	static SettingsModelBoolean createUseAbsoluteSecondDerivModel() {
		return new SettingsModelBoolean("use_absolute_second_deriv", true);
	}

	static SettingsModelBoolean createHogBoolModel() {
		return new SettingsModelBoolean("add_hogl", true);
	}

	static SettingsModelInteger createHogNumbBinsModel() {
		return new SettingsModelInteger("hog_num_bins", 9);
	}

	static SettingsModelBoolean createApplyMinMaxModel() {
		return new SettingsModelBoolean("apply_min_max", true);
	}

	static SettingsModelBoolean createUseAbsoluteModel() {
		return new SettingsModelBoolean("use_absolute", false);
	}

	static SettingsModelIntegerBounded createPatchGapXModel() {
		return new SettingsModelIntegerBounded("gap_horizontal", 8, 0, Integer.MAX_VALUE);
	}

	static SettingsModelIntegerBounded createPatchGapYModel() {
		return new SettingsModelIntegerBounded("gap_vertical", 8, 0, Integer.MAX_VALUE);
	}

	static SettingsModelIntegerBounded createPatchWidthModel() {
		return new SettingsModelIntegerBounded("patch_width", 16, 1, Integer.MAX_VALUE);
	}

	static SettingsModelIntegerBounded createPatchHeightModel() {
		return new SettingsModelIntegerBounded("patch_height", 16, 1, Integer.MAX_VALUE);
	}

	/**
	 * Table in, model out.
	 */
	public HoughForestLearnerNodeModel() {
		super(new PortType[] { BufferedDataTable.TYPE }, new PortType[] { HoughForestModelPortObject.TYPE });
	}

	private int fetchImgColIdx(final DataTableSpec spec) throws InvalidSettingsException {
		if (m_colImage.getStringValue() != null && !m_colImage.getStringValue().isEmpty()) {
			final int imgIdx = spec.findColumnIndex(m_colImage.getStringValue());
			if (imgIdx < 0) {
				throw new InvalidSettingsException(
						"Image column '" + m_colImage.getStringValue() + "' not found in the input table!");
			}
			return imgIdx;
		} else {
			throw new InvalidSettingsException("An image column must be selected!");
		}
	}

	private int fetchLabelingColIdx(final DataTableSpec spec) throws InvalidSettingsException {
		if (m_colLabel.getStringValue() != null && !m_colLabel.getStringValue().isEmpty()) {
			final int labelIdx = spec.findColumnIndex(m_colLabel.getStringValue());
			if (labelIdx < 0) {
				throw new InvalidSettingsException(
						"Labeling column '" + m_colLabel.getStringValue() + "' not found in the input table!");
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
		// Get the image and labeling column
		final DataTableSpec spec = ((BufferedDataTable) inObjects[0]).getSpec();
		final int imgIdx = fetchImgColIdx(spec);
		final int labelIdx = fetchLabelingColIdx(spec);

		// Set progress to zero, so that in the parallel threads the progress can still be added
		exec.setProgress(0);

		// Parallelization stuff
		final ExecutorService es = KNIPGateway.threads().getExecutorService();
		final List<ExtractParallel> threads = new ArrayList<>((int) table.size());

		/*
		 * Patch Extraction
		 */
		getLogger().infoWithFormat("Extract patches of %d images...", table.size());
		final long[] patchGap = new long[] { m_patchGapX.getIntValue(), m_patchGapY.getIntValue(), 0 };
		final long[] patchsize = new long[] { m_patchWidth.getIntValue(), m_patchHeight.getIntValue(), -1 };
		final List<TrainingObject<FloatType>> patches = new ArrayList<>();

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

		final FeatureDescriptor<T> featureDescriptor = new FeatureDescriptor<T>(isColorImage,
				m_convertToLab.getBooleanValue(), m_firstDerivative.getBooleanValue(),
				m_useAbsoluteFirstDerivative.getBooleanValue(), m_secondDerivative.getBooleanValue(),
				m_useAbsoluteSecondDerivative.getBooleanValue(), m_hog.getBooleanValue(), m_hogNumBins.getIntValue(),
				m_applyMinMax.getBooleanValue(), m_useAbsolute.getBooleanValue());

		final int batchSize = (int) (table.size() / Runtime.getRuntime().availableProcessors()) + 1;
		final double progressStepSize = 0.5 / table.size();
		ArrayList<ImgPlus<T>> listImg = new ArrayList<>(batchSize);
		ArrayList<RandomAccessibleInterval<LabelingType<L>>> listLabelings = new ArrayList<>(batchSize);
		for (final DataRow row : table) {
			if (row.getCell(imgIdx).isMissing() || row.getCell(labelIdx).isMissing()) {
				throw new IllegalArgumentException("Row '" + row.getKey() + "' contains a missing cell!");
			}
			listImg.add(((ImgPlusValue<T>) row.getCell(imgIdx)).getImgPlus());
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
				patches.addAll(future.get());
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}
		es.shutdown();
		getLogger().infoWithFormat("%d patches extracted.", patches.size());

		/*
		 * Forest Training
		 */
		getLogger().info("Train hough forest...");
		final long seed = m_useSeed.getBooleanValue() ? m_seed.getLongValue() : System.currentTimeMillis();
		final List<SplitNode> trees = Learner.trainForest(new PatchSample<>(patches), m_depth.getIntValue(),
				m_minSizeSample.getIntValue(), m_numSamples.getIntValue(), m_numTrees.getIntValue(),
				m_numSplitFunctions.getIntValue(), m_threshold.getDoubleValue(), exec, seed);
		final HoughForest forest = new HoughForest(trees, patchsize, featureDescriptor);

		if (forest.getListOfTrees().isEmpty()) {
			throw new IllegalStateException("Learned Hough Forest has no trees!");
		}

		return new PortObject[] { new HoughForestModelPortObject(forest, seed) };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
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
		for (final SettingsModel s : m_listSettingsModels) {
			s.saveSettingsTo(settings);
		}
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		for (final SettingsModel s : m_listSettingsModels) {
			s.validateSettings(settings);
		}
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		for (final SettingsModel s : m_listSettingsModels) {
			s.loadSettingsFrom(settings);
		}

		if (m_numTrees.getIntValue() < 1) {
			throw new InvalidSettingsException("The number of trees must be at least 1!");
		}
		if (m_numSamples.getIntValue() < m_minSizeSample.getIntValue()) {
			throw new InvalidSettingsException("The size of the sample must not be lower than the minimum size!");
		}
	}

	@Override
	protected void reset() {
		// nothing to do
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
			final List<TrainingObject<FloatType>> allPatches = new ArrayList<>();
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
							"The image and label must have the same size in the first two dimensions!");
				}
				m_exec.checkCanceled();
				final Grid<FloatType> grid = Grids.createGrid(m_featureDescriptor.apply(img), m_patchGap, m_patchSize);
				final RandomAccess<RandomAccessibleInterval<FloatType>> raGrid = grid.randomAccess();
				final LabelRegions<L> labelRegions = new LabelRegions<>(labeling);
				for (int i = 0; i < grid.dimension(0); i++) {
					for (int j = 0; j < grid.dimension(1); j++) {
						raGrid.setPosition(new int[] { i, j, 0 });
						final RandomAccessibleInterval<FloatType> patch = raGrid.get();
						final Point midOfPatch = new Point(patch.min(0) + patch.dimension(0) / 2,
								patch.min(1) + patch.dimension(1) / 2);
						for (LabelRegion<L> labelRegion : labelRegions) {
							if (Intervals.contains(labelRegion, midOfPatch)) {
								allPatches.add(new TrainingObject<>(Views.zeroMin(patch), 1,
										new int[] {
												(int) (labelRegion.getCenterOfMass().getFloatPosition(0)
														- midOfPatch.getFloatPosition(0)),
												(int) (labelRegion.getCenterOfMass().getFloatPosition(1)
														- midOfPatch.getFloatPosition(1)) }));
							} else {
								allPatches.add(new TrainingObject<>(Views.zeroMin(patch), 0, new int[] {}));
							}
						}
					}
				}
				m_exec.setProgress(m_exec.getProgressMonitor().getProgress() + m_progress);
			}
			return allPatches;
		}

	}
}
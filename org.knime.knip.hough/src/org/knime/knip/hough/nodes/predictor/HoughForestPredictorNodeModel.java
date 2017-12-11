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
package org.knime.knip.hough.nodes.predictor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.append.AppendedColumnRow;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortObjectInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableFunction;
import org.knime.core.node.tableview.TableContentModel;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.base.data.labeling.LabelingCellFactory;
import org.knime.knip.base.nodes.proc.maxfinder.MaximumFinderOp;
import org.knime.knip.core.KNIPGateway;
import org.knime.knip.core.awt.labelingcolortable.DefaultLabelingColorTable;
import org.knime.knip.core.data.img.DefaultLabelingMetadata;
import org.knime.knip.core.util.StringTransformer;
import org.knime.knip.hough.features.FeatureDescriptor;
import org.knime.knip.hough.forest.HoughForest;
import org.knime.knip.hough.forest.PredictionObject;
import org.knime.knip.hough.forest.Predictor;
import org.knime.knip.hough.grid.Grid;
import org.knime.knip.hough.grid.Grids;
import org.knime.knip.hough.ports.HoughForestModelPortObject;

import net.imagej.ImgPlus;
import net.imagej.ops.OpService;
import net.imagej.ops.transform.scaleView.DefaultScaleView;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.IterableInterval;
import net.imglib2.Localizable;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImgView;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.roi.geometric.Polygon;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

/**
 * The node model of the node which makes predictions based on a hough forest.
 * 
 * @author Simon Schmid, University of Konstanz
 */
final class HoughForestPredictorNodeModel<T extends RealType<T>> extends NodeModel implements BufferedDataTableHolder {
	// Input
	private HoughForest m_houghForest;
	private final SettingsModelString m_colImage = createColSelectModel();
	// Patch Extraction
	private final SettingsModelIntegerBounded m_patchGapX = createPatchGapXModel();
	private final SettingsModelIntegerBounded m_patchGapY = createPatchGapYModel();
	// Bounding Box Estimation
	private final SettingsModelIntegerBounded m_spanIntervalBackprojection = createSpanIntervalBackprojectionModel();
	// Multiple Detection
	private final SettingsModelDouble m_thresholdMultipleDetection = createThresholdMultipleDetectionDoubleModel();
	private final SettingsModelDoubleBounded m_sigma = createSigmaModel();
	private final SettingsModelBoolean m_multipleDetection = createMultipleDetectionBoolModel(
			m_thresholdMultipleDetection);
	// Scales
	private final SettingsModelBoolean m_scaleBool1 = createScalesBoolModel(1);
	private final SettingsModelBoolean m_scaleBool2 = createScalesBoolModel(2);
	private final SettingsModelBoolean m_scaleBool3 = createScalesBoolModel(3);
	private final SettingsModelBoolean m_scaleBool4 = createScalesBoolModel(4);
	private final SettingsModelDouble m_scaleValue1 = createScalesDoubleModel(1);
	private final SettingsModelDouble m_scaleValue2 = createScalesDoubleModel(2);
	private final SettingsModelDouble m_scaleValue3 = createScalesDoubleModel(3);
	private final SettingsModelDouble m_scaleValue4 = createScalesDoubleModel(4);
	private double[] m_scales;
	// Output
	private final SettingsModelBoolean m_outputVotes = createOutputVotesBoolModel();
	private final SettingsModelBoolean m_outputMaxima = createOutputMaximaBoolModel();
	private final SettingsModelBoolean m_outputAdvanced = createOutputAdvancedBoolModel();

	private final SettingsModel[] m_listSettingsModels = { m_colImage, m_patchGapX, m_patchGapY,
			m_spanIntervalBackprojection, m_sigma, m_thresholdMultipleDetection, m_multipleDetection, m_scaleBool1,
			m_scaleBool2, m_scaleBool3, m_scaleBool4, m_scaleValue1, m_scaleValue2, m_scaleValue3, m_scaleValue4,
			m_outputVotes, m_outputMaxima, m_outputAdvanced };

	// data table for the table cell view
	private BufferedDataTable m_data;
	private int m_imageColIdx;
	private int m_labelColIdx;

	// Services
	private final OpService m_ops;
	private final ExecutorService m_es;

	static SettingsModelString createColSelectModel() {
		return new SettingsModelString("image_column", "");
	}

	static SettingsModelIntegerBounded createPatchGapXModel() {
		return new SettingsModelIntegerBounded("gap_horizontal", 8, 0, Integer.MAX_VALUE);
	}

	static SettingsModelIntegerBounded createPatchGapYModel() {
		return new SettingsModelIntegerBounded("gap_vertical", 8, 0, Integer.MAX_VALUE);
	}

	static SettingsModelDoubleBounded createSigmaModel() {
		return new SettingsModelDoubleBounded("sigma", 10.0, 0, Double.MAX_VALUE);
	}

	static SettingsModelIntegerBounded createSpanIntervalBackprojectionModel() {
		return new SettingsModelIntegerBounded("span_area_backprojection", 15, 1, Integer.MAX_VALUE);
	}

	static SettingsModelBoolean createMultipleDetectionBoolModel(final SettingsModelDouble thresholdModel) {
		final SettingsModelBoolean settingsModelBoolean = new SettingsModelBoolean("multiple_detection_bool", false);
		settingsModelBoolean.addChangeListener(e -> thresholdModel.setEnabled(settingsModelBoolean.getBooleanValue()));
		return settingsModelBoolean;
	}

	static SettingsModelDouble createThresholdMultipleDetectionDoubleModel() {
		final SettingsModelDouble settingsModelDouble = new SettingsModelDouble("multiple_detection_threshold", 1.0);
		settingsModelDouble.setEnabled(false);
		return settingsModelDouble;
	}

	static SettingsModelBoolean createScalesBoolModel(final int idx) {
		final SettingsModelBoolean settingsModelBoolean = new SettingsModelBoolean("scaleBool" + idx, false);
		if (idx > 1)
			settingsModelBoolean.setEnabled(false);
		return settingsModelBoolean;
	}

	static SettingsModelDouble createScalesDoubleModel(final int idx) {
		final SettingsModelDouble settingsModelDouble = new SettingsModelDouble("scaleValue" + idx, 1);
		settingsModelDouble.setEnabled(false);
		return settingsModelDouble;
	}

	static SettingsModelBoolean createOutputVotesBoolModel() {
		return new SettingsModelBoolean("is_output_votes", false);
	}

	static SettingsModelBoolean createOutputMaximaBoolModel() {
		return new SettingsModelBoolean("is_output_maxima", false);
	}

	static SettingsModelBoolean createOutputAdvancedBoolModel() {
		return new SettingsModelBoolean("is_output_advanced", false);
	}

	public HoughForestPredictorNodeModel() {
		super(new PortType[] { HoughForestModelPortObject.TYPE, BufferedDataTable.TYPE },
				new PortType[] { BufferedDataTable.TYPE });
		// OpService
		m_ops = KNIPGateway.getInstance().ctx().getService(OpService.class);
		// ExecutorService
		m_es = KNIPGateway.threads().getExecutorService();
	}

	/** {@inheritDoc} */
	@Override
	protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
		final DataTableSpec in = (DataTableSpec) inSpecs[1];
		final ColumnRearranger r = createColumnRearranger(in, null);
		final DataTableSpec out = r.createSpec();
		return new DataTableSpec[] { out };
	}

	private DataColumnSpec[] createSpec(final DataTableSpec spec) {
		final UniqueNameGenerator uniqueNameGenerator = new UniqueNameGenerator(spec);
		final ArrayList<DataColumnSpec> specs = new ArrayList<>();
		specs.add(uniqueNameGenerator.newColumn("Prediction", LabelingCell.TYPE));
		if (m_outputVotes.getBooleanValue()) {
			specs.add(uniqueNameGenerator.newColumn("Votes", ImgPlusCell.TYPE));
		}
		if (m_outputMaxima.getBooleanValue()) {
			specs.add(uniqueNameGenerator.newColumn("Maxima", ImgPlusCell.TYPE));
		}
		return specs.toArray(new DataColumnSpec[specs.size()]);
	}

	private void fetchImgColIdx(final DataTableSpec inSpec) throws InvalidSettingsException {
		if (m_colImage.getStringValue() != null && !m_colImage.getStringValue().isEmpty()) {
			m_imageColIdx = inSpec.findColumnIndex(m_colImage.getStringValue());
			if (m_imageColIdx < 0)
				throw new InvalidSettingsException(
						"Image column '" + m_colImage.getStringValue() + "' not found in the input table!");
		} else {
			throw new InvalidSettingsException("An image column must be selected!");
		}
	}

	/** {@inheritDoc} */
	@Override
	protected BufferedDataTable[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
		final BufferedDataTable in = (BufferedDataTable) inObjects[1];
		m_houghForest = ((HoughForestModelPortObject) inObjects[0]).getForest();
		if (m_houghForest.getListOfTrees().isEmpty()) {
			throw new InvalidSettingsException("The Hough forest does not contain any tree. Retrain the model!");
		}
		final ColumnRearranger r = createColumnRearranger(in.getDataTableSpec(), exec);
		final BufferedDataTable out = exec.createColumnRearrangeTable(in, r, exec);
		m_data = out;
		return new BufferedDataTable[] { out };
	}

	private ColumnRearranger createColumnRearranger(final DataTableSpec spec, final ExecutionContext exec)
			throws InvalidSettingsException {
		final ColumnRearranger rearranger = new ColumnRearranger(spec);
		fetchImgColIdx(spec);
		m_labelColIdx = spec.getNumColumns();
		final PredictCellFactory predictCellFactory = new PredictCellFactory(exec, true, createSpec(spec));
		rearranger.append(predictCellFactory);
		return rearranger;
	}

	/** {@inheritDoc} */
	@Override
	public InputPortRole[] getInputPortRoles() {
		return new InputPortRole[] { InputPortRole.NONDISTRIBUTED_NONSTREAMABLE, InputPortRole.DISTRIBUTED_STREAMABLE };
	}

	/** {@inheritDoc} */
	@Override
	public OutputPortRole[] getOutputPortRoles() {
		return new OutputPortRole[] { OutputPortRole.DISTRIBUTED };
	}

	/** {@inheritDoc} */
	@Override
	public StreamableFunction createStreamableOperator(final PartitionInfo partitionInfo,
			final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
		return new StreamableFunction(1, 0) {
			private ExecutionContext m_exec;

			@Override
			public void init(ExecutionContext ctx) throws Exception {
				super.init(ctx);
				m_exec = ctx;
			}

			@Override
			public DataRow compute(DataRow input) throws Exception {
				PredictCellFactory predictCellFactory = new PredictCellFactory(m_exec, true,
						createSpec((DataTableSpec) inSpecs[1]));
				return new AppendedColumnRow(input, predictCellFactory.getCells(input));
			}

			/** {@inheritDoc} */
			@Override
			public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext ctx)
					throws Exception {
				RowInput rowInput = ((RowInput) inputs[1]);
				RowOutput rowOutput = ((RowOutput) outputs[0]);
				if (m_houghForest == null) {
					m_houghForest = ((HoughForestModelPortObject) ((PortObjectInput) inputs[0]).getPortObject())
							.getForest();
					if (m_houghForest.getListOfTrees().isEmpty()) {
						setWarningMessage("Forest does not contain any tree!");
					}
				}
				init(ctx);
				try {
					DataRow inputRow;
					long index = 0;
					while ((inputRow = rowInput.poll()) != null) {
						rowOutput.push(compute(inputRow));
						final long i = ++index;
						final DataRow r = inputRow;
						ctx.setMessage(() -> String.format("Row %d (\"%s\"))", i, r.getKey()));
					}
					rowInput.close();
					rowOutput.close();
				} finally {
					finish();
				}
			}
		};
	}

	@Override
	protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		final File f = new File(nodeInternDir, "view");
		try (ObjectInputStream objIn = new ObjectInputStream(new FileInputStream(f))) {
			m_imageColIdx = objIn.readInt();
			m_labelColIdx = objIn.readInt();
		}
	}

	@Override
	protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		final File f = new File(nodeInternDir, "view");
		try (ObjectOutputStream objOut = new ObjectOutputStream(new FileOutputStream(f))) {
			objOut.writeInt(m_imageColIdx);
			objOut.writeInt(m_labelColIdx);
		}
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

		// Set scales array
		if (m_scaleBool4.isEnabled() && m_scaleBool4.getBooleanValue()) {
			m_scales = new double[] { m_scaleValue1.getDoubleValue(), m_scaleValue2.getDoubleValue(),
					m_scaleValue3.getDoubleValue(), m_scaleValue4.getDoubleValue() };
		} else if (m_scaleBool3.isEnabled() && m_scaleBool3.getBooleanValue()) {
			m_scales = new double[] { m_scaleValue1.getDoubleValue(), m_scaleValue2.getDoubleValue(),
					m_scaleValue3.getDoubleValue() };
		} else if (m_scaleBool2.isEnabled() && m_scaleBool2.getBooleanValue()) {
			m_scales = new double[] { m_scaleValue1.getDoubleValue(), m_scaleValue2.getDoubleValue() };
		} else if (m_scaleBool1.isEnabled() && m_scaleBool1.getBooleanValue()) {
			m_scales = new double[] { m_scaleValue1.getDoubleValue() };
		} else {
			m_scales = new double[] { 1.0 };
		}
	}

	@Override
	protected void reset() {
		m_data = null;
	}

	private final class PredictCellFactory extends AbstractCellFactory {

		private final ExecutionContext m_exec;

		private final LabelingCellFactory m_labelingCellFac;

		private final ImgPlusCellFactory m_imageCellFac;

		private PredictCellFactory(final ExecutionContext exec, final boolean processConcurrently,
				final DataColumnSpec... colSpecs) {
			super(processConcurrently, colSpecs);
			m_exec = exec;
			m_labelingCellFac = new LabelingCellFactory(m_exec);
			m_imageCellFac = new ImgPlusCellFactory(m_exec);
		}

		@SuppressWarnings("unchecked")
		@Override
		public DataCell[] getCells(DataRow row) {
			final ImgPlus<T> img = ((ImgPlusValue<T>) row.getCell(m_imageColIdx)).getImgPlus();
			if (img.numDimensions() != 2 && !(img.numDimensions() == 3 && img.dimension(2) == 3)) {
				throw new IllegalArgumentException(
						"Image of row '" + row.getKey() + "' must be either 2D or 3D with three channels!");
			}
			// will collect votes of the different scales
			final List<RandomAccessibleInterval<FloatType>> votesAllSc = new ArrayList<>(m_scales.length);
			// will collect all prediction objects the of different scales
			final List<List<PredictionObject<FloatType>>> listPredObjAllSc = new ArrayList<>(m_scales.length);

			// Get the feature descriptor stored in the model and apply it to the image
			final FeatureDescriptor<T> featureDescriptor = (FeatureDescriptor<T>) m_houghForest.getFeatureDescriptor();
			final RandomAccessibleInterval<FloatType> featureImg = featureDescriptor.apply(img);

			// === Do the voting for each scale ===
			for (int scIdx = 0; scIdx < m_scales.length; scIdx++) {
				// Scale the feature image, if necessary
				final RandomAccessibleInterval<FloatType> scaledFeatureImage;
				if (Double.compare(m_scales[scIdx], 1.0) == 0) {
					scaledFeatureImage = featureImg;
				} else {
					scaledFeatureImage = (RandomAccessibleInterval<FloatType>) m_ops.run(DefaultScaleView.class,
							featureImg, new double[] { m_scales[scIdx], m_scales[scIdx], 1.0 },
							new NLinearInterpolatorFactory<T>());
				}

				/*
				 * === Patch Extraction ===
				 */
				// Build grid of patch descriptors
				final long[] patchGap = new long[] { m_patchGapX.getIntValue(), m_patchGapY.getIntValue(), 0 };
				final Grid<FloatType> grid = Grids.createGrid(scaledFeatureImage, patchGap,
						m_houghForest.getPatchSize());
				final RandomAccess<RandomAccessibleInterval<FloatType>> raGrid = grid.randomAccess();
				final List<PredictionObject<FloatType>> listPredObjSc = new ArrayList<>(
						(int) (grid.dimension(0) * grid.dimension(1)));
				for (int i = 0; i < grid.dimension(0); i++) {
					for (int j = 0; j < grid.dimension(1); j++) {
						raGrid.setPosition(new int[] { i, j, 0 });
						listPredObjSc.add(new PredictionObject<FloatType>(raGrid.get()));
					}
				}
				listPredObjAllSc.add(listPredObjSc);

				/*
				 * === Prediction/Voting ===
				 */
				final RandomAccessibleInterval<FloatType> votesSc = m_ops.create()
						.img(new FinalInterval(img.dimension(0), img.dimension(1)), new FloatType());
				// Read test data and predict the votes
				final class PredictParallel implements Callable<Void> {

					private final List<PredictionObject<FloatType>> m_batchPredObj;
					private final double m_scale;

					public PredictParallel(final List<PredictionObject<FloatType>> batchPredObj, final double scale) {
						m_batchPredObj = batchPredObj;
						m_scale = scale;
					}

					@Override
					public Void call() {
						for (final PredictionObject<FloatType> predObj : m_batchPredObj) {
							Predictor.predictForest(m_houghForest, predObj, votesSc.randomAccess(),
									new FinalInterval(scaledFeatureImage.dimension(0), scaledFeatureImage.dimension(1)),
									m_scale);
						}
						return null;
					}

				}
				final List<PredictParallel> threads = new ArrayList<>();
				final int sizeListPredObjSc = listPredObjSc.size();
				final int batchSize = (int) (sizeListPredObjSc / Runtime.getRuntime().availableProcessors()) + 1;
				int i = 0;
				while (true) {
					final int to = (i + 1) * batchSize;
					final List<PredictionObject<FloatType>> subList;
					if (to < sizeListPredObjSc) {
						subList = listPredObjSc.subList(i * batchSize, to);
						threads.add(new PredictParallel(subList, m_scales[scIdx]));
						i++;
					} else {
						subList = listPredObjSc.subList(i * batchSize, sizeListPredObjSc);
						threads.add(new PredictParallel(subList, m_scales[scIdx]));
						break;
					}
				}
				try {
					m_es.invokeAll(threads);
				} catch (final InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new RuntimeException(e);
				}
				votesAllSc.add(votesSc);
			}

			// Blur votes
			final double sigma = m_sigma.getDoubleValue();
			final RandomAccessibleInterval<FloatType> votes = m_ops.filter().convolve(Views.stack(votesAllSc),
					(RandomAccessibleInterval<T>) m_ops.create().kernelGauss(sigma, sigma, 0)); // TODO sigma for 3rd
																								// dim?

			/*
			 * === Bounding Box Estimation ===
			 */
			// Get points with max votes
			final List<int[]> maxVotesPositions = new ArrayList<>();
			final RandomAccessibleInterval<BitType> maxima;
			if (m_multipleDetection.getBooleanValue()) {
				final MaximumFinderOp<T> maximumFinderOp = new MaximumFinderOp<T>(0, 0);
				final IterableInterval<BitType> thresholdedVotings = m_ops.threshold().apply(Views.iterable(votes),
						new FloatType((float) m_thresholdMultipleDetection.getDoubleValue()));
				maxima = m_ops.create().img(votes, new BitType());
				maximumFinderOp.compute((RandomAccessibleInterval<T>) thresholdedVotings, maxima);
				final Cursor<BitType> cursorMaxima = Views.flatIterable(maxima).cursor();
				while (cursorMaxima.hasNext()) {
					cursorMaxima.fwd();
					if (cursorMaxima.get().get()) {
						int[] maxVotesPos = new int[votes.numDimensions()];
						cursorMaxima.localize(maxVotesPos);
						maxVotesPositions.add(maxVotesPos);
					}
				}
			} else {
				maxima = m_ops.create().img(votes, new BitType());
				final int[] maxVotesPos = new int[votes.numDimensions()];
				final Cursor<FloatType> cursor = Views.iterable(votes).cursor();
				float tmpMax = Integer.MIN_VALUE;
				while (cursor.hasNext()) {
					cursor.fwd();
					final float currentValue = cursor.get().getRealFloat();
					if (currentValue > tmpMax) {
						tmpMax = currentValue;
						cursor.localize(maxVotesPos);
					}
				}
				maxVotesPositions.add(maxVotesPos);
			}

			/*
			 * === Back projection ===
			 */
			// Create output labeling on which is drawn
			final ImgLabeling<String, IntType> labelings = m_ops.create()
					.imgLabeling(new FinalInterval(img.dimension(0), img.dimension(1)));
			final RandomAccess<LabelingType<String>> raLabeling = labelings.randomAccess();

			// Draw every found object
			final int sizeBackprojection = m_spanIntervalBackprojection.getIntValue();
			boolean predictionSuccessful = true;
			for (final int[] maxVotesPos : maxVotesPositions) {
				// will collect all vertices which voted inside the field around the
				// point with max votes
				final List<Localizable> vertices = new ArrayList<>();

				for (int i = 0; i < m_scales.length; i++) {
					// Compute scaled interval of max votes
					final FinalInterval scaledIntervalOfMaxVotes = new FinalInterval(
							new long[] { (long) ((maxVotesPos[0] - sizeBackprojection) * m_scales[i]),
									(long) ((maxVotesPos[1] - sizeBackprojection) * m_scales[i]) },
							new long[] { (long) ((maxVotesPos[0] + sizeBackprojection) * m_scales[i]),
									(long) ((maxVotesPos[1] + sizeBackprojection) * m_scales[i]) });

					// Get all the patches which vote inside the
					// scaledIntervalOfMaxVotes
					for (final PredictionObject<FloatType> predObj : listPredObjAllSc.get(i)) {
						vertices.addAll(Predictor.getVertices(predObj, scaledIntervalOfMaxVotes, m_scales[i]));
					}
				}

				/*
				 * Draw Bounding Box
				 */
				if (vertices.isEmpty()) {
					predictionSuccessful = false;
				} else {
					final Polygon boundingBox = new Polygon(vertices);
					for (int i = (int) boundingBox.realMin(0); i <= boundingBox.realMax(0); i++) {
						for (int j = (int) boundingBox.realMin(1); j <= boundingBox.realMax(1); j++) {
							raLabeling.setPosition(new int[] { i, j });
							raLabeling.get().add("1");
						}
					}

					if (m_outputAdvanced.getBooleanValue()) {
						/*
						 * Draw max interval
						 */
						for (int i = maxVotesPos[0] - sizeBackprojection; i <= maxVotesPos[0]
								+ sizeBackprojection; i++) {
							for (int j = maxVotesPos[1] - sizeBackprojection; j <= maxVotesPos[1]
									+ sizeBackprojection; j++) {
								raLabeling.setPosition(new int[] { i, j });
								raLabeling.get().add("2");
							}
						}

						/*
						 * Draw vertices from which has been voted inside max interval
						 */
						for (final Localizable vertix : vertices) {
							raLabeling.setPosition(vertix);
							raLabeling.get().add("3");
						}
					}
				}
			}

			if (!predictionSuccessful) {
				setWarningMessage(
						"For some images the prediction was not successfull! Probably the model needs to be trained with more data.");
			}

			/*
			 * === Create output ===
			 */
			final ArrayList<DataCell> cells = new ArrayList<>();
			try {
				cells.add(m_labelingCellFac.createCell(labelings,
						new DefaultLabelingMetadata(2, new DefaultLabelingColorTable())));
				if (m_outputVotes.getBooleanValue()) {
					cells.add(m_imageCellFac
							.createCell(new ImgPlus<>(ImgView.wrap(votes, new ArrayImgFactory<FloatType>()))));
				}
				if (m_outputMaxima.getBooleanValue()) {
					cells.add(m_imageCellFac
							.createCell(new ImgPlus<>(ImgView.wrap(maxima, new ArrayImgFactory<BitType>()))));
				}
			} catch (IOException e) {
				throw new IllegalStateException(e.getMessage());
			}
			return cells.toArray(new DataCell[cells.size()]);
		}

	}

	// ========= VIEW =========
	// mainly copied from org.knime.knip.base.nodes.view.segmentoverlay.SegmentOverlayNodeModel

	@Override
	public BufferedDataTable[] getInternalTables() {
		return new BufferedDataTable[] { m_data };
	}

	@Override
	public void setInternalTables(BufferedDataTable[] tables) {
		m_data = tables[0];
	}

	public TableContentModel getTableContentModel() {
		final TableContentModel contentModel = new TableContentModel();
		contentModel.setDataTable(m_data);
		return contentModel;
	}

	public StringTransformer getTransformer() {
		return new StringTransformer("$" + LabelTransformVariables.Label + "$", "$");
	}

	int getImageColIdx() {
		return m_imageColIdx;
	}

	int getLabelColIdx() {

		return m_labelColIdx;
	}

	enum LabelTransformVariables {
		ImgName, ImgSource, Label, LabelingName, LabelingSource, RowID;
	};
}

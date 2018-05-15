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
import org.knime.knip.hough.forest.node.Node;
import org.knime.knip.hough.forest.prediction.PredictionObject;
import org.knime.knip.hough.forest.prediction.PredictorEntangled;
import org.knime.knip.hough.grid.Grid;
import org.knime.knip.hough.grid.Grids;
import org.knime.knip.hough.nodes.evaluator.HoughForestEvaluator;
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
import net.imglib2.roi.Regions;
import net.imglib2.roi.geometric.Polygon;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.BooleanType;
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

	private HoughForestPredictorConfig m_config;

	// Input
	private HoughForest m_houghForest;

	// Data table and column indices for the table cell view
	private BufferedDataTable m_data;
	private int m_imageColIdx;
	private int m_labelColIdx;

	// Services
	private OpService m_ops;
	private ExecutorService m_es;

	public HoughForestPredictorNodeModel() {
		super(new PortType[] { HoughForestModelPortObject.TYPE, BufferedDataTable.TYPE },
				new PortType[] { BufferedDataTable.TYPE });
	}

	/** {@inheritDoc} */
	@Override
	protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
		if (m_config == null) {
			m_config = new HoughForestPredictorConfig();
		}
		final DataTableSpec in = (DataTableSpec) inSpecs[1];
		final ColumnRearranger r = createColumnRearranger(in, null);
		final DataTableSpec out = r.createSpec();
		return new DataTableSpec[] { out };
	}

	private DataColumnSpec[] createSpec(final DataTableSpec spec) {
		final UniqueNameGenerator uniqueNameGenerator = new UniqueNameGenerator(spec);
		final ArrayList<DataColumnSpec> specs = new ArrayList<>();
		specs.add(uniqueNameGenerator.newColumn("Prediction", LabelingCell.TYPE));
		if (m_config.getOutputVotes()) {
			specs.add(uniqueNameGenerator.newColumn("Votes", ImgPlusCell.TYPE));
		}
		if (m_config.getOutputMaxima()) {
			specs.add(uniqueNameGenerator.newColumn("Maxima", ImgPlusCell.TYPE));
		}
		if (m_config.getOutputFeatureImg()) {
			specs.add(uniqueNameGenerator.newColumn("Features", ImgPlusCell.TYPE));
		}
		if (m_config.getOutputNodeIdx()) {
			specs.add(uniqueNameGenerator.newColumn("NodeIdx", ImgPlusCell.TYPE));
		}
		return specs.toArray(new DataColumnSpec[specs.size()]);
	}

	private void fetchImgColIdx(final DataTableSpec inSpec) throws InvalidSettingsException {
		if (m_config.getColImage() != null && !m_config.getColImage().isEmpty()) {
			m_imageColIdx = inSpec.findColumnIndex(m_config.getColImage());
			if (m_imageColIdx < 0)
				throw new InvalidSettingsException(
						"Image column '" + m_config.getColImage() + "' not found in the input table!");
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
		// OpService
		m_ops = KNIPGateway.getInstance().ctx().getService(OpService.class);
		// ExecutorService
		m_es = KNIPGateway.threads().getExecutorService();

		final ColumnRearranger r = createColumnRearranger(in.getDataTableSpec(), exec);
		final BufferedDataTable out = exec.createColumnRearrangeTable(in, r, exec);
		m_data = out;
		m_es.shutdown();
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
			m_config = new HoughForestPredictorConfig();
		}
		m_config.loadValidatedSettingsFrom(settings);
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
			if (row.getCell(m_imageColIdx).isMissing()) {
				throw new IllegalArgumentException("Row '" + row.getKey() + "' contains a missing cell!");
			}
			final ImgPlus<T> img = ((ImgPlusValue<T>) row.getCell(m_imageColIdx)).getImgPlus();
			if (img.numDimensions() != 2 && !(img.numDimensions() == 3 && img.dimension(2) == 3)) {
				throw new IllegalArgumentException(
						"Image of row '" + row.getKey() + "' must be either 2D or 3D with three channels!");
			}
			final double[] scales = m_config.getScales();
			// will collect votes of the different scales
			final List<RandomAccessibleInterval<FloatType>> votesAllSc = new ArrayList<>(scales.length);
			// will collect all prediction objects the of different scales
			final List<List<PredictionObject<FloatType>>> listPredObjAllSc = new ArrayList<>(scales.length);

			// Get the feature descriptor stored in the model and apply it to the image
			final FeatureDescriptor<T> featureDescriptor = (FeatureDescriptor<T>) m_houghForest.getFeatureDescriptor();
			final boolean isColorImage = img.numDimensions() == 3;
			if (isColorImage && !featureDescriptor.isColorImage()) {
				throw new IllegalArgumentException("Predictions with this model can only be done on grayscale images!");
			}
			if (!isColorImage && featureDescriptor.isColorImage()) {
				throw new IllegalArgumentException("Predictions with this model can only be done on color images!");
			}
			final RandomAccessibleInterval<FloatType> featureImg = featureDescriptor.apply(img);

			RandomAccessibleInterval<IntType> nodeIdxImage = null;
			// === Do the voting for each scale ===
			for (int scIdx = 0; scIdx < scales.length; scIdx++) {
				// Scale the feature image, if necessary
				final RandomAccessibleInterval<FloatType> scaledFeatureImage;
				if (Double.compare(scales[scIdx], 1.0) == 0) {
					scaledFeatureImage = featureImg;
				} else {
					scaledFeatureImage = (RandomAccessibleInterval<FloatType>) m_ops.run(DefaultScaleView.class,
							featureImg, new double[] { scales[scIdx], scales[scIdx], 1.0 },
							new NLinearInterpolatorFactory<T>());
				}

				final RandomAccess<FloatType>[] randomAccess = new RandomAccess[m_houghForest.getListOfTrees().size()];
				for (int i = 0; i < randomAccess.length; i++) {
					randomAccess[i] = scaledFeatureImage.randomAccess();
				}

				/*
				 * === Patch Extraction ===
				 */
				// Build grid of patch descriptors
				final long[] patchGap = new long[] { m_config.getPatchGapX(), m_config.getPatchGapY(), 0 };
				final Grid<FloatType> grid = Grids.createGrid(scaledFeatureImage, patchGap,
						m_houghForest.getPatchSize());
				final RandomAccess<RandomAccessibleInterval<FloatType>> raGrid = grid.randomAccess();
				@SuppressWarnings("rawtypes")
				final PredictionObject[][] predictionObjectGrid = new PredictionObject[(int) grid
						.dimension(0)][(int) grid.dimension(1)];
				final Node[][][] nodeGrid = new Node[m_houghForest.getListOfTrees()
						.size()][(int) grid.dimension(0)][(int) grid.dimension(1)];
				final List<PredictionObject<FloatType>> listPredObjSc = new ArrayList<>(
						(int) (grid.dimension(0) * grid.dimension(1)));
				for (int i = 0; i < grid.dimension(0); i++) {
					for (int j = 0; j < grid.dimension(1); j++) {
						final int[] pos = new int[] { i, j, 0 };
						raGrid.setPosition(pos);
						final RandomAccessibleInterval<FloatType> patch = raGrid.get();
						final int[] patchMid = new int[] { (int) (patch.min(0) + (patch.dimension(0) / 2)),
								(int) (patch.min(1) + (patch.dimension(1) / 2)) };
						final PredictionObject<FloatType> pObj = new PredictionObject<FloatType>(patch, randomAccess,
								patchMid, predictionObjectGrid, pos, nodeGrid);
						listPredObjSc.add(pObj);
						predictionObjectGrid[i][j] = pObj;
					}
				}
				listPredObjAllSc.add(listPredObjSc);

				/*
				 * === Prediction/Voting ===
				 */
				final RandomAccessibleInterval<FloatType> votesSc = m_ops.create()
						.img(new FinalInterval(img.dimension(0), img.dimension(1)), new FloatType());
				PredictorEntangled.predictForest(m_houghForest, listPredObjSc, votesSc.randomAccess(),
						new FinalInterval(scaledFeatureImage.dimension(0), scaledFeatureImage.dimension(1)),
						scales[scIdx], m_config);
				votesAllSc.add(votesSc);

				// Node[][] nodes = listPredObjSc.get(0).getNodeGrid()[0];
				// StringBuilder stringBuilder = new StringBuilder();
				// for (int i = 0; i < nodes.length; i++) {
				// for (int j = 0; j < nodes[0].length; j++) {
				// stringBuilder.append(String.format("%3d ", nodes[i][j].getNodeIdx()));
				// }
				// stringBuilder.append("\n");
				// }
				// System.out.println(stringBuilder.toString());
				nodeIdxImage = HoughForestEvaluator.createNodeIdxImage(listPredObjSc.get(0).getNodeGrid(), m_ops);
			}

			// Blur votes
			final RandomAccessibleInterval<FloatType> votes = m_ops.filter().convolve(Views.stack(votesAllSc),
					(RandomAccessibleInterval<T>) m_ops.create().kernelGauss(m_config.getSigmaXY(),
							m_config.getSigmaXY(), m_config.getSigmaZ())); // TODO filtering in z seems to be incorrect

			/*
			 * === Bounding Box Estimation ===
			 */
			// Get points with max votes
			final List<int[]> maxVotesPositions = new ArrayList<>();
			final RandomAccessibleInterval<BitType> maxima;
			if (m_config.getMultipleDetection()) {
				final MaximumFinderOp<T> maximumFinderOp = new MaximumFinderOp<T>(0,
						m_config.getMaxSuppressionMultipleDetection());
				final IterableInterval<BitType> thresholdedVotings = m_ops.threshold().apply(Views.iterable(votes),
						new FloatType((float) m_config.getThresholdMultipleDetection()));
				maxima = m_ops.create().img(votes, new BitType());
				maximumFinderOp.compute((RandomAccessibleInterval<T>) votes, maxima);
				final RandomAccess<BitType> randomAccess = maxima.randomAccess();
				@SuppressWarnings("rawtypes")
				final Cursor<Void> cursorMaxima = Regions
						.iterable((RandomAccessibleInterval<BooleanType>) thresholdedVotings).localizingCursor();
				while (cursorMaxima.hasNext()) {
					cursorMaxima.fwd();
					randomAccess.setPosition(cursorMaxima);
					if (randomAccess.get().get()) {
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
			final int sizeBackprojection = m_config.getSpanIntervalBackprojection();
			boolean predictionSuccessful = true;
			int count = 0;
			for (final int[] maxVotesPos : maxVotesPositions) {
				// will collect all vertices which voted inside the field around the
				// point with max votes
				final List<Localizable> vertices = new ArrayList<>();

				for (int i = 0; i < scales.length; i++) {
					// Compute scaled interval of max votes
					final FinalInterval scaledIntervalOfMaxVotes = new FinalInterval(
							new long[] { (long) ((maxVotesPos[0] - sizeBackprojection) * scales[i]),
									(long) ((maxVotesPos[1] - sizeBackprojection) * scales[i]) },
							new long[] { (long) ((maxVotesPos[0] + sizeBackprojection) * scales[i]),
									(long) ((maxVotesPos[1] + sizeBackprojection) * scales[i]) });

					// Get all the patches which vote inside the
					// scaledIntervalOfMaxVotes
					for (final PredictionObject<FloatType> predObj : listPredObjAllSc.get(i)) {
						vertices.addAll(PredictorEntangled.getVertices(predObj, scaledIntervalOfMaxVotes, scales[i]));
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
							raLabeling.get().add("Object" + count);
						}
					}

					if (m_config.getOutputAdvanced()) {
						/*
						 * Draw max interval
						 */
						for (int i = maxVotesPos[0] - sizeBackprojection; i <= maxVotesPos[0]
								+ sizeBackprojection; i++) {
							for (int j = maxVotesPos[1] - sizeBackprojection; j <= maxVotesPos[1]
									+ sizeBackprojection; j++) {
								raLabeling.setPosition(new int[] { i, j });
								raLabeling.get().add("MaxInterval");
							}
						}

						/*
						 * Draw vertices from which has been voted inside max interval
						 */
						for (final Localizable vertix : vertices) {
							raLabeling.setPosition(vertix);
							raLabeling.get().add("Vertix");
						}
					}
				}
				count++;
			}

			if (!predictionSuccessful) {
				setWarningMessage(
						"For some images the prediction was not successful! Probably the model needs to be trained with more data.");
			}

			/*
			 * === Create output ===
			 */
			final ArrayList<DataCell> cells = new ArrayList<>();
			try {
				cells.add(m_labelingCellFac.createCell(labelings,
						new DefaultLabelingMetadata(2, new DefaultLabelingColorTable())));
				if (m_config.getOutputVotes()) {
					cells.add(m_imageCellFac
							.createCell(new ImgPlus<>(ImgView.wrap(votes, new ArrayImgFactory<FloatType>()))));
				}
				if (m_config.getOutputMaxima()) {
					cells.add(m_imageCellFac
							.createCell(new ImgPlus<>(ImgView.wrap(maxima, new ArrayImgFactory<BitType>()))));
				}
				if (m_config.getOutputFeatureImg()) {
					cells.add(m_imageCellFac
							.createCell(new ImgPlus<>(ImgView.wrap(featureImg, new ArrayImgFactory<FloatType>()))));
				}
				if (m_config.getOutputNodeIdx()) {
					cells.add(m_imageCellFac
							.createCell(new ImgPlus<>(ImgView.wrap(nodeIdxImage, new ArrayImgFactory<IntType>()))));
				}
				// if (m_config.getOutputNodeIdx()) {
				// cells.add(m_imageCellFac
				// .createCell(new ImgPlus<>(ImgView.wrap(nodeIdxImage, new ArrayImgFactory<IntType>()))));
				// }
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

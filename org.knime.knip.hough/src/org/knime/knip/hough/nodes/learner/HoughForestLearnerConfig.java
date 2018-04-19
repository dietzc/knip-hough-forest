package org.knime.knip.hough.nodes.learner;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelLong;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

public final class HoughForestLearnerConfig {

	private double[] m_thresholds;
	// Input
	private final SettingsModelString m_colImage = createColSelectionModel();
	private final SettingsModelString m_colLabel = createLabelSelectionModel();
	// Tree Options
	private final SettingsModelIntegerBounded m_numSamples = createNumSamplesModel();
	private final SettingsModelIntegerBounded m_numSplitFunctions = createNumSplitFunctionsModel();
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

	// Entanglement
	private final SettingsModelBoolean m_entanglement = createEntanglementModel();
	private final SettingsModelDoubleBounded m_ratioEntanglement = createRatioEntanglementModel();

	private final SettingsModelIntegerBounded m_horizontalMinOffset = createHorizontalMinOffsetModel();
	private final SettingsModelIntegerBounded m_horizontalMaxOffset = createHorizontalMaxOffsetModel();
	private final SettingsModelIntegerBounded m_verticalMinOffset = createVerticalMinOffsetModel();
	private final SettingsModelIntegerBounded m_verticalMaxOffset = createVerticalMaxOffsetModel();

	public static final String MAP_CLASS_SF = "use_map_class";
	private final SettingsModelBoolean m_useMapClassSplitFunction = createUseMapClassSplitFunctionModel();

	public static final String NODE_DESCENDANT_SF = "use_node_descendant";
	private final SettingsModelBoolean m_useNodeDescendantSplitFunction = createUseNodeDescendantSplitFunctionModel();

	public static final String ANCESTOR_NODE_PAIR_SF = "use_ancestor_node_pair";
	private final SettingsModelBoolean m_useAncestorNodePairSplitFunction = createUseAncestorNodePairSplitFunctionModel();
	private final SettingsModelIntegerBounded m_ancestorNodePairThreshold = createAncestorNodePairThresholdModel(
			m_useAncestorNodePairSplitFunction);

	public static final String OFFSET_SIMILARITY_NODE_PAIR_SF = "use_offset_similarity_node_pair";
	private final SettingsModelBoolean m_useOffsetSimiliarityNodePairSplitFunction = createUseOffsetNodePairSplitFunctionModel();
	private final SettingsModelDoubleBounded m_offsetSimilarityNodePairSigma = createOffsetSimilarityNodePairSigmaModel(
			m_useOffsetSimiliarityNodePairSplitFunction);

	public static final String ENTANGLED_DEFAULT_SF = "use_entangled_default";

	private final SettingsModel[] m_listSettingsModels = { m_colImage, m_colLabel, m_numSamples, m_numSplitFunctions,
			m_depth, m_minSizeSample, m_numTrees, m_useSeed, m_seed, m_convertToLab, m_firstDerivative,
			m_useAbsoluteFirstDerivative, m_secondDerivative, m_useAbsoluteSecondDerivative, m_hog, m_hogNumBins,
			m_applyMinMax, m_useAbsolute, m_patchWidth, m_patchHeight, m_patchGapX, m_patchGapY, m_entanglement,
			m_ratioEntanglement, m_useMapClassSplitFunction, m_useNodeDescendantSplitFunction,
			m_useAncestorNodePairSplitFunction, m_useOffsetSimiliarityNodePairSplitFunction, m_horizontalMinOffset,
			m_horizontalMaxOffset, m_verticalMinOffset, m_verticalMaxOffset, m_ancestorNodePairThreshold,
			m_offsetSimilarityNodePairSigma };

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

	static SettingsModelBoolean createEntanglementModel() {
		return new SettingsModelBoolean("entanglement", true);
	}

	static SettingsModelIntegerBounded createHorizontalMinOffsetModel() {
		return new SettingsModelIntegerBounded("horizontal_min_offset", 0, 0, Integer.MAX_VALUE);
	}

	static SettingsModelIntegerBounded createHorizontalMaxOffsetModel() {
		return new SettingsModelIntegerBounded("horizontal_max_offset", 50, 0, Integer.MAX_VALUE);
	}

	static SettingsModelIntegerBounded createVerticalMinOffsetModel() {
		return new SettingsModelIntegerBounded("vertical_min_offset", 0, 0, Integer.MAX_VALUE);
	}

	static SettingsModelIntegerBounded createVerticalMaxOffsetModel() {
		return new SettingsModelIntegerBounded("vertical_max_offset", 50, 0, Integer.MAX_VALUE);
	}

	static SettingsModelDoubleBounded createRatioEntanglementModel() {
		return new SettingsModelDoubleBounded("ratio_entanglement", 0.5, 0.0, 1.0);
	}

	static SettingsModelBoolean createUseMapClassSplitFunctionModel() {
		return new SettingsModelBoolean(MAP_CLASS_SF, true);
	}

	static SettingsModelBoolean createUseNodeDescendantSplitFunctionModel() {
		return new SettingsModelBoolean(NODE_DESCENDANT_SF, false);
	}

	static SettingsModelBoolean createUseAncestorNodePairSplitFunctionModel() {
		return new SettingsModelBoolean(ANCESTOR_NODE_PAIR_SF, false);
	}

	static SettingsModelIntegerBounded createAncestorNodePairThresholdModel(
			final SettingsModelBoolean useAncestorNodePairSplitFunctionModel) {
		final SettingsModelIntegerBounded ancestorNodePairThresholdModel = new SettingsModelIntegerBounded(
				"threshold_ancestor_node_pair", 5, 1, Integer.MAX_VALUE);
		ancestorNodePairThresholdModel.setEnabled(false);
		useAncestorNodePairSplitFunctionModel.addChangeListener(
				l -> ancestorNodePairThresholdModel.setEnabled(useAncestorNodePairSplitFunctionModel.isEnabled()
						&& useAncestorNodePairSplitFunctionModel.getBooleanValue()));
		return ancestorNodePairThresholdModel;
	}

	static SettingsModelBoolean createUseOffsetNodePairSplitFunctionModel() {
		return new SettingsModelBoolean(OFFSET_SIMILARITY_NODE_PAIR_SF, false);
	}

	static SettingsModelDoubleBounded createOffsetSimilarityNodePairSigmaModel(
			final SettingsModelBoolean useOffsetNodePairSplitFunctionModel) {
		final SettingsModelDoubleBounded modelDoubleBounded = new SettingsModelDoubleBounded(
				"sigma_offset_similarity_node_pair", 7.0, 0.1, Long.MAX_VALUE);
		modelDoubleBounded.setEnabled(false);
		useOffsetNodePairSplitFunctionModel
				.addChangeListener(l -> modelDoubleBounded.setEnabled(useOffsetNodePairSplitFunctionModel.isEnabled()
						&& useOffsetNodePairSplitFunctionModel.getBooleanValue()));
		return modelDoubleBounded;
	}

	protected void saveSettingsTo(NodeSettingsWO settings) {
		for (final SettingsModel s : m_listSettingsModels) {
			s.saveSettingsTo(settings);
		}
	}

	protected void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		for (final SettingsModel s : m_listSettingsModels) {
			s.validateSettings(settings);
		}
	}

	protected void loadValidatedSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		for (final SettingsModel s : m_listSettingsModels) {
			s.loadSettingsFrom(settings);
		}

		if (getNumTrees() < 1) {
			throw new InvalidSettingsException("The number of trees must be at least 1!");
		}
		if (getNumSamples() < getMinSizeSample()) {
			throw new InvalidSettingsException("The size of the sample must not be lower than the minimum size!");
		}
		if (getHorizontalMinOffset() > getHorizontalMaxOffset() || getVerticalMinOffset() > getVerticalMaxOffset()) {
			throw new InvalidSettingsException("Min. offset must not be greater than max. offset!");
		}
		if (getEntanglement() && getEnabledSFs().size() < 1) {
			throw new InvalidSettingsException("At least one entangled split function must be enabled!");
		}
	}

	/**
	 * @return the colImage
	 */
	public String getColImage() {
		return m_colImage.getStringValue();
	}

	/**
	 * @return the colLabel
	 */
	public String getColLabel() {
		return m_colLabel.getStringValue();
	}

	/**
	 * @return the numSamples
	 */
	public int getNumSamples() {
		return m_numSamples.getIntValue();
	}

	/**
	 * @return the numSplitFunctions
	 */
	public int getNumSplitFunctions() {
		return m_numSplitFunctions.getIntValue();
	}

	/**
	 * @return the depth
	 */
	public int getDepth() {
		return m_depth.getIntValue();
	}

	/**
	 * @return the minSizeSample
	 */
	public int getMinSizeSample() {
		return m_minSizeSample.getIntValue();
	}

	/**
	 * @return the numTrees
	 */
	public int getNumTrees() {
		return m_numTrees.getIntValue();
	}

	/**
	 * @return the useSeed
	 */
	public boolean getUseSeed() {
		return m_useSeed.getBooleanValue();
	}

	/**
	 * @return the seed
	 */
	public long getSeed() {
		return m_seed.getLongValue();
	}

	/**
	 * @return the convertToLab
	 */
	public boolean getConvertToLab() {
		return m_convertToLab.getBooleanValue();
	}

	/**
	 * @return the firstDerivative
	 */
	public boolean getFirstDerivative() {
		return m_firstDerivative.getBooleanValue();
	}

	/**
	 * @return the useAbsoluteFirstDerivative
	 */
	public boolean getUseAbsoluteFirstDerivative() {
		return m_useAbsoluteFirstDerivative.getBooleanValue();
	}

	/**
	 * @return the secondDerivative
	 */
	public boolean getSecondDerivative() {
		return m_secondDerivative.getBooleanValue();
	}

	/**
	 * @return the useAbsoluteSecondDerivative
	 */
	public boolean getUseAbsoluteSecondDerivative() {
		return m_useAbsoluteSecondDerivative.getBooleanValue();
	}

	/**
	 * @return the hog
	 */
	public boolean getHog() {
		return m_hog.getBooleanValue();
	}

	/**
	 * @return the hogNumBins
	 */
	public int getHogNumBins() {
		return m_hogNumBins.getIntValue();
	}

	/**
	 * @return the applyMinMax
	 */
	public boolean getApplyMinMax() {
		return m_applyMinMax.getBooleanValue();
	}

	/**
	 * @return the useAbsolute
	 */
	public boolean getUseAbsolute() {
		return m_useAbsolute.getBooleanValue();
	}

	/**
	 * @return the patchGapX
	 */
	public int getPatchGapX() {
		return m_patchGapX.getIntValue();
	}

	/**
	 * @return the patchGapY
	 */
	public int getPatchGapY() {
		return m_patchGapY.getIntValue();
	}

	/**
	 * @return the stride
	 */
	public int[] getStride() {
		return new int[] { getPatchGapX(), getPatchGapY() };
	}

	public int[] createRandomOffset(final Random random) {
		final int minX = getHorizontalMinOffset() / getPatchGapX();
		final int maxX = (int) ((double) getHorizontalMaxOffset() / getPatchGapX() + 0.5);
		final int minY = getVerticalMinOffset() / getPatchGapY();
		final int maxY = (int) ((double) getVerticalMaxOffset() / getPatchGapY() + 0.5);
		return new int[] { (random.nextBoolean() ? 1 : (-1)) * (random.nextInt(maxX - minX + 1) + minX),
				(random.nextBoolean() ? 1 : (-1)) * (random.nextInt(maxY - minY + 1) + minY) };
	}

	/**
	 * @return the patchWidth
	 */
	public int getPatchWidth() {
		return m_patchWidth.getIntValue();
	}

	/**
	 * @return the patchHeight
	 */
	public int getPatchHeight() {
		return m_patchHeight.getIntValue();
	}

	/**
	 * @return the entanglement
	 */
	public boolean getEntanglement() {
		return m_entanglement.getBooleanValue();
	}

	/**
	 * @return the ratioEntanglement
	 */
	public double getRatioEntanglement() {
		return m_ratioEntanglement.getDoubleValue();
	}

	/**
	 * @return the useMapClassSplitFunction
	 */
	public boolean getUseMapClassSplitFunction() {
		return m_useMapClassSplitFunction.getBooleanValue();
	}

	/**
	 * @return the useNodeDescendantSplitFunction
	 */
	public boolean getUseNodeDescendantSplitFunction() {
		return m_useNodeDescendantSplitFunction.getBooleanValue();
	}

	/**
	 * @return the useAncestorNodePairSplitFunction
	 */
	public boolean getUseAncestorNodePairSplitFunction() {
		return m_useAncestorNodePairSplitFunction.getBooleanValue();
	}

	/**
	 * @return the useOffsetSimilarityNodePairSplitFunction
	 */
	public boolean getUseOffsetSimilarityNodePairSplitFunction() {
		return m_useOffsetSimiliarityNodePairSplitFunction.getBooleanValue();
	}

	/**
	 * @return list of used split entangled split functions
	 */
	public List<String> getEnabledSFs() {
		final List<String> list = new ArrayList<>();
		if (getUseMapClassSplitFunction()) {
			list.add(MAP_CLASS_SF);
		}
		if (getUseNodeDescendantSplitFunction()) {
			list.add(NODE_DESCENDANT_SF);
		}
		if (getUseAncestorNodePairSplitFunction()) {
			list.add(ANCESTOR_NODE_PAIR_SF);
		}
		if (getUseOffsetSimilarityNodePairSplitFunction()) {
			list.add(OFFSET_SIMILARITY_NODE_PAIR_SF);
		}
		// list.add(ENTANGLED_DEFAULT_SF);
		return list;
	}

	/**
	 * @return the thresholds
	 */
	public double[] getThresholds() {
		return m_thresholds;
	}

	/**
	 * @param thresholds the thresholds to set
	 */
	public void setThresholds(final double[] thresholds) {
		m_thresholds = thresholds;
	}

	/**
	 * @return the horizontalMinOffset
	 */
	public int getHorizontalMinOffset() {
		return m_horizontalMinOffset.getIntValue();
	}

	/**
	 * @return the horizontalMaxOffset
	 */
	public int getHorizontalMaxOffset() {
		return m_horizontalMaxOffset.getIntValue();
	}

	/**
	 * @return the verticalMinOffset
	 */
	public int getVerticalMinOffset() {
		return m_verticalMinOffset.getIntValue();
	}

	/**
	 * @return the verticalMaxOffset
	 */
	public int getVerticalMaxOffset() {
		return m_verticalMaxOffset.getIntValue();
	}

	/**
	 * @return the ancestorNodePairThreshold
	 */
	public int getAncestorNodePairThreshold() {
		return m_ancestorNodePairThreshold.getIntValue();
	}

	/**
	 * @return the offsetSimilarityNodePairSigma
	 */
	public double getOffsetSimilarityNodePairSigma() {
		return m_offsetSimilarityNodePairSigma.getDoubleValue();
	}
}

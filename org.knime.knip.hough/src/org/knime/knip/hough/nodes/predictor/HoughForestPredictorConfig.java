package org.knime.knip.hough.nodes.predictor;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

public final class HoughForestPredictorConfig {

	private final SettingsModelString m_colImage = createColSelectModel();
	// Patch Extraction
	private final SettingsModelIntegerBounded m_patchGapX = createPatchGapXModel();
	private final SettingsModelIntegerBounded m_patchGapY = createPatchGapYModel();
	// Bounding Box Estimation
	private final SettingsModelIntegerBounded m_spanIntervalBackprojection = createSpanIntervalBackprojectionModel();
	// Multiple Detection
	private final SettingsModelBoolean m_multipleDetection = createMultipleDetectionBoolModel();
	private final SettingsModelDoubleBounded m_thresholdMultipleDetection = createThresholdMultipleDetectionDoubleModel(
			m_multipleDetection);
	private final SettingsModelDoubleBounded m_maxSuppressionMultipleDetection = createMaxSuppressionMultipleDetectionDoubleModel(
			m_multipleDetection);
	private final SettingsModelDoubleBounded m_sigmaXY = createSigmaXYModel();
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

	private final SettingsModelDoubleBounded m_sigmaZ = createSigmaZModel(m_scaleBool2);
	// Output
	private final SettingsModelBoolean m_outputVotes = createOutputVotesBoolModel();
	private final SettingsModelBoolean m_outputMaxima = createOutputMaximaBoolModel();
	private final SettingsModelBoolean m_outputAdvanced = createOutputAdvancedBoolModel();
	private final SettingsModelBoolean m_outputFeatureImg = createOutputFeatureImgBoolModel();
	private final SettingsModelBoolean m_outputNodeIdx = createOutputNodeIdxBoolModel();

	private final SettingsModel[] m_listSettingsModels = { m_colImage, m_patchGapX, m_patchGapY,
			m_spanIntervalBackprojection, m_sigmaXY, m_sigmaZ, m_thresholdMultipleDetection, m_multipleDetection,
			m_maxSuppressionMultipleDetection, m_scaleBool1, m_scaleBool2, m_scaleBool3, m_scaleBool4, m_scaleValue1,
			m_scaleValue2, m_scaleValue3, m_scaleValue4, m_outputVotes, m_outputMaxima, m_outputAdvanced,
			m_outputFeatureImg, m_outputNodeIdx };

	static SettingsModelString createColSelectModel() {
		return new SettingsModelString("image_column", "");
	}

	static SettingsModelIntegerBounded createPatchGapXModel() {
		return new SettingsModelIntegerBounded("gap_horizontal", 8, 0, Integer.MAX_VALUE);
	}

	static SettingsModelIntegerBounded createPatchGapYModel() {
		return new SettingsModelIntegerBounded("gap_vertical", 8, 0, Integer.MAX_VALUE);
	}

	static SettingsModelDoubleBounded createSigmaXYModel() {
		return new SettingsModelDoubleBounded("sigma_xy", 3.0, 0.0, Double.MAX_VALUE);
	}

	static SettingsModelDoubleBounded createSigmaZModel(final SettingsModelBoolean scale2Model) {
		final SettingsModelDoubleBounded settingsModelDoubleBounded = new SettingsModelDoubleBounded("sigma_z", 1.0,
				0.0, Double.MAX_VALUE);
		settingsModelDoubleBounded.setEnabled(false);
		scale2Model.addChangeListener(
				l -> settingsModelDoubleBounded.setEnabled(scale2Model.getBooleanValue() && scale2Model.isEnabled()));
		return settingsModelDoubleBounded;
	}

	static SettingsModelIntegerBounded createSpanIntervalBackprojectionModel() {
		return new SettingsModelIntegerBounded("span_area_backprojection", 15, 1, Integer.MAX_VALUE);
	}

	static SettingsModelBoolean createMultipleDetectionBoolModel() {
		return new SettingsModelBoolean("multiple_detection_bool", false);
	}

	static SettingsModelDoubleBounded createThresholdMultipleDetectionDoubleModel(
			final SettingsModelBoolean thresholdModel) {
		final SettingsModelDoubleBounded settingsModelDouble = new SettingsModelDoubleBounded(
				"multiple_detection_threshold", 1.0, 0.0, Double.MAX_VALUE);
		settingsModelDouble.setEnabled(false);
		thresholdModel.addChangeListener(l -> settingsModelDouble.setEnabled(thresholdModel.getBooleanValue()));
		return settingsModelDouble;
	}

	static SettingsModelDoubleBounded createMaxSuppressionMultipleDetectionDoubleModel(
			final SettingsModelBoolean thresholdModel) {
		final SettingsModelDoubleBounded settingsModelDouble = new SettingsModelDoubleBounded(
				"multiple_detection_max_suppression", 25.0, 0.0, Double.MAX_VALUE);
		settingsModelDouble.setEnabled(false);
		thresholdModel.addChangeListener(l -> settingsModelDouble.setEnabled(thresholdModel.getBooleanValue()));
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

	static SettingsModelBoolean createOutputFeatureImgBoolModel() {
		return new SettingsModelBoolean("is_feature_image", false);
	}

	static SettingsModelBoolean createOutputNodeIdxBoolModel() {
		return new SettingsModelBoolean("is_output_node_idx", false);
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

	/**
	 * @return the colImage
	 */
	public String getColImage() {
		return m_colImage.getStringValue();
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
	 * @return the spanIntervalBackprojection
	 */
	public int getSpanIntervalBackprojection() {
		return m_spanIntervalBackprojection.getIntValue();
	}

	/**
	 * @return the thresholdMultipleDetection
	 */
	public double getThresholdMultipleDetection() {
		return m_thresholdMultipleDetection.getDoubleValue();
	}

	/**
	 * @return the sigmaXY
	 */
	public double getSigmaXY() {
		return m_sigmaXY.getDoubleValue();
	}

	/**
	 * @return the sigmaZ
	 */
	public double getSigmaZ() {
		return m_sigmaZ.getDoubleValue();
	}

	/**
	 * @return the multipleDetection
	 */
	public boolean getMultipleDetection() {
		return m_multipleDetection.getBooleanValue();
	}

	/**
	 * @return the scales
	 */
	public double[] getScales() {
		return m_scales;
	}

	/**
	 * @return the outputVotes
	 */
	public boolean getOutputVotes() {
		return m_outputVotes.getBooleanValue();
	}

	/**
	 * @return the outputMaxima
	 */
	public boolean getOutputMaxima() {
		return m_outputMaxima.getBooleanValue();
	}

	/**
	 * @return the outputAdvanced
	 */
	public boolean getOutputAdvanced() {
		return m_outputAdvanced.getBooleanValue();
	}

	/**
	 * @return the outputFeatureImg
	 */
	public boolean getOutputFeatureImg() {
		return m_outputFeatureImg.getBooleanValue();
	}

	/**
	 * @return the outputNodeIdx
	 */
	public boolean getOutputNodeIdx() {
		return m_outputNodeIdx.getBooleanValue();
	}

	/**
	 * @return the maxSuppressionMultipleDetection
	 */
	public double getMaxSuppressionMultipleDetection() {
		return m_maxSuppressionMultipleDetection.getDoubleValue();
	}

}

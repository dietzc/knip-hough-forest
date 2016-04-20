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
package org.knime.knip.hough.ports;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.config.Config;
import org.knime.core.node.port.AbstractSimplePortObjectSpec;
import org.knime.knip.hough.features.FeatureDescriptor;
import org.knime.knip.hough.forest.HoughForest;

/**
 * The {@link AbstractSimplePortObjectSpec} for the {@link HoughForestModelPortObject}.
 * 
 * @author Simon Schmid, University of Konstanz
 */
public final class HoughForestModelPortObjectSpec extends AbstractSimplePortObjectSpec {

	public static final class HoughForestModelPortObjectSpecSerializer
			extends AbstractSimplePortObjectSpecSerializer<HoughForestModelPortObjectSpec> {
	}

	private static final String CFGKEY_NUM_TREES = "No. trees";

	private static final String CFGKEY_PATCH_WIDTH = "Patch width";

	private static final String CFGKEY_PATCH_HEIGHT = "Patch height";

	private static final String CFGKEY_FEATURES = "Features";

	private static final String CFGKEY_CONVERT_TO_LAB = "Convert to Lab color space";

	private static final String CFGKEY_ADD_1ST_DERIV = "Add 1st derivative";

	private static final String CFGKEY_USE_ABS_VALUE_1ST_DERIV = "Use absolute value of 1st derivative";

	private static final String CFGKEY_ADD_2ND_DERIV = "Add 2nd derivative";

	private static final String CFGKEY_USE_ABS_VALUE_2ND_DERIV = "Use absolute value of 2nd derivative";

	private static final String CFGKEY_ADD_HOG = "Add histogram of oriented gradients (HoG)";

	private static final String CFGKEY_HOG_NUM_BINS = "Num of bins (HoG)";

	private static final String CFGKEY_APPLY_MIN_MAX = "Apply a min and max filter";

	private static final String CFGKEY_USE_ABS_VALUES = "Use abosulte values";

	private int m_numTrees;

	private long m_patchWidth;

	private long m_patchHeight;

	private boolean m_convertToLab;

	private boolean m_addFirstDerivative;

	private boolean m_useAbsoluteFirstDerivative;

	private boolean m_addSecondDerivative;

	private boolean m_useAbsoluteSecondDerivative;

	private boolean m_addHoG;

	private int m_hogNumBins;

	private boolean m_applyMinMax;

	private boolean m_useAbsoluteValues;

	/** Framework constructor, not to be used by node itself. */
	public HoughForestModelPortObjectSpec() {
		// needed for loading
	}

	/**
	 * @param forest The {@link HoughForest} to display.
	 */
	public HoughForestModelPortObjectSpec(final HoughForest forest) {
		m_numTrees = forest.getListOfTrees().size();
		m_patchWidth = forest.getPatchSize()[0];
		m_patchHeight = forest.getPatchSize()[1];
		final FeatureDescriptor<?> featureDescriptor = forest.getFeatureDescriptor();
		m_convertToLab = featureDescriptor.isConvertToLab();
		m_addFirstDerivative = featureDescriptor.isAddFirstDerivative();
		m_useAbsoluteFirstDerivative = featureDescriptor.isUseAbsoluteFirstDerivative();
		m_addSecondDerivative = featureDescriptor.isAddSecondDerivative();
		m_useAbsoluteSecondDerivative = featureDescriptor.isUseAbsoluteSecondDerivative();
		m_addHoG = featureDescriptor.isAddHoG();
		m_hogNumBins = featureDescriptor.getHogNumBins();
		m_applyMinMax = featureDescriptor.isApplyMinMax();
		m_useAbsoluteValues = featureDescriptor.isUseAbsoluteValues();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void load(final ModelContentRO model) throws InvalidSettingsException {
		m_numTrees = model.getInt(CFGKEY_NUM_TREES);
		m_patchWidth = model.getLong(CFGKEY_PATCH_WIDTH);
		m_patchHeight = model.getLong(CFGKEY_PATCH_HEIGHT);
		final Config configFeatures = model.getConfig(CFGKEY_FEATURES);
		m_convertToLab = configFeatures.getBoolean(CFGKEY_CONVERT_TO_LAB);
		m_addFirstDerivative = configFeatures.getBoolean(CFGKEY_ADD_1ST_DERIV);
		m_useAbsoluteFirstDerivative = configFeatures.getBoolean(CFGKEY_USE_ABS_VALUE_1ST_DERIV);
		m_addSecondDerivative = configFeatures.getBoolean(CFGKEY_ADD_2ND_DERIV);
		m_useAbsoluteSecondDerivative = configFeatures.getBoolean(CFGKEY_USE_ABS_VALUE_2ND_DERIV);
		m_addHoG = configFeatures.getBoolean(CFGKEY_ADD_HOG);
		m_hogNumBins = configFeatures.getInt(CFGKEY_HOG_NUM_BINS);
		m_applyMinMax = configFeatures.getBoolean(CFGKEY_APPLY_MIN_MAX);
		m_useAbsoluteValues = configFeatures.getBoolean(CFGKEY_USE_ABS_VALUES);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void save(final ModelContentWO model) {
		model.addInt(CFGKEY_NUM_TREES, m_numTrees);
		model.addLong(CFGKEY_PATCH_WIDTH, m_patchWidth);
		model.addLong(CFGKEY_PATCH_HEIGHT, m_patchHeight);
		final Config configFeatures = model.addConfig(CFGKEY_FEATURES);
		configFeatures.addBoolean(CFGKEY_CONVERT_TO_LAB, m_convertToLab);
		configFeatures.addBoolean(CFGKEY_ADD_1ST_DERIV, m_addFirstDerivative);
		configFeatures.addBoolean(CFGKEY_USE_ABS_VALUE_1ST_DERIV, m_addFirstDerivative && m_useAbsoluteFirstDerivative);
		configFeatures.addBoolean(CFGKEY_ADD_2ND_DERIV, m_addSecondDerivative);
		configFeatures.addBoolean(CFGKEY_USE_ABS_VALUE_2ND_DERIV,
				m_addSecondDerivative && m_useAbsoluteSecondDerivative);
		configFeatures.addBoolean(CFGKEY_ADD_HOG, m_addHoG);
		configFeatures.addInt(CFGKEY_HOG_NUM_BINS, m_addHoG ? m_hogNumBins : 0);
		configFeatures.addBoolean(CFGKEY_APPLY_MIN_MAX, m_applyMinMax);
		configFeatures.addBoolean(CFGKEY_USE_ABS_VALUES, m_useAbsoluteValues);
	}

}
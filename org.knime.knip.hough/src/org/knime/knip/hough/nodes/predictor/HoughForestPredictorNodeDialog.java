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

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.knip.base.data.img.ImgPlusValue;

/**
 * The node dialog of the node which makes predictions based on a hough forest.
 * 
 * @author Simon Schmid, University of Konstanz
 */
final class HoughForestPredictorNodeDialog extends DefaultNodeSettingsPane {

	@SuppressWarnings("unchecked")
	public HoughForestPredictorNodeDialog() {
		// General
		createNewGroup("Input");
		addDialogComponent(new DialogComponentColumnNameSelection(HoughForestPredictorConfig.createColSelectModel(),
				"Image column", 1, true, ImgPlusValue.class));

		// Patch Extraction
		createNewGroup("Patch Extraction");
		addDialogComponent(new DialogComponentNumber(HoughForestPredictorConfig.createPatchGapXModel(),
				"Horizontal stride size", 1));
		addDialogComponent(new DialogComponentNumber(HoughForestPredictorConfig.createPatchGapYModel(),
				"Vertical stride size", 1));

		// Voting
		createNewGroup("Voting");
		setHorizontalPlacement(true);
		final SettingsModelBoolean scalesBoolModel1 = HoughForestPredictorConfig.createScalesBoolModel(1);
		addDialogComponent(new DialogComponentBoolean(scalesBoolModel1, "Scale 1"));
		final SettingsModelDouble scalesDoubleModel1 = HoughForestPredictorConfig.createScalesDoubleModel(1);
		addDialogComponent(new DialogComponentNumber(scalesDoubleModel1, "", 0.1));
		setHorizontalPlacement(false);

		setHorizontalPlacement(true);
		final SettingsModelBoolean scalesBoolModel2 = HoughForestPredictorConfig.createScalesBoolModel(2);
		addDialogComponent(new DialogComponentBoolean(scalesBoolModel2, "Scale 2"));
		final SettingsModelDouble scalesDoubleModel2 = HoughForestPredictorConfig.createScalesDoubleModel(2);
		addDialogComponent(new DialogComponentNumber(scalesDoubleModel2, "", 0.1));
		setHorizontalPlacement(false);

		setHorizontalPlacement(true);
		final SettingsModelBoolean scalesBoolModel3 = HoughForestPredictorConfig.createScalesBoolModel(3);
		addDialogComponent(new DialogComponentBoolean(scalesBoolModel3, "Scale 3"));
		final SettingsModelDouble scalesDoubleModel3 = HoughForestPredictorConfig.createScalesDoubleModel(3);
		addDialogComponent(new DialogComponentNumber(scalesDoubleModel3, "", 0.1));
		setHorizontalPlacement(false);

		setHorizontalPlacement(true);
		final SettingsModelBoolean scalesBoolModel4 = HoughForestPredictorConfig.createScalesBoolModel(4);
		addDialogComponent(new DialogComponentBoolean(scalesBoolModel4, "Scale 4"));
		final SettingsModelDouble scalesDoubleModel4 = HoughForestPredictorConfig.createScalesDoubleModel(4);
		addDialogComponent(new DialogComponentNumber(scalesDoubleModel4, "", 0.1));

		setHorizontalPlacement(false);
		addDialogComponent(new DialogComponentNumber(HoughForestPredictorConfig.createSigmaModel(), "Sigma", 1));

		// Detection
		createNewGroup("Detection");
		final SettingsModelBoolean multipleDetectionBoolModel = HoughForestPredictorConfig
				.createMultipleDetectionBoolModel();
		addDialogComponent(new DialogComponentBoolean(multipleDetectionBoolModel, "Detect multiple objects"));
		final SettingsModelDouble thresholdModel = HoughForestPredictorConfig
				.createThresholdMultipleDetectionDoubleModel(multipleDetectionBoolModel);
		addDialogComponent(new DialogComponentNumber(thresholdModel, "Threshold", 0.1));
		final SettingsModelDouble maxSuppressionModel = HoughForestPredictorConfig
				.createMaxSuppressionMultipleDetectionDoubleModel(multipleDetectionBoolModel);
		addDialogComponent(new DialogComponentNumber(maxSuppressionModel, "Max. suppression", 5.0));

		// Back Projection
		createNewGroup("Back Projection");
		addDialogComponent(new DialogComponentNumber(HoughForestPredictorConfig.createSpanIntervalBackprojectionModel(),
				"Size of the area around found maxima", 1.0));

		// Output
		createNewTab("Output");
		addDialogComponent(
				new DialogComponentBoolean(HoughForestPredictorConfig.createOutputVotesBoolModel(), "Votes"));
		addDialogComponent(
				new DialogComponentBoolean(HoughForestPredictorConfig.createOutputMaximaBoolModel(), "Maxima"));
		addDialogComponent(new DialogComponentBoolean(HoughForestPredictorConfig.createOutputFeatureImgBoolModel(),
				"Feature image"));
		addDialogComponent(new DialogComponentBoolean(HoughForestPredictorConfig.createOutputAdvancedBoolModel(),
				"Extended prediction labeling"));
		addDialogComponent(new DialogComponentBoolean(HoughForestPredictorConfig.createOutputNodeIdxBoolModel(),
				"Node index image"));

		/*
		 * Change listeners
		 */
		scalesBoolModel1.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				scalesDoubleModel1.setEnabled(scalesBoolModel1.getBooleanValue());
				scalesBoolModel2.setEnabled(scalesBoolModel1.getBooleanValue() && scalesBoolModel1.isEnabled());
				scalesDoubleModel2.setEnabled(scalesBoolModel2.getBooleanValue() && scalesBoolModel2.isEnabled());

			}
		});
		scalesBoolModel2.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				scalesDoubleModel2.setEnabled(scalesBoolModel2.getBooleanValue());
				scalesBoolModel3.setEnabled(scalesBoolModel2.getBooleanValue() && scalesBoolModel2.isEnabled());
				scalesDoubleModel3.setEnabled(scalesBoolModel3.getBooleanValue() && scalesBoolModel3.isEnabled());

			}
		});
		scalesBoolModel3.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				scalesDoubleModel3.setEnabled(scalesBoolModel3.getBooleanValue());
				scalesBoolModel4.setEnabled(scalesBoolModel3.getBooleanValue() && scalesBoolModel3.isEnabled());
				scalesDoubleModel4.setEnabled(scalesBoolModel4.getBooleanValue() && scalesBoolModel4.isEnabled());

			}
		});
		scalesBoolModel4.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				scalesDoubleModel4.setEnabled(scalesBoolModel4.getBooleanValue());

			}
		});

	}
}

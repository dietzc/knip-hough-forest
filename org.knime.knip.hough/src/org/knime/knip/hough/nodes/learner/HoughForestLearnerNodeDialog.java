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

import java.util.Random;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButton;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelLong;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;

/**
 * The node dialog of the node which learns a hough forest.
 * 
 * @author Simon Schmid, University of Konstanz
 */
final class HoughForestLearnerNodeDialog extends DefaultNodeSettingsPane {

	@SuppressWarnings("unchecked")
	public HoughForestLearnerNodeDialog() {

		createNewGroup("Input");
		addDialogComponent(new DialogComponentColumnNameSelection(HoughForestLearnerNodeModel.createColSelectionModel(),
				"Image column", 0, true, ImgPlusValue.class));
		addDialogComponent(
				new DialogComponentColumnNameSelection(HoughForestLearnerNodeModel.createLabelSelectionModel(),
						"Labeling column", 0, true, LabelingValue.class));

		createNewGroup("Tree Options");
		addDialogComponent(new DialogComponentNumber(HoughForestLearnerNodeModel.createNumSamplesModel(),
				"Size of sample per tree", 1));
		addDialogComponent(new DialogComponentNumber(HoughForestLearnerNodeModel.createNumSplitFunctionsModel(),
				"Number of split functions", 1));
		addDialogComponent(new DialogComponentNumber(HoughForestLearnerNodeModel.createThresholdModel(),
				"Max. threshold of split functions", 1));
		addDialogComponent(
				new DialogComponentNumber(HoughForestLearnerNodeModel.createDepthModel(), "Max. tree depth", 1));
		addDialogComponent(new DialogComponentNumber(HoughForestLearnerNodeModel.createMinSizeSampleModel(),
				"Min. size of sample", 1));

		createNewGroup("Forest Options");
		addDialogComponent(
				new DialogComponentNumber(HoughForestLearnerNodeModel.createNumTreesModel(), "Number of trees", 1));
		setHorizontalPlacement(true);
		final SettingsModelBoolean useSeedBoolModel = HoughForestLearnerNodeModel.createUseSeedBoolModel();
		addDialogComponent(new DialogComponentBoolean(useSeedBoolModel, "Use static random seed"));
		final SettingsModelLong seedModel = HoughForestLearnerNodeModel.createSeedLongModel();
		addDialogComponent(new DialogComponentNumberEdit(seedModel, null, 15));
		final DialogComponentButton seedButton = new DialogComponentButton("New");
		addDialogComponent(seedButton);

		setHorizontalPlacement(false);
		createNewGroup("Feature Selection");
		addDialogComponent(new DialogComponentBoolean(HoughForestLearnerNodeModel.createConvertToLabModel(),
				"Convert from RGB to Lab color space, if possible"));
		final SettingsModelBoolean fistDerivModel = HoughForestLearnerNodeModel.createFistDerivModel();
		addDialogComponent(new DialogComponentBoolean(fistDerivModel, "Add first derivatives"));
		final SettingsModelBoolean useAbsoluteFistDerivModel = HoughForestLearnerNodeModel
				.createUseAbsoluteFistDerivModel();
		addDialogComponent(
				new DialogComponentBoolean(useAbsoluteFistDerivModel, "Use absolute value for first derivatives"));
		final SettingsModelBoolean secondDerivModel = HoughForestLearnerNodeModel.createSecondDerivModel();
		addDialogComponent(new DialogComponentBoolean(secondDerivModel, "Add second derivatives"));
		final SettingsModelBoolean useAbsoluteSecondDerivModel = HoughForestLearnerNodeModel
				.createUseAbsoluteSecondDerivModel();
		addDialogComponent(
				new DialogComponentBoolean(useAbsoluteSecondDerivModel, "Use absolute value for second derivatives"));
		final SettingsModelBoolean hogBoolModel = HoughForestLearnerNodeModel.createHogBoolModel();
		addDialogComponent(new DialogComponentBoolean(hogBoolModel, "Add histogram of oriented gradients"));
		final SettingsModelInteger hogNumbBinsModel = HoughForestLearnerNodeModel.createHogNumbBinsModel();
		addDialogComponent(new DialogComponentNumber(hogNumbBinsModel, "Number of bins", 1));
		addDialogComponent(new DialogComponentBoolean(HoughForestLearnerNodeModel.createApplyMinMaxModel(),
				"Apply a min and max filter (doubles the feature dimension)"));
		addDialogComponent(new DialogComponentBoolean(HoughForestLearnerNodeModel.createUseAbsoluteModel(),
				"Use only absolute values"));

		createNewGroup("Patch Extraction");
		addDialogComponent(
				new DialogComponentNumber(HoughForestLearnerNodeModel.createPatchWidthModel(), "Patch width", 1));
		addDialogComponent(
				new DialogComponentNumber(HoughForestLearnerNodeModel.createPatchHeightModel(), "Patch height", 1));
		addDialogComponent(new DialogComponentNumber(HoughForestLearnerNodeModel.createPatchGapXModel(),
				"Horizontal stride size", 1));
		addDialogComponent(new DialogComponentNumber(HoughForestLearnerNodeModel.createPatchGapYModel(),
				"Vertical stride size", 1));

		/*
		 * Change Listeners
		 */
		useSeedBoolModel.addChangeListener(e -> {
			seedModel.setEnabled(useSeedBoolModel.getBooleanValue());
			seedButton.getComponentPanel().getComponent(0).setEnabled(useSeedBoolModel.getBooleanValue());
		});
		seedButton.addActionListener(e -> seedModel.setLongValue(new Random().nextLong()));

		fistDerivModel.addChangeListener(e -> {
			useAbsoluteFistDerivModel.setEnabled(fistDerivModel.getBooleanValue());
			secondDerivModel.setEnabled(fistDerivModel.getBooleanValue());
			useAbsoluteSecondDerivModel
					.setEnabled(fistDerivModel.getBooleanValue() && secondDerivModel.getBooleanValue());
		});

		secondDerivModel.addChangeListener(e -> {
			useAbsoluteSecondDerivModel
					.setEnabled(fistDerivModel.getBooleanValue() && secondDerivModel.getBooleanValue());
		});

		hogBoolModel.addChangeListener(e -> hogNumbBinsModel.setEnabled(hogBoolModel.getBooleanValue()));
	}
}

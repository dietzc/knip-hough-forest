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
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
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
		addDialogComponent(new DialogComponentColumnNameSelection(HoughForestLearnerConfig.createColSelectionModel(),
				"Image column", 0, true, ImgPlusValue.class));
		addDialogComponent(new DialogComponentColumnNameSelection(HoughForestLearnerConfig.createLabelSelectionModel(),
				"Labeling column", 0, true, LabelingValue.class));

		createNewGroup("Tree Options");
		addDialogComponent(new DialogComponentNumber(HoughForestLearnerConfig.createNumSamplesModel(),
				"Size of sample per tree", 1));
		addDialogComponent(new DialogComponentNumber(HoughForestLearnerConfig.createNumSplitFunctionsModel(),
				"Number of split functions", 1));
		addDialogComponent(
				new DialogComponentNumber(HoughForestLearnerConfig.createDepthModel(), "Max. tree depth", 1));
		addDialogComponent(new DialogComponentNumber(HoughForestLearnerConfig.createMinSizeSampleModel(),
				"Min. size of sample", 1));

		createNewGroup("Forest Options");
		addDialogComponent(
				new DialogComponentNumber(HoughForestLearnerConfig.createNumTreesModel(), "Number of trees", 1));
		setHorizontalPlacement(true);
		final SettingsModelBoolean useSeedBoolModel = HoughForestLearnerConfig.createUseSeedBoolModel();
		addDialogComponent(new DialogComponentBoolean(useSeedBoolModel, "Use static random seed"));
		final SettingsModelLong seedModel = HoughForestLearnerConfig.createSeedLongModel();
		addDialogComponent(new DialogComponentNumberEdit(seedModel, null, 15));
		final DialogComponentButton seedButton = new DialogComponentButton("New");
		addDialogComponent(seedButton);

		setHorizontalPlacement(false);
		createNewGroup("Feature Selection");
		addDialogComponent(new DialogComponentBoolean(HoughForestLearnerConfig.createConvertToLabModel(),
				"Convert from RGB to Lab color space, if possible"));
		final SettingsModelBoolean fistDerivModel = HoughForestLearnerConfig.createFistDerivModel();
		addDialogComponent(new DialogComponentBoolean(fistDerivModel, "Add first derivatives"));
		final SettingsModelBoolean useAbsoluteFistDerivModel = HoughForestLearnerConfig
				.createUseAbsoluteFistDerivModel();
		addDialogComponent(
				new DialogComponentBoolean(useAbsoluteFistDerivModel, "Use absolute value for first derivatives"));
		final SettingsModelBoolean secondDerivModel = HoughForestLearnerConfig.createSecondDerivModel();
		addDialogComponent(new DialogComponentBoolean(secondDerivModel, "Add second derivatives"));
		final SettingsModelBoolean useAbsoluteSecondDerivModel = HoughForestLearnerConfig
				.createUseAbsoluteSecondDerivModel();
		addDialogComponent(
				new DialogComponentBoolean(useAbsoluteSecondDerivModel, "Use absolute value for second derivatives"));
		final SettingsModelBoolean hogBoolModel = HoughForestLearnerConfig.createHogBoolModel();
		addDialogComponent(new DialogComponentBoolean(hogBoolModel, "Add histogram of oriented gradients"));
		final SettingsModelInteger hogNumbBinsModel = HoughForestLearnerConfig.createHogNumbBinsModel();
		addDialogComponent(new DialogComponentNumber(hogNumbBinsModel, "Number of bins", 1));
		addDialogComponent(new DialogComponentBoolean(HoughForestLearnerConfig.createApplyMinMaxModel(),
				"Apply a min and max filter (doubles the feature dimension)"));
		addDialogComponent(new DialogComponentBoolean(HoughForestLearnerConfig.createUseAbsoluteModel(),
				"Use only absolute values"));

		createNewGroup("Patch Extraction");
		addDialogComponent(
				new DialogComponentNumber(HoughForestLearnerConfig.createPatchWidthModel(), "Patch width", 1));
		addDialogComponent(
				new DialogComponentNumber(HoughForestLearnerConfig.createPatchHeightModel(), "Patch height", 1));
		addDialogComponent(new DialogComponentNumber(HoughForestLearnerConfig.createPatchGapXModel(),
				"Horizontal stride size", 1));
		addDialogComponent(
				new DialogComponentNumber(HoughForestLearnerConfig.createPatchGapYModel(), "Vertical stride size", 1));

		// Entanglement
		createNewTab("Entanglement");

		createNewGroup("General Settings");
		final SettingsModelBoolean entanglementModel = HoughForestLearnerConfig.createEntanglementModel();
		addDialogComponent(new DialogComponentBoolean(entanglementModel, "Enable entanglement"));

		final SettingsModelDoubleBounded ratioEntanglementModel = HoughForestLearnerConfig
				.createRatioEntanglementModel();
		addDialogComponent(
				new DialogComponentNumber(ratioEntanglementModel, "Ratio of entangled split functions", 0.1));

		final SettingsModelIntegerBounded horizontalMinOffsetModel = HoughForestLearnerConfig
				.createHorizontalMinOffsetModel();
		addDialogComponent(new DialogComponentNumber(horizontalMinOffsetModel, "Horizontal min. offset", 5));

		final SettingsModelIntegerBounded horizontalMaxOffsetModel = HoughForestLearnerConfig
				.createHorizontalMaxOffsetModel();
		addDialogComponent(new DialogComponentNumber(horizontalMaxOffsetModel, "Horizontal max. offset", 5));

		final SettingsModelIntegerBounded createVerticalMinOffsetModel = HoughForestLearnerConfig
				.createVerticalMinOffsetModel();
		addDialogComponent(new DialogComponentNumber(createVerticalMinOffsetModel, "Vertical min. offset", 5));

		final SettingsModelIntegerBounded verticalMaxOffsetModel = HoughForestLearnerConfig
				.createVerticalMaxOffsetModel();
		addDialogComponent(new DialogComponentNumber(verticalMaxOffsetModel, "Vertical max. offset", 5));

		createNewGroup("Entangled Split Functions");
		final SettingsModelBoolean useMapClassSplitFunctionModel = HoughForestLearnerConfig
				.createUseMapClassSplitFunctionModel();
		addDialogComponent(new DialogComponentBoolean(useMapClassSplitFunctionModel, "Use MAPClass split function"));

		final SettingsModelBoolean useNodeDescendantSplitFunctionModel = HoughForestLearnerConfig
				.createUseNodeDescendantSplitFunctionModel();
		addDialogComponent(
				new DialogComponentBoolean(useNodeDescendantSplitFunctionModel, "Use NodeDescendant split function"));

		final SettingsModelBoolean useAncestorNodePairSplitFunctionModel = HoughForestLearnerConfig
				.createUseAncestorNodePairSplitFunctionModel();
		addDialogComponent(new DialogComponentBoolean(useAncestorNodePairSplitFunctionModel,
				"Use AncestorNodePair split function"));

		final SettingsModelIntegerBounded ancestorNodePairThresholdModel = HoughForestLearnerConfig
				.createAncestorNodePairThresholdModel(useAncestorNodePairSplitFunctionModel);
		addDialogComponent(new DialogComponentNumber(ancestorNodePairThresholdModel, "Max. threshold", 1));

		final SettingsModelBoolean useOffsetNodePairSplitFunctionModel = HoughForestLearnerConfig
				.createUseOffsetNodePairSplitFunctionModel();
		addDialogComponent(
				new DialogComponentBoolean(useOffsetNodePairSplitFunctionModel, "Use OffsetNodePair split function"));

		final SettingsModelDoubleBounded offsetSimilarityNodePairSigmaModel = HoughForestLearnerConfig
				.createOffsetSimilarityNodePairSigmaModel(useOffsetNodePairSplitFunctionModel);
		addDialogComponent(new DialogComponentNumber(offsetSimilarityNodePairSigmaModel, "Sigma", 1.0));

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

		entanglementModel.addChangeListener(l -> {
			final boolean isEntangled = entanglementModel.getBooleanValue();
			ratioEntanglementModel.setEnabled(isEntangled);
			horizontalMinOffsetModel.setEnabled(isEntangled);
			horizontalMaxOffsetModel.setEnabled(isEntangled);
			createVerticalMinOffsetModel.setEnabled(isEntangled);
			verticalMaxOffsetModel.setEnabled(isEntangled);
			useMapClassSplitFunctionModel.setEnabled(isEntangled);
			useNodeDescendantSplitFunctionModel.setEnabled(isEntangled);
			useAncestorNodePairSplitFunctionModel.setEnabled(isEntangled);
			useOffsetNodePairSplitFunctionModel.setEnabled(isEntangled);
		});
	}

}

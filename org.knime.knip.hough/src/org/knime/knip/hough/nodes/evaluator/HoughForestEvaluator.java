package org.knime.knip.hough.nodes.evaluator;

import java.util.HashMap;
import java.util.Map;

import org.knime.core.node.NodeLogger;
import org.knime.knip.hough.forest.node.LeafNode;
import org.knime.knip.hough.forest.node.Node;
import org.knime.knip.hough.forest.node.SplitNode;
import org.knime.knip.hough.forest.split.AncestorNodePairSplitFunction;
import org.knime.knip.hough.forest.split.MAPClassSplitFunction;
import org.knime.knip.hough.forest.split.NodeDescendantSplitFunction;
import org.knime.knip.hough.forest.split.SplitFunction;

import net.imagej.ops.OpService;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.IntType;

public final class HoughForestEvaluator {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(HoughForestEvaluator.class);

	public static RandomAccessibleInterval<IntType> createNodeIdxImage(final Node[][][] nodeGrid, final OpService ops) {
		final RandomAccessibleInterval<IntType> img = ops.create()
				.img(new FinalInterval(nodeGrid[0].length, nodeGrid[0][0].length, nodeGrid.length), new IntType());
		final RandomAccess<IntType> raImg = img.randomAccess();
		final Map<Integer, Integer> values = new HashMap<>();
		int counter = 0;
		for (int channel = 0; channel < nodeGrid.length; channel++) {
			for (int i = 0; i < nodeGrid[0].length; i++) {
				for (int j = 0; j < nodeGrid[0][0].length; j++) {
					final int nodeIdx = nodeGrid[channel][i][j].getNodeIdx();
					if (!values.containsKey(nodeIdx)) {
						values.put(nodeIdx, counter++);
					}
					raImg.setPosition(new int[] { i, j, channel });
					raImg.get().set(values.get(nodeIdx));
					// raImg.get().set(nodeIdx);
				}
			}
		}

		// TODO normalize or colormap
		return img;
	}

	// https://stackoverflow.com/questions/4965335/how-to-print-binary-tree-diagram
	public static void printTree(final Node node, String prefix, boolean isTail) {
		StringBuilder string = new StringBuilder();
		string.append(String.format("P(0)=%3.2f, P(1)=%3.2f ;", node.getProbability(0), node.getProbability(1)));
		if (node instanceof SplitNode) {
			SplitNode sn = (SplitNode) node;

			SplitFunction sf = sn.getSplitFunction();
			string.append(sf.getName());
			if (sf instanceof MAPClassSplitFunction) {
				string.append(": [" + ((MAPClassSplitFunction) sf).getOffset()[0] + ","
						+ ((MAPClassSplitFunction) sf).getOffset()[1] + "]");
				string.append(", " + ((MAPClassSplitFunction) sf).getClazz());
			} else if (sf instanceof NodeDescendantSplitFunction) {
				string.append(": [" + ((NodeDescendantSplitFunction) sf).getOffset()[0] + ","
						+ ((NodeDescendantSplitFunction) sf).getOffset()[1] + "]");
				string.append(", " + ((NodeDescendantSplitFunction) sf).getNodeIdx());
			} else if (sf instanceof AncestorNodePairSplitFunction) {
				string.append(": " + ((AncestorNodePairSplitFunction) sf).getThreshold());
			}
			final String print = prefix + (isTail ? "RS-- " : "LS-- ") + node.getNodeIdx() + "; " + string;
			System.out.println(print);
			LOGGER.info(print);
			printTree(sn.getLeftChild(), prefix + (isTail ? "    " : "|   "), false);
			printTree(sn.getRightChild(), prefix + (isTail ? "    " : "|   "), true);
		} else {
			LeafNode ln = (LeafNode) node;
			final String print = prefix + (isTail ? "RL-- " : "LL-- ") + node.getNodeIdx() + "; " + string
					+ (ln.getNumElementsOfClazz0() + ln.getNumElementsOfClazz1())
					+ String.format("; [%3.2f, %3.2f]", node.getOffsetMean()[0], node.getOffsetMean()[1]);
			System.out.println(print);
			LOGGER.info(print);
		}
	}
}

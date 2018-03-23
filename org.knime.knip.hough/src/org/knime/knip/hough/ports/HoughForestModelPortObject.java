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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.swing.JComponent;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.port.AbstractPortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;
import org.knime.knip.hough.forest.HoughForest;

/**
 * The {@link AbstractPortObject} for a {@link HoughForest}.
 * 
 * @author Simon Schmid, University of Konstanz
 */
public final class HoughForestModelPortObject extends AbstractPortObject {

	private HoughForest m_forest;

	private HoughForestModelPortObjectSpec m_spec;

	public static class HoughForestModelPortObjectSerializer extends PortObjectSerializer<HoughForestModelPortObject> {

		@Override
		public void savePortObject(HoughForestModelPortObject portObject, PortObjectZipOutputStream out,
				ExecutionMonitor exec) throws IOException, CanceledExecutionException {
			portObject.save(out, exec);
		}

		@Override
		public HoughForestModelPortObject loadPortObject(PortObjectZipInputStream in, PortObjectSpec spec,
				ExecutionMonitor exec) throws IOException, CanceledExecutionException {
			final HoughForestModelPortObject obj = new HoughForestModelPortObject();
			obj.load(in, spec, exec);
			return obj;
		}
	}

	/**
	 * Creates a parameterized object of this class.
	 * 
	 * @param forest the {@link HoughForest}
	 */
	public HoughForestModelPortObject(final HoughForest forest, final long seed) {
		m_forest = forest;
		m_spec = new HoughForestModelPortObjectSpec(forest);
	}

	private HoughForestModelPortObject() {

	}

	@Override
	public String getSummary() {
		return null;
	}

	@Override
	public PortObjectSpec getSpec() {
		if (m_spec == null)
			m_spec = new HoughForestModelPortObjectSpec();
		return m_spec;
	}

	@Override
	public JComponent[] getViews() {
		return null;
	}

	@Override
	protected void save(PortObjectZipOutputStream out, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		try (final ObjectOutputStream oo = new ObjectOutputStream(out)) {
			oo.writeObject(m_forest);
		}
	}

	@Override
	protected void load(PortObjectZipInputStream in, PortObjectSpec spec, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		try (final ObjectInputStream io = new ObjectInputStream(in)) {
			m_forest = (HoughForest) io.readObject();
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		m_spec = (HoughForestModelPortObjectSpec) spec;
	}

	/**
	 * @return the forest
	 */
	public HoughForest getForest() {
		return m_forest;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((m_forest == null) ? 0 : m_forest.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof HoughForestModelPortObject)) {
			return false;
		}
		HoughForestModelPortObject other = (HoughForestModelPortObject) obj;
		if (m_forest == null) {
			if (other.m_forest != null) {
				return false;
			}
		} else if (!m_forest.equals(other.m_forest)) {
			return false;
		}
		return true;
	}

}

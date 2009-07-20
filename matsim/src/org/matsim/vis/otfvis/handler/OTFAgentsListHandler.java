/* *********************************************************************** *
 * project: org.matsim.*
 * OTFAgentsListHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.vis.otfvis.handler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.utils.misc.ByteBufferUtils;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.data.OTFServerQuad;
import org.matsim.vis.otfvis.data.OTFDataReceiver;
import org.matsim.vis.otfvis.data.OTFDataSimpleAgentReceiver;
import org.matsim.vis.otfvis.interfaces.OTFDataReader;
import org.matsim.vis.snapshots.writers.PositionInfo;

public class OTFAgentsListHandler extends OTFDataReader {

	static {
		OTFDataReader.setPreviousVersion(OTFAgentsListHandler.class.getCanonicalName() + "V1.1", ReaderV1_2.class);
		OTFDataReader.setPreviousVersion(OTFAgentsListHandler.class.getCanonicalName() + "V1.2", ReaderV1_2.class);
	}

	protected Class agentReceiverClass = null;

	protected List<OTFDataSimpleAgentReceiver> agents = new LinkedList<OTFDataSimpleAgentReceiver>();
	public static class ExtendedPositionInfo extends PositionInfo {

		int type = 0;
		int user = 0;

		public ExtendedPositionInfo(Id driverId, double easting, double northing, double elevation, double azimuth, double speed, VehicleState vehicleState, int type, int userdata) {
			super(driverId, easting, northing, elevation, azimuth, speed, vehicleState, "");
			this.type = type;
			this.user = userdata;
		}
		public ExtendedPositionInfo(PositionInfo i, int type, int userdata) {
			super(i.getAgentId(), i.getEasting(), i.getNorthing(), i.getElevation(), i.getAzimuth(), i.getSpeed(), i.getVehicleState(), "");
			this.type = type;
			this.user = userdata;
		}

	}

	@SuppressWarnings("unchecked")
	static public class Writer extends OTFDataWriter {

		private static final long serialVersionUID = -6368752578878835954L;

		public transient Collection<ExtendedPositionInfo> positions = new ArrayList<ExtendedPositionInfo>();

		@Override
		public void writeConstData(ByteBuffer out) throws IOException {
		}

		public void writeAgent(ExtendedPositionInfo pos, ByteBuffer out) {
			String id = pos.getAgentId().toString();
			ByteBufferUtils.putString(out, id);
			out.putFloat((float)(pos.getEasting() - OTFServerQuad.offsetEast));
			out.putFloat((float)(pos.getNorthing()- OTFServerQuad.offsetNorth));
			out.putInt(pos.type);
			out.putInt(pos.user);
			out.putFloat((float)pos.getSpeed());
		}

		@Override
		public void writeDynData(ByteBuffer out) throws IOException {
			// Write additional agent data
			out.putInt(positions.size());

			for (ExtendedPositionInfo pos : positions) {
				writeAgent(pos, out);
			}
			positions.clear();
		}

	}

	public void readAgent(ByteBuffer in, SceneGraph graph) {
		String id = ByteBufferUtils.getString(in);
		float x = in.getFloat();
		float y = in.getFloat();
		int type = in.getInt();
		int user = in.getInt();
		// Convert to km/h
		float speed = in.getFloat()*3.6f;

			OTFDataSimpleAgentReceiver drawer = null;
			try {
				drawer = (OTFDataSimpleAgentReceiver) graph.newInstance(agentReceiverClass);
				drawer.setAgent(id.toCharArray(), x, y, type, user, speed);
				agents.add(drawer);
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} //factoryAgent.getOne();

 	}


	@Override
	public void readDynData(ByteBuffer in, SceneGraph graph) throws IOException {
		// read additional agent data
		agents.clear();

		int count = in.getInt();
		for(int i= 0; i< count; i++) readAgent(in, graph);
	}

	@Override
	public void readConstData(ByteBuffer in) throws IOException {
	}


	@Override
	public void connect(OTFDataReceiver receiver) {
		//connect agent receivers
		if (receiver  instanceof OTFDataSimpleAgentReceiver) {
			this.agentReceiverClass = receiver.getClass();
		}

	}


	@Override
	public void invalidate(SceneGraph graph) {
		// invalidate agent receivers
		for(OTFDataSimpleAgentReceiver agent : agents) agent.invalidate(graph);
	}


	/***
	 * PREVIOUS VERSION of the reader
	 * @author dstrippgen
	 *
	 */
	public static final class ReaderV1_2 extends OTFAgentsListHandler {

		@Override
		public void readAgent(ByteBuffer in, SceneGraph graph) {
			String id = ByteBufferUtils.getString(in);
			float x = in.getFloat();
			float y = in.getFloat();
			int state = in.getInt();
			// Convert to km/h
			float color = in.getFloat()*3.6f;

				OTFDataSimpleAgentReceiver drawer = null;
				try {
					drawer = (org.matsim.vis.otfvis.data.OTFDataSimpleAgentReceiver) graph.newInstance(agentReceiverClass);
					drawer.setAgent(id.toCharArray(), x, y, 0, state, color);
					agents.add(drawer);
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} //factoryAgent.getOne();
				// at this version, only userdata was defined... aka state

	 	}
	}
}

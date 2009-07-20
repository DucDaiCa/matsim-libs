/* *********************************************************************** *
 * project: org.matsim.*
 * OTFDataReader.java
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

package org.matsim.vis.otfvis.interfaces;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.data.OTFDataReceiver;


public abstract class  OTFDataReader {
	public final static Map<String, Class> previousVersions = new HashMap<String, Class>();
	
	public static Class getPreviousVersion(String identifier) {
		return previousVersions.get(identifier);
	}
	
	public static void setPreviousVersion(String identifier, Class clazz) {
		previousVersions.put(identifier, clazz);
	}
	public static String getVersionString(int major, int minor) {
		return "V" + major + "." + minor;
	}
	
	private OTFDataWriter src;
	public void setSrc(OTFDataWriter src) {
		this.src = src;
	}
	public OTFDataWriter getSrc() {
		return this.src;
	}
	public abstract void readConstData(ByteBuffer in) throws IOException;
	public abstract void readDynData(ByteBuffer in, SceneGraph graph) throws IOException;
	public abstract void connect(OTFDataReceiver receiver);
	public abstract void invalidate(SceneGraph graph);
}


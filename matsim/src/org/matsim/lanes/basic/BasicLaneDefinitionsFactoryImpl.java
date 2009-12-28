/* *********************************************************************** *
 * project: org.matsim.*
 * BasicLaneDefinitionsBuilderImpl
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.lanes.basic;

import org.matsim.api.core.v01.Id;


/**
 * 
 * @author dgrether
 * @see org.matsim.lanes.basic.BasicLaneDefinitionsFactory
 */
public class BasicLaneDefinitionsFactoryImpl implements BasicLaneDefinitionsFactory {
	/**
	 * @see org.matsim.lanes.basic.BasicLaneDefinitionsFactory#createLanesToLinkAssignment(org.matsim.api.core.v01.Id)
	 */
	public BasicLanesToLinkAssignment createLanesToLinkAssignment(Id linkIdReference) {
		return new BasicLanesToLinkAssignmentImpl(linkIdReference);
	}
	/**
	 * @see org.matsim.lanes.basic.BasicLaneDefinitionsFactory#createLane(org.matsim.api.core.v01.Id)
	 */
	public BasicLane createLane(Id id) {
		return new BasicLaneImpl(id);
	}
}

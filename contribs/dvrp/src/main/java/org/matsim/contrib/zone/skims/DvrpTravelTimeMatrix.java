/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.zone.skims;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.trafficmonitoring.QSimFreeSpeedTravelTime;
import org.matsim.contrib.zone.SquareGridSystem;
import org.matsim.contrib.zone.ZonalSystems;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DvrpTravelTimeMatrix {
	private final SquareGridSystem gridSystem;
	private final Matrix freeSpeedTravelTimeMatrix;

	public DvrpTravelTimeMatrix(Network dvrpNetwork, DvrpTravelTimeMatrixParams params, int numberOfThreads,
			double qSimTimeStepSize) {
		gridSystem = new SquareGridSystem(dvrpNetwork.getNodes().values(), params.getCellSize());
		var centralNodes = ZonalSystems.computeMostCentralNodes(dvrpNetwork.getNodes().values(), gridSystem);
		var travelTime = new QSimFreeSpeedTravelTime(qSimTimeStepSize);
		var travelDisutility = new TimeAsTravelDisutility(travelTime);
		freeSpeedTravelTimeMatrix = TravelTimeMatrices.calculateTravelTimeMatrix(dvrpNetwork, centralNodes, 0,
				travelTime, travelDisutility, numberOfThreads);
	}

	public int getFreeSpeedTravelTime(Node fromNode, Node toNode) {
		if (fromNode == toNode) {
			return 0;
		}
		return freeSpeedTravelTimeMatrix.get(gridSystem.getZone(fromNode), gridSystem.getZone(toNode));
	}
}

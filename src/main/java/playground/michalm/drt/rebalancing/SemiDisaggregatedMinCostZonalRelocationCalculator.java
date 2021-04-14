/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package playground.michalm.drt.rebalancing;

import java.util.List;
import java.util.Map;

import org.matsim.contrib.drt.analysis.zonal.DrtZone;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy.Relocation;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.AggregatedMinCostRelocationCalculator.DrtZoneVehicleSurplus;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.ZonalRelocationCalculator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

/**
 * @author michalm
 */
public class SemiDisaggregatedMinCostZonalRelocationCalculator implements ZonalRelocationCalculator {
	@Override
	public List<Relocation> calcRelocations(List<DrtZoneVehicleSurplus> vehicleSurpluses,
			Map<DrtZone, List<DvrpVehicle>> rebalancableVehiclesPerZone) {

		// The idea is to expand each supply node 's' into a sub-tree 't' consisting of edges (each of capacity 1)
		// going from 's' to each rebalancable vehicle located in the zone represented by 's',
		// and replacing supply->demand edges by vehicle->demand edges (each of capacity 1)

		// This requires implementing a two-level transport problem.

		throw new UnsupportedOperationException("Not implemented yet");
	}
}

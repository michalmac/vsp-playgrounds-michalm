/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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

package playground.michalm.drt.run;

import java.util.Collections;
import java.util.Random;
import java.util.stream.Collectors;

import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.trafficmonitoring.QSimFreeSpeedTravelTime;
import org.matsim.contrib.zone.skims.DvrpTravelTimeMatrixParams;
import org.matsim.contrib.zone.skims.TravelTimeMatrices;
import org.matsim.core.network.NetworkUtils;

/**
 * @author Michal Maciejewski (michalm)
 */
public class RunMatrixOnBerlinNetwork {
	public static void main(String[] args) {
		String networkFile = "d:/matsim-repos/shared-svn/projects/audi_av/scenario/network_reduced_cleaned.xml.gz";
		//String networkFile = "d:/matsim-intelliJ/matsim-maas/scenarios/mielec_2014_02/network.xml";
		var dvrpNetwork = NetworkUtils.readNetwork(networkFile);

		var matrixParams = new DvrpTravelTimeMatrixParams();
		matrixParams.setMaxNeighborDistance(4000);

		int numberOfThreads = 12;// Runtime.getRuntime().availableProcessors();

		var travelTime = new QSimFreeSpeedTravelTime(1);
		var travelDisutility = new TimeAsTravelDisutility(travelTime);

		var freeSpeedTravelTimeSparseMatrix = TravelTimeMatrices.calculateTravelTimeSparseMatrix(dvrpNetwork,
				matrixParams.getMaxNeighborDistance(), 0, travelTime, travelDisutility, numberOfThreads);
		var fromIndices = dvrpNetwork.getNodes()
				.values()
				.stream()
				.map(value -> value.getId().index())
				.collect(Collectors.toList());
		Collections.shuffle(fromIndices, new Random(1234));
		var fromArray = fromIndices.stream().mapToInt(Integer::intValue).toArray();

		var toIndices = dvrpNetwork.getNodes()
				.values()
				.stream()
				.map(value -> value.getId().index())
				.collect(Collectors.toList());
		Collections.shuffle(toIndices, new Random(5678));
		var toArray = toIndices.stream().mapToInt(Integer::intValue).toArray();

		System.out.println("Number of nodes: " + fromArray.length);

		int count = fromArray.length;

		for (int shift = 0; shift < 1000; shift++) {
			for (int i = 0; i < count; i++) {
				int from = fromArray[i];
				int to = toArray[(i + shift) % count];
				freeSpeedTravelTimeSparseMatrix.get(from, to);
				freeSpeedTravelTimeSparseMatrix.get(to, from);
			}
		}

		long t0 = System.currentTimeMillis();
		for (int shift = 1000; shift < 6000; shift++) {
			for (int i = 0; i < count; i++) {
				int from = fromArray[i];
				int to = toArray[(i + shift) % count];
				freeSpeedTravelTimeSparseMatrix.get(from, to);
				freeSpeedTravelTimeSparseMatrix.get(to, from);
			}
		}
		long t1 = System.currentTimeMillis();

		double time = t1 - t0;
		int operations = 2 * count * 5000;
		System.out.println("Computation time: " + (time / 1000));
		System.out.println("Queries per second: " + operations / time);
	}
}

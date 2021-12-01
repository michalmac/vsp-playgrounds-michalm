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

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.function.ToDoubleFunction;
import java.util.stream.IntStream;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.util.DistanceUtils;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.trafficmonitoring.QSimFreeSpeedTravelTime;
import org.matsim.contrib.util.ExecutorServiceWithResource;
import org.matsim.contrib.zone.skims.DvrpTravelTimeMatrix;
import org.matsim.contrib.zone.skims.DvrpTravelTimeMatrixParams;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.speedy.LeastCostPathTree;
import org.matsim.core.router.speedy.SpeedyGraph;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.OptionalTime;

import com.google.common.base.Preconditions;
import com.google.common.math.StatsAccumulator;

/**
 * @author Michal Maciejewski (michalm)
 */
public class CombineZonalAndBeeLineTTs {
	public static void main(String[] args) {
		String networkFile = "d:/matsim-repos/shared-svn/projects/audi_av/scenario/network_reduced_cleaned.xml.gz";
		//String networkFile = "d:/matsim-intelliJ/matsim-maas/scenarios/mielec_2014_02/network.xml";
		var network = NetworkUtils.readTimeInvariantNetwork(networkFile);

		var matrixParams = new DvrpTravelTimeMatrixParams();
		matrixParams.setCellSize(100);
		matrixParams.setMaxNeighborDistance(1000);

		int numberOfThreads = 12;// Runtime.getRuntime().availableProcessors();

		var dvrpTTMatrix = DvrpTravelTimeMatrix.createFreeSpeedMatrix(network, matrixParams, numberOfThreads, 1);

		Counter counter = new Counter("DVRP free-speed trees: node ", " / " + network.getNodes().size());

		SpeedyGraph graph = new SpeedyGraph(network);
		var executorService = new ExecutorServiceWithResource<>(IntStream.range(0, numberOfThreads)
				.mapToObj(i -> new LeastCostPathTree(graph, new QSimFreeSpeedTravelTime(1),
						new TimeAsTravelDisutility(new QSimFreeSpeedTravelTime(1))))
				.collect(toList()));

		AllStats combinedAllStats = new AllStats();
		var context = new Context(network, 0, counter, dvrpTTMatrix, combinedAllStats);

		executorService.submitRunnablesAndWait(network.getNodes()
				.values()
				.stream()
				.map(node -> (lpcTree -> computeDistancesForAllOutgoingNodes(node, lpcTree, context))));
		executorService.shutdown();

		String prefix = matrixParams.getCellSize() + "_" + matrixParams.getMaxNeighborDistance() + "_";

		saveToCsv(prefix + "statsByNetworkTT_mean.csv", combinedAllStats.statsByNetworkTT, StatsAccumulator::mean);
		saveToCsv(prefix + "statsByBeelineDistance_mean.csv", combinedAllStats.statsByBeelineDistance,
				StatsAccumulator::mean);
		saveToCsv(prefix + "statsByNetworkDistance_mean.csv", combinedAllStats.statsByNetworkDistance,
				StatsAccumulator::mean);
		saveToCsv(prefix + "statsByZonalTT_mean.csv", combinedAllStats.statsByZonalTT, StatsAccumulator::mean);

		saveToCsv(prefix + "statsByNetworkTT_std.csv", combinedAllStats.statsByNetworkTT,
				StatsAccumulator::populationStandardDeviation);
		saveToCsv(prefix + "statsByBeelineDistance_std.csv", combinedAllStats.statsByBeelineDistance,
				StatsAccumulator::populationStandardDeviation);
		saveToCsv(prefix + "statsByNetworkDistance_std.csv", combinedAllStats.statsByNetworkDistance,
				StatsAccumulator::populationStandardDeviation);
		saveToCsv(prefix + "statsByZonalTT_std.csv", combinedAllStats.statsByZonalTT,
				StatsAccumulator::populationStandardDeviation);
	}

	private static void saveToCsv(String filename, BinnedAccumulatedStats binnedStats,
			ToDoubleFunction<StatsAccumulator> function) {
		try (var pw = new PrintWriter(filename)) {
			pw.printf("bin,count,networkTT,beelineDistance,networkDistance,zonalTT,hybridTT%n");
			for (int i = 0; i < binnedStats.networkTT.length; i++) {
				int binValue = i * binnedStats.binSize;
				long count = binnedStats.networkTT[i].count();
				if (count > 0) {
					pw.printf("%d,%d,%f,%f,%f,%f,%f%n", binValue, count,
							function.applyAsDouble(binnedStats.networkTT[i]),
							function.applyAsDouble(binnedStats.beelineDistance[i]),
							function.applyAsDouble(binnedStats.networkDistance[i]),
							function.applyAsDouble(binnedStats.zonalTT[i]),
							function.applyAsDouble(binnedStats.hybridTT[i]));
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	record Context(Network network, double departureTime, Counter counter, DvrpTravelTimeMatrix travelTimeMatrix,
				   AllStats allStatsAccumulator) {
	}

	record Sample(double networkTT, double networkDistance, double zonalTT, double beelineDistance, double hybridTT) {
	}

	private static final double MAX_DISTANCE = 7_500;
	private static final double MAX_TIME = 15 * 60;
	private static final int DISTANCE_BIN = 50 * 2;
	private static final int TIME_BIN = 3 * 2;

	static class AllStats {
		// time: 10 hours, 30 s bins
		// distance: 200 km, 100 m bins
		private final BinnedAccumulatedStats statsByNetworkTT = new BinnedAccumulatedStats(TIME_BIN, MAX_TIME);
		private final BinnedAccumulatedStats statsByBeelineDistance = new BinnedAccumulatedStats(DISTANCE_BIN,
				MAX_DISTANCE);
		private final BinnedAccumulatedStats statsByNetworkDistance = new BinnedAccumulatedStats(DISTANCE_BIN,
				MAX_DISTANCE);
		private final BinnedAccumulatedStats statsByZonalTT = new BinnedAccumulatedStats(TIME_BIN, MAX_TIME);

		private void addSample(Sample sample) {
			statsByNetworkTT.addSample(sample, sample.networkTT);
			statsByBeelineDistance.addSample(sample, sample.beelineDistance);
			statsByNetworkDistance.addSample(sample, sample.networkDistance);
			statsByZonalTT.addSample(sample, sample.zonalTT);
		}

		//accumulated from different threads
		private synchronized void accumulate(AllStats other) {
			statsByNetworkTT.combine(other.statsByNetworkTT);
			statsByBeelineDistance.combine(other.statsByBeelineDistance);
			statsByNetworkDistance.combine(other.statsByNetworkDistance);
			statsByZonalTT.combine(other.statsByZonalTT);
		}
	}

	static class BinnedAccumulatedStats {
		private final int binSize;

		private final StatsAccumulator[] networkTT;
		private final StatsAccumulator[] networkDistance;
		private final StatsAccumulator[] zonalTT;
		private final StatsAccumulator[] beelineDistance;
		private final StatsAccumulator[] hybridTT;

		BinnedAccumulatedStats(int binSize, double maxValue) {
			this.binSize = binSize;
			int binCount = (int)Math.ceil(maxValue / binSize) * 20;

			networkTT = new StatsAccumulator[binCount];
			networkDistance = new StatsAccumulator[binCount];
			zonalTT = new StatsAccumulator[binCount];
			beelineDistance = new StatsAccumulator[binCount];
			hybridTT = new StatsAccumulator[binCount];

			for (int i = 0; i < binCount; i++) {
				networkTT[i] = new StatsAccumulator();
				networkDistance[i] = new StatsAccumulator();
				zonalTT[i] = new StatsAccumulator();
				beelineDistance[i] = new StatsAccumulator();
				hybridTT[i] = new StatsAccumulator();
			}
		}

		void addSample(Sample sample, double binValue) {
			int binIdx = (int)Math.floor(binValue / binSize);

			networkTT[binIdx].add(sample.networkTT);
			networkDistance[binIdx].add(sample.networkDistance);
			zonalTT[binIdx].add(sample.zonalTT);
			beelineDistance[binIdx].add(sample.beelineDistance);
			hybridTT[binIdx].add(sample.hybridTT);
		}

		void combine(BinnedAccumulatedStats other) {
			Preconditions.checkArgument(binSize == other.binSize);
			Preconditions.checkArgument(networkTT.length == other.networkTT.length);

			for (int i = 0; i < networkTT.length; i++) {
				networkTT[i].addAll(other.networkTT[i]);
				networkDistance[i].addAll(other.networkDistance[i]);
				zonalTT[i].addAll(other.zonalTT[i]);
				beelineDistance[i].addAll(other.beelineDistance[i]);
				hybridTT[i].addAll(other.hybridTT[i]);
			}
		}
	}

	private static void computeDistancesForAllOutgoingNodes(Node fromNode, LeastCostPathTree lcpTree, Context context) {
		context.counter.incCounter();

		lcpTree.calculate(fromNode.getId().index(), context.departureTime, null, null,
				(nodeIndex, arrivalTime, travelCost, distance, departureTime) -> distance >= MAX_DISTANCE
						|| travelCost >= MAX_TIME);

		var allStats = new AllStats();
		for (Node toNode : context.network.getNodes().values()) {
			int nodeIndex = toNode.getId().index();
			OptionalTime currOptionalTime = lcpTree.getTime(nodeIndex);
			if (currOptionalTime.isUndefined()) {
				continue;
			}
			double currTime = currOptionalTime.seconds();
			double networkTT = currTime - context.departureTime;
			// FIXME make it zonal only (now this call is computing the hybrid distance)
			double zonalTT = context.travelTimeMatrix.getFreeSpeedTravelTime(fromNode, toNode);
			double networkDistance = lcpTree.getDistance(nodeIndex);
			double beelineDistance = DistanceUtils.calculateDistance(fromNode, toNode);
			double hybridTT = context.travelTimeMatrix.getFreeSpeedTravelTime(fromNode, toNode);

			if (zonalTT != hybridTT && hybridTT != networkTT && networkDistance < 1000) {
				System.out.println();
			}

			allStats.addSample(new Sample(networkTT, networkDistance, zonalTT, beelineDistance, hybridTT));
		}

		context.allStatsAccumulator.accumulate(allStats);
	}
}

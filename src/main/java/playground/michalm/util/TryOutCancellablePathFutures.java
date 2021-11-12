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

package playground.michalm.util;

import java.util.ArrayList;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Michal Maciejewski (michalm)
 */
public class TryOutCancellablePathFutures {

	public static void main(String[] args) {
		var executor = Executors.newSingleThreadExecutor();
		var pathFutures = new ArrayList<Future<Path>>();
		for (int i = 0; i < 3; i++) {
			String name = "supplier_" + i;
			pathFutures.add(executor.submit(() -> {
				System.out.println("[" + name + "] Started computing path...");
				for (int ii = 0; ii <= 10; ii += 2) {
					sleep(200);
					System.out.println("[" + name + "] progress: " + ii + "0%");
				}
				System.out.println("[supplier] Path computed...");
				return new Path("A->B->C->D");
			}));
		}
		executor.shutdown();
		sleep(300);
		pathFutures.get(0).cancel(true);

		for (var pathFuture : pathFutures) {
			try {
				while (!pathFuture.isDone()) {
					System.out.println("[main] path not yet computed");
					sleep(500);
				}

				System.out.println("[main] Obtained path: " + pathFuture.get());
			} catch (InterruptedException | ExecutionException | CancellationException e) {
				e.printStackTrace();
			}
		}

		//	var task = new TaskWithPath(new CompletableFuture<>());
		//if we see the task will be soon executed, then we send the completable future to the executor
		//task.getPathFuture()
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	record Path(String txt) {
	}

	private static class TaskWithPath {
		private final Future<Path> pathFuture;

		private TaskWithPath(Future<Path> pathFuture) {
			this.pathFuture = pathFuture;
		}

		Future<Path> getPathFuture() {
			return pathFuture;
		}

		public Path getPath() {
			try {
				return pathFuture.get();
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		}
	}
}

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

package playground.michalm.taxi.run;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;
import org.matsim.core.utils.io.IOUtils;

public class TaxiBenchmarkPostProcessor {
	private static class Experiment {
		private final String id;
		private final List<Stats> stats = new ArrayList<>();

		private Experiment(String id) {
			this.id = id;
		}
	}

	/**
	 * represents a single row in a file created by MultiRunStats all stats in such a file have the same values for 'n'
	 * and 'm'
	 */
	private static class Stats {
		// private final String cfg;
		private final int n;
		private final int m;

		private final double[] values;

		private Stats(Scanner sc, int count) {
			// cfg = sc.next();
			n = sc.nextInt();
			m = sc.nextInt();

			values = new double[count];
			for (int i = 0; i < count; i++) {
				values[i] = sc.nextDouble();
			}
		}
	}

	private static final Experiment EMPTY_COLUMN = new Experiment("empty column");

	private final String[] header;
	private final Experiment[] experiments;
	private final String[] statsColumns;

	public TaxiBenchmarkPostProcessor(String[] header, String... ids) {
		this.header = header;

		experiments = new Experiment[ids.length];
		for (int i = 0; i < experiments.length; i++) {
			String id = ids[i];
			experiments[i] = id == null ? EMPTY_COLUMN : new Experiment(ids[i]);
		}

		if (!header[0].equals("n") || !header[1].equals("m")) {
			throw new IllegalArgumentException("Incompatibile header");
		}
		statsColumns = Arrays.copyOfRange(header, 2, header.length);
	}

	public void process(String dir, String subDirPrefix, String file) {
		for (Experiment e : experiments) {
			if (e != EMPTY_COLUMN) {
				readFile(dir + subDirPrefix + e.id + "/" + file + ".txt", e);
			}
		}

		for (int i = 0; i < statsColumns.length; i++) {
			writeValues(dir + file, i);
		}
	}

	private void readFile(String file, Experiment experiment) {
		try (Scanner sc = new Scanner(new File(file))) {
			String header = sc.nextLine();
			if (!Arrays.equals(header.split("\t"), this.header)) {
				throw new RuntimeException("Incompatible header");
			}

			if (!sc.hasNext()) {
				throw new RuntimeException("No stats");
			}

			Stats s0 = new Stats(sc, statsColumns.length);
			experiment.stats.add(s0);

			while (sc.hasNext()) {
				Stats stats = new Stats(sc, statsColumns.length);

				if (stats.m != s0.m || stats.n != s0.n) {
					throw new RuntimeException("The file must contain result for the same 'm' and 'n'");
				}

				experiment.stats.add(stats);
			}
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private void writeValues(String file, int column) {
		String field = statsColumns[column];
		PrintWriter pw = new PrintWriter(IOUtils.getBufferedWriter(file + "_" + field + ".txt"));
		StringBuilder lineId = new StringBuilder(StringUtils.leftPad(field, 20));
		StringBuilder lineN = new StringBuilder(StringUtils.leftPad("n", 20));
		StringBuilder lineM = new StringBuilder(StringUtils.leftPad("m", 20));
		StringBuilder lineRatio = new StringBuilder(StringUtils.leftPad("ratio", 20));

		for (Experiment e : experiments) {
			if (e == EMPTY_COLUMN) {
				lineId.append('\t');
				lineN.append('\t');
				lineM.append('\t');
				lineRatio.append('\t');
			} else {
				Stats s = e.stats.get(0);
				double ratio = (double)s.n / s.m;
				lineId.append('\t').append(e.id);
				lineN.append('\t').append(s.n);
				lineM.append('\t').append(s.m);
				lineRatio.append('\t').append(String.format("%.2f", ratio));
			}
		}

		pw.println(lineId.toString());
		pw.println(lineN.toString());
		pw.println(lineM.toString());
		pw.println(lineRatio.toString());

		int statsCount = experiments[0].stats.size();
		DecimalFormat format = new DecimalFormat("#.###");

		for (int i = 0; i < statsCount; i++) {
			// String cfg0 = experiments[0].stats.get(i).cfg;
			// pw.printf("%20s", cfg0);

			for (Experiment e : experiments) {
				if (e == EMPTY_COLUMN) {
					pw.print('\t');// insert one empty column
				} else {
					Stats s = e.stats.get(i);

					// if (!cfg0.equals(s.cfg)) {
					// throw new RuntimeException();
					// }

					pw.print("\t" + format.format(s.values[column]));
				}
			}

			pw.println();
		}

		pw.close();
	}
}

package playground.michalm.util.array2d;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.matsim.core.utils.io.IOUtils;

/**
 * Reads 2D arrays (of different types) from files. To handle different types of data use <code>TypeStrategy</code>.
 * There are some predefined strategies for following types: double, int, String.
 *
 * @author michalm
 */
public class Array2DReader {
	public static double[][] getDoubleArray(String file, int cols) {
		try (BufferedReader br = IOUtils.getBufferedReader(file)) {
			double[][] result1 = null;
			List<double[]> rows = new ArrayList<>();
			boolean endOfArray = false;

			for (String line = br.readLine(); line != null; line = br.readLine()) {
				StringTokenizer st = new StringTokenizer(line, " \t");

				if (endOfArray) {
					if (st.hasMoreTokens()) {
						throw new RuntimeException("Non-empty line after matrix");
					}
				} else {
					if (!st.hasMoreTokens()) {
						endOfArray = true;
						continue;
					}

					var row = new double[cols];

					for (int i = 0; i < cols; i++) {
						if (!st.hasMoreTokens()) {
							throw new RuntimeException("Too few elements");
						}
						row[i] = Double.parseDouble(st.nextToken());
					}

					if (st.hasMoreTokens()) {
						throw new RuntimeException("Too many elements");
					}

					rows.add(row);
				}
			}

			if (rows.size() != 0) {
				result1 = rows.toArray(double[][]::new);
			}

			return result1;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}

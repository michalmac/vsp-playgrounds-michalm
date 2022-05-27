package playground.michalm.util.array2d;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.StringTokenizer;

import org.matsim.core.utils.io.IOUtils;

import com.google.common.base.Preconditions;

/**
 * Reads 2D arrays (of different types) from files. To handle different types of data use <code>TypeStrategy</code>.
 * There are some predefined strategies for following types: double, int, String.
 *
 * @author michalm
 */
public class Array2DReader {
	public static double[][] getDoubleArray(String file, int cols) {
		try (BufferedReader br = IOUtils.getBufferedReader(file)) {
			return br.lines()
					.map(line -> Collections.list(new StringTokenizer(line, " \t"))
							.stream()
							.mapToDouble(v -> Double.parseDouble((String)v))
							.toArray())
					.peek(row -> Preconditions.checkArgument(row.length == cols))
					.toArray(double[][]::new);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}

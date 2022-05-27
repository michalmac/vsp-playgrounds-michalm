package playground.michalm.util.array2d;

import java.lang.reflect.Array;

/**
 * @author michalm
 */
public class Array2DUtils {
	public static double[][] transponse(double[][] array) {
		if (array == null || array.length == 0 || array[0].length == 0) {
			throw new RuntimeException("Null or empty array");
		}

		double[][] transposed = new double[array[0].length][array.length];

		for (int i = 0; i < transposed.length; i++) {
			for (int j = 0; j < array.length; j++) {
				transposed[i][j] = array[j][i];
			}
		}

		return transposed;
	}
}

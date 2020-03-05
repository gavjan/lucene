package pl.edu.mimuw.gc401929.core;

public class SimpleAdder {
	public static int add(int x, int y) {
		return x + y;
	}
	
	public static int addPositive(int x, int y) {
		if (x <= 0 || y <= 0) {
			throw new IllegalArgumentException("Arguments must be positive");
		}
		return x + y;
	}
}

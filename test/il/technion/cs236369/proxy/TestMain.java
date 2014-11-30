package il.technion.cs236369.proxy;

import il.technion.cs236369.proxy.test.BasicTest;

import java.io.IOException;
import java.sql.SQLException;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

public class TestMain {
	public static void main(String[] args) throws SQLException, IOException {
		JUnitCore junit = new JUnitCore();
		Result result = junit.run(BasicTest.class);
		System.out.println("You " + (result.wasSuccessful() ? "passed " : "didn't pass ")
				+ "the basic test " + (result.wasSuccessful() ? ":)" : ":("));
	}
}

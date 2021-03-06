package eu.stratosphere.meteor.base;

import org.junit.Test;

import eu.stratosphere.meteor.MeteorTest;
import eu.stratosphere.sopremo.base.Intersection;
import eu.stratosphere.sopremo.io.Sink;
import eu.stratosphere.sopremo.io.Source;
import eu.stratosphere.sopremo.operator.SopremoPlan;

public class IntersectionTest extends MeteorTest {

	@Test
	public void testIntersection() {
		final SopremoPlan actualPlan = parseScript("$users1 = read from 'file://users1.json';\n" +
			"$users2 = read from 'file://users2.json';\n" +
			"$commonUsers = intersect $users1, $users2;\n" +
			"write $commonUsers to 'file://commonUsers.json';");

		final SopremoPlan expectedPlan = new SopremoPlan();
		final Source users1 = new Source("file://users1.json");
		final Source users2 = new Source("file://users2.json");
		final Intersection intersection = new Intersection().withInputs(users1, users2);
		final Sink output = new Sink("file://commonUsers.json").withInputs(intersection);
		expectedPlan.setSinks(output);

		assertPlanEquals(expectedPlan, actualPlan);
	}

}

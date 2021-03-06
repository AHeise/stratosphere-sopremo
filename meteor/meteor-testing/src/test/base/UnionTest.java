package eu.stratosphere.meteor.base;

import org.junit.Test;

import eu.stratosphere.meteor.MeteorTest;
import eu.stratosphere.sopremo.base.Union;
import eu.stratosphere.sopremo.io.Sink;
import eu.stratosphere.sopremo.io.Source;
import eu.stratosphere.sopremo.operator.SopremoPlan;

public class UnionTest extends MeteorTest {

	@Test
	public void testUnion1() {
		final SopremoPlan actualPlan = this.parseScript("$users1 = read from 'file://users1.json';\n" +
			"$users2 = read from 'file://users2.json';\n" +
			"$allUsers = union $users1, $users2;\n" +
			"write $allUsers to 'file://allUsers.json';");

		final SopremoPlan expectedPlan = new SopremoPlan();
		final Source users1 = new Source("file://users1.json");
		final Source users2 = new Source("file://users2.json");
		final Union union = new Union().
			withInputs(users1, users2);
		final Sink output = new Sink("file://allUsers.json").withInputs(union);
		expectedPlan.setSinks(output);

		assertPlanEquals(expectedPlan, actualPlan);
	}
}

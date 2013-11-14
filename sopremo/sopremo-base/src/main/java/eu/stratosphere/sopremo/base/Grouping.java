package eu.stratosphere.sopremo.base;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Predicates;

import eu.stratosphere.sopremo.EvaluationContext;
import eu.stratosphere.sopremo.CoreFunctions;
import eu.stratosphere.sopremo.aggregation.AssociativeAggregation;
import eu.stratosphere.sopremo.expressions.AggregationExpression;
import eu.stratosphere.sopremo.expressions.ArrayAccess;
import eu.stratosphere.sopremo.expressions.ArrayCreation;
import eu.stratosphere.sopremo.expressions.BatchAggregationExpression;
import eu.stratosphere.sopremo.expressions.ConstantExpression;
import eu.stratosphere.sopremo.expressions.EvaluationExpression;
import eu.stratosphere.sopremo.expressions.ExpressionUtil;
import eu.stratosphere.sopremo.expressions.InputSelection;
import eu.stratosphere.sopremo.operator.CompositeOperator;
import eu.stratosphere.sopremo.operator.ElementaryOperator;
import eu.stratosphere.sopremo.operator.InputCardinality;
import eu.stratosphere.sopremo.operator.JsonStream;
import eu.stratosphere.sopremo.operator.Name;
import eu.stratosphere.sopremo.operator.OutputCardinality;
import eu.stratosphere.sopremo.operator.Property;
import eu.stratosphere.sopremo.operator.SopremoModule;
import eu.stratosphere.sopremo.pact.JsonCollector;
import eu.stratosphere.sopremo.pact.SopremoCoGroup;
import eu.stratosphere.sopremo.pact.SopremoReduce;
import eu.stratosphere.sopremo.type.ArrayNode;
import eu.stratosphere.sopremo.type.IArrayNode;
import eu.stratosphere.sopremo.type.IJsonNode;
import eu.stratosphere.sopremo.type.IStreamNode;
import eu.stratosphere.sopremo.type.NullNode;
import eu.stratosphere.util.CollectionUtil;

@InputCardinality(min = 1, max = 2)
@OutputCardinality(1)
@Name(verb = "group")
public class Grouping extends CompositeOperator<Grouping> {
	private final static EvaluationExpression GROUP_ALL = new ConstantExpression(NullNode.getInstance());

	private EvaluationExpression resultProjection = EvaluationExpression.VALUE;

	private final List<EvaluationExpression> keyExpressions = new ArrayList<EvaluationExpression>(1);

	private EvaluationExpression defaultGroupingKey = GROUP_ALL;

	@Override
	public void addImplementation(SopremoModule module, EvaluationContext context) {
		JsonStream output;
		switch (this.getNumInputs()) {
		case 0:
			throw new IllegalStateException("No input given for grouping");
		case 1:
			output = createGrouping(module);
			break;
		case 2:
			output = new CoGroupProjection().withResultProjection(this.resultProjection).
				withKeyExpression(0, this.getGroupingKey(0).clone().remove(new InputSelection(0))).
				withKeyExpression(1, this.getGroupingKey(1).clone().remove(new InputSelection(1))).
				withInputs(module.getInputs());
			break;
		default:
			throw new IllegalStateException("More than two sources are not supported");
			// List<JsonStream> inputs = new ArrayList<JsonStream>();
			// List<EvaluationExpression> keyExpressions = new ArrayList<EvaluationExpression>();
			// for (int index = 0; index < numInputs; index++) {
			// inputs.add(OperatorUtil.positionEncode(module.getInput(index), index, numInputs));
			// keyExpressions.add(new PathExpression(new InputSelection(index), getGroupingKey(index)));
			// }
			// final UnionAll union = new UnionAll().
			// withInputs(inputs);
			// final PathExpression projection =
			// new PathExpression(new AggregationExpression(new ArrayUnion()), this.resultProjection);
			// output = new GroupProjection(projection).
			// withInputs(union);
			// break;
		}

		module.getOutput(0).setInput(0, output);
	}

	private JsonStream createGrouping(SopremoModule module) {
		EvaluationExpression resultProjection = this.resultProjection.clone().remove(new InputSelection(0));
		final List<AggregationExpression> aggregations = resultProjection.findAll(AggregationExpression.class);
		for (AggregationExpression aggregationExpression : aggregations)
			// not combinable, if there is a non-associative expression
			if (!(aggregationExpression.getAggregation() instanceof AssociativeAggregation))
				return new GroupProjection().withResultProjection(resultProjection).
					withKeyExpression(0, this.getGroupingKey(0).clone().remove(new InputSelection(0))).
					withInputs(module.getInputs());

		// first project all tuples to an array with a slot for each aggregation
		// fill the array with the input expressions of the respective aggregation
		final ArrayCreation aggregatedValues = new ArrayCreation();
		aggregatedValues.add(this.getGroupingKey(0).clone().remove(new InputSelection(0)));
		for (AggregationExpression aggregationExpression : aggregations)
			aggregatedValues.add(aggregationExpression.getInputExpression());
		final Projection initialValues = new Projection().withResultProjection(aggregatedValues).
			withInputs(module.getInputs());

		// now we can create a combinable aggregation, that associatively aggregates the elements in the array
		final ArrayCreation combinableAggregation = new ArrayCreation();
		BatchAggregationExpression bae = new BatchAggregationExpression();
		combinableAggregation.add(bae.add(CoreFunctions.FIRST, new ArrayAccess(0)));
		for (int index = 0, size = aggregations.size(); index < size; index++)
			combinableAggregation.add(bae.add(aggregations.get(index).getAggregation(), new ArrayAccess(index + 1)));
		final JsonStream combinableGrouping = new GroupProjection().
			withCombinable(true).
			withResultProjection(combinableAggregation).
			withKeyExpression(0, new ArrayAccess(0)).
			withInputs(initialValues);

		// and finally, we need to perform the actual project from the array to the desired output
		// replace the aggregation expression with the corresponding array access
		final EvaluationExpression finalProjection =
			resultProjection.replace(Predicates.instanceOf(AggregationExpression.class),
				new Function<EvaluationExpression, EvaluationExpression>() {
					int aggregationIndex = 1;

					@Override
					public EvaluationExpression apply(EvaluationExpression expression) {
						return new ArrayAccess(this.aggregationIndex++);
					}
				});
		return new Projection().withResultProjection(finalProjection).
			withInputs(combinableGrouping);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		final Grouping other = (Grouping) obj;
		return this.resultProjection.equals(other.resultProjection);
	}

	public EvaluationExpression getResultProjection() {
		return this.resultProjection;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + this.resultProjection.hashCode();
		return result;
	}

	@Property(preferred = true)
	@Name(preposition = "into")
	public void setResultProjection(EvaluationExpression resultProjection) {
		if (resultProjection == null)
			throw new NullPointerException("resultProjection must not be null");

		this.resultProjection =
			ExpressionUtil.replaceAggregationWithBatchAggregation(
				ExpressionUtil.replaceIndexAccessWithAggregation(resultProjection));
	}

	public Grouping withResultProjection(EvaluationExpression resultProjection) {
		this.setResultProjection(resultProjection);
		return this;
	}

	@Property(preferred = true, input = true)
	@Name(preposition = "by")
	public void setGroupingKey(final int inputIndex, final EvaluationExpression keyExpression) {
		CollectionUtil.ensureSize(this.keyExpressions, inputIndex + 1);
		this.keyExpressions.set(inputIndex, keyExpression);
	}

	public void setGroupingKey(final JsonStream input, final EvaluationExpression keyExpression) {
		if (keyExpression == null)
			throw new NullPointerException("keyExpression must not be null");

		this.setGroupingKey(this.getSafeInputIndex(input), keyExpression);
	}

	public Grouping withGroupingKey(int inputIndex, EvaluationExpression groupingKey) {
		this.setGroupingKey(inputIndex, groupingKey);
		return this;
	}

	public Grouping withGroupingKey(EvaluationExpression groupingKey) {
		this.setDefaultGroupingKey(groupingKey);
		return this;
	}

	public EvaluationExpression getGroupingKey(final int index) {
		final EvaluationExpression keyExpression =
			index < this.keyExpressions.size() ? this.keyExpressions.get(index) : null;
		if (keyExpression == null)
			return this.getDefaultGroupingKey();
		return keyExpression;
	}

	public EvaluationExpression getGroupingKey(final JsonStream input) {
		return this.getGroupingKey(this.getSafeInputIndex(input));
	}

	public EvaluationExpression getDefaultGroupingKey() {
		return this.defaultGroupingKey;
	}

	@Property(hidden = true)
	public void setDefaultGroupingKey(EvaluationExpression defaultGroupingKey) {
		if (defaultGroupingKey == null)
			throw new NullPointerException("defaultGroupingKey must not be null");

		this.defaultGroupingKey = defaultGroupingKey;
	}

	/*
	 * (non-Javadoc)
	 * @see eu.stratosphere.sopremo.operator.Operator#appendAsString(java.lang.Appendable)
	 */
	@Override
	public void appendAsString(Appendable appendable) throws IOException {
		super.appendAsString(appendable);
		appendable.append(" on ");
		for (int input = 0; input < getNumInputs(); input++) {
			if (input > 1)
				appendable.append(", ");
			getGroupingKey(input).appendAsString(appendable);
		}
		appendable.append(" to ");
		this.resultProjection.appendAsString(appendable);
	}

	@InputCardinality(min = 2, max = 2)
	public static class CoGroupProjection extends ElementaryOperator<CoGroupProjection> {
		public static class Implementation extends SopremoCoGroup {
			private final IArrayNode<IStreamNode<IJsonNode>> streams = new ArrayNode<IStreamNode<IJsonNode>>(2);

			@Override
			protected void coGroup(IStreamNode<IJsonNode> values1, IStreamNode<IJsonNode> values2,
					JsonCollector<IJsonNode> out) {
				this.streams.set(0, values1);
				this.streams.set(1, values2);
				out.collect(this.streams);
			}
		}
	}

	@InputCardinality(1)
	public static class GroupProjection extends ElementaryOperator<GroupProjection> {
		// /* (non-Javadoc)
		// * @see
		// eu.stratosphere.sopremo.operator.ElementaryOperator#getContract(eu.stratosphere.sopremo.serialization.SopremoRecordLayout)
		// */
		// @Override
		// protected Contract getContract(SopremoRecordLayout layout) {
		// ReduceContract.Builder builder =
		// ReduceContract.builder(this.isCombinable() ? CombinableImplementation.class : Implementation.class);
		// if (!this.getKeyExpressions(0).contains(GROUP_ALL)) {
		// int[] keyIndices = this.getKeyIndices(globalSchema, this.getKeyExpressions(0));
		// PactBuilderUtil.addKeys(builder, this.getKeyClasses(globalSchema, keyIndices), keyIndices);
		// }
		// builder.name(this.toString());
		// return builder.build();
		// }
		/*
		 * (non-Javadoc)
		 * @see eu.stratosphere.sopremo.operator.ElementaryOperator#getStubClass()
		 */
		public static class Implementation extends SopremoReduce {
			@Override
			protected void reduce(final IStreamNode<IJsonNode> values, final JsonCollector<IJsonNode> out) {
				out.collect(values);
			}
		}
	}
}

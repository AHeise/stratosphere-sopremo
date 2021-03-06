/***********************************************************************************************************************
 *
 * Copyright (C) 2010-2013 by the Stratosphere project (http://stratosphere.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 **********************************************************************************************************************/
package eu.stratosphere.meteor;

import java.io.IOException;

import eu.stratosphere.sopremo.expressions.EvaluationExpression;
import eu.stratosphere.sopremo.expressions.UnevaluableExpression;
import eu.stratosphere.sopremo.operator.JsonStream;

/**
 */
public class StreamIndexExpression extends UnevaluableExpression {

	private final JsonStream stream;

	private final EvaluationExpression indexExpression;

	/**
	 * Initializes StreamIndexExpression.
	 */
	public StreamIndexExpression(final JsonStream stream, final EvaluationExpression indexExpression) {
		super("Stream index");
		this.stream = stream;
		this.indexExpression = indexExpression;
	}

	@Override
	public void appendAsString(final Appendable appendable) throws IOException {
		this.stream.appendAsString(appendable);
		appendable.append("[");
		this.indexExpression.appendAsString(appendable);
		appendable.append("]");
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		final StreamIndexExpression other = (StreamIndexExpression) obj;
		return this.indexExpression.equals(other.indexExpression); // && stream.equals(other.stream)
	}

	/**
	 * Returns the indexExpression.
	 * 
	 * @return the indexExpression
	 */
	public EvaluationExpression getIndexExpression() {
		return this.indexExpression;
	}

	/**
	 * Returns the stream.
	 * 
	 * @return the stream
	 */
	public JsonStream getStream() {
		return this.stream;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + this.indexExpression.hashCode();
		result = prime * result + this.stream.hashCode();
		return result;
	}
}

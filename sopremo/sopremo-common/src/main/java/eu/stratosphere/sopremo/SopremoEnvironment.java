/***********************************************************************************************************************
 *
 * Copyright (C) 2010 by the Stratosphere project (http://stratosphere.eu)
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
package eu.stratosphere.sopremo;

import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.configuration.Configuration;

import eu.stratosphere.sopremo.pact.SopremoUtil;
import eu.stratosphere.sopremo.serialization.SopremoRecordLayout;

/**
 * Provides a unified access to less needed information about an {@link Operator} or its implementation during compile
 * and runtime.<br/>
 * There is exactly one {@link SopremoEnvironment} per {@link Thread} and it is thus thread-safe.
 */
public class SopremoEnvironment {
	/**
	 * 
	 */
	private static final ThreadLocal<SopremoEnvironment> INSTANCE = new ThreadLocal<SopremoEnvironment>() {
		@Override
		protected SopremoEnvironment initialValue() {
			return new SopremoEnvironment();
		};
	};

	private EvaluationContext evaluationContext = new EvaluationContext();

	private ClassLoader classLoader = this.getClass().getClassLoader();

	private Configuration configuration;

	private RuntimeContext runtimeContext;

	/**
	 * Returns the classLoader.
	 * 
	 * @return the classLoader
	 */
	public ClassLoader getClassLoader() {
		return this.classLoader;
	}

	/**
	 * Returns the configuration.
	 * 
	 * @return the configuration
	 */
	public Configuration getConfiguration() {
		return this.configuration;
	}

	/**
	 * Returns the evaluationContext.
	 * 
	 * @return the evaluationContext
	 */
	public EvaluationContext getEvaluationContext() {
		return this.evaluationContext;
	}

	/**
	 * Returns the runtimeContext.
	 * 
	 * @return the runtimeContext
	 */
	public RuntimeContext getRuntimeContext() {
		return this.runtimeContext;
	}

	/**
	 * Sets the classLoader to the specified value.
	 * 
	 * @param classLoader
	 *        the classLoader to set
	 */
	public void setClassLoader(final ClassLoader classLoader) {
		if (classLoader == null)
			throw new NullPointerException("classLoader must not be null");

		this.classLoader = classLoader;
	}

	private static final String CONTEXT = "sopremo.context";

	/**
	 * Sets the configuration to the specified value.
	 * 
	 * @param configuration
	 *        the configuration to set
	 */
	public void load(Configuration configuration) {
		if (configuration == null)
			throw new NullPointerException("configuration must not be null");

		final EvaluationContext context = SopremoUtil.getObject(configuration, CONTEXT, null);
		// context may be null if the format is used within another second order function, i.e. not the expected Data Source
		if (context != null) {
			this.configuration = configuration;
			this.classLoader = configuration.getClassLoader();
			this.evaluationContext = context;
		}
	}

	public void setConfigurationAndContext(final Configuration parameters, final RuntimeContext runtimeContext) {
		this.load(parameters);
		this.setRuntimeContext(runtimeContext);
	}

	/**
	 * Sets the evaluationContext to the specified value.
	 * 
	 * @param evaluationContext
	 *        the evaluationContext to set
	 */
	public void setEvaluationContext(final EvaluationContext evaluationContext) {
		if (evaluationContext == null)
			throw new NullPointerException("evaluationContext must not be null");

		this.evaluationContext = evaluationContext;
	}

	/**
	 * Sets the runtimeContext to the specified value.
	 * 
	 * @param context
	 *        the runtimeContext to set
	 */
	public void setRuntimeContext(final RuntimeContext context) {
		if (context == null)
			throw new NullPointerException("runtimeContext must not be null");

		this.runtimeContext = context;
	}

	public static SopremoEnvironment getInstance() {
		return INSTANCE.get();
	}

	private SopremoRecordLayout layout = SopremoRecordLayout.create();

	/**
	 * Returns the layout.
	 * 
	 * @return the layout
	 */
	public SopremoRecordLayout getLayout() {
		return this.layout;
	}

	/**
	 * Sets the layout to the specified value.
	 * 
	 * @param layout
	 *        the layout to set
	 */
	public void setLayout(SopremoRecordLayout layout) {
		if (layout == null)
			throw new NullPointerException("layout must not be null");

		this.layout = layout;
	}

	/**
	 * @param parameters
	 */
	public void save(Configuration parameters) {
		SopremoUtil.setObject(parameters, CONTEXT, this.evaluationContext);
	}
}

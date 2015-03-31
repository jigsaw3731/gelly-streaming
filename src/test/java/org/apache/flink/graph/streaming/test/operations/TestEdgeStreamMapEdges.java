/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.graph.streaming.test.operations;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.graph.Edge;
import org.apache.flink.graph.streaming.EdgeStream;
import org.apache.flink.graph.streaming.test.GraphStreamTestUtils;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.MultipleProgramsTestBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class TestEdgeStreamMapEdges extends MultipleProgramsTestBase {

	public TestEdgeStreamMapEdges(TestExecutionMode mode) {
		super(mode);
	}

	private String resultPath;
	private String expectedResult;

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Before
	public void before() throws Exception {
		resultPath = tempFolder.newFile().toURI().toString();
	}

	@After
	public void after() throws Exception {
		compareResultsByLinesInMemory(expectedResult, resultPath);
	}

	@Test
	public void testWithSameType() throws Exception {
		/*
		 * Test mapEdges() keeping the same edge types
	     */
		final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

		EdgeStream<Long, Long> graph = new EdgeStream<>(GraphStreamTestUtils.getLongLongEdgeDataStream(env), env);

		graph = graph.mapEdges(new AddOneMapper());

		graph.getEdges().writeAsCsv(resultPath, FileSystem.WriteMode.OVERWRITE);
		env.execute();
		expectedResult = "1,2,13\n" +
				"1,3,14\n" +
				"2,3,24\n" +
				"3,4,35\n" +
				"3,5,36\n" +
				"4,5,46\n" +
				"5,1,52\n";
	}

	private static final class AddOneMapper implements MapFunction<Edge<Long, Long>, Long> {
		@Override
		public Long map(Edge<Long, Long> edge) throws Exception {
			return edge.getValue() + 1;
		}
	}

	@Test
	public void testWithTupleType() throws Exception {
		/*
		 * Test mapEdges() converting the edge value type to tuple
	     */
		final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

		EdgeStream<Long, Long> graph = new EdgeStream<>(GraphStreamTestUtils.getLongLongEdgeDataStream(env), env);

		EdgeStream<Long, Tuple2<Long, Long>> mappedGraph = graph.mapEdges(new ToTuple2Mapper());

		mappedGraph.getEdges().writeAsCsv(resultPath, FileSystem.WriteMode.OVERWRITE);
		env.execute();
		expectedResult = "1,2,(12,13)\n" +
				"1,3,(13,14)\n" +
				"2,3,(23,24)\n" +
				"3,4,(34,35)\n" +
				"3,5,(35,36)\n" +
				"4,5,(45,46)\n" +
				"5,1,(51,52)\n";
	}

	private static final class ToTuple2Mapper implements MapFunction<Edge<Long, Long>, Tuple2<Long, Long>> {
		@Override
		public Tuple2<Long, Long> map(Edge<Long, Long> edge) throws Exception {
			return new Tuple2<>(edge.getValue(), edge.getValue() + 1);
		}
	}

	@Test
	public void testChainedMaps() throws Exception {
		/*
		 * Test mapEdges() where two maps are chained together
	     */
		final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

		EdgeStream<Long, Long> graph = new EdgeStream<>(GraphStreamTestUtils.getLongLongEdgeDataStream(env), env);

		EdgeStream<Long, Tuple2<Long, Long>> mappedGraph = graph
				.mapEdges(new AddOneMapper())
				.mapEdges(new ToTuple2Mapper());

		mappedGraph.getEdges().writeAsCsv(resultPath, FileSystem.WriteMode.OVERWRITE);
		env.execute();
		expectedResult = "1,2,(13,14)\n" +
				"1,3,(14,15)\n" +
				"2,3,(24,25)\n" +
				"3,4,(35,36)\n" +
				"3,5,(36,37)\n" +
				"4,5,(46,47)\n" +
				"5,1,(52,53)\n";
	}
}
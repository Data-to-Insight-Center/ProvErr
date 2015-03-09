/*
#
# Copyright 2013 The Trustees of Indiana University
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or areed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
 */

package edu.iu.d2i.proverr.evaluation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import edu.iu.d2i.proverr.diagnosis.CauseDetermination;
import edu.iu.d2i.proverr.diagnosis.DependencyGraph;
import edu.iu.d2i.proverr.diagnosis.XmlParser;

/**
 * Evaluate the performance of ProvErr on software bugs
 * 
 * @author peng
 * 
 */
public class SoftwareBugEval {

	public static void main(String[] args) {
		try {
			/*
			 * Add candidate success cases
			 */
			String dir = "/home/peng/workspace/ProvErr/Group1/";

			List<TestTuple> successfulCases = new ArrayList<TestTuple>();

			DependencyGraph s1 = XmlParser.parsePROVXml(dir
					+ "setNull-1.pigProv.xml");
			successfulCases
					.add(new TestTuple("setNull-1.pigProv.xml", s1, null));

			DependencyGraph s2 = XmlParser.parsePROVXml(dir
					+ "setNull-2.pigProv.xml");
			successfulCases
					.add(new TestTuple("setNull-2.pigProv.xml", s2, null));

			DependencyGraph s3 = XmlParser.parsePROVXml(dir
					+ "setNull.pigProv.xml");
			successfulCases.add(new TestTuple("setNull.pigProv.xml", s3, null));

			DependencyGraph s4 = XmlParser.parsePROVXml(dir
					+ "lastIndex-store.pigProv.xml");
			successfulCases.add(new TestTuple("lastIndex-store.pigProv.xml",
					s4, null));

			DependencyGraph s5 = XmlParser.parsePROVXml(dir
					+ "abs-store-upper.pigProv.xml");
			successfulCases.add(new TestTuple("abs-store-upper.pigProv.xml",
					s5, null));

			DependencyGraph s6 = XmlParser.parsePROVXml(dir
					+ "concat-store-avg.pigProv.xml");
			successfulCases.add(new TestTuple("concat-store-avg.pigProv.xml",
					s6, null));

			DependencyGraph s7 = XmlParser.parsePROVXml(dir
					+ "concat-store-min.pigProv.xml");
			successfulCases.add(new TestTuple("concat-store-min.pigProv.xml",
					s7, null));

			DependencyGraph s8 = XmlParser.parsePROVXml(dir
					+ "lastIndex-store-lower.pigProv.xml");
			successfulCases.add(new TestTuple(
					"lastIndex-store-lower.pigProv.xml", s8, null));

			DependencyGraph s9 = XmlParser.parsePROVXml(dir
					+ "setNull-clean-dataset.pigProv.xml");
			successfulCases.add(new TestTuple(
					"setNull-clean-dataset.pigProv.xml", s9, null));

			/*
			 * Add candidate failures
			 */
			List<TestTuple> failedCases = new ArrayList<TestTuple>();

			// 5 faults caused by 4 individual causes
			DependencyGraph f1 = XmlParser.parsePROVXml(dir
					+ "e-lastIndex-store.pigProv.xml");
			failedCases.add(new TestTuple("e-lastIndex-store.pigProv.xml", f1,
					"org.apache.pig.error.LAST_INDEX_OF"));

			DependencyGraph f2 = XmlParser.parsePROVXml(dir
					+ "e-abs-store-upper.pigProv.xml");
			failedCases.add(new TestTuple("e-abs-store-upper.pigProv.xml", f2,
					"org.apache.pig.builtin.IntAbs"));

			DependencyGraph f3 = XmlParser.parsePROVXml(dir
					+ "concat-e-store-avg.pigProv.xml");
			failedCases.add(new TestTuple("concat-e-store-avg.pigProv.xml", f3,
					"org.apache.pig.error.PigStorage"));

			DependencyGraph f4 = XmlParser.parsePROVXml(dir
					+ "e-concat-store-min.pigProv.xml");
			failedCases.add(new TestTuple("e-concat-store-min.pigProv.xml", f4,
					"org.apache.pig.error.CONCAT"));

			DependencyGraph f5 = XmlParser.parsePROVXml(dir
					+ "e-lastIndex-store-lower.pigProv.xml");
			failedCases.add(new TestTuple(
					"e-lastIndex-store-lower.pigProv.xml", f5,
					"org.apache.pig.error.LAST_INDEX_OF"));

			// 2 co-causes
			DependencyGraph f6 = XmlParser.parsePROVXml(dir
					+ "e-setNull.pigProv.xml");
			failedCases
					.add(new TestTuple("e-setNull.pigProv.xml", f6,
							"org.apache.pig.error.SetIntNull::org.apache.pig.error.SetCharArrayNull"));

			DependencyGraph f7 = XmlParser.parsePROVXml(dir
					+ "e-setNull-dataset.pigProv.xml");
			failedCases
					.add(new TestTuple(
							"e-setNull-dataset.pigProv.xml",
							f7,
							"hdfs://localhost:9000/user/peng/dataset::org.apache.pig.error.SetCharArrayNull_SEC"));

			/*
			 * Repeat the experiments
			 */
			int n = 3, m = 7;
			double sigma = 0.1;

			List<CauseDetermination.Performance> ps = new ArrayList<CauseDetermination.Performance>();
			int numberOfExp = 1000;

			while (numberOfExp-- > 0) {
				CauseDetermination alg = new CauseDetermination();
				/*
				 * Randomly select n successful cases
				 */
				Collections.shuffle(successfulCases, new Random());
				for (int i = 0; i < successfulCases.size() && i < n; i++) {
					alg.addSuccess(successfulCases.get(i).g);
				}

				/*
				 * Randomly select m failure cases
				 */
				Collections.shuffle(failedCases, new Random());
				List<String> realCauses = new ArrayList<String>();
				for (int i = 0; i < failedCases.size() && i < m; i++) {
					alg.addFailure(failedCases.get(i).g);

					// Avoid duplicates
					if (false == realCauses
							.contains(failedCases.get(i).realCause)) {
						realCauses.add(failedCases.get(i).realCause);
					}
				}

				CauseDetermination.Performance p = alg.diagnose(realCauses,
						sigma);
				ps.add(p);
			}

			CauseDetermination.Performance sumP = new CauseDetermination.Performance();
			for (CauseDetermination.Performance p : ps) {
				sumP.addUp(p);
			}

			System.out.println("Total recall:\t" + sumP.getRecall());
			System.out.println("Total precision:\t" + sumP.getPrecision());
			System.out.println("Total recall_co:\t" + sumP.getCoCauseRecall());
			System.out.println("Total precision_co:\t"
					+ sumP.getCoCausePrecision());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static class TestTuple {
		DependencyGraph g;
		String name, realCause;

		public TestTuple(String name, DependencyGraph g, String realCause) {
			this.name = name;
			this.g = g;
			this.realCause = realCause;
		}
	}
}

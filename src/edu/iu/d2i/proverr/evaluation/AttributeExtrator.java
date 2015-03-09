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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.iu.d2i.proverr.diagnosis.DependencyGraph;
import edu.iu.d2i.proverr.diagnosis.DependencyVertex;
import edu.iu.d2i.proverr.diagnosis.XmlParser;

/**
 * Extract components from dependency models to create training instances for
 * machine learning tool Weka
 * 
 * @author peng
 * 
 */
public class AttributeExtrator {
	public static void main(String[] args) throws Exception {
		List<String> successes = new ArrayList<String>();
		successes.add("setNull-1.pig.pigProv.xml");
		successes.add("setNull-2.pig.pigProv.xml");
		successes.add("setNull.pig.pigProv.xml");
		successes.add("lastIndex-store.pig.pigProv.xml");
		successes.add("abs-store-upper.pig.pigProv.xml");
		successes.add("concat-store-avg.pig.pigProv.xml");
		successes.add("concat-store-min.pig.pigProv.xml");
		successes.add("lastIndex-store-lower.pig.pigProv.xml");
		successes.add("e-lower-store-cleandata-avg.pig.pigProv.xml");
		successes.add("lower-store-avg.pig.pigProv.xml");

		List<String> failures = new ArrayList<String>();
		failures.add("e-lastIndex-store.pig.pigProv.xml");
		failures.add("e-abs-store-upper.pig.pigProv.xml");
		failures.add("concat-e-store-avg.pig.pigProv.xml");
		failures.add("e-concat-store-min.pig.pigProv.xml");
		failures.add("e-lastIndex-store-lower.pig.pigProv.xml");
		// failures.add("e-lower-store-avg.pig.pigProv.xml");
		failures.add("e-setNull.pig.pigProv.xml");

		extractFeatures(successes, failures, "diagnositic.feature.csv");
	}

	public static void extractFeatures(List<String> successes,
			List<String> failures, String output) {
		List<DependencyGraph> successGraphs = new ArrayList<DependencyGraph>();
		List<DependencyGraph> failureGraphs = new ArrayList<DependencyGraph>();
		Set<String> features = new HashSet<String>();

		try {
			for (String success : successes) {
				DependencyGraph g = XmlParser.parsePROVXml(success);
				successGraphs.add(g);

				for (DependencyVertex v : g.getAllVertices()) {
					if (v.getReferenceId() != null)
						features.add(v.getReferenceId());
				}
			}

			for (String failure : failures) {
				DependencyGraph g = XmlParser.parsePROVXml(failure);
				failureGraphs.add(g);

				for (DependencyVertex v : g.getAllVertices()) {
					if (v.getReferenceId() != null)
						features.add(v.getReferenceId());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		FileWriter fw = null;
		BufferedWriter bw = null;

		try {

			fw = new FileWriter(new File(output));
			bw = new BufferedWriter(fw);

			String[] featureNames = new String[features.size()];
			featureNames = features.toArray(featureNames);
			bw.append("isFailure");
			for (String featureName : featureNames) {
				bw.append("\t" + featureName);
			}
			bw.newLine();

			for (DependencyGraph g : successGraphs) {
				bw.append("1");// 1 stands for success

				Set<String> includedFeatures = new HashSet<String>();
				for (DependencyVertex v : g.getAllVertices()) {
					if (v.getReferenceId() != null)
						includedFeatures.add(v.getReferenceId());
				}

				for (String featureName : featureNames) {
					if (includedFeatures.contains(featureName)) {
						bw.append("\t1");
					} else {
						bw.append("\t0");
					}
					System.out.println(featureName);
				}

				bw.newLine();
			}

			for (DependencyGraph g : failureGraphs) {
				bw.append("0");// 1 stands for failure
				Set<String> includedFeatures = new HashSet<String>();
				for (DependencyVertex v : g.getAllVertices()) {
					if (v.getReferenceId() != null)
						includedFeatures.add(v.getReferenceId());
				}

				for (String featureName : featureNames) {
					if (includedFeatures.contains(featureName)) {
						bw.append("\t1");
					} else {
						bw.append("\t0");
					}
				}

				bw.newLine();
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (bw != null)
				try {
					bw.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}

			if (bw != null)
				try {
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

			if (fw != null)
				try {
					fw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}
}

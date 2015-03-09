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
import java.util.List;

import edu.iu.d2i.proverr.diagnosis.CauseDetermination;
import edu.iu.d2i.proverr.diagnosis.DependencyGraph;
import edu.iu.d2i.proverr.diagnosis.XmlParser;

/**
 * Evaluate ProvErr on Hardware Problems
 * 
 * @author peng
 * 
 */
public class HardwareProblemEval {
	public static void main(String[] args) {
		CauseDetermination alg = new CauseDetermination();

		String dir = "/home/peng/workspace/ProvErr/Group2/";

		try {
			/*
			 * Add candidate success cases
			 */
			System.out.println("--------SUCCESSFUL-------------");

			DependencyGraph s0 = XmlParser.parsePROVXml(dir
					+ "match-keyword.0.pigProv.xml");
			alg.addSuccess(s0);
			System.out.println("match-keyword.0.pigProv.xml");

			DependencyGraph s1 = XmlParser.parsePROVXml(dir
					+ "match-keyword.1.pigProv.xml");
			alg.addSuccess(s1);
			System.out.println("match-keyword.1.pigProv.xml");

			DependencyGraph s3 = XmlParser.parsePROVXml(dir
					+ "match-keyword.3.pigProv.xml");
			alg.addSuccess(s3);
			System.out.println("match-keyword.3.pigProv.xml");

			DependencyGraph s5 = XmlParser.parsePROVXml(dir
					+ "match-keyword.5.pigProv.xml");
			alg.addSuccess(s5);
			System.out.println("match-keyword.5.pigProv.xml");

			DependencyGraph s6 = XmlParser.parsePROVXml(dir
					+ "match-keyword.6.pigProv.xml");
			alg.addSuccess(s6);
			System.out.println("match-keyword.6.pigProv.xml");

			DependencyGraph s7 = XmlParser.parsePROVXml(dir
					+ "match-keyword.7.pigProv.xml");
			alg.addSuccess(s7);
			System.out.println("match-keyword.7.pigProv.xml");

			DependencyGraph s8 = XmlParser.parsePROVXml(dir
					+ "match-keyword.8.pigProv.xml");
			alg.addSuccess(s8);
			System.out.println("match-keyword.8.pigProv.xml");

			DependencyGraph s9 = XmlParser.parsePROVXml(dir
					+ "match-keyword.9.pigProv.xml");
			alg.addSuccess(s9);
			System.out.println("match-keyword.9.pigProv.xml");

			/*
			 * Add candidate failures
			 */
			System.out.println("--------FAILED-------------");
			List<String> realCauses = new ArrayList<String>();

			realCauses.add("planetlab-3.cs.ucy.ac.cy"); // master node
			realCauses.add("planetlab2.ionio.gr");
			realCauses.add("planetlab2.acis.ufl.edu");

			DependencyGraph f2 = XmlParser.parsePROVXml(dir
					+ "match-keyword.2.pigProv.xml");
			alg.addFailure(f2);
			System.out.println("match-keyword.2.pigProv.xml");

			DependencyGraph f4 = XmlParser.parsePROVXml(dir
					+ "match-keyword.4.pigProv.xml");
			alg.addFailure(f4);
			System.out.println("match-keyword.4.pigProv.xml");

			System.out.println("--------DIAGNOSTICS-------------");
			alg.diagnose(realCauses, 0.1);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

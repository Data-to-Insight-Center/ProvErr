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

package edu.iu.d2i.proverr.diagnosis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 'Cause Determination' algorithm
 * 
 * @author peng
 * 
 */
public class CauseDetermination {
	// if true, consider the redundant cause a true positive in evaluation
	public static boolean considerRedundantCauseCorrect = true;
	// if true, select "strong correlated" causes instead of "correlated" causes
	public static boolean useStrongCorrelation = false;

	Set<DependencyGraph> successGraph = new HashSet<DependencyGraph>();
	Set<DependencyGraph> failureGraph = new HashSet<DependencyGraph>();

	Map<String, List<DependencyGraph>> successVertexMap = new HashMap<String, List<DependencyGraph>>();
	Map<String, List<DependencyGraph>> failureVertexMap = new HashMap<String, List<DependencyGraph>>();

	Map<String, List<DependencyGraph>> completeFailureVertexMap = new HashMap<String, List<DependencyGraph>>();

	private void addToSuccessMap(String key, DependencyGraph g) {
		if (this.successVertexMap.containsKey(key)) {
			this.successVertexMap.get(key).add(g);
		} else {
			List<DependencyGraph> ls = new ArrayList<DependencyGraph>();
			ls.add(g);
			this.successVertexMap.put(key, ls);
		}
	}

	public boolean isFailureGraphEmpty() {
		return failureGraph.isEmpty();
	}

	public void addSuccess(DependencyGraph g) {
		successGraph.add(g);
	}

	public void removeSuccess(DependencyGraph g) {
		successGraph.remove(g);
	}

	private void addToFailureMap(String key, DependencyGraph g) {
		if (this.failureVertexMap.containsKey(key)) {
			this.failureVertexMap.get(key).add(g);
		} else {
			List<DependencyGraph> ls = new ArrayList<DependencyGraph>();
			ls.add(g);
			this.failureVertexMap.put(key, ls);
		}
	}

	public void addFailure(DependencyGraph g) {
		failureGraph.add(g);

		// add all vertices into the complete failure vertex map
		for (DependencyVertex v : g.getAllVertices()) {
			if (v.getReferenceId() != null) {
				if (this.completeFailureVertexMap.containsKey(v
						.getReferenceId())) {
					this.completeFailureVertexMap.get(v.getReferenceId())
							.add(g);
				} else {
					List<DependencyGraph> ls = new ArrayList<DependencyGraph>();
					ls.add(g);
					this.completeFailureVertexMap.put(v.getReferenceId(), ls);
				}
			}
		}
	}

	/*
	 * Check if vertex v1 is correlated to vertex v2:
	 * 
	 * For all faulty runs, if v2 exists in the dependency model,
	 * so does v1. Besides, there must be at least one faulty run
	 * that is caused by v2.
	 */
	public boolean isCorrelated(String v1, String v2) {
		List<DependencyGraph> v2_failures = completeFailureVertexMap.get(v2);
		if (v2_failures == null || v2_failures.size() == 0) {
			return false;
		}

		List<DependencyGraph> v1_failures = completeFailureVertexMap.get(v1);
		Set<DependencyGraph> v1_set = new HashSet<DependencyGraph>(v1_failures);
		Set<DependencyGraph> v2_set = new HashSet<DependencyGraph>(v2_failures);
		v2_set.removeAll(v1_set);

		return v2_set.isEmpty();
	}

	public void removeFailure(DependencyGraph g) {
		failureGraph.remove(g);
	}

	public Set<DependencyGraph> removeFailureGraphs(String vertex) {
		Set<DependencyGraph> removedGraphs = new HashSet<DependencyGraph>();
		if (successVertexMap.containsKey(vertex)) {
			removedGraphs.addAll(successVertexMap.get(vertex));
		}

		if (failureVertexMap.containsKey(vertex)) {
			removedGraphs.addAll(failureVertexMap.get(vertex));
		}

		removedGraphs.retainAll(failureGraph);
		failureGraph.removeAll(removedGraphs);
		return removedGraphs;
	}

	public boolean isAncestor(String v1, String v2) {
		Set<DependencyGraph> graphs1 = new HashSet<DependencyGraph>();
		if (successVertexMap.containsKey(v1))
			graphs1.addAll(successVertexMap.get(v1));
		if (failureVertexMap.containsKey(v1))
			graphs1.addAll(failureVertexMap.get(v1));

		Set<DependencyGraph> graphs2 = new HashSet<DependencyGraph>();
		if (successVertexMap.containsKey(v2))
			graphs2.addAll(successVertexMap.get(v2));
		if (failureVertexMap.containsKey(v2))
			graphs2.addAll(failureVertexMap.get(v2));

		graphs1.retainAll(graphs2);

		for (DependencyGraph g : graphs1) {
			if (g.checkPathByReferenceId(v1, v2)) {
				return true;
			}
		}
		return false;
	}

	public Map<String, Double> getScoreMap() {
		successVertexMap.clear();
		for (DependencyGraph g : successGraph) {
			for (DependencyVertex v : g.getAllVertices()) {
				if (v.getReferenceId() != null) {
					addToSuccessMap(v.getReferenceId(), g);
				}
			}
		}

		failureVertexMap.clear();
		for (DependencyGraph g : failureGraph) {
			for (DependencyVertex v : g.getAllVertices()) {
				if (v.getReferenceId() != null) {
					addToFailureMap(v.getReferenceId(), g);
				}
			}
		}

		Collection<String> allVertices = new HashSet<String>();
		allVertices.addAll(this.successVertexMap.keySet());
		allVertices.addAll(this.failureVertexMap.keySet());

		Map<String, Double> scoreMap = new HashMap<String, Double>();
		for (String vertex : allVertices) {
			// calculate the score
			int n1 = (this.failureVertexMap.containsKey(vertex)) ? this.failureVertexMap
					.get(vertex).size() : 0;
			int n0 = (this.successVertexMap.containsKey(vertex)) ? this.successVertexMap
					.get(vertex).size() : 0;
			double score = (double) n1 / (n1 + n0);

			// if score is 0, then it's not considered as a cause candidate
			if (score > 0)
				scoreMap.put(vertex, score);
		}

		return scoreMap;
	}

	public List<String> findTopVertex(Map<String, Double> scoreMap) {
		TreeMap<Double, List<String>> map = new TreeMap<Double, List<String>>();

		for (String vertex : scoreMap.keySet()) {
			if (map.containsKey(scoreMap.get(vertex))) {
				map.get(scoreMap.get(vertex)).add(vertex);
			} else {
				List<String> ls = new ArrayList<String>();
				ls.add(vertex);
				map.put(scoreMap.get(vertex), ls);
			}
		}

		// return the highest key
		if (map.lastKey() > 0) {
			return map.lastEntry().getValue();
		} else
			return null;
	}

	/**
	 * Diagnose the current list of successes & faults and print out the result
	 * list
	 * 
	 * @param sigma
	 */
	public void diagnose(double sigma) {
		Map<String, Double> scoreMap = getScoreMap();
		while (false == isFailureGraphEmpty()) {
			List<String> topCandidates = findTopVertex(scoreMap);
			if (topCandidates == null)
				// there is no candidate that has a positive score
				break;

			for (String topCandidate : topCandidates) {
				boolean hasAncestor = false;
				for (String otherCandidate : topCandidates) {
					if (false == topCandidate.equals(otherCandidate)) {
						if (isAncestor(otherCandidate, topCandidate)) {
							hasAncestor = true;
							break;
						}
					}
				}

				if (false == hasAncestor) {
					System.out.println("top candidate:\t" + topCandidate);

					Set<DependencyGraph> removedFailureGraphs = removeFailureGraphs(topCandidate);

					Map<String, Double> newScoreMap = getScoreMap();

					Set<String> diffSet = new HashSet<String>(scoreMap.keySet());
					diffSet.removeAll(newScoreMap.keySet());

					for (String s : diffSet) {
						boolean select = false;

						if (// it is not a duplicate
						s.equals(topCandidate)
						// it must falls into the deviation border
								|| scoreMap.get(s) > scoreMap.get(topCandidate)
										- sigma) {
							// decide whether to select strong co-cause or
							// not
							if (useStrongCorrelation
									&& isCorrelated(topCandidate, s)
									&& isCorrelated(s, topCandidate)) {
								select = true;
							} else if (isCorrelated(s, topCandidate)) {
								select = true;
							}
						}

						if (false == select)
							continue;

						boolean isDependent = false;
						for (DependencyGraph g : removedFailureGraphs) {
							if (g.checkPathByReferenceId(topCandidate, s)
									|| g.checkPathByReferenceId(s, topCandidate)) {
								isDependent = true;
								break;
							}
						}

						if (isDependent) {
							System.out.println("\tredundant cause:\t" + s);
						} else {
							System.out.println("\tco-cause:\t" + s);
						}
					}

					scoreMap = newScoreMap;
					break;
				}
			}
		}
	}

	/**
	 * Diagnosing the current list of successes & faults and evaluate the
	 * results with given real causes
	 * 
	 * @param realCauses
	 * @param sigma
	 * @return the object contains the recall and precision
	 */
	public Performance diagnose(List<String> realCauses, double sigma) {
		int n = 0;// number of correctly identified fault causes
		int n_co = 0;// number of correctly identified fault co-causes

		int N = 0; // number of real causes of fault
		int N_co = 0; // number of real co-causes of fault

		List<String> realCoCauses = new ArrayList<String>();
		for (String s : realCauses) {
			int indexOfCol = s.indexOf("::");
			if (indexOfCol != -1) {
				String left = s.substring(0, indexOfCol);
				String right = s.substring(indexOfCol + 2);
				realCoCauses.add(left);
				realCoCauses.add(right);

				N_co += 1;
			} else
				N++;
		}

		int D = 0;// number of diagnosed causes
		int D_co = 0;// number of diagnosed co-causes

		Map<String, Double> scoreMap = getScoreMap();

		while (false == isFailureGraphEmpty()) {
			List<String> topCandidates = findTopVertex(scoreMap);
			if (topCandidates == null)
				// there is no candidate that has a positive score
				break;

			// pick up the top candidate that does not have a 'top
			// candidate' ancestor
			String selectedTopCandidate = null;
			for (String topCandidate : topCandidates) {
				boolean hasAncestor = false;
				for (String otherCandidate : topCandidates) {
					if (false == topCandidate.equals(otherCandidate)) {
						if (isAncestor(otherCandidate, topCandidate)) {
							hasAncestor = true;
							break;
						}
					}
				}

				if (false == hasAncestor) {
					selectedTopCandidate = topCandidate;
					break;
				}
			}

			D++;

			// if (false == hasAncestor) {
			// Check if the selectedTopCandidate is a real cause
			boolean res = false;
			String identifiedCause = null;
			int i = 0;
			for (; i < realCauses.size(); i++) {
				if (selectedTopCandidate.contains(realCauses.get(i))) {
					identifiedCause = realCauses.get(i);
					break;
				}
			}

			if (i < realCauses.size()) {
				res = true;
				n++; // num of correctly diagnosed individual cause ++
				realCauses.remove(i);
			} else {
				int j = 0;
				for (; j < realCoCauses.size(); j++) {
					if (selectedTopCandidate.contains(realCoCauses.get(j))) {
						identifiedCause = realCoCauses.get(j);
						D--; // this is not an individual cause
						break; // only when we match the other part of co-cause,
								// we update the num
					}
				}

				if (j < realCoCauses.size()) {
					res = true;
					realCoCauses.remove(j);
				}
			}

			System.out.println("top candidate:\t" + selectedTopCandidate + "\t"
					+ res + "\t score:" + scoreMap.get(selectedTopCandidate));

			Set<DependencyGraph> removedFailureGraphs = removeFailureGraphs(selectedTopCandidate);

			Map<String, Double> newScoreMap = getScoreMap();

			Set<String> diffSet = new HashSet<String>(scoreMap.keySet());
			diffSet.removeAll(newScoreMap.keySet());

			for (String s : diffSet) {
				boolean select = false;

				// it is not a duplicate
				// it must falls into the deviation border
				if (false == s.equals(selectedTopCandidate)
						&& (scoreMap.get(s) > scoreMap
								.get(selectedTopCandidate) - sigma)) {
					// decide whether to select strong related causes or not
					if (useStrongCorrelation
							&& isCorrelated(selectedTopCandidate, s)
							&& isCorrelated(s, selectedTopCandidate)) {
						select = true;
					} else if (isCorrelated(s, selectedTopCandidate)) {
						select = true;
					}
				}

				if (false == select)
					continue;

				boolean isDependent = false;
				for (DependencyGraph g : removedFailureGraphs) {
					if (g.checkPathByReferenceId(selectedTopCandidate, s)
							|| g.checkPathByReferenceId(s, selectedTopCandidate)) {
						isDependent = true;
						break;
					}
				}

				if (identifiedCause != null && s.contains(identifiedCause)) {
					// a related cause that actually points to the same
					// function
					// for example:
					// "Load:org.apache.pig.error.PigStorage" and
					// "Store:org.apache.pig.error.PigStorage"
					System.out.println("\tduplicate cause:\t" + s);
				} else if (isDependent) {
					// Check if the redundant cause is a real cause
					// diagnosing the redundant cause is considered as
					// failure
					boolean redudant_res = false;
					if (redudant_res == false) {
						int k = 0;
						for (; k < realCauses.size(); k++) {
							if (s.contains(realCauses.get(k))) {
								redudant_res = true;
								break;
							}
						}

						if (k < realCauses.size()) {
							realCauses.remove(k);
						}
					}

					System.out.println("\tredundant cause:\t" + s + "\t"
							+ redudant_res + "\t score:"
							+ scoreMap.get(selectedTopCandidate));

					if (considerRedundantCauseCorrect) {
						D++;
						if (redudant_res) {
							n++;
						}
					}

				} else {
					// Check if the co-cause is a real cause
					boolean co_res = false;

					int k = 0;
					for (; k < realCauses.size(); k++) {
						int indexOfCol = realCauses.get(k).indexOf("::");
						if (indexOfCol != -1) {
							String left = realCauses.get(k).substring(0,
									indexOfCol);
							String right = realCauses.get(k).substring(
									indexOfCol + 2);

							if ((s.contains(right) && selectedTopCandidate
									.contains(left))
									|| (s.contains(left) && selectedTopCandidate
											.contains(right))) {
								co_res = true;
								n_co++;
								break;
							}
						}
					}

					if (k < realCauses.size()) {
						realCauses.remove(k);
					}

					System.out.println("\tco-cause:\t" + s + "\t" + co_res
							+ "\t score:" + scoreMap.get(selectedTopCandidate));

					D_co++;
				}
			}

			scoreMap = newScoreMap;
		}

		System.out.println("--------STATISTICS-------------");
		System.out.println("Number of diagnosed individual causes -- D:\t" + D);
		System.out.println("Number of real individual causes -- N:\t" + N);
		System.out
				.println("Number of correctly diagnosed individual causes -- n:\t"
						+ n);
		System.out.println("Recall(individual cause):\t" + (double) n / N);
		System.out.println("Precision(individual cause):\t" + (double) n / D);
		System.out.println("Number of diagnosed co-causes -- D:\t" + D_co);
		System.out.println("Number of real co-causes -- N:\t" + N_co);
		System.out.println("Number of correctly diagnosed cocauses -- n:\t"
				+ n_co);
		System.out.println("Recall(co-cause):\t" + (double) n_co / N_co);
		System.out.println("Precision(co-cause):\t" + (double) n_co / D_co);

		return new Performance(N, D, n, N_co, D_co, n_co);
	}

	/**
	 * Performance for ProvErr diagnosis that contains recall and precision for
	 * individual causes and co-causes separately
	 * 
	 * @author peng
	 * 
	 */
	public static class Performance {
		public int numOfInCauses, numOfDgInCauses, numOfCorrDgInCauses,
				numOfCoCauses, numOfDgCoCauses, numOfCorrDgCoCauses;

		public Performance() {
		}

		public Performance(int numOfInCauses, int numOfDgInCauses,
				int numOfCorrDgInCauses, int numOfCoCauses,
				int numOfDgCoCauses, int numOfCorrDgCoCauses) {
			this.numOfInCauses = numOfInCauses;
			this.numOfDgInCauses = numOfDgInCauses;
			this.numOfCorrDgInCauses = numOfCorrDgInCauses;
			this.numOfCoCauses = numOfCoCauses;
			this.numOfDgCoCauses = numOfDgCoCauses;
			this.numOfCorrDgCoCauses = numOfCorrDgCoCauses;
		}

		public double getPrecision() {
			return numOfCorrDgInCauses * 1.0 / numOfDgInCauses;
		}

		public double getRecall() {
			return numOfCorrDgInCauses * 1.0 / numOfInCauses;
		}

		public double getCoCausePrecision() {
			return numOfCorrDgCoCauses * 1.0 / numOfDgCoCauses;
		}

		public double getCoCauseRecall() {
			return numOfCorrDgCoCauses * 1.0 / numOfCoCauses;
		}

		public void addUp(Performance p) {
			numOfInCauses = numOfInCauses + p.numOfInCauses;
			numOfDgInCauses = numOfDgInCauses + p.numOfDgInCauses;
			numOfCorrDgInCauses = numOfCorrDgInCauses + p.numOfCorrDgInCauses;
			numOfCoCauses = numOfCoCauses + p.numOfCoCauses;
			numOfDgCoCauses = numOfDgCoCauses + p.numOfDgCoCauses;
			numOfCorrDgCoCauses = numOfCorrDgCoCauses + p.numOfCorrDgCoCauses;
		}
	}

	public static void main(String[] args) throws Exception {
		long startTime = new Date().getTime();

		// repeat a thousand time and measure the time cost
		int numberOfExp = 1000;

		while (numberOfExp-- > 0) {
			CauseDetermination alg = new CauseDetermination();

			String dir = "/home/peng/workspace/ProvErr/Scalibility/100/";

			DependencyGraph s1 = XmlParser.parsePROVXml(dir
					+ "concat-store-min.pigProv.xml");
			alg.addSuccess(s1);

			DependencyGraph f1 = XmlParser.parsePROVXml(dir
					+ "e-concat-store-min.pigProv.xml");
			alg.addFailure(f1);

			List<String> realCauses = new ArrayList<String>();
			realCauses.add("org.apache.pig.error.CONCAT");

			double sigma = 0.1;
			alg.diagnose(realCauses, sigma);
		}

		long endTime = new Date().getTime();
		System.out.println("time elapsed: " + (endTime - startTime));
	}
}

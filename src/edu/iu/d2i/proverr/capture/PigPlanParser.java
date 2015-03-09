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

package edu.iu.d2i.proverr.capture;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parse the Pig MapReduce plan generate using the 'explain' command. A Pig
 * Latin script is transformed into a MapReduce Plan that describes the maps,
 * combines, and reduces and physical operators at each stage of execution.
 * 
 * @author peng
 * 
 */
public class PigPlanParser {
	/**
	 * Verifies that the line starts with:
	 * ' |';
	 * 
	 * or '| |'; and so on...
	 * 
	 * The corresponding regular expr is:
	 * ^(\s)*\|
	 * 
	 * The String passed to Pattern needs to double every backslash:
	 * ^(\\s)*\\|
	 */
	Pattern nodeRegexp = Pattern.compile("^(\\s)*\\|");
	Matcher nodeMatcher = nodeRegexp.matcher("");

	/**
	 * Verifies that the line starts with:
	 * ' | Project[bytearray][0] - scope-23';
	 * 
	 * or * '| | Project[bytearray][0] - scope-23'; and so on ...
	 * 
	 * The corresponding regular expr is:
	 * ^(\|(\s)+)*(\s)*\|(\s)+\w
	 * 
	 * The String passed to Pattern needs to double every backslash:
	 * ^(\\|(\\s)+)*(\\s)*\\|(\\s)+\\w
	 */
	Pattern siblingRegexp = Pattern.compile("^(\\|(\\s)+)*(\\s)*\\|(\\s)+\\w");
	Matcher siblingMatcher = siblingRegexp.matcher("");

	/**
	 * Verifies that the line starts with:
	 * '|---ngramed1: New For Each(false,false,true)[bag] - scope-30' or
	 * ' | |---Project[chararray][2] - scope-27'
	 * 
	 * The corresponding regular expr is:
	 * ^(\|(\s)+)*(\s)*(\|(\s)+)?\|---\w
	 * 
	 * The String passed to Pattern needs to double every backslash:
	 * ^(\\|(\\s)+)*(\\s)*(\\|(\\s)+)?\\|---\\w
	 */
	Pattern childRegexp = Pattern
			.compile("^(\\|(\\s)+)*(\\s)*(\\|(\\s)+)?\\|---\\w");
	Matcher childMatcher = childRegexp.matcher("");

	/**
	 * Verifies that the part of line like:
	 * '- scope-30'
	 * 
	 * The corresponding regular expr is:
	 * \w*-\sscope-[0-9]+
	 * 
	 * The String passed to Pattern needs to double every backslash:
	 * \\w*-\\sscope-[0-9]+
	 */
	Pattern scopeRegexp = Pattern.compile("\\w*-\\sscope-[0-9]+");
	Matcher scopeMatcher = scopeRegexp.matcher("");

	/**
	 * check if the given line matches the siblingRegexp
	 * 
	 * @param line
	 *            given string
	 * @return boolean result
	 */
	public boolean testSibling(String line) {
		siblingMatcher.reset(line);
		return siblingMatcher.find();
	}

	/**
	 * Extract "Project[bytearray][0] - scope-14" from
	 * "        |   Project[bytearray][0] - scope-14"
	 * 
	 * @param line
	 *            given string
	 * @return return the index of a matched sibling; otherwise return -1
	 */
	public int indexOfSibling(String line) {
		if (testSibling(line)) {
			return siblingMatcher.end() - 1;
		} else
			return -1;
	}

	/**
	 * Extract "Project[bytearray][0]" from
	 * "Project[bytearray][0] - scope-14"
	 * 
	 * @param line
	 *            the node content
	 * @return the operator string
	 */
	public String matchSiblingOperator(String line) {
		if (line != null) {
			String operatorWithParameter = line.substring(0,
					line.lastIndexOf("- scope")).trim();
			String operator = (operatorWithParameter.indexOf('[') != -1) ? operatorWithParameter
					.substring(0, operatorWithParameter.indexOf('['))
					: operatorWithParameter;
			return operator;
		} else
			return null;
	}

	/**
	 * check if the given line matches the childRegexp
	 * 
	 * @param line
	 *            given string
	 * @return boolean result
	 */
	public boolean testChild(String line) {
		childMatcher.reset(line);
		return childMatcher.find();
	}

	/**
	 * find the index of
	 * "houred: New For Each(false,false,false)[bag] - scope-22" in
	 * "    |---houred: New For Each(false,false,false)[bag] - scope-22"
	 * 
	 * @param line
	 *            given string
	 * @return return the index of a matched child; otherwise return -1
	 */
	public int indexOfChild(String line) {
		if (testChild(line)) {
			return childMatcher.end() - 1;
		} else
			return -1;
	}

	/**
	 * Extract "houred:" from
	 * "houred: New For Each(false,false,false)[bag] - scope-22"
	 * 
	 * @param line
	 *            the node content
	 * @return the alias string
	 */
	public String matchChildAlias(String line) {
		int index = -1;
		if (line != null && (index = line.indexOf(':')) != -1) {
			return line.substring(0, index);
		} else
			return null;
	}

	/**
	 * Extract "New For Each(false,false,false)" from
	 * "houred: New For Each(false,false,false)[bag] - scope-22"
	 * 
	 * @param line
	 *            the result from 'matchChild'
	 * @return the operator string
	 */
	public String matchChildOperator(String line) {
		int index = -1;
		if (line != null) {
			String tmp = line;
			if ((index = line.indexOf(':')) != -1)
				tmp = line.substring(index + 1);

			// return tmp.substring(0, tmp.lastIndexOf("- scope")).trim();
			String operatorWithParameter = tmp.substring(0,
					tmp.lastIndexOf("- scope")).trim();
			String operator = (operatorWithParameter.indexOf('[') != -1) ? operatorWithParameter
					.substring(0, operatorWithParameter.indexOf('['))
					: operatorWithParameter;
			return operator;
		} else
			return null;
	}

	/**
	 * Extract "scope-22" from
	 * "houred: New For Each(false,false,false)[bag] - scope-22"
	 * 
	 * @param line
	 * @return the scope string
	 */
	public String matchScope(String line) {
		scopeMatcher.reset(line);
		if (scopeMatcher.find()) {
			return line.substring(scopeMatcher.start() + 2, scopeMatcher.end());
		}

		return null;
	}

	/**
	 * Parse the root node of a map/reduce/combine plan
	 * 
	 * @param line
	 * @return PlanNode
	 */
	public PlanNode parseRootNode(String line) {
		String scope = matchScope(line);

		if (line.startsWith("Store")) {// store operation without an alias
			// 6 is the index of char after "Store"
			String tmp = line.substring(6, line.lastIndexOf(')'));
			int index = tmp.lastIndexOf(':');

			String location = tmp.substring(0, index);
			String operator = "Store:" + tmp.substring(index + 1);

			return new PlanNode.Builder(0, operator, scope, PlanNodeType.ROOT)
					.location(location).build();
		} else {
			String operator = matchChildOperator(line);
			String alias = matchChildAlias(line);

			if (operator.startsWith("Store")) {
				String tmp = operator.substring(6, operator.lastIndexOf(')'));
				int index = tmp.lastIndexOf(':');

				String location = tmp.substring(0, index);
				operator = "Store:" + tmp.substring(index + 1);
				return new PlanNode.Builder(0, operator, scope,
						PlanNodeType.ROOT).alias(alias).location(location)
						.build();
			}

			return new PlanNode.Builder(0, operator, scope, PlanNodeType.ROOT)
					.alias(alias).build();
		}
	}

	/**
	 * Parse the inner node of a plan
	 * 
	 * @param line
	 * @return
	 */
	public PlanNode parseInnerNode(String line) {
		int indexOfContent;
		String scope = matchScope(line);

		if ((indexOfContent = indexOfChild(line)) != -1) {// parse the child
															// node
			String content = line.substring(indexOfContent);

			if (content.startsWith("Load")) {// the node does not have a alias
												// and is a 'load' operation
				String parameter = null, tmp = null;
				// 5 is the first char after "Load"
				if (content.indexOf('(', 5) != -1) {// has a parameter
					parameter = content.substring(content.indexOf('(', 5),
							content.lastIndexOf(')'));
					tmp = content.substring(5, content.indexOf('(', 5));
				} else {
					tmp = content.substring(5, content.lastIndexOf(')'));
				}

				int index = tmp.lastIndexOf(':');

				String location = tmp.substring(0, index);
				String operator = tmp.substring(index + 1);
				operator = "Load:" + operator;
				return new PlanNode.Builder(indexOfContent, operator, scope,
						PlanNodeType.PRECEDENT).location(location)
						.parameter(parameter).build();
			} else {
				String operator = matchChildOperator(content);
				String alias = matchChildAlias(content);

				if (operator.startsWith("Load")) {
					// 5 is the first char after "Load"
					String parameter = operator.substring(
							operator.indexOf('(', 5), operator.length() - 1);
					String tmp = operator
							.substring(5, operator.indexOf('(', 5));
					int index = tmp.lastIndexOf(':');

					String location = tmp.substring(0, index);
					operator = "Load:" + tmp.substring(index + 1);
					return new PlanNode.Builder(indexOfContent, operator,
							scope, PlanNodeType.PRECEDENT).alias(alias)
							.location(location).parameter(parameter).build();
				}

				return new PlanNode.Builder(indexOfContent, operator, scope,
						PlanNodeType.PRECEDENT).alias(alias).build();
			}
		} else if ((indexOfContent = indexOfSibling(line)) != -1) {// parse a
																	// sibling
																	// node
			String content = line.substring(indexOfContent);
			String operator = matchSiblingOperator(content);

			return new PlanNode.Builder(indexOfContent, operator, scope,
					PlanNodeType.CHILD).build();
		} else
			// not a valid node, could be ' | |'
			return null;
	}

	/**
	 * Link the new node to its parent
	 * 
	 * @param levelList
	 *            contains the most recently encountered node at each level
	 * */
	private void linkToParent(List<PlanNode> levelList, PlanNode newNode) {
		boolean exists = false;
		for (int i = 0; i < levelList.size(); i++) {
			if (levelList.get(i).getLevelOfIndent() == newNode
					.getLevelOfIndent()) {
				PlanNode nodeFromLastLevel = levelList.get(i - 1);

				if (newNode.getType() == PlanNodeType.CHILD) {
					nodeFromLastLevel.addChild(newNode);
				} else if (newNode.getType() == PlanNodeType.PRECEDENT) {
					nodeFromLastLevel.addPrecedent(newNode);
				}

				levelList.set(i, newNode);
				exists = true;
				break;
			}
		}

		if (false == exists) {
			if (false == levelList.isEmpty()) {
				PlanNode nodeFromLastLevel = levelList
						.get(levelList.size() - 1);
				if (newNode.getType() == PlanNodeType.CHILD) {
					nodeFromLastLevel.addChild(newNode);
				} else if (newNode.getType() == PlanNodeType.PRECEDENT) {
					nodeFromLastLevel.addPrecedent(newNode);
				}
			}
			levelList.add(newNode);
		}
	}

	public void printPlan(PlanNode node) {
		if (node != null) {
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < node.getLevelOfIndent(); i++) {
				sb.append("  ");
			}
			System.out.println(sb.toString() + node.getOperator());

			List<PlanNode> children = node.getChildren();
			if (children.size() != 0) {
				System.out.println(sb.toString() + "<children>");
				for (PlanNode child : children) {
					printPlan(child);
				}
				System.out.println(sb.toString() + "</children>");
			}

			List<PlanNode> precedents = node.getPrecedents();
			if (precedents.size() != 0) {
				System.out.println(sb.toString() + "<precedent>");
				for (PlanNode precedent : precedents) {
					printPlan(precedent);
				}
				System.out.println(sb.toString() + "</precedent>");
			}
		}
	}

	/**
	 * Parse the file that has mapReduce plans
	 * 
	 * @param file
	 */
	public List<PlanJob> parsePlan(File file) {
		FileReader fr = null;
		BufferedReader br = null;
		List<PlanJob> result = new ArrayList<PlanJob>();

		try {
			fr = new FileReader(file);
			br = new BufferedReader(fr);

			// List stores the most-recent node at each level
			List<PlanNode> levelList = new ArrayList<PlanNode>();

			String readLine = null;
			while ((readLine = br.readLine()) != null) {
				if (readLine.startsWith("MapReduce node")) {
					System.out.println("Find a MapReduce job:");
					PlanJob job = null;

					readLine = br.readLine();
					/*
					 * Match a map job if possible
					 */
					if (readLine != null && readLine.equals("Map Plan")) {
						System.out.println("Map:");
						String mapEnd = br.readLine();
						PlanNode root = parseRootNode(mapEnd);
						System.out.println("\t" + root.getLevelOfIndent()
								+ "\t" + root.getAlias() + "\t"
								+ root.getOperator());
						job = new PlanJob(root);
						levelList.clear();
						linkToParent(levelList, root);

						while ((readLine = br.readLine()) != null) {
							nodeMatcher.reset(readLine);
							if (nodeMatcher.find()) {
								PlanNode node = parseInnerNode(readLine);
								if (node != null) {
									linkToParent(levelList, node);
									System.out.println("\t"
											+ node.getLevelOfIndent() + "\t"
											+ node.getAlias() + "\t"
											+ node.getOperator() + "\t"
											+ node.getLocation() + "\t"
											+ node.getScope());
								}

							} else
								break;
						}
					}

					/*
					 * Match a combine job if possible
					 */
					if (readLine != null && readLine.equals("Combine Plan")) {
						System.out.println("Combiner:");
						String combineEnd = br.readLine();
						PlanNode root = parseRootNode(combineEnd);
						System.out.println("\t" + root.getLevelOfIndent()
								+ "\t" + root.getAlias() + "\t"
								+ root.getOperator());
						job.setCombineRoot(root);
						levelList.clear();
						linkToParent(levelList, root);

						while ((readLine = br.readLine()) != null) {
							nodeMatcher.reset(readLine);
							if (nodeMatcher.find()) {
								PlanNode node = parseInnerNode(readLine);
								if (node != null) {
									linkToParent(levelList, node);
									System.out.println("\t"
											+ node.getLevelOfIndent() + "\t"
											+ node.getAlias() + "\t"
											+ node.getOperator() + "\t"
											+ node.getLocation() + "\t"
											+ node.getScope());
								}
							} else
								break;
						}
					}

					/*
					 * Match a reduce job if possible
					 */
					if (readLine != null && readLine.equals("Reduce Plan")) {
						System.out.println("Reduce:");
						String reduceEnd = br.readLine();
						PlanNode root = parseRootNode(reduceEnd);
						System.out.println("\t" + root.getLevelOfIndent()
								+ "\t" + root.getAlias() + "\t"
								+ root.getOperator() + "\t"
								+ root.getLocation());
						job.setReduceRoot(root);
						levelList.clear();
						linkToParent(levelList, root);

						while ((readLine = br.readLine()) != null) {
							nodeMatcher.reset(readLine);
							if (nodeMatcher.find()) {
								PlanNode node = parseInnerNode(readLine);
								if (node != null) {
									linkToParent(levelList, node);
									System.out.println("\t"
											+ node.getLevelOfIndent() + "\t"
											+ node.getAlias() + "\t"
											+ node.getOperator() + "\t"
											+ node.getLocation() + "\t"
											+ node.getScope());
								}
							} else
								break;
						}
					}

					if (job != null)
						result.add(job);
				}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null)
				try {
					br.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}

			if (fr != null)
				try {
					fr.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
		}

		return result;
	}

	public static void main(String[] args) {
		PigPlanParser parser = new PigPlanParser();

		List<PlanJob> jobs = parser.parsePlan(new File(
				"e-setNull.pig.explain.txt"));
		for (PlanJob job : jobs) {
			System.out.println("Map:");
			parser.printPlan(job.getMapRoot());
			System.out.println("Combine:");
			parser.printPlan(job.getCombineRoot());
			System.out.println("Reduce:");
			parser.printPlan(job.getReduceRoot());
		}
	}
}

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
#
 */

package edu.iu.d2i.proverr.capture;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.RunningJob;
import org.apache.hadoop.mapred.TaskCompletionEvent;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.purl.dc.terms.HasPart;
import org.purl.net.pPlan.IsInputVarOf;
import org.purl.net.pPlan.IsOutputVarOf;
import org.purl.net.pPlan.IsPreceededBy;
import org.purl.net.pPlan.IsStepOfPlan;
import org.purl.net.pPlan.Step;
import org.purl.net.pPlan.Variable;
import org.w3.ns.prov.Activity;
import org.w3.ns.prov.Association;
import org.w3.ns.prov.Collection;
import org.w3.ns.prov.Communication;
import org.w3.ns.prov.Document;
import org.w3.ns.prov.DocumentDocument;
import org.w3.ns.prov.Entity;
import org.w3.ns.prov.Generation;
import org.w3.ns.prov.IDRef;
import org.w3.ns.prov.Membership;
import org.w3.ns.prov.SoftwareAgent;
import org.w3.ns.prov.Usage;

/**
 * Build dependency models using 'PigLogParser', 'PigPlanParser' and
 * 'MachineTracker'
 * 
 * @author peng
 * 
 */
public class DependencyModelBuilder {
	public String jobTrackerIP = "localhost";
	public int jobTrackerPort = 9001;

	private final String ns = "http://pig.d2i.iu.edu#";
	private int documentStepId = 0;
	private int documentPlanId = 0;

	// whether to collect job history info from hadoop or not
	private boolean queryHadoopTurnedOn = false;

	private String prefix = "";
	private String script_name = "setNull.pig";
	private String inputPigLogFile;
	private String inputPigPlanFile;
	private String outputXmlFile;

	public DependencyModelBuilder(String jobTrackerIP, int jobTrackerPort,
			boolean queryHadoopTurnedOn, String script_name, String directory) {
		this.jobTrackerIP = jobTrackerIP;
		this.jobTrackerPort = jobTrackerPort;
		this.queryHadoopTurnedOn = queryHadoopTurnedOn;
		this.script_name = script_name;
		this.prefix = directory;

		this.inputPigLogFile = prefix + script_name + ".out.txt";
		this.inputPigPlanFile = prefix + script_name + ".explain.txt";
		this.outputXmlFile = prefix + script_name + ".pigProv.xml";
	}

	public DependencyModelBuilder(String jobTrackerIP, int jobTrackerPort,
			boolean queryHadoopTurnedOn, String script_name) {
		this(jobTrackerIP, jobTrackerPort, queryHadoopTurnedOn, script_name,
				"./");
	}

	public void build() {
		DocumentDocument docdoc = DocumentDocument.Factory.newInstance();
		Document doc = docdoc.addNewDocument();

		IDRef jobTrackerAgentRef = null;// job tracker
		IDRef hadoopAgentRef = null;
		IDRef pigAgentRef = null;

		Map<String, IDRef> jobMap = new HashMap<String, IDRef>();
		Map<String, IDRef> featureMap = new HashMap<String, IDRef>();
		Map<String, IDRef> taskTrackerMap = new HashMap<String, IDRef>();

		PigLogParser logParser = new PigLogParser();
		PigLog log = logParser.parseLog(new File(inputPigLogFile));

		/*
		 * Store all job id into a list for late node matching
		 */
		List<String> jobIds = new LinkedList<String>();
		List<IDRef> planRefs = new LinkedList<IDRef>();

		if (log != null) {
			/*
			 * Collect provenance from pig log statistics
			 */
			log.getScripStats().printStats();
			LogStats script = log.getScripStats();

			/*
			 * Add pig agent
			 */
			SoftwareAgent pigAgent = doc.addNewSoftwareAgent();
			String pigId = "Pig_" + script.getStatsValue("PigVersion");
			pigAgent.setId(new QName(ns, pigId));
			pigAgent.addNewLabel().setStringValue(
					"PigVersion:" + script.getStatsValue("PigVersion"));
			pigAgentRef = IDRef.Factory.newInstance();
			pigAgentRef.setRef(pigAgent.getId());

			/*
			 * Add hadoop agent
			 */
			SoftwareAgent hadoopAgent = doc.addNewSoftwareAgent();
			String hadoopId = "Hadoop_" + script.getStatsValue("HadoopVersion");
			hadoopAgent.setId(new QName(ns, hadoopId));
			hadoopAgent.addNewLabel().setStringValue(
					"HadoopVersion:" + script.getStatsValue("HadoopVersion"));
			hadoopAgentRef = IDRef.Factory.newInstance();
			hadoopAgentRef.setRef(hadoopAgent.getId());

			/*
			 * Create the pig script entity
			 */
			Entity scriptEntity = doc.addNewEntity();
			String scriptId = "Script(" + script_name + "_"
					+ script.getStatsValue("UserId") + "_"
					+ script.getStatsValue("StartedAt") + ")";
			scriptEntity.setId(new QName(ns, toNCName(scriptId)));
			// we breakdown the script by parsing its plan
			// scriptEntity.addNewLabel().setStringValue("Script:" +
			// script_name);
			IDRef scriptEntityRef = IDRef.Factory.newInstance();
			scriptEntityRef.setRef(scriptEntity.getId());

			/*
			 * Create the compilation activity with unique id
			 */
			Activity compilation = doc.addNewActivity();
			String compilationId = "Compilation_"
					+ script.getStatsValue("UserId") + "_"
					+ script.getStatsValue("StartedAt");
			compilation.setId(new QName(ns, toNCName(compilationId)));
			compilation.addNewLabel().setStringValue("Compilation");
			IDRef compilationRef = IDRef.Factory.newInstance();
			compilationRef.setRef(compilation.getId());

			/*
			 * compilation uses the script
			 */
			Usage compilationUsage = doc.addNewUsed();
			compilationUsage.setEntity(scriptEntityRef);
			compilationUsage.setActivity(compilationRef);

			/*
			 * Collect provenance from pig environment
			 */
			log.getEnvs().printEnvs();
			PigEnvironment envs = log.getEnvs();
			for (String envName : envs.getNames()) {
				String envValue = envs.getEnv(envName);
				if (envName.equals("logicalOptimizers")) {
					envValue = envValue.substring(envValue
							.indexOf("RULES_ENABLED"));
					envValue = envValue.substring(envValue.indexOf('[') + 1,
							envValue.indexOf(']'));

					/*
					 * Create a collection that stands for all "optimizers"
					 */
					Collection optimizerCollection = doc.addNewCollection();
					optimizerCollection.setId(new QName(ns, envName));
					IDRef collectionRef = IDRef.Factory.newInstance();
					collectionRef.setRef(optimizerCollection.getId());

					/*
					 * Compilation used the optimizers
					 */
					Usage usedRelation = doc.addNewUsed();
					usedRelation.setActivity(compilationRef);
					usedRelation.setEntity(collectionRef);

					/*
					 * Initialize the membership relations
					 */
					String[] optimizers = envValue.split(",");
					Membership membership = doc.addNewHadMember();
					membership.setCollection(collectionRef);
					IDRef[] entityArray = new IDRef[optimizers.length];

					for (int i = 0; i < optimizers.length; i++) {
						String optimizer = optimizers[i].trim();
						/*
						 * Create a optimizer entity
						 */
						Entity optimizerEntity = doc.addNewEntity();
						optimizerEntity.setId(new QName(ns, "Optimizer_"
								+ optimizer));
						optimizerEntity.addNewLabel().setStringValue(
								"Optimizer:" + optimizer);
						IDRef entityRef = IDRef.Factory.newInstance();
						entityRef.setRef(optimizerEntity.getId());
						entityArray[i] = entityRef;
					}

					/*
					 * Add the entity array to the collection as members
					 */
					membership.setEntityArray(entityArray);
				} else if (envName.equals("jobTracker")) {

					SoftwareAgent jobTrackerAgent = doc.addNewSoftwareAgent();
					String id = envName + ":" + envValue;
					jobTrackerAgent.setId(new QName(ns, toNCName(id)));
					jobTrackerAgent.addNewLabel().setStringValue(id);
					jobTrackerAgentRef = IDRef.Factory.newInstance();
					jobTrackerAgentRef.setRef(jobTrackerAgent.getId());
				}
			}

			for (LogJob job : log.getJobs()) {

				Activity jobActivity = doc.addNewActivity();
				jobActivity.setId(new QName(ns, job.getId()));
				// intermediate dependents aren't important
				// jobActivity.addNewLabel().setStringValue(job.getId());
				IDRef jobActivityRef = IDRef.Factory.newInstance();
				jobActivityRef.setRef(jobActivity.getId());
				jobMap.put(job.getId(), jobActivityRef);
				jobIds.add(job.getId());

				/*
				 * Create a MapReduce Job Plan
				 */
				Collection plan = doc.addNewCollection();
				String planId = script_name + "_"
						+ script.getStatsValue("UserId") + "_"
						+ script.getStatsValue("StartedAt") + "_Plan_"
						+ documentPlanId++;
				plan.setId(new QName(ns, toNCName(planId)));
				// the intermediate dependency isn't important
				// plan.addNewLabel().setStringValue(planId);
				IDRef planRef = IDRef.Factory.newInstance();
				planRef.setRef(plan.getId());
				planRefs.add(planRef);

				/*
				 * compilation generated the plan
				 */
				Generation generation = doc.addNewWasGeneratedBy();
				generation.setActivity(compilationRef);
				generation.setEntity(planRef);

				/*
				 * Job is executed by Pig following the
				 * MapReduce Plan
				 */
				Association execution = doc.addNewWasAssociatedWith();
				execution.setActivity(jobActivityRef);
				execution.setAgent(pigAgentRef);
				execution.setPlan(planRef);

				/*
				 * Job is controlled by JobTracker,
				 */
				Association jobControl = doc.addNewWasAssociatedWith();
				jobControl.setActivity(jobActivityRef);
				jobControl.setAgent(jobTrackerAgentRef);

				/*
				 * Job is run on Hadoop
				 */
				Association runOn = doc.addNewWasAssociatedWith();
				runOn.setActivity(jobActivityRef);
				runOn.setAgent(hadoopAgentRef);

				job.printStats();
				for (String statsName : job.getAllStatsName()) {
					String statsValue = job.getStatsValue(statsName);

					if (statsName.equals("Feature")) {
						String[] features = statsValue.split(",");
						for (String feature : features) {
							feature = feature.trim();
							if (false == featureMap.containsKey(feature)) {
								// create the feature entity
								Entity featureEntity = doc.addNewEntity();
								featureEntity.setId(new QName(ns, "Feature_"
										+ feature));
								featureEntity.addNewLabel().setStringValue(
										"Feature:" + feature);
								IDRef featureEntityRef = IDRef.Factory
										.newInstance();
								featureEntityRef.setRef(featureEntity.getId());
								featureMap.put(feature, featureEntityRef);
							}

							/*
							 * Job used some Pig feature
							 */
							Usage usage = doc.addNewUsed();
							usage.setActivity(jobActivityRef);
							usage.setEntity(featureMap.get(feature));
						}
					} else {
						/*
						 * Add attributes to job
						 */
						try {
							XmlObject xobj = XmlObject.Factory.parse("<pig:"
									+ statsName
									+ " xmlns:pig=\"http://pig.d2i.iu.edu#\">"
									+ statsValue + "</pig:" + statsName + ">");
							XmlCursor c1 = xobj.newCursor();
							c1.toFirstContentToken();

							XmlCursor cursor = jobActivity.newCursor();
							cursor.toEndToken();

							c1.moveXml(cursor);
							c1.dispose();
						} catch (XmlException e) {
							e.printStackTrace();
						}
					}
				}
			}

			for (LogJob job : log.getJobs()) {
				job.printDependents();

				for (LogJob dependentJob : job.getDependentJobs()) {
					/*
					 * Add Job dependency
					 */
					Communication comm = doc.addNewWasInformedBy();
					comm.setInformant(jobMap.get(job.getId()));
					comm.setInformed(jobMap.get(dependentJob.getId()));
				}
			}
		}

		if (jobIds.size() > 0) {
			/*
			 * Collect provenance from MapReduce plan
			 */
			PigPlanParser planParser = new PigPlanParser();

			Iterator<String> itr_job = jobIds.iterator();
			Iterator<IDRef> itr_IDRef = planRefs.iterator();

			List<PlanJob> jobs = planParser
					.parsePlan(new File(inputPigPlanFile));
			for (PlanJob job : jobs) {
				if (itr_job.hasNext()) {
					// String jobId = itr_job.next();
					IDRef planRef = itr_IDRef.next();

					/*
					 * Collect Map operations
					 */
					System.out.println("Map:");
					planParser.printPlan(job.getMapRoot());
					PlanNode mapRoot = job.getMapRoot();

					if (mapRoot != null) {
						Step mapRootStep = doc.addNewStep();
						String stepId = script_name
								+ "_"
								+ log.getScripStats().getStatsValue("UserId")
								+ "_"
								+ log.getScripStats()
										.getStatsValue("StartedAt")
								+ "_MapPlan_" + documentStepId++;
						mapRootStep.setId(new QName(ns, toNCName(stepId)));
						IDRef mapRootStepRef = IDRef.Factory.newInstance();
						mapRootStepRef.setRef(mapRootStep.getId());

						/*
						 * Add this mapRootPlan as a child step of plan
						 */
						IsStepOfPlan isStepOfPlan = doc.addNewIsStepOfPlan();
						isStepOfPlan.setPlan(planRef);
						isStepOfPlan.setStep(mapRootStepRef);

						/*
						 * Recursively write all child operations
						 */
						collectLogJobProv(mapRoot, mapRootStepRef, null, doc,
								log);
					}

					/*
					 * Collect provenance of combine operations
					 */
					System.out.println("Combine:");
					planParser.printPlan(job.getCombineRoot());
					PlanNode combineRoot = job.getCombineRoot();

					if (combineRoot != null) {
						Step combineRootStep = doc.addNewStep();
						String stepId = script_name
								+ "_"
								+ log.getScripStats().getStatsValue("UserId")
								+ "_"
								+ log.getScripStats()
										.getStatsValue("StartedAt")
								+ "_CombinePlan_" + documentStepId++;
						combineRootStep.setId(new QName(ns, toNCName(stepId)));
						IDRef combineRootStepRef = IDRef.Factory.newInstance();
						combineRootStepRef.setRef(combineRootStep.getId());

						/*
						 * Add this mapRootPlan as a child step of plan
						 */
						IsStepOfPlan isStepOfPlan = doc.addNewIsStepOfPlan();
						isStepOfPlan.setPlan(planRef);
						isStepOfPlan.setStep(combineRootStepRef);

						/*
						 * Recursively write all child operations
						 */
						collectLogJobProv(combineRoot, combineRootStepRef,
								null, doc, log);
					}

					/*
					 * Collect provenance of reduce operations
					 */
					System.out.println("Reduce:");
					planParser.printPlan(job.getReduceRoot());
					PlanNode reduceRoot = job.getReduceRoot();

					if (reduceRoot != null) {
						Step reduceRootStep = doc.addNewStep();
						String stepId = script_name
								+ "_"
								+ log.getScripStats().getStatsValue("UserId")
								+ "_"
								+ log.getScripStats()
										.getStatsValue("StartedAt")
								+ "_ReducePlan_" + documentStepId++;
						reduceRootStep.setId(new QName(ns, toNCName(stepId)));
						IDRef reduceRootStepRef = IDRef.Factory.newInstance();
						reduceRootStepRef.setRef(reduceRootStep.getId());

						/*
						 * Add this mapRootPlan as a child step of plan
						 */
						IsStepOfPlan isStepOfPlan = doc.addNewIsStepOfPlan();
						isStepOfPlan.setPlan(planRef);
						isStepOfPlan.setStep(reduceRootStepRef);

						/*
						 * Recursively write all child operations
						 */
						collectLogJobProv(reduceRoot, reduceRootStepRef, null,
								doc, log);
					}
				} else
					break;// this can happen if there are jobs crashed

				/*
				 * Collect task provenance from TaskTracker
				 */
				if (queryHadoopTurnedOn) {
					Configuration conf = new Configuration();
					// conf.addResource(new Path(
					// "/home/peng/Dropbox/python-scripts/conf/core-site.xml"));
					// conf.addResource(new Path(
					// "/home/peng/Dropbox/python-scripts/conf/hdfs-site.xml"));
					InetSocketAddress jobtracker = new InetSocketAddress(
							jobTrackerIP, jobTrackerPort);

					try {
						MachineTracker tracker = new MachineTracker(jobtracker,
								conf);

						Iterator<String> itr = jobIds.iterator();
						while (itr.hasNext()) {
							String jobId = itr.next();

							RunningJob runningJob = tracker
									.getRunningJob(jobId);
							List<TaskCompletionEvent> tasks = tracker
									.getTaskEvents(runningJob);
							for (TaskCompletionEvent task : tasks) {
								System.out.println(task.getTaskTrackerHttp());

								Activity taskActivity = doc.addNewActivity();
								taskActivity.setId(new QName(ns, task
										.getTaskAttemptId().toString()));
								IDRef taskActivityRef = IDRef.Factory
										.newInstance();
								taskActivityRef.setRef(taskActivity.getId());

								/*
								 * Create a taskTracker by Http addr if not
								 * exists
								 */
								if (false == taskTrackerMap.containsKey(task
										.getTaskTrackerHttp())) {
									SoftwareAgent taskTraker = doc
											.addNewSoftwareAgent();
									taskTraker
											.setId(new QName(ns, toNCName(task
													.getTaskTrackerHttp())));
									taskTraker.addNewLabel().setStringValue(
											task.getTaskTrackerHttp());
									IDRef taskTrackerRef = IDRef.Factory
											.newInstance();
									taskTrackerRef.setRef(taskTraker.getId());

									taskTrackerMap.put(
											task.getTaskTrackerHttp(),
											taskTrackerRef);
								}

								/*
								 * taskAttempt was associated to a taskTracker
								 */
								Association association = doc
										.addNewWasAssociatedWith();
								association.setActivity(taskActivityRef);
								association.setAgent(taskTrackerMap.get(task
										.getTaskTrackerHttp()));

								/*
								 * The task is a part of a job
								 */
								HasPart hasPart = doc.addNewHasPart();
								hasPart.setWhole(jobMap.get(jobId));
								hasPart.setPart(taskActivityRef);
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			}
		}

		File xmlFile = new File(outputXmlFile);
		try {
			ArrayList<String> validationErrors = new ArrayList<String>();
			XmlOptions validationOptions = new XmlOptions();
			validationOptions.setErrorListener(validationErrors);

			boolean isValid = docdoc.validate(validationOptions);
			if (false == isValid) {
				for (Object error : validationErrors) {
					System.out.println(">>" + error);
				}
			}

			docdoc.save(xmlFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Recursion to collect the provenance of all jobs in the tree
	 * 
	 * @param parentJob
	 *            the parent job
	 * @param parentRef
	 *            the IDRef of the job node
	 */
	private void collectLogJobProv(PlanNode currentOperator,
			IDRef parentStepRef, IDRef postStepRef, Document doc, PigLog log) {

		Step currentStep = doc.addNewStep();
		String currentStepId = script_name + "_"
				+ log.getScripStats().getStatsValue("UserId") + "_"
				+ log.getScripStats().getStatsValue("StartedAt") + "_Step_"
				+ documentStepId++;
		currentStep.setId(new QName(ns, toNCName(currentStepId)));
		IDRef currentStepRef = IDRef.Factory.newInstance();
		currentStepRef.setRef(currentStep.getId());

		writePlanNodeAttributes(currentStep, currentOperator, doc);

		/*
		 * Add current step as a child step of parent step
		 */
		IsStepOfPlan isStepOfPlan = doc.addNewIsStepOfPlan();
		isStepOfPlan.setPlan(parentStepRef);
		isStepOfPlan.setStep(currentStepRef);

		if (postStepRef != null) {
			/*
			 * Add it as the proceeder of postStepRef
			 */
			IsPreceededBy isPreceededBy = doc.addNewIsPreceededBy();
			isPreceededBy.setPostStep(postStepRef);
			isPreceededBy.setPriorStep(currentStepRef);
		}

		for (PlanNode node : currentOperator.getChildren()) {
			/*
			 * Traverse children
			 */
			collectLogJobProv(node, currentStepRef, null, doc, log);
		}

		for (PlanNode node : currentOperator.getPrecedents()) {
			/*
			 * Add it as the proceeder of parentStepRef
			 */
			collectLogJobProv(node, parentStepRef, currentStepRef, doc, log);
		}
	}

	private void writePlanNodeAttributes(Step step, PlanNode node, Document doc) {
		/*
		 * Add the used relation
		 */
		// Usage usage = doc.addNewUsed();
		IDRef stepRef = IDRef.Factory.newInstance();
		stepRef.setRef(step.getId());

		step.addNewLabel().setStringValue(node.getOperator());
		if (node.getLocation() != null) {
			if (node.getOperator().startsWith("Load:")) {
				/*
				 * Add the load file as an input variable
				 */
				Variable input = doc.addNewVariable();
				input.setId(new QName(ns, toNCName(node.getLocation() + "_"
						+ node.getScope())));
				input.addNewValue().setStringValue(node.getLocation());

				/*
				 * Add the inputOf relation
				 */
				IsInputVarOf isInputVarOf = doc.addNewIsInputVarOf();
				isInputVarOf.setStep(stepRef);
				IDRef variableRef = IDRef.Factory.newInstance();
				variableRef.setRef(input.getId());
				isInputVarOf.setVariable(variableRef);
			} else if (node.getOperator().startsWith("Store:")) {
				/*
				 * Add the store file as an output variable
				 */
				Variable output = doc.addNewVariable();
				output.setId(new QName(ns, toNCName(node.getLocation() + "_"
						+ node.getScope())));
				output.addNewValue().setStringValue(node.getLocation());

				/*
				 * Add the inputOf relation
				 */
				IsOutputVarOf isOutputVarOf = doc.addNewIsOutputVarOf();
				isOutputVarOf.setStep(stepRef);
				IDRef variableRef = IDRef.Factory.newInstance();
				variableRef.setRef(output.getId());
				isOutputVarOf.setVariable(variableRef);
			} else
				step.addNewLocation().setStringValue(node.getLocation());
		}
	}

	private static String toNCName(String str) {
		/*
		 * Change it to valid NCName -- "non-colonized name" to pass the
		 * validation
		 */
		return str.replaceAll("[^\\w-_.]", "_");
	}

	// to be used in the local test mode
	// public static void main(String[] args) {
	// String dir = "/home/peng/workspace/ProvErr/Group3/";
	// // String[] pigFileNames = { "abs-store-upper", "concat-store-min",
	// // "e-lastIndex-store-lower", "lastIndex-store", "setNull-1",
	// // "concat-e-store-avg", "e-abs-store-upper", "e-lastIndex-store",
	// // "e-setNull", "lower-store-avg", "setNull-2",
	// // "concat-store-avg", "e-concat-store-min",
	// // "lastIndex-store-lower", "lower-store", "setNull",
	// // "e-setNull-dataset", "setNull-clean-dataset" };
	//
	// // String[] pigFileNames = { "match-keyword.0", "match-keyword.1",
	// // "match-keyword.2", "match-keyword.3", "match-keyword.4",
	// // "match-keyword.5", "match-keyword.6", "match-keyword.7",
	// // "match-keyword.8", "match-keyword.9" };
	//
	// String[] pigFileNames = { "concat-store-avg" };
	//
	// for (String pigFileName : pigFileNames) {
	// DependencyModelBuilder builder = new DependencyModelBuilder(
	// "planetlab2.acis.ufl.edu", 14001, true, pigFileName, dir);
	// builder.build();
	// }
	// }

	// to be invoked from the exported jar file
	public static void main(String[] args) {
		if (args.length != 6) {
			System.out
					.println("usage:java -jar modelBuilder.jar -f filename -h host -p port");
			System.exit(-1);
		}

		String fileName = null, host = null;
		int port = -1;
		for (int i = 0; i < args.length; i += 2) {
			if (args[i].equals("-f")) {
				fileName = args[i + 1];
			} else if (args[i].equals("-h")) {
				host = args[i + 1];
			} else if (args[i].equals("-p")) {
				port = Integer.valueOf(args[i + 1]);
			} else {
				System.out.println("option" + args[i] + " not recognized");
				System.exit(-1);
			}
		}

		DependencyModelBuilder builder = new DependencyModelBuilder(host, port,
				true, fileName);
		builder.build();
	}
}

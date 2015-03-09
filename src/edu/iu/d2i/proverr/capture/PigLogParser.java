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
import java.util.HashMap;
import java.util.Map;

/**
 * Parse the standard Pig output (redirected into a log file)
 * 
 * @author peng
 *
 */
public class PigLogParser {
	/*
	 * keywordBook has all the keywords and the corresponding env-names to be
	 * matched; it's initialized in a separate block that can be customized in a
	 * configuration file lately
	 */
	private final Map<String, String> keywordBook;
	
	public PigLogParser() {
		keywordBook = new HashMap<String, String>();
		keywordBook.put("hadoop file system at:", "hdfs");
		keywordBook.put("map-reduce job tracker at:", "jobTracker");
		keywordBook
				.put("org.apache.pig.newplan.logical.optimizer.LogicalPlanOptimizer -",
						"logicalOptimizers");
	}

	public PigLog parseLog(File file) {
		PigLog log = new PigLog(keywordBook);

		FileReader fr = null;
		BufferedReader br = null;

		try {
			fr = new FileReader(file);
			br = new BufferedReader(fr);

			String readLine = null;
			while ((readLine = br.readLine()) != null) {
				/*
				 * Match environment keywords
				 */
				String[] keywords = new String[log.getEnvs().getkeywords()
						.size()];
				log.getEnvs().getkeywords().toArray(keywords);

				for (String keyword : keywords) {
					if (readLine.contains(keyword)) {
						String value = readLine.substring(
								readLine.indexOf(keyword) + keyword.length())
								.trim();
						log.getEnvs().addEnvByKeyword(keyword, value);
					}
				}

				/*
				 * Stop at "- Script Statistics: "
				 */
				if (readLine.contains("- Script Statistics: ")) {
					break;
				}
			}

			/*
			 * Parse after "- Script Statistics: "
			 */
			while ((readLine = br.readLine()) != null) {
				if (readLine.startsWith("HadoopVersion")) {
					String[] labels = readLine.split("\t");

					// get the next value line
					readLine = br.readLine();
					String[] values = readLine.split("\t");

					for (int i = 0; i < labels.length; i++) {
						log.getScripStats().addStatsItem(labels[i], values[i]);
					}

					break;
				}
			}

			while ((readLine = br.readLine()) != null) {
				/*
				 * Parse "Job Stats"
				 */
				if (readLine.startsWith("Job Stats")) {
					readLine = br.readLine();
					String[] labels = readLine.split("\t");

					// get value lines
					while ((readLine = br.readLine()) != null
							&& false == readLine.equals("")) {
						String[] values = readLine.split("\t");

						LogJob job = new LogJob();
						for (int i = 0; i < labels.length; i++) {
							if (i < values.length) {// may not have the
													// corresponding attribute
								if (labels[i].equals("JobId")) {
									job.setId(values[i]);
								} else
									job.addStatsItem(labels[i], values[i]);
							} else
								break;
						}

						log.addJob(job.getId(), job);
					}

					break;
				}

				/*
				 * Parse "Failed Jobs"
				 */
				if (readLine.startsWith("Failed Jobs")) {
					readLine = br.readLine();
					String[] labels = readLine.split("\t");

					// get value lines
					while ((readLine = br.readLine()) != null
							&& false == readLine.equals("")) {
						String[] values = readLine.split("\t");

						LogJob job = new LogJob();
						for (int i = 0; i < labels.length; i++) {
							if (i < values.length) {// may not have the
													// corresponding attribute
								if (labels[i].equals("JobId")) {
									job.setId(values[i]);
								} else
									job.addStatsItem(labels[i], values[i]);
							} else
								break;
						}

						log.addJob(job.getId(), job);
					}

					break;
				}
			}

			/*
			 * Parse "Job DAG"
			 */
			while ((readLine = br.readLine()) != null) {
				if (readLine.startsWith("Job DAG")) {
					while ((readLine = br.readLine()) != null
							&& false == readLine.equals("")) {
						String[] strs = readLine.split("->");
						if (strs.length == 2) {
							String leftJobId = strs[0].trim();
							String rightJobId = strs[1].substring(0,
									strs[1].length() - 1).trim();

							if (false == leftJobId.equals("null")
									&& false == rightJobId.equals("null")) {
								LogJob leftJob = log.getJob(leftJobId);
								LogJob rightJob = log.getJob(rightJobId);
								rightJob.addDependentJob(leftJob);
							}
						}
					}

					break;
				}
			}

			return log;
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

		return null;
	}

	public static void main(String[] args) {
		PigLogParser parser = new PigLogParser();
		PigLog log = parser.parseLog(new File(
				"crash_script1-hadoop.pig.out.txt"));

		if (log != null) {
			log.getEnvs().printEnvs();
			log.getScripStats().printStats();
			for (LogJob job : log.getJobs()) {
				System.out.println(job.getId());
				job.printDependents();
				job.printStats();
			}
		}
	}
}

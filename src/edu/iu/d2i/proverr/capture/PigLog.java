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
# -----------------------------------------------------------------
#
# Project: ProvErr
# File: PigLog.java
# Description: 
#
# -----------------------------------------------------------------
#
 */

package edu.iu.d2i.proverr.capture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The output (redirected into a log) from a Pig script execution
 * 
 * @author peng
 *
 */
public class PigLog {
	// Pig script environments
	private PigEnvironment envs;
	// Properties
	private LogStats scriptStats = new LogStats();
	// Pig jobs that were run in the execution
	private List<LogJob> jobs = new ArrayList<LogJob>();
	// Map between jobId to jobs
	private Map<String, LogJob> jobIdMap = new HashMap<String, LogJob>();

	public PigLog(Map<String, String> keywordBook) {
		this.envs = new PigEnvironment(keywordBook);
	}

	public PigEnvironment getEnvs() {
		return envs;
	}

	public LogStats getScripStats() {
		return scriptStats;
	}

	public List<LogJob> getJobs() {
		return jobs;
	}

	/**
	 * 
	 * @param jobId
	 *            Add a pigJob with jobId
	 * @param job
	 */
	public void addJob(String jobId, LogJob job) {
		jobs.add(job);
		jobIdMap.put(jobId, job);
	}

	/**
	 * 
	 * @param jobId
	 * @return PigJob instance
	 */
	public LogJob getJob(String jobId) {
		return jobIdMap.get(jobId);
	}
}

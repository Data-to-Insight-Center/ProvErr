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
# File: LogJob.java
# Description: a Pig Job inside a 'PigLog'
#
# -----------------------------------------------------------------
#
 */

package edu.iu.d2i.proverr.capture;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LogJob {
	// Unique job Id
	private String jobId;
	// <name, value> pair for job properties
	private final Map<String, String> stats = new HashMap<String, String>();
	// List of dependents
	private List<LogJob> dependentJobs = new LinkedList<LogJob>();

	public LogJob() {
	}

	public LogJob(String jobId) {
		this.jobId = jobId;
	}

	public void setId(String jobId) {
		this.jobId = jobId;
	}

	public List<LogJob> getDependentJobs() {
		return dependentJobs;
	}

	public void addDependentJob(LogJob dependentJob) {
		this.dependentJobs.add(dependentJob);
	}

	public String getId() {
		return jobId;
	}

	public void addStatsItem(String name, String value) {
		stats.put(name, value);
	}

	public Set<String> getAllStatsName() {
		return stats.keySet();
	}

	public String getStatsValue(String name) {
		return stats.get(name);
	}

	public void printStats() {
		for (String statsName : stats.keySet()) {
			System.out.println(statsName + ":\t" + stats.get(statsName));
		}
	}

	public void printDependents() {
		for (LogJob job : dependentJobs) {
			System.out.println("<-" + job.jobId);
		}
	}
}

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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.*;

/**
 * Connect to an existing JobClient to get job (task) history
 * 
 * @author peng
 *
 */
public class MachineTracker {
	// jobClient that is connected to a given jobTracker
	private JobClient jobClient;

	public MachineTracker(InetSocketAddress jobtracker, Configuration conf)
			throws IOException {
		jobClient = new JobClient(jobtracker, conf);
	}

	public JobStatus[] getAllJobStatus() throws IOException {
		return jobClient.getAllJobs();
	}

	public RunningJob[] getAllRunningJob() throws IOException {
		JobStatus[] allJobStatus = getAllJobStatus();
		RunningJob[] allJobs = new RunningJob[allJobStatus.length];

		for (int i = 0; i < allJobStatus.length; i++) {
			allJobs[i] = jobClient.getJob(allJobStatus[i].getJobID());
		}

		return allJobs;
	}

	public RunningJob getRunningJob(String jobId) throws IOException {
		return jobClient.getJob(jobId);
	}

	public List<TaskCompletionEvent> getTaskEvents(RunningJob job)
			throws Exception {
		List<TaskCompletionEvent> completionEvents = new LinkedList<TaskCompletionEvent>();

		while (true) {
			TaskCompletionEvent[] bunchOfEvents;
			bunchOfEvents = job
					.getTaskCompletionEvents(completionEvents.size());

			if (bunchOfEvents == null || bunchOfEvents.length == 0) {
				break;
			} else {
				completionEvents.addAll(Arrays.asList(bunchOfEvents));
			}
		}

		return completionEvents;
	}

	public void printCurrentJobs() throws Exception {
		RunningJob[] jobs = getAllRunningJob();

		for (RunningJob job : jobs) {
			System.out.println("jobID:\t" + job.getJobID());
			System.out.println("jobName:\t" + job.getJobName());
			System.out.println("isJobComplete:\t" + job.isComplete());
			System.out.println("isSuccessful:\t" + job.isSuccessful());

			List<TaskCompletionEvent> tasks = getTaskEvents(job);
			for (TaskCompletionEvent task : tasks) {
				System.out.println("\ttaskID:\t" + task.getTaskAttemptId());
				System.out.println("\tisMapTask:\t" + task.isMapTask());
				System.out.println("\tgetTaskStatus:\t" + task.getTaskStatus());
				System.out.println("\tMachine Addr:\t"
						+ task.getTaskTrackerHttp());
			}
		}
	}

	/**
	 * Test Code to connect to an existing JobTracker
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Configuration conf = new Configuration();
		conf.addResource(new Path(
				"/home/peng/Desktop/hadoop-1.2.1/conf/core-site.xml"));
		conf.addResource(new Path(
				"/home/peng/Desktop/hadoop-1.2.1/conf/hdfs-site.xml"));
		InetSocketAddress jobtracker = new InetSocketAddress("localhost", 9001);

		try {
			MachineTracker tracker = new MachineTracker(jobtracker, conf);
			RunningJob job = tracker.getRunningJob("job_201311121537_0015");
			List<TaskCompletionEvent> tasks = tracker.getTaskEvents(job);
			for (TaskCompletionEvent task : tasks) {
				System.out.println(task.getTaskTrackerHttp());
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}

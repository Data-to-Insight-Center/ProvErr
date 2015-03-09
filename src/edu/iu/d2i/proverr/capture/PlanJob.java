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

/**
 * A Pig job in Pig's MapReduce Plan
 * 
 * @author peng
 *
 */
public class PlanJob {
	private final PlanNode mapRoot; // required
	private PlanNode combineRoot; // optional
	private PlanNode reduceRoot; // optional

	public PlanJob(PlanNode mapRoot) {
		this.mapRoot = mapRoot;
	}

	public void setCombineRoot(PlanNode combineRoot) {
		this.combineRoot = combineRoot;
	}

	public void setReduceRoot(PlanNode reduceRoot) {
		this.reduceRoot = reduceRoot;
	}

	public PlanNode getMapRoot() {
		return mapRoot;
	}

	public PlanNode getCombineRoot() {
		return combineRoot;
	}

	public PlanNode getReduceRoot() {
		return reduceRoot;
	}
}

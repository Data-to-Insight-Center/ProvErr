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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * a set of properties in 'PigLog'
 * 
 * @author peng
 *
 */
public class LogStats {
	// Properties: <name, value> pair
	private final Map<String, String> stats = new HashMap<String, String>();

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
}

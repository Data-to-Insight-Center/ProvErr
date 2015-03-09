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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * a set of environment properties inside a PigLog
 * 
 * @author peng
 *
 */
public class PigEnvironment {
	// <keyword-to-match, env-name>: list of keywords to be matched when parsing
	// for environments
	private final Map<String, String> keywordBook;
	// <env-name, env-value>: environment properties
	private final Map<String, String> envs = new HashMap<String, String>();

	public PigEnvironment(Map<String, String> keywordBook) {
		this.keywordBook = keywordBook;
	}

	/**
	 * 
	 * @return the to-match list of keywords
	 */
	public Set<String> getkeywords() {
		return keywordBook.keySet();
	}

	/**
	 * Add one property key-value pair, and remove the corresponding keyword
	 * from to-match list
	 * 
	 * @param keyword
	 * @param env
	 * @return
	 */
	public boolean addEnvByKeyword(String keyword, String env) {
		if (keywordBook.containsKey(keyword)) {
			envs.put(keywordBook.get(keyword), env);
			keywordBook.remove(keyword);
			return true;
		} else
			return false;
	}

	/**
	 * 
	 * @return The names of all captured environment properties
	 */
	public Set<String> getNames() {
		return envs.keySet();
	}

	/**
	 * 
	 * @param envName
	 *            Env name
	 * @return value of property
	 */
	public String getEnv(String envName) {
		if (envs.containsKey(envName)) {
			return envs.get(envName);
		} else
			return null;
	}

	public void printEnvs() {
		for (String envName : envs.keySet()) {
			System.out.println(envName + ":\t" + envs.get(envName));
		}
	}
}

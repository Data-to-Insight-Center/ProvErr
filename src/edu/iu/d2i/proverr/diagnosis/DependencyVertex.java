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

package edu.iu.d2i.proverr.diagnosis;

import java.util.LinkedList;
import java.util.List;

/**
 * A node in dependency graph
 * 
 * @author peng
 *
 */
public class DependencyVertex {
	private final String id;
	private List<DependencyVertex> children = new LinkedList<DependencyVertex>();
	private List<DependencyVertex> parents = new LinkedList<DependencyVertex>();
	private String referenceId, nodeMark;

	public DependencyVertex(String id) {
		this.id = id;
	}

	public DependencyVertex(String id, String referenceId) {
		this.id = id;
		this.referenceId = referenceId;
	}

	public DependencyVertex(String id, String referenceId, String nodeMark) {
		this.id = id;
		this.referenceId = referenceId;
		this.nodeMark = nodeMark;
	}

	public String getId() {
		return id;
	}

	public void setReferenceId(String referenceId) {
		this.referenceId = referenceId;
	}

	public String getReferenceId() {
		return referenceId;
	}

	public void setNodeMark(String nodeMark) {
		this.nodeMark = nodeMark;
	}

	public String getNodeMark() {
		return nodeMark;
	}

	public void addChild(DependencyVertex child) {
		children.add(child);
		child.parents.add(this);
	}

	public void addParent(DependencyVertex parent) {
		parent.children.add(this);
		parents.add(parent);
	}

	public List<DependencyVertex> getChildren() {
		return children;
	}

	public List<DependencyVertex> getParents() {
		return parents;
	}
}

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

package edu.iu.d2i.proverr.diagnosis;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A dependency graph
 * 
 * @author peng
 *
 */
public class DependencyGraph {
	private final String id;
	private Map<String, DependencyVertex> vertexMap = new HashMap<String, DependencyVertex>();
	private Map<String, List<DependencyVertex>> referenceIdMap = new HashMap<String, List<DependencyVertex>>();

	public DependencyGraph(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void addVertex(DependencyVertex v) {
		vertexMap.put(v.getId(), v);

		if (v.getReferenceId() != null) {
			if (false == referenceIdMap.containsKey(v.getReferenceId())) {
				referenceIdMap
						.put(v.getReferenceId(), new LinkedList<DependencyVertex>());
			}
			referenceIdMap.get(v.getReferenceId()).add(v);
		}
	}

	public void removeVertex(String id) {
		if (vertexMap.containsKey(id))
			vertexMap.remove(id);
	}

	public DependencyVertex getVertex(String id) {
		return vertexMap.get(id);
	}

	public Set<String> getReferenceIds() {
		return referenceIdMap.keySet();
	}

	public List<DependencyVertex> getVertexByReferenceId(String referenceId) {
		return referenceIdMap.get(referenceId);
	}

	public Collection<DependencyVertex> getAllVertices() {
		return vertexMap.values();
	}

	public boolean addChild(String parentId, String childId) {
		DependencyVertex parent = getVertex(parentId);
		DependencyVertex child = getVertex(childId);
		if (parent == null || child == null) {
			return false;
		} else {
			parent.addChild(child);
			return true;
		}
	}

	/**
	 * Check if there is a path from vertex 'fromId' to vertex 'toId'
	 * 
	 * @param fromId
	 *            vertex id
	 * @param toId
	 *            vertex id
	 * @return
	 */
	public boolean checkPath(String fromId, String toId) {
		// Label the node that has been traveled
		Set<DependencyVertex> travelTags = new HashSet<DependencyVertex>();

		DependencyVertex from = getVertex(fromId);
		DependencyVertex to = getVertex(toId);
		return checkPath(from, to, travelTags);
	}

	/**
	 * Check if there is a path from vertex by reference Id 'from' to vertex
	 * 'to'
	 * 
	 * @param fromId
	 *            vertex reference id
	 * @param toId
	 *            vertex reference id
	 * @return
	 */
	public boolean checkPathByReferenceId(String fromId, String toId) {
		List<DependencyVertex> froms = getVertexByReferenceId(fromId);
		List<DependencyVertex> tos = getVertexByReferenceId(toId);

		for (DependencyVertex from : froms) {
			for (DependencyVertex to : tos) {
				if (checkPath(from, to))
					return true;
			}
		}

		return false;
	}

	/**
	 * Check if there is a path from vertex 'from' to vertex 'to'
	 * 
	 * @param from
	 *            vertex
	 * @param to
	 *            vertex
	 * @return
	 */
	public boolean checkPath(DependencyVertex from, DependencyVertex to) {
		// Label the node that has been traveled
		Set<DependencyVertex> travelTags = new HashSet<DependencyVertex>();

		return checkPath(from, to, travelTags);
	}

	private boolean checkPath(DependencyVertex from, DependencyVertex to, Set<DependencyVertex> travelTags) {
		if (from == null || to == null) {
			return false;
		} else if (from.equals(to)) {
			return true;
		} else {
			// DFS
			for (DependencyVertex child : from.getChildren()) {
				if (false == travelTags.contains(child)) {
					travelTags.add(child);
					if (checkPath(child, to, travelTags)) {
						return true;
					}
					travelTags.remove(child);
				}
			}

			return false;
		}
	}

	public static void main(String[] args) {
		DependencyGraph g = new DependencyGraph("graph");
		DependencyVertex v1 = new DependencyVertex("v1");
		g.addVertex(v1);

		DependencyVertex v2 = new DependencyVertex("v2");
		g.addVertex(v2);
		DependencyVertex v3 = new DependencyVertex("v3");
		g.addVertex(v3);

		v1.addChild(g.getVertex("v3"));
		v2.addChild(g.getVertex("v3"));

		DependencyVertex v4 = new DependencyVertex("v4");
		g.addVertex(v4);
		v3.addChild(g.getVertex("v4"));

		v4.addChild(g.getVertex("v1"));

		System.out.println(g.checkPath("v1", "v2"));
		System.out.println(g.checkPath("v1", "v4"));
		System.out.println(g.checkPath("v4", "v2"));
		System.out.println(g.checkPath("v4", "v3"));
	}
}

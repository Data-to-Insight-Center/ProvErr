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

 */

package edu.iu.d2i.proverr.capture;

import java.util.LinkedList;
import java.util.List;

/**
 * A node in the hierarchical (tree-like) Pig MapReduce Plan; Builder pattern is
 * used.
 * 
 * @author peng
 * 
 */
public class PlanNode {
	// required: the level of indent of current parsing line
	private final int levelOfIndent;
	// required: name of operator in current line
	private final String operator;
	// required: operator scope in current line
	private final String scope;
	// required: node type
	private final PlanNodeType type;
	
	// optional: any location info in current line
	private final String location; // optional
	// optional: any alias introduced in current line
	private final String alias;
	// parameter: any parameter used in the operator (like UDF) in current line
	private final String parameter;

	// Children list of current node
	private final List<PlanNode> children = new LinkedList<PlanNode>();
	// list of nodes preceding current nodes
	private final List<PlanNode> precedents = new LinkedList<PlanNode>();

	private PlanNode(Builder builder) {
		this.operator = builder.operator;
		this.levelOfIndent = builder.levelOfIndent;
		this.scope = builder.scope;
		this.type = builder.type;
		
		this.location = builder.location;
		this.alias = builder.alias;
		this.parameter = builder.parameter;
	}

	public int getLevelOfIndent() {
		return levelOfIndent;
	}

	public String getOperator() {
		return operator;
	}
	
	public String getScope() {
		return scope;
	}

	public String getLocation() {
		return location;
	}

	public String getAlias() {
		return alias;
	}

	public String getParameter() {
		return parameter;
	}

	public List<PlanNode> getChildren() {
		return children;
	}
	
	public void addChild(PlanNode child) {
		children.add(child);
	}
	
	public List<PlanNode> getPrecedents() {
		return precedents;
	}
	
	public void addPrecedent(PlanNode precedent) {
		precedents.add(precedent);
	}
	
	public PlanNodeType getType() {
		return type;
	}

	/**
	 * Builder Pattern
	 * 
	 * @author peng
	 * 
	 */
	public static class Builder {
		private final int levelOfIndent; // required
		private final String operator; // required
		private final String scope; //required
		private final PlanNodeType type; //required
		
		private String location; // optional
		private String alias; // optional
		private String parameter; // optional

		public Builder(int levelOfIndent, String operator, String scope, PlanNodeType type) {
			this.levelOfIndent = levelOfIndent;
			this.operator = operator;
			this.scope = scope;
			this.type = type;
		}

		public Builder location(String location) {
			this.location = location;
			return this;
		}

		public Builder alias(String alias) {
			this.alias = alias;
			return this;
		}

		public Builder parameter(String parameter) {
			this.parameter = parameter;
			return this;
		}

		public PlanNode build() {
			return new PlanNode(this);
		}
	}
}

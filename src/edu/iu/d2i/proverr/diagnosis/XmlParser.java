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

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.purl.dc.terms.HasPart;
import org.purl.net.pPlan.IsInputVarOf;
import org.purl.net.pPlan.IsOutputVarOf;
import org.purl.net.pPlan.IsPreceededBy;
import org.purl.net.pPlan.IsStepOfPlan;
import org.purl.net.pPlan.Step;
import org.purl.net.pPlan.Variable;
import org.w3.ns.prov.Activity;
import org.w3.ns.prov.Agent;
import org.w3.ns.prov.Association;
import org.w3.ns.prov.Collection;
import org.w3.ns.prov.Communication;
import org.w3.ns.prov.Document;
import org.w3.ns.prov.DocumentDocument;
import org.w3.ns.prov.Entity;
import org.w3.ns.prov.Generation;
import org.w3.ns.prov.IDRef;
import org.w3.ns.prov.InternationalizedString;
import org.w3.ns.prov.Membership;
import org.w3.ns.prov.SoftwareAgent;
import org.w3.ns.prov.Usage;

/**
 * Parsing the dependency models stored in XML format
 * 
 * @author peng
 * 
 */
public class XmlParser {
	public static DependencyGraph parsePROVXml(String xmlFilePath)
			throws Exception {
		File xmlFile = new File(xmlFilePath);
		return parsePROVXml(xmlFile);
	}

	public static DependencyGraph parsePROVXml(File xmlFile) throws Exception {
		DocumentDocument docdoc = DocumentDocument.Factory.parse(xmlFile);
		Document doc = docdoc.getDocument();

		DependencyGraph provGraph = new DependencyGraph(xmlFile.getName());
		/*
		 * Process all agents
		 */
		Agent[] agents = doc.getAgentArray();
		for (Agent agent : agents) {
			DependencyVertex v = new DependencyVertex(agent.getId().toString());
			InternationalizedString[] labels = agent.getLabelArray();
			if (labels != null && labels.length > 0) {
				v.setReferenceId(labels[0].getStringValue().toString());
			}

			provGraph.addVertex(v);
		}

		/*
		 * Process all softwareAgents
		 */
		SoftwareAgent[] softwareAgents = doc.getSoftwareAgentArray();
		for (SoftwareAgent softwareAgent : softwareAgents) {
			DependencyVertex v = new DependencyVertex(softwareAgent.getId()
					.toString());
			InternationalizedString[] labels = softwareAgent.getLabelArray();
			if (labels != null && labels.length > 0) {
				v.setReferenceId(labels[0].getStringValue().toString());
			}

			provGraph.addVertex(v);
		}

		/*
		 * Process all activities
		 */
		Activity[] activities = doc.getActivityArray();
		for (Activity activity : activities) {
			DependencyVertex v = new DependencyVertex(activity.getId()
					.toString());
			InternationalizedString[] labels = activity.getLabelArray();
			if (labels != null && labels.length > 0) {
				v.setReferenceId(labels[0].getStringValue().toString());
			}

			provGraph.addVertex(v);
		}

		/*
		 * Process all collections
		 */
		Collection[] collections = doc.getCollectionArray();
		for (Collection collection : collections) {
			DependencyVertex v = new DependencyVertex(collection.getId()
					.toString());
			InternationalizedString[] labels = collection.getLabelArray();
			if (labels != null && labels.length > 0) {
				v.setReferenceId(labels[0].getStringValue().toString());
			}

			provGraph.addVertex(v);
		}

		/*
		 * Process all entities
		 */
		Entity[] entities = doc.getEntityArray();
		for (Entity entity : entities) {
			DependencyVertex v = new DependencyVertex(entity.getId().toString());
			InternationalizedString[] labels = entity.getLabelArray();
			if (labels != null && labels.length > 0) {
				v.setReferenceId(labels[0].getStringValue().toString());
			}

			provGraph.addVertex(v);
		}

		/*
		 * Process all planSteps
		 */
		Step[] steps = doc.getStepArray();
		for (Step step : steps) {
			DependencyVertex v = new DependencyVertex(step.getId().toString());
			InternationalizedString[] labels = step.getLabelArray();
			if (labels != null && labels.length > 0) {
				// COMMENTED OUT --- plan step is an abstract operator thus is
				// not considered as cause candidate
				// v.setReferenceId(labels[0].getStringValue().toString());
				v.setReferenceId(labels[0].getStringValue().toString()
						.replaceAll("\\[\\d*\\]", ""));// [n] means to repeat
				// the same step n times
			}

			provGraph.addVertex(v);
		}

		/*
		 * Process all variables
		 */
		Variable[] variables = doc.getVariableArray();
		for (Variable variable : variables) {
			DependencyVertex v = new DependencyVertex(variable.getId()
					.getLocalPart());
			v.setReferenceId(variable.getValue().stringValue());

			provGraph.addVertex(v);
		}

		/*
		 * Remove all output variables --- we do not consider the output data as
		 * cause candidates
		 */
		IsOutputVarOf[] isOutputVarOfs = doc.getIsOutputVarOfArray();
		for (IsOutputVarOf isOutputVarOf : isOutputVarOfs) {
			// output depends on the step
			String parentId = isOutputVarOf.getVariable().getRef()
					.getLocalPart().toString();
			provGraph.removeVertex(parentId);
		}

		/*
		 * Process all isPreceededBy relations
		 */
		IsPreceededBy[] isPreceededBys = doc.getIsPreceededByArray();
		for (IsPreceededBy isPreceededBy : isPreceededBys) {
			// post depends on its prior
			String parentId = isPreceededBy.getPriorStep().getRef().toString();
			String childId = isPreceededBy.getPostStep().getRef().toString();

			if (false == provGraph.addChild(parentId, childId))
				throw new Exception(
						"isPreceededBy relations not matched to vertex!");
		}

		/*
		 * Process all isInputVarOf relations
		 */
		IsInputVarOf[] isInputVarOfs = doc.getIsInputVarOfArray();
		for (IsInputVarOf isInputVarOf : isInputVarOfs) {
			// step depends on its input
			String parentId = isInputVarOf.getVariable().getRef()
					.getLocalPart().toString();
			String childId = isInputVarOf.getStep().getRef().toString();

			if (false == provGraph.addChild(parentId, childId))
				throw new Exception(
						"isInputVarOfs relations not matched to vertex!");
		}

		/*
		 * Process all isStepOfPlan relations
		 */
		IsStepOfPlan[] isStepOfPlans = doc.getIsStepOfPlanArray();
		for (IsStepOfPlan isStepOfPlan : isStepOfPlans) {
			// plan depends on its step
			String parentId = isStepOfPlan.getStep().getRef().toString();
			String childId = isStepOfPlan.getPlan().getRef().toString();

			if (false == provGraph.addChild(parentId, childId))
				throw new Exception(
						"isStepOfPlan relations not matched to vertex!");

			provGraph.getVertex(parentId).setNodeMark("step");
			provGraph.getVertex(childId).setNodeMark("plan");
		}

		/*
		 * Process all isOutputVarOf relations: COMMENTED OUT --- we do not
		 * consider the output data as cause candidates
		 */
		// IsOutputVarOf[] isOutputVarOfs = doc.getIsOutputVarOfArray();
		// for (IsOutputVarOf isOutputVarOf : isOutputVarOfs) {
		// // output depends on the step
		// String parentId = isOutputVarOf.getVariable().getRef()
		// .getLocalPart().toString();
		// String childId = isOutputVarOf.getStep().getRef().toString();
		//
		// if (false == provGraph.addChild(parentId, childId))
		// throw new Exception(
		// "isOutputVarOf relations not matched to vertex!");
		// }

		/*
		 * Process all association relations
		 */
		Association[] associations = doc.getWasAssociatedWithArray();
		for (Association association : associations) {
			// activity depends on its associated agent
			String parentId = association.getAgent().getRef().toString();
			String childId = association.getActivity().getRef().toString();

			if (false == provGraph.addChild(parentId, childId))
				throw new Exception(
						"association relations not matched to vertex!");

			if (association.isSetPlan()) {
				String parentId2 = association.getPlan().getRef().toString();
				if (false == provGraph.addChild(parentId2, childId))
					throw new Exception(
							"association relations not matched to vertex!");
			}
		}

		/*
		 * Process all Generation relations
		 */
		Generation[] generations = doc.getWasGeneratedByArray();
		for (Generation generation : generations) {
			// entity depends on the activity
			String parentId = generation.getActivity().getRef().toString();
			String childId = generation.getEntity().getRef().toString();

			if (false == provGraph.addChild(parentId, childId))
				throw new Exception(
						"generation relations not matched to vertex!");
		}

		/*
		 * Process all usage relations
		 */
		Usage[] usages = doc.getUsedArray();
		for (Usage usage : usages) {
			// activity depends on the used entity
			String parentId = usage.getEntity().getRef().toString();
			String childId = usage.getActivity().getRef().toString();

			if (false == provGraph.addChild(parentId, childId))
				throw new Exception("usage relations not matched to vertex!");
		}

		/*
		 * Process all hasPart relations
		 */
		HasPart[] hasParts = doc.getHasPartArray();
		for (HasPart hasPart : hasParts) {
			// the whole depends on its part
			String parentId = hasPart.getPart().getRef().toString();
			String childId = hasPart.getWhole().getRef().toString();
			if (false == provGraph.addChild(parentId, childId))
				throw new Exception("hasPart relations not matched to vertex!");

			provGraph.getVertex(parentId).setNodeMark("part");
			provGraph.getVertex(childId).setNodeMark("whole");
		}

		/*
		 * Process all memberShip relations
		 */
		Membership[] memberShips = doc.getHadMemberArray();
		for (Membership memberShip : memberShips) {
			// the collection depends on its members
			String childId = memberShip.getCollection().getRef().toString();
			for (IDRef parentdRef : memberShip.getEntityArray()) {
				String parentId = parentdRef.getRef().toString();
				if (false == provGraph.addChild(parentId, childId))
					throw new Exception(
							"memberShip relations not matched to vertex!");

				provGraph.getVertex(parentId).setNodeMark("member");
				provGraph.getVertex(childId).setNodeMark("collection");
			}
		}

		/*
		 * Process all communication relations
		 */
		Communication[] communications = doc.getWasInformedByArray();
		for (Communication communication : communications) {
			// informed activite depends on its informant
			String parentId = communication.getInformant().getRef().toString();
			String childId = communication.getInformed().getRef().toString();

			if (false == provGraph.addChild(parentId, childId))
				throw new Exception(
						"communication relations not matched to vertex!");
		}

		/*
		 * the parent (part) inherit its child's (the whole's) dependency
		 */
		for (HasPart hasPart : hasParts) {
			// the whole depends on its part
			String parentId = hasPart.getPart().getRef().toString();
			String childId = hasPart.getWhole().getRef().toString();

			List<DependencyVertex> childDependents = provGraph.getVertex(
					childId).getParents();
			for (DependencyVertex childDependent : childDependents) {
				// avoid adding the part dependents
				if (childDependent.getNodeMark() == null
						|| false == childDependent.getNodeMark().equals("part")) {
					if (false == provGraph.addChild(childDependent.getId(),
							parentId))
						throw new Exception(
								"hasPart inheritance relations not matched to vertex!");
				}
			}
		}

		/*
		 * the parent (member) inherit its child's (the collection's) dependency
		 */
		for (Membership memberShip : memberShips) {
			// the collection depends on its members
			String childId = memberShip.getCollection().getRef().toString();
			List<DependencyVertex> collectionDependents = provGraph.getVertex(
					childId).getParents();

			for (IDRef parentdRef : memberShip.getEntityArray()) {
				String parentId = parentdRef.getRef().toString();

				for (DependencyVertex collectionDependent : collectionDependents) {
					// avoid adding the member dependents
					if (collectionDependent.getNodeMark() == null
							|| false == collectionDependent.getNodeMark()
									.equals("member")) {
						if (false == provGraph.addChild(
								collectionDependent.getId(), parentId))
							throw new Exception(
									"memberShip inheritance relations not matched to vertex!");
					}
				}
			}
		}

		/*
		 * the parent (step) inherit its child's (the plan's) dependency
		 */
		Map<String, Set<String>> planToSteps = new HashMap<String, Set<String>>();
		for (IsStepOfPlan isStepOfPlan : isStepOfPlans) {
			// plan depends on its step
			String parentId = isStepOfPlan.getStep().getRef().toString();
			String childId = isStepOfPlan.getPlan().getRef().toString();

			if (planToSteps.containsKey(childId)) {
				planToSteps.get(childId).add(parentId);
			} else {
				Set<String> stepSet = new HashSet<String>();
				stepSet.add(parentId);
				planToSteps.put(childId, stepSet);
			}
		}

		for (IsStepOfPlan isStepOfPlan : isStepOfPlans) {
			// plan depends on its step
			String parentId = isStepOfPlan.getStep().getRef().toString();
			String childId = isStepOfPlan.getPlan().getRef().toString();

			Set<String> parentStepSet = planToSteps.get(childId);
			List<DependencyVertex> childDependents = provGraph.getVertex(
					childId).getParents();
			for (DependencyVertex childDependent : childDependents) {
				// avoid adding the part dependents
				if (false == parentStepSet.contains(childDependent.getId())) {
					if (false == provGraph.addChild(childDependent.getId(),
							parentId))
						throw new Exception(
								"isStepOfPlan inheritance relations not matched to vertex!");
				}
			}
		}

		return provGraph;
	}
}

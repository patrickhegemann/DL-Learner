/**
 * Copyright (C) 2007 - 2016, Jens Lehmann
 *
 * This file is part of DL-Learner.
 *
 * DL-Learner is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * DL-Learner is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.dllearner.algorithms.qtl.operations.lgg;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.vocabulary.RDF;
import org.dllearner.algorithms.qtl.datastructures.impl.RDFResourceTree;
import org.dllearner.algorithms.qtl.operations.StoppableOperation;
import org.dllearner.algorithms.qtl.operations.TimeoutableOperation;
import org.dllearner.algorithms.qtl.util.filters.PredicateExistenceFilter;
import org.dllearner.algorithms.qtl.util.filters.PredicateExistenceFilterDBpedia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * An LGG generator that can be stopped and given a timeout.
 *
 * @author Lorenz Buehmann
 *
 */
public abstract class AbstractLGGGenerator implements LGGGenerator, StoppableOperation, TimeoutableOperation {
	
	protected Logger logger = LoggerFactory.getLogger(getClass());
	
	private Monitor mon = MonitorFactory.getTimeMonitor("lgg");
	
	protected int subCalls;
	
	private long timeoutMillis = -1;
	private long startTime;

	protected volatile boolean stop = false;

	private boolean complete = true;

	PredicateExistenceFilter filter;

	public void setFilter(PredicateExistenceFilter filter) {
		this.filter = filter;
	}

	private void reset() {
		stop = false;
		subCalls = 0;
	}

	/* (non-Javadoc)
	 * @see org.dllearner.algorithms.qtl.operations.lgg.LGGGenerator2#getLGG(org.dllearner.algorithms.qtl.datastructures.impl.RDFResourceTree, org.dllearner.algorithms.qtl.datastructures.impl.RDFResourceTree, boolean)
	 */
	@Override
	public RDFResourceTree getLGG(RDFResourceTree tree1, RDFResourceTree tree2, boolean learnFilters) {
		startTime = System.currentTimeMillis();

		reset();

		// apply some pre-processing
		tree1 = preProcess(tree1);
		tree2 = preProcess(tree2);
		
		// compute the LGG
		mon.start();
		RDFResourceTree lgg = computeLGG(tree1, tree2, learnFilters);
		mon.stop();

		// apply some post-processing
		lgg = postProcess(lgg);

		addNumbering(0, lgg);
		
		return lgg;
	}

	protected RDFResourceTree computeLGG(RDFResourceTree tree1, RDFResourceTree tree2, boolean learnFilters){
		subCalls++;

		// 1. compare the root nodes
		// a) if both root nodes have same URI or literal value, just return one of the two trees as LGG
		if((tree1.isResourceNode() || tree1.isLiteralValueNode()) && tree1.getData().equals(tree2.getData())){
			logger.trace("Early termination. Tree 1 {}  and tree 2 {} describe the same resource.", tree1, tree2);
			return new RDFResourceTree(tree1);
		}

		// b) handle literal nodes
		if(tree1.isLiteralNode() && tree2.isLiteralNode()){
			return processLiteralNodes(tree1, tree2);
		}

		// c) handle class nodes
		if(tree1.isClassNode()) {
			return processClassNodes(tree1, tree2);
		}

		// d) else create new empty tree
		RDFResourceTree lgg = new RDFResourceTree();

		// 2. compare the edges
		// we only have to compare edges which are
		// a) contained in both trees
		// b) related via subsumption, i.e. p1 ⊑ p2

		// get edges of tree 2 connected via subsumption
		Set<Triple<Node, Node, Node>> relatedEdges = getRelatedEdges(tree1, tree2);
		for (Triple<Node, Node, Node> entry : relatedEdges){
			if(stop || isTimeout()) {
				complete = false;
				break;
			}

			Node edge1 = entry.getLeft();
			Node edge2 = entry.getMiddle();
			Node lcs = entry.getRight();

			Set<RDFResourceTree> addedChildren = new HashSet<>();

//			System.out.println(lcs + ": " + tree1.getChildren(edge1).size() * tree2.getChildren(edge2).size());
 			System.out.println(lcs + ":::\n" + tree1.getChildren(edge1) + "\n" + tree2.getChildren(edge2));

			// loop over children of first tree
			for(RDFResourceTree child1 : tree1.getChildren(edge1)){System.out.println("c1:" + child1);
				if(stop || isTimeout()) {
					complete = false;
					break;
				}
				// loop over children of second tree
				for(RDFResourceTree child2 : tree2.getChildren(edge2)){System.out.println("c2:" + child2);
					if(stop || isTimeout()) {
						complete = false;
						break;
					}

					RDFResourceTree lggChild = computeLGG(child1, child2, learnFilters);
					lgg.addChild(lggChild, lcs);
					System.out.println("c_lgg:" + lggChild);

					// check if there was already a more specific child computed before
					// and if so don't add the current one
					boolean add = true;
					for(Iterator<RDFResourceTree> it = addedChildren.iterator(); it.hasNext() && !(stop || isTimeout());){
						RDFResourceTree addedChild = it.next();

						if(isSubTreeOf(addedChild, lggChild)){
//								logger.trace("Skipped adding: Previously added child {} is subsumed by {}.",
//										addedChild.getStringRepresentation(),
//										lggChild.getStringRepresentation());
							System.out.println("Skipped adding: Previously added child " + addedChild +  " is more specific than " + lggChild.getStringRepresentation());
							lgg.removeChild(lggChild, lcs);
							add = false;
							break;
						} else if(isSubTreeOf(lggChild, addedChild)){
//								logger.trace("Removing child node: {} is subsumed by previously added child {}.",
//										lggChild.getStringRepresentation(),
//										addedChild.getStringRepresentation());
							System.out.println("Removing child node: " + lggChild +  " is subsumed by previously added child " + addedChild.getStringRepresentation());
							lgg.removeChild(addedChild, lgg.getEdgeToChild(addedChild));
							it.remove();
						}
					}
					if(add){
						lgg.addChild(lggChild, lcs);
						addedChildren.add(lggChild);
//							logger.trace("Adding child {}", lggChild.getStringRepresentation());
					}
				}
			}
		}

		return lgg;
	}

	protected RDFResourceTree processClassNodes(RDFResourceTree tree1, RDFResourceTree tree2) {
		RDFResourceTree lgg = new RDFResourceTree();

		Set<Triple<Node, Node, Node>> relatedEdges = getRelatedEdges(tree1, tree2);
		for (Triple<Node, Node, Node> entry : relatedEdges) {
			if (stop || isTimeout()) {
				complete = false;
				break;
			}
			Node edge1 = entry.getLeft();
			Node edge2 = entry.getMiddle();
			Node lcs = entry.getRight();

			Set<RDFResourceTree> addedChildren = new HashSet<>();

			// loop over children of first tree
			for(RDFResourceTree child1 : tree1.getChildren(edge1)){//System.out.println("c1:" + child1);
				if(stop || isTimeout()) {
					complete = false;
					break;
				}
				// loop over children of second tree
				for(RDFResourceTree child2 : tree2.getChildren(edge2)){//System.out.println("c2:" + child2);
					if(stop || isTimeout()) {
						complete = false;
						break;
					}

					RDFResourceTree lggChild = computeLGG(child1, child2, false);

					// check if there was already a more specific child computed before
					// and if so don't add the current one
					boolean add = true;
					for(Iterator<RDFResourceTree> it = addedChildren.iterator(); it.hasNext();){
						RDFResourceTree addedChild = it.next();

						if(isSubTreeOf(addedChild, lggChild)){
//								logger.trace("Skipped adding: Previously added child {} is subsumed by {}.",
//										addedChild.getStringRepresentation(),
//										lggChild.getStringRepresentation());
							add = false;
							break;
						} else if(isSubTreeOf(lggChild, addedChild)){
//								logger.trace("Removing child node: {} is subsumed by previously added child {}.",
//										lggChild.getStringRepresentation(),
//										addedChild.getStringRepresentation());
							lgg.removeChild(addedChild, lgg.getEdgeToChild(addedChild));
							it.remove();
						}
					}
					if(add){
						lgg.addChild(lggChild, lcs);
						addedChildren.add(lggChild);
//							logger.trace("Adding child {}", lggChild.getStringRepresentation());
					}
				}
			}
		}
		return lgg;
	}

	protected RDFResourceTree processLiteralNodes(RDFResourceTree tree1, RDFResourceTree tree2) {
		RDFDatatype d1 = tree1.getData().getLiteralDatatype();
		RDFDatatype d2 = tree2.getData().getLiteralDatatype();

		if(d1 != null && d1.equals(d2)){
			return new RDFResourceTree(d1);
			// TODO collect literal values
		}
		return RDFResourceTree.newVarNode();//LiteralNode();
	}

	@Override
	public void setTimeout(long timeout, TimeUnit timeoutUnits) {
		this.timeoutMillis = timeoutUnits.toMillis(timeout);
	}

	@Override
	public void stop() {
		stop = true;
	}

	protected boolean isTimeout() {
		return timeoutMillis > 0 && System.currentTimeMillis() - startTime >= timeoutMillis;
	}

	public boolean isComplete() {
		return complete;
	}

	private void addNumbering(int nodeId, RDFResourceTree tree){
//		tree.setId(nodeId);
		for(RDFResourceTree child : tree.getChildren()){
			addNumbering(nodeId++, child);
		}
	}

	protected RDFResourceTree postProcess(RDFResourceTree tree) {
		return tree;
	}

	protected RDFResourceTree preProcess(RDFResourceTree tree) {
		return tree;
	}

	protected abstract boolean isSubTreeOf(RDFResourceTree tree1, RDFResourceTree tree2);
	
	protected abstract Set<Triple<Node, Node, Node>> getRelatedEdges(RDFResourceTree tree1, RDFResourceTree tree2);

}

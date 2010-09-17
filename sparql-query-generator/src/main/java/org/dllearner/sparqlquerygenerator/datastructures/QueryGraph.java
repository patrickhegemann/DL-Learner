/**
 * Copyright (C) 2007-2010, Jens Lehmann
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
 *
 */
package org.dllearner.sparqlquerygenerator.datastructures;

import java.util.Set;

/**
 * 
 * @author Lorenz Bühmann
 *
 */
public interface QueryGraph {
	
	Set<Node> getNodes();
	
	Set<Edge> getEdges();
	
	Node getRootNode();
	
	Node createNode(String id);
	
	Edge createEdge(Node sourceNode, Node targetNode, String label);
	
	boolean addNode(Node node);
	
	boolean addEdge(Edge edge);
	
	void setRootNode(Node rootNode);

}

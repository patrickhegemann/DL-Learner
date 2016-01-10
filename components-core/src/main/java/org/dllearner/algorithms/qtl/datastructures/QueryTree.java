package org.dllearner.algorithms.qtl.datastructures;

import java.io.PrintWriter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dllearner.algorithms.qtl.datastructures.impl.QueryTreeImpl;
import org.dllearner.algorithms.qtl.datastructures.impl.QueryTreeImpl.LiteralNodeConversionStrategy;
import org.dllearner.algorithms.qtl.datastructures.impl.QueryTreeImpl.LiteralNodeSubsumptionStrategy;
import org.dllearner.algorithms.qtl.datastructures.impl.QueryTreeImpl.NodeType;
import org.semanticweb.owlapi.model.OWLClassExpression;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.rdf.model.Literal;

/**
 * 
 * @author Lorenz Bühmann
 *
 */
public interface QueryTree<N> {
	
	 /**
     * Gets the "content" of this tree node.
     * @return The user content of this node.
     */
    N getUserObject();
    
    void setUserObject(N userObject);
    
    /**
     * Set the ID of the current node
     * @param id the ID
     */
    void setId(int id);
    
    /**
     * @return the ID of the tree node in the whole tree
     */
    int getId();
    
    boolean isEmpty();
    
    QueryTree<N> getNodeById(int nodeId);
    
    boolean sameType(QueryTree<N> tree);
    	
    boolean isLiteralNode();
    
    void setIsLiteralNode(boolean isLiteralNode);
    
    boolean isResourceNode();
    
    void setIsResourceNode(boolean isResourceNode);
    
    boolean isVarNode();
    
    void setVarNode(boolean isVarNode);

    QueryTree<N> getParent();
    
    List<QueryTree<N>> getChildren();
    
    List<QueryTree<N>> getChildren(Object edge);
    
    List<QueryTree<N>> getChildrenClosure();

    Object getEdge(QueryTree<N> child);
    
    void addChild(QueryTreeImpl<N> child);
    
    void addChild(QueryTreeImpl<N> child, int position);
    
    void addChild(QueryTreeImpl<N> child, Object edge);
    
    void addChild(QueryTree<N> child, Object edge);
    
    void addChild(QueryTreeImpl<N> child, Object edge, int position);
    
    int removeChild(QueryTreeImpl<N> child);
    
    Set<Object> getEdges();
    
    void sortChildren(Comparator<QueryTree<N>> comparator);

    int getChildCount();
    
    int getMaxDepth();

    boolean isRoot();

    boolean isLeaf();
    
    boolean isSubsumedBy(QueryTree<N> tree);
    
    boolean isSubsumedBy(QueryTree<N> tree, boolean stopAfterError);
    
    boolean isSameTreeAs(QueryTree<N> tree);
    
    void tag();
    
    boolean isTagged();

    QueryTree<N> getRoot();
    
    List<QueryTree<N>> getLeafs();

    List<QueryTree<N>> getPathToRoot();

    List<N> getUserObjectPathToRoot();
    
    void dump();
    
    String getStringRepresentation();

    void dump(PrintWriter writer);

    void dump(PrintWriter writer, int indent);

    Set<N> getUserObjectClosure();

    List<N> fillDepthFirst();
    
    String toSPARQLQueryString();
    
    String toSPARQLQueryString(boolean filterMeaninglessProperties, boolean useNumericalFilters);
    
    String toSPARQLQueryString(boolean filterMeaninglessProperties, boolean useNumericalFilters, Map<String, String> prefixMap);
    
    Query toSPARQLQuery();
    
    OWLClassExpression asOWLClassExpression();
    
    int getTriplePatternCount();
    
    Query toQuery();
    
    RDFDatatype getDatatype();
    
    Set<Literal> getLiterals();
    
    void setParent(QueryTree<N> parent);

	/**
	 * @param edge
	 */
	void removeChildren(Object edge);

	/**
	 * @param stopIfChildIsResourceNode
	 * @return
	 */
	String getStringRepresentation(boolean stopIfChildIsResourceNode);

	/**
	 * @param literalNodeConversionStrategy
	 * @return
	 */
	OWLClassExpression asOWLClassExpression(LiteralNodeConversionStrategy literalNodeConversionStrategy);

	/**
	 * @param tree
	 * @param s
	 * @return
	 */
	boolean isSubsumedBy(QueryTree<N> tree, LiteralNodeSubsumptionStrategy s);

	/**
	 * @return
	 */
	NodeType getNodeType();
    
}

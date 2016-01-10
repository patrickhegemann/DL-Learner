package org.dllearner.kb.extraction;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.dllearner.kb.aquisitors.TupleAquisitor;
import org.dllearner.kb.manipulator.Manipulator;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;

/**
 * A node in the graph that is a Literal.
 * 
 * @author Sebastian Hellmann
 * 
 */
public class LiteralNode extends Node {
	
	private Literal l;
	
	@SuppressWarnings("unused")
	private static Logger logger = Logger
		.getLogger(LiteralNode.class);


	public LiteralNode(String uri) {
		super(uri);
		
	}
	
	public LiteralNode(RDFNode node) {
		super(node.toString());
		l = (Literal) node;
	}
	
	public Literal getLiteral(){
		return l;
	}

	// expands all directly connected nodes
	@Override
	public List<Node> expand(TupleAquisitor tupelAquisitor, Manipulator manipulator) {
		return new ArrayList<>();
	}
	
	

	// gets the types for properties recursively
	@Override
	public List<BlankNode>  expandProperties(TupleAquisitor tupelAquisitor, Manipulator manipulator, boolean dissolveBlankNodes) {
		return new ArrayList<>();
	}

	@Override
	public SortedSet<String> toNTriple() {
		return new TreeSet<>();
	}

	
	
	@Override
	public String getNTripleForm() {
		String quote = "\\\"";
		quote = "&quot;";
		String retVal = l.getLexicalForm();
		retVal = retVal.replaceAll("\n", "\\n");
		retVal = retVal.replaceAll("\"", quote);
		retVal = "\""+retVal+"\"";
		if(l.getDatatypeURI()!=null) {
			return retVal +"^^<"+l.getDatatypeURI()+">";
		}else {
			return retVal+((l.getLanguage().length()==0)?"":"@"+l.getLanguage());
		}
		
	}
	
	@Override
	public void toOWLOntology( OWLAPIOntologyCollector owlAPIOntologyCollector){
		
	}
	
	public boolean isDouble(){
		try{
			return l.getDatatypeURI() != null && l.getDatatypeURI().contains("double");
		}catch (Exception e) {
			return false;
		}
	}
	
	public boolean isFloat(){
		try{
			return l.getDatatypeURI() != null && l.getDatatypeURI().contains("float");
		}catch (Exception e) {
			return false;
		}
	}
	
	public boolean isInt(){
		try{
			return l.getDatatypeURI() != null && l.getDatatypeURI().contains("int");
		}catch (Exception e) {
			return false;
		}
	}
	
	public boolean isBoolean(){
		try{
			return l.getDatatypeURI() != null && l.getDatatypeURI().contains("boolean");
		}catch (Exception e) {
			return false;
		}
	}
	
	public boolean isString(){
		try{
			l.getString();
			return true;
		}catch (Exception e) {
			return false;
		}
	}
	public boolean hasLanguageTag(){
		return (!(l.getLanguage().length()==0));
	}
	
	
	
	

}

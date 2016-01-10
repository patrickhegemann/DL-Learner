package org.dllearner.algorithms.properties;

import java.util.SortedSet;

import org.dllearner.core.ComponentAnn;
import org.dllearner.kb.SparqlEndpointKS;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import com.hp.hpl.jena.query.ParameterizedSparqlString;

@ComponentAnn(name = "disjoint object properties axiom learner", shortName = "opldisjoint", version = 0.1, description="A learning algorithm for disjoint object properties axioms.")
public class DisjointObjectPropertyAxiomLearner extends ObjectPropertyHierarchyAxiomLearner<OWLDisjointObjectPropertiesAxiom> {

	public DisjointObjectPropertyAxiomLearner(SparqlEndpointKS ks) {
		super(ks);

		super.posExamplesQueryTemplate = new ParameterizedSparqlString(
				"SELECT DISTINCT ?s ?o WHERE {?s ?p ?o. FILTER NOT EXISTS{?s ?p_other ?o}}");
		super.negExamplesQueryTemplate = new ParameterizedSparqlString(
				"SELECT DISTINCT ?s ?o WHERE {?s ?p ?o; ?p_other ?o.}");
		
		axiomType = AxiomType.DISJOINT_OBJECT_PROPERTIES;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.dllearner.core.AbstractAxiomLearningAlgorithm#getExistingAxioms()
	 */
	@Override
	protected void getExistingAxioms() {
		SortedSet<OWLObjectProperty> existingDisjointProperties = reasoner.getDisjointProperties(entityToDescribe);
		if (existingDisjointProperties != null && !existingDisjointProperties.isEmpty()) {
			for (OWLObjectProperty disProp : existingDisjointProperties) {
				existingAxioms.add(df.getOWLDisjointObjectPropertiesAxiom(entityToDescribe, disProp));
			}
			logger.info("Existing axioms:" + existingAxioms);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.dllearner.algorithms.properties.ObjectPropertyHierarchyAxiomLearner#getAxiom(org.semanticweb.owlapi.model.OWLObjectProperty, org.semanticweb.owlapi.model.OWLObjectProperty)
	 */
	@Override
	public OWLDisjointObjectPropertiesAxiom getAxiom(OWLObjectProperty property, OWLObjectProperty otherProperty) {
		return df.getOWLDisjointObjectPropertiesAxiom(property, otherProperty);
	}
	
	public double computeScore(int candidatePopularity, int popularity, int overlap) {
		return 1 - super.computeScore(candidatePopularity, popularity, overlap);
	}
}

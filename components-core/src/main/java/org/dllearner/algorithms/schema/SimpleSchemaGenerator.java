package org.dllearner.algorithms.schema;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.model.QueryExecutionFactoryModel;
import org.dllearner.algorithms.properties.AxiomAlgorithms;
import org.dllearner.utilities.OwlApiJenaUtils;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * {@inheritDoc}
 * <p>
 * This is a very simple implementation of a schema generator which
 * iterates over all entities and all axiom types in a specific order
 * and adds axioms as long as the knowledge base remains consistent
 * and coherent.
 * 
 * @author Lorenz Buehmann
 *
 */
public class SimpleSchemaGenerator extends AbstractSchemaGenerator{
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleSchemaGenerator.class);
	
	// how often the enrichment process is executed
	private int nrOfIterations = 1;

	public SimpleSchemaGenerator(QueryExecutionFactory qef) {
		super(qef);
	}
	
	public SimpleSchemaGenerator(OWLOntology ontology) {
		super(OwlApiJenaUtils.getModel(ontology));
	}
	
	public SimpleSchemaGenerator(Model model) {
		super(model);
	}

	/* (non-Javadoc)
	 * @see org.dllearner.algorithms.schema.SchemaGenerator#generateSchema()
	 */
	@Override
	public Set<OWLAxiom> generateSchema() {
		Set<OWLAxiom> generatedAxiomsTotal = new HashSet<>();
		
		// get the entities
		SortedSet<OWLEntity> entities = getEntities();
		
		// we repeat the whole process
		for (int i = 0; i < nrOfIterations; i++) {
			LOGGER.trace("Iteration " + (i+1) + " ...");
			Set<OWLAxiom> generatedAxioms = new HashSet<>();
			
			// iterate over the entities
			for (OWLEntity entity : entities) {
				// get the applicable axiom types
				SetView<AxiomType<? extends OWLAxiom>> applicableAxiomTypes = Sets.intersection(AxiomAlgorithms.getAxiomTypes(entity.getEntityType()), axiomTypes);
				
				// iterate over the axiom types
				for (AxiomType<? extends OWLAxiom> axiomType : applicableAxiomTypes) {
					// apply the appropriate learning algorithm
					try {
						List<OWLAxiom> axioms = applyLearningAlgorithm(entity, axiomType);
						generatedAxioms.addAll(axioms);
					} catch (Exception e) {
						LOGGER.error("Exception occured for axiom type "
								+ axiomType.getName() + " and entity " + entity + ".", e);
						//TODO handle exception despite logging
					}
				}
			}
			
			// add the generated axioms to the knowledge base
			addToKnowledgebase(generatedAxioms);
			
			// new axioms
			SetView<OWLAxiom> newAxioms = Sets.difference(generatedAxioms, generatedAxiomsTotal);
			
			LOGGER.trace(newAxioms.isEmpty() ? "Got no new axioms." : ("Got " + newAxioms.size()  + " new axioms:"));
			if(newAxioms.isEmpty()){ // terminate if iteration lead to no new axioms
				if((i+1) < nrOfIterations)
					LOGGER.trace("Early termination. Ignoring further iterations.");
				break;
			}
			LOGGER.trace(newAxioms.toString());
			
			// add to total set
			generatedAxiomsTotal.addAll(generatedAxioms);
		}
		return generatedAxiomsTotal;
	}
	
	/**
	 * @param nrOfIterations the nrOfIterations to set
	 */
	public void setNrOfIterations(int nrOfIterations) {
		this.nrOfIterations = nrOfIterations;
	}
}

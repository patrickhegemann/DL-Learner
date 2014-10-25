/**
 * 
 */
package org.dllearner.algorithms.schema;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.dllearner.algorithms.properties.AxiomAlgorithms;
import org.dllearner.core.AbstractAxiomLearningAlgorithm;
import org.dllearner.core.AxiomLearningProgressMonitor;
import org.dllearner.core.ComponentInitException;
import org.dllearner.core.ConsoleAxiomLearningProgressMonitor;
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.reasoning.SPARQLReasoner;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.EntityType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.profiles.OWL2DLProfile;
import org.semanticweb.owlapi.profiles.OWLProfile;

import com.google.common.collect.Sets;


/**
 * 
 * @author Lorenz Buehmann
 *
 */
public abstract class AbstractSchemaGenerator implements SchemaGenerator{
	
	protected QueryExecutionFactory qef;
	protected OWLProfile owlProfile = new OWL2DLProfile();
	protected SPARQLReasoner reasoner;
	
	protected AxiomLearningProgressMonitor progressMonitor = new ConsoleAxiomLearningProgressMonitor();
	
	// the types of axioms to be processed
	protected Set<AxiomType<? extends OWLAxiom>> axiomTypes = AxiomAlgorithms.TBoxAndRBoxAxiomTypes;
	
	// the types of entities to be processed
	protected Set<EntityType<? extends OWLEntity>> entityTypes = Sets.<EntityType<? extends OWLEntity>>newHashSet(
			EntityType.CLASS, EntityType.OBJECT_PROPERTY, EntityType.DATA_PROPERTY);
	
	// the minimum accuracy threshold for generated axioms to be accepted
	private int accuracyThreshold;
	
	// the entities which are processed
	protected SortedSet<OWLEntity> entities;
	
	public AbstractSchemaGenerator(QueryExecutionFactory qef) {
		this.qef = qef;
		this.reasoner = new SPARQLReasoner(qef);
	}
	
	/**
	 * Set the types of axioms that are generated.
	 * @param axiomTypes the axiom types to set
	 */
	public void setAxiomTypes(Set<AxiomType<? extends OWLAxiom>> axiomTypes) {
		this.axiomTypes = axiomTypes;
	}
	
	/**
	 * Set the types of axioms that are generated by giving an OWL profile.
	 * @param axiomTypes the axiom types to set
	 */
	public void setAxiomTypes(OWLProfile owlProfile) {
		if(owlProfile == OWLProfile.OWL2_EL){
			
		} else if(owlProfile == OWLProfile.OWL2_RL){
			
		} else if(owlProfile == OWLProfile.OWL2_QL){
			
		} else if(owlProfile == OWLProfile.OWL2_DL){
			
		} else {
			throw new IllegalArgumentException("OWL profile " + owlProfile.getName() + " not supported.");
		}
	}
	
	public void setEntityTypes(Set<EntityType<? extends OWLEntity>> entityTypes) {
		this.entityTypes = entityTypes;
	}
	
	/**
	 * @param entities the entities to set
	 */
	public void setEntities(SortedSet<OWLEntity> entities) {
		this.entities = entities;
	}
	
	/**
	 * Return the entities contained in the current knowledge base.
	 */
	protected SortedSet<OWLEntity> getEntities(){
		if(entities == null){
			entities = new TreeSet<OWLEntity>();
			for (EntityType<? extends OWLEntity> entityType : entityTypes) {
				if(entityType == EntityType.CLASS){
					entities.addAll(reasoner.getOWLClasses());
				} else if(entityType == EntityType.OBJECT_PROPERTY){
					entities.addAll(reasoner.getOWLObjectProperties());
				} else if(entityType == EntityType.DATA_PROPERTY){
					entities.addAll(reasoner.getOWLDataProperties());
				} else {
					throw new IllegalArgumentException("Entity type " + entityType.getName() + " not supported.");
				}
			}
		}
		return entities;
	}
	
	protected List<OWLAxiom> applyLearningAlgorithm(OWLEntity entity, AxiomType<? extends OWLAxiom> axiomType) throws Exception{
		// get the algorithm class
		Class<? extends AbstractAxiomLearningAlgorithm<? extends OWLAxiom, ? extends OWLObject, ? extends OWLEntity>> algorithmClass = AxiomAlgorithms.getAlgorithmClass(axiomType);
		
		// create learning algorithm object
		AbstractAxiomLearningAlgorithm learner = null;
		try {
			learner = (AbstractAxiomLearningAlgorithm)algorithmClass.getConstructor(
					SparqlEndpointKS.class).newInstance(new SparqlEndpointKS(qef));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		learner.setEntityToDescribe(entity);
		learner.setUseSampling(false);
		learner.setProgressMonitor(progressMonitor);
		
		
		try {
			// initialize the learning algorithm
			learner.init();
			
			// run the learning algorithm
			learner.start();
			
			// return the result
			return learner.getCurrentlyBestAxioms(accuracyThreshold);
		} catch (Exception e) {
			throw new Exception("Generation of " + axiomType.getName() + " axioms failed.", e);
		}
	}

}
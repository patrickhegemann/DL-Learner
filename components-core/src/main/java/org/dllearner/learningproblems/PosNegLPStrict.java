package org.dllearner.learningproblems;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.NotImplementedException;
import org.dllearner.core.AbstractReasonerComponent;
import org.dllearner.core.ComponentInitException;
import org.dllearner.core.EvaluatedDescription;
import org.dllearner.core.config.ConfigOption;
import org.dllearner.reasoning.ReasonerType;
import org.dllearner.utilities.Helper;
import org.dllearner.utilities.datastructures.SortedSetTuple;
import org.dllearner.utilities.owl.OWLClassExpressionUtils;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author Jens Lehmann
 *
 */
public class PosNegLPStrict extends PosNegLP {

	private Set<OWLIndividual> neutralExamples;

	@ConfigOption(name="penaliseNeutralExamples", description = "if set to true neutral examples are penalised")
	private boolean penaliseNeutralExamples = false;
	
	private static final double defaultAccuracyPenalty = 1;
	@ConfigOption(name="accuracyPenalty", description = "penalty for pos/neg examples which are classified as neutral", defaultValue = "1.0")
	private double accuracyPenalty = defaultAccuracyPenalty;

	private static final double defaultErrorPenalty = 3;
	@ConfigOption(name="errorPenalty", description = "penalty for pos. examples classified as negative or vice versa", defaultValue = "3.0")
	private double errorPenalty = defaultErrorPenalty;
	
	
	public PosNegLPStrict(AbstractReasonerComponent reasoningService) {
		super(reasoningService);
	}
	
	/* (non-Javadoc)
	 * @see org.dllearner.core.Component#getName()
	 */
	public static String getName() {
		return "three valued definition learning problem";
	}


	/* (non-Javadoc)
	 * @see org.dllearner.core.Component#init()
	 */
	@Override
	public void init() throws ComponentInitException {
		super.init();
		// compute neutral examples, i.e. those which are neither positive
		// nor negative (we have to take care to copy sets instead of 
		// modifying them)
		neutralExamples = Sets.intersection(getReasoner().getIndividuals(), positiveExamples);
		neutralExamples.retainAll(negativeExamples);
	}

	/* (non-Javadoc)
	 * @see org.dllearner.learningproblems.DefinitionLP#computeScore(org.dllearner.core.dl.Concept)
	 */
	@Override
	public ScorePosNeg computeScore(OWLClassExpression concept) {
	   	if(isUseRetrievalForClassification()) {
			SortedSet<OWLIndividual> posClassified = getReasoner().getIndividuals(concept);
			SortedSet<OWLIndividual> negClassified = getReasoner().getIndividuals(dataFactory.getOWLObjectComplementOf(concept));
			Set<OWLIndividual> neutClassified = new TreeSet<>(Sets.intersection(getReasoner().getIndividuals(),posClassified));
			neutClassified.retainAll(negClassified);
			return new ScoreThreeValued(
					OWLClassExpressionUtils.getLength(concept), accuracyPenalty, errorPenalty,
					penaliseNeutralExamples, getPercentPerLengthUnit(), posClassified, neutClassified,
					negClassified, positiveExamples, neutralExamples, negativeExamples);
    	} else {
    		if(getReasoner().getReasonerType().isOWLAPIReasoner()) {
    			if(penaliseNeutralExamples)
    				throw new Error("It does not make sense to use single instance checks when" +
    						"neutral examples are penalized. Use Retrievals instead.");
    				
    			// TODO: umschreiben in instance checks
    			SortedSet<OWLIndividual> posClassified = new TreeSet<OWLIndividual>();
    			SortedSet<OWLIndividual> negClassified = new TreeSet<OWLIndividual>();
    			// Beispiele durchgehen
    			// TODO: Implementierung ist ineffizient, da man hier schon in Klassen wie
    			// posAsNeut, posAsNeg etc. einteilen koennte; so wird das extra in der Score-Klasse
    			// gemacht; bei wichtigen Benchmarks des 3-wertigen Lernproblems muesste man das
    			// umstellen
    			// pos => pos
    			for(OWLIndividual example : positiveExamples) {
    				if(getReasoner().hasType(concept, example))
    					posClassified.add(example);
    			}
    			// neg => pos
    			for(OWLIndividual example: negativeExamples) {
    				if(getReasoner().hasType(concept, example))
    					posClassified.add(example);
    			}

				OWLClassExpression negatedConcept = dataFactory.getOWLObjectComplementOf(concept);

    			// pos => neg
    			for(OWLIndividual example : positiveExamples) {
    				if(getReasoner().hasType(negatedConcept, example))
    					negClassified.add(example);
    			}
    			// neg => neg
    			for(OWLIndividual example : negativeExamples) {
    				if(getReasoner().hasType(negatedConcept, example))
    					negClassified.add(example);
    			}    			
    			
    			Set<OWLIndividual> neutClassified = new TreeSet<>(Sets.intersection(getReasoner().getIndividuals(),posClassified));
    			neutClassified.retainAll(negClassified);
				return new ScoreThreeValued(OWLClassExpressionUtils.getLength(concept), accuracyPenalty, errorPenalty,
											penaliseNeutralExamples, getPercentPerLengthUnit(), posClassified,
											neutClassified, negClassified, positiveExamples, neutralExamples,
											negativeExamples);
			} else
    			throw new Error("score cannot be computed in this configuration");
    	}
	}

	@Override
	public ScorePosNeg<OWLNamedIndividual> computeScore(OWLClassExpression ce, double noise) {
		return null;
	}

	public Set<OWLIndividual> getNeutralExamples() {
		return neutralExamples;
	}

	/**
	 * @return the accuracyPenalty
	 */
	public double getAccuracyPenalty() {
		return accuracyPenalty;
	}

	/**
	 * @return the errorPenalty
	 */
	public double getErrorPenalty() {
		return errorPenalty;
	}

	/**
	 * @return the penaliseNeutralExamples
	 */
	public boolean isPenaliseNeutralExamples() {
		return penaliseNeutralExamples;
	}

	/* (non-Javadoc)
	 * @see org.dllearner.core.LearningProblem#getAccuracyOrTooWeak(org.dllearner.core.owl.Description, double)
	 */
	@Override
	public double getAccuracyOrTooWeak(OWLClassExpression description, double minAccuracy) {
		throw new NotImplementedException("Not implemented yet!");
	}

	/* (non-Javadoc)
	 * @see org.dllearner.core.LearningProblem#evaluate(org.dllearner.core.owl.Description)
	 */
	@Override
	public EvaluatedDescription evaluate(OWLClassExpression description) {
		throw new NotImplementedException("Not implemented yet!");
	}
}
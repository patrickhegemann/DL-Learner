package org.dllearner.refinementoperators.spatial;

import com.google.common.collect.Sets;
import org.dllearner.core.AbstractReasonerComponent;
import org.dllearner.core.ComponentInitException;
import org.dllearner.core.KnowledgeSource;
import org.dllearner.core.owl.OWLObjectUnionOfImplExt;
import org.dllearner.kb.OWLFile;
import org.dllearner.reasoning.ClosedWorldReasoner;
import org.dllearner.reasoning.spatial.DBConnectionSetting;
import org.dllearner.reasoning.spatial.SpatialReasoner;
import org.dllearner.reasoning.spatial.SpatialReasonerPostGIS;
import org.dllearner.refinementoperators.RhoDRDown;
import org.dllearner.utilities.owl.OWLClassExpressionLengthMetric;
import org.dllearner.utilities.owl.OWLClassExpressionUtils;
import org.dllearner.vocabulary.spatial.SpatialVocabulary;
import org.semanticweb.owlapi.model.*;
import uk.ac.manchester.cs.owl.owlapi.*;

import java.util.*;

/**
 * TODO: Check handling of max expression length
 */
public class SpatialRhoDRDown extends RhoDRDown {
    private SpatialReasoner reasoner;
    private OWLClassExpressionLengthMetric lengthMetric =
            OWLClassExpressionLengthMetric.getDefaultMetric();

    // <getter/setter>
    public void setReasoner(SpatialReasoner reasoner) {
        // The spatial reasoner needs to be set here, since RhoDRDown won't be
        // able to get the domains/ranges of the virtual spatial properties
        // otherwise.
        super.setReasoner((AbstractReasonerComponent) reasoner);

        this.reasoner = reasoner;
    }
    // </getter/setter>

    // <interface methods>
    @Override
    public Set<OWLClassExpression> refine(OWLClassExpression description, int maxLength) {
        Set<OWLClassExpression> refinements = super.refine(description, maxLength);
        refinements.addAll(spatiallyRefine(description, maxLength));

        return refinements;
    }

    @Override
    public void init() throws ComponentInitException {
        super.init();
    }
    // </interface methods>

    private Set<OWLClassExpression> spatiallyRefine(OWLClassExpression ce, int maxLength) {
        Set<OWLClassExpression> refinements = new HashSet<>();
        if (ce instanceof OWLClass)
            refinements.addAll(spatiallyRefineOWLClass((OWLClass) ce));

        else if (ce instanceof OWLObjectIntersectionOf)
            refinements.addAll(
                    spatiallyRefineOWLObjectIntersectionOf((OWLObjectIntersectionOf) ce, maxLength));

        else if (ce instanceof OWLObjectSomeValuesFrom)
            refinements.addAll(
                    spatiallyRefineOWLObjectSomeValuesFrom((OWLObjectSomeValuesFrom) ce, maxLength));

        else if (ce instanceof OWLObjectMinCardinality)
            refinements.addAll(
                    spatiallyRefineOWLObjectMinCardinality((OWLObjectMinCardinality) ce, maxLength));

        else if (ce instanceof OWLObjectUnionOfImplExt)
            refinements.addAll(
                    spatiallyRefineOWLObjectUnionOf(
                            ((OWLObjectUnionOfImplExt) ce).getOperands(), maxLength));
        else if (ce instanceof OWLObjectAllValuesFrom)
            refinements.addAll(
                    spatiallyRefineOWLObjectAllValuesFrom((OWLObjectAllValuesFrom) ce, maxLength));
        else
            throw new RuntimeException(
                    "Class expression type " + ce.getClass() + " not covered");

        return refinements;
    }

    private Set<OWLClassExpression> spatiallyRefineOWLObjectAllValuesFrom(OWLObjectAllValuesFrom ce, int maxLength) {
        OWLObjectPropertyExpression property = ce.getProperty();
        // FIXME: extend this to also support general object property expressions
        assert !property.isAnonymous();

        Set<OWLObjectProperty> properties =
                // FIXME: Should be part of the spatial reasoner interface
                reasoner.getBaseReasoner().getSubProperties(property.asOWLObjectProperty());
        properties.add(property.asOWLObjectProperty());

        OWLClassExpression filler = ce.getFiller();

        Set<OWLClassExpression> refinements = new HashSet<>();

        // FIXME: replace lengthMetric.objectProperyLength with actual prop length
        Set<OWLClassExpression> fillerRefinements = refine(
                filler,
                maxLength-lengthMetric.objectProperyLength);

        for (OWLObjectProperty p : properties) {
            for (OWLClassExpression fillerRefinement : fillerRefinements) {
                refinements.add(new OWLObjectAllValuesFromImpl(p, fillerRefinement));
            }
        }

        return refinements;
    }

    /**
     * I'm using the operands set instead of the union class expression object
     * here since we have two different union types: {@link OWLObjectUnionOfImplExt}
     * and {@link OWLObjectUnionOf}.
     */
    private Set<OWLClassExpression> spatiallyRefineOWLObjectUnionOf(
            Set<OWLClassExpression> unionOperands, int maxLength) {

        Set<OWLClassExpression> refinements = new HashSet<>();

        if (reasoner.isSuperClassOf(
                SpatialVocabulary.SpatialFeature, new OWLObjectUnionOfImpl(unionOperands))) {

            // isInside
            refinements.add(new OWLObjectIntersectionOfImpl(Sets.newHashSet(
                    new OWLObjectUnionOfImplExt(unionOperands),
                    new OWLObjectSomeValuesFromImpl(
                            SpatialVocabulary.isInside, SpatialVocabulary.SpatialFeature))));

            // isNear
            refinements.add(new OWLObjectIntersectionOfImpl(Sets.newHashSet(
                    new OWLObjectUnionOfImplExt(unionOperands),
                    new OWLObjectSomeValuesFromImpl(
                            SpatialVocabulary.isNear, SpatialVocabulary.SpatialFeature))));
        }

        List<OWLClassExpression> unionOperandsList = new ArrayList<>(unionOperands);
        assert unionOperandsList.size() == 2;

        OWLClassExpression firstOperand = unionOperandsList.get(0);
        OWLClassExpression secondOperand = unionOperandsList.get(1);

        int tmpMaxLength = maxLength - OWLClassExpressionUtils.getLength(secondOperand);
        for (OWLClassExpression refinement1 : refine(firstOperand, tmpMaxLength)) {
            refinements.add(new OWLObjectIntersectionOfImpl(Sets.newHashSet(
                    refinement1, secondOperand)));
        }

        tmpMaxLength = maxLength - OWLClassExpressionUtils.getLength(firstOperand);
        for (OWLClassExpression refinement2 : refine(secondOperand, tmpMaxLength)) {
            refinements.add(new OWLObjectIntersectionOfImpl(Sets.newHashSet(
                    firstOperand, refinement2)));
        }

        return refinements;
    }

    private Set<OWLClassExpression> spatiallyRefineOWLObjectMinCardinality(OWLObjectMinCardinality ce, int maxLength) {
        OWLObjectPropertyExpression property = ce.getProperty();

        // FIXME: extend this to also support general object property expressions
        assert !property.isAnonymous();

        SortedSet<OWLObjectProperty> properties =
                // FIXME: Should be part of the spatial reasoner interface
                reasoner.getBaseReasoner().getSubProperties(property.asOWLObjectProperty());
        properties.add(property.asOWLObjectProperty());

        OWLClassExpression filler = ce.getFiller();
        int minCardinality = ce.getCardinality();

        // FIXME: replace lengthMetric.objectProperyLength with actual prop length
        Set<OWLClassExpression> fillerRefinements =
                refine(filler,
                        maxLength-lengthMetric.objectCardinalityLength-lengthMetric.objectProperyLength);
        int refinedCardinality = minCardinality + 1;

        Set<OWLClassExpression> refinements = new HashSet<>();
        refinements.add(new OWLObjectMinCardinalityImpl(
                property, refinedCardinality, filler));

        for (OWLObjectProperty p : properties) {
            for (OWLClassExpression fillerRefinement : fillerRefinements) {
                // First take the original cardinality...
                refinements.add(new OWLObjectMinCardinalityImpl(
                        p, minCardinality, fillerRefinement));
                // ...then the refined cardinality
                refinements.add(new OWLObjectMinCardinalityImpl(
                        p, refinedCardinality, fillerRefinement));
            }
        }

        return refinements;
    }

    private Set<OWLClassExpression> spatiallyRefineOWLObjectSomeValuesFrom(OWLObjectSomeValuesFrom ce, int maxLength) {
        OWLObjectPropertyExpression property = ce.getProperty();
        // FIXME: extend this to also support general object property expressions
        assert !property.isAnonymous();

        SortedSet<OWLObjectProperty> properties =
                // FIXME: Should be part of the spatial reasoner interface
                reasoner.getBaseReasoner().getSubProperties(property.asOWLObjectProperty());
        properties.add(property.asOWLObjectProperty());

        OWLClassExpression filler = ce.getFiller();

        Set<OWLClassExpression> refinements = new HashSet<>();

        // FIXME: replace lengthMetric.objectProperyLength with actual prop length
        Set<OWLClassExpression> fillerRefinements = refine(
                filler,
                maxLength-lengthMetric.objectProperyLength);

        for (OWLObjectProperty p : properties) {
            for (OWLClassExpression fillerRefinement : fillerRefinements) {
                refinements.add(new OWLObjectSomeValuesFromImpl(p, fillerRefinement));
            }
        }

        return refinements;
    }

    private Set<OWLClassExpression> spatiallyRefineOWLObjectIntersectionOf(OWLObjectIntersectionOf intersection, int maxLength) {
        Set<OWLClassExpression> refinements = new HashSet<>();
        if (reasoner.isSuperClassOf(SpatialVocabulary.SpatialFeature, intersection)) {
            // isInside
            refinements.add(new OWLObjectIntersectionOfImpl(Sets.newHashSet(
                    intersection,
                    new OWLObjectSomeValuesFromImpl(
                            SpatialVocabulary.isInside, SpatialVocabulary.SpatialFeature))));

            // isNear
            refinements.add(new OWLObjectIntersectionOfImpl(Sets.newHashSet(
                    intersection,
                    new OWLObjectSomeValuesFromImpl(
                            SpatialVocabulary.isNear, SpatialVocabulary.SpatialFeature))));
        }

        List<OWLClassExpression> operandsList = intersection.getOperandsAsList();
        assert operandsList.size() == 2;

        OWLClassExpression firstOperand = operandsList.get(0);
        OWLClassExpression secondOperand = operandsList.get(1);

        int tmpMaxLength = maxLength - OWLClassExpressionUtils.getLength(secondOperand);
        for (OWLClassExpression refinement1 : refine(firstOperand, tmpMaxLength)) {
            refinements.add(new OWLObjectIntersectionOfImpl(Sets.newHashSet(
                    refinement1, secondOperand)));
        }

        tmpMaxLength = maxLength - OWLClassExpressionUtils.getLength(firstOperand);
        for (OWLClassExpression refinement2 : refine(secondOperand, tmpMaxLength)) {
            refinements.add(new OWLObjectIntersectionOfImpl(Sets.newHashSet(
                    firstOperand, refinement2)));
        }

        return refinements;
    }

    private Set<OWLClassExpression> spatiallyRefineOWLClass(OWLClass cls) {
        Set<OWLClassExpression> refinements = new HashSet<>();
        if (reasoner.isSuperClassOf(SpatialVocabulary.SpatialFeature, cls)) {
            // isInside
            refinements.add(new OWLObjectIntersectionOfImpl(Sets.newHashSet(
                    cls,
                    new OWLObjectSomeValuesFromImpl(
                            SpatialVocabulary.isInside, SpatialVocabulary.SpatialFeature))));

            // isNear
            refinements.add(new OWLObjectIntersectionOfImpl(Sets.newHashSet(
                    cls,
                    new OWLObjectSomeValuesFromImpl(
                            SpatialVocabulary.isNear, SpatialVocabulary.SpatialFeature))));
        }

        return refinements;
    }

    public static void main(String[] args) throws ComponentInitException {
        String exampleFilePath =
                SpatialRhoDRDown.class.getClassLoader()
                        .getResource("test/example_data.owl").getFile();
        KnowledgeSource ks = new OWLFile(exampleFilePath);
        ClosedWorldReasoner cwr = new ClosedWorldReasoner(ks);
        SpatialReasonerPostGIS spatialReasoner = new SpatialReasonerPostGIS(
                cwr, new DBConnectionSetting(
                "localhost",5432, "dllearner",
                "postgres", "postgres"));
        spatialReasoner.init();

        SpatialRhoDRDown refinementOperator = new SpatialRhoDRDown();
        refinementOperator.setReasoner((SpatialReasoner) spatialReasoner);
        refinementOperator.init();
        int maxLength = 3;

        System.out.println("============================================\nRefinements:");
        Set<OWLClassExpression> refinements =
                refinementOperator.refine(SpatialVocabulary.SpatialFeature, maxLength);
        refinements.forEach(System.out::println);

        for (OWLClassExpression ce : refinements) {
            if (OWLClassExpressionUtils.getLength(ce) <= maxLength) {
                Set<OWLClassExpression> refnemnts = refinementOperator.refine(ce, maxLength);
                refnemnts.forEach(System.out::println);
            }
        }
    }
}
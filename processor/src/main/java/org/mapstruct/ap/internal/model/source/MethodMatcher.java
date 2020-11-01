/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.model.source;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.SimpleTypeVisitor6;

import org.mapstruct.ap.internal.util.TypeUtils;

import org.mapstruct.ap.internal.model.common.Parameter;
import org.mapstruct.ap.internal.model.common.Type;
import org.mapstruct.ap.internal.model.common.TypeFactory;

/**
 * SourceMethodMatcher $8.4 of the JavaLanguage specification describes a method body as such:
 *
 * <pre>
 * SourceMethodDeclaration: SourceMethodHeader SourceMethodBody
 * SourceMethodHeader: SourceMethodModifiers TypeParameters Result SourceMethodDeclarator Throws
 * SourceMethodDeclarator: Identifier ( FormalParameterList )
 *
 * example &lt;T extends String &amp; Serializable&gt;  T   getResult(? extends T) throws Exception
 *         \-------------------------------/ \-/            \---------/
 *               TypeParameters             Result        ParameterList
 * </pre>
 *
 * Matches a given method with given ParameterList and Result type obeying the constraints in the TypeParameters block.
 * <p>
 * For more info on java-generics: http://www.javacodegeeks.com/2011/04/java-generics-quick-tutorial.html
 * http://www.angelikalanger.com/GenericsFAQ/FAQSections/ParameterizedTypes.html
 * <p>
 * The following situations is not supported / tested:
 * <ol>
 * <li>Multiple bounds were the bound itself is again a generic type.</li>
 * </ol>
 *
 * @author Sjaak Derksen
 */
public class MethodMatcher {

    private final SourceMethod candidateMethod;
    private final TypeUtils typeUtils;
    private final TypeFactory typeFactory;

    MethodMatcher(TypeUtils typeUtils, TypeFactory typeFactory, SourceMethod candidateMethod) {
        this.typeUtils = typeUtils;
        this.candidateMethod = candidateMethod;
        this.typeFactory = typeFactory;
    }

    /**
     * Whether the given source and target types are matched by this matcher's candidate method.
     *
     * @param sourceTypes the source types
     * @param targetType the target type
     * @return {@code true} when both, source type and target types match the signature of this matcher's method;
     *         {@code false} otherwise.
     */
    boolean matches(List<Type> sourceTypes, Type targetType) {

        GenericAnalyser analyser = new GenericAnalyser( typeFactory, typeUtils, candidateMethod, sourceTypes, targetType );
        if ( analyser.lineUp() == false ) {
            return false;
        }

        for ( int i = 0; i < sourceTypes.size(); i++ ) {
            Type candidateSourceParType = analyser.candidateParTypes.get( i );
            if ( !sourceTypes.get( i ).isAssignableTo( candidateSourceParType ) ) {
                return false;
            }
        }

        // check if the method matches the proper result type to construct
//        Parameter targetTypeParameter = candidateMethod.getTargetTypeParameter();
//        if ( targetTypeParameter != null ) {
//            Type returnClassType = typeFactory.classTypeOf( resultType );
//            if ( !matchSourceType( returnClassType, targetTypeParameter.getType(), genericTypesMap ) ) {
//                return false;
//            }
//        }

        // TODO: is this really needed:
        if ( analyser.positionMappingTargetType != null ) {
            Type mappingTargetType = analyser.candidateParTypes.get( analyser.positionMappingTargetType );
            Type returnClassType = typeFactory.classTypeOf( targetType );
            if ( !returnClassType.isAssignableTo( mappingTargetType ) ) {
                return false;
            }
        }

        if ( !analyser.candidateReturnType.isVoid() ) {
            if ( !analyser.candidateReturnType.isAssignableTo( targetType ) ) {
                return false;
            }
        }

        // check if the method matches the proper result type to construct


        // check result type
//        if ( !matchResultType( targetType, genericTypesMap ) ) {
//            return false;
//        }
//
//        // check if all type parameters are indeed mapped
//        if ( candidateMethod.getExecutable().getTypeParameters().size() != genericTypesMap.size() ) {
//            return false;
//        }
//
//        // check if all entries are in the bounds
//        for ( Map.Entry<TypeVariable, TypeMirror> entry : genericTypesMap.entrySet() ) {
//            if ( !isWithinBounds( entry.getValue(), getTypeParamFromCandidate( entry.getKey() ) ) ) {
//                // checks if the found Type is in bounds of the TypeParameters bounds.
//                return false;
//            }
//        }
        return true;
    }

    private static class GenericAnalyser {

        private TypeFactory typeFactory;
        private TypeUtils typeUtils;
        private Method candidateMethod;
        private List<Type> sourceTypes;
        private Type targetType;

        public GenericAnalyser(TypeFactory typeFactory, TypeUtils typeUtils, Method candidateMethod,
                               List<Type> sourceTypes, Type targetType) {
            this.typeFactory = typeFactory;
            this.typeUtils = typeUtils;
            this.candidateMethod = candidateMethod;
            this.sourceTypes = sourceTypes;
            this.targetType = targetType;
        }

        Type candidateReturnType = null;
        List<Type> candidateParTypes;
        Integer positionMappingTargetType = null;

        private boolean lineUp() {

            if ( candidateMethod.getParameters().size() != sourceTypes.size() ) {
                return false;
            }

            List<Type> genMethodParTypes = candidateMethod.getTypeParameters();
            int nrOfMethodPars = candidateMethod.getParameters().size();

            if ( !genMethodParTypes.isEmpty() ) {

                // find candidates for generic parameter

                // Per method parameter (argument) the set of generic parameters or generic parameterized wildcards
                // with their resolved type.
                List<Map<Type, Type>> genParsForMethodPar = Stream.generate( () -> new HashMap<Type, Type>() )
                    .limit( nrOfMethodPars )
                    .collect( Collectors.toList() );

                Type returnType = candidateMethod.getReturnType();

                Map<Type, Type> genParsForReturnType = new HashMap<>();
                for ( int i = 0; i < nrOfMethodPars; i++ ) {
                    Type sourceType = sourceTypes.get( i );
                    Parameter par = candidateMethod.getParameters().get( i );
                    Type parType = par.getType();
                    if ( par.isMappingTarget() ) {
                        positionMappingTargetType = i;
                    }

                    Map<Type, Type> genParsForThisPar = genParsForMethodPar.get( i );
                    if ( parType.isTypeVarOrBoundByTypeVar() ) {
                        // parType is a generic type or bounded by a generic type
                        genParsForThisPar.put( parType, sourceType );
                    }
                    // investigate the type parameters of parType
                    for ( Type genParPar : parType.getTypeParameters() ) {
                        Type candidateTypeForGenPar = genParPar.resolveTypeVarToType( sourceType, parType );
                        if ( candidateTypeForGenPar != null ) {
                            if ( conflictWithPrior( genParsForThisPar, genParPar, candidateTypeForGenPar ) ) {
                                // fail fast when more candidates are found for the same type parameter
                                return false;
                            }
                            genParsForThisPar.put( genParPar, candidateTypeForGenPar );
                        }
                    }
                }
                if ( !returnType.isVoid() ) {
                    // parType could be a generic type as well
                    if ( returnType.isTypeVarOrBoundByTypeVar() ) {
                        genParsForReturnType.put( returnType, targetType );
                    }
                    for ( Type genParReturn : returnType.getTypeParameters() ) {
                        // should also comply to contract
                        Type candidateTypeForGenPar = genParReturn.resolveTypeVarToType( targetType, returnType );
                        if ( candidateTypeForGenPar != null ) {
                            if ( conflictWithPrior( genParsForReturnType, genParReturn, candidateTypeForGenPar ) ) {
                                // fail fast when more candidates are found for the same type parameter
                                return false;
                            }
                            genParsForReturnType.put( genParReturn, candidateTypeForGenPar );
                        }
                    }
                }

                // validate

                // iterate over the method generic parameters.
                for ( Type genMethodParType : genMethodParTypes ) {

                    // Step 1: Check whether all parameters and return type resolved to the same type for the
                    // generic method parameter type
                    Type candidateTypeForGenPar = null;
                    for (int i = 0; i< nrOfMethodPars; i++ ) {
                        Map<Type, Type> genParsForThisPar = genParsForMethodPar.get( i );
                        Type genParForThisPar = genParsForThisPar.get( genMethodParType );
                        if ( genParForThisPar != null ) {
                            if ( candidateTypeForGenPar == null ) {
                                candidateTypeForGenPar = genParForThisPar;
                            }
                            else if ( !areEquivalent( genParForThisPar, candidateTypeForGenPar ) ) {
                                // fail: the method parameters, parameter types resolved to a different type
                                return false;
                            }
                        }
                    }
                    if ( !returnType.isVoid() ) {
                        Type genParForReturn = genParsForReturnType.get( genMethodParType );
                        if ( genParForReturn != null ) {
                            if ( candidateTypeForGenPar == null ) {
                                candidateTypeForGenPar = genParForReturn; // TODO kan dit valt weg als we alles in 1 set stoppen?
                            }
                            else if ( !areEquivalent( genParForReturn, candidateTypeForGenPar ) ) {
                                // fail: the method parameters, parameter types resolved to a different type
                                return false;
                            }
                        }
                    }

                    // Step 2: Check whether the found parameter (when found) complies to the type bounds.
                    // NOTE: It can be that only references to parameterized wildcards bound are found. See step 3.
                    if ( candidateTypeForGenPar != null &&
                        !compliesToTypeBounds( genMethodParType, candidateTypeForGenPar )) {
                        return false;
                    }

                    // Step 3. It can be that the parameter is only used as wildcard bound: ? extends T in the method
                    // parameter definition, for instance. there's no reference to A directly in:
                    // <A extends Number> List<Integer> getEqualInts(List<? extends A> x, @Context List<? extends A> y)
                    for (int i = 0; i< nrOfMethodPars; i++ ) {
                        Map<Type, Type> genParsForThisPar = genParsForMethodPar.get( i );
                        Type boundary = candidateTypeForGenPar != null ? candidateTypeForGenPar : genMethodParType.getTypeBound();
                        if ( !wildCardsCompliesToBounds( genParsForThisPar, boundary ) ) {
                            return false;
                        }

                    }
                    if ( !returnType.isVoid() ) {
                        Type boundary = candidateTypeForGenPar != null ? candidateTypeForGenPar : genMethodParType.getTypeBound();
                        if ( !wildCardsCompliesToBounds( genParsForReturnType, boundary ) ) {
                            return false;
                        }
                    }
                }

                // resolve parameters & return type
                this.candidateParTypes = new ArrayList<>();
                for ( int i = 0; i < nrOfMethodPars; i++ ) {

                    Type parType = candidateMethod.getParameters().get( i ).getType();
                    Map<Type, Type> genParsForThisPar = genParsForMethodPar.get( i );
                    if ( parType.isTypeVarOrBoundByTypeVar() ) {
                        this.candidateParTypes.add( getTypeArg( genParsForThisPar, parType ) );
                    }
                    else {
                        TypeMirror[] typeArgs = new TypeMirror[parType.getTypeParameters().size()];
                        for ( int j = 0; j < parType.getTypeParameters().size(); j++ ) {
                            Type candidate = getTypeArg(genParsForThisPar, parType.getTypeParameters().get( j ) );
                            if ( candidate == null ) {
                                // E.g. BigDecimalWrapper implements Wrapper<T> drops out here
                                return false;
                            }
                            typeArgs[j] = candidate.getTypeMirror();
                        }
                        DeclaredType typeArg = typeUtils.getDeclaredType( parType.getTypeElement(), typeArgs );
                        this.candidateParTypes.add( typeFactory.getType( typeArg ) );
                    }
                }
                if ( !returnType.isVoid() ) {
                    if ( returnType.isTypeVarOrBoundByTypeVar() ) {
                        this.candidateReturnType = getTypeArg( genParsForReturnType, returnType );
                    }
                    else {
                        TypeMirror[] typeArgs = new TypeMirror[returnType.getTypeParameters().size()];
                        for ( int i = 0; i < returnType.getTypeParameters().size(); i++ ) {
                            Type candidate = getTypeArg( genParsForReturnType, returnType.getTypeParameters().get( i ) );
                            if ( candidate == null ) {
                                // E.g. BigDecimalWrapper implements Wrapper<T> drops out here
                                return false;
                            }
                            typeArgs[i] = candidate.getTypeMirror();
                        }
                        DeclaredType typeArg = typeUtils.getDeclaredType( returnType.getTypeElement(), typeArgs );
                        this.candidateReturnType = typeFactory.getType( typeArg );
                    }
                }
                else {
                    this.candidateReturnType = returnType;
                }
            }
            else {
                this.candidateParTypes = candidateMethod.getParameters().stream()
                    .map( Parameter::getType )
                    .collect( Collectors.toList() );
                this.candidateReturnType = candidateMethod.getReturnType();
            }
            return true;
        }

        private Type getTypeArg(Map<Type, Type> candidates, Type genParPar) {
            Type result = candidates.get( genParPar );
            if ( result != null ) {
                return result;
            }
            // result can be a parameterized wild card, ? extends T
            for ( Map.Entry<Type, Type> entry : candidates.entrySet() ) {
                if (genParPar.equals( entry.getKey().getTypeBound() ) ) {
                    return entry.getValue();
                }
            }
            return null;
        }

        boolean compliesToTypeBounds( Type genParOrWildCard, Type typeToComply ) {
            if ( genParOrWildCard.isWildCardSuperBound() ) {
                return genParOrWildCard.getTypeBound().isAssignableTo( typeToComply );
            }
            // to cope with the extends or upper bound, resolving for typed boundary is handled later
            return typeToComply.erasure().isAssignableTo( genParOrWildCard.getTypeBound().erasure() );
        }

        /**
         * e.g. <T extends TypeBound> void map( List<? extends T> )
         *
         * Concerns cases like ? extends T and ? super T. They are the key in the map. We cannot derive
         * T itself (sometimes), but we can check if the candidate complies to its defined bound.
         *
         * @param candidates Map.Entry(<? extends T>, String)
         * @param genPar e.g. T extends TypeBound
         * @return if String is assignable to TypeBound
         */
        boolean wildCardsCompliesToBounds(Map<Type, Type> candidates, Type genPar) {
            for ( Map.Entry<Type, Type> entry : candidates.entrySet() ) {
                if ( entry.getKey().isWildCardExtendsBound() ) {
                    return entry.getValue().isAssignableTo( genPar );
                }
                if ( entry.getKey().isWildCardSuperBound() ) {
                    return genPar.isAssignableTo( entry.getValue() );
                }
            }
            return true;
        }

        boolean conflictWithPrior(Map<Type, Type> genParTypes, Type genType, Type candidate) {
            return genParTypes.containsKey( genType ) && areEquivalent( genType, candidate );
        }

        boolean areEquivalent( Type a, Type b ) {
            if ( a == null || b == null ) {
                return false;
            }
            TypeMirror aMirror = a.getTypeMirror();
            TypeMirror bMirror = b.getTypeMirror();
            TypeMirror aBoxed = a.isPrimitive() ? typeUtils.boxedClass( (PrimitiveType) aMirror ).asType() : aMirror;
            TypeMirror bBoxed = b.isPrimitive() ? typeUtils.boxedClass( (PrimitiveType) bMirror ).asType() : bMirror;
            return typeUtils.isSameType( aBoxed, bBoxed );
        }
    }

}


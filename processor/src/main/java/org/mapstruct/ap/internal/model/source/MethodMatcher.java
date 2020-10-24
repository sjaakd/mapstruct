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
                                candidateTypeForGenPar = genParForReturn;
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
            if ( genParOrWildCard.isWildCardExtendsBound() ) {
                return typeToComply.isAssignableTo2( genParOrWildCard.getTypeBound() );
            }
            if ( genParOrWildCard.isWildCardSuperBound() ) {
                return genParOrWildCard.getTypeBound().isAssignableTo2( typeToComply );
            }
            // should not happen, the genParOrWildCard does not have any bounds.
            return true;
        }

        boolean wildCardsCompliesToBounds(Map<Type, Type> candidates, Type genPar) {
            for ( Map.Entry<Type, Type> entry : candidates.entrySet() ) {
                if ( entry.getKey().isWildCardExtendsBound() ) {
                    return entry.getValue().isAssignableTo2( genPar );
                }
                if ( entry.getKey().isWildCardSuperBound() ) {
                    return genPar.isAssignableTo2( entry.getValue() );
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

    private boolean matchSourceType(Type sourceType,
                                    Type candidateSourceType,
                                    Map<TypeVariable, TypeMirror> genericTypesMap) {

        if ( !isJavaLangObject( candidateSourceType.getTypeMirror() ) ) {
            TypeMatcher parameterMatcher = new TypeMatcher( Assignability.VISITED_ASSIGNABLE_FROM, genericTypesMap );
            if ( !parameterMatcher.visit( candidateSourceType.getTypeMirror(), sourceType.getTypeMirror() ) ) {
                if ( sourceType.isPrimitive() ) {
                    // the candidate source is primitive, so promote to its boxed type and check again (autobox)
                    TypeMirror boxedType = typeUtils.boxedClass( (PrimitiveType) sourceType.getTypeMirror() ).asType();
                    if ( !parameterMatcher.visit( candidateSourceType.getTypeMirror(), boxedType ) ) {
                        return false;
                    }
                }
                else {
                    // NOTE: unboxing is deliberately not considered here. This should be handled via type-conversion
                    // (for NPE safety).
                    return false;
                }
            }
        }
        return true;
    }

    private boolean matchResultType(Type resultType, Map<TypeVariable, TypeMirror> genericTypesMap) {

        Type candidateResultType = candidateMethod.getResultType();

        if ( !isJavaLangObject( candidateResultType.getTypeMirror() ) && !candidateResultType.isVoid() ) {

            final Assignability visitedAssignability;
            if ( candidateMethod.getReturnType().isVoid() ) {
                // for void-methods, the result-type of the candidate needs to be assignable from the given result type
                visitedAssignability = Assignability.VISITED_ASSIGNABLE_FROM;
            }
            else {
                // for non-void methods, the result-type of the candidate needs to be assignable to the given result
                // type
                visitedAssignability = Assignability.VISITED_ASSIGNABLE_TO;
            }

            TypeMatcher returnTypeMatcher = new TypeMatcher( visitedAssignability, genericTypesMap );
            if ( !returnTypeMatcher.visit( candidateResultType.getTypeMirror(), resultType.getTypeMirror() ) ) {
                if ( resultType.isPrimitive() ) {
                    TypeMirror boxedType = typeUtils.boxedClass( (PrimitiveType) resultType.getTypeMirror() ).asType();
                    TypeMatcher boxedReturnTypeMatcher =
                        new TypeMatcher( visitedAssignability, genericTypesMap );

                    if ( !boxedReturnTypeMatcher.visit( candidateResultType.getTypeMirror(), boxedType ) ) {
                        return false;
                    }
                }
                else if ( candidateResultType.getTypeMirror().getKind().isPrimitive() ) {
                    TypeMirror boxedCandidateReturnType =
                        typeUtils.boxedClass( (PrimitiveType) candidateResultType.getTypeMirror() ).asType();
                    TypeMatcher boxedReturnTypeMatcher =
                        new TypeMatcher( visitedAssignability, genericTypesMap );

                    if ( !boxedReturnTypeMatcher.visit( boxedCandidateReturnType, resultType.getTypeMirror() ) ) {
                        return false;
                    }

                }
                else {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * @param type the type
     * @return {@code true}, if the type represents java.lang.Object
     */
    private boolean isJavaLangObject(TypeMirror type) {
        return type.getKind() == TypeKind.DECLARED
            && ( (TypeElement) ( (DeclaredType) type ).asElement() ).getQualifiedName().contentEquals(
                Object.class.getName() );
    }

    private enum Assignability {
        VISITED_ASSIGNABLE_FROM, VISITED_ASSIGNABLE_TO;

        Assignability invert() {
            return this == VISITED_ASSIGNABLE_FROM
                            ? VISITED_ASSIGNABLE_TO
                            : VISITED_ASSIGNABLE_FROM;
        }
    }

    private class TypeMatcher extends SimpleTypeVisitor6<Boolean, TypeMirror> {
        private final Assignability assignability;
        private final Map<TypeVariable, TypeMirror> genericTypesMap;
        private final TypeMatcher inverse;

        TypeMatcher(Assignability assignability, Map<TypeVariable, TypeMirror> genericTypesMap) {
            super( Boolean.FALSE ); // default value
            this.assignability = assignability;
            this.genericTypesMap = genericTypesMap;
            this.inverse = new TypeMatcher( this, genericTypesMap );
        }

        TypeMatcher(TypeMatcher inverse, Map<TypeVariable, TypeMirror> genericTypesMap) {
            super( Boolean.FALSE ); // default value
            this.assignability = inverse.assignability.invert();
            this.genericTypesMap = genericTypesMap;
            this.inverse = inverse;
        }

        @Override
        public Boolean visitPrimitive(PrimitiveType t, TypeMirror p) {
            return typeUtils.isSameType( t, p );
        }

        @Override
        public Boolean visitArray(ArrayType t, TypeMirror p) {

            if ( p.getKind().equals( TypeKind.ARRAY ) ) {
                return t.getComponentType().accept( this, ( (ArrayType) p ).getComponentType() );
            }
            else {
                return Boolean.FALSE;
            }
        }

        @Override
        public Boolean visitDeclared(DeclaredType t, TypeMirror p) {
            // its a match when: 1) same kind of type, name is equals, nr of type args are the same
            // (type args are checked later).
            if ( p.getKind() == TypeKind.DECLARED ) {
                DeclaredType t1 = (DeclaredType) p;
                if ( rawAssignabilityMatches( t, t1 ) ) {
                    if ( t.getTypeArguments().size() == t1.getTypeArguments().size() ) {
                        // compare type var side by side
                        for ( int i = 0; i < t.getTypeArguments().size(); i++ ) {
                            if ( !visit( t.getTypeArguments().get( i ), t1.getTypeArguments().get( i ) ) ) {
                                return Boolean.FALSE;
                            }
                        }
                        return Boolean.TRUE;
                    }
                    else {
                        // return true (e.g. matching Enumeration<E> with an enumeration E)
                        // but do not try to line up raw type arguments with types that do have arguments.
                        return assignability == Assignability.VISITED_ASSIGNABLE_TO ?
                            !t1.getTypeArguments().isEmpty() : !t.getTypeArguments().isEmpty();
                    }
                }
                else {
                    return Boolean.FALSE;
                }
            }
            else if ( p.getKind() == TypeKind.WILDCARD ) {
                return inverse.visit( p, t ); // inverse, as we switch the params
            }
            else {
                return Boolean.FALSE;
            }
        }

        private boolean rawAssignabilityMatches(DeclaredType t1, DeclaredType t2) {
            if ( assignability == Assignability.VISITED_ASSIGNABLE_TO ) {
                return typeUtils.isAssignable( toRawType( t1 ), toRawType( t2 ) );
            }
            else {
                return typeUtils.isAssignable( toRawType( t2 ), toRawType( t1 ) );
            }
        }

        private DeclaredType toRawType(DeclaredType t) {
            return typeUtils.getDeclaredType( (TypeElement) t.asElement() );
        }

        @Override
        public Boolean visitTypeVariable(TypeVariable t, TypeMirror p) {
            if ( genericTypesMap.containsKey( t ) ) {
                // when already found, the same mapping should apply
                // Then we should visit the resolved generic type.
                // Which can potentially be another generic type
                // e.g.
                // <T> T fromOptional(Optional<T> optional)
                // T resolves to Collection<Integer>
                // We know what T resolves to, so we should treat it as if the method signature was
                // Collection<Integer> fromOptional(Optional<Collection<Integer> optional)
                TypeMirror p1 = genericTypesMap.get( t );
                // p (Integer) should be a subType of p1 (Number)
                // i.e. you can assign p (Integer) to p1 (Number)
                return visit( p, p1 );
            }
            else {
                // check if types are in bound
                TypeMirror lowerBound = t.getLowerBound();
                TypeMirror upperBound = t.getUpperBound();
                if ( ( isNullType( lowerBound ) || typeUtils.isSubtypeErased( lowerBound, p ) )
                    && ( isNullType( upperBound ) || typeUtils.isSubtypeErased( p, upperBound ) ) ) {
                    genericTypesMap.put( t, p );
                    return Boolean.TRUE;
                }
                else {
                    return Boolean.FALSE;
                }
            }
        }

        private boolean isNullType(TypeMirror type) {
            return type == null || type.getKind() == TypeKind.NULL;
        }

        @Override
        public Boolean visitWildcard(WildcardType t, TypeMirror p) {

            // check extends bound
            TypeMirror extendsBound = t.getExtendsBound();
            if ( !isNullType( extendsBound ) ) {
                switch ( extendsBound.getKind() ) {
                    case DECLARED:
                        // for example method: String method(? extends String)
                        // isSubType checks range [subtype, type], e.g. isSubtype [Object, String]==true
                        return visit( extendsBound, p );

                    case TYPEVAR:
                        // for example method: <T extends String & Serializable> T method(? extends T)
                        // this can be done the directly by checking: ? extends String & Serializable
                        // this checks the part? <T extends String & Serializable>
                        return isWithinBounds( p, getTypeParamFromCandidate( extendsBound ) );

                    default:
                        // does this situation occur?
                        return Boolean.FALSE;
                }
            }

            // check super bound
            TypeMirror superBound = t.getSuperBound();
            if ( !isNullType( superBound ) ) {
                switch ( superBound.getKind() ) {
                    case DECLARED:
                        // for example method: String method(? super String)
                        // to check super type, we can simply inverse the argument, but that would initially yield
                        // a result: <type, superType] (so type not included) so we need to check sameType also.
                        return typeUtils.isSubtypeErased( superBound, p ) || typeUtils.isSameType( p, superBound );

                    case TYPEVAR:

                        TypeParameterElement typeParameter = getTypeParamFromCandidate( superBound );
                        // for example method: <T extends String & Serializable> T method(? super T)
                        if ( !isWithinBounds( p, typeParameter ) ) {
                            // this checks the part? <T extends String & Serializable>
                            return Boolean.FALSE;
                        }
                        // now, it becomes a bit more hairy. We have the relation (? super T). From T we know that
                        // it is a subclass of String & Serializable. However, The Java Language Secification,
                        // Chapter 4.4, states that a bound is either: 'A type variable-', 'A class-' or 'An
                        // interface-' type followed by further interface types. So we must compare with the first
                        // argument in the Expression String & Serializable & ..., so, in this case String.
                        // to check super type, we can simply inverse the argument, but that would initially yield
                        // a result: <type, superType] (so type not included) so we need to check sameType also.
                        TypeMirror superBoundAsDeclared = typeParameter.getBounds().get( 0 );
                        return ( typeUtils.isSubtypeErased( superBoundAsDeclared, p ) || typeUtils.isSameType(
                            p,
                            superBoundAsDeclared ) );
                    default:
                        // does this situation occur?
                        return Boolean.FALSE;
                }
            }
            return Boolean.TRUE;
        }

    }

    /**
     * Looks through the list of type parameters of the candidate method for a match
     *
     * @param t type parameter to match
     *
     * @return matching type parameter
     */
    private TypeParameterElement getTypeParamFromCandidate(TypeMirror t) {
        for ( TypeParameterElement candidateTypeParam : candidateMethod.getExecutable().getTypeParameters() ) {
            if ( typeUtils.isSameType( candidateTypeParam.asType(), t ) ) {
                return candidateTypeParam;
            }
        }
        return null;
    }

    /**
     * checks whether a type t is in bounds of the typeParameter tpe
     *
     * @return true if within bounds
     */
    private boolean isWithinBounds(TypeMirror t, TypeParameterElement tpe) {
        List<? extends TypeMirror> bounds = tpe != null ? tpe.getBounds() : null;
        if ( t != null && bounds != null ) {
            for ( TypeMirror bound : bounds ) {
                if ( !( bound.getKind() == TypeKind.DECLARED && typeUtils.isSubtypeErased( t, bound ) ) ) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}


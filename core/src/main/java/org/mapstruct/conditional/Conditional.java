package org.mapstruct.conditional;

import java.lang.annotation.Annotation;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.Qualifier;

public @interface Conditional {

    /**
     * A qualifier can be specified to aid the selection process of a suitable presence check method.
     * This is useful in case multiple presence check methods qualify and thus would result in an
     * 'Ambiguous presence check methods found' error.
     * A qualifier is a custom annotation and can be placed on a hand written mapper class or a method.
     * This is similar to the {@link #qualifiedBy()}, but it is only applied for {@link Condition} methods.
     *
     * @return the qualifiers
     * @see Qualifier
     * @see #qualifiedBy()
     * @since 1.5
     */
    Class<? extends Annotation>[] conditionBy() default { };

    /**
     * String-based form of qualifiers for condition / presence check methods;
     * When looking for a suitable presence check method for a given property, MapStruct will
     * only consider those methods carrying directly or indirectly (i.e. on the class-level) a {@link Named} annotation
     * for each of the specified qualifier names.
     *
     * This is similar like {@link #qualifiedByName()} but it is only applied for {@link Condition} methods.
     * <p>
     *   Note that annotation-based qualifiers are generally preferable as they allow more easily to find references and
     *   are safe for refactorings, but name-based qualifiers can be a less verbose alternative when requiring a large
     *   number of qualifiers as no custom annotation types are needed.
     * </p>
     *
     *
     * @return One or more qualifier name(s)
     * @see #conditionBy()
     * @see #qualifiedByName()
     * @see Named
     * @since 1.5
     */
    String[] conditionByName() default { };

    /**
     * A conditionExpression {@link String} based on which the specified property is to be checked
     * whether it is present or not.
     * <p>
     * Currently, Java is the only supported "expression language" and expressions must be given in form of Java
     * expressions using the following format: {@code java(<EXPRESSION>)}. For instance the mapping:
     * <pre><code>
     * &#64;Mapping(
     *     target = "someProp",
     *     conditionExpression = "java(s.getAge() &#60; 18)"
     * )
     * </code></pre>
     * <p>
     * will cause the following target property assignment to be generated:
     * <pre><code>
     *     if (s.getAge() &#60; 18) {
     *         targetBean.setSomeProp( s.getSomeProp() );
     *     }
     * </code></pre>
     * <p>
     * <p>
     * Any types referenced in expressions must be given via their fully-qualified name. Alternatively, types can be
     * imported via {@link Mapper#imports()}.
     * <p>
     * This attribute can not be used together with {@link #expression()} or {@link #constant()}.
     *
     * @return An expression specifying a condition check for the designated property
     *
     * @since 1.5
     */
    String conditionExpression() default "";

    /**
     * An expression {@link String} based on which the specified target property is to be set.
     * <p>
     * Currently, Java is the only supported "expression language" and expressions must be given in form of Java
     * expressions using the following format: {@code java(<EXPRESSION>)}. For instance the mapping:
     *
     * the current constant could be used here as well.. Perhaps {@code <CONSTANT>} (note: other api places might
     * have the similar reduction..
     *
     */
    String elseDo() default MappingConstants.ElseMapping.NO_ELSE_BRANCH;
}

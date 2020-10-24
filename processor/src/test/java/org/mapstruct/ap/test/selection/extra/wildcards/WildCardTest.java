/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.selection.extra.wildcards;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mapstruct.ap.testutil.WithClasses;
import org.mapstruct.ap.testutil.runner.AnnotationProcessorTestRunner;

/**
 * @author Sjaak Derksen
 *
 */
@RunWith(AnnotationProcessorTestRunner.class)
public class WildCardTest {

    @Test
    @WithClasses( WildCardExtendsMapper.class )
    public void testShouldCreate() {

        WildCardExtendsMapper.Wrapper wrapper = new WildCardExtendsMapper.Wrapper( new WildCardExtendsMapper.TypeC() );

        WildCardExtendsMapper.Target target =
            WildCardExtendsMapper.INSTANCE.map( new WildCardExtendsMapper.Source( wrapper ) );
    }

}

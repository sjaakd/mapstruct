/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.selection.extra.onewrappedparam;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mapstruct.ap.testutil.WithClasses;
import org.mapstruct.ap.testutil.runner.AnnotationProcessorTestRunner;

/**
 * @author Sjaak Derksen
 *
 */
@RunWith(AnnotationProcessorTestRunner.class)
public class OneGenericParameterTest {

    @Test
    @WithClasses( SourceTargetMapper.class )
    public void testShouldCreate() {
        SourceTargetMapper.Target target =
            SourceTargetMapper.INSTANCE.sourceToTarget( new SourceTargetMapper.Source( "test" ) );
    }

}

package org.mapstruct.ap.test.collection.adder.extra;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mapstruct.ap.testutil.WithClasses;
import org.mapstruct.ap.testutil.runner.AnnotationProcessorTestRunner;

@RunWith(AnnotationProcessorTestRunner.class)
public class MyTest {

    @Test
    @WithClasses( MyMapper.class )
    public void test(){

    }
}

package org.mapstruct.conditional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;


/* this is perhaps we something we should already leave up to the user */
@Conditional( conditionBy = CheckForEmpty.IsEmpty.class  )
public @interface CheckForEmpty {

    class Tester {

        @IsEmpty
        public static boolean test(String in) {
            return in.isEmpty();
        }

        @IsEmpty
        public static boolean test(Collection in) {
            return in.isEmpty();
        }
    }

    @Condition
    @Target( ElementType.METHOD )
    @Retention( RetentionPolicy.CLASS )
    public @interface IsEmpty {
    }

}


package org.mapstruct.conditional;

import org.mapstruct.MappingConstants;

@Conditional( elseDo = MappingConstants.ElseMapping.SET_TO_DEFAULT )
public @interface CheckForNullElseDefault {

}

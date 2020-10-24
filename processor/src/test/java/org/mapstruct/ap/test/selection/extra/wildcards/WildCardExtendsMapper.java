package org.mapstruct.ap.test.selection.extra.wildcards;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface WildCardExtendsMapper {

    WildCardExtendsMapper INSTANCE = Mappers.getMapper( WildCardExtendsMapper.class );

    Target map( Source source);

    default <T extends TypeB> T unwrap(Wrapper<? extends T> t) {
        return t.getWrapped();
    }

    class Source {

        private Wrapper<TypeC> prop;

        public Source(Wrapper<TypeC> prop) {
            this.prop = prop;
        }

        public Wrapper<TypeC> getProp() {
            return prop;
        }

        public void setProp( Wrapper<TypeC> prop) {
            this.prop = prop;
        }
    }

    class Wrapper<T> {

        private T wrapped;

        public Wrapper(T wrapped) {
            this.wrapped = wrapped;
        }

        public T getWrapped() {
            return wrapped;
        }

    }

    class Target {

        private TypeC prop;

        public TypeC getProp() {
            return prop;
        }

        public void setProp(TypeC prop) {
            this.prop = prop;
        }
    }

    class TypeC extends TypeB {
    }

    class TypeB extends TypeA {
    }

    class TypeA {
    }

}

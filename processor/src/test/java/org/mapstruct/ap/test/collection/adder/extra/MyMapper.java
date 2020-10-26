package org.mapstruct.ap.test.collection.adder.extra;

import java.util.ArrayList;
import java.util.List;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.TargetType;

@Mapper(collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED )
public interface MyMapper {

    void map(Source source, @MappingTarget Target target);

    default List<Long> toPets(List<String> pets)  {
        return null;
    }

    default <T extends Pet> T toPet(String pet, @TargetType Class<T> clazz) {
        return null;
    }

    class Source {
        private List<String> pets;

        public List<String> getPets() {
            return pets;
        }

        public void setPets(List<String> pets) {
            this.pets = pets;
        }
    }

    class Target {
        private List<Pet> pets;

        public List<Pet> getPets() {
            return pets;
        }

        public void setPets(List<Pet> pets) {
            this.pets = pets;
        }

        public Pet addPet(Pet pet) {
            if ( pets == null ) {
                pets = new ArrayList<>();
            }
            pets.add( pet );
            return pet;
        }
    }

    class Pet {

    }
}

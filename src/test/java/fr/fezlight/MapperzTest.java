package fr.fezlight;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;

class MapperzTest {
    private static final int MAX_EXECUTION_TIME = 10;

    @Data
    @AllArgsConstructor(staticName = "of")
    static final class TestObject {
        Integer integer;
        boolean bool;
        String string;
    }

    @Data
    static final class TestObjectDTO {
        Integer integer;
        Boolean bool;
        String string;
    }

    @Data
    static final class TestObject2 {
        Integer id;
        String description;
        LocalDate releaseDate;
        String notInConstructor;

        public TestObject2(Integer id, String description, LocalDate releaseDate) {
            this.id = id;
            this.description = description;
            this.releaseDate = releaseDate;
        }
    }

    @Data
    @AllArgsConstructor(staticName = "of")
    static final class TestObject2DTO {
        Integer id;
        String description;
        LocalDate releaseDate;
        String inConstructor;
    }

    @Test
    @DisplayName("Given input, output class When init Then I=input class type and O=output class type")
    void testInit() throws NoSuchFieldException, IllegalAccessException {
        Mapperz<TestObject, TestObjectDTO> mapper = Mapperz.init(TestObject.class, TestObjectDTO.class);

        Field inputClass = mapper.getClass().getDeclaredField("inClass");
        Field outClass = mapper.getClass().getDeclaredField("outClass");
        inputClass.setAccessible(true);
        outClass.setAccessible(true);

        assertThat(inputClass.get(mapper)).isEqualTo(TestObject.class);
        assertThat(outClass.get(mapper)).isEqualTo(TestObjectDTO.class);
    }

    @Test
    @DisplayName("Given null values When init Then fail")
    void testInit_nullArgs() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Mapperz.init(null, null)
        );

        assertThat(e.getMessage()).isEqualTo("One of the argument provided is null");
    }

    @Test
    @DisplayName("Given function, biconsumer When declare Then success an has two items")
    @SuppressWarnings("unchecked")
    void testDeclare() throws NoSuchFieldException, IllegalAccessException {
        Mapperz<TestObject, TestObjectDTO> mapper = Mapperz
                .init(TestObject.class, TestObjectDTO.class)
                .declare(TestObject::isBool, TestObjectDTO::setBool)
                .declare(TestObject::getInteger, TestObjectDTO::setInteger);

        Field mappings = mapper.getClass().getDeclaredField("mappings");
        mappings.setAccessible(true);
        Map<Function<TestObject, ?>, BiConsumer<TestObjectDTO, ?>> maps = (Map<Function<TestObject, ?>, BiConsumer<TestObjectDTO, ?>>)mappings.get(mapper);

        assertThat(maps).hasSize(2);
    }

    @Test
    @DisplayName("Given function, biconsumer When map Then success an map to dto without string attribute")
    void testMap() {
        Mapperz<TestObject, TestObjectDTO> mapper = Mapperz
                .init(TestObject.class, TestObjectDTO.class)
                .declare(TestObject::isBool, TestObjectDTO::setBool)
                .declare(TestObject::getInteger, TestObjectDTO::setInteger);

        AtomicReference<TestObjectDTO> result = new AtomicReference<>();
        assertTimeout(Duration.ofMillis(MAX_EXECUTION_TIME), () -> {
            result.set(mapper.map(TestObject.of(12, true, "test")));
        });

        assertThat(result.get()).isNotNull();
        assertThat(result.get().getInteger()).isEqualTo(12);
        assertThat(result.get().getBool()).isTrue();
        // Always null because no mapping has been declared by using declare()
        assertThat(result.get().getString()).isNull();
    }

    @Test
    @DisplayName("Given function, biconsumer When map Then success an map to dto")
    void testMap_allMapped() {
        Mapperz<TestObject, TestObjectDTO> mapper = Mapperz
                .init(TestObject.class, TestObjectDTO.class)
                .declare(TestObject::isBool, TestObjectDTO::setBool)
                .declare(TestObject::getInteger, TestObjectDTO::setInteger)
                .declare(TestObject::getString, TestObjectDTO::setString);

        AtomicReference<TestObjectDTO> result = new AtomicReference<>();
        assertTimeout(Duration.ofMillis(MAX_EXECUTION_TIME), () -> {
            result.set(mapper.map(TestObject.of(12, true, "test")));
        });

        assertThat(result.get()).isNotNull();
        assertThat(result.get().getInteger()).isEqualTo(12);
        assertThat(result.get().getBool()).isTrue();
        assertThat(result.get().getString()).isEqualTo("test");
    }

    @Test
    @DisplayName("Given function, biconsumer with null input When map Then success and return null")
    void testMap_nullInput() {
        Mapperz<TestObject, TestObjectDTO> mapper = Mapperz
                .init(TestObject.class, TestObjectDTO.class)
                .declare(TestObject::isBool, TestObjectDTO::setBool)
                .declare(TestObject::getInteger, TestObjectDTO::setInteger);

        AtomicReference<TestObjectDTO> result = new AtomicReference<>();
        assertTimeout(Duration.ofMillis(MAX_EXECUTION_TIME), () -> {
            result.set(mapper.map(null));
        });

        assertThat(result.get()).isNull();
    }

    @Test
    @DisplayName("Given function, biconsumer, output class with not enough constructor params When map Then error ")
    void testMap_usingDeclareConstructorErrorWhenInstanciate() {
        Mapperz<TestObject2DTO, TestObject2> mapper = Mapperz
                .init(TestObject2DTO.class, TestObject2.class)
                .declareInConstructor(TestObject2DTO::getId)
                .declareInConstructor(TestObject2DTO::getDescription);
                // Missing one constructor params

        LocalDate date = LocalDate.now();
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
            mapper.map(TestObject2DTO.of(12, "test", date, "test2"));
        });

        assertThat(e.getMessage()).isEqualTo(
                "Be sure to provide arguments in constructor in the right order when you use declareInConstructor() " +
                "method or when you have an output object with no default constructor."
        );
    }

    @Test
    @DisplayName("Given function, biconsumer, output class with constructor params When map Then success ")
    void testMap_usingDeclareConstructor() {
        Mapperz<TestObject2DTO, TestObject2> mapper = Mapperz
                .init(TestObject2DTO.class, TestObject2.class)
                .declareInConstructor(TestObject2DTO::getId)
                .declareInConstructor(TestObject2DTO::getDescription)
                .declareInConstructor(TestObject2DTO::getReleaseDate);

        LocalDate date = LocalDate.now();
        AtomicReference<TestObject2> result = new AtomicReference<>();
        assertTimeout(Duration.ofMillis(MAX_EXECUTION_TIME), () -> {
            result.set(mapper.map(
                    TestObject2DTO.of(12, "test", date, "test2")
            ));
        });

        assertThat(result.get()).isNotNull();
        assertThat(result.get().getId()).isEqualTo(12);
        assertThat(result.get().getDescription()).isEqualTo("test");
        assertThat(result.get().getReleaseDate()).isEqualTo(date);
        // Not mapped with declare()
        assertThat(result.get().getNotInConstructor()).isNull();
    }

    @Test
    @DisplayName("Given function, biconsumer, output class with constructor params and all args mapped When map Then success")
    void testMap_usingDeclareConstructorAllMapped() {
        Mapperz<TestObject2DTO, TestObject2> mapper = Mapperz
                .init(TestObject2DTO.class, TestObject2.class)
                .declareInConstructor(TestObject2DTO::getId)
                .declareInConstructor(TestObject2DTO::getDescription)
                .declareInConstructor(TestObject2DTO::getReleaseDate)
                .declare(TestObject2DTO::getInConstructor, TestObject2::setNotInConstructor);

        LocalDate date = LocalDate.now();
        AtomicReference<TestObject2> result = new AtomicReference<>();
        assertTimeout(Duration.ofMillis(MAX_EXECUTION_TIME), () -> {
            result.set(mapper.map(
                    TestObject2DTO.of(12, "test", date, "test2")
            ));
        });

        assertThat(result.get()).isNotNull();
        assertThat(result.get().getId()).isEqualTo(12);
        assertThat(result.get().getDescription()).isEqualTo("test");
        assertThat(result.get().getReleaseDate()).isEqualTo(date);
        assertThat(result.get().getNotInConstructor()).isEqualTo("test2");
    }

    @Test
    @DisplayName("Given function, biconsumer, with custom formatter on field When map Then success")
    void testMap_usingDeclareWithFormatter() {
        Mapperz<TestObject, TestObjectDTO> mapper = Mapperz
                .init(TestObject.class, TestObjectDTO.class)
                .declare(TestObject::getString, TestObjectDTO::setInteger, Integer::valueOf);

        AtomicReference<TestObjectDTO> result = new AtomicReference<>();
        assertTimeout(Duration.ofMillis(MAX_EXECUTION_TIME), () -> {
            result.set(mapper.map(
                    TestObject.of(1, true, "20")
            ));
        });

        assertThat(result.get()).isNotNull();
        assertThat(result.get().getInteger()).isEqualTo(20);
    }

    @Test
    @DisplayName("Given function, biconsumer, with custom formatter on field When map Then success")
    void testMap_usingDeclareWithMoreComplexFormatter() {
        Function<String, Integer> function = (d) -> {
            int i = Integer.parseInt(d);
            return i + 40;
        };
        Mapperz<TestObject, TestObjectDTO> mapper = Mapperz
                .init(TestObject.class, TestObjectDTO.class)
                .declare(TestObject::getString, TestObjectDTO::setInteger, function);

        AtomicReference<TestObjectDTO> result = new AtomicReference<>();
        assertTimeout(Duration.ofMillis(MAX_EXECUTION_TIME), () -> {
            result.set(mapper.map(
                    TestObject.of(1, true, "20")
            ));
        });

        assertThat(result.get()).isNotNull();
        assertThat(result.get().getInteger()).isEqualTo(60);
    }

    @Test
    @DisplayName("Given function, biconsumer, with custom formatter on field When map Then success")
    void testMap_usingDeclareWithMoreComplexFormatterClass() {
        Mapperz<TestObject, TestObjectDTO> mapper = Mapperz
                .init(TestObject.class, TestObjectDTO.class)
                .declare(TestObject::getString, TestObjectDTO::setInteger, new TestFormatter());

        AtomicReference<TestObjectDTO> result = new AtomicReference<>();
        assertTimeout(Duration.ofMillis(MAX_EXECUTION_TIME), () -> {
            result.set(mapper.map(
                    TestObject.of(1, true, "20")
            ));
        });

        assertThat(result.get()).isNotNull();
        assertThat(result.get().getInteger()).isEqualTo(60);
    }

}

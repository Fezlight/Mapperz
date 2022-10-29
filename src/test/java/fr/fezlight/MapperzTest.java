package fr.fezlight;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class MapperzTest {
    @Data
    @AllArgsConstructor(staticName = "of")
    public static class TestObject {
        Integer integer;
        boolean bool;
        String string;
        List<String> stringList;
    }

    @Data
    public static class TestObjectDTO {
        Integer integer;
        Boolean bool;
        String string;
        List<Integer> stringList;
    }

    @Getter
    public static final class TestObject2 {
        Integer id;
        String description;
        LocalDate releaseDate;
        @Setter
        String notInConstructor;

        public TestObject2(Integer id, String description, LocalDate releaseDate) {
            this.id = id;
            this.description = description;
            this.releaseDate = releaseDate;
        }
    }

    @Data
    @AllArgsConstructor(staticName = "of")
    public static final class TestObject2DTO {
        Integer id;
        String description;
        LocalDate releaseDate;
        String inConstructor;
    }

    @Data
    @AllArgsConstructor(staticName = "of")
    public static final class TestObject3 {
        private String string;
        private List<String> stringList;
        private Map<String, Object> map;
        private List<String> otherStringList;
    }

    @Data
    public static final class TestObject3DTO {
        private List<String> string;
        private String stringList;
        private List<String> map;
        private List<Integer> otherStringList;
    }

    @Data
    @AllArgsConstructor(staticName = "of")
    public static final class TestObject4 {
        private String string;
        private Integer integer;
        private boolean bool;
        private Object o;
        private List<Integer> integerList;
    }

    @Data
    public static final class TestObject4DTO {
        private String string;
        private Integer integer;
        private Boolean bool;
        private Object other;
        private List<Integer> integerList;
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

        assertThat(e.getMessage()).isEqualTo("Mapperz.init() - One of the argument provided is null");
    }

    @Test
    @DisplayName("Given function, biconsumer When declare Then success and has two items")
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
    @DisplayName("Given function, biconsumer When declareAutomatic Then success and has two items")
    @SuppressWarnings("unchecked")
    void testDeclareAutomatic() throws NoSuchFieldException, IllegalAccessException {
        Mapperz<TestObject, TestObjectDTO> mapper = Mapperz
                .init(TestObject.class, TestObjectDTO.class)
                .declareAutomatic(Collections.singletonList("stringList"));

        Field mappings = mapper.getClass().getDeclaredField("mappings");
        mappings.setAccessible(true);
        Map<Function<TestObject, ?>, BiConsumer<TestObjectDTO, ?>> maps = (Map<Function<TestObject, ?>, BiConsumer<TestObjectDTO, ?>>)mappings.get(mapper);

        assertThat(maps).hasSize(3);
    }

    @Test
    @DisplayName("Given function, biconsumer When declareAutomatic with invalid target class parameterized type (List<String> to String) field Then error")
    void testDeclareAutomatic_errorParameterizedType_TargetClassNotValidWithNoParameterizedType() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Mapperz
                .init(TestObject3.class, TestObject3DTO.class)
                .declareAutomatic(Arrays.asList("map", "otherStringList", "string")));

        assertEquals(String.format(Mapperz.ERROR_MAPPING_FIELDS_TYPE_DIFFER, "stringList", "java.util.List<java.lang.String>", "class java.lang.String"), e.getMessage());
    }

    @Test
    @DisplayName("Given function, biconsumer When declareAutomatic with invalid target class parameterized type (Map<String, Object> to List<String>) Then error")
    void testDeclareAutomatic_errorParameterizedType_TargetClassNotValidWithLessParameterizedType() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Mapperz
                .init(TestObject3.class, TestObject3DTO.class)
                .declareAutomatic(Arrays.asList("stringList", "otherStringList", "string")));

        assertEquals(String.format(Mapperz.ERROR_MAPPING_FIELDS_TYPE_DIFFER, "map", "java.util.Map<java.lang.String, java.lang.Object>", "java.util.List<java.lang.String>"), e.getMessage());
    }

    @Test
    @DisplayName("Given function, biconsumer When declareAutomatic with invalid target class parameterized type (List<String> to List<Integer>) Then error")
    void testDeclareAutomatic_errorParameterizedType_TargetClassNotValidWithInvalidParameterizedType() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Mapperz
                .init(TestObject3.class, TestObject3DTO.class)
                .declareAutomatic(Arrays.asList("stringList", "map", "string")));

        assertEquals(String.format(Mapperz.ERROR_MAPPING_FIELDS_TYPE_DIFFER, "otherStringList", "java.util.List<java.lang.String>", "java.util.List<java.lang.Integer>"), e.getMessage());
    }

    @Test
    @DisplayName("Given function, biconsumer When declareAutomatic with invalid target class parameterized type (String to List<String>) Then error")
    void testDeclareAutomatic_errorParameterizedType_OriginClassNotValidWithInvalidParameterizedType() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Mapperz
                .init(TestObject3.class, TestObject3DTO.class)
                .declareAutomatic(Arrays.asList("stringList", "map", "otherStringList")));

        assertEquals(String.format(Mapperz.ERROR_MAPPING_FIELDS_TYPE_DIFFER, "string", "class java.lang.String", "java.util.List<java.lang.String>"), e.getMessage());
    }

    @Test
    @DisplayName("Given function, biconsumer When map Then success and map to dto without string attribute")
    void testMap() {
        Mapperz<TestObject, TestObjectDTO> mapper = Mapperz
                .init(TestObject.class, TestObjectDTO.class)
                .declare(TestObject::isBool, TestObjectDTO::setBool)
                .declare(TestObject::getInteger, TestObjectDTO::setInteger);

        TestObjectDTO result = mapper.map(TestObject.of(12, true, "test", Collections.singletonList("")));

        assertThat(result).isNotNull();
        assertThat(result.getInteger()).isEqualTo(12);
        assertThat(result.getBool()).isTrue();
        // Always null because no mapping has been declared by using declare()
        assertThat(result.getString()).isNull();
    }

    @Test
    @DisplayName("Given function, biconsumer When map Then success and map to dto")
    void testMap_allMapped() {
        Mapperz<TestObject, TestObjectDTO> mapper = Mapperz
                .init(TestObject.class, TestObjectDTO.class)
                .declare(TestObject::isBool, TestObjectDTO::setBool)
                .declare(TestObject::getInteger, TestObjectDTO::setInteger)
                .declare(TestObject::getStringList, TestObjectDTO::setStringList, i -> Collections.singletonList(1))
                .declare(TestObject::getString, TestObjectDTO::setString);

        TestObjectDTO result = mapper.map(TestObject.of(12, true, "test", Collections.singletonList("")));

        assertThat(result).isNotNull();
        assertThat(result.getInteger()).isEqualTo(12);
        assertThat(result.getBool()).isTrue();
        assertThat(result.getStringList())
                .hasSize(1)
                .anyMatch(i -> i == 1);
        assertThat(result.getString()).isEqualTo("test");
    }

    @Test
    @DisplayName("Given function, biconsumer with declareAutomatic and manual When map Then success and map to dto")
    void testMap_declareAutomatic_allMapped_withManual() {
        Mapperz<TestObject, TestObjectDTO> mapper = Mapperz
                .init(TestObject.class, TestObjectDTO.class)
                .declare(TestObject::getStringList, TestObjectDTO::setStringList, i -> Collections.singletonList(1))
                .declareAutomatic(Collections.singletonList("stringList"));

        TestObjectDTO result = mapper.map(TestObject.of(12, true, "test", Collections.singletonList("")));

        assertThat(result).isNotNull();
        assertThat(result.getInteger()).isEqualTo(12);
        assertThat(result.getBool()).isTrue();
        assertThat(result.getStringList())
                .hasSize(1)
                .anyMatch(i -> i == 1);
        assertThat(result.getString()).isEqualTo("test");
    }

    @Test
    @DisplayName("Given function, biconsumer with declareAutomatic When map Then success and map to dto")
    void testMap_declareAutomatic_allMapped_withoutManual() {
        Mapperz<TestObject4, TestObject4DTO> mapper = Mapperz
                .init(TestObject4.class, TestObject4DTO.class)
                .declareAutomatic();

        TestObject4DTO result = mapper.map(TestObject4.of("test", 12, false, new Object(), Arrays.asList(1, 2)));

        assertThat(result).isNotNull();
        assertThat(result.getInteger()).isEqualTo(12);
        assertThat(result.getBool()).isFalse();
        assertNull(result.getOther());
        assertThat(result.getIntegerList())
                .hasSize(2)
                .contains(1, 2);
        assertThat(result.getString()).isEqualTo("test");
    }

    @Test
    @DisplayName("Given function, biconsumer with declareAutomatic and manual When map multiple times Then success and map to dto")
    void testMap_declareAutomatic_allMapped_withManual_multimapping() {
        Mapperz<TestObject, TestObjectDTO> mapper = Mapperz
                .init(TestObject.class, TestObjectDTO.class)
                .declare(TestObject::getStringList, TestObjectDTO::setStringList, i -> Collections.singletonList(1))
                .declareAutomatic(Collections.singletonList("stringList"));

        List<TestObject> list = Arrays.asList(
                TestObject.of(12, true, "test", Collections.singletonList("")),
                TestObject.of(12, true, "test", Collections.singletonList("")),
                TestObject.of(12, true, "test", Collections.singletonList("")),
                TestObject.of(12, true, "test", Collections.singletonList("")),
                TestObject.of(12, true, "test", Collections.singletonList("")),
                TestObject.of(12, true, "test", Collections.singletonList("")),
                TestObject.of(12, true, "test", Collections.singletonList("")),
                TestObject.of(12, true, "test", Collections.singletonList("")),
                TestObject.of(12, true, "test", Collections.singletonList("")),
                TestObject.of(12, true, "test", Collections.singletonList("")),
                TestObject.of(12, true, "test", Collections.singletonList("")),
                TestObject.of(12, true, "test", Collections.singletonList("")),
                TestObject.of(12, true, "test", Collections.singletonList("")),
                TestObject.of(12, true, "test", Collections.singletonList("")),
                TestObject.of(12, true, "test", Collections.singletonList("")),
                TestObject.of(12, true, "test", Collections.singletonList("")),
                TestObject.of(12, true, "test", Collections.singletonList("")),
                TestObject.of(12, true, "test", Collections.singletonList("")),
                TestObject.of(12, true, "test", Collections.singletonList("")),
                TestObject.of(12, true, "test", Collections.singletonList("")),
                TestObject.of(12, true, "test", Collections.singletonList("")),
                TestObject.of(12, true, "test", Collections.singletonList("")),
                TestObject.of(12, true, "test", Collections.singletonList("")),
                TestObject.of(12, true, "test", Collections.singletonList(""))
        );

        List<TestObjectDTO> result = list.parallelStream()
                .map(mapper::map)
                .collect(Collectors.toList());

        assertThat(result)
                .isNotNull()
                .hasSize(24)
                .allMatch(TestObjectDTO::getBool)
                .allMatch(t -> t.getInteger() == 12)
                .allMatch(t -> t.getStringList().stream().allMatch(i -> i == 1))
                .allMatch(t -> "test".equals(t.getString()));
    }

    @Test
    @DisplayName("Given function and destination dto with datas, biconsumer When map Then success and map to dto")
    void testMap_allMapped_preserve_destination_data() {
        Mapperz<TestObject, TestObjectDTO> mapper = Mapperz
                .init(TestObject.class, TestObjectDTO.class)
                .declare(TestObject::isBool, TestObjectDTO::setBool)
                .declare(TestObject::getInteger, TestObjectDTO::setInteger)
                .declare(TestObject::getString, TestObjectDTO::setString);

        TestObjectDTO testObjectDto = new TestObjectDTO();
        testObjectDto.setString("test");

        TestObjectDTO result = mapper.map(TestObject.of(12, true, null, null), () -> testObjectDto);

        assertThat(result).isNotNull();
        assertThat(result.getInteger()).isEqualTo(12);
        assertThat(result.getBool()).isTrue();
        assertThat(result.getString()).isEqualTo("test");
    }

    @Test
    @DisplayName("Given function, biconsumer with null input When map Then success and return null")
    void testMap_nullInput() {
        Mapperz<TestObject, TestObjectDTO> mapper = Mapperz
                .init(TestObject.class, TestObjectDTO.class)
                .declare(TestObject::isBool, TestObjectDTO::setBool)
                .declare(TestObject::getInteger, TestObjectDTO::setInteger);

        TestObjectDTO result = mapper.map(null);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Given function, biconsumer, output class with not enough constructor params When map Then error ")
    void testMap_usingDeclareConstructorErrorWhenInstanciate() {
        Mapperz<TestObject2DTO, TestObject2> mapper = Mapperz
                .init(TestObject2DTO.class, TestObject2.class)
                .declareInConstructor(TestObject2DTO::getId, Integer.class)
                .declareInConstructor(TestObject2DTO::getDescription, String.class);
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
    @DisplayName("Given function, biconsumer, output class with one args null in constructor When map Then success")
    void testMap_usingDeclareConstructor_oneArgsNull() {
        Mapperz<TestObject2DTO, TestObject2> mapper = Mapperz
                .init(TestObject2DTO.class, TestObject2.class)
                .declareInConstructor(TestObject2DTO::getId, Integer.class)
                .declareInConstructor(TestObject2DTO::getDescription, String.class)
                .declareInConstructor(TestObject2DTO::getReleaseDate, LocalDate.class);

        TestObject2 result = mapper.map(TestObject2DTO.of(12, "test", null, "test2"));

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(12);
        assertThat(result.getDescription()).isEqualTo("test");
        assertThat(result.getReleaseDate()).isNull();
        // Not mapped with declare()
        assertThat(result.getNotInConstructor()).isNull();
    }

    @Test
    @DisplayName("Given function, biconsumer, output class with constructor params When map Then success ")
    void testMap_usingDeclareConstructor() {
        Mapperz<TestObject2DTO, TestObject2> mapper = Mapperz
                .init(TestObject2DTO.class, TestObject2.class)
                .declareInConstructor(TestObject2DTO::getId, Integer.class)
                .declareInConstructor(TestObject2DTO::getDescription, String.class)
                .declareInConstructor(TestObject2DTO::getReleaseDate, LocalDate.class);

        LocalDate date = LocalDate.now();
        TestObject2 result = mapper.map(TestObject2DTO.of(12, "test", date, "test2"));

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(12);
        assertThat(result.getDescription()).isEqualTo("test");
        assertThat(result.getReleaseDate()).isEqualTo(date);
        // Not mapped with declare()
        assertThat(result.getNotInConstructor()).isNull();
    }

    @Test
    @DisplayName("Given function, biconsumer, output class with constructor params and all args mapped When map Then success")
    void testMap_usingDeclareConstructorAllMapped() {
        Mapperz<TestObject2DTO, TestObject2> mapper = Mapperz
                .init(TestObject2DTO.class, TestObject2.class)
                .declareInConstructor(TestObject2DTO::getId, Integer.class)
                .declareInConstructor(TestObject2DTO::getDescription, String.class)
                .declareInConstructor(TestObject2DTO::getReleaseDate, LocalDate.class)
                .declare(TestObject2DTO::getInConstructor, TestObject2::setNotInConstructor);

        LocalDate date = LocalDate.now();
        TestObject2 result = mapper.map(TestObject2DTO.of(12, "test", date, "test2"));

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(12);
        assertThat(result.getDescription()).isEqualTo("test");
        assertThat(result.getReleaseDate()).isEqualTo(date);
        assertThat(result.getNotInConstructor()).isEqualTo("test2");
    }

    @Test
    @DisplayName("Given function, biconsumer, with custom formatter on field When map Then success")
    void testMap_usingDeclareWithFormatter() {
        Mapperz<TestObject, TestObjectDTO> mapper = Mapperz
                .init(TestObject.class, TestObjectDTO.class)
                .declare(TestObject::getString, TestObjectDTO::setInteger, Integer::valueOf);

        TestObjectDTO result = mapper.map(TestObject.of(1, true, "20", Collections.singletonList("")));

        assertThat(result).isNotNull();
        assertThat(result.getInteger()).isEqualTo(20);
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

        TestObjectDTO result = mapper.map(TestObject.of(1, true, "20", Collections.singletonList("")));

        assertThat(result).isNotNull();
        assertThat(result.getInteger()).isEqualTo(60);
    }

    @Test
    @DisplayName("Given function, biconsumer, with custom formatter on field When map Then success")
    void testMap_usingDeclareWithMoreComplexFormatterClass() {
        Mapperz<TestObject, TestObjectDTO> mapper = Mapperz
                .init(TestObject.class, TestObjectDTO.class)
                .declare(TestObject::getString, TestObjectDTO::setInteger, new TestFormatter());

        TestObjectDTO result = mapper.map(TestObject.of(1, true, "20", Collections.singletonList("")));

        assertThat(result).isNotNull();
        assertThat(result.getInteger()).isEqualTo(60);
    }
}

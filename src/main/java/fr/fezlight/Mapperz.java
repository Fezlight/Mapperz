package fr.fezlight;

import de.cronn.reflection.util.PropertyUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

/**
 * Class used to map from any object to another by using lambda expression
 * and clear syntax to be easy to use.
 *
 * @param <I> Input class generic type
 * @param <O> Output class generic type
 * @author FezLight
 * @version 1.1.0
 * @since 1.0.0
 */
public final class Mapperz<I, O> {
    public static final String ERROR_MAPPING_FIELDS_TYPE_DIFFER =
            "Mapping between '%s' fields cannot be achieve because types differ from %s to %s\n" +
            "- Rename this field to avoid auto mapping or declare it manually with declare() method\n" +
            "Note : Be sure to exclude field from auto-mapping after manual mapping by using declareAutomatic(<excludedFields>)";
    private final Map<Function<I, Object>, BiConsumer<O, Object>> mappings = new HashMap<>();
    private final List<Function<I, Object>> listArgsConstructor = new ArrayList<>();
    private final List<PropertyDescriptor> inputPropertyDescriptors;

    private final List<PropertyDescriptor> outputPropertyDescriptors;
    private final Class<I> inClass;
    private final Class<O> outClass;

    private Mapperz(Class<I> inClass, Class<O> outClass) {
        this.inClass = inClass;
        this.inputPropertyDescriptors = new ArrayList<>(PropertyUtils.getPropertyDescriptors(inClass));
        this.outputPropertyDescriptors = new ArrayList<>(PropertyUtils.getPropertyDescriptors(outClass));
        this.outClass = outClass;
    }

    /**
     * Method used to init a mapper object with <code>input</code> class and target <code>output</code>
     * class to be mapped.
     *
     * @param input  Input class type
     * @param output Output class type
     * @param <I>    Input class generic type inherited from <code>input</code> class
     * @param <O>    Output class generic type inherited from <code>output</code> class
     * @return a new Mapperz Object with input and output types ready for mapping
     */
    public static <I, O> Mapperz<I, O> init(Class<I> input, Class<O> output) {
        if(input == null || output == null) {
            throw new IllegalArgumentException("Mapperz.init() - One of the argument provided is null");
        }
        return new Mapperz<>(input, output);
    }

    /**
     * Method used to declare one-field value to be provided into constructor when creating new output instance.
     * <p>
     * Re-use multiple time for each field inside input class who need to be added to constructor.
     *
     * @param from Input class function can return value (most of the time it can be the getter method)
     * @param <D>  Value class type to be mapped into constructor
     * @return current instance to be chained
     */
    @SuppressWarnings("unchecked")
    public <D> Mapperz<I, O> declareInConstructor(Function<I, D> from) {
        listArgsConstructor.add((Function<I, Object>)from);
        return this;
    }

    /**
     * Method used to declare one-field mapping to another from input class to
     * output class.
     * <p>
     * Re-use multiple time for each field inside input class who need to be mapped.
     *
     * @param from Input class function can return value (most of the time it can be the getter method)
     * @param to   Output class consumer can accept value (most of the time it can be the setter method)
     * @param <D>  Value class type to be mapped from <code>input</code> to <code>output</code>
     * @return current instance to be chained
     */
    public <D> Mapperz<I, O> declare(Function<I, D> from, BiConsumer<O, D> to) {
       return declare(from, to, UnaryOperator.identity());
    }

    /**
     * Method used to declare one-field mapping to another from input class to
     * output class.
     * <p>
     * Re-use multiple time for each field inside input class who need to be mapped.
     *
     * @param from Input class function can return value (most of the time it can be the getter method)
     * @param to   Output class consumer can accept value (most of the time it can be the setter method)
     * @param formatter a formatter to apply to <code>D</code> object (apply transformation to object)
     * @param <D>  Value class type to be mapped from <code>input</code> to <code>output</code>
     * @param <D1> Value class type after formatting by <code>formatter</code>
     * @return current instance to be chained
     */
    @SuppressWarnings("unchecked")
    public <D, D1> Mapperz<I, O> declare(Function<I, D> from, BiConsumer<O, D1> to, Function<D, D1> formatter) {
        mappings.put(from.andThen(formatter), (BiConsumer<O, Object>) to);
        return this;
    }

    /**
     * Method used to automate mappings for all field from input class to output class using reflection.
     *
     * @return current instance to be chained
     */
    public Mapperz<I, O> declareAutomatic(){
        return declareAutomatic(null);
    }

    /**
     * Method used to automate mappings for all field from input class to output class using reflection.
     *
     * @param excludedFields list of all field name need to be excluded from auto-mapping
     * @return current instance to be chained
     */
    public Mapperz<I, O> declareAutomatic(List<String> excludedFields){
        List<PropertyDescriptor> inputFields = inputPropertyDescriptors.stream()
                .filter(field -> excludedFields == null || !excludedFields.contains(field.getName()))
                .filter(field -> isFieldExistInTargetClass(outClass, field.getName()))
                .collect(Collectors.toList());

        inputFields.forEach(inputField -> {
            String fieldName = inputField.getName();

            Function<I, Object> from = input -> PropertyUtils.read(input, inputField);

            PropertyDescriptor outputField = getPropertyDescriptorOfField(outputPropertyDescriptors, fieldName);

            Type getterType = inputField.getReadMethod().getGenericReturnType();
            Type setterType = outputField.getWriteMethod().getGenericParameterTypes()[0];

            validateGenericType(getterType, setterType, fieldName);

            BiConsumer<O, Object> biConsumer = (output, data) -> PropertyUtils.write(output, outputField, data);

            this.declare(from, biConsumer);
        });
        return this;
    }

    private static void validateGenericType(Type getterType, Type setterType, String fieldName) {
        if (!(getterType instanceof ParameterizedType) && !(setterType instanceof ParameterizedType)) {
            return;
        }

        Type[] typeGetter = null;
        if (getterType instanceof ParameterizedType) {
            typeGetter = ((ParameterizedType) getterType).getActualTypeArguments();
        }

        Type[] typeSetter = null;
        if (setterType instanceof ParameterizedType) {
            typeSetter = ((ParameterizedType) setterType).getActualTypeArguments();
        }

        if (typeSetter == null || typeGetter == null) {
            throw new IllegalArgumentException(String.format(ERROR_MAPPING_FIELDS_TYPE_DIFFER, fieldName, getterType, setterType));
        }

        if (typeGetter.length != typeSetter.length) {
            throw new IllegalArgumentException(String.format(ERROR_MAPPING_FIELDS_TYPE_DIFFER, fieldName, getterType, setterType));
        }

        for (int i = 0; i < typeGetter.length; i++) {
            if (!typeGetter[i].getTypeName().equals(typeSetter[i].getTypeName())) {
                throw new IllegalArgumentException(String.format(ERROR_MAPPING_FIELDS_TYPE_DIFFER, fieldName, getterType, setterType));
            }
        }
    }

    /**
     * Method used to start mapping between input class and output class using declared items previously provided.
     * If any of the input class field is not declared into mappings, using {@link Mapperz#declare(Function, BiConsumer)},
     * it will not be mapped to output class.
     * @param input Input class instance
     * @return Output class instance with all value declared mapped from input class or null.
     */
    public O map(I input) {
        return map(input, this::instanciate);
    }

    /**
     * Method used to start mapping between input class and output class using declared items previously provided.
     * If any of the input class field is not declared into mappings, using {@link Mapperz#declare(Function, BiConsumer)},
     * it will not be mapped to output class.
     * @param input Input class instance
     * @param output Output class if you want to provide, be careful if you use <br>
     * {@link Mapperz#declareInConstructor(Function)}, you cannot use this field to override. Instead use {@link Mapperz#map(Object)}
     * @return Output class instance with all value declared mapped from input class or null.
     */
    public O map(I input, Supplier<O> output) {
        // Return a null output if input is null
        if(input == null) {
            return null;
        }

        if(!listArgsConstructor.isEmpty()) {
            Object[] args = listArgsConstructor.parallelStream()
                    .map(f -> f.apply(input))
                    .toArray();
            return executeMapping(input, this.instanciate(args));
        }

        return executeMapping(input, output.get());
    }

    /**
     * Method used to execute all mappings between each field declared into {@link Mapperz#mappings}.
     * @param input Input class instance
     * @param output Output class instance to be mapped
     * @return the output class instance with all mappings executed.
     */
    private O executeMapping(I input, O output) {
        mappings.entrySet()
                .parallelStream()
                .map(m -> resolve(m.getKey(), m.getValue()))
                .forEach(biFunction -> biFunction.apply(input, output));
        return output;
    }

    /**
     * Method used to create a bi-function able to resolve all mappings between each field.
     * @param from Input class function can return value (most of the time it can be the getter method)
     * @param to Output class consumer can accept value (most of the time it can be the setter method)
     * @return a bi-function used to resolve mappings.
     */
    private BiFunction<I, O, O> resolve(Function<I, Object> from, BiConsumer<O, Object> to) {
        return (input, output) -> {
            Object o = from.apply(input);
            if (o != null) to.accept(output, o);
            return output;
        };
    }

    /**
     * Method used to create a new instance of the output class with any object needed in constructor declared previously
     * by using {@link Mapperz#declareInConstructor(Function)} method.
     * @param objects Constructor object (Must be in the right order)
     * @return a new instance of Output class
     */
    private O instanciate(Object ... objects) {
        Class<?>[] params = Arrays.stream(objects).parallel().map(Object::getClass).toArray(Class[]::new);
        try {
            return outClass.getDeclaredConstructor(params).newInstance(objects);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Be sure to provide arguments in constructor in the right order when you use declareInConstructor() " +
                            "method or when you have an output object with no default constructor.", e
            );
        }
    }

    private static PropertyDescriptor getPropertyDescriptorOfField(List<PropertyDescriptor> attributesDesc, String fieldName) {
        return attributesDesc.stream()
                .filter(attribute -> attribute.getName().equals(fieldName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("No property descriptor found for field %s", fieldName)));
    }

    private static boolean isFieldExistInTargetClass(Class<?> targetClazz, String fieldName) {
        try {
            targetClazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            return false;
        }
        return true;
    }
}

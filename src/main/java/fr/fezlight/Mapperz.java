package fr.fezlight;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.*;

/**
 * Class used to map from any object to another by using lambda expression
 * and clear syntax to be easy to use.
 *
 * @param <I> Input class generic type
 * @param <O> Output class generic type
 * @author FezLight
 * @version 1.0
 */
@Slf4j
public final class Mapperz<I, O> {
    private final Map<Function<I, Object>, BiConsumer<O, Object>> mappings = new HashMap<>();
    private final List<Function<I, Object>> listArgsConstructor = new ArrayList<>();
    private Class<I> inClass;
    private Class<O> outClass;

    private Mapperz(Class<I> inClass, Class<O> outClass) {
        this.inClass = inClass;
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
     * @return a new MapperHelper Object with input and output types ready for mapping
     */
    public static <I, O> Mapperz<I, O> init(Class<I> input, Class<O> output) {
        if(input == null || output == null) {
            throw new IllegalArgumentException("One of the argument provided is null");
        }
        return new Mapperz<>(input, output);
    }

    /**
     * Method used to declare one-field value to be provided into constructor when creating new instance.
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
     * ouput class.
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
     * ouput class.
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
     * Method used to start mapping between input class and output class using declared items previously provided.
     * If any of the input class field is not declared into mappings, using {@link Mapperz#declare(Function, BiConsumer)},
     * it will not be mapped to output class.
     * @param input Input class instance
     * @return Output class instance with all value declared mapped from input class.
     */
    public O map(I input) {
        return map(input, this::instanciate);
    }

    /**
     * Method used to start mapping between input class and output class using declared items previously provided.
     * If any of the input class field is not declared into mappings, using {@link Mapperz#declare(Function, BiConsumer)},
     * it will not be mapped to output class.
     * @param input Input class instance
     * @param output Output class if you want to provide (be careful if you use <br>
     * {@link Mapperz#declareInConstructor(Function)}, you cannot use this field to override. Instead use {@link Mapperz#map(Object)}
     * @return Output class instance with all value declared mapped from input class.
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
            to.accept(output, from.apply(input));
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
            log.error("Error when trying to instanciate output class", e);
            throw new IllegalArgumentException(
                    "Be sure to provide arguments in constructor in the right order when you use declareInConstructor() " +
                            "method or when you have an output object with no default constructor."
            );
        }
    }
}

/*
 *  Copyright 2023 Curity AB
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.curity.identityserver.plugins.utils;

import com.google.common.base.Throwables;
import org.apache.commons.lang3.StringUtils;
import se.curity.identityserver.sdk.NullableFunction;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class NullUtils
{

    private NullUtils()
    {
    }

    /**
     * Apply a map operation on the given value if it is present, returning the value given by the transformation,
     * otherwise return null.
     *
     * @param value     value to be transformed
     * @param transform the map operation
     * @param <T>       type of value to be transformed
     * @param <R>       type of result if any
     * @return result of applying the transform operation on the given value if it is present, null otherwise.
     */
    @Nullable
    public static <T, R> R map(@Nullable T value, NullableFunction<T, R> transform)
    {
        if (value == null)
        {
            return null;
        }

        return transform.apply(value);
    }

    /**
     * Consume the given value if it is present.
     * <p>
     * This is useful when consuming volatile (or just non-final) variables in multi-threaded environments, because
     * just checking for null in such circumstances would be unsafe, so a temporary, final variable would be required.
     * This method makes that easier.
     *
     * @param value    value that might be consumed if non null
     * @param useValue use action to run if value is non null
     * @param <T>      type of value to be consumed
     */
    public static <T> void ifNotNull(@Nullable T value, Consumer<T> useValue)
    {
        if (value != null)
        {
            useValue.accept(value);
        }
    }

    /**
     * Apply a map operation on the given value if it is present, returning the value given by the transformation,
     * otherwise return the value supplied by the defaultValueSupplier.
     *
     * @param value                value to be transformed
     * @param transform            the map operation
     * @param defaultValueSupplier supplier of a default value. Only called if the given value was null.
     * @param <T>                  type of value to be transformed
     * @param <R>                  type of result
     * @return result of applying the transform operation on the given value if it is present, or the default value
     * supplied by defaultValueSupplier otherwise.
     */
    public static <T, R> R map(@Nullable T value, Function<T, R> transform, Supplier<R> defaultValueSupplier)
    {
        if (value == null)
        {
            return defaultValueSupplier.get();
        }
        else
        {
            return transform.apply(value);
        }
    }

    /**
     * Apply a map operation on the given value if it is present, returning the value given by the transformation,
     * otherwise throw the exception supplied by the exceptionSupplier.
     * <p>
     * If the passed <code>transform</code> returns <code>null</code>, <code>exceptionSupplier</code> is not called
     * and <code>null</code> is returned by this function.
     *
     * @param value             value to be transformed
     * @param transform         the map operation
     * @param exceptionSupplier supplier of the exception to throw. Only called if the given value was null.
     * @param <T>               type of value to be transformed
     * @param <R>               type of result
     * @return result of applying the transform operation on the given value if it is present.
     */
    @Nullable
    public static <T, R, E extends Throwable> R mapOrError(@Nullable T value, NullableFunction<T, R> transform,
                                                           Supplier<E> exceptionSupplier) throws E
    {
        if (value == null)
        {
            throw exceptionSupplier.get();
        }
        else
        {
            return transform.apply(value);
        }
    }

    /**
     * @param value        nullable value
     * @param defaultValue default value
     * @param <T>          type of value
     * @return value if it is non-null, defaultValue otherwise.
     */
    public static <T> T valueElse(@Nullable T value, T defaultValue)
    {
        if (value != null)
        {
            return value;
        }
        else
        {
            return defaultValue;
        }
    }

    /**
     * Returns the given value if it is non-null or uses the supplier to create one
     *
     * @param value                a possibly null value that should be used if it is initialized
     * @param defaultValueSupplier a supplier that will return a default value if <tt>value</tt> is null
     * @param <T>                  the type of <tt>value</tt>
     * @return a non-null object
     */
    public static <T> T valueElse(@Nullable T value, Supplier<T> defaultValueSupplier)
    {
        if (value != null)
        {
            return value;
        }
        else
        {
            return defaultValueSupplier.get();
        }
    }

    /**
     * @param type         type of the Object
     * @param object       possibly instance of T
     * @param defaultValue default value
     * @param <T>          type of the desired Object
     * @return object cast to T if possible, defaultValue otherwise
     */
    public static <T> T valueOfType(Class<T> type, @Nullable Object object, T defaultValue)
    {
        if (object != null && type.isInstance(object))
        {
            return type.cast(object);
        }
        return defaultValue;
    }

    /**
     * @param type   type of the Object
     * @param object possibly instance of T
     * @param <T>    type of the desired Object
     * @return object cast to T if possible, null otherwise
     */
    @Nullable
    public static <T> T optionalValueOfType(Class<T> type, @Nullable Object object)
    {
        if (type.isInstance(object))
        {
            return type.cast(object);
        }
        else
        {
            return null;
        }
    }

    /**
     * @param type   type of the Object
     * @param object possibly instance of T
     * @param error  Error message to throw if object is not of type
     * @param <T>    type of the desired Object
     * @return object cast to T
     * @throws IllegalArgumentException if object is not of type T or null
     */
    public static <T> T valueOfTypeOrError(Class<T> type, @Nullable Object object, String error)
    {
        @Nullable T value = optionalValueOfType(type, object);
        if (value == null)
        {
            throw new IllegalArgumentException(error);
        }
        return value;
    }

    /**
     * @param type type of the Object
     * @param map  the map from where to retrieve the value
     * @param key  the lookup key
     * @param <T>  type of the desired Object
     * @return object cast to T
     * @throws IllegalArgumentException if object is not of type T or null
     */
    public static <T> T valueOfTypeInMapOrError(Class<T> type, Map<String, ?> map, String key)
    {
        @Nullable T value = optionalValueOfType(type, map.get(key));
        if (value == null)
        {
            throw new IllegalArgumentException(key + " is missing on map");
        }
        return value;
    }

    /**
     * @param type   expected type
     * @param object object expected to be of type T or null.
     * @param <T>    type of expected object
     * @return instance of T
     * @throws IllegalArgumentException if the type of the object is unexpected.
     */
    @Nullable
    public static <T> T nullOrOfType(Class<T> type, @Nullable Object object)
    {
        if (object == null)
        {
            return null;
        }

        if (type.isInstance(object))
        {
            return type.cast(object);
        }
        else
        {
            throw new IllegalArgumentException(
                    String.format("Object has unexpected type. Expected %s but got a %s", type, object));
        }
    }

    /**
     * @param type             type of the Object
     * @param value            possibly null value
     * @param errorDescription message of NullPointerException to throw if value is null
     * @param <T>              type of the desired Object
     * @return object cast to T if possible
     * @throws NullPointerException if the given value is null or not assignable to type
     */
    public static <T> T valueOrError(Class<T> type, @Nullable Object value, String errorDescription)
            throws NullPointerException
    {
        return valueOrError(optionalValueOfType(type, value), errorDescription);
    }

    /**
     * @param value            possibly null value
     * @param errorDescription message of NullPointerException to throw if value is null
     * @param <T>              type of value
     * @return the given value if it's not null
     * @throws NullPointerException if the given value is null
     */
    public static <T> T valueOrError(@Nullable T value, String errorDescription)
            throws NullPointerException
    {
        if (value != null)
        {
            return value;
        }
        else
        {
            throw new NullPointerException(errorDescription);
        }
    }

    /**
     * @param type              type of the Object
     * @param value             possibly null value
     * @param exceptionSupplier a supplier of the exception to be thrown when the value is not an instance of type
     * @param <T>               type of the desired Object
     * @return object cast to T if possible
     * @throws E if the given value is null or not assignable to type
     */
    public static <T, E extends Throwable> T valueOrError(Class<T> type, @Nullable Object value,
                                                          Supplier<E> exceptionSupplier) throws E
    {
        @Nullable
        T result = optionalValueOfType(type, value);
        if (result == null)
        {
            throw exceptionSupplier.get();
        }
        else
        {
            return result;
        }
    }

    /**
     * @param value             possibly null value
     * @param exceptionSupplier a supplier of the exception to be thrown when null
     * @param <T>               type of value
     * @return the given value if not null
     * @throws E of given type when null.
     */
    public static <T, E extends Throwable> T valueOrError(@Nullable T value, Supplier<E> exceptionSupplier) throws E
    {
        if (value != null)
        {
            return value;
        }
        else
        {
            throw exceptionSupplier.get();
        }
    }

    /**
     * Returns the long represented by the Object if possible, or 0L otherwise.
     * <p>
     * A valid String will be turned into a long.
     *
     * @param object possibly long (base 10)
     * @return long value
     */
    public static long safeLong(@Nullable Object object)
    {
        return safeLong(object, 0L);
    }

    /**
     * Returns the long represented by the Object if possible, or defaultValue otherwise.
     * <p>
     * A valid String will be turned into a long.
     *
     * @param object       possibly long (base 10)
     * @param defaultValue to return in case the object cannot be converted to a long
     * @return long value
     */
    public static long safeLong(@Nullable Object object, long defaultValue)
    {
        return safeLong(object, defaultValue, 10);
    }

    /**
     * Returns the long represented by the Object if possible, or defaultValue otherwise.
     * <p>
     * A valid String will be turned into a long.
     *
     * @param object               possibly long (base 10)
     * @param defaultValueSupplier supplier of the value to return in case the object cannot be converted to a long
     * @return long value
     */
    public static long safeLong(@Nullable Object object, LongSupplier defaultValueSupplier)
    {
        return safeLong(object, defaultValueSupplier, 10);
    }

    /**
     * Returns the long represented by the Object using the given radix if possible, or defaultValue otherwise.
     * <p>
     * A valid String will be turned into a long.
     *
     * @param object       possibly long (of the specified radix)
     * @param defaultValue to return in case the object cannot be converted to a long
     * @param radix        the radix or base to be used when converting
     * @return long value
     * @see Long#parseLong(String, int)
     */
    public static long safeLong(@Nullable Object object, long defaultValue, int radix)
    {
        long result;

        if (object instanceof Long)
        {
            result = (Long) object;
        }
        else if (object instanceof Number)
        {
            result = ((Number) object).longValue();
        }
        else if (object != null)
        {
            try
            {
                result = Long.parseLong(object.toString(), radix);
            }
            catch (NumberFormatException ignored)
            {
                result = defaultValue;
            }
        }
        else
        {
            result = defaultValue;
        }

        return result;
    }

    /**
     * Returns the long represented by the Object using the given radix if possible, or defaultValue otherwise.
     * <p>
     * A valid String will be turned into a long.
     *
     * @param object               possibly long (of the specified radix)
     * @param defaultValueSupplier supplier of the value to return in case the object cannot be converted to a long
     * @param radix                the radix or base to be used when converting
     * @return long value
     * @see Long#parseLong(String, int)
     */
    public static long safeLong(@Nullable Object object, LongSupplier defaultValueSupplier, int radix)
    {
        long result;

        if (object instanceof Long)
        {
            result = (Long) object;
        }
        else if (object instanceof Number)
        {
            result = ((Number) object).longValue();
        }
        else if (object != null)
        {
            try
            {
                result = Long.parseLong(object.toString(), radix);
            }
            catch (NumberFormatException ignored)
            {
                result = defaultValueSupplier.getAsLong();
            }
        }
        else
        {
            result = defaultValueSupplier.getAsLong();
        }

        return result;
    }

    public static int safeInt(@Nullable Object object, int defaultValue)
    {
        return safeInt(object, defaultValue, 10);
    }

    public static int safeInt(@Nullable Object object, int defaultValue, int radix)
    {
        int result;

        if (object instanceof Integer)
        {
            result = (Integer) object;
        }
        else if (object instanceof Number)
        {
            result = ((Number) object).intValue();
        }
        else if (object != null)
        {
            try
            {
                result = Integer.parseInt(object.toString(), radix);
            }
            catch (NumberFormatException ignored)
            {
                result = defaultValue;
            }
        }
        else
        {
            result = defaultValue;
        }

        return result;
    }

    public static double safeDouble(@Nullable Object object, double defaultValue)
    {
        double result;

        if (object instanceof Double)
        {
            result = (Double) object;
        }
        else if (object instanceof Number)
        {
            result = ((Number) object).doubleValue();
        }
        else if (object != null)
        {
            try
            {
                result = Double.parseDouble(object.toString());
            }
            catch (NumberFormatException ignored)
            {
                result = defaultValue;
            }
        }
        else
        {
            result = defaultValue;
        }

        return result;
    }

    /**
     * Returns the value of the object if it's a non-null Boolean object, tries to parse the value as
     * a boolean value if it is not null, or returns the default value. Note that non-null values are
     * parsed using {@link Boolean#parseBoolean}, so any non-null value will be false unless it is "true"
     * (in any case).
     *
     * <p>
     * Example usage:
     * <pre>
     * {@code
     * safeBoolean(null, false); // Returns false
     * safeBoolean("TrUe", false); // Returns true
     * safeBoolean("", true); // Returns false
     * safeBoolean((Object)Boolean.valueOf(true), false); // Return true
     * }
     * </pre>
     *
     * @param object       the object to check for a boolean value if it's not null
     * @param defaultValue the value to use if the object parameter is null
     * @return the boolean value of object when the object parameter is a Boolean, the parsed value
     * of the object parameter when it is not null, or the default value
     */
    public static boolean safeBoolean(@Nullable Object object, boolean defaultValue)
    {
        boolean result;

        if (object instanceof Boolean)
        {
            result = (Boolean) object;
        }
        else if (object != null)
        {
            result = Boolean.parseBoolean(object.toString());
        }
        else
        {
            result = defaultValue;
        }

        return result;
    }

    /**
     * Get the first element of the array if it is present, or null otherwise.
     *
     * @param elements array elements
     * @param <T>      type of elements
     * @return first element or null
     */
    @Nullable
    public static <T> T firstElementOrNull(T[] elements)
    {
        if (elements.length == 0)
        {
            return null;
        }
        else
        {
            return elements[0];
        }
    }

    public static String rootCauseErrorMessage(@Nullable Throwable e)
    {
        String unknownError = "No additional details";

        if (e == null)
        {
            return unknownError;
        }

        return map(Throwables.getRootCause(e), t ->
        {
            @Nullable String maybeMessage = t.getMessage();

            return StringUtils.isEmpty(maybeMessage) ? unknownError : maybeMessage;
        }, () -> unknownError);
    }

    /**
     * Safe equals makes sure that nullable objects safely can be compared for equality.
     * Note that Objects.equals is not entirely nullsafe despite it's apperence, thus cannot always
     * be used
     *
     * @param one the first object to compare
     * @param two the second object to compare
     * @return true if objects are equal
     */
    public static boolean safeEquals(@Nullable Object one, @Nullable Object two)
    {
        if (one == null && two == null)
        {
            return true;
        }

        if (one == null || two == null)
        {
            return false;
        }

        return one.equals(two);
    }

    /**
     * Given a series of values, return the first one encountered that's non-null, or null if no non-null value found
     *
     * @param values A series of values
     * @param <T>    The type of values provided
     * @return The first non-null value found, or null if no non-null value was found
     */
    @Nullable
    @SafeVarargs
    public static <T> T firstNonNull(T... values)
    {
        return Stream.of(values).filter(Objects::nonNull).findFirst().orElse(null);
    }

    public static <T> void accept(@Nullable T value, @Nullable Consumer<T> useValue)
    {
        if (value != null && useValue != null)
        {
            useValue.accept(value);
        }
    }

    /**
     * Checks an array of objects to see if any of them is not null. Note that if the given array is empty, the
     * result will be false.
     *
     * @param values the array of objects to check for a non-null value
     * @return true if values contains at least one object that is not null; false otherwise
     */
    public static boolean anyNonNull(Object... values)
    {
        return Arrays.stream(values).anyMatch(Objects::nonNull);
    }
}


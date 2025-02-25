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
import se.curity.identityserver.sdk.attribute.Attribute;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class NullUtils
{
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

    public static <T> T valueOrError(Class<T> type, @Nullable Object value, String errorDescription)
            throws NullPointerException
    {
        return valueOrError(optionalValueOfType(type, value), errorDescription);
    }

    public static <T> void ifNotNull(@Nullable T value, Consumer<T> useValue)
    {
        if (value != null)
        {
            useValue.accept(value);
        }
    }

    public static <R> R mapOptionalAttribute(@Nullable Attribute value, Function<Attribute, R> transform, Supplier<R> defaultValueSupplier)
    {
        if (value == null)
        {
            return defaultValueSupplier.get();
        }
        else
        {
            var result = transform.apply(value);
            if (result == null)
            {
                return defaultValueSupplier.get();
            }

            return result;
        }
    }

    @Nullable
    public static <T, R> R map(@Nullable T value, NullableFunction<T, R> transform)
    {
        if (value == null)
        {
            return null;
        }

        return transform.apply(value);
    }

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

    public static <T> T valueOfType(Class<T> type, @Nullable Object object, T defaultValue)
    {
        if (object != null && type.isInstance(object))
        {
            return type.cast(object);
        }
        return defaultValue;
    }

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

    public static <T> T valueOfTypeOrError(Class<T> type, @Nullable Object object, String error)
    {
        @Nullable T value = optionalValueOfType(type, object);
        if (value == null)
        {
            throw new IllegalArgumentException(error);
        }
        return value;
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
}


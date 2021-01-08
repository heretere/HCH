/*
 * MIT License
 *
 * Copyright (c) 2021 Justin Heflin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * @author imaginedevMC
 */

package com.heretere.hch.processor.yaml.typehandler;

import com.google.common.collect.Lists;
import com.heretere.hch.processor.MultiSerializer;
import com.heretere.hch.processor.exception.InvalidTypeException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

@SuppressWarnings({"rawtypes", "unchecked"}) //We specifically want to handle all enum types for this class
public final class YamlRawEnumSerializer implements MultiSerializer<YamlConfiguration, Enum> {
    @Override public @NotNull Enum deserialize(
        final @NotNull YamlConfiguration parser,
        final @NotNull Class<?> exactType,
        final @NotNull String key
    ) throws InvalidTypeException {
        if (!exactType.isEnum()) {
            throw new InvalidTypeException();
        }

        if (!parser.isString(key)) {
            throw new InvalidTypeException();
        }

        String output = parser.getString(key);

        if (output == null) {
            throw new InvalidTypeException();
        }

        return Enum.valueOf((Class<Enum>) exactType, output.toUpperCase(Locale.ROOT));
    }

    @Override public @NotNull Enum deserializeRaw(
        final @NotNull Class<?> exactType,
        final @NotNull Object value
    ) {
        return Enum.valueOf((Class<Enum>) exactType, value.toString().toUpperCase(Locale.ROOT));
    }

    @Override public @NotNull List<String> serialize(final @NotNull Object value) {
        return Lists.newArrayList('"' + this.getGenericType().cast(value).name() + '"');
    }

    @Override public @NotNull Class<Enum> getGenericType() {
        return Enum.class;
    }
}

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
 */

package com.heretere.hch.structure.backend;

import com.heretere.hch.generics.TypeDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * The abstraction for a config field. Used to represent a field with a value in a config value.
 *
 * @param <T> The type of the config field.
 */
public interface ConfigField<T> extends ConfigPath, TypeDefinition<T> {
    /**
     * @return The value of the config field.
     */
    @NotNull Optional<T> getValue() throws IllegalAccessException;

    /**
     * @param value The value to set the config field to.
     */
    void setValue(@NotNull T value) throws IllegalAccessException;

    /**
     * @param value Sets the value when you don't have access to the type.
     */
    void setValueRaw(@NotNull Object value) throws IllegalAccessException;
}
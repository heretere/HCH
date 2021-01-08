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

package com.heretere.hch.processor.yaml;

import com.google.common.collect.Lists;
import com.heretere.hch.collection.ConfigList;
import com.heretere.hch.processor.Processor;
import com.heretere.hch.processor.exception.InvalidTypeException;
import com.heretere.hch.processor.yaml.typehandler.YamlBooleanSerializer;
import com.heretere.hch.processor.yaml.typehandler.YamlCharacterSerializer;
import com.heretere.hch.processor.yaml.typehandler.YamlDoubleSerializer;
import com.heretere.hch.processor.yaml.typehandler.YamlFloatSerializer;
import com.heretere.hch.processor.yaml.typehandler.YamlIntegerSerializer;
import com.heretere.hch.processor.yaml.typehandler.YamlRawCollectionSerializer;
import com.heretere.hch.processor.yaml.typehandler.YamlRawEnumSerializer;
import com.heretere.hch.processor.yaml.typehandler.YamlStringSerializer;
import com.heretere.hch.structure.backend.ConfigField;
import com.heretere.hch.structure.backend.ConfigPath;
import com.heretere.hch.structure.backend.ConfigSection;
import com.heretere.hch.structure.backend.ReflectiveConfigField;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public final class YamlProcessor extends Processor<YamlConfiguration> {
    /**
     * The processing backend for the YamlProcessor.
     */
    private @Nullable YamlConfiguration yaml;

    private boolean loadSuccess = false;

    /**
     * Creates a new YamlProcessor.
     *
     * @param location The location of the config file.
     */
    public YamlProcessor(
        final @NotNull Path location
    ) {
        super(location);
        this.createDefaultHandlers();
    }

    /**
     * Attaches pre made serializers to this processor.
     */
    private void createDefaultHandlers() {
        super.attachTypeHandler(new YamlBooleanSerializer());
        super.attachTypeHandler(new YamlCharacterSerializer());
        super.attachTypeHandler(new YamlDoubleSerializer());
        super.attachTypeHandler(new YamlFloatSerializer());
        super.attachTypeHandler(new YamlIntegerSerializer());
        super.attachTypeHandler(new YamlRawCollectionSerializer());
        super.attachTypeHandler(new YamlRawEnumSerializer());
        super.attachTypeHandler(new YamlStringSerializer());
    }

    private void attachSectionParent(final @NotNull ConfigPath path) {
        String parentPath = StringUtils.substringBeforeLast(path.getKey(), ".");

        if (!parentPath.isEmpty() && !path.getKey().equals(parentPath)) {
            ConfigSection section = new ConfigSection(parentPath);

            if (!super.getEntries().containsKey(parentPath)) {
                super.getEntries().put(parentPath, section);
            }

            this.attachSectionParent(section);
        }
    }

    @Override public void processConfigPath(final @NotNull ConfigPath configPath) throws InvalidTypeException,
        IllegalAccessException {

        if (configPath instanceof ConfigField) {
            this.attachSectionParent(configPath);
            super.getEntries().put(configPath.getKey(), configPath);

            if (this.yaml != null && this.yaml.contains(configPath.getKey())) {
                super.deserializeToField(this.yaml, (ConfigField<?>) configPath);
            }
        } else {
            this.getEntries().put(configPath.getKey(), configPath);
        }
    }

    @Override
    public void load() throws IOException, InvalidTypeException, IllegalAccessException {
        this.createFileIfNotExists();

        this.yaml = YamlConfiguration.loadConfiguration(super.getFileLocation().toFile());

        for (String key : this.yaml.getKeys(true)) {
            if (this.yaml.isConfigurationSection(key)) {
                super.getEntries().put(key, new ConfigSection(key));
            } else {
                ConfigField<?> configField = super.getEntries().containsKey(key)
                    ? (ConfigField<?>) super.getEntries().get(key)
                    : new ReflectiveConfigField<>(
                        key,
                        Lists.newArrayList(),
                        Object.class,
                        null
                    );

                this.deserializeToField(this.yaml, configField);
                super.getEntries().put(key, configField);
            }
        }

        this.loadSuccess = true;
    }

    private void createFileIfNotExists() throws IOException {
        if (!Files.exists(super.getFileLocation())) {
            Files.createDirectories(super.getFileLocation().getParent());
            Files.createFile(super.getFileLocation());
        }
    }

    @Override
    public void save() throws IOException, IllegalAccessException {
        if (!loadSuccess) {
            return;
        }

        List<String> lines = Lists.newArrayList();

        for (ConfigPath configPath : super.getEntries().values()) {
            String indentation = StringUtils.repeat("  ", StringUtils.split(configPath.getKey(), ".").length - 1);

            configPath.getComments().forEach(comment -> lines.add(indentation + "# " + comment));

            if (configPath instanceof ConfigField) {
                AtomicBoolean attached = new AtomicBoolean(false);
                String firstLine = indentation + this.getPathString(configPath.getKey()) + ":";
                Optional<?> value = ((ConfigField<?>) configPath).getValue();

                if (value.isPresent()) {
                    this.serializeToString(value.get()).forEach(line -> {
                        if (!attached.getAndSet(true)) {
                            lines.add(firstLine + (line.length() != 0 ? " " : "") + line);
                        } else {
                            lines.add(indentation + "  - " + line);
                        }
                    });
                } else {
                    lines.add(firstLine + null);
                }
            } else {
                lines.add(indentation + this.getPathString(configPath.getKey()) + ":");
            }
        }

        this.createFileIfNotExists();
        Files.write(super.getFileLocation(), lines, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);

    }

    private List<String> serializeToString(final @NotNull Object object) {
        List<String> output = Lists.newArrayList();

        if (ConfigList.class.isAssignableFrom(object.getClass())) {
            super.getSerializer(((ConfigList<?>) object).getGenericType())
                 .ifPresent(serializer -> ((Collection<?>) object)
                     .forEach(item -> output.addAll(serializer.serialize(item))));
        } else {
            /* If there is a serializer use it */
            super.getSerializer(object.getClass())
                 .ifPresent(serializer -> output.addAll(serializer.serialize(object)));
        }

        /* if no serializer was found we use a generic approach to creating a valid toml string. */
        if (output.isEmpty()) {
            output.add(object.toString());
        }

        if (output.size() > 1) {
            output.add(0, "");
        }

        return output;
    }

    @Override protected String getPathString(final @NotNull String path) {
        String output = StringUtils.substringAfterLast(path, ".");

        if (output.isEmpty()) {
            output = path;
        }

        return output;
    }
}

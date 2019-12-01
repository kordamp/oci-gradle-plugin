/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Andres Almiray.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kordamp.gradle

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.Transformer
import org.gradle.api.file.RegularFile
import org.gradle.api.internal.provider.HasConfigurableValueInternal
import org.gradle.api.internal.provider.OwnerAware
import org.gradle.api.internal.provider.PropertyInternal
import org.gradle.api.internal.provider.ProviderInternal
import org.gradle.api.internal.provider.ScalarSupplier
import org.gradle.api.internal.provider.ValueSanitizer
import org.gradle.api.internal.tasks.TaskDependencyContainer
import org.gradle.api.internal.tasks.TaskDependencyResolveContext
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.internal.DisplayName

import javax.annotation.Nullable
import java.nio.file.Paths

import static org.kordamp.gradle.StringUtils.isBlank
import static org.kordamp.gradle.StringUtils.isNotBlank

/**
 * @author Andres Almiray
 * @since 0.2.0
 */
@CompileStatic
class PropertyUtils {
    static Property<String> stringProperty(String envKey, String propertyKey, Property<String> property) {
        new StringPropertyDecorator(envKey, propertyKey, property)
    }

    static Property<Boolean> booleanProperty(String envKey, String propertyKey, Property<Boolean> property) {
        new BooleanPropertyDecorator(envKey, propertyKey, property)
    }

    static Property<Integer> integerProperty(String envKey, String propertyKey, Property<Integer> property) {
        new IntegerPropertyDecorator(envKey, propertyKey, property)
    }

    static String stringProperty(String envKey, String propertyKey, String alternateValue) {
        String value = System.getenv(envKey)
        if (isBlank(value)) value = System.getProperty(propertyKey)
        return isNotBlank(value) ? value : alternateValue
    }

    static boolean booleanProperty(String envKey, String propertyKey, boolean alternateValue) {
        String value = System.getenv(envKey)
        if (isBlank(value)) value = System.getProperty(propertyKey)
        if (isNotBlank(value)) {
            return Boolean.parseBoolean(value)
        }
        return alternateValue
    }

    static int integerProperty(String envKey, String propertyKey, int alternateValue) {
        String value = System.getenv(envKey)
        if (isBlank(value)) value = System.getProperty(propertyKey)
        if (isNotBlank(value)) {
            return Integer.parseInt(value)
        }
        return alternateValue
    }

    static RegularFile fileProperty(String envKey, String propertyKey, RegularFile alternateValue, Project project) {
        String value = System.getenv(envKey)
        if (isBlank(value)) value = System.getProperty(propertyKey)
        if (isNotBlank(value)) {
            return project.objects.fileProperty().set(Paths.get(value).toFile())
        }
        return alternateValue
    }

    private static class ProviderDecorator<T> implements Provider<T>,
        TaskDependencyContainer,
        HasConfigurableValueInternal,
        ProviderInternal<T> {
        private final Provider<T> delegate

        ProviderDecorator(Provider<T> delegate) {
            this.delegate = delegate
        }

        protected Provider<T> getDelegate() {
            return this.@delegate
        }

        @Override
        T get() {
            return delegate.get()
        }

        @Override
        T getOrNull() {
            return delegate.getOrNull()
        }

        @Override
        T getOrElse(T t) {
            return delegate.getOrElse(t)
        }

        @Override
        <S> ProviderInternal<S> map(Transformer<? extends S, ? super T> transformer) {
            return (ProviderInternal<S>) delegate.map(transformer)
        }

        @Override
        <S> Provider<S> flatMap(Transformer<? extends Provider<? extends S>, ? super T> transformer) {
            return delegate.flatMap(transformer)
        }

        @Override
        boolean isPresent() {
            return delegate.isPresent()
        }

        @Override
        Provider<T> orElse(T t) {
            delegate.orElse(t)
            return this
        }

        @Override
        Provider<T> orElse(Provider<? extends T> provider) {
            delegate.orElse(provider)
            return this
        }

        @Override
        void visitDependencies(TaskDependencyResolveContext t) {
            if (delegate instanceof TaskDependencyContainer) {
                ((TaskDependencyContainer) delegate).visitDependencies(t)
            }
        }

        @Override
        void finalizeValue() {
            if (delegate instanceof HasConfigurableValueInternal) {
                ((HasConfigurableValueInternal) delegate).finalizeValue()
            }
        }

        @Override
        void disallowChanges() {
            if (delegate instanceof HasConfigurableValueInternal) {
                ((HasConfigurableValueInternal) delegate).disallowChanges()
            }
        }

        @Override
        void implicitFinalizeValue() {
            if (delegate instanceof HasConfigurableValueInternal) {
                ((HasConfigurableValueInternal) delegate).implicitFinalizeValue()
            }
        }

        @Override
        Class getType() {
            if (delegate instanceof ProviderInternal) {
                return ((ProviderInternal) delegate).getType()
            }
            return null
        }

        @Override
        boolean isValueProducedByTask() {
            if (delegate instanceof ProviderInternal) {
                return ((ProviderInternal) delegate).isValueProducedByTask()
            }
            return false
        }

        @Override
        boolean isContentProducedByTask() {
            if (delegate instanceof ProviderInternal) {
                return ((ProviderInternal) delegate).isContentProducedByTask()
            }
            return false
        }

        @Override
        boolean maybeVisitBuildDependencies(TaskDependencyResolveContext t) {
            if (delegate instanceof ProviderInternal) {
                return ((ProviderInternal) delegate).maybeVisitBuildDependencies(t)
            }
            return false
        }

        @Override
        ScalarSupplier asSupplier(DisplayName displayName, Class aClass, ValueSanitizer valueSanitizer) {
            if (delegate instanceof ProviderInternal) {
                return ((ProviderInternal) delegate).asSupplier(displayName, aClass, valueSanitizer)
            }
            return null
        }
    }

    private static class PropertyDecorator<T> implements Property<T>,
        TaskDependencyContainer,
        HasConfigurableValueInternal,
        PropertyInternal<T> {
        private final Property<T> delegate

        PropertyDecorator(Property<T> delegate) {
            this.delegate = delegate
        }

        protected Property<T> getDelegate() {
            return this.@delegate
        }

        @Override
        void set(@Nullable T t) {
            delegate.set(t)
        }

        @Override
        void set(Provider<? extends T> provider) {
            delegate.set(provider)
        }

        @Override
        Property<T> value(@Nullable T t) {
            delegate.value(t)
            return this
        }

        @Override
        Property<T> value(Provider<? extends T> provider) {
            delegate.value(provider)
            return this
        }

        @Override
        Property<T> convention(T t) {
            delegate.convention(t)
            return this
        }

        @Override
        Property<T> convention(Provider<? extends T> provider) {
            delegate.convention(provider)
            return this
        }

        @Override
        void finalizeValue() {
            delegate.finalizeValue()
        }

        @Override
        void disallowChanges() {
            delegate.disallowChanges()
        }

        @Override
        T get() {
            return delegate.get()
        }

        @Override
        T getOrNull() {
            return delegate.getOrNull()
        }

        @Override
        T getOrElse(T t) {
            return delegate.getOrElse(t)
        }

        @Override
        <S> ProviderInternal<S> map(Transformer<? extends S, ? super T> transformer) {
            return (ProviderInternal<S>) delegate.map(transformer)
        }

        @Override
        <S> Provider<S> flatMap(Transformer<? extends Provider<? extends S>, ? super T> transformer) {
            return delegate.flatMap(transformer)
        }

        @Override
        boolean isPresent() {
            return delegate.isPresent()
        }

        @Override
        Provider<T> orElse(T t) {
            return delegate.orElse(t)
        }

        @Override
        Provider<T> orElse(Provider<? extends T> provider) {
            return delegate.orElse(provider)
        }

        @Override
        void visitDependencies(TaskDependencyResolveContext t) {
            if (delegate instanceof TaskDependencyContainer) {
                ((TaskDependencyContainer) delegate).visitDependencies(t)
            }
        }

        @Override
        void implicitFinalizeValue() {
            if (delegate instanceof HasConfigurableValueInternal) {
                ((HasConfigurableValueInternal) delegate).implicitFinalizeValue()
            }
        }

        @Override
        void setFromAnyValue(Object o) {
            if (delegate instanceof PropertyInternal) {
                ((PropertyInternal) delegate).setFromAnyValue(o)
            }
        }

        @Override
        void attachProducer(Task task) {
            if (delegate instanceof PropertyInternal) {
                ((PropertyInternal) delegate).attachProducer(task)
            }
        }

        @Override
        void attachDisplayName(DisplayName displayName) {
            if (delegate instanceof OwnerAware) {
                ((OwnerAware) delegate).attachDisplayName(displayName)
            }
        }

        @Override
        Class getType() {
            if (delegate instanceof ProviderInternal) {
                return ((ProviderInternal) delegate).getType()
            }
            return null
        }

        @Override
        boolean isValueProducedByTask() {
            if (delegate instanceof ProviderInternal) {
                return ((ProviderInternal) delegate).isValueProducedByTask()
            }
            return false
        }

        @Override
        boolean isContentProducedByTask() {
            if (delegate instanceof ProviderInternal) {
                return ((ProviderInternal) delegate).isContentProducedByTask()
            }
            return false
        }

        @Override
        boolean maybeVisitBuildDependencies(TaskDependencyResolveContext t) {
            if (delegate instanceof ProviderInternal) {
                return ((ProviderInternal) delegate).maybeVisitBuildDependencies(t)
            }
            return false
        }

        @Override
        ScalarSupplier asSupplier(DisplayName displayName, Class aClass, ValueSanitizer valueSanitizer) {
            if (delegate instanceof ProviderInternal) {
                return ((ProviderInternal) delegate).asSupplier(displayName, aClass, valueSanitizer)
            }
            return null
        }
    }

    private static abstract class EnvironmentAwareProviderDecorator<T> extends ProviderDecorator<T> {
        private final String envKey
        private final String propertyKey

        EnvironmentAwareProviderDecorator(String envKey, String propertyKey, Provider<T> delegate) {
            super(delegate)
            this.envKey = envKey
            this.propertyKey = propertyKey
        }

        @Override
        T get() {
            T result = resolveValue()
            return null != result ? result : delegate.get()
        }

        @Override
        T getOrNull() {
            T result = resolveValue()
            return null != result ? result : delegate.getOrNull()
        }

        @Override
        T getOrElse(T t) {
            T result = resolveValue()
            return null != result ? result : delegate.getOrElse(t)
        }

        @Override
        boolean isPresent() {
            return null != resolveValue() || delegate.isPresent()
        }

        protected abstract T resolveValue()
    }

    private static class BooleanProviderDecorator extends EnvironmentAwareProviderDecorator<Boolean> {
        BooleanProviderDecorator(String envKey, String propertyKey, Provider<Boolean> delegate) {
            super(envKey, propertyKey, delegate)
        }

        protected Boolean resolveValue() {
            String value = System.getenv(envKey)
            if (isBlank(value)) value = System.getProperty(propertyKey)
            if (isNotBlank(value)) {
                return Boolean.parseBoolean(value)
            }
            return null
        }
    }

    private static class StringProviderDecorator extends EnvironmentAwareProviderDecorator<String> {
        StringProviderDecorator(String envKey, String propertyKey, Provider<String> delegate) {
            super(envKey, propertyKey, delegate)
        }

        protected String resolveValue() {
            String value = System.getenv(envKey)
            if (isBlank(value)) value = System.getProperty(propertyKey)
            return isNotBlank(value) ? value : null
        }
    }

    private static class IntegerProviderDecorator extends EnvironmentAwareProviderDecorator<Integer> {
        IntegerProviderDecorator(String envKey, String propertyKey, Provider<Integer> delegate) {
            super(envKey, propertyKey, delegate)
        }

        protected Integer resolveValue() {
            String value = System.getenv(envKey)
            if (isBlank(value)) value = System.getProperty(propertyKey)
            if (isNotBlank(value)) {
                return Integer.parseInt(value)
            }
            return null
        }
    }

    private static abstract class EnvironmentAwarePropertyDecorator<T> extends PropertyDecorator<T> {
        final String envKey
        final String propertyKey

        EnvironmentAwarePropertyDecorator(String envKey, String propertyKey, Property<T> delegate) {
            super(delegate)
            this.envKey = envKey
            this.propertyKey = propertyKey
        }

        @Override
        T get() {
            T result = resolveValue()
            return null != result ? result : delegate.get()
        }

        @Override
        T getOrNull() {
            T result = resolveValue()
            return null != result ? result : delegate.getOrNull()
        }

        @Override
        T getOrElse(T t) {
            T result = resolveValue()
            return null != result ? result : delegate.getOrElse(t)
        }

        @Override
        boolean isPresent() {
            return null != resolveValue() || delegate.isPresent()
        }

        protected abstract T resolveValue()
    }

    private static class BooleanPropertyDecorator extends EnvironmentAwarePropertyDecorator<Boolean> {
        BooleanPropertyDecorator(String envKey, String propertyKey, Property<Boolean> delegate) {
            super(envKey, propertyKey, delegate)
        }

        @Override
        Provider<Boolean> orElse(Boolean t) {
            new BooleanProviderDecorator(envKey, propertyKey, delegate.orElse(t))
        }

        @Override
        Provider<Boolean> orElse(Provider<? extends Boolean> provider) {
            new BooleanProviderDecorator(envKey, propertyKey, delegate.orElse(provider))
        }

        protected Boolean resolveValue() {
            String value = System.getenv(envKey)
            if (isBlank(value)) value = System.getProperty(propertyKey)
            if (isNotBlank(value)) {
                return Boolean.parseBoolean(value)
            }
            return null
        }
    }

    private static class StringPropertyDecorator extends EnvironmentAwarePropertyDecorator<String> {
        StringPropertyDecorator(String envKey, String propertyKey, Property<String> delegate) {
            super(envKey, propertyKey, delegate)
        }

        @Override
        Provider<String> orElse(String t) {
            new StringProviderDecorator(envKey, propertyKey, delegate.orElse(t))
        }

        @Override
        Provider<String> orElse(Provider<? extends String> provider) {
            new StringProviderDecorator(envKey, propertyKey, delegate.orElse(provider))
        }

        protected String resolveValue() {
            String value = System.getenv(envKey)
            if (isBlank(value)) value = System.getProperty(propertyKey)
            return isNotBlank(value) ? value : null
        }
    }

    private static class IntegerPropertyDecorator extends EnvironmentAwarePropertyDecorator<Integer> {
        IntegerPropertyDecorator(String envKey, String propertyKey, Property<Integer> delegate) {
            super(envKey, propertyKey, delegate)
        }

        @Override
        Provider<Integer> orElse(Integer t) {
            new IntegerProviderDecorator(envKey, propertyKey, delegate.orElse(t))
        }

        @Override
        Provider<Integer> orElse(Provider<? extends Integer> provider) {
            new IntegerProviderDecorator(envKey, propertyKey, delegate.orElse(provider))
        }

        protected Integer resolveValue() {
            String value = System.getenv(envKey)
            if (isBlank(value)) value = System.getProperty(propertyKey)
            if (isNotBlank(value)) {
                return Integer.parseInt(value)
            }
            return null
        }
    }
}

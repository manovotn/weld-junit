/*
 * JBoss, Home of Professional Open Source
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.weld.junit5.auto;

import org.jboss.weld.environment.se.Weld;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.util.AnnotationUtils;
import org.junit.platform.commons.util.CollectionUtils;
import org.junit.platform.commons.util.Preconditions;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.NormalScope;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.spi.Extension;
import javax.inject.Inject;
import javax.inject.Qualifier;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static org.junit.platform.commons.support.AnnotationSupport.isAnnotated;


/**
 * Provides <b>automagic</b> bean class discovery for a test class instance.
 */
public class ClassScanning {

    public static void scanForRequiredBeanClass(Class<?> testClass, Weld weld, boolean explicitInjection) {

        List<Class<?>> classesToProcess = new ArrayList<>();
        classesToProcess.add(testClass);

        Set<Class<?>> foundClasses = new HashSet<>();
        Set<Class<?>> alternatives = new HashSet<>();

        while (!classesToProcess.isEmpty()) {

            Class<?> currClass = classesToProcess.remove(0);

            if (foundClasses.contains(currClass) || currClass.isPrimitive() || currClass.isSynthetic() || currClass.getName().startsWith("java") || currClass.getName().startsWith("sun")) {
                continue;
            }

            foundClasses.add(currClass);

            findAnnotatedFields(currClass, Object.class, OverrideBean.class).stream()
                    .map(Field::getType)
                    .forEach(alternatives::add);

            AnnotationSupport.findAnnotatedMethods(currClass, OverrideBean.class, HierarchyTraversalMode.BOTTOM_UP).stream()
                    .map(Method::getReturnType)
                    .forEach(alternatives::add);

            findAnnotatedFields(currClass, Object.class, Inject.class).stream()
                    .map(Field::getType)
                    .forEach(cls -> addClassesToProcess(classesToProcess, cls));

            AnnotationSupport.findAnnotatedMethods(currClass, Inject.class, HierarchyTraversalMode.BOTTOM_UP).stream()
                    .map(Method::getReturnType)
                    .forEach(cls -> addClassesToProcess(classesToProcess, cls));

            findFirstAnnotatedConstructor(currClass, Inject.class)
                    .map(Stream::of).orElseGet(Stream::empty)
                    .flatMap(cons -> getExecutableParameterTypes(cons, weld, explicitInjection).stream())
                    .forEach(cls -> addClassesToProcess(classesToProcess, cls));

            findAnnotatedFields(currClass, Object.class, Produces.class).stream()
                    .map(Field::getType)
                    .forEach(cls -> addClassesToProcess(classesToProcess, cls));

            AnnotationSupport.findAnnotatedMethods(currClass, Produces.class, HierarchyTraversalMode.BOTTOM_UP).stream()
                    .map(Method::getReturnType)
                    .forEach(cls -> addClassesToProcess(classesToProcess, cls));

            AnnotationSupport.findAnnotatedMethods(currClass, Test.class, HierarchyTraversalMode.BOTTOM_UP).stream()
                    .flatMap(method -> getExecutableParameterTypes(method, weld, explicitInjection).stream())
                    .forEach(cls -> addClassesToProcess(classesToProcess, cls));

            AnnotationSupport.findAnnotatedMethods(currClass, RepeatedTest.class, HierarchyTraversalMode.BOTTOM_UP).stream()
                    .flatMap(method -> getExecutableParameterTypes(method, weld, explicitInjection).stream())
                    .forEach(cls -> addClassesToProcess(classesToProcess, cls));

            AnnotationSupport.findAnnotatedMethods(currClass, BeforeAll.class, HierarchyTraversalMode.BOTTOM_UP).stream()
                    .flatMap(method -> getExecutableParameterTypes(method, weld, explicitInjection).stream())
                    .forEach(cls -> addClassesToProcess(classesToProcess, cls));

            AnnotationSupport.findAnnotatedMethods(currClass, BeforeEach.class, HierarchyTraversalMode.BOTTOM_UP).stream()
                    .flatMap(method -> getExecutableParameterTypes(method, weld, explicitInjection).stream())
                    .forEach(cls -> addClassesToProcess(classesToProcess, cls));

            AnnotationSupport.findAnnotatedMethods(currClass, AfterEach.class, HierarchyTraversalMode.BOTTOM_UP).stream()
                    .flatMap(method -> getExecutableParameterTypes(method, weld, explicitInjection).stream())
                    .forEach(cls -> addClassesToProcess(classesToProcess, cls));

            AnnotationSupport.findAnnotatedMethods(currClass, AfterAll.class, HierarchyTraversalMode.BOTTOM_UP).stream()
                    .flatMap(method -> getExecutableParameterTypes(method, weld, explicitInjection).stream())
                    .forEach(cls -> addClassesToProcess(classesToProcess, cls));

            AnnotationSupport.findRepeatableAnnotations(currClass, AddPackages.class)
                    .forEach(ann ->
                            stream(ann.value())
                                    .distinct()
                                    .forEach(cls -> weld.addPackage(ann.recursively(), cls)));

            AnnotationSupport.findRepeatableAnnotations(currClass, AddBeanClasses.class).stream()
                    .flatMap(ann -> stream(ann.value()))
                    .distinct()
                    .forEach(weld::addBeanClass);

            AnnotationSupport.findRepeatableAnnotations(currClass, AddExtensions.class).stream()
                    .flatMap(ann -> stream(ann.value()))
                    .distinct()
                    .map(ClassScanning::createExtension)
                    .forEach(weld::addExtension);

            AnnotationSupport.findRepeatableAnnotations(currClass, AddEnabledInterceptors.class).stream()
                    .flatMap(ann -> stream(ann.value()))
                    .distinct()
                    .forEach(interceptor -> {
                        weld.addInterceptor(interceptor);
                        weld.addBeanClass(interceptor);
                    });

            AnnotationSupport.findRepeatableAnnotations(currClass, AddEnabledDecorators.class).stream()
                    .flatMap(ann -> stream(ann.value()))
                    .distinct()
                    .forEach(decorator -> {
                        weld.addDecorator(decorator);
                        weld.addBeanClass(decorator);
                    });

            AnnotationSupport.findRepeatableAnnotations(currClass, EnableAlternatives.class).stream()
                    .flatMap(ann -> stream(ann.value()))
                    .distinct()
                    .forEach(weld::addAlternative);

            AnnotationSupport.findRepeatableAnnotations(currClass, EnableAlternativeStereotypes.class).stream()
                    .flatMap(ann -> stream(ann.value()))
                    .distinct()
                    .forEach(weld::addAlternativeStereotype);

        }

        foundClasses.removeAll(alternatives);
        foundClasses.add(testClass);

        for (Class<?> foundClass : foundClasses) {

            if (hasBeanDefiningAnnotation(foundClass)) {
                weld.addBeanClass(foundClass);
            }

        }

    }

    private static void addClassesToProcess(Collection<Class<?>> classesToProcess, Type type) {

        if (type instanceof Class) {

            classesToProcess.add((Class<?>) type);
        } else if (type instanceof ParameterizedType) {

            ParameterizedType ptype = (ParameterizedType) type;

            classesToProcess.add((Class<?>) ptype.getRawType());

            for (Type arg : ptype.getActualTypeArguments()) {
                addClassesToProcess(classesToProcess, arg);
            }

        }

    }

    private static List<Class<?>> getExecutableParameterTypes(Executable executable, Weld weld, boolean explicitInjection) {

        List<Class<?>> types = new ArrayList<>();

        if (explicitInjection) {
            Annotation[][] paramAnns = executable.getParameterAnnotations();
            Class<?>[] paramTypes = executable.getParameterTypes();
            for (int c = 0; c < paramAnns.length; ++c) {
                if (stream(paramAnns[c]).anyMatch(ann -> isAnnotated(ann.annotationType(), Qualifier.class) || isAnnotated(ann.annotationType(), NormalScope.class))) {
                    weld.addBeanClass(paramTypes[c]);
                    types.add(paramTypes[c]);
                }
            }
        } else {
            for (Class<?> paramType : executable.getParameterTypes()) {
                weld.addBeanClass(paramType);
                types.add(paramType);
            }
        }

        return types;
    }

    private static Extension createExtension(Class<? extends Extension> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean hasBeanDefiningAnnotation(Class<?> clazz) {
        return
                AnnotationUtils.isAnnotated(clazz, ApplicationScoped.class) ||
                        AnnotationUtils.isAnnotated(clazz, SessionScoped.class) ||
                        AnnotationUtils.isAnnotated(clazz, ConversationScoped.class) ||
                        AnnotationUtils.isAnnotated(clazz, RequestScoped.class) ||
                        AnnotationUtils.isAnnotated(clazz, Dependent.class) ||
                        AnnotationUtils.isAnnotated(clazz, Stereotype.class);
    }

    private static List<Field> findAllFieldsInHierarchy(Class<?> clazz) {
        Preconditions.notNull(clazz, "Class must not be null");
        List<Field> localFields = getDeclaredFields(clazz).stream().filter((field) -> {
            return !field.isSynthetic();
        }).collect(Collectors.toList());
        List<Field> superclassFields = getSuperclassFields(clazz).stream().filter((field) -> {
            return !isMethodShadowedByLocalFields(field, localFields);
        }).collect(Collectors.toList());
        List<Field> methods = new ArrayList<>();
        methods.addAll(superclassFields);
        methods.addAll(localFields);

        return methods;
    }

    private static List<Field> getSuperclassFields(Class<?> clazz) {
        Class<?> superclass = clazz.getSuperclass();
        return superclass != null && superclass != Object.class ? findAllFieldsInHierarchy(superclass) : Collections.emptyList();
    }

    private static List<Field> getDeclaredFields(Class<?> clazz) {
        return asList(clazz.getDeclaredFields());
    }

    private static boolean isMethodShadowedByLocalFields(Field field, List<Field> localFields) {
        return localFields.stream().anyMatch((local) -> {
            return isFieldShadowedBy(field, local);
        });
    }

    private static boolean isFieldShadowedBy(Field upper, Field lower) {
        return upper.getType().equals(lower.getType());
    }

    public static List<Field> findAnnotatedFields(Class<?> clazz, Class<?> fieldType, Class<? extends Annotation> annotationType) {
        return getDeclaredFields(clazz).stream().filter((field) -> {
            return fieldType.isAssignableFrom(field.getType()) && isAnnotated(field, annotationType);
        }).collect(CollectionUtils.toUnmodifiableList());
    }

    private static List<Constructor<?>> getDeclaredConstructors(Class<?> clazz) {
        return asList(clazz.getDeclaredConstructors());
    }

    public static Optional<Constructor<?>> findFirstAnnotatedConstructor(Class<?> clazz, Class<? extends Annotation> annotationType) {

        Optional<Constructor<?>> found = getDeclaredConstructors(clazz).stream()
                .filter((cons) -> isAnnotated(cons, annotationType))
                .findFirst();

        if (found.isPresent() || clazz.getSuperclass() == null) {
            return found;
        }

        return findFirstAnnotatedConstructor(clazz.getSuperclass(), annotationType);
    }

}

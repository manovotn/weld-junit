package org.jboss.weld.junit5.auto;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code ExcludeBean} excludes a class, or all classes within a hierarchy, that include a bean defining annotation (
 * e.g. scope) from automatic discovery. This can be helpful to allow replacing a bean class with a different
 * implementation; typically a mock.
 *
 * The type of bean to exclude is implied by the annotated field's type or annotated method's return type. If the type
 * is a base class or interface all beans in the hierarchy will be excluded.
 *
 * NOTE: This annotation will only exclude beans defined by class annotations. It will not exclude beans of the
 * implied type that are defined by {@link javax.enterprise.inject.Produces producer methods or fields}. Also, the
 * current implementation excludes beans based on type, disregarding any qualifiers that are specified.
 *
 * Example:
 * <pre>
 * &#64;EnableAutoWeld
 * class TestSomeFoo {
 *
 *   &#64;Inject
 *   SomeFoo someFoo;   // SomeFoo depends upon application scoped bean Foo
 *
 *   &#64;Produces
 *   &#64;ExcludeBean   // Excludes Foo bean class from automatic discovery
 *   Foo mockFoo = mock(Foo.class);  // mockFoo is now produced in place of original Foo impl
 *
 *   &#64;Test
 *   void test(Foo myFoo) {
 *     assertNotNull(myFoo);
 *     assertEquals(myFoo.getBar(), "mock-foo");
 *   }
 * }
 * </pre>
 *
 * @see ExcludeBeanClasses
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@Inherited
public @interface ExcludeBean {
}
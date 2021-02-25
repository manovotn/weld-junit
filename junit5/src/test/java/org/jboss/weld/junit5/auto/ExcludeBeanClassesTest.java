package org.jboss.weld.junit5.auto;

import org.jboss.weld.junit5.basic.SomeFoo;
import org.jboss.weld.junit5.basic.Foo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.enterprise.inject.Produces;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@EnableAutoWeld
@AddBeanClasses(SomeFoo.class)
@ExcludeBeanClasses(Foo.class)
class ExcludeBeanClassesTest {

    @Produces
    static Foo fakeFoo = new Foo("non-baz");

    @Test
    @DisplayName("Ensure @ExcludeBeanClasses excludes the specified classes")
    void test(Foo myFoo) {
        assertNotNull(myFoo);
        assertEquals(myFoo.getBar(), "non-baz");
    }

}

package org.jboss.weld.junit5.auto;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;

@EnableAutoWeld
public class InjectParameterizedInstanceTest {

    @Dependent
    static class Foo<T> {
    }

    @Inject
    Instance<Foo<String>> fooInstance;

    @Test
    void test() {
        assertNotNull(fooInstance.get());
    }

}

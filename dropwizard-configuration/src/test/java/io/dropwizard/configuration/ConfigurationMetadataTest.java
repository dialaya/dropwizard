package io.dropwizard.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.dropwizard.jackson.Jackson;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ConfigurationMetadataTest {

    @SuppressWarnings("UnusedDeclaration")
    public static class ExampleConfiguration {

        @JsonProperty
        private int port = 8000;

        @JsonProperty
        private ExampleInterface example = new DefaultExampleInterface();

        @JsonProperty
        private ExampleInterfaceWithDefaultImpl exampleWithDefault = new DefaultExampleInterface();

        @JsonProperty
        private List<ExampleInterfaceWithDefaultImpl> exampleWithDefaults = new ArrayList<>();

        public int getPort() {
            return port;
        }

        public ExampleInterface getExample() {
            return example;
        }

        public ExampleInterfaceWithDefaultImpl getExampleWithDefault() {
            return exampleWithDefault;
        }

        public List<ExampleInterfaceWithDefaultImpl> getExampleWithDefaults() {
            return exampleWithDefaults;
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    public interface ExampleInterface {

    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = DefaultExampleInterface.class)
    public interface ExampleInterfaceWithDefaultImpl {

    }

    @SuppressWarnings("UnusedDeclaration")
    public static class DefaultExampleInterface implements ExampleInterface,
        ExampleInterfaceWithDefaultImpl {

        @JsonProperty
        private String[] array = new String[]{};

        @JsonProperty
        private List<String> list = Collections.emptyList();

        @JsonProperty
        private Set<String> set = Collections.emptySet();

        public String[] getArray() {
            return array;
        }

        public List<String> getList() {
            return list;
        }

        public Set<String> getSet() {
            return set;
        }
    }

    @ParameterizedTest
    @MethodSource("provideArgsForShouldDiscoverAllFields")
    public void shouldDiscoverAllFields(String name, boolean isPrimitive,
        boolean isCollectionOrArrayType,
        Class<?> klass) {
        final ConfigurationMetadata metadata = new ConfigurationMetadata(
            Jackson.newObjectMapper(), ExampleConfiguration.class);

        assertThat(metadata.fields.get(name)).isNotNull().satisfies((f) -> {
            assertThat(f.isPrimitive()).isEqualTo(isPrimitive);
            assertThat(f.isCollectionLikeType() || f.isArrayType())
                .isEqualTo(isCollectionOrArrayType);

            if (isCollectionOrArrayType) {
                assertThat(f.getContentType().isTypeOrSubTypeOf(klass)).isTrue();
            } else {
                assertThat(f.isTypeOrSubTypeOf(klass)).isTrue();
            }
        });
    }

    private static Stream<Arguments> provideArgsForShouldDiscoverAllFields() {
        return Stream.of(
            Arguments.of("port", true, false, Integer.TYPE),
            Arguments.of("example", false, false, ExampleInterface.class),
            Arguments.of("exampleWithDefault.array", false, true, String.class),
            Arguments.of("exampleWithDefault.list", false, true, String.class),
            Arguments.of("exampleWithDefault.set", false, true, String.class),
            Arguments.of("exampleWithDefaults[*].array", false, true, String.class),
            Arguments.of("exampleWithDefaults[*].list", false, true, String.class),
            Arguments.of("exampleWithDefaults[*].set", false, true, String.class)
        );
    }

    @ParameterizedTest
    @MethodSource("provideArgsForIsCollectionOfStringsShouldWork")
    public void isCollectionOfStringsShouldWork(String name, boolean isCollectionOfStrings) {
        final ConfigurationMetadata metadata = new ConfigurationMetadata(
            Jackson.newObjectMapper(), ExampleConfiguration.class);

        assertThat(metadata.isCollectionOfStrings(name)).isEqualTo(isCollectionOfStrings);
    }


    private static Stream<Arguments> provideArgsForIsCollectionOfStringsShouldWork() {
        return Stream.of(
            Arguments.of("doesnotexist", false),
            Arguments.of("port", false),
            Arguments.of("example.array", false),
            Arguments.of("example.list", false),
            Arguments.of("example.set", false),
            Arguments.of("exampleWithDefault.array", true),
            Arguments.of("exampleWithDefault.list", true),
            Arguments.of("exampleWithDefault.set", true),
            Arguments.of("exampleWithDefaults[0].array", true),
            Arguments.of("exampleWithDefaults[0].list", true),
            Arguments.of("exampleWithDefaults[0].set", true)
        );
    }
}

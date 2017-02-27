package io.t28.json2java.core.builder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import io.t28.json2java.core.naming.NamePolicy;

import javax.annotation.Nonnull;
import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.stream.Collectors;

public class JacksonClassBuilder extends ClassBuilder {
    public JacksonClassBuilder(@Nonnull NamePolicy fieldNamePolicy,
                               @Nonnull NamePolicy methodNamePolicy,
                               @Nonnull NamePolicy parameterNamePolicy) {
        super(fieldNamePolicy, methodNamePolicy, parameterNamePolicy);
    }

    @Nonnull
    @Override
    protected List<FieldSpec> buildFields() {
        return getProperties().entrySet()
                .stream()
                .map(property -> {
                    final String name = property.getKey();
                    final TypeName type = property.getValue();

                    final String fieldName = fieldNamePolicy.convert(name, type);
                    return FieldSpec.builder(type, fieldName)
                            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Nonnull
    @Override
    protected List<MethodSpec> buildMethods() {
        final MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(JsonCreator.class)
                        .build());

        final ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
        getProperties().entrySet().forEach(property -> {
            final String name = property.getKey();
            final TypeName type = property.getValue();

            final String fieldName = fieldNamePolicy.convert(name, type);
            final String methodName = methodNamePolicy.convert(name, type);
            builder.add(MethodSpec.methodBuilder(methodName)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(type)
                    .addAnnotation(AnnotationSpec.builder(JsonProperty.class)
                            .addMember("value", "$S", name)
                            .build())
                    .addStatement("return $L", fieldName)
                    .build());

            final String propertyName = parameterNamePolicy.convert(name, type);
            constructorBuilder.addParameter(ParameterSpec.builder(type, propertyName)
                    .addAnnotation(AnnotationSpec.builder(JsonProperty.class)
                            .addMember("value", "$S", name)
                            .build())
                    .build())
                    .addStatement("this.$L = $L", fieldName, propertyName);
        });
        builder.add(constructorBuilder.build());
        return builder.build();
    }
}
package com.yourcompany.plugins.rewriteprepare.serializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

/**
 * 문자열을 따옴표 없이 직렬화하는 커스텀 serializer
 * Recipe.attributes, preconditions, exclusions를 제외한 다른 문자열 필드에 사용됩니다.
 * 
 * YAMLGenerator의 writeString 메서드는 기본적으로 따옴표를 최소화하지만,
 * 특정 경우(특수문자 포함 등)에는 따옴표를 추가합니다.
 * 이 serializer는 MINIMIZE_QUOTES 설정과 함께 작동합니다.
 */
public class UnquotedStringSerializer extends StdScalarSerializer<String> {

    private static final long serialVersionUID = 1L;

    public UnquotedStringSerializer() {
        super(String.class);
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        // YAMLGenerator는 MINIMIZE_QUOTES 설정에 따라 자동으로 따옴표를 최소화합니다.
        // writeString을 사용하면 기본 동작을 따릅니다.
        gen.writeString(value);
    }
}


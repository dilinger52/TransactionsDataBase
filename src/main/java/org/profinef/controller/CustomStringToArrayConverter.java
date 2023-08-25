package org.profinef.controller;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CustomStringToArrayConverter implements Converter<String, List<String>> {
    @Override
    public List<String> convert(String source) {
        System.out.println("hear");
        return Arrays.stream(StringUtils.delimitedListToStringArray(source, ";")).toList();
    }
}

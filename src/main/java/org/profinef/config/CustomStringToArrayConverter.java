package org.profinef.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
/**Настройка делимитера для передачи значений из нескольких инпутов в одной переменной (по умолчанию используется ",",
 * что не применимо с дробными числами, еслим для разделения целой и дробной части используется тот же символ).
 * Запись же нескольких значений в одну переменную делает программу более универсальной и позволяет обрабатывать
 * переменное количество контрагентов/записей.
 */
@Configuration
public class CustomStringToArrayConverter implements Converter<String, List<String>> {
    @Override
    public List<String> convert(String source) {
        return Arrays.stream(StringUtils.delimitedListToStringArray(source, ";")).toList();
    }
}

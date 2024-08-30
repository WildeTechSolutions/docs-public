package com.thomaswilde.jacksonfx;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.thomaswilde.fxcore.FXPropertyUtils;

import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class JacksonUtil {

    private static final Logger log = LoggerFactory.getLogger(JacksonUtil.class);

    private static ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new AfterburnerModule())
            .enable(SerializationFeature.INDENT_OUTPUT)
//            .disable(MapperFeature.AUTO_DETECT_FIELDS)
            .configure(MapperFeature.AUTO_DETECT_FIELDS, false)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ;

    public static <T> String toJson(T object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (Exception e) {
            e.printStackTrace();
            log.warn(e.getMessage());
        }
        return null;
    }


    public static <T> T fromJson(String json, Class<T> type) throws IOException {
        return mapper.readValue(json, type);
    }

    public static <T> T fromJson(String json, Class<T> wrapperClass, Class<?> type) throws IOException {
        JavaType javaType = mapper.getTypeFactory().constructParametricType(wrapperClass, type);
        return mapper.readValue(json, javaType);
    }

    public static <T> List<T> fromJsonList(String json, Class<T> type) throws IOException {
        JavaType javaType = mapper.getTypeFactory().constructCollectionType(ArrayList.class, type);
        return mapper.readValue(json, javaType);
    }

    public static List<?> fromJsonListWrapped(String json, Class<?> wrapperClass, Class<?> type) throws IOException {
        JavaType innerType = mapper.getTypeFactory().constructParametricType(wrapperClass, type);
        JavaType listType = mapper.getTypeFactory().constructCollectionType(ArrayList.class, innerType);
        return mapper.readValue(json, listType);
    }

    public static <T> void deepCopy(T copyFrom, T copyTo){
        patchJson(copyTo, toJson(copyFrom));
    }


    public static <T> void patchJson(T bean, String json) {
        try {
            JsonNode jsonNode = mapper.readTree(json);
            setJsonValues(jsonNode, bean);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    private static void setJsonValues(JsonNode jsonNode, Object obj) {
        log.trace("patching jsonNode: {}", jsonNode.textValue());
        Class<?> objType = obj.getClass();
        Iterator<String> fieldNames = jsonNode.fieldNames();
        while(fieldNames.hasNext()){
            String pathToField = fieldNames.next();

            Class<?> fieldType = FXPropertyUtils.getFieldType(objType, pathToField);

            log.trace("Getting jsonNode at pathToField: {}", pathToField);
//            JsonNode fieldValue = jsonNode.get(pathToField);
//            setFieldValue(bean, fieldName, fieldValue);

            if(fieldType == null) continue;

            if (isPrimitive(fieldType)) {
                Object value = convertObjectToTargetType(pathToField, jsonNode, fieldType);

                try {
                    log.debug("setting {} to {}", pathToField, value);
                    PropertyUtils.setProperty(obj, pathToField, value);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }else{
                if(jsonNode.get(pathToField).isObject()){
                    JsonNode subJsonObject = jsonNode.get(pathToField);
                    FXPropertyUtils.ensureInstantiatedNestedPath(obj, pathToField);
                    try {
                        Object subObject = PropertyUtils.getProperty(obj, pathToField);
                        setJsonValues(subJsonObject, subObject);


                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
    }

    private static boolean isPrimitive(Class<?> type) {

        return (type == int.class || type == long.class || type == double.class || type == float.class
                || type == Integer.class || type == Double.class || type == Long.class || type == Boolean.class
                || type == boolean.class || type == byte.class || type == char.class || type == short.class || type == String.class || type == LocalDate.class || type == LocalDateTime.class);

    }

    private static Object convertObjectToTargetType(String key, JsonNode jsonObject, Class<?> fieldType){
        Object value = null;


        if(jsonObject.get(key) == null){
            log.warn("Json object is null, key is {}", key);
            return null;
        }
        log.trace("Converting json object with key {}, value {}, to type {}", key, jsonObject.get(key), fieldType.getName());

        if(String.class.isAssignableFrom(fieldType)) {
            value = jsonObject.get(key).textValue();
        }else if(Long.class.isAssignableFrom(fieldType) || long.class.isAssignableFrom(fieldType)){
            value = jsonObject.get(key).longValue();
        }
        else if(Integer.class.isAssignableFrom(fieldType) || int.class.isAssignableFrom(fieldType)) {
            value = jsonObject.get(key).intValue();
        }else if(Double.class.isAssignableFrom(fieldType) || double.class.isAssignableFrom(fieldType)) {
            value = jsonObject.get(key).doubleValue();
        }else if(Boolean.class.isAssignableFrom(fieldType) || boolean.class.isAssignableFrom(fieldType)) {
            value = jsonObject.get(key).booleanValue();
        }else if(LocalDate.class.isAssignableFrom(fieldType)) {
            if(jsonObject.get(key).textValue() == null) return null;
            value = LocalDate.parse(jsonObject.get(key).textValue(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }else if(LocalDateTime.class.isAssignableFrom(fieldType)) {
            if(jsonObject.get(key).textValue() == null) return null;
            value = LocalDateTime.parse(jsonObject.get(key).textValue());
        }

        return value;
    }



}

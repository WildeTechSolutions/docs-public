package com.thomaswilde.api;

import com.thomaswilde.fxcore.FXPropertyUtils;
import com.thomaswilde.jacksonfx.JacksonUtil;
import com.thomaswilde.lucene_desktop.SearchResultSummary;
import com.thomaswilde.lucene_desktop.SearchResultWrapper;
import com.thomaswilde.util.SearchOperation;
import com.thomaswilde.util.SqlItem;

import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.Column;
import javax.persistence.Id;

public abstract class ApiFXRepository<T, ID> implements ApiMethods<T, ID>{

    private static Logger log = LoggerFactory.getLogger(ApiFXRepository.class);

    public abstract String getBaseUrl();
    public abstract String getEndPoint();
    public abstract Map<String, String> getHeaders();

    protected Class<T> persistentClass;

    public ApiFXRepository(Class<T> persistentClass){
        this.persistentClass = persistentClass;
    }
    @SuppressWarnings("unchecked")
    public ApiFXRepository(){
        persistentClass = (Class<T>)
                ((ParameterizedType) getClass().getGenericSuperclass())
                        .getActualTypeArguments()[0];
    }

    @Override
    public List<T> get() throws IllegalStateException, IOException {
        return get(new HashMap<>());
    }

    @Override
    public List<T> get(String sort) throws Exception {
        return get(Map.of("order_by", sort));
    }

    @Override
    public T getTop1(Map<String, String> fieldValueMap) throws IllegalStateException, IOException  {
        List<T> results = get(fieldValueMap);
        if (results == null || results.isEmpty()) {
            return null;
        }
        return results.get(0);
    }

    //TODO need to account for a DateRange sql Item
    @Override
    public List<T> get(Collection<SqlItem> sqlItems) throws Exception {
        return get(sqlItems, null);
    }

    @Override
    public List<T> get(Collection<SqlItem> sqlItems, String sort) throws Exception {
        List<String> whereStrings = new ArrayList<>();
        List<Object> objects = new ArrayList<>();

        sqlItems.forEach(sqlItem -> {
            List<String> embeddedWhereStrings = new ArrayList<>();

            for (int i = 0; i < sqlItem.getValues().size(); i++) {
                Object value = sqlItem.getValues().get(i);
                SearchOperation searchOperation = sqlItem.getOperators().get(i);
                String operator = SqlItem.apiOperatorMap.get(searchOperation);

                String key = sqlItem.getFieldName();
                Class<?> fieldType = FXPropertyUtils.getFieldType(persistentClass, key);
                if (!FXPropertyUtils.isPrimitive(fieldType) && fieldType.isAssignableFrom(value.getClass())) {
                    // Need to to get its ID value
                    List<Field> fieldTypeIdFields = FXPropertyUtils.getFieldsOfDirectClassOnlyWithAnnotation(fieldType, Id.class);

                    try {
                        String whereString = sqlItem.getFieldName() + "." + fieldTypeIdFields.get(0).getName() + operator + "%s";
                        log.debug("Adding whereString: {}", whereString);
                        embeddedWhereStrings.add(whereString);

                        objects.add(PropertyUtils.getProperty(value, fieldTypeIdFields.get(0).getName()));
                    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                        e.printStackTrace();
                        log.error("did not find id field of provided Where Object");
                    }
                }else{
                    String whereString = sqlItem.getFieldName() + operator + "%s";
                    log.debug("Adding whereString: {}", whereString);
                    embeddedWhereStrings.add(whereString);

                    log.debug("Adding value: {}", value);
                    objects.add(value);
                }

            }
            whereStrings.add(String.format("(%s)", String.join(" OR ", embeddedWhereStrings)));
        });

        String queryTemplate = String.join(" AND ", whereStrings);

        log.debug("query template: {}", queryTemplate);
        objects.forEach(o -> log.debug("Adding value: {}", o.toString()));

        return querySort(queryTemplate, sort, objects.toArray(new Object[0]));
    }

    @Override
    public List<T> query(String queryTemplate, Object... values) throws IllegalStateException, IOException {
        return querySort(queryTemplate, null, values);
    }

    @Override
    public List<T> querySort(String query, String sort, Object... values) throws IllegalStateException, IOException  {
        String response = null;

        Map<String, String> paramMap = new HashMap<>();
//        String[] stringValues = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            if(values[i] != null){
                values[i] = values[i].toString().replaceAll("%", "");
            }
        }

        String queryValue;

        if (values.length == 1) {
            queryValue = String.format(query, values[0]);
        } else {
            queryValue = String.format(query, (Object[]) values);
        }

        log.debug("'query' value: {}", queryValue);
        paramMap.put("query", queryValue);
        if (sort != null && !sort.isBlank()) {
            paramMap.put("order_by", sort);
        }

        paramMap.forEach((key, value) -> log.debug("Query param: {}, value: {}", key, value));

        Response apiResponse = ApiRequest.builder()
                .setMethodType(ApiRequest.MethodType.GET)
                .setBaseUrl(getBaseUrl())
                .setEndPoint(getEndPoint())
                .setParams(paramMap)
                .setHeaders(getHeaders())
                .send();

        response = apiResponse.getBody();

        log.debug("Http code ({})Response:\n{}", apiResponse.getCode(), response);

        if(!okStatus(apiResponse.getCode())){
            throw new IllegalStateException(response);
        }

        if (response != null && !response.isEmpty()) {
            if (response.startsWith("<!DOCTYPE HTML")) {
                throw new IllegalStateException("SMSESSION has expired");
            }
        }

        return JacksonUtil.fromJsonList(response, persistentClass);
    }

    @Override
    public List<SearchResultWrapper<T>> search(String query) throws Exception {
        String response = null;

        Map<String, String> paramMap = Map.of(
                "search", query
        );

        Response apiResponse = ApiRequest.builder()
                .setMethodType(ApiRequest.MethodType.GET)
                .setBaseUrl(getBaseUrl())
                .setEndPoint(getEndPoint())
                .setParams(paramMap)
                .setHeaders(getHeaders())
                .send();

        response = apiResponse.getBody();

        log.debug("Http code ({})Response:\n{}", apiResponse.getCode(), response);
        if(!okStatus(apiResponse.getCode())){
            throw new IllegalStateException(response);
        }
        if (response != null && !response.isEmpty()) {
            if (response.startsWith("<!DOCTYPE HTML")) {
                throw new IllegalStateException("SMSESSION has expired");
            }
        }

        return (List<SearchResultWrapper<T>>) JacksonUtil.fromJsonListWrapped(response, SearchResultWrapper.class, persistentClass);
    }

    @Override
    public SearchResultSummary<T> search(String query, int page, int size) throws Exception {
        String response = null;

        Map<String, String> paramMap = Map.of(
          "search", query,
          "page", Integer.toString(page),
          "size", Integer.toString(size)
        );

        Response apiResponse = ApiRequest.builder()
                .setMethodType(ApiRequest.MethodType.GET)
                .setBaseUrl(getBaseUrl())
                .setEndPoint(getEndPoint())
                .setParams(paramMap)
                .setHeaders(getHeaders())
                .send();

        response = apiResponse.getBody();

        log.debug("Http code ({})Response:\n{}", apiResponse.getCode(), response);
        if(!okStatus(apiResponse.getCode())){
            throw new IllegalStateException(response);
        }
        if (response != null && !response.isEmpty()) {
            if (response.startsWith("<!DOCTYPE HTML")) {
                throw new IllegalStateException("SMSESSION has expired");
            }
        }

        return JacksonUtil.fromJson(response, SearchResultSummary.class, persistentClass);
    }

    @Override
    public List<T> get(String methodName, Object... values) throws IllegalStateException, IOException {
        String response = null;

        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("methodName", methodName);


        List<String> valuesAsStrings = Stream.of(values).map(Object::toString).collect(Collectors.toList());
        paramMap.put("values", String.join(",", valuesAsStrings));


        Response apiResponse = ApiRequest.builder()
                .setMethodType(ApiRequest.MethodType.GET)
                .setBaseUrl(getBaseUrl())
                .setEndPoint(getEndPoint())
                .setParams(paramMap)
                .setHeaders(getHeaders())
                .send();

        response = apiResponse.getBody();

        log.debug("Http code ({})Response:\n{}", apiResponse.getCode(), response);
        if(!okStatus(apiResponse.getCode())){
            throw new IllegalStateException(response);
        }
        if (response != null && !response.isEmpty()) {
            if (response.startsWith("<!DOCTYPE HTML")) {
                throw new IllegalStateException("SMSESSION has expired");
            }
        }



        return JacksonUtil.fromJsonList(response, persistentClass);
    }



    @Override
    public List<T> get(Map<String, String> fieldValueMap) throws IllegalStateException, IOException {
        String response = null;


        Response apiResponse = ApiRequest.builder()
                .setMethodType(ApiRequest.MethodType.GET)
                .setBaseUrl(getBaseUrl())
                .setEndPoint(getEndPoint())
                .setParams(fieldValueMap)
                .setHeaders(getHeaders())
                .send();

        response = apiResponse.getBody();

        log.debug("Http code ({})Response:\n{}", apiResponse.getCode(), response);
        if(!okStatus(apiResponse.getCode())){
            throw new IllegalStateException(response);
        }
        if (response != null && !response.isEmpty()) {
            if (response.startsWith("<!DOCTYPE HTML")) {
                throw new IllegalStateException("SMSESSION has expired");
            }
        }



        return JacksonUtil.fromJsonList(response, persistentClass);
    }

    @Override
    public T getById(ID id) throws IllegalStateException, IOException {

        String response = getResponseById(id);

        return JacksonUtil.fromJson(response, persistentClass);
    }

    private String getResponseById(ID id) throws IllegalStateException, IOException {
        List<Field> idFields = FXPropertyUtils.getFieldsOfDirectClassOnlyWithAnnotation(persistentClass, Id.class);
        if (idFields.isEmpty()) {
            log.error("Class {} does not have an ID field", persistentClass.getName());
            return null;
        }

        Map<String, String> params = new HashMap<>();
        List<String> idValues = new ArrayList<>();
        if(idFields.size() > 1){
            Collection<?> idValuesCollection = (Collection<?>) id;
            for (Object o : idValuesCollection) {
                idValues.add(o.toString());
            }
        }else{
            idValues.add(id.toString());
        }

        String response = null;

        Response apiResponse = ApiRequest.builder()
                .setMethodType(ApiRequest.MethodType.GET)
                .setBaseUrl(getBaseUrl())
                .setEndPoint(getEndPoint() + "/" + String.join("/", idValues))
//                .setParams(params)
                .setHeaders(getHeaders())
                .send();

        response = apiResponse.getBody();

        log.debug("Http code ({})Response:\n{}", apiResponse.getCode(), response);
        if(!okStatus(apiResponse.getCode())){
            throw new IllegalStateException(response);
        }
        if (response != null && !response.isEmpty()) {
            if (response.startsWith("<!DOCTYPE HTML")) {
                throw new IllegalStateException("SMSESSION has expired");
            }
        }

        return response;
    }

    @Override
    public void refreshBean(T bean) throws IllegalStateException, IOException {
        List<Field> idFields = FXPropertyUtils.getFieldsOfDirectClassOnlyWithAnnotation(persistentClass, Id.class);
        if (idFields.isEmpty()) {
            log.error("Class {} does not have an ID field", persistentClass.getName());
            return;
        }

        Map<String, String> params = new HashMap<>();
        List<String> idValues = new ArrayList<>();
        for (Field field : idFields) {
            try {
//                params.put(field.getName(), PropertyUtils.getProperty(bean, field.getName()).toString());
                idValues.add(PropertyUtils.getProperty(bean, field.getName()).toString());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        String response = null;

        Response apiResponse = ApiRequest.builder()
                .setMethodType(ApiRequest.MethodType.GET)
                .setBaseUrl(getBaseUrl())
                .setEndPoint(getEndPoint() + "/" + String.join("/", idValues))
//                .setParams(params)
                .setHeaders(getHeaders())
                .send();

        response = apiResponse.getBody();

        log.debug("Http code ({})Response:\n{}", apiResponse.getCode(), response);
        if(!okStatus(apiResponse.getCode())){
            throw new IllegalStateException(response);
        }
        if (response != null && !response.isEmpty()) {
            if (response.startsWith("<!DOCTYPE HTML")) {
                throw new IllegalStateException("SMSESSION has expired");
            }
        }

        JacksonUtil.patchJson(bean, response);
    }

    @Override
    public T post(T t) throws IOException, IllegalStateException{
        Response apiResponse = ApiRequest.builder()
                .setMethodType(ApiRequest.MethodType.POST)
                .setBaseUrl(getBaseUrl())
                .setEndPoint(getEndPoint())
                .setBody(JacksonUtil.toJson(t))
                .setHeaders(getHeaders())
                .send();

        String response = apiResponse.getBody();

        log.debug("Http code ({})Response:\n{}", apiResponse.getCode(), response);
        if(!okStatus(apiResponse.getCode())){
            log.warn("Body:\n" + JacksonUtil.toJson(t));
            throw new IllegalStateException(response);
        }
        if (response != null && !response.isEmpty()) {
            if (response.startsWith("<!DOCTYPE HTML")) {
                throw new IllegalStateException("SMSESSION has expired");
            }
        }

        // Let's patch original object to get ID into original value
        JacksonUtil.patchJson(t, response);

        return t;
    }

    @Override
    public T put(T t) throws IOException, IllegalStateException{
        Response apiResponse = ApiRequest.builder()
                .setMethodType(ApiRequest.MethodType.PUT)
                .setBaseUrl(getBaseUrl())
                .setEndPoint(getEndPoint())
                .setBody(JacksonUtil.toJson(t))
                .setHeaders(getHeaders())
                .send();

        String response = apiResponse.getBody();

        log.debug("Http code ({})Response:\n{}", apiResponse.getCode(), response);
        if(!okStatus(apiResponse.getCode())){
            throw new IllegalStateException(response);
        }
        if (response != null && !response.isEmpty()) {
            if (response.startsWith("<!DOCTYPE HTML")) {
                throw new IllegalStateException("SMSESSION has expired");
            }
        }

        return JacksonUtil.fromJson(response, persistentClass);
    }

    @Override
    public void putAll(Collection<T> t) throws IllegalStateException, IOException {
        Response apiResponse = ApiRequest.builder()
                .setMethodType(ApiRequest.MethodType.PUT)
                .setBaseUrl(getBaseUrl())
                .setEndPoint(getEndPoint())
                .setBody(JacksonUtil.toJson(t))
                .setHeaders(getHeaders())
                .send();

        String response = apiResponse.getBody();

        log.debug("Http code ({})Response:\n{}", apiResponse.getCode(), response);
        if(!okStatus(apiResponse.getCode())){
            throw new IllegalStateException(response);
        }
        if (response != null && !response.isEmpty()) {
            if (response.startsWith("<!DOCTYPE HTML")) {
                throw new IllegalStateException("SMSESSION has expired");
            }
        }
    }

    @Override
    public T patch(T t, String... fields) throws IOException, IllegalStateException {
        List<Field> idFields = FXPropertyUtils.getFieldsOfDirectClassOnlyWithAnnotation(persistentClass, Id.class);
        if (idFields.isEmpty()) {
            log.error("Class {} does not have an ID field", persistentClass.getName());
            return null;
        }

        Map<String, String> params = new HashMap<>();
        List<String> idValues = new ArrayList<>();
        if(idFields.size() > 1){
            Collection<?> idValuesCollection = (Collection<?>) idFields.stream().map(field -> {
                try {
                    return PropertyUtils.getProperty(t, field.getName()).toString();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
                return null;
            });
            for (Object o : idValuesCollection) {
                idValues.add(o.toString());
            }
        }else{
            try {
                idValues.add(PropertyUtils.getProperty(t, idFields.get(0).getName()).toString());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        Map<String, Object> fieldAndValue = new HashMap<>();
        for(String field : fields){
            try {
                Object value = PropertyUtils.getProperty(t, field);
                fieldAndValue.put(field, value);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        Response apiResponse = ApiRequest.builder()
                .setMethodType(ApiRequest.MethodType.PATCH)
                .setBaseUrl(getBaseUrl())
                .setEndPoint(getEndPoint() + "/" + String.join("/", idValues))
                .setBody(JacksonUtil.toJson(fieldAndValue))
                .setHeaders(getHeaders())
                .send();

        String response = apiResponse.getBody();

        log.debug("Http code ({})Response:\n{}", apiResponse.getCode(), response);
        if(!okStatus(apiResponse.getCode())){
            throw new IllegalStateException(response);
        }
        if (response != null && !response.isEmpty()) {
            if (response.startsWith("<!DOCTYPE HTML")) {
                throw new IllegalStateException("SMSESSION has expired");
            }
        }

        return JacksonUtil.fromJson(response, persistentClass);
    }

    @Override
    public void delete(T t) throws IOException, IllegalStateException {
        Response apiResponse = ApiRequest.builder()
                .setMethodType(ApiRequest.MethodType.DELETE)
                .setBaseUrl(getBaseUrl())
                .setEndPoint(getEndPoint())
                .setBody(JacksonUtil.toJson(t))
                .setHeaders(getHeaders())
                .send();

        String response = apiResponse.getBody();

        log.debug("Http code ({})Response:\n{}", apiResponse.getCode(), response);
        if(!okStatus(apiResponse.getCode())){
            throw new IllegalStateException(response);
        }
        if (response != null && !response.isEmpty()) {
            if (response.startsWith("<!DOCTYPE HTML")) {
                throw new IllegalStateException("SMSESSION has expired");
            }
        }
    }

    @Override
    public void deleteAll(Collection<T> t) throws IOException, IllegalStateException {
        Response apiResponse = ApiRequest.builder()
                .setMethodType(ApiRequest.MethodType.DELETE)
                .setBaseUrl(getBaseUrl())
                .setEndPoint(getEndPoint())
                .setBody(JacksonUtil.toJson(t))
                .setHeaders(getHeaders())
                .send();

        String response = apiResponse.getBody();

        log.debug("Http code ({})Response:\n{}", apiResponse.getCode(), response);
        if(!okStatus(apiResponse.getCode())){
            throw new IllegalStateException(response);
        }
        if (response != null && !response.isEmpty()) {
            if (response.startsWith("<!DOCTYPE HTML")) {
                throw new IllegalStateException("SMSESSION has expired");
            }
        }

    }
    
    private boolean okStatus(int status){
        return status == 200 || status == 201;
    }

    @Override
    public void fetchList(T entity, String fieldName) throws IllegalStateException, IOException  {
        log.warn("Fetch list is not yet supported in API repository");
    }

    @Override
    public void fetchAllLists(T entity, boolean async) throws IllegalStateException, IOException  {
        log.warn("Fetch list is not yet supported in API repository");
    }

    @Override
    public void fetchEagerLists(T entity, boolean async) throws Exception {
        log.warn("Fetch eager lists is not yet supported in API repository");
    }

    public Class<T> getPersistentClass() {
        return persistentClass;
    }
}

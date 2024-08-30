package com.thomaswilde.wildebeans.database;

import com.thomaswilde.fxcore.FXPropertyUtils;
import com.thomaswilde.util.SearchOperation;
import com.thomaswilde.util.SqlItem;
import com.thomaswilde.wildebeans.annotations.DefaultSort;
import com.thomaswilde.wildebeans.application.WildeDBApplication;
import com.thomaswilde.wildebeans.ui.dbsearch.DatePickerOption;
import com.thomaswilde.wildebeans.ui.dbsearch.DateRange;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.JoinColumn;

//import sun.reflect.generics.reflectiveObjects.TypeVariableImpl;

public abstract class Repository<T, ID> implements DbMethods<T, ID>{

    private static Logger log = LoggerFactory.getLogger(Repository.class);

    protected Class<T> persistentClass;

    public Repository(Class<T> persistentClass){
        this.persistentClass = persistentClass;
    }
    @SuppressWarnings("unchecked")
    public Repository(){


        Type repositoryType = getClass().getGenericSuperclass();

        if (repositoryType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) repositoryType;
            Type first = parameterizedType.getActualTypeArguments()[0];

            log.trace("A repository was created of type {}", first);

            if(first instanceof WildcardType) {
                log.warn("A repository was of wildcard type");
            }
//            else if (first instanceof TypeVariableImpl) {
//
//                log.trace("A repository was of TypeVariableImpl type");
//                TypeVariableImpl<?> typeVariableImpl = (TypeVariableImpl<?>) first;
//                GenericDeclaration genericDeclaration = typeVariableImpl.getGenericDeclaration();
//                if (genericDeclaration instanceof Class) {
//                    persistentClass = (Class<T>) genericDeclaration;
//                }
//            }
            else{

                log.trace("A repository had normal type parameter");
                persistentClass = (Class<T>) first;
            }

        }


//        persistentClass = (Class<T>)
//                ((ParameterizedType) getClass().getGenericSuperclass())
//                        .getActualTypeArguments()[0];
    }

    @SuppressWarnings("unchecked")
    public T getById(ID id) throws SQLException {

        // For now, let's no support composite IDs for simplicity
        // could probably check if ID is stance of collection  and then just create the Where string
        List<Field> idFields = FXPropertyUtils.getFieldsOfDirectClassOnlyWithAnnotation(persistentClass, Id.class);
        if (idFields.isEmpty()) {
            log.error("Class {} does not have an ID field", persistentClass.getName());
            return null;
        }

        String alias = persistentClass.getSimpleName();

        if (id instanceof Collection) {
            Collection<?> idValues = (Collection<?>) id;
            List<String> whereStatements = new ArrayList<>();
            List<Object> whereObjects = new ArrayList<>();
            Iterator<?> idValueIterator = idValues.iterator();
            for (Field idField : idFields) {
                whereStatements.add(String.format("%s = ?", alias + "." + idField.getAnnotation(Column.class).name()));
                Object idValue = idValueIterator.next();
                log.trace("adding id value: {}", idValue);
                whereObjects.add(idValue);
            }

            return WildeDBApplication.getInstance().queryTopOne(persistentClass,
                    String.join(" AND ", whereStatements),
                    whereObjects.toArray());

        }else{
            Column idColumn = idFields.get(0).getAnnotation(Column.class);

            return WildeDBApplication.getInstance().queryTopOne(persistentClass,
                    String.format("%s = ?", alias + "." + idColumn.name()),
                    (Object) id);
        }



    }

    @Override
    public void refreshBean(T bean) throws SQLException{
        WildeDBApplication.getInstance().refreshBean(bean);
    }

    public T insert(T entity) throws SQLException{
        WildeDBApplication.getInstance().insert(entity);
        return entity;
    }

    @Override
    public void insert(Collection<T> entity) throws Exception {
        WildeDBApplication.getInstance().insert(entity);
    }

    public T update(T entity) throws SQLException{
        WildeDBApplication.getInstance().update(entity);
        return entity;
    }

    public T update(T entity, String... fields) throws SQLException{
        WildeDBApplication.getInstance().update(entity, fields);
        return entity;
    }

    @Override
    public T update(T entity, Collection<String> fields) throws Exception {
        WildeDBApplication.getInstance().update(entity, fields.toArray(new String[0]));
        return entity;
    }

    @SuppressWarnings("unchecked")
    public List<T> findAll() throws SQLException{
        DefaultSort defaultSort = persistentClass.getAnnotation(DefaultSort.class);
        String orderBy = null;
        if(defaultSort != null && Strings.isNotBlank(defaultSort.columns())){
            orderBy = "ORDER BY " + defaultSort.columns();
        }

        return WildeDBApplication.getInstance().query(persistentClass, orderBy);
    }

    @Override
    public List<T> findAll(String sort) throws Exception {
        DefaultSort defaultSort = persistentClass.getAnnotation(DefaultSort.class);
        String orderBy = generatedOrderBy(sort);

        return WildeDBApplication.getInstance().query(persistentClass, orderBy);
    }

    @SuppressWarnings("unchecked")
    public List<T> findBy(String where, Object... values) throws SQLException{

        return WildeDBApplication.getInstance().query(persistentClass, where, values);
    }

    @Override
    public T findTop1By(Map<String, Object> fieldValueMap) throws SQLException {
        List<T> results = findBy(fieldValueMap);
        if (!results.isEmpty()) {
            return results.get(0);
        }
        return null;
    }

    @Override
    public List<T> findBySuggestibleMethod(String methodName, Object... values) throws Exception {
        return WildeDBApplication.getInstance().findBySuggestibleMethod(persistentClass, methodName, values);
    }

    @Override
    public List<T> findByCustomQuery(String sqlQuery, String apiQuery, Object... values) throws Exception {
        return WildeDBApplication.getInstance().query(persistentClass, sqlQuery, values);
    }

    @Override
    public List<T> findByWildeQuery(String query, Object... values) throws Exception {
        return findByWildeQuery(query, null, null, values);
    }

    @Override
    public List<T> findByWildeQuery(String query, Collection<String> lazyFieldsJoinEagerly, Collection<String> lazyFieldsFetchEagerly, Object... values) throws Exception {
        String orderBy = null;
        int limit = 0;

        if(query.contains("LIMIT")){
            limit = Integer.parseInt(StringUtils.substringAfter(query, "LIMIT").trim());
            query = StringUtils.substringBefore(query, "LIMIT").trim();
        }
        if (query.contains("ORDER BY")) {
            orderBy = StringUtils.substringAfter(query, "ORDER BY").trim();
            query = StringUtils.substringBefore(query, "ORDER BY").trim();
        }
        StringBuilder sqlQuery = new StringBuilder();
        query = query.replace("%s", "?");
        char[] chars = query.toCharArray();

        int index = 0;
        int valueIndex = 0;

        while (index < chars.length) {
            if(chars[index] == '(' || chars[index] == ')' || chars[index] == ' '){
                log.trace("appending: `{}`", chars[index]);
                sqlQuery.append(chars[index]);
                index++;
            }
            else{
                log.trace("Building fieldName for remaining string `{}`", query.substring(index));
                String remainingString = query.substring(index);
                char lastLetterOfOperator = 'a';
                if(remainingString.trim().startsWith("AND")){
                    lastLetterOfOperator = 'D';
                }else if(remainingString.trim().startsWith("OR")){
                    lastLetterOfOperator = 'R';
                }
                if(lastLetterOfOperator != 'a'){

                    while(chars[index] != lastLetterOfOperator) {
                        log.trace("appending: {}", chars[index]);
                        sqlQuery.append(chars[index]);
                        index++;
                    }
                    log.trace("appending: {}", chars[index]);
                    sqlQuery.append(chars[index]);
                    index++;
                    continue;
                }

                StringBuilder fieldString = new StringBuilder();
                StringBuilder operatorString = new StringBuilder();
                StringBuilder valueString = new StringBuilder();

                // Piece together fieldName              // && chars[index] != '*'
                while(chars[index] != ':' && chars[index] != '<' && chars[index] != '>' && chars[index] != '!' ){
                    fieldString.append(chars[index]);
                    index++;
                }

                // Piece together operator              //  || chars[index] == '*'
                while(chars[index] == ':' || chars[index] == '<' || chars[index] == '>' || chars[index] == '!'){
                    operatorString.append(chars[index]);
                    index++;
                }

                // Piece together value
                while(index < chars.length && chars[index] != ' ' && chars[index] != ')'){
                    valueString.append(chars[index]);
                    index++;
                }
                List<String> fieldOperatorValue = new ArrayList<>();

                boolean containsNullOperation = false;

                if(values[valueIndex] instanceof String){

                    if(values[valueIndex].toString().equalsIgnoreCase("null")){
                        fieldOperatorValue.add(String.format("%s IS NULL", convertFieldNameToAliasedColumn(fieldString.toString().trim())));
                        containsNullOperation = true;
                    }else if(values[valueIndex].toString().equalsIgnoreCase("notnull") || values[valueIndex].toString().equalsIgnoreCase("not null")){
                        fieldOperatorValue.add(String.format("%s IS NOT NULL", convertFieldNameToAliasedColumn(fieldString.toString().trim())));
                        containsNullOperation = true;
                    }else{
                        fieldOperatorValue.add(String.format("upper(%s)", convertFieldNameToAliasedColumn(fieldString.toString().trim())));
                        values[valueIndex] = ((String) values[valueIndex]).toUpperCase();
                    }


                }else{
                    fieldOperatorValue.add(convertFieldNameToAliasedColumn(fieldString.toString().trim()));
                }

                if (!containsNullOperation) {
                    log.trace("Operator String is: `{}`", operatorString.toString());
                    if (valueString.toString().contains("%") || valueString.toString().contains("*")) {
                        fieldOperatorValue.add("LIKE");
                    }else{
                        fieldOperatorValue.add(operatorString.toString().replace(":", "=").trim());
                    }
                }


                boolean startsWith = valueString.toString().startsWith("%") || valueString.toString().startsWith("*");
                boolean endsWith = valueString.toString().endsWith("%") || valueString.toString().endsWith("*");
                if (startsWith && endsWith) {
                    values[valueIndex] = "%" + values[valueIndex] + "%";
                }else if(startsWith){
                    values[valueIndex] = "%" + values[valueIndex];
                }else if(endsWith){
                    values[valueIndex] = values[valueIndex] + "%";
                }
                valueIndex++;

                if (!containsNullOperation) {
                    fieldOperatorValue.add("?");
                }


                log.trace("appending: {}", String.join(" ", fieldOperatorValue));
                sqlQuery.append(String.join(" ", fieldOperatorValue));




            }

        }


        if(orderBy != null){
            List<String> orders = new ArrayList<>();
            String[] sortParams = orderBy.split(",");

            for (String sortParam : sortParams) {
                sortParam = sortParam.trim();

                // Determine if a direction was provided
                String[] sortAndDirection = sortParam.split("\\s+");
                String fieldName;
                String direction;

                if(sortAndDirection.length == 2){
                    fieldName = sortAndDirection[0];
                    direction = sortAndDirection[1];
                }else{
                    fieldName = sortParam;
                    direction = "ASC";
                }

                orders.add(String.format("%s %s", convertFieldNameToAliasedColumn(fieldName), direction));
            }
            sqlQuery.append(" ORDER BY " + String.join(", ", orders));
        }

        // Need to remove null and not null values as they won't be injected
        List<Object> valueList = new ArrayList<>(Arrays.asList(values));
        valueList.removeIf(o -> o.toString().matches("not null|notnull|null|NOT NULL|NOTNULL|NULL"));

//        return WildeDBApplication.getInstance().query(persistentClass, sqlQuery.toString(), valueList);
//        if(limit > 0){
//            return WildeDBApplication.getInstance().query(persistentClass, lazyFieldsJoinEagerly, lazyFieldsFetchEagerly, limit, sqlQuery.toString(), valueList);
//        }else{
//            return WildeDBApplication.getInstance().query(persistentClass, sqlQuery.toString(), valueList);
//        }
        return WildeDBApplication.getInstance().query(persistentClass, lazyFieldsJoinEagerly, lazyFieldsFetchEagerly, limit, sqlQuery.toString(), valueList);

    }

    private String convertWildeQueryToSqlQuery(String query, Object... values){
        // Need to identify all fields and operators and convert them
//        query = query.replace("%s", "?");
//        query = query.replace(":", "=");
//
//        String fieldString = query.replace("=", " ")
//                .replace("?", " ")
//                .replace(">", " ")
//                .replace("<", " ")
//                .replace("!", " ");
//        List<String> fieldNames = new ArrayList<>(Arrays.asList(fieldString.split("\\s+")));
//        fieldNames.removeIf(s -> s.matches("AND|OR|ORDER|BY|WHERE|SELECT"));
//
//        for (String fieldName : fieldNames) {
//            query = query.replace(fieldName, convertFieldNameToAliasedColumn(fieldName));
//        }
//
//        if (query.contains("%")) {
//            String reverseQuery = StringUtils.reverse(query);
//            StringUtils.ge
//        }

        StringBuilder sqlQuery = new StringBuilder();
        char[] chars = query.toCharArray();

        int index = 0;
        int valueIndex = 0;

        while (index < chars.length) {
            if(chars[index] == '('){
                log.trace("appending: (");
                sqlQuery.append(chars[index]);

                index++;
            }else if(chars[index] == ')'){
                log.trace("appending: )");
                sqlQuery.append(chars[index]);
                index++;
            }
            else{
                StringBuilder fieldString = new StringBuilder();
                StringBuilder operatorString = new StringBuilder();
                StringBuilder valueString = new StringBuilder();

                // Piece together fieldName              // && chars[index] != '*'
                while(chars[index] != ':' && chars[index] != '<' && chars[index] != '>' && chars[index] != '!' ){
                    fieldString.append(chars[index]);
                    index++;
                }

                // Piece together operator              //  || chars[index] == '*'
                while(chars[index] == ':' || chars[index] == '<' || chars[index] == '>' || chars[index] == '!'){
                    operatorString.append(chars[index]);
                    index++;
                }

                // Piece together value
                while(index < chars.length && chars[index] != ' '){
                    valueString.append(chars[index]);
                    index++;
                }
                List<String> fieldOperatorValue = new ArrayList<>();
                fieldOperatorValue.add(convertFieldNameToAliasedColumn(fieldString.toString().trim()));

                if (valueString.toString().contains("%") || valueString.toString().contains("*")) {
                    fieldOperatorValue.add("LIKE");
                }else if(valueString.toString().equalsIgnoreCase("null")){
                    fieldOperatorValue.add("IS NULL");
                }else if(valueString.toString().equalsIgnoreCase("notnull") || valueString.toString().equalsIgnoreCase("not null")){
                    fieldOperatorValue.add("IS NOT NULL");
                }
                else{
                    fieldOperatorValue.add(operatorString.toString().replace(":", "=").trim());
                }

                boolean startsWith = valueString.toString().startsWith("%") || valueString.toString().startsWith("*");
                boolean endsWith = valueString.toString().endsWith("%") || valueString.toString().endsWith("*");
                if (startsWith && endsWith) {
                    values[valueIndex] = "%" + values[valueIndex] + "%";
                }else if(startsWith){
                    values[valueIndex] = "%" + values[valueIndex];
                }else if(endsWith){
                    values[valueIndex] = values[valueIndex] + "%";
                }
                if(!valueString.toString().equalsIgnoreCase("null") && !valueString.toString().equalsIgnoreCase("notnull") && !valueString.toString().equalsIgnoreCase("not null")){
                    fieldOperatorValue.add("?");
                }


                log.trace("appending: {}", String.join(" ", fieldOperatorValue));
                sqlQuery.append(String.join(" ", fieldOperatorValue));

                String remainingString = query.substring(index);
                char lastLetterOfOperator = 'a';
                if(remainingString.trim().startsWith("AND")){
                    lastLetterOfOperator = 'D';
                }else if(remainingString.trim().startsWith("OR")){
                    lastLetterOfOperator = 'R';
                }
                if(lastLetterOfOperator != 'a'){
                    while(chars[index] != lastLetterOfOperator){
                        log.trace("appending: {}", chars[index]);
                        sqlQuery.append(chars[index]);
                        index++;
                    }
                }


            }

        }


        return query;
    }

    @Override
    public List<T> findByCustomQuerySort(String sqlQuery, String apiQuery, String sort, Object... values) throws Exception {
        return WildeDBApplication.getInstance().query(persistentClass, sqlQuery, values);
    }

    @Override
    public List<T> findBy(Collection<SqlItem> sqlItems, String sort, Collection<String> lazyFieldsJoinEagerly, Collection<String> lazyFieldsFetchEagerly, int limit) throws Exception {
        List<String> whereStrings = new ArrayList<>();
        List<Object> objects = new ArrayList<>();

        sqlItems.forEach(sqlItem -> {
            String alias = persistentClass.getSimpleName();

            String key = sqlItem.getFieldName();

            // Create the alias
            Class<?> currentClass = persistentClass;
            List<String> hierarchy = new ArrayList<>(Arrays.asList(key.split("\\.")));

            if(hierarchy.size() > 1) {
                currentClass = FXPropertyUtils.getFieldType(currentClass, hierarchy.get(0));
                for (int i = 1; i < hierarchy.size(); i++) {
                    String childField = hierarchy.get(i);
                    log.debug("checking child field: {}, of class {}", childField, currentClass.getSimpleName());
                    // Check if it's of the class or if it's instead a parent class
                    if (Arrays.stream(currentClass.getDeclaredFields())
                            .anyMatch(f -> f.getName().equals(childField))) {

                        log.debug("Current class contained field {}", childField);

                        // if the field is the id
                        alias = String.format("%s_%s", alias, hierarchy.get(i-1));

                    } else {
                        // Need to recursively check parent classes and append Alias while doing so
                        Class<?> parentClass = currentClass.getSuperclass();
                        int level = 1;
                        while (parentClass != null) {
                            alias = String.format("%s_%s_%s", alias, "parent", level);
                            if (Arrays.stream(parentClass.getDeclaredFields())
                                    .anyMatch(f -> f.getName().equals(childField))) {
                                break;
                            }
                            level++;
                            parentClass = parentClass.getSuperclass();
                        }
                    }
                    currentClass = FXPropertyUtils.getFieldType(currentClass, childField);
                }
            }

            // We could get the field's column, but it won't be the fully qualified alias + columnName
            Field field = FXPropertyUtils.getField(persistentClass, key);
            Class<?> fieldType = FXPropertyUtils.getFieldType(persistentClass, key);

            Column column = field.getAnnotation(Column.class);
            JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
            String columnName = column != null ? column.name() : joinColumn.name();

            List<String> embeddedWhereStrings = new ArrayList<>();

            int operatorIndex = 0;
            for (int i = 0; i < sqlItem.getValues().size(); i++) {
                Object value = sqlItem.getValues().get(i);
                SearchOperation searchOperation = sqlItem.getOperators().get(operatorIndex);

                if(value != null){
                    String operator = SqlItem.operatorMap.get(searchOperation);

                    // For string we apply upper
                    // For non-string it is just `alias + "." + columnName` `operator`
                    // For multi argument, need to know the number of repeats and increment operator index

                    String columnPlaceHolder = null;
                    boolean containsNullOperation = false;
                    if (fieldType.isAssignableFrom(String.class)){

                        if(value.toString().equalsIgnoreCase("null")){
                            columnPlaceHolder = String.format("%s IS NULL", alias + "." + columnName);
                            embeddedWhereStrings.add(columnPlaceHolder);
                            containsNullOperation = true;
                        }else if(value.toString().equalsIgnoreCase("notnull") || value.toString().equalsIgnoreCase("not null")){
                            columnPlaceHolder = String.format("%s IS NOT NULL", alias + "." + columnName);
                            embeddedWhereStrings.add(columnPlaceHolder);
                            containsNullOperation = true;
                        }else{
                            columnPlaceHolder = String.format("upper(%s)", alias + "." + columnName);
                            embeddedWhereStrings.add(String.format("%s %s ?", columnPlaceHolder, operator));
                        }


                    }else if(fieldType.isAssignableFrom(LocalDate.class)){
                        // We are going to assume the SqlItem is instance of DateRange
                        DateRange dateRange = (DateRange) value;

                        columnPlaceHolder = alias + "." + columnName;
//                        embeddedWhereStrings.add(String.format("%s %s ?", columnPlaceHolder, operator));
                        operatorIndex++;
                        searchOperation = sqlItem.getOperators().get(operatorIndex);
                        String operator2 = SqlItem.operatorMap.get(searchOperation);

                        // Need to consider if NONE was selected, proper query is then IS NULL
                        if(!Objects.equals(dateRange.getDatePickerOption().getId(), DatePickerOption.NONE)){
                            embeddedWhereStrings.add(String.format("%1$s %2$s ? AND %1$s %3$s ?", columnPlaceHolder, operator, operator2));
                        }else{
                            embeddedWhereStrings.add(String.format("%1$s IS NULL", columnPlaceHolder));
                        }

                    }else{
                        columnPlaceHolder = alias + "." + columnName;
                        embeddedWhereStrings.add(String.format("%s %s ?", columnPlaceHolder, operator));
                    }
//                        columnPlaceHolder = (fieldType.isAssignableFrom(String.class)) ?
//                            // If the field is a String, then set the column to upper
//                            String.format("upper(%s)", alias + "." + columnName) :
//                            // Else, just the column
//                            alias + "." + columnName;
//                    embeddedWhereStrings.add(String.format("%s %s ?", columnPlaceHolder, operator));

                    if (!FXPropertyUtils.isPrimitive(fieldType) && fieldType.isAssignableFrom(value.getClass())) {
                        // Need to to get its ID value
                        List<Field> fieldTypeIdFields = FXPropertyUtils.getFieldsOfDirectClassOnlyWithAnnotation(fieldType, Id.class);

                        try {
                            objects.add(PropertyUtils.getProperty(value, fieldTypeIdFields.get(0).getName()));
                        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                            e.printStackTrace();
                            log.error("did not find id field of provided Where Object");
                        }
                    }else{
                        if(searchOperation.equals(SearchOperation.STARTS_WITH)){
                            objects.add(value.toString().toUpperCase() + "%");
                        }else if(searchOperation.equals(SearchOperation.CONTAINS)){
                            objects.add("%" + value.toString().toUpperCase() + "%");
                        }else{
                            if (!containsNullOperation) {
                                if(value instanceof String){
                                    objects.add(value.toString().toUpperCase());
                                }else if(value instanceof DateRange){
                                    DateRange dateRange = (DateRange) value;
                                    // We won't add any values if the date picker option was NONE i.e. null
                                    if(!Objects.equals(dateRange.getDatePickerOption().getId(), DatePickerOption.NONE)) {
                                        objects.add(dateRange.getStartDate());
                                        objects.add(dateRange.getEndDate());
                                    }
                                }
                                else{
                                    objects.add(value);
                                }
                            }


                        }

                    }
                }else{
                    embeddedWhereStrings.add(String.format("%s IS NULL", alias + "." + columnName));
                }

                operatorIndex++;
            }


            whereStrings.add(String.format("(%s)", String.join(" OR ", embeddedWhereStrings)));

        });


        String orderBy = generatedOrderBy(sort);


        return WildeDBApplication.getInstance().query(persistentClass, lazyFieldsJoinEagerly, lazyFieldsFetchEagerly, limit, String.join(" AND ", whereStrings) + orderBy, objects);
    }

    @Override
    public List<T> findBy(Collection<SqlItem> sqlItems, String sort) throws Exception {
        return findBy(sqlItems, sort, null, null, 0);
    }

    @Override
    public List<T> findBy(Collection<SqlItem> sqlItems) throws Exception {
        return findBy(sqlItems, null);
    }

    @Override
    public List<T> findBy(Map<String, Object> fieldValueMap, Collection<String> lazyFieldsJoinEagerly, Collection<String> lazyFieldsFetchEagerly) throws SQLException {

        // The Map may be mutable preventing removing the "order_by" key
        fieldValueMap = new HashMap<>(fieldValueMap);

        List<String> whereStrings = new ArrayList<>();
        List<Object> objects = new ArrayList<>();

        String sort = null;
        int limit = 0;
        if(fieldValueMap.containsKey("order_by")){
            sort = (String) fieldValueMap.get("order_by");
            fieldValueMap.remove("order_by");
        }
        if(fieldValueMap.containsKey("limit")){
            limit = (int) fieldValueMap.get("limit");
            fieldValueMap.remove("limit");
        }
        if(fieldValueMap.containsKey("fetch")){
            Object fetchFields = fieldValueMap.get("fetch");
            if(fetchFields instanceof String){
                lazyFieldsFetchEagerly = Arrays.asList(((String) fetchFields).split(","));
            }else{
                lazyFieldsJoinEagerly = (Collection<String>) fetchFields;
            }
            fieldValueMap.remove("fetch");
        }



        fieldValueMap.forEach((key, value) -> {

            String aliasedColumnName = convertFieldNameToAliasedColumn(key);
            Class<?> fieldType = FXPropertyUtils.getFieldType(persistentClass, key);

            if(value != null){
                if ("notnull".equalsIgnoreCase(value.toString()) || "not null".equalsIgnoreCase(value.toString())) {
                    whereStrings.add(String.format("%s IS NOT NULL", aliasedColumnName));
                } else if ("null".equalsIgnoreCase(value.toString())) {
                    whereStrings.add(String.format("%s IS NULL", aliasedColumnName));
                }
                else {
                    if(value.toString().contains("*")){
                        value = value.toString().replace("*", "%");
                        whereStrings.add(String.format("%s LIKE ?", aliasedColumnName));
                    }else{
                        whereStrings.add(String.format("%s = ?", aliasedColumnName));
                    }

                    if (!FXPropertyUtils.isPrimitive(fieldType) && fieldType.isAssignableFrom(value.getClass())) {
                        // Need to to get its ID value
                        List<Field> fieldTypeIdFields = FXPropertyUtils.getFieldsOfDirectClassOnlyWithAnnotation(fieldType, Id.class);

                        try {
                            objects.add(PropertyUtils.getProperty(value, fieldTypeIdFields.get(0).getName()));
                        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                            e.printStackTrace();
                            log.error("did not find id field of provided Where Object");
                        }
                    } else {
                        objects.add(value);
                    }
                }
            }else{
                whereStrings.add(String.format("%s IS NULL", aliasedColumnName));
            }



        });
//        List<String> whereStrings = fieldValueMap.keySet().stream().map(key -> String.format("%s = ?", key)).collect(Collectors.toList());

        String orderBy = generatedOrderBy(sort);

        return WildeDBApplication.getInstance().query(persistentClass, lazyFieldsJoinEagerly, lazyFieldsFetchEagerly, limit, String.join(" AND ", whereStrings) + orderBy, objects);

    }

    @Override
    public List<T> findBy(Map<String, Object> fieldValueMap) throws SQLException {
        return findBy(fieldValueMap, null, null);
    }

    private String convertFieldNameToAliasedColumn(String fieldName){
        // Get field to get annotation
        List<String> hierarchy = new ArrayList<>(Arrays.asList(fieldName.split("\\.")));
//            hierarchy.remove(0);

        // Create the alias
        String alias = persistentClass.getSimpleName();
        Class<?> currentClass = persistentClass;
        log.debug("hierarchy size for {} is {}", fieldName, hierarchy.size());
        if(hierarchy.size() > 1) {
//            String childField = hierarchy.get(0);
            currentClass = FXPropertyUtils.getFieldType(currentClass, hierarchy.get(0));
            for (int i = 1; i < hierarchy.size(); i++) {
                String childField = hierarchy.get(i);
                log.debug("checking child field: {}, of class {}", childField, currentClass.getSimpleName());
                // Check if it's of the class or if it's instead a parent class
                if (Arrays.stream(currentClass.getDeclaredFields())
                        .anyMatch(f -> f.getName().equals(childField))) {

                    log.debug("Current class contained field {}", childField);

                    // if the field is the id
                    alias = String.format("%s_%s", alias, hierarchy.get(i-1));

                } else {
                    // Need to recursively check parent classes and append Alias while doing so
                    Class<?> parentClass = currentClass.getSuperclass();
                    int level = 1;
                    while (parentClass != null) {
                        alias = String.format("%s_%s_%s", alias, "parent", level);
                        if (Arrays.stream(parentClass.getDeclaredFields())
                                .anyMatch(f -> f.getName().equals(childField))) {
                            break;
                        }
                        level++;
                        parentClass = parentClass.getSuperclass();
                    }
                }
                currentClass = FXPropertyUtils.getFieldType(currentClass, childField);
            }
        }

        // We could get the field's column, but it won't be the fully qualified alias + columnName
        Field field = FXPropertyUtils.getField(persistentClass, fieldName);

        Column column = field.getAnnotation(Column.class);
        JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
        String columnName = column != null ? column.name() : joinColumn.name();
        String aliasedColumnName = alias + "." + columnName;

        return aliasedColumnName;
    }

    private String generatedOrderBy(String sort){
        String orderBy = "";
        if(sort != null && !sort.isBlank()){
            orderBy = " ORDER BY ";
            List<String> orderByParams = new ArrayList<>();

            String[] sortParams = sort.split(",");
            for (String sortParam : sortParams) {
                sortParam = sortParam.trim();
                // Determine if a direction was provided
                String[] sortAndDirection = sortParam.split("\\s+");

                String fieldName;
                String direction;
                if(sortAndDirection.length == 2){
                    fieldName = sortAndDirection[0].trim();
                    direction = sortAndDirection[1].trim();
                }else{
                    fieldName = sortParam;
                    direction = "ASC";
                }

                // Get field to get annotation
                List<String> hierarchy = new ArrayList<>(Arrays.asList(fieldName.split("\\.")));
//            hierarchy.remove(0);

                // Create the alias
                String alias = persistentClass.getSimpleName();
                Class<?> currentClass = persistentClass;
                log.debug("hierarchy size for {} is {}", fieldName, hierarchy.size());
                if(hierarchy.size() > 1) {
                    currentClass = FXPropertyUtils.getFieldType(currentClass, hierarchy.get(0));
                    for (int i = 1; i < hierarchy.size(); i++) {
                        String childField = hierarchy.get(i);
                        log.debug("checking child field: {}, of class {}", childField, currentClass.getSimpleName());
                        // Check if it's of the class or if it's instead a parent class
                        if (Arrays.stream(currentClass.getDeclaredFields())
                                .anyMatch(f -> f.getName().equals(childField))) {

                            log.debug("Current class contained field {}", childField);

                            // if the field is the id
                            alias = String.format("%s_%s", alias, hierarchy.get(i-1));

                        } else {
                            // Need to recursively check parent classes and append Alias while doing so
                            Class<?> parentClass = currentClass.getSuperclass();
                            int level = 1;
                            while (parentClass != null) {
                                alias = String.format("%s_%s_%s", alias, "parent", level);
                                if (Arrays.stream(parentClass.getDeclaredFields())
                                        .anyMatch(f -> f.getName().equals(childField))) {
                                    break;
                                }
                                level++;
                                parentClass = parentClass.getSuperclass();
                            }
                        }
                        currentClass = FXPropertyUtils.getFieldType(currentClass, childField);
                    }
                }

                // We could get the field's column, but it won't be the fully qualified alias + columnName
                Field field = FXPropertyUtils.getField(persistentClass, fieldName);

                Column column = field.getAnnotation(Column.class);
                JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
                String columnName = column != null ? column.name() : joinColumn.name();
                String aliasedColumnName = alias + "." + columnName;

                orderByParams.add(aliasedColumnName + " " + direction);
            }

            orderBy = " ORDER BY " + String.join(", ", orderByParams);
        }else{
            DefaultSort defaultSort = persistentClass.getAnnotation(DefaultSort.class);

            if(defaultSort != null && Strings.isNotBlank(defaultSort.columns())){
                orderBy = " ORDER BY " + defaultSort.columns();
            }
        }

        log.debug("Creating Order by: {}", orderBy);
        return orderBy;
    }

    public void updateAll(Collection<T> entities) throws SQLException{
        WildeDBApplication.getInstance().update(new ArrayList<>(entities));
    }

    @Override
    public void updateAll(Collection<T> entities, String... fields) throws Exception {
        WildeDBApplication.getInstance().update(new ArrayList<>(entities), fields);
    }

    public void delete(T entity) throws SQLException{
        WildeDBApplication.getInstance().delete(entity);
    }

    public void deleteAll(Collection<T> entities) throws SQLException{
        WildeDBApplication.getInstance().delete(new ArrayList<T>(entities));
    }

    @Override
    public void fetchList(T entity, String fieldName) throws SQLException {
        WildeDBApplication.getInstance().fetchList(entity, fieldName);
    }

    @Override
    public void fetchAllLists(T entity, boolean async) throws SQLException {
        WildeDBApplication.getInstance().fetchAllLists(entity, async);
    }

    @Override
    public void fetchEagerLists(T entity, boolean async) throws Exception {
        WildeDBApplication.getInstance().fetchEagerLists(entity, async);
    }

    public void setPersistentClass(Class<T> persistentClass) {
        this.persistentClass = persistentClass;
    }
}

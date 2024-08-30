package com.thomaswilde.api;

import com.thomaswilde.lucene_desktop.SearchResultSummary;
import com.thomaswilde.lucene_desktop.SearchResultWrapper;
import com.thomaswilde.util.SqlItem;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ApiMethods<T, ID> {
    T getById(ID id) throws Exception;
    List<T> get() throws Exception;
    List<T> get(String sort) throws Exception;
    List<T> get(String methodName, Object... values) throws Exception;
    List<T> query(String query, Object... values) throws Exception;
    List<T> querySort(String query, String sort, Object... values) throws Exception;
    List<T> get(Map<String, String> fieldValueMap) throws Exception;
    List<T> get(Collection<SqlItem> sqlItems) throws Exception;
    List<T> get(Collection<SqlItem> sqlItems, String sort) throws Exception;
    T getTop1(Map<String, String> fieldValueMap) throws Exception;
    void refreshBean(T bean) throws Exception;
    T post(T t) throws Exception;
    T put(T t) throws Exception;
    void putAll(Collection<T> t) throws Exception;
    T patch(T t, String... fields) throws Exception;
    void delete(T t) throws Exception;
    void deleteAll(Collection<T> t) throws Exception;

    void fetchList(T entity, String fieldName) throws Exception;
    void fetchAllLists(T entity, boolean async) throws Exception;
    void fetchEagerLists(T entity, boolean async) throws Exception;

    List<SearchResultWrapper<T>> search(String search) throws Exception;
    SearchResultSummary<T> search(String search, int page, int size) throws Exception;
}

package com.thomaswilde.wildebeans.application;

import com.google.common.reflect.ClassPath;
import com.thomaswilde.api.DefaultEndPoint;
import com.thomaswilde.fxcore.TablePreferences;
import com.thomaswilde.util.DesktopUtil;
import com.thomaswilde.wildebeans.GetComboBoxValues;
import com.thomaswilde.wildebeans.GetMoreInfoFactory;
import com.thomaswilde.wildebeans.UiBeanProperty;
import com.thomaswilde.wildebeans.WildeBeanProperty;
import com.thomaswilde.wildebeans.WildeEditableBeanTable;
import com.thomaswilde.wildebeans.database.DbMethods;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Table;

import javafx.scene.control.TableView;
import javafx.util.Callback;

public class WildeDBApplication implements
		WildeAppDBMethods,
		WildeAppCoreMethods,
		WildeAppPreferencesMethods,
		WildeEditableBeanTable.TableCommitCallback<Object>,
		Callback<WildeBeanProperty, WildeBeanProperty.GetMoreInfoCallback>,
		ApiRequestHeadersCallback,
		GetComboBoxValues
{

	private static final Logger log = LoggerFactory.getLogger(WildeDBApplication.class);

	private WildeAppDBMethods wildeAppDBMethods;
	private WildeAppCoreMethods wildeAppCoreMethods;
	private WildeAppPreferencesMethods wildeAppPreferencesMethods;
	private ApiRequestHeadersCallback apiRequestHeadersCallback;
	private WildeEditableBeanTable.TableCommitCallback<Object> wildeTableCommitCallback;

	private GetMoreInfoFactory getMoreInfoFactory;
	private GetComboBoxValues getComboBoxValuesFactory;
	
	private static WildeDBApplication wildeDBApplication;

	public String baseApiUrl;

	private Map<Class<?>, DbMethods<?, ?>> entityRepositoryMap = new HashMap<>();

	public Map<Class<?>, DbMethods<?, ?>> getEntityRepositoryMap() {
		return entityRepositoryMap;
	}

	public<T, ID> DbMethods<T, ID> getRepository(Class<T> clazz){
		return (DbMethods<T, ID>) entityRepositoryMap.get(clazz);
	}

	public void registerRepository(Class<?> clazz, DbMethods<?, ?> repository){
		entityRepositoryMap.put(clazz, repository);
	}

	@Override
	public Map<String, String> getApiRequestHeaders() {
		if(apiRequestHeadersCallback != null){
			return apiRequestHeadersCallback.getApiRequestHeaders();
		}
		return Map.of();
	}

	public enum DataRetrievalType{
		DATABASE, API
	}

	private DataRetrievalType dataRetrievalType = DataRetrievalType.DATABASE;
	public DataRetrievalType getDataRetrievalType() {
		return dataRetrievalType;
	}

	public void setDataRetrievalType(DataRetrievalType dataRetrievalType) {
		this.dataRetrievalType = dataRetrievalType;
	}
	
	private WildeDBApplication() {
		System.getProperties().setProperty("oracle.jdbc.J2EE13Compliant", "true");
	}



	public static WildeDBApplication getInstance() {
		if(wildeDBApplication == null) {
			wildeDBApplication = new WildeDBApplication();
		}
		
		return wildeDBApplication;
	}

	public void setWildeAppDBMethods(WildeAppDBMethods wildeAppDBMethods) {
		this.wildeAppDBMethods = wildeAppDBMethods;
	}

	public void setWildeAppCoreMethods(WildeAppCoreMethods wildeAppCoreMethods) {
		this.wildeAppCoreMethods = wildeAppCoreMethods;
	}

	public void setWildeAppPreferencesMethods(WildeAppPreferencesMethods wildeAppPreferencesMethods) {
		this.wildeAppPreferencesMethods = wildeAppPreferencesMethods;
	}

	public void setWildeTableCommitCallback(WildeEditableBeanTable.TableCommitCallback<Object> wildeTableCommitCallback) {
		this.wildeTableCommitCallback = wildeTableCommitCallback;
	}

	public User getUserInterface() {
		if(wildeAppCoreMethods != null){
			return wildeAppCoreMethods.getUserInterface();
		}
		return null;
	}

	@Override
	public Path getApplicationJarPath() {
		if(wildeAppCoreMethods != null){
			return wildeAppCoreMethods.getApplicationJarPath();
		}
		try {
			return Paths.get(WildeDBApplication.class.getProtectionDomain().getCodeSource().getLocation()
					.toURI());
		} catch (URISyntaxException e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}
		return null;
	}

	@Override
	public Path getLogFilePath() {
		if(wildeAppCoreMethods != null){
			return wildeAppCoreMethods.getLogFilePath();
		}

		return getApplicationJarPath().getParent().resolveSibling("logs").resolve("app.log");
	}

	@Override
	public <T> List<T> query(Class<T> clazz) throws SQLException {
		if(wildeAppDBMethods != null) {
			return wildeAppDBMethods.query(clazz);
		}
		return null;
	}

	@Override
	public <T> List<T> query(Class<T> clazz, String filter) throws SQLException{
		if(wildeAppDBMethods != null) {
			return wildeAppDBMethods.query(clazz, filter);
		}
		return null;
	}

	@Override
	public <T> List<T> query(Class<T> clazz, String filter, Object... whereObjects) throws SQLException {
		if(wildeAppDBMethods != null) {
			return wildeAppDBMethods.query(clazz, filter, whereObjects);
		}
		return null;
	}
	
	@Override
	public <T> List<T> query(Class<T> clazz, String filter, List<Object> whereObjects) throws SQLException{
		if(wildeAppDBMethods != null) {
			return wildeAppDBMethods.query(clazz, filter, whereObjects);
		}
		return null;
	}

	@Override
	public <T> List<T> query(Class<T> clazz, Collection<String> lazyJoinFields, Collection<String> lazyFetchFields, String filter, List<Object> whereObjects) throws SQLException {
		if(wildeAppDBMethods != null) {
			return wildeAppDBMethods.query(clazz, lazyJoinFields, lazyFetchFields, filter, whereObjects);
		}
		return null;
	}

	@Override
	public <T> List<T> query(Class<T> clazz, Collection<String> lazyJoinFields, Collection<String> lazyFetchFields, String filter, Object... whereObjects) throws SQLException {
		if(wildeAppDBMethods != null) {
			return wildeAppDBMethods.query(clazz, lazyJoinFields, lazyFetchFields, filter, whereObjects);
		}
		return null;
	}

	@Override
	public <T> List<T> query(Class<T> clazz, Collection<String> lazyJoinFields, Collection<String> lazyFetchFields, int limit, String filter, List<Object> whereObjects) throws SQLException {
		if(wildeAppDBMethods != null) {
			return wildeAppDBMethods.query(clazz, lazyJoinFields, lazyFetchFields, limit, filter, whereObjects);
		}
		return null;
	}

	@Override
	public <T> List<T> query(Class<T> clazz, String tableName, String filter, List<Object> whereObjects) throws SQLException {
		if(wildeAppDBMethods != null) {
			return wildeAppDBMethods.query(clazz, tableName, filter, whereObjects);
		}
		return null;
	}

	@Override
	public <T> List<T> findBySuggestibleMethod(Class<T> clazz, String methodName, Object... values) throws SQLException {
		if(wildeAppDBMethods != null) {
			return wildeAppDBMethods.findBySuggestibleMethod(clazz, methodName, values);
		}
		return null;
	}

	@Override
	public <T> T queryTopOne(Class<T> clazz, String filter, Object... whereObjects) throws SQLException{
		if(wildeAppDBMethods != null) {
			return wildeAppDBMethods.queryTopOne(clazz, filter, whereObjects);
		}
		return null;
	}

	@Override
	public <T> void refreshBean(T bean, String filter, Object... whereObjects) throws SQLException{
		if(wildeAppDBMethods != null) {
			wildeAppDBMethods.refreshBean(bean, filter, whereObjects);
		}
		
	}

	@Override
	public <T> void refreshBean(T bean) throws SQLException{
		if(wildeAppDBMethods != null) {
			wildeAppDBMethods.refreshBean(bean);
		}
	}

	@Override
	public <T> void insert(T bean) throws SQLException{
		if(wildeAppDBMethods != null) {
			wildeAppDBMethods.insert(bean);
		}
	}

	@Override
	public <T> void insert(Collection<T> beans) throws SQLException{
		if(wildeAppDBMethods != null) {
			wildeAppDBMethods.insert(beans);
		}		
	}

	@Override
	public <T> void update(T bean) throws SQLException{
		if(wildeAppDBMethods != null) {
			wildeAppDBMethods.update(bean);
		}
	}

	@Override
	public <T> void update(List<T> beans) throws SQLException{
		if(wildeAppDBMethods != null) {
			wildeAppDBMethods.update(beans);
		}
	}

	@Override
	public <T> void update(T bean, String... fieldsToUpdate) throws SQLException{
		if(wildeAppDBMethods != null) {
			wildeAppDBMethods.update(bean, fieldsToUpdate);
		}
	}

	@Override
	public <T> void update(T bean, List<String> fieldsToUpdate) throws SQLException{
		if(wildeAppDBMethods != null) {
			wildeAppDBMethods.update(bean, fieldsToUpdate);
		}
	}

	@Override
	public <T> void update(List<T> beans, String... fieldsToUpdate) throws SQLException{
		if(wildeAppDBMethods != null) {
			wildeAppDBMethods.update(beans, fieldsToUpdate);
		}
	}

	@Override
	public <T> void update(List<T> beans, List<String> fieldsToUpdate) throws SQLException{
		if(wildeAppDBMethods != null) {
			wildeAppDBMethods.update(beans, fieldsToUpdate);
		}
	}

	@Override
	public <T> void delete(T bean) throws SQLException{
		if(wildeAppDBMethods != null) {
			wildeAppDBMethods.delete(bean);
		}
	}

	@Override
	public <T> void delete(List<T> beans) throws SQLException{
		if(wildeAppDBMethods != null) {
			wildeAppDBMethods.delete(beans);
		}
	}
	
	@Override
	public void delete(Class<?> clazz, String where, List<Object> whereObjects){
		if(wildeAppDBMethods != null) {
			wildeAppDBMethods.delete(clazz, where, whereObjects);
		}
	}

	@Override
	public <T> void fetchList(T entity, String fieldName) throws SQLException{
		if(wildeAppDBMethods != null) {
			wildeAppDBMethods.fetchList(entity, fieldName);
		}
	}

	@Override
	public <T> void fetchAllLists(T entity, boolean async) throws SQLException{
		if(wildeAppDBMethods != null) {
			wildeAppDBMethods.fetchAllLists(entity, async);
		}
	}

	@Override
	public <T> void fetchEagerLists(T entity, boolean async) throws SQLException {
		if(wildeAppDBMethods != null) {
			wildeAppDBMethods.fetchEagerLists(entity, async);
		}
	}

	@Override
	public void updateDirtyBean(Object bean, List<String> fieldsChanged) throws Exception{
//		if(wildeTableCommitCallback != null) {
//			wildeTableCommitCallback.updateDirtyBean(bean, fieldsChanged);
//		}
		RepositoryUtil.update(bean, fieldsChanged);
	}

	@Override
	public void addNewBeans(List<Object> beans) throws Exception{
//		if(wildeTableCommitCallback != null) {
//			wildeTableCommitCallback.addNewBeans(beans);
//		}
		for(Object bean: beans){
			RepositoryUtil.insert(bean);
		}
	}

	@Override
	public void deleteBeans(List<Object> beans) throws Exception{
//		if(wildeTableCommitCallback != null) {
//			wildeTableCommitCallback.deleteBeans(beans);
//		}
		RepositoryUtil.delete(beans);
	}



	@Override
	public String getApplicationName() {
		if(wildeAppCoreMethods != null) {
			return wildeAppCoreMethods.getApplicationName();
		}
		return "Wilde Application";
	}

	@Override
	public String getVersionName() {
		if(wildeAppCoreMethods != null) {
			return wildeAppCoreMethods.getVersionName();
		}
		return "1.0.0";
	}

	@Override
	public int getVersionNumber() {
		if(wildeAppCoreMethods != null) {
			return wildeAppCoreMethods.getVersionNumber();
		}
		return 0;
	}

	@Override
	public void savePreferences() {
		if(wildeAppPreferencesMethods != null) {
			wildeAppPreferencesMethods.savePreferences();
		}
	}

	@Override
	public boolean customFileExplorer() {
		if(wildeAppPreferencesMethods != null) {
			return wildeAppPreferencesMethods.customFileExplorer();
		}
		return false;
	}

	@Override
	public String getCustomFileExplorerAppPath() {
		if(wildeAppPreferencesMethods != null) {
			return wildeAppPreferencesMethods.getCustomFileExplorerAppPath();
		}
		return null;
	}

	public void openInPreferredFileViewer(Path path){
		if(Files.isDirectory(path) && getInstance().customFileExplorer() && getInstance().customFileExplorer()){
			try {
				Runtime.getRuntime().exec(getInstance().getCustomFileExplorerAppPath() + " \"" + path + "\"");
			} catch (IOException e) {
				e.printStackTrace();
				DesktopUtil.open(path);
			}
		}else{
			DesktopUtil.open(path);
		}
	}

	@Override
	public Map<String, String> getWildePropertySheetModePreferences() {
		if(wildeAppPreferencesMethods != null) {
			return wildeAppPreferencesMethods.getWildePropertySheetModePreferences();
		}
		return new HashMap<>();
	}

	@Override
	public Map<String, Boolean> getWildePropertySheetFloatingLabels() {
		if(wildeAppPreferencesMethods != null) {
			return wildeAppPreferencesMethods.getWildePropertySheetFloatingLabels();
		}
		return new HashMap<>();
	}

	@Override
	public void saveTableColumnSelections(TableView<?> tableView) {
		if(wildeAppPreferencesMethods != null) {
			wildeAppPreferencesMethods.saveTableColumnSelections(tableView);
		}
	}

	@Override
	public List<TablePreferences> getTablePreferencesList() {
		if(wildeAppPreferencesMethods != null) {
			wildeAppPreferencesMethods.getTablePreferencesList();
		}
		return new ArrayList<>();
	}

	@Override
	public WildeBeanProperty.GetMoreInfoCallback call(WildeBeanProperty param) {
		if(getMoreInfoFactory == null){
			return null;
		}else{
			return getMoreInfoFactory.call(param);
		}
	}

	@Override
	public List<?> getComboBoxValues(UiBeanProperty<?> wildeBeanProperty, String entry) {
		if(this.getComboBoxValuesFactory != null){
			return this.getComboBoxValuesFactory.getComboBoxValues(wildeBeanProperty, entry);
		}
		return null;
	}

	public void setGetComboBoxValuesFactory(GetComboBoxValues getComboBoxValuesFactory) {
		this.getComboBoxValuesFactory = getComboBoxValuesFactory;
	}

	public void setGetMoreInfoFactory(GetMoreInfoFactory getMoreInfoFactory) {
		this.getMoreInfoFactory = getMoreInfoFactory;
	}

	public void registerRepositories(String basePackageFilter, String entityPackageName, String repositoryPackageName){
		long startTime = System.nanoTime();

		try {

//            Set<Class<?>> entityClasses = findAllClassesUsingGoogleGuice("com.thomaswilde.matdb.objects", Table.class);
			Set<Class<?>> entityClasses = new HashSet<>();
//            Set<Class<?>> repositoryClasses = findAllClassesUsingGoogleGuice("com.thomaswilde.matdb.services.repositories");
			Set<Class<?>> repositoryClasses = new HashSet<>();

			Set<ClassPath.ClassInfo> allClassInfo = getAllClassInfo(basePackageFilter);
			log.info("Found {} class with provided base entity package name", allClassInfo.size());
			for(var classInfo : allClassInfo){
				if(classInfo.getPackageName().startsWith(entityPackageName)){
					Class<?> possibleEntity = classInfo.load();
					if(possibleEntity.getAnnotation(Table.class) != null){
						log.debug("Adding repository: {}", possibleEntity);
						entityClasses.add(possibleEntity);
					}
				}else if(classInfo.getPackageName().startsWith(repositoryPackageName)){
					repositoryClasses.add(classInfo.load());
				}
			}


			Map<String, Class<?>> entityClassNameMap = new HashMap<>();
			entityClasses.forEach(aClass -> entityClassNameMap.put(aClass.getSimpleName(), aClass));


			repositoryClasses.forEach(repositoryClass -> {
				String className = repositoryClass.getSimpleName().replace("Repository", "");
				try {

					Method getInstance = repositoryClass.getMethod("getInstance");

					log.debug("Registering custom repository for {}", className);

					Object repository = getInstance.invoke(null);
					// Check that the repository is really a repository
					if(repository instanceof DbMethods<?,?>){
						registerRepository(entityClassNameMap.get(className), (DbMethods<?, ?>) repository);
					}else{
						log.warn("{} does not fully implement the methods of a repository and will not be added to app repositories", className);
					}





				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			});

			log.trace("Creating repositories for entities not explicitly created for");
			entityClasses.forEach(entityClass -> {
				if(!getEntityRepositoryMap().containsKey(entityClass)){
					DefaultEndPoint defaultEndPoint = entityClass.getAnnotation(DefaultEndPoint.class);
					if(defaultEndPoint != null){
						registerNewRepository(entityClass, defaultEndPoint.value());
					}else{
						log.warn("Registering repository with no default end point for entity: {}", entityClass.getSimpleName());
						registerNewRepository(entityClass, "");
					}

				}
			});

		} catch (IOException e) {
			e.printStackTrace();
			log.error("Unable to retrieve package classes");
		}

		long endTime = System.nanoTime();
		log.debug("Package detection took {} ms", (endTime - startTime)/Math.pow(10,6));
	}

	private void registerNewRepository(Class<?> clazz, String endPoint){
		registerRepository(clazz, getNewRepository(clazz, endPoint));
	}

	private <T, ID> DbMethods<T, ID> getNewRepository(Class<T> clazz, String endPoint){
		return SimpleApiAndDatabaseRepository.newRepository(clazz, getBaseApiUrl(), endPoint, this::getApiRequestHeaders);
	}

	private static Set<ClassPath.ClassInfo> getAllClassInfo(String packageName) throws IOException {
		return ClassPath.from(ClassLoader.getSystemClassLoader())
				.getTopLevelClasses()
				.stream()
				.filter(clazz -> clazz.getPackageName()
						.startsWith(packageName))
				.collect(Collectors.toSet());
	}

	public String getBaseApiUrl() {
		return baseApiUrl;
	}

	public void setBaseApiUrl(String baseApiUrl) {
		this.baseApiUrl = baseApiUrl;
	}

	public ApiRequestHeadersCallback getApiRequestHeadersCallback() {
		return apiRequestHeadersCallback;
	}

	public void setApiRequestHeadersCallback(ApiRequestHeadersCallback apiRequestHeadersCallback) {
		this.apiRequestHeadersCallback = apiRequestHeadersCallback;
	}
}

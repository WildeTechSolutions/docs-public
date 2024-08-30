package com.thomaswilde.wildebeans.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ClassDescriptor {

    public String displayName() default "";

    public String shortDescription() default "";

    @Deprecated
    public String sqlReadTable() default "";

    @Deprecated
    public String sqlWriteTable() default "";

    @Deprecated
    public int fetchSize() default -1;

    @Deprecated
    public boolean hasNestedTableProperties() default false;
        
    public String[] orderedDisplayCategories() default {};
    
    public int defaultPropertyGridColumns() default 2;

    @Deprecated
    public boolean isSubClassOfDifferentDbTable() default false;
    
    public boolean isInsertable() default true;
    
    public String[] excludeWildeBeanProperties() default {};
    
    public String[] viewPermissions() default {};
    public String[] editPermissions() default {};
    public String[] createPermissions() default {};

    String[] fieldsToOmitFromSearch() default {};
    // this would be like a nested field within a OneToMany
    String[] fieldsToIncludeInSearch() default {};
    String[] fieldsToFetchForSearch() default {};
}

package grill24.sizzlib.persistence;

import com.google.gson.reflect.TypeToken;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Type;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)

public @interface Persists {
    String value() default "";
    Class[] genericTypes() default {};
}

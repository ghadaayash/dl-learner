package org.dllearner.core.config;

import java.beans.PropertyEditor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dllearner.algorithms.properties.PropertyDomainAxiomLearner;
import org.dllearner.core.Component;

public class ConfigHelper {
	
	public final static Map<Class<?>, Class<?>> map = new HashMap<Class<?>, Class<?>>();
	
	static {
		map.put(Boolean.class, boolean.class);
		map.put(Byte.class, byte.class);
		map.put(Short.class, short.class);
		map.put(Character.class, char.class);
		map.put(Integer.class, int.class);
		map.put(Long.class, long.class);
		map.put(Float.class, float.class);
		map.put(Double.class, double.class);
	}
	
	/**
	 * Configures the given component by setting the value for the appropriate config option.
	 * @param component the component to be configured
	 * @param configName the name of the config option
	 * @param configValue the value of the config option
	 */
	public static void configure(Component component, String configName, String configValue){
		Field[] fields = component.getClass().getDeclaredFields();
        for(Field f : fields){
        	ConfigOption option = f.getAnnotation(ConfigOption.class);
        	if(option != null){
        		if(option.name().equals(configName)){
        			try {
						PropertyEditor editor = (PropertyEditor) option.propertyEditorClass().newInstance();
						editor.setAsText(configValue);
						Method method = component.getClass().getMethod("set" + Character.toUpperCase(f.getName().charAt(0)) + f.getName().substring(1), getClassForObject(editor.getValue()));
						method.invoke(component, editor.getValue());
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (InstantiationException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					} catch (SecurityException e) {
						e.printStackTrace();
					} catch (NoSuchMethodException e) {
						e.printStackTrace();
					}
        		}
        		
        	}
        }
	}
	
	/**
	 * Returns all config options for the given component.
	 * @param component
	 * @return
	 */
	public static List<ConfigOption> getConfigOptions(Component component){
		List<ConfigOption> options = new ArrayList<ConfigOption>();
		
		Field[] fields = component.getClass().getDeclaredFields();
		for(Field f : fields){
        	ConfigOption option = f.getAnnotation(ConfigOption.class);
        	if(option != null){
        		options.add(option);
        	}
        }
		
		return options;
	}
	
	private static Class<?> getClassForObject(Object obj){
		if(map.containsKey(obj.getClass())){
			return map.get(obj.getClass());
		} else {
			return obj.getClass();
		}
	}
	
	public static void main(String[] args) {
		PropertyDomainAxiomLearner l = new PropertyDomainAxiomLearner(null);
		ConfigHelper.configure(l, "maxExecutionTimeInSeconds", "11");
		System.out.println(l.getMaxExecutionTimeInSeconds());
	}

}

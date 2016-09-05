package de.prob2.ui.groovy

import javafx.collections.ObservableList;

class MetaPropertiesHandler {

	public handleProperties(Object object, ObservableList<GroovyClassPropertyItem> properties) {
		for(PropertyValue p : object.getMetaPropertyValues()) {
			properties.add(new GroovyClassPropertyItem(p));
		}
	}
	
	public handleMethods(Object object, ObservableList<GroovyClassPropertyItem> methods) {
		for(MetaMethod m : object.getMetaClass().getMetaMethods()) {
			methods.add(new GroovyClassPropertyItem(m));
		}
	}
	
	
}
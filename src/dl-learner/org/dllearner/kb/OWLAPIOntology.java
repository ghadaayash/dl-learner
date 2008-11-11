package org.dllearner.kb;

import java.io.File;
import java.net.URI;

import org.dllearner.core.KnowledgeSource;
import org.dllearner.core.OntologyFormat;
import org.dllearner.core.configurators.OWLAPIOntologyConfigurator;
import org.dllearner.core.options.ConfigEntry;
import org.dllearner.core.options.InvalidConfigOptionValueException;
import org.dllearner.core.owl.KB;
import org.semanticweb.owl.model.OWLOntology;

public class OWLAPIOntology extends KnowledgeSource {

	private OWLAPIOntologyConfigurator configurator;
	@Override
	public OWLAPIOntologyConfigurator getConfigurator(){
		return configurator;
	}
	
	private OWLOntology ontology;
	
	public OWLAPIOntology() {
		this(null);
	}
	
	public OWLAPIOntology(OWLOntology onto)
	{
		this.ontology = onto;
		this.configurator = new OWLAPIOntologyConfigurator(this);
	}
	
	public static String getName() {
		return "OWL API Ontology";
	}
	
	@Override
	public <T> void applyConfigEntry(ConfigEntry<T> entry) throws InvalidConfigOptionValueException 
	{
		
	}
	
	public OWLOntology getOWLOntolgy()
	{
		return ontology;
	}
	
	@Override
	public KB toKB()
	{
		throw new Error("OWL -> KB conversion not implemented yet.");
	}
	
	@Override
	public void init()
	{
		
	}
	
	@Override
	public void export(File file, OntologyFormat format)
	{
		
	}
	
	@Override
	public String toDIG(URI kbURI)
	{
		return null;
	}
}

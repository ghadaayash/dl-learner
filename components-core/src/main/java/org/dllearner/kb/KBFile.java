/**
 * Copyright (C) 2007-2011, Jens Lehmann
 *
 * This file is part of DL-Learner.
 *
 * DL-Learner is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * DL-Learner is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.dllearner.kb;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.dllearner.core.ComponentAnn;
import org.dllearner.core.ComponentInitException;
import org.dllearner.core.AbstractKnowledgeSource;
import org.dllearner.core.configurators.KBFileConfigurator;
import org.dllearner.core.options.ConfigEntry;
import org.dllearner.core.options.ConfigOption;
import org.dllearner.core.options.InvalidConfigOptionValueException;
import org.dllearner.core.options.URLConfigOption;
import org.dllearner.core.owl.KB;
import org.dllearner.parser.KBParser;
import org.dllearner.parser.ParseException;
import org.dllearner.reasoning.DIGConverter;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;


/**
 * KB files are an internal convenience format used in DL-Learner. Their
 * syntax is close to Description Logics and easy to use. KB files can be
 * exported to OWL for usage outside of DL-Learner.
 * 
 * @author Jens Lehmann
 *
 */
@ComponentAnn(name = "KB file", shortName = "kbfile", version = 0.8)
public class KBFile extends AbstractKnowledgeSource {

	private static Logger logger = Logger.getLogger(KBFile.class);
	
	private KB kb;

	@org.dllearner.core.config.ConfigOption(name = "url", description = "URL pointer to the KB file", defaultValue = "", required = false, propertyEditorClass = StringTrimmerEditor.class)
    private String url;

	/**
	 * Default constructor (needed for reflection in ComponentManager).
	 */
	public KBFile() {
	}
	
	/**
	 * Constructor allowing you to treat an already existing KB object
	 * as a KBFile knowledge source. Use it sparingly, because the
	 * standard way to create components is via 
	 * {@link org.dllearner.core.ComponentManager}.
	 * 
	 * @param kb A KB object.
	 */
	public KBFile(KB kb) {
		this.kb = kb;
	}
	
	public static String getName() {
		return "KB file";
	}
	
    @Override
    public void init() throws ComponentInitException {
        try {
            if (getUrl() != null) {
                kb = KBParser.parseKBFile(getUrl());
                logger.trace("KB File " + getUrl() + " parsed successfully.");
            } else {
                throw new ComponentInitException("No URL option or kb object given. Cannot initialise KBFile component.");
            }

        } catch (ParseException e) {
            throw new ComponentInitException("KB file " + getUrl() + " could not be parsed correctly.", e);
        }
    }

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.dllearner.core.KnowledgeSource#toDIG()
	 */
	@Override
	public String toDIG(URI kbURI) {
		return DIGConverter.getDIGString(kb, kbURI).toString();
	}
	
	@Override
	public String toString() {
		if(kb==null)
			return "KB file (not initialised)";
		else
			return kb.toString();
	}
	
	@Override
	public void export(File file, org.dllearner.core.OntologyFormat format){
		kb.export(file, format);
	}
	
	public String getUrl() {
		return url;
	}

	@Override
	public KB toKB() {
		return kb;
	}


    public void setUrl(String url) {
        this.url = url;
    }
}

package org.edu_sharing.alfresco.store;

import java.util.ArrayList;

import de.acosix.alfresco.simplecontentstores.repo.store.routing.SelectorPropertyContentStore;
import org.alfresco.repo.content.ContentStore;
import org.alfresco.repo.content.filestore.FileContentStore;
import org.alfresco.repo.dictionary.constraint.ListOfValuesConstraint;
import org.alfresco.repo.node.NodeServicePolicies.BeforeRemoveAspectPolicy;
import org.alfresco.repo.node.NodeServicePolicies.OnAddAspectPolicy;
import org.alfresco.repo.node.NodeServicePolicies.OnUpdatePropertiesPolicy;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.service.cmr.dictionary.ClassDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.PropertyCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Edu_SharingSelectorPropertyContentStore extends SelectorPropertyContentStore {

	
	private static final Logger LOGGER = LoggerFactory.getLogger(Edu_SharingSelectorPropertyContentStore.class);
	
	
	FileContentStore edu_SharingScopeFileStore;
	String scope;
	
	public Edu_SharingSelectorPropertyContentStore() {
		LOGGER.error("Constructor called");
		
		
	}
	
	public void init(){
		LOGGER.error("Init called");
	}
	
	public void setScope(String scope) {
		this.scope = scope;
	}
	
	public void setEdu_SharingScopeFileStore(FileContentStore edu_SharingScopeFileStore) {
		this.edu_SharingScopeFileStore = edu_SharingScopeFileStore;
	}
	
	
	
	@Override
	public void afterPropertiesSet() {
		LOGGER.error("afterPropertiesSet called");
		
		afterPropertiesSet_validateSelectors();
		afterPropertiesSet_setupStoreData();
		afterPropertiesSet_setupChangePolicies();
		afterPropertiesSet_setupConstraint();
	}
	
	 private void afterPropertiesSet_validateSelectors()
	    {
	        PropertyCheck.mandatory(this, "selectorClassName", this.selectorClassName);
	        PropertyCheck.mandatory(this, "selectorPropertyName", this.selectorPropertyName);

	        this.selectorClassQName = QName.resolveToQName(this.namespaceService, this.selectorClassName);
	        this.selectorPropertyQName = QName.resolveToQName(this.namespaceService, this.selectorPropertyName);
	        PropertyCheck.mandatory(this, "selectorClassQName", this.selectorClassQName);
	        PropertyCheck.mandatory(this, "selectorPropertyQName", this.selectorPropertyQName);

	        final ClassDefinition classDefinition = this.dictionaryService.getClass(this.selectorClassQName);
	        if (classDefinition == null)
	        {
	            throw new IllegalStateException(this.selectorClassName + " is not a valid content model class");
	        }

	        final PropertyDefinition propertyDefinition = this.dictionaryService.getProperty(this.selectorPropertyQName);
	        if (propertyDefinition == null || !DataTypeDefinition.TEXT.equals(propertyDefinition.getDataType().getName())
	                || propertyDefinition.isMultiValued())
	        {
	            throw new IllegalStateException(
	                    this.selectorPropertyName + " is not a valid content model property of type single-valued d:text");
	        }
	    }

	    private void afterPropertiesSet_setupStoreData()
	    {
	        PropertyCheck.mandatory(this, "storeBySelectorPropertyValue", this.storeBySelectorPropertyValue);
	        if (this.storeBySelectorPropertyValue.isEmpty())
	        {
	            throw new IllegalStateException("No stores have been defined for property values");
	        }

	        this.allStores = new ArrayList<>();
	        for (final ContentStore store : this.storeBySelectorPropertyValue.values())
	        {
	            if (!this.allStores.contains(store))
	            {
	                this.allStores.add(store);
	            }
	        }

	        if (!this.allStores.contains(this.fallbackStore))
	        {
	            this.allStores.add(this.fallbackStore);
	        }
	    }
	
	private void afterPropertiesSet_setupChangePolicies()
    {
        if (this.moveStoresOnChangeOptionPropertyName != null)
        {
            this.moveStoresOnChangeOptionPropertyQName = QName.resolveToQName(this.namespaceService,
                    this.moveStoresOnChangeOptionPropertyName);
            PropertyCheck.mandatory(this, "moveStoresOnChangeOptionPropertyQName", this.moveStoresOnChangeOptionPropertyQName);

            final PropertyDefinition moveStoresOnChangeOptionPropertyDefinition = this.dictionaryService
                    .getProperty(this.moveStoresOnChangeOptionPropertyQName);
            if (moveStoresOnChangeOptionPropertyDefinition == null
                    || !DataTypeDefinition.BOOLEAN.equals(moveStoresOnChangeOptionPropertyDefinition.getDataType().getName())
                    || moveStoresOnChangeOptionPropertyDefinition.isMultiValued())
            {
                throw new IllegalStateException(this.moveStoresOnChangeOptionPropertyName
                        + " is not a valid content model property of type single-valued d:boolean");
            }
        }

        if (this.moveStoresOnChange || this.moveStoresOnChangeOptionPropertyQName != null)
        {
            this.policyComponent.bindClassBehaviour(OnUpdatePropertiesPolicy.QNAME, this.selectorClassQName,
                    new JavaBehaviour(this, "onUpdateProperties", NotificationFrequency.EVERY_EVENT));

            final ClassDefinition classDefinition = this.dictionaryService.getClass(this.selectorClassQName);
            if (classDefinition.isAspect())
            {
                this.policyComponent.bindClassBehaviour(BeforeRemoveAspectPolicy.QNAME, this.selectorClassQName,
                        new JavaBehaviour(this, "beforeRemoveAspect", NotificationFrequency.EVERY_EVENT));
                this.policyComponent.bindClassBehaviour(OnAddAspectPolicy.QNAME, this.selectorClassQName,
                        new JavaBehaviour(this, "onAddAspect", NotificationFrequency.EVERY_EVENT));
            }
        }
    }

    private void afterPropertiesSet_setupConstraint()
    {
        if (this.selectorValuesConstraintShortName != null && !this.selectorValuesConstraintShortName.trim().isEmpty())
        {
            final ListOfValuesConstraint lovConstraint = new ListOfValuesConstraint();
            lovConstraint.setShortName(this.selectorValuesConstraintShortName);
            lovConstraint.setRegistry(this.constraintRegistry);
            lovConstraint.setAllowedValues(new ArrayList<>(this.storeBySelectorPropertyValue.keySet()));
            lovConstraint.initialize();
        }
    }
	
}

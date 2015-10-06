package org.fiteagle.core.reservation;

import com.hp.hpl.jena.ontology.impl.ObjectPropertyImpl;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.rdf.model.impl.StatementImpl;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

import info.openmultinet.ontology.exceptions.InvalidModelException;
import info.openmultinet.ontology.vocabulary.Omn;
import info.openmultinet.ontology.vocabulary.Omn_component;
import info.openmultinet.ontology.vocabulary.Omn_domain_pc;
import info.openmultinet.ontology.vocabulary.Omn_federation;
import info.openmultinet.ontology.vocabulary.Omn_lifecycle;
import info.openmultinet.ontology.vocabulary.Omn_resource;

import org.fiteagle.api.core.*;
import org.fiteagle.core.tripletStoreAccessor.TripletStoreAccessor;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.xml.transform.ErrorListener;

/**
 * Created by dne on 15.02.15.
 */
public class ReservationHandler {
    private static final Logger LOGGER = Logger.getLogger(ReservationHandler.class.getName());

    public Message handleReservation(Model requestModel, String serialization, String requestID, JMSContext context) throws TripletStoreAccessor.ResourceRepositoryException {

        LOGGER.log(Level.INFO, "handle reservation ...");
        Message responseMessage = null;
        String errorMessage = checkReservationRequest(requestModel);
        if(errorMessage == null || errorMessage.isEmpty()){
          Model reservationModel = ModelFactory.createDefaultModel();
          createReservationModel(requestModel, reservationModel);
          reserve(reservationModel);
          String serializedResponse = MessageUtil.serializeModel(reservationModel, serialization);
          responseMessage = MessageUtil.createRDFMessage(serializedResponse, IMessageBus.TYPE_INFORM, null, serialization, requestID, context);
          try {
            responseMessage.setStringProperty(IMessageBus.MESSAGE_SOURCE, IMessageBus.SOURCE_RESERVATION);
          } catch (JMSException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
          }
        }
        else {
          responseMessage = MessageUtil.createErrorMessage(errorMessage, requestID, context);
        }
        return responseMessage;
    }

    private Model createReservationModel(Model requestModel, Model reservationModel) {
        
      Map<String, Resource> resourcesIDs = new HashMap<String, Resource>();
      Model assistantModel = ModelFactory.createDefaultModel();
      
        ResIterator iterator = requestModel.listResourcesWithProperty(RDF.type, Omn.Topology);
        while (iterator.hasNext()) {
            Resource topology = iterator.nextResource();
            assistantModel.add(topology, RDF.type, Omn.Topology);
           if (TripletStoreAccessor.exists(topology.getURI())) {
               LOGGER.log(Level.INFO, "Topology already exists");
               Model topologyModel = TripletStoreAccessor.getResource(topology.getURI());
               
               ResIterator iter = topologyModel.listResourcesWithProperty(RDF.type, Omn.Topology);
               while(iter.hasNext()){
                 Resource topo = iter.nextResource();
                 if(topo.hasProperty(MessageBusOntologyModel.endTime)){
                   Statement endTimeStmt = topo.getProperty(MessageBusOntologyModel.endTime);
                   assistantModel.add(topo, endTimeStmt.getPredicate(), endTimeStmt.getString());
                 }
                 
                 if(topo.hasProperty(Omn_lifecycle.hasAuthenticationInformation)){
                   Statement hasAuthenticationInformationStmt = topo.getProperty(Omn_lifecycle.hasAuthenticationInformation);
                   assistantModel.add(topo, hasAuthenticationInformationStmt.getPredicate(), hasAuthenticationInformationStmt.getObject());
                 }
               }

            }
        else {

               Resource newTopology = assistantModel.getResource(topology.getURI());
               Property property = assistantModel.createProperty(MessageBusOntologyModel.endTime.getNameSpace(), MessageBusOntologyModel.endTime.getLocalName());
               property.addProperty(RDF.type, OWL.FunctionalProperty);
               newTopology.addProperty(property, new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(getDefaultExpirationTime()));
               if (topology.getProperty(Omn_lifecycle.hasAuthenticationInformation) != null)
                   newTopology.addProperty(Omn_lifecycle.hasAuthenticationInformation, topology.getProperty(Omn_lifecycle.hasAuthenticationInformation).getObject());


           }

//            ResIterator resIter =  requestModel.listResourcesWithProperty(Omn.isResourceOf, topology);
            ResIterator resIter = requestModel.listSubjects();
            
            
            
            while(resIter.hasNext()){
                Resource resource = resIter.nextResource();
                
                if(resource.hasProperty(Omn.isResourceOf)){
                
                SimpleSelector selector= new SimpleSelector(resource, null,(Object) null);

                StmtIterator statementIter = requestModel.listStatements(selector);
                Resource newResource = assistantModel.createResource(resource.getNameSpace()+ UUID.randomUUID().toString());
                
                if(!resource.hasProperty(Omn_lifecycle.implementedBy)){
                  
                  newResource.addProperty(Omn_lifecycle.implementedBy, getAdapterForResource(requestModel, resource));
                  
                }

               resourcesIDs.put(resource.getURI(), newResource);
                
                while(statementIter.hasNext()){
                    Statement statement = statementIter.nextStatement();

                    newResource.addProperty(statement.getPredicate(), statement.getObject());
                    
                    
                    if(statement.getPredicate().equals(Omn_lifecycle.usesService)){
                        StmtIterator serviceModel =requestModel.listStatements(new SimpleSelector(statement.getObject().asResource(),null,(Object) null));
                        assistantModel.add(serviceModel);
                    }
               
                }
                assistantModel.add(topology, Omn.hasResource,newResource);
                
            } 
                else if(resource.hasProperty(Omn.hasResource)){
              
            } else {
                
//                if(resource.hasProperty(Omn.isResourceOf) || !resource.hasProperty(Omn.hasResource)){
                  StmtIterator stmtIterator = resource.listProperties();
                  while(stmtIterator.hasNext()){
                    Statement statement = stmtIterator.nextStatement();
                    assistantModel.add(statement);
                  }

                }
                
                
                
            }



        }

        
        ResIterator resIter = assistantModel.listSubjects();
        while(resIter.hasNext()){
          Resource res = resIter.nextResource();
          StmtIterator stmtIter = res.listProperties();
          while(stmtIter.hasNext()){
            Statement stmt = stmtIter.nextStatement();
            if("deployedOn".equals(stmt.getPredicate().getLocalName()) || "requires".equals(stmt.getPredicate().getLocalName())){
              Statement newStatement = new StatementImpl(stmt.getSubject(), stmt.getPredicate(), resourcesIDs.get(stmt.getObject().toString()));
              reservationModel.add(newStatement);
            }
            else{
              reservationModel.add(stmt);
            }
          }
        }
        
        

        
        return reservationModel;
    }

    
  private Resource getAdapterForResource(Model requestModel, Resource resource){
    
    Resource adapter = null;
    SimpleSelector typeSelector = new SimpleSelector(resource, RDF.type, (RDFNode) null);
    StmtIterator typeStatementIterator = requestModel.listStatements(typeSelector);
    
    while(typeStatementIterator.hasNext()){
      
      Statement typeStatement = typeStatementIterator.next();
      Resource typeResource = typeStatement.getObject().asResource();
      String typeURI = typeResource.getURI();
      
      Model adapterModel = TripletStoreAccessor.getResource(typeURI);
      
      if(modelHasProperty(adapterModel, typeResource)){
        
        SimpleSelector adapterInstanceSelector = new SimpleSelector(null, Omn_lifecycle.canImplement, (RDFNode) null);
        StmtIterator adapterInstanceIterator = adapterModel.listStatements(adapterInstanceSelector);
        
        while(adapterInstanceIterator.hasNext()){
          
          Statement adapterInstanceStatement = adapterInstanceIterator.nextStatement();
          String adapterInstanceURI = adapterInstanceStatement.getObject().asResource().getURI();
          
          if(typeURI.equals(adapterInstanceURI)){
            adapter = adapterInstanceStatement.getSubject();
            break;
          }
        }
      }
    }
    return adapter;
  }
  
  private boolean modelHasProperty(Model model, Resource value){
    if(!model.isEmpty() && model != null ){
      if(model.contains((Resource) null, Omn_lifecycle.canImplement, value)){
        return true;
      } else return false;
    } else return false;
      
  }

  /**
   * checks reservation request
   * @param requestModel
   * @return error message
   */
  private String checkReservationRequest(Model requestModel){
    
    final List<String> errorsList = new ArrayList<String>();
    
    ResIterator resIterator = requestModel.listResourcesWithProperty(Omn.isResourceOf); 
    
    while (resIterator.hasNext()) {
      Resource resource = resIterator.nextResource();
      
      checkResourceAdapterInstance(resource, requestModel, errorsList);
        
      
      
    }
    
    
    return getErrorMessage(errorsList);
  }
  
  private String getErrorMessage(final List<String> errorsList){
    String errorMessage = "";
    if(!errorsList.isEmpty()){
      for (String error : errorsList){
        errorMessage += error + " .";
      }
    }
      return errorMessage;
  }
  
  private void checkResourceAdapterInstance(Resource resource, Model requestModel, final List<String> errorList){
    
    RDFNode type = getResourceType(resource);
    
    if (resource.hasProperty(Omn_lifecycle.implementedBy)) {
      Object adapterInstance = resource.getProperty(Omn_lifecycle.implementedBy).getObject();
      checkResourceAdapterType(type, adapterInstance, errorList);
    }
    else {
      // check if resource's type is supported by any adapter instance.
      findAdapterSupportsType(resource, requestModel, errorList);
    }
  }
  
  /**
   * checks if resource type is supported by any adapter instance.
   * @param resource
   * @param requestModel
   * @param errorMessage
   */
  private void findAdapterSupportsType(Resource resource, Model requestModel, final List<String> errorList){
    SimpleSelector typeSelector = new SimpleSelector(resource, RDF.type, (RDFNode) null);
    StmtIterator typeStatementIterator = requestModel.listStatements(typeSelector);
    Boolean resourceFound = false;
    while(typeStatementIterator.hasNext()){
      Statement typeStatement = typeStatementIterator.next();
      Model model = TripletStoreAccessor.getResource(typeStatement.getObject().asResource().getURI());
      if(!model.isEmpty() && model != null && model.contains((Resource) null, Omn_lifecycle.canImplement, typeStatement.getObject().asResource())){
        resourceFound = true;
        break;
      }
    }
    if(!resourceFound){
      String errorMessage = "The requested resource " + resource.getLocalName() + " is not supported";
      errorList.add(errorMessage);
    }
  }
  
  /**
   * checks if resource type is supported by the requested adapter instance
   * @param type
   * @param adapterInstance
   * @param errorMessage
   */
  private void checkResourceAdapterType(RDFNode type, Object adapterInstance, final List<String> errorList){
    
    Model mo = ModelFactory.createDefaultModel();
    Resource re = mo.createResource(adapterInstance.toString());
    Model model = TripletStoreAccessor.getResource(re.getURI());
    if (model.isEmpty() || model == null) {
      errorList.add("The requested component id " + re.getURI() + " is not supported");
    } else 
      if(!model.contains(re, Omn_lifecycle.canImplement, type)){
        String errorMessage = "The requested sliver type " + type.toString()
            + " is not supported. Please see supported sliver types";
        errorList.add(errorMessage); 
      }
    
  }
  
  /**
   * 
   * @param resource
   * @return the type of the resource
   */
  private RDFNode getResourceType(Resource resource){
    RDFNode type = null;
    if (resource.hasProperty(RDF.type)) {
      
      StmtIterator stmtIterator = resource.listProperties(RDF.type);
      while (stmtIterator.hasNext()) {
        
        Statement statement = stmtIterator.nextStatement();
        if (!Omn_resource.Node.equals(statement.getObject())) {
          type = statement.getObject();
        }
      }
    }
    return type;
  }
  
    
    public void reserve(Model model) {


        ResIterator resIterator = model.listResourcesWithProperty(Omn.isResourceOf);
        while(resIterator.hasNext()){

            Resource requestedResource = resIterator.nextResource();
            Config config =new Config();
            Resource reservation = model.createResource(config.getProperty(IConfig.LOCAL_NAMESPACE).concat("reservation/")+ UUID.randomUUID().toString());
            reservation.addProperty(RDFS.label, reservation.getURI());
            reservation.addProperty(RDF.type,Omn.Reservation);
            requestedResource.addProperty(Omn.hasReservation, reservation);
            reservation.addProperty(Omn.isReservationOf, requestedResource);
            Date afterAdding2h = getDefaultExpirationTime();
            reservation.addProperty(MessageBusOntologyModel.endTime, new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(afterAdding2h));
            reservation.addProperty(Omn_lifecycle.hasReservationState, Omn_lifecycle.Allocated);
            Property property  = model.createProperty(Omn_lifecycle.hasState.getNameSpace(), Omn_lifecycle.hasState.getLocalName());
            property.addProperty(RDF.type, OWL.FunctionalProperty);
            requestedResource.addProperty(property,Omn_lifecycle.Uncompleted);


        }


        try {
            TripletStoreAccessor.addModel(model);


        } catch (TripletStoreAccessor.ResourceRepositoryException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
        } catch (InvalidModelException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
        }
    }


    private static Date getDefaultExpirationTime() {
        Date date = new Date();
        long t = date.getTime();
        return new Date(t + (120 * 60000));
    }

}

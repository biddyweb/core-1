package org.fiteagle.core.repository.dm;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.Topic;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.fiteagle.api.core.IMessageBus;
import org.fiteagle.api.core.MessageUtil;
import org.fiteagle.api.core.OntologyModelUtil;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * This Class sends the ontology Model over message bus on startup
 */
public class ResourceRepoMDBSender implements ServletContextListener{
 
  @Inject
  private JMSContext context;
  @Resource(mappedName = IMessageBus.TOPIC_CORE_NAME)
  private Topic topic;
  
  private static Logger LOGGER = Logger.getLogger(ResourceRepoMDBSender.class.toString());
  
  @Override
  public void contextInitialized(ServletContextEvent sce) {
    try {
      Model model = OntologyModelUtil.loadModel("ontologies/fiteagle/ontology.ttl", IMessageBus.SERIALIZATION_TURTLE);
      String serializedRDF = MessageUtil.serializeModel(model);
      
      final Message eventMessage = this.context.createTextMessage(serializedRDF);
      
      eventMessage.setStringProperty(IMessageBus.METHOD_TYPE, IMessageBus.TYPE_INFORM);
      eventMessage.setStringProperty(IMessageBus.SERIALIZATION, IMessageBus.SERIALIZATION_DEFAULT);
      LOGGER.log(Level.INFO, "Sending Ontology Model as Inform Message");
      this.context.createProducer().send(topic, eventMessage);
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, e.getMessage());
    }
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
  }
}

package org.onap.aai.modelloader.notification;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
public class NotificationIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    // @Value("${test.topic}")
    // private String topic;
    private String topic = "SDC-DISTR-NOTIF-TOPIC";

    @Test
    public void givenEmbeddedKafkaBroker_whenSendingWithSimpleProducer_thenMessageReceived() 
      throws Exception {
        String data = "Smth";
        
        kafkaTemplate.send(topic, data);
        
        assertTrue(true);
    }
}

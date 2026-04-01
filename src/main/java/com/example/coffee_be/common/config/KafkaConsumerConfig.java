package com.example.coffee_be.common.config;

import com.example.coffee_be.common.model.kafka.event.OrderCompletedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;
// 기존에 나열했던 복잡한 컨슈머컨피그들 중 공통 내용을 메서드로 뺌


@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // =====================================================================================
    // 공통 Consumer 설정 생성
    // =====================================================================================
    private Map<String, Object> baseConsumerProps(String groupId) {
        Map<String, Object> props = new HashMap<>();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        return props;
    }

    private ConsumerFactory<String, OrderCompletedEvent> buildConsumerFactory(String groupId) {
        JsonDeserializer<OrderCompletedEvent> deserializer = new JsonDeserializer<>(OrderCompletedEvent.class);
        deserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(
            baseConsumerProps(groupId),
            new StringDeserializer(),
            deserializer
        );
    }

    // 카프카리스너에서 에러가 감지되면 에러핸들러로 넘겨줌
    // 따라서 카프카리스너 어노테이션에 연결된 리스너컨테이너팩토리에 이 에러핸들러를 추가해야 함(setCommonErrorHandler)
    // 실패한 메시지를 브로커에 전달해주기 위해서 카프카템플릿 필요
    // =====================================================================================
    // 공통 DLT ErrorHandler
    // =====================================================================================
    @Bean
    public CommonErrorHandler commonErrorHandlerWithDLT(
        KafkaTemplate<String, OrderCompletedEvent> orderCompletedKafkaTemplate) {

        // 재처리했는데도 계속 실패한 경우 dlt 토픽으로 해당 메시지를 보낸다
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(orderCompletedKafkaTemplate);

        //실패하자마자 재처리할건지, 실패 후 기다렸다가 재처리할건지 결정
        // 디폴트 즉시10회( 재처리시도 9회)
        // → 1초 간격으로 2회 재시도 (총 3회)
        FixedBackOff backOff = new FixedBackOff(1000L, 2L);

        return new DefaultErrorHandler(recoverer, backOff);
    }


    // =====================================================================================
    // Product Ranking Consumer
    // =====================================================================================

    @Bean
    public ConsumerFactory<String, OrderCompletedEvent> productRankingConsumerFactory() {
        return buildConsumerFactory("product-ranking-group");
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderCompletedEvent> productRankingKafkaListenerContainerFactory(
        CommonErrorHandler commonErrorHandlerWithDLT) {

        var factory = new ConcurrentKafkaListenerContainerFactory<String, OrderCompletedEvent>();
        factory.setConsumerFactory(productRankingConsumerFactory());
        factory.setCommonErrorHandler(commonErrorHandlerWithDLT);
        return factory;
    }


    // =====================================================================================
    // Payment History Consumer
    // =====================================================================================

    @Bean
    public ConsumerFactory<String, OrderCompletedEvent> orderHistoryConsumerFactory() {
        return buildConsumerFactory("order-history-group");
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderCompletedEvent> orderHistoryKafkaListenerContainerFactory(
        CommonErrorHandler commonErrorHandlerWithDLT) {

        var factory = new ConcurrentKafkaListenerContainerFactory<String, OrderCompletedEvent>();
        factory.setConsumerFactory(orderHistoryConsumerFactory());
        factory.setConcurrency(9);
        factory.setCommonErrorHandler(commonErrorHandlerWithDLT);
        return factory;
    }


    // =====================================================================================
    // Delivery Consumer
    // =====================================================================================

    @Bean
    public ConsumerFactory<String, OrderCompletedEvent> deliveryConsumerFactory() {
        return buildConsumerFactory("delivery-group");
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderCompletedEvent> deliveryKafkaListenerContainerFactory(
        CommonErrorHandler commonErrorHandlerWithDLT) {

        var factory = new ConcurrentKafkaListenerContainerFactory<String, OrderCompletedEvent>();
        factory.setConsumerFactory(deliveryConsumerFactory());
        factory.setCommonErrorHandler(commonErrorHandlerWithDLT);
        return factory;
    }

}


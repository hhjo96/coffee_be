package com.example.coffee_be.common.config;

import com.example.coffee_be.common.model.kafka.event.OrderCompletedEvent;
import com.example.coffee_be.common.model.kafka.event.PaymentCompletedEvent;
import com.example.coffee_be.common.model.kafka.event.PointChargedEvent;
import com.example.coffee_be.common.model.kafka.event.PointUsedEvent;
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

// 추가할 경우 맨 상단 공통컨슈머설정 만들고 에러핸들러 만들고 컨슈머 만들면 됨
// 주문, 결제, 포인트충전, 포인트사용
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

    private ConsumerFactory<String, OrderCompletedEvent> orderBuildConsumerFactory(String groupId) {
        JsonDeserializer<OrderCompletedEvent> deserializer = new JsonDeserializer<>(OrderCompletedEvent.class);
        deserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(
            baseConsumerProps(groupId),
            new StringDeserializer(),
            deserializer
        );
    }

    private ConsumerFactory<String, PaymentCompletedEvent> paymentBuildConsumerFactory(String groupId) {
        JsonDeserializer<PaymentCompletedEvent> deserializer = new JsonDeserializer<>(PaymentCompletedEvent.class);
        deserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(
                baseConsumerProps(groupId),
                new StringDeserializer(),
                deserializer
        );
    }

    private ConsumerFactory<String, PointChargedEvent> pointChargedBuildConsumerFactory(String groupId) {
        JsonDeserializer<PointChargedEvent> deserializer = new JsonDeserializer<>(PointChargedEvent.class);
        deserializer.addTrustedPackages("*");
        return new DefaultKafkaConsumerFactory<>(
                baseConsumerProps(groupId), new StringDeserializer(), deserializer);
    }

    private ConsumerFactory<String, PointUsedEvent> pointUsedBuildConsumerFactory(String groupId) {
        JsonDeserializer<PointUsedEvent> deserializer = new JsonDeserializer<>(PointUsedEvent.class);
        deserializer.addTrustedPackages("*");
        return new DefaultKafkaConsumerFactory<>(
                baseConsumerProps(groupId), new StringDeserializer(), deserializer);
    }

    // 카프카리스너에서 에러가 감지되면 에러핸들러로 넘겨줌
    // 따라서 카프카리스너 어노테이션에 연결된 리스너컨테이너팩토리에 이 에러핸들러를 추가해야 함(setCommonErrorHandler)
    // 실패한 메시지를 브로커에 전달해주기 위해서 카프카템플릿 필요
    // =====================================================================================
    // 공통 DLT ErrorHandler
    // =====================================================================================
    @Bean
    public CommonErrorHandler orderCommonErrorHandlerWithDLT(
        KafkaTemplate<String, OrderCompletedEvent> orderCompletedKafkaTemplate) {

        // 재처리했는데도 계속 실패한 경우 dlt 토픽으로 해당 메시지를 보낸다
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(orderCompletedKafkaTemplate);

        //실패하자마자 재처리할건지, 실패 후 기다렸다가 재처리할건지 결정
        // 디폴트 즉시10회( 재처리시도 9회)
        // → 1초 간격으로 2회 재시도 (총 3회)
        FixedBackOff backOff = new FixedBackOff(1000L, 2L);

        return new DefaultErrorHandler(recoverer, backOff);
    }

    @Bean
    public CommonErrorHandler paymentCommonErrorHandlerWithDLT(
            KafkaTemplate<String, PaymentCompletedEvent> paymentCompletedKafkaTemplate) {

        // 재처리했는데도 계속 실패한 경우 dlt 토픽으로 해당 메시지를 보낸다
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(paymentCompletedKafkaTemplate);

        //실패하자마자 재처리할건지, 실패 후 기다렸다가 재처리할건지 결정
        // 디폴트 즉시10회( 재처리시도 9회)
        // → 1초 간격으로 2회 재시도 (총 3회)
        FixedBackOff backOff = new FixedBackOff(1000L, 2L);

        return new DefaultErrorHandler(recoverer, backOff);
    }

    @Bean
    public CommonErrorHandler pointChargedCommonErrorHandlerWithDLT(
            KafkaTemplate<String, PointChargedEvent> paymentCompletedKafkaTemplate) {

        // 재처리했는데도 계속 실패한 경우 dlt 토픽으로 해당 메시지를 보낸다
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(paymentCompletedKafkaTemplate);

        //실패하자마자 재처리할건지, 실패 후 기다렸다가 재처리할건지 결정
        // 디폴트 즉시10회( 재처리시도 9회)
        // → 1초 간격으로 2회 재시도 (총 3회)
        FixedBackOff backOff = new FixedBackOff(1000L, 2L);

        return new DefaultErrorHandler(recoverer, backOff);
    }

    @Bean
    public CommonErrorHandler pointUsedCommonErrorHandlerWithDLT(
            KafkaTemplate<String, PointUsedEvent> paymentCompletedKafkaTemplate) {

        // 재처리했는데도 계속 실패한 경우 dlt 토픽으로 해당 메시지를 보낸다
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(paymentCompletedKafkaTemplate);

        //실패하자마자 재처리할건지, 실패 후 기다렸다가 재처리할건지 결정
        // 디폴트 즉시10회( 재처리시도 9회)
        // → 1초 간격으로 2회 재시도 (총 3회)
        FixedBackOff backOff = new FixedBackOff(1000L, 2L);

        return new DefaultErrorHandler(recoverer, backOff);
    }

    // =====================================================================================
    // Menu Ranking Consumer
    // =====================================================================================

    @Bean
    public ConsumerFactory<String, OrderCompletedEvent> menuRankingConsumerFactory() {
        return orderBuildConsumerFactory("menu-ranking-group");
    }

    // 얘는 현재 주문의 에러핸들러 사용중!
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderCompletedEvent> menuRankingKafkaListenerContainerFactory(
        CommonErrorHandler orderCommonErrorHandlerWithDLT) {

        var factory = new ConcurrentKafkaListenerContainerFactory<String, OrderCompletedEvent>();
        factory.setConsumerFactory(menuRankingConsumerFactory());
        factory.setCommonErrorHandler(orderCommonErrorHandlerWithDLT);
        return factory;
    }


    // =====================================================================================
    // Order History Consumer
    // =====================================================================================

    @Bean
    public ConsumerFactory<String, OrderCompletedEvent> orderHistoryConsumerFactory() {
        return orderBuildConsumerFactory("order-history-group");
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderCompletedEvent> orderHistoryKafkaListenerContainerFactory(
        CommonErrorHandler orderCommonErrorHandlerWithDLT) {

        var factory = new ConcurrentKafkaListenerContainerFactory<String, OrderCompletedEvent>();
        factory.setConsumerFactory(orderHistoryConsumerFactory());
        factory.setConcurrency(9);
        factory.setCommonErrorHandler(orderCommonErrorHandlerWithDLT);
        return factory;
    }


    // =====================================================================================
    // Payment Consumer
    // =====================================================================================

    @Bean
    public ConsumerFactory<String, PaymentCompletedEvent> paymentConsumerFactory() {
        return paymentBuildConsumerFactory("payment-history-group");
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String,PaymentCompletedEvent> paymentKafkaListenerContainerFactory(
        CommonErrorHandler paymentCommonErrorHandlerWithDLT) {

        var factory = new ConcurrentKafkaListenerContainerFactory<String, PaymentCompletedEvent>();
        factory.setConsumerFactory(paymentConsumerFactory());
        factory.setCommonErrorHandler(paymentCommonErrorHandlerWithDLT);
        return factory;
    }


    // =====================================================================================
    // Point Charged Consumer
    // =====================================================================================

    @Bean
    public ConsumerFactory<String, PointChargedEvent> pointChargedConsumerFactory() {
        return pointChargedBuildConsumerFactory("point-charged-group");
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PointChargedEvent> pointChargedKafkaListenerContainerFactory(
            CommonErrorHandler pointChargedCommonErrorHandlerWithDLT) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, PointChargedEvent>();
        factory.setConsumerFactory(pointChargedConsumerFactory());
        factory.setCommonErrorHandler(pointChargedCommonErrorHandlerWithDLT);
        return factory;
    }

    // =====================================================================================
    // Point Used Consumer
    // =====================================================================================

    @Bean
    public ConsumerFactory<String, PointUsedEvent> pointUsedConsumerFactory() {
        return pointUsedBuildConsumerFactory("point-used-group");
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PointUsedEvent> pointUsedKafkaListenerContainerFactory(
            CommonErrorHandler pointUsedCommonErrorHandlerWithDLT) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, PointUsedEvent>();
        factory.setConsumerFactory(pointUsedConsumerFactory());
        factory.setCommonErrorHandler(pointUsedCommonErrorHandlerWithDLT);
        return factory;
    }
}


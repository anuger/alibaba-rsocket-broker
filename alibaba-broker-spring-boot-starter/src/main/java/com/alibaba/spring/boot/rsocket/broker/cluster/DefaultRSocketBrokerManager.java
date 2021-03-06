package com.alibaba.spring.boot.rsocket.broker.cluster;

import com.alibaba.rsocket.ServiceLocator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.stream.Collectors;

import static com.alibaba.rsocket.transport.NetworkUtil.getLocalIP;

/**
 * Default RSocket Broker Manager
 *
 * @author leijuan
 */
public class DefaultRSocketBrokerManager implements RSocketBrokerManager {
    private Collection<String> hosts;

    public DefaultRSocketBrokerManager() {
        try {
            this.hosts = Collections.singletonList(getLocalIP());
        } catch (Exception ignore) {

        }
    }

    public DefaultRSocketBrokerManager(String... hosts) {
        this.hosts = Arrays.asList(hosts);
    }

    @Override
    public Flux<Collection<RSocketBroker>> requestAll() {
        return Flux.just(hostsToBrokers());
    }

    @Override
    public Collection<RSocketBroker> currentBrokers() {
        return hostsToBrokers();
    }

    @Override
    public Mono<RSocketBroker> findByIp(String ip) {
        return Mono.justOrEmpty(hostsToBrokers().stream().filter(rSocketBroker -> rSocketBroker.getIp().equals(ip)).findFirst());
    }

    @Override
    public Flux<ServiceLocator> findServices(String ip) {
        return null;
    }

    @Override
    public Boolean isStandAlone() {
        return true;
    }

    public Collection<RSocketBroker> hostsToBrokers() {
        return this.hosts.stream().map(host -> {
            RSocketBroker broker = new RSocketBroker();
            broker.setIp(host);
            return broker;
        }).collect(Collectors.toList());
    }

}

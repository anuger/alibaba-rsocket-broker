package com.alibaba.spring.boot.rsocket.broker.cluster;

import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.transport.NetworkUtil;
import com.alibaba.spring.boot.rsocket.broker.cluster.RSocketBroker;
import com.alibaba.spring.boot.rsocket.broker.cluster.RSocketBrokerManager;
import io.scalecube.cluster.Cluster;
import io.scalecube.cluster.ClusterImpl;
import io.scalecube.cluster.ClusterMessageHandler;
import io.scalecube.cluster.Member;
import io.scalecube.cluster.membership.MembershipEvent;
import io.scalecube.cluster.transport.api.Message;
import io.scalecube.net.Address;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * RSocket Broker Manager Gossip implementation
 *
 * @author leijuan
 */
@Component("rsocketBrokerManager")
@ConditionalOnExpression("'${rsocket.broker.topology}'=='gossip'")
public class RSocketBrokerManagerGossipImpl implements RSocketBrokerManager, ClusterMessageHandler {
    /**
     * Gossip listen port
     */
    private static int gossipListenPort = 42254;
    /**
     * seed members
     */
    @Value("${rsocket.broker.seeds}")
    private String[] seeds;
    private Cluster cluster;
    /**
     * rsocket brokers, key is ip address
     */
    private Map<String, RSocketBroker> brokers = new HashMap<>();

    @PostConstruct
    public void init() {
        final String localIp = NetworkUtil.getLocalIP();
        cluster = new ClusterImpl()
                .config(clusterConfig -> clusterConfig.memberHost(localIp).memberPort(gossipListenPort))
                .membership(membershipConfig -> membershipConfig.seedMembers(seedMembers()).syncInterval(5_000))
                .transport(transportConfig -> transportConfig.host(localIp).port(gossipListenPort))
                .handler(cluster1 -> this)
                .startAwait();
    }

    @Override
    public Flux<Collection<RSocketBroker>> requestAll() {
        return null;
    }

    @Override
    public Collection<RSocketBroker> currentBrokers() {
        return this.brokers.values();
    }

    @Override
    public Mono<RSocketBroker> findByIp(String ip) {
        if (brokers.containsKey(ip)) {
            return Mono.just(this.brokers.get(ip));
        } else {
            return Mono.empty();
        }
    }

    @Override
    public Flux<ServiceLocator> findServices(String ip) {
        return Flux.empty();
    }

    @Override
    public Boolean isStandAlone() {
        return false;
    }

    public List<Address> seedMembers() {
        return Stream.of(seeds)
                .map(host -> Address.create(host, gossipListenPort))
                .collect(Collectors.toList());
    }

    @Override
    public void onMessage(Message message) {

    }

    @Override
    public void onGossip(Message gossip) {

    }

    @Override
    public void onMembershipEvent(MembershipEvent event) {
        RSocketBroker broker = memberToBroker(event.member());
        if (event.isAdded()) {
            brokers.put(broker.getIp(), broker);
        } else if (event.isRemoved()) {
            brokers.remove(broker.getIp());
        } else if (event.isLeaving()) {
            brokers.remove(broker.getIp());
        }
    }

    private RSocketBroker memberToBroker(Member member) {
        RSocketBroker broker = new RSocketBroker();
        broker.setIp(member.address().host());
        return broker;
    }
}

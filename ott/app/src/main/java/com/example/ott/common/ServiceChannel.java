package com.example.ott.common;

public class ServiceChannel {

    public enum SourceType {
        OTT,
        BROADCAST
    }

    private final String serviceId;
    private final String name;
    private final String description;
    private final SourceType sourceType;

    // OTT-specific
    private final String streamUrl;

    // Broadcast-specific
    private final Integer onid;
    private final Integer tsid;
    private final Integer sid;

    public ServiceChannel(String serviceId,
                          String name,
                          String description,
                          SourceType sourceType,
                          String streamUrl,
                          Integer onid,
                          Integer tsid,
                          Integer sid) {
        this.serviceId = serviceId;
        this.name = name;
        this.description = description;
        this.sourceType = sourceType;
        this.streamUrl = streamUrl;
        this.onid = onid;
        this.tsid = tsid;
        this.sid = sid;
    }

    public static ServiceChannel createOttChannel(String serviceId,
                                                  String name,
                                                  String description,
                                                  String streamUrl) {
        return new ServiceChannel(
                serviceId,
                name,
                description,
                SourceType.OTT,
                streamUrl,
                null,
                null,
                null
        );
    }

    public static ServiceChannel createBroadcastChannel(String serviceId,
                                                        String name,
                                                        String description,
                                                        int onid,
                                                        int tsid,
                                                        int sid) {
        return new ServiceChannel(
                serviceId,
                name,
                description,
                SourceType.BROADCAST,
                null,
                onid,
                tsid,
                sid
        );
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public Integer getOnid() {
        return onid;
    }

    public Integer getTsid() {
        return tsid;
    }

    public Integer getSid() {
        return sid;
    }
}

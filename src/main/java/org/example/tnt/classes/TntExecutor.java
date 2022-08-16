package org.example.tnt.classes;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


@Slf4j
public class TntExecutor<V> {

    private static final String PARAMS_SEPARATOR = ",";

    public static String[] paramsToKeys(String params) {
        Assert.hasText(params, "Params cannot be blank");
        return params.split(PARAMS_SEPARATOR);
    }

    private boolean active = false;
    private int maxSize;

    private Function<String, Map<String, V>> onRemoteRequest;
    private final Object resultsArrivedMonitor;

    private List<String> keys = new ArrayList<>();
    private List<Bi<String, V>> results = new ArrayList<>();


    public TntExecutor(int maxSize,
                       Object resultsArrivedMonitor,
                       Function<String, Map<String, V>> onRemoteRequest) {
        Assert.isTrue(maxSize >= 1, "Max Size must be positive");
        Assert.notNull(resultsArrivedMonitor, "Result Monitor cannot be null");
        Assert.notNull(onRemoteRequest, "onRemoteRequest cannot be null");

        this.maxSize = maxSize;
        this.resultsArrivedMonitor = resultsArrivedMonitor;
        this.onRemoteRequest = onRemoteRequest;
    }


    private void addItem(String item) {
        //TODO: lock
        keys.add(item);

        if (this.active) {
            checkLimitReached();
        }
    }


    private void checkLimitReached() {
        if (keys.size() >= maxSize) {
            String params = keys.stream().map(Object::toString).collect(Collectors.joining(PARAMS_SEPARATOR));

            Map<String, V> remoteData;
            try {
                remoteData = onRemoteRequest.apply(params);
            } catch (Exception e) {
                log.error("Cannot get data from remote service due to {} : {}", e.getClass().getSimpleName(), e.getMessage());
                remoteData = null;
            }

            if (remoteData != null && !remoteData.isEmpty()) {
                for (String key : keys) {
                    if (remoteData.containsKey(key)) {
                        results.add(new Bi<>(key, remoteData.get(key)));
                    }
                }

                keys.clear();

                synchronized (resultsArrivedMonitor) {
                    log.info("Signalling that results arrived");
                    resultsArrivedMonitor.notifyAll();
                }
            }
        }
    }


    public void addItems(String params) {
        addItemsInternal(paramsToKeys(params));
    }


    private void addItemsInternal(String[] arr) {
        for (String item : arr) {
            addItem(item);
        }
    }


    public void grabResults(Map<String, V> resultMap) {
        Iterator<Bi<String, V>> iterator = results.iterator();
        while (iterator.hasNext()) {
            Bi<String, V> bi = iterator.next();
            String key = bi.getKey();
            if (resultMap.containsKey(key) && resultMap.get(key) == null) {
                resultMap.put(key, bi.getValue());
                iterator.remove();
            }
        }
    }


    public Map<String, V> createResultMap(String[] keys) {
        Map<String, V> res = new HashMap<>();
        if (keys != null) {
            for (String key : keys) {
                res.put(key, null);  // no result yet
            }
        }
        return res;
    }


}

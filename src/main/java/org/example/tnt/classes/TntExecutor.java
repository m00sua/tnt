package org.example.tnt.classes;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Function;
import java.util.stream.Collectors;


@Slf4j
public class TntExecutor<V> {

    static final String PARAMS_SEPARATOR = ",";
    public static final int MILLISECONDS_IN_SECOND = 1000;

    public static String[] paramsToKeys(String params) {
        Assert.hasText(params, "Params cannot be blank");
        return params.split(PARAMS_SEPARATOR);
    }

    private String name;
    private int maxSize;
    private int timeoutSeconds;
    private Timer timer;

    private Function<String, Map<String, V>> onRemoteRequest;
    private final Object resultsArrivedMonitor;

    private List<String> keys = new ArrayList<>();
    private List<Bi<String, V>> results = new ArrayList<>();


    public TntExecutor(String name,
                       int maxSize,
                       int timeoutSeconds,
                       Object resultsArrivedMonitor,
                       Function<String, Map<String, V>> onRemoteRequest) {
        Assert.hasText(name, "Name cannot be blank");
        Assert.isTrue(maxSize >= 1, "Max Size must be positive");
        Assert.isTrue(timeoutSeconds >= 1, "Timeout must be positive");
        Assert.notNull(resultsArrivedMonitor, "Result Monitor cannot be null");
        Assert.notNull(onRemoteRequest, "onRemoteRequest cannot be null");

        this.name = name;
        this.maxSize = maxSize;
        this.timeoutSeconds = timeoutSeconds;
        this.resultsArrivedMonitor = resultsArrivedMonitor;
        this.onRemoteRequest = onRemoteRequest;
    }


    private void addItem(String item) {
        stopTimer();
        keys.add(item);

        if (keys.size() >= maxSize) {
            updateDataFromRemoteService();
        } else {
            startTimer();
        }
    }


    private void updateDataFromRemoteService() {
        log.info("[{}] Updating data from remote service", name);
        String params = keys.stream().map(Object::toString).collect(Collectors.joining(PARAMS_SEPARATOR));

        Map<String, V> remoteData;
        try {
            remoteData = onRemoteRequest.apply(params);
        } catch (Exception e) {
            log.error("[{}] Cannot get data from remote service due to {} : {}", name, e.getClass().getSimpleName(), e.getMessage());
            remoteData = null;
        }

        log.info("[{}] remoteData={}", name, remoteData);
        
        if (remoteData != null && !remoteData.isEmpty()) {
            for (String key : keys) {
                if (remoteData.containsKey(key)) {
                    results.add(new Bi<>(key, remoteData.get(key)));
                }
            }

            keys.clear();

            synchronized (resultsArrivedMonitor) {
                log.info("[{}] Signalling that results arrived", name);
                resultsArrivedMonitor.notifyAll();
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
        int countGrabbed = 0;
        Iterator<Bi<String, V>> iterator = results.iterator();
        while (iterator.hasNext()) {
            Bi<String, V> bi = iterator.next();
            String key = bi.getKey();
            if (resultMap.containsKey(key) && resultMap.get(key) == null) {
                resultMap.put(key, bi.getValue());
                iterator.remove();
                countGrabbed++;
            }
        }
        log.info("[{}]   {} received from {}", name, countGrabbed, resultMap.size());
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


    private void stopTimer() {
        if (timer != null) {
            log.info("[{}] Stopping the timer", name);
            timer.cancel();
            timer.purge();
            timer = null;
        } else {
            log.info("[{}] No active timer to stop", name);
        }
    }


    private void startTimer() {
        log.info("[{}] Start timer", name);

        timer = new Timer(name + "-executor-timer");
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                stopTimer();

                if (keys.isEmpty()) {
                    log.info("[{}] No items, no need to call On Timer event", name);
                } else {
                    updateDataFromRemoteService();
                }
            }
        }, timeoutSeconds * MILLISECONDS_IN_SECOND);
    }

}

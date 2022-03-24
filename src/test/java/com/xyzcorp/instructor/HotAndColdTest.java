package com.xyzcorp.instructor;

import io.reactivex.Observable;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class HotAndColdTest {
    @Test
    public void testColdObservable() throws InterruptedException {
        Observable<Long> i1 = Observable.interval(1, TimeUnit.SECONDS);
        i1.subscribe(x -> System.out.println("S1: " + x));
        Thread.sleep(2000);
        i1.subscribe(x -> System.out.println("S2: " + x));
        Thread.sleep(7000);
    }

    @Test
    public void testHotObservable() throws InterruptedException {
        Observable<Long> i1 = Observable.interval(1, TimeUnit.SECONDS);
        Observable<Long> publish = i1.publish().refCount();
        publish.subscribe(x -> System.out.println("S1: " + x));
        Thread.sleep(2000);
        publish.subscribe(x -> System.out.println("S2: " + x));
        Thread.sleep(7000);
        publish.subscribe(x -> System.out.println("S3: " + x));
        Thread.sleep(15000);
        Thread.sleep(5000);
    }
}

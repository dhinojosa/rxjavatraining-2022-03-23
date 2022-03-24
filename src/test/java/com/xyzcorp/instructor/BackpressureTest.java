package com.xyzcorp.instructor;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import org.junit.Before;
import org.junit.Test;

public class BackpressureTest {

    private Flowable<Long> flowable;
    @Before
    public void startUp() {
        flowable = Flowable.create(emitter -> {
            long i = 0;
            while (true) {
                emitter.onNext(i++);
            }
        }, BackpressureStrategy.MISSING);
    }

    @Test
    public void testBackpressure() {
        flowable
            .observeOn(Schedulers.newThread())
            .onBackpressureDrop()
            .subscribe(i -> {
                Thread.sleep(5);
                System.out.println(i);
            }, Throwable::printStackTrace);
    }
}

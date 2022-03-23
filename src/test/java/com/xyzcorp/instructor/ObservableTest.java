package com.xyzcorp.instructor;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import org.junit.Test;

public class ObservableTest {

    @Test
    public void testBasicObservable() {

        Observable<Long> longObservable =
            Observable.create(emitter -> {
                emitter.onNext(100L);
                emitter.onNext(250L);
                emitter.onNext(440L);
                emitter.onComplete();
            });

        longObservable.subscribe(new Observer<Long>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {

            }

            @Override
            public void onNext(@NonNull Long aLong) {
                System.out.printf("S1 (onNext): %d [%s]\n",
                    aLong, Thread.currentThread());
            }

            @Override
            public void onError(@NonNull Throwable e) {
                System.out.printf("S1 (onError): %s [%s]\n",
                    e.getMessage(), Thread.currentThread());
            }

            @Override
            public void onComplete() {
                System.out.printf("S1 (onComplete): [%s]\n",
                    Thread.currentThread());
            }
        });
    }
}

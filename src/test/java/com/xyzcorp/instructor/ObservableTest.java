package com.xyzcorp.instructor;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import org.junit.Test;

public class ObservableTest {

    @Test
    public void testBasicObservable() {

        Observable<Long> longObservable =
            Observable.create(emitter -> {
                emitter.onNext(100L);
                emitter.onNext(250L);
                emitter.onNext(440L);
                emitter.onNext(600L);
                emitter.onNext(10L);
                emitter.onComplete();
            });

        longObservable.subscribe(new Observer<Long>() {
            private Disposable disposable;

            @Override
            public void onSubscribe(@NonNull Disposable disposable) {
                this.disposable = disposable;
            }

            @Override
            public void onNext(@NonNull Long aLong) {
                System.out.printf("S1 (onNext): %d [%s]\n",
                    aLong, Thread.currentThread());
            }

            @Override
            public void onError(@NonNull Throwable e) {
                e.printStackTrace();
                System.out.printf("S1 (onError): %s [%s]\n",
                    e.getMessage(), Thread.currentThread());
            }

            @Override
            public void onComplete() {
                System.out.printf("S1 (onComplete): [%s]\n",
                    Thread.currentThread());
            }
        });

        Disposable disposable = longObservable.subscribe(
            aLong -> debug("S2 (OnNext)", aLong),
            throwable -> debug("S2 (On Error)", throwable),
            () -> debugComplete("S2")
        );

        longObservable.subscribe(
            System.out::println,
            Throwable::printStackTrace,
            () -> System.out.println("Done"));

        disposable.dispose(); //Powerless because we are running sequentially
        // on the same thread.
    }

    public <A> void debug(String label, A item) {
        System.out.printf("%s: %s [%s]\n", label, item, Thread.currentThread());
    }

    public void debugComplete(String label) {
        System.out.printf("%s (On Complete: [%s]\n", label, Thread.currentThread());
    }
}

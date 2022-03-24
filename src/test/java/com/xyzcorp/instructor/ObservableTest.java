package com.xyzcorp.instructor;

import com.xyzcorp.Employee;
import com.xyzcorp.Manager;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
        System.out.printf("%s (On Complete: [%s]\n", label,
            Thread.currentThread());
    }

    @Test
    public void testDoOnNext() throws InterruptedException {
        //Predicate: x -> (true | false)
        //Function: x -> y
        //Identity Function: x -> x (Not a java standard)
        //Consumer: x -> void
        //Supplier: void -> x

        Observable<String> stringObservable = Observable
            .just(10, 40, 320, 100, 0, 19, 600)
            .doOnNext(integer -> debug("i1", integer))
            .map(x -> 100 / x)
            .doOnNext(integer -> debug("i2", integer))
            .map(integer -> integer + 1 + "!");


        stringObservable.retry(4)
                        .subscribe(System.out::println,
                            Throwable::printStackTrace,
                            () -> System.out.println("Done"));

        System.out.println("----------");

        stringObservable.subscribe(x -> {
                if (Objects.equals(x, "20!"))
                    throw new RuntimeException("I don't like that number");
                System.out.println(x);
            }, Throwable::printStackTrace,
            () -> System.out.println("Done"));


        System.out.println("----------");

        stringObservable
            .subscribe(System.out::println);

        Thread.sleep(10000);
    }

    @Test
    public void testCreateWithFuture() {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        Observable<Integer> integerObservable =
            Observable.fromFuture(executorService.submit(() -> 50 * 60));
    }

    @Test
    public void testCreateRange() {
        Observable.range(1, 5).subscribe(System.out::println);
        Observable.range(0, 5).subscribe(System.out::println);
        Observable.range(10, 20).subscribe(System.out::println);
    }

    @Test
    public void testCreateIterable() {
        Observable<Integer> integerObservable =
            Observable.fromIterable(Arrays.asList(30, 10, 500, 12));
    }

    @Test
    public void testInterval() throws InterruptedException {
        Observable.interval(2, TimeUnit.SECONDS)
                  .doOnNext(i -> debug("i1", i))
                  .subscribe(System.out::println);
        Thread.sleep(10000);
    }

    @Test
    public void testDefer() throws InterruptedException {
        Observable<ZonedDateTime> zonedDateTimeObservable =
            Observable.defer(() -> Observable.just(ZonedDateTime.now()));

        zonedDateTimeObservable.subscribe(s -> System.out.printf("S1: %s\n",
            s));

        Thread.sleep(4000);

        zonedDateTimeObservable.subscribe(s -> System.out.printf("S2: %s\n",
            s));
    }

    @Test
    public void testSameDeferWithoutDefer() throws InterruptedException {
        Observable<ZonedDateTime> zonedDateTimeObservable =
            Observable.just(ZonedDateTime.now());

        zonedDateTimeObservable.subscribe(s -> System.out.printf("S1: %s\n",
            s));

        Thread.sleep(4000);

        zonedDateTimeObservable.subscribe(s -> System.out.printf("S2: %s\n",
            s));
    }


    @Test
    public void testCreateWithPublisher() throws InterruptedException {
        Observable<Long> longObservable =
            Observable.fromPublisher(new MyPublisher(Executors.newFixedThreadPool(19)));

        longObservable.subscribe(System.out::println);
        Thread.sleep(10000);
    }


    @Test
    public void testFlatMap() {


        Observable<Integer> integerObservable1 = Observable
            .just(1, 3, 4, 10)
            .map(i -> Observable.just(i + 1, i * 2, i * i))
            .flatMap(integerObservable -> integerObservable.firstElement().toObservable());

        integerObservable1.subscribe(System.out::println);

        Observable<Integer> map =
            Observable.just(1, 3, 4, 10).map(x -> x + 1).map(x -> x * 2).map(x -> x * x);

        System.out.println("------------");
        map.subscribe(System.out::println);
    }


    @Test
    public void testResumeIfError() {
        Observable
            .just(10, 20, 50, 0, 100, 25)
            .flatMap(this::divideHundredBy)
            .subscribe(System.out::println,
                Throwable::printStackTrace,
                () -> System.out.println("Done"));

    }

    private Observable<Integer> divideHundredBy(Integer x) {
        try {
            return Observable.just(100 / x);
        } catch (ArithmeticException e) {
            return Observable.empty();
        }
    }

    @Test
    public void testSingle() {
        Single<Integer> integerSingle = Single.just(30);
        Maybe<Integer> integerMaybe = Maybe.just(40);
    }

    @Test
    public void testReduce() {
        Single<Integer> integerMaybe =
            Observable.just(10, 40, 30, 100).reduce(0, (total, next) -> {
                System.out.printf("total: %d, next: %d\n", total, next);
                return Integer.sum(total, next);
            });

        integerMaybe.subscribe(System.out::println);

        Maybe<Integer> integerMaybe2 =
            Observable.<Integer>empty().reduce((a, b) -> Integer.sum(a, b));
    }


    @Test
    public void testConcat() {
        Observable
            .concat(Observable.just(30), Observable.just(60, 80, 0, 40))
            .map(x -> 100 / x)
            .onErrorResumeNext(throwable -> {
                if (throwable instanceof ArithmeticException) {
                    return Observable.just(0, 5, 4, 3);
                }
                return Observable.empty();
            })
            .subscribe(System.out::println);
    }

    @Test
    public void testFlatMap2() {
        List<Employee> jkRowlingsEmployees =
            Arrays.asList(
                new Employee("Harry", "Potter", 30000),
                new Employee("Hermione", "Granger", 32000),
                new Employee("Ron", "Weasley", 32000),
                new Employee("Albus", "Dumbledore", 40000));

        Manager jkRowling =
            new Manager("J.K", "Rowling", 46000, jkRowlingsEmployees);

        List<Employee> georgeLucasEmployees =
            Arrays.asList(
                new Employee("Luke", "Skywalker", 33000),
                new Employee("Leia", "Organa", 36000),
                new Employee("Han", "Solo", 36000),
                new Employee("Lando", "Calrissian", 41000));

        Manager georgeLucas =
            new Manager("George", "Lucas", 46000, georgeLucasEmployees);

        Observable<Manager> managerObservable =
            Observable.just(jkRowling, georgeLucas);

        //1. Get the total salaries of all the employees
        //2. Get the total salaries of all the employees and the managers
        //IMPORTANT: You can only reference and use managerObservable



    }
}






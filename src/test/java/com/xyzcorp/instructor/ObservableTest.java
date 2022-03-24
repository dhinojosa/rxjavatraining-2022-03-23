package com.xyzcorp.instructor;

import com.xyzcorp.Employee;
import com.xyzcorp.Manager;
import com.xyzcorp.Pair;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import io.reactivex.observables.GroupedObservable;
import io.reactivex.schedulers.Schedulers;
import org.junit.Test;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.Callable;
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

        managerObservable
            .flatMap(m -> Observable.fromIterable(m.getEmployees()))
            .map(Employee::getSalary)
            .reduce(Integer::sum)
            .subscribe(System.out::println);

        managerObservable
            .flatMap(m -> Observable.<Employee>concat(Observable.fromIterable(m.getEmployees()), Observable.just(m)))
            .map(Employee::getSalary)
            .reduce(Integer::sum)
            .subscribe(System.out::println);

        managerObservable
            .map(m -> m.getSalary())
            .reduce(Integer::sum);
    }

    @Test
    public void testGroupBy() {
        Observable<Integer> just = Observable
            .just(10, 20, 50, 100, 22, 23, 55, 78, 103, 501);


        Observable<GroupedObservable<String, Integer>> observable = just
            .groupBy(integer -> integer % 2 == 0 ? "Even" : "Odd");

        observable.subscribe(go ->
            go.subscribe(i -> System.out.println(go.getKey() + ":" + i)));
    }

    @Test
    public void testGroupByCategorization() {
        Observable<Integer> just = Observable
            .just(10, 20, 50, 100, 22, 23, 55, 78, 103, 501);

        Observable<GroupedObservable<String, Integer>> groupBy = just
            .groupBy(integer -> integer % 2 == 0 ? "Even" : "Odd");


        Single<HashMap<String, List<Integer>>> collect = groupBy
            .collect(HashMap::new,
                (map, go) -> go.subscribe(i -> {
                List<Integer> integers = map.getOrDefault(go.getKey(),
                    new ArrayList<>());
                integers.add(i);
                map.put(go.getKey(), integers);
            }));

        //Even: [10, 20, 50, 78, 100, 22]
        //Odd: [23, 55, 103, 501]

        Single<Map<String, List<Integer>>> mapSingle =
            groupBy.toMap(go -> go.getKey(),
                stringIntegerGroupedObservable -> stringIntegerGroupedObservable.toList().blockingGet());

        mapSingle.subscribe(m -> System.out.println(m));
//        collect.subscribe(m -> System.out.println(m));
    }


    @Test
    public void testStartWith() {
        Observable.just(1, 2, 3, 4)
                  .startWith(Observable.just(-1, 0))
                  .subscribe(System.out::println);
    }

    @Test
    public void testRepeat() {
        Observable.just(1, 2, 3).repeat(4).subscribe(System.out::println);
    }

    @Test
    public void testMerge() {
        Observable<Integer> observable1 =
            Observable.just(1, 2, 3, 4);
        Observable<Integer> observable2 =
            Observable.just(100, 200, 300, 400);

        Observable<Integer> merge =
            Observable.merge(observable1, observable2);

        merge.subscribe(System.out::println);
    }

    @Test
    public void testMergeAgainButDifferentThreads() throws InterruptedException {
        Observable<Long> o1 = Observable.interval(1, TimeUnit.SECONDS).map(i -> i * 1000);
        Observable<Long> o2 = Observable.interval(1, TimeUnit.SECONDS);
        Observable.merge(o1, o2).subscribe(System.out::println);

        Thread.sleep(10000);
    }

    @Test
    public void testAmb() throws InterruptedException {
        Observable<Integer> o1 = Observable.range(1, 10)
                                           .delay(5, TimeUnit.SECONDS);

        Observable<Integer> o2 = Observable.range(10, 10)
                                           .delay(2, TimeUnit.SECONDS);

        Observable<Integer> o3 = Observable.range(20, 10)
                                           .delay(15, TimeUnit.SECONDS);

        Observable.amb(Arrays.asList(o1, o2, o3)).subscribe(System.out::println);

        Thread.sleep(30000);
    }

    @Test
    public void testZip() {

        Observable<String> groceries = Observable.just(
            "Almonds",
            "Naan",
            "Eggs",
            "Broccoli",
            "Pineapple",
            "Potatoes");

        Observable<Integer> integers = Observable.range(1, 1000);

        //non-denotable-type
        Observable<String> map = Observable.zip(integers,
            groceries, (integer, s) -> new Object() {
                final String name = s;
                final Integer index = integer;
            }).map(o -> String.format("%d. %s", o.index, o.name));

        map.subscribe(System.out::println);

    }
}






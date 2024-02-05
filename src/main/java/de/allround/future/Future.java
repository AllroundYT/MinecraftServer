package de.allround.future;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface Future<T>{

    static <T> @NotNull Future<T> streamAsyncSupply(Supplier<T> consumer) {
        Future<T> future = new FutureImpl<>();
        StreamFutureImpl.EXECUTOR_SERVICE.submit(() -> {
            try {
                future.succeed(consumer.get());
            }catch (Throwable throwable){
                future.fail(throwable);
            }
        });
        return future;
    }

    static <T> @NotNull Future<T> streamAsyncSupply(Function<Future<T>, T> consumer) {
        Future<T> future = new FutureImpl<>();
        StreamFutureImpl.EXECUTOR_SERVICE.submit(() -> {
            try {
                future.succeed(consumer.apply(future));
            }catch (Throwable throwable){
                future.fail(throwable);
            }
        });
        return future;
    }

    static <T> @NotNull Future<T> streamFuture(Runnable runnable) {
        Future<T> future = new FutureImpl<>();
        StreamFutureImpl.EXECUTOR_SERVICE.submit(() -> {
            try {
                runnable.run();
                future.succeed();
            } catch (Exception e){
                future.fail(e);
            }
        });
        return future;
    }

    static <T> @NotNull Future<T> streamFuture(Consumer<Promise<T>> consumer) {
        Future<T> future = new FutureImpl<>();
        StreamFutureImpl.EXECUTOR_SERVICE.submit(() -> {
            Promise<T> promise = new PromiseImpl<>(future);
            consumer.accept(promise);
        });
        return future;
    }


    static <T> @NotNull Future<T> asyncSupply(Supplier<T> consumer) {
        Future<T> future = new FutureImpl<>();
        FutureImpl.EXECUTOR_SERVICE.submit(() -> {
            try {
                future.succeed(consumer.get());
            }catch (Throwable throwable){
                future.fail(throwable);
            }
        });
        return future;
    }

    static <T> @NotNull Future<T> asyncSupply(Function<Future<T>, T> consumer) {
        Future<T> future = new FutureImpl<>();
        FutureImpl.EXECUTOR_SERVICE.submit(() -> {
            try {
                future.succeed(consumer.apply(future));
            }catch (Throwable throwable){
                future.fail(throwable);
            }
        });
        return future;
    }

    static <T> @NotNull Future<T> future(Runnable runnable) {
        Future<T> future = new FutureImpl<>();
        FutureImpl.EXECUTOR_SERVICE.submit(() -> {
            try {
                runnable.run();
                future.succeed();
            } catch (Exception e){
                future.fail(e);
            }
        });
        return future;
    }

    static <T> @NotNull Future<T> future(Consumer<Promise<T>> consumer) {
        Future<T> future = new FutureImpl<>();
        FutureImpl.EXECUTOR_SERVICE.submit(() -> {
            Promise<T> promise = new PromiseImpl<>(future);
            consumer.accept(promise);
        });
        return future;
    }

    static <T> @NotNull Future<T> future(Future<T> otherFuture) {
        return new FutureImpl<T>().combine(otherFuture);
    }

    static <T> @NotNull Future<T> failedFuture(Throwable throwable) {
        return new FutureImpl<T>().fail(throwable);
    }

    static <T> @NotNull Future<T> succeededFuture() {
        return new FutureImpl<T>().succeed();
    }

    static <T> @NotNull Future<T> succeededFuture(T result) {
        return new FutureImpl<T>().succeed(result);
    }

    @Contract(" -> new")
    static <T> @NotNull Future<T> create(){
        return new FutureImpl<>();
    }

    @Contract(" -> new")
    static <T> @NotNull Future<T> createStream(){
        return new StreamFutureImpl<>();
    }

    static void cancelAll(){
        FutureImpl.EXECUTOR_SERVICE.shutdownNow();
    }

    Future<T> onSuccess(Consumer<T> resultConsumer);

    Future<T> onNullValueResult(Runnable runnable);

    Future<T> onNonNullValueResult(Consumer<T> nonNullValueConsumer);

    Future<T> onFailure(Consumer<Throwable> throwableConsumer);

    Future<T> succeed(T result);

    Optional<T> getResult();

    Optional<Throwable> getCause();

    void await() throws Exception;

    <MT> Future<MT> map(Function<T, MT> mappingFunction);

    CompletionStage<T> toCompletionStage();

    default Future<T> succeed() {
        return succeed(null);
    }

    default Future<T> combine(@NotNull Future<T> complete) {
        complete.onFailure(this::fail).onSuccess(this::succeed);
        return this;
    }

    Future<T> fail(Throwable throwable);

}

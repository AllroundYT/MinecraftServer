package de.allround.future;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

class StreamFutureImpl<T> implements Future<T> {

    static final ExecutorService EXECUTOR_SERVICE;

    static {
        EXECUTOR_SERVICE = Executors.newCachedThreadPool();
    }

    private final List<Consumer<T>> successConsumers = Collections.synchronizedList(new ArrayList<>());
    private final List<Consumer<T>> nonNullValueConsumers = Collections.synchronizedList(new ArrayList<>());
    private final List<Consumer<Throwable>> failConsumers = Collections.synchronizedList(new ArrayList<>());
    private final List<Runnable> nullValueRunnables = Collections.synchronizedList(new ArrayList<>());
    private boolean done;
    private T result;
    private Throwable cause;

    StreamFutureImpl() {
    }

    public boolean isDone() {
        return done;
    }

    @Override
    public Future<T> onSuccess(Consumer<T> resultConsumer) {
        successConsumers.add(resultConsumer);
        return this;
    }

    @Override
    public Future<T> onNullValueResult(Runnable runnable) {
        nullValueRunnables.add(runnable);
        return this;
    }

    @Override
    public Future<T> onNonNullValueResult(Consumer<T> nonNullValueConsumer) {
        nonNullValueConsumers.add(nonNullValueConsumer);
        return this;
    }

    @Override
    public synchronized Optional<T> getResult() {
        return Optional.ofNullable(result);
    }

    @Override
    public synchronized Optional<Throwable> getCause() {
        return Optional.ofNullable(cause);
    }

    @Override
    public Future<T> onFailure(Consumer<Throwable> throwableConsumer) {
        failConsumers.add(throwableConsumer);
        return this;
    }

    @Override
    public void await() throws Exception {
        //todo await
        toCompletionStage().toCompletableFuture().get();
    }

    @Override
    public <MT> Future<MT> map(Function<T, MT> mappingFunction) {
        StreamFutureImpl<MT> future = new StreamFutureImpl<>();
        onSuccess(t -> future.succeed(mappingFunction.apply(t)));
        onFailure(future::fail);
        return future;
    }

    @Override
    public CompletionStage<T> toCompletionStage() {
        if (getResult().isPresent()){
            return CompletableFuture.completedStage(getResult().get());
        } else if (getCause().isPresent()){
            return CompletableFuture.failedStage(getCause().get());
        } else {
            CompletableFuture<T> completableFuture = new CompletableFuture<>();
            onSuccess(completableFuture::complete);
            onFailure(completableFuture::completeExceptionally);
            return completableFuture;
        }
    }

    @Override
    public synchronized Future<T> succeed(T result) {
        this.result = result;
        done = true;
        EXECUTOR_SERVICE.submit(() -> successConsumers.forEach(consumer -> consumer.accept(result)));
        if (result != null) {
            EXECUTOR_SERVICE.submit(() -> nonNullValueConsumers.forEach(consumer -> consumer.accept(result)));
        } else {
            EXECUTOR_SERVICE.submit(() -> nullValueRunnables.forEach(Runnable::run));
        }
        return this;
    }

    @Override
    public synchronized Future<T> fail(Throwable throwable) {
        this.cause = throwable;
        done = true;
        EXECUTOR_SERVICE.submit(() -> failConsumers.forEach(throwableConsumer -> throwableConsumer.accept(throwable)));
        return this;
    }
}

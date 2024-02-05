package de.allround.future;

class PromiseImpl<T> implements Promise<T> {
    private final Future<T> future;

    PromiseImpl(Future<T> future) {
        this.future = future;
    }

    @Override
    public void complete(T result) {
        future.succeed(result);
    }

    @Override
    public void complete() {
        future.succeed();
    }

    @Override
    public void complete(Throwable throwable) {
        future.fail(throwable);
    }
}

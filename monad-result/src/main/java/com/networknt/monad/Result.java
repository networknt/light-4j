package com.networknt.monad;

import com.networknt.status.Status;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Result<T> {

    boolean isSuccess();

    default boolean isFailure() {
        return !isSuccess();
    }

    Status getError();

    T getResult();

    /*
        Use this method when you want to apply a function on the insides of the Result
        without unwrapping it. Mapper won't be invoked if the Result is Failure
     */
    default <R> Result<R> map(Function<? super T, ? extends R> mapper) {
        return isSuccess() ?
                Success.of(mapper.apply(getResult())) :
                (Failure<R>) this;
    }

    /*
        Use this method when you your mapping function is returning a Result<T> (which will make
        the return type the Result<Result<T>> using just 'map').
     */
    default <R> Result<R> flatMap(Function<? super T, Result<R>> mapper) {
        return isSuccess() ?
                mapper.apply(getResult()) :
                (Failure<R>) this;
    }

    /*
        Use this method if you want to reduce your Result to some type R. For example at the
        end of the flow, you could convert it to two different client responses depending on
        the insides.
     */
    default <R> R fold(Function<? super T, ? extends R> successFunction, Function<Failure<R>, ? extends R> failureFunction) {
        return isSuccess() ?
                successFunction.apply(getResult()) :
                failureFunction.apply((Failure<R>) this);
    }

    /*
        Use this method when you have two instances of Result and you want to invoke a function on
        their insides without unwrapping them separately. If both of them are Failures then only
        the first (this) Failure will be returned.
     */
    default <R, Z> Result<Z> lift(Result<R> other, BiFunction<? super T, ? super R, ? extends Z> function) {
        return flatMap(first -> other.map(second -> function.apply(first, second)));
    }

    default Result<T> ifSuccess(Consumer<? super T> successConsumer) {
        if (isSuccess()) {
            successConsumer.accept(this.getResult());
        }
        return this;
    }

    default Result<T> ifFailure(Consumer<Failure<T>> failureConsumer) {
        if (isFailure()) {
            failureConsumer.accept((Failure<T>) this);
        }
        return this;
    }
}

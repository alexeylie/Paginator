package com.alexeyshmakov.paginator

/**
 * @author Alexey Shmakov
 */
class LiveData<T> {

    private var data: T? = null
    private val observers: MutableList<Observer<T>> = ArrayList()

    fun getValue(): T? {
        return data
    }

    fun setValue(value: T?) {
        data = value
        for (observer in observers) {
            observer.onChangeValue(data)
        }
    }

    fun observe(observer: Observer<T>) {
        observers.remove(observer)
        observers.add(observer)
        observer.onChangeValue(data)
    }

    fun unregisterObderver(observer: Observer<T>){
        observers.remove(observer)
    }

    interface Observer<T> {
        fun onChangeValue(value: T?)
    }
}
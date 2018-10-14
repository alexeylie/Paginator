package com.alexeyshmakov.paginator

/**
 * @author Alexey Shmakov
 */
interface PaginatorAdapter<T> {
    fun attach(pager: T, paginator: Paginator)
    fun detach()
}

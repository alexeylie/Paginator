package com.alexeyshmakov.paginator

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View

/**
 * @author Alexey Shmakov
 */
class RecyclerViewPaginatorAdapter : PaginatorAdapter<RecyclerView> {

    private lateinit var paginator: Paginator
    private lateinit var recyclerView: RecyclerView
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var recyclerViewAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>
    private lateinit var adapterDataObserver: RecyclerView.AdapterDataObserver
    private lateinit var scrollListener: RecyclerView.OnScrollListener
    private var measuredChildWidth: Int = 0

    override fun attach(pager: RecyclerView, paginator: Paginator) {
        if (pager.layoutManager !is LinearLayoutManager) {
            throw IllegalStateException("Set LinearLayoutManager")
        }

        linearLayoutManager = pager.layoutManager as LinearLayoutManager

        if (linearLayoutManager.orientation != LinearLayoutManager.HORIZONTAL) {
            throw IllegalStateException("Set horizontal orientation for LinearLayoutManager")
        }

        recyclerView = pager
        recyclerViewAdapter = pager.adapter!!
        this.paginator = paginator

        adapterDataObserver = object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                paginator.setDotCount(recyclerViewAdapter.itemCount)
                updateCurrentOffset()
            }

            override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
                onChanged()
            }

            override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
                onChanged()
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                onChanged()
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                onChanged()
            }

            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                onChanged()
            }
        }
        recyclerViewAdapter.registerAdapterDataObserver(adapterDataObserver)
        paginator.setDotCount(recyclerViewAdapter.itemCount)
        updateCurrentOffset()

        scrollListener = object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE && isIdleState()) {
                    val newPosition = findVisiblePosition()
                    if (newPosition != RecyclerView.NO_POSITION) {
                        paginator.setDotCount(recyclerViewAdapter.itemCount)
                        if (newPosition < recyclerViewAdapter.itemCount) {
                            paginator.setCurrentPosition(newPosition)
                        }
                    }
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updateCurrentOffset()
            }
        }

        recyclerView.addOnScrollListener(scrollListener)
    }

    override fun detach() {
        recyclerViewAdapter.unregisterAdapterDataObserver(adapterDataObserver)
        recyclerView.removeOnScrollListener(scrollListener)
        measuredChildWidth = 0
    }

    private fun updateCurrentOffset() {
        val leftView = findFirstVisibleView() ?: return

        var position = recyclerView.getChildAdapterPosition(leftView)
        if (position == RecyclerView.NO_POSITION) {
            return
        }

        val itemCount = recyclerViewAdapter.itemCount


        if (position >= itemCount && itemCount != 0) {
            position %= itemCount
        }

        val offset = (getCurrentFrameLeft() - leftView.x) / leftView.measuredWidth

        if (offset in 0F..1F && position < itemCount) {
            paginator.onPageScrolled(position, offset)
        }
    }

    private fun findVisiblePosition(): Int {
        for (i in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(i)
            if (child.x >= getCurrentFrameLeft() && child.x
                    + child.measuredWidth <= getCurrentFrameRight()) {
                val holder = recyclerView.findContainingViewHolder(child)
                if (holder != null && holder.adapterPosition != RecyclerView.NO_POSITION) {
                    return holder.adapterPosition
                }
            }
        }
        return RecyclerView.NO_POSITION
    }

    private fun isIdleState(): Boolean {
        return findVisiblePosition() != RecyclerView.NO_POSITION
    }

    private fun findFirstVisibleView(): View? {
        val childCount = linearLayoutManager.childCount
        if (childCount == 0) {
            return null
        }

        var closestChild: View? = null
        var firstVisibleChildX = Integer.MAX_VALUE

        for (i in 0 until childCount) {
            val child = linearLayoutManager.getChildAt(i)

            val childStart: Int? = child?.x?.toInt()

            if (childStart != null) {
                if (childStart + child.measuredWidth < firstVisibleChildX &&
                        childStart + child.measuredWidth > getCurrentFrameLeft()) {
                    firstVisibleChildX = childStart
                    closestChild = child
                }
            }
        }

        return closestChild
    }

    private fun getCurrentFrameLeft(): Float {
        return (recyclerView.measuredWidth - getChildWidth()) / 2
    }

    private fun getCurrentFrameRight(): Float {
        return (recyclerView.measuredWidth - getChildWidth()) / 2 + getChildWidth()
    }

    private fun getChildWidth(): Float {
        if (measuredChildWidth == 0) {
            for (i in 0 until recyclerView.childCount) {
                val child = recyclerView.getChildAt(i)
                if (child.measuredWidth != 0) {
                    measuredChildWidth = child.measuredWidth
                    return measuredChildWidth.toFloat()
                }
            }
        }
        return measuredChildWidth.toFloat()
    }

}
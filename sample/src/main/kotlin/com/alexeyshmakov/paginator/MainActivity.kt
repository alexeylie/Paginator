package com.alexeyshmakov.paginator

import android.app.Activity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.LinearSnapHelper
import android.support.v7.widget.RecyclerView
import kotlinx.android.synthetic.main.main_activity_layout.*
import java.util.*

/**
 * @author Alexey Shmakov
 */
class MainActivity : Activity(), LiveData.Observer<ArrayList<Item>> {

    private var viewModel: ViewModel? = null
    private var adapter: RecyclerViewAdapter = RecyclerViewAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity_layout)

        viewModel = ViewModel.getModel()

        name.text = RecyclerView::class.java.simpleName

        snapList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        snapList.adapter = adapter
        RecyclerViewPaginatorAdapter().attach(snapList, snapListPaginator)
        LinearSnapHelper().attachToRecyclerView(snapList)
    }

    override fun onStart() {
        super.onStart()
        viewModel?.getData()?.observe(this)
    }

    override fun onStop() {
        super.onStop()
        viewModel?.getData()?.unregisterObderver(this)
    }

    override fun onChangeValue(value: ArrayList<Item>?) {
        if (value != null) {
            val itemsCount = value.size.toString() + " " + getString(R.string.items)
            itemCounter.text = itemsCount
        }

        adapter.submitData(value)
    }
}
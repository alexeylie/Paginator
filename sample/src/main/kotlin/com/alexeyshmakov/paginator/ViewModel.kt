package com.alexeyshmakov.paginator

import android.graphics.Color
import java.util.*

/**
 * @author Alexey Shmakov
 */
class ViewModel {

    private val data: LiveData<ArrayList<Item>> = LiveData()

    init {
        data.setValue(getItemList())
    }

    fun getData(): LiveData<ArrayList<Item>> {
        return data
    }

    private fun getItemList() : ArrayList<Item> {
        val itemList: ArrayList<Item> = ArrayList()
        val random = Random()
        for (i in 0..20) {
            val color = Color.argb(255, random.nextInt(255),
                    random.nextInt(255), random.nextInt(255))
            val item = Item(i, color)
            itemList.add(item)
        }
        return itemList
    }

    companion object {
        @JvmStatic
        private var viewModel: ViewModel? = null

        @JvmStatic
        fun getModel(): ViewModel? {
            if (viewModel == null) {
                viewModel = ViewModel()
            }

            return viewModel
        }
    }
}
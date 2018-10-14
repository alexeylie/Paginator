package com.alexeyshmakov.paginator

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.list_item_layout.view.*

/**
 * @author Alexey Shmakov
 */
class RecyclerViewAdapter : RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>() {

    private var itemList: ArrayList<Item>? = null

    fun submitData(itemList: ArrayList<Item>?) {
        this.itemList = itemList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_layout, parent, false))
    }

    override fun getItemCount(): Int {
        return itemList?.size ?: 0
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        itemList?.get(position)?.let { viewHolder.bind(it) }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(item: Item) {
            itemView.mainLayout.setBackgroundColor(item.color)
            itemView.number.text = item.id.toString()
        }
    }
}
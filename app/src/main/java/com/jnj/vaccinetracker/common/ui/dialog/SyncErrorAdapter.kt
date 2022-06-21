package com.jnj.vaccinetracker.common.ui.dialog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.databinding.ItemSyncErrorBinding


class SyncErrorAdapter() :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<SyncErrorOverviewUiModel> = emptyList()

    fun updateItems(items: List<SyncErrorOverviewUiModel>) {
        this.items = items
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return SyncErrorViewHolder(DataBindingUtil.inflate(layoutInflater, R.layout.item_sync_error, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SyncErrorViewHolder -> holder.bind(items[position])
        }
    }

    class SyncErrorViewHolder(
        private val binding: ItemSyncErrorBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SyncErrorOverviewUiModel) {
            binding.item = item
            binding.executePendingBindings()
        }
    }

}
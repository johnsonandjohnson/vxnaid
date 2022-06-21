package com.jnj.vaccinetracker.setup.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.databinding.ItemSetupMenuBinding
import com.jnj.vaccinetracker.setup.models.SetupMenuItemUiModel

class SetupMenuItemAdapter(private val onItemClick: (SetupMenuItemUiModel) -> Unit) : RecyclerView.Adapter<SetupMenuItemAdapter.ViewHolder>() {
    var items: List<SetupMenuItemUiModel> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    class ViewHolder(val binding: ItemSetupMenuBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(DataBindingUtil.inflate(layoutInflater, R.layout.item_setup_menu, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.apply {
            model = items[position]
            executePendingBindings()
            root.setOnClickListener {
                val item = items[holder.bindingAdapterPosition]
                onItemClick(item)
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

}
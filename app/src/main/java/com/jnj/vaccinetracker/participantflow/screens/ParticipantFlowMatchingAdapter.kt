package com.jnj.vaccinetracker.participantflow.screens

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.databinding.ItemParticipantMatchingOtherSiteParticipantBinding
import com.jnj.vaccinetracker.databinding.ItemParticipantMatchingParticipantBinding
import com.jnj.vaccinetracker.databinding.ItemParticipantMatchingSubtitleBinding

/**
 * @author maartenvangiel
 * @version 1
 */
class ParticipantFlowMatchingAdapter(private val itemSelectedListener: (ParticipantFlowMatchingViewModel.MatchingListItem) -> Unit) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private companion object {
        private const val ITEM_TYPE_PARTICIPANT = 0
        private const val ITEM_TYPE_OTHER_SITE_PARTICIPANT = 1
        private const val ITEM_TYPE_SUBTITLE = 2
    }

    private var items: List<ParticipantFlowMatchingViewModel.MatchingListItem> = emptyList()
    private var selectedPosition: Int? = null

    fun updateItems(items: List<ParticipantFlowMatchingViewModel.MatchingListItem>) {
        this.items = items
        notifyDataSetChanged()
    }

    fun getSelectedItem(): ParticipantFlowMatchingViewModel.MatchingListItem? {
        return selectedPosition?.let { items[it] }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ParticipantFlowMatchingViewModel.ParticipantItem -> ITEM_TYPE_PARTICIPANT
            is ParticipantFlowMatchingViewModel.OtherSiteParticipantItem -> ITEM_TYPE_OTHER_SITE_PARTICIPANT
            is ParticipantFlowMatchingViewModel.SubtitleItem -> ITEM_TYPE_SUBTITLE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            ITEM_TYPE_PARTICIPANT -> ParticipantViewHolder(
                DataBindingUtil.inflate(layoutInflater, R.layout.item_participant_matching_participant, parent, false), onClickListener = ::onParticipantItemClicked
            )
            ITEM_TYPE_OTHER_SITE_PARTICIPANT -> ParticipantOtherSiteViewHolder(
                DataBindingUtil.inflate(layoutInflater, R.layout.item_participant_matching_other_site_participant, parent, false)
            )
            ITEM_TYPE_SUBTITLE -> SubtitleViewHolder(DataBindingUtil.inflate(layoutInflater, R.layout.item_participant_matching_subtitle, parent, false))
            else -> error("No such viewtype exists")
        }
    }

    private fun onParticipantItemClicked(adapterPosition: Int) {
        val previousSelection = selectedPosition
        selectedPosition = adapterPosition
        previousSelection?.let { notifyItemChanged(it) }
        notifyItemChanged(adapterPosition)
        itemSelectedListener(items[adapterPosition])
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ParticipantViewHolder -> holder.bind(items[position] as ParticipantFlowMatchingViewModel.ParticipantItem, position == selectedPosition)
            is ParticipantOtherSiteViewHolder -> holder.bind(items[position] as ParticipantFlowMatchingViewModel.OtherSiteParticipantItem)
            is SubtitleViewHolder -> holder.bind(items[position] as ParticipantFlowMatchingViewModel.SubtitleItem)
        }
    }

    class ParticipantViewHolder(
        private val binding: ItemParticipantMatchingParticipantBinding,
        private val onClickListener: (adapterPosition: Int) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener { onClickListener(adapterPosition) }
        }

        fun bind(item: ParticipantFlowMatchingViewModel.ParticipantItem, selected: Boolean) {
            binding.item = item
            binding.selected = selected
            binding.executePendingBindings()
        }
    }

    class ParticipantOtherSiteViewHolder(private val binding: ItemParticipantMatchingOtherSiteParticipantBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ParticipantFlowMatchingViewModel.OtherSiteParticipantItem) {
            binding.item = item
            binding.executePendingBindings()
        }
    }

    class SubtitleViewHolder(private val binding: ItemParticipantMatchingSubtitleBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ParticipantFlowMatchingViewModel.SubtitleItem) {
            binding.item = item
            binding.executePendingBindings()
        }
    }

}
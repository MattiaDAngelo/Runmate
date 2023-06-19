package com.example.runmate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

// Interface implemented by StatsFragment; it allows the deletion of a training
interface TrainingItemClickListener {
    fun deleteTrainingItem(position: Int)
}

// Adapter class to manage the training list
class TrainingAdapter(private val dataList: MutableList<TrainingObject>, private val itemClickListener: TrainingItemClickListener) : RecyclerView.Adapter<TrainingAdapter.TrainingItemHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrainingItemHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.training_item, parent, false)

        return TrainingItemHolder(itemView)
    }

    override fun onBindViewHolder(holder: TrainingItemHolder, position: Int) {
        if (dataList.isEmpty()) {
            holder.showEmptyState()
        } else {
            val itemData = dataList[position]
            holder.bind(itemData)
        }
    }

    override fun getItemCount(): Int {
        return if (dataList.isEmpty()) 1 else dataList.size
    }

    // Class to handle the training item
    inner class TrainingItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tv_act_sum_1: TextView = itemView.findViewById(R.id.tv_activity_summary_1)
        private val tv_act_sum_2: TextView = itemView.findViewById(R.id.tv_activity_summary_2)
        private val imgv_activity: ImageView = itemView.findViewById(R.id.imgv_activity_icon)

        init {

            // the user performs a long click on an item in the list, with the intention of deleting it
            itemView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    itemClickListener.deleteTrainingItem(position)
                }
                true
            }
        }

        fun bind(itemData: TrainingObject) {

            // update text views and images
            tv_act_sum_1.text = "${itemData.type} | ${itemData.date} | ${itemData.startTime}"
            tv_act_sum_2.text = "${itemData.steps} passi | ${itemData.distance.roundToInt()} m | ${itemData.calories.roundToInt()} kcal" + " | ${itemData.duration}"

            if (itemData.type == "Camminata") imgv_activity.setImageResource(R.drawable.walk)
            else imgv_activity.setImageResource(R.drawable.run)
        }

        // there are no trainings => show a specific text
        fun showEmptyState() {
            val tv_act_empty: TextView = itemView.findViewById(R.id.tv_activity_empty)

            tv_act_empty.visibility = View.VISIBLE
            imgv_activity.visibility = View.INVISIBLE
            tv_act_sum_1.visibility = View.INVISIBLE
            tv_act_sum_2.visibility = View.INVISIBLE
        }
    }
}
package com.example.bdwisher


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.Serializable

// Add the Birthday data class if it's not defined elsewhere
data class Birthday(
    val id: Int,
    val name: String,
    val date: String,
    val whatsapp: String,
    val wish: String
) : Serializable

class BirthdayAdapter(private val birthdays: MutableList<Birthday>, private val onItemClick: (Birthday) -> Unit) : RecyclerView.Adapter<BirthdayAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.birthday_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val birthday = birthdays[position]
        holder.nameTextView.text = birthday.name
        holder.dateTextView.text = birthday.date
        holder.wishTextView.text = birthday.wish

        holder.itemView.setOnClickListener {
            onItemClick(birthday)
        }
    }

    override fun getItemCount() = birthdays.size

    fun getItem(position: Int): Birthday {
        return birthdays[position]
    }

    fun removeItem(position: Int) {
        birthdays.removeAt(position)
        notifyItemRemoved(position)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.nameTextView)
        val dateTextView: TextView = view.findViewById(R.id.dateTextView)
        val wishTextView: TextView = view.findViewById(R.id.wishTextView)
    }
}
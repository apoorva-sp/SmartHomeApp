package com.example.myhome

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myhome.Beans.Property
import com.example.myhome.UserInterface.AppliancesActivity

class PropertyAdapter(private val propertyList: List<Property>) :
    RecyclerView.Adapter<PropertyAdapter.PropertyViewHolder>() {

    class PropertyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val housecard: LinearLayout = itemView.findViewById(R.id.housecard)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvType: TextView = itemView.findViewById(R.id.tvType)
        val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        val tvDetails: TextView = itemView.findViewById(R.id.tvDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PropertyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.home_list, parent, false)
        return PropertyViewHolder(view)
    }

    override fun onBindViewHolder(holder: PropertyViewHolder, position: Int) {
        val property = propertyList[position]

        holder.tvTitle.text = property.title
        holder.tvType.text = property.type
        holder.tvLocation.text = property.location
        holder.tvDetails.text = property.details

        // Handle card clicks here
        holder.housecard.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, AppliancesActivity::class.java)
            intent.putExtra("housename",holder.tvTitle.text)
            intent.putExtra("location",holder.tvLocation.text)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return propertyList.size
    }
}

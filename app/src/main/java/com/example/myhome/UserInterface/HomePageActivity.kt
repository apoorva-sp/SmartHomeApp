package com.example.myhome.UserInterface

import android.os.Bundle
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myhome.Beans.Property
import com.example.myhome.PropertyAdapter
import com.example.myhome.R

class HomePageActivity : AppCompatActivity() {
    private lateinit var menuButton: ImageButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var propertyAdapter: PropertyAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.homepage)
        initViews()
        setupMenuButton()
        setupRecyclerView()


    }
    /** Initialize all UI components **/
    private fun initViews() {
        menuButton = findViewById(R.id.menuButton)
        recyclerView = findViewById(R.id.recyclerViewProperties)
    }

    /** Handle the 3-dot menu click and sign-out **/
    private fun setupMenuButton() {
        menuButton.setOnClickListener {
            val popup = PopupMenu(this, menuButton)
            popup.menuInflater.inflate(R.menu.top_menu, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_signout -> {
                        Toast.makeText(this, "Signing out...", Toast.LENGTH_SHORT).show()
                        // TODO: Add your actual sign-out logic here
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    /** Setup RecyclerView with 2 columns and sample data **/
    private fun setupRecyclerView() {
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        // Temporary: sample data for 5 cards
        val properties = mutableListOf<Property>()
        for (i in 1..5) {
            properties.add(
                Property(
                    title = "Home $i",
                    type = "Villa",
                    location = "Location $i",
                    details = "Details about Home $i"
                )
            )
        }


        propertyAdapter = PropertyAdapter(properties)
        recyclerView.adapter = propertyAdapter
    }
}
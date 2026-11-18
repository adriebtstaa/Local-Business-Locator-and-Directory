package com.example.nasugbubusinesslocator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

abstract class Business(
    val id: Int,
    val name: String,
    val ownerName: String,
    val address: String,
    val barangay: String,
    val category: String,
    val phone: String,
    val imageName: String,
    val latitude: Double,
    val longitude: Double,
    val photos: List<String> = listOf()
) {
    private val reviews = mutableListOf<Review>()

    fun addReview(review: Review) {
        reviews.add(review)
    }
    fun getAllPhotos(): List<String>{
        return if (photos.isNotEmpty()) photos else listOf(imageName)
    }

    fun getReviews(): List<Review> {
        return reviews.toList()
    }

    fun getAverageRating(): Double {
        if (reviews.isEmpty()) return 0.0
        return reviews.map { it.rating }.average()
    }

    abstract fun getDisplayInfo(): String
}

class Resort(
    id: Int,
    name: String,
    ownerName: String,
    address: String,
    barangay: String,
    phone: String,
    imageName: String,
    latitude: Double,
    longitude: Double,
    val hasBeachAccess: Boolean,
    val entranceFee: Double,
    photos: List<String> = listOf()
) : Business(id, name, ownerName, address, barangay, "Resort", phone, imageName, latitude, longitude, photos) {
    override fun getDisplayInfo(): String {
        return "$name - Resort in $barangay | Entrance: P$entranceFee | Beach: $hasBeachAccess"
    }
}

class Restaurant(
    id: Int,
    name: String,
    ownerName: String,
    address: String,
    barangay: String,
    phone: String,
    imageName: String,
    latitude: Double,
    longitude: Double,
    val cuisineType: String,
    photos: List<String> = listOf()
) : Business(id, name, ownerName, address, barangay, "Food", phone, imageName, latitude, longitude, photos) {
    override fun getDisplayInfo(): String {
        return "$name - $cuisineType Restaurant in $barangay"
    }
}

class Shop(
    id: Int,
    name: String,
    ownerName: String,
    address: String,
    barangay: String,
    phone: String,
    imageName: String,
    latitude: Double,
    longitude: Double,
    val openingHours: String,
    photos: List<String> = listOf()
) : Business(id, name, ownerName, address, barangay, "Shop", phone, imageName, latitude, longitude, photos) {
    override fun getDisplayInfo(): String {
        return "$name - Shop in $barangay | Open: $openingHours"
    }
}

interface CanComment {
    fun postComment(business: Business, commentText: String, rating: Int)
}

data class Review(
    val guestName: String,
    val commentText: String,
    val rating: Int,
    val date: String
)

abstract class User(
    val name: String,
    val email: String,
    private var password: String
) {
    fun checkPassword(input: String): Boolean {
        return input == password
    }

    abstract fun getRole(): String
}

class BusinessOwner(
    name: String,
    email: String,
    password: String,
    val businessNameOwned: String
) : User(name, email, password) {
    override fun getRole(): String = "Business Owner"

    fun registerBusiness(business: Business): String {
        return "$name registered business: ${business.name}"
    }
}

class GuestUser(
    name: String,
    email: String,
    password: String
) : User(name, email, password), CanComment {
    override fun getRole(): String = "Guest"

    override fun postComment(business: Business, commentText: String, rating: Int) {
        val newReview = Review(guestName = name, commentText = commentText, rating = rating, date = "2026-11-15")
        business.addReview(newReview)
    }
}

class BusinessDirectory {
    private val businessList = mutableListOf<Business>()

    fun addBusiness(business: Business) {
        businessList.add(business)
    }

    fun getAllBusinesses(): List<Business> {
        return businessList
    }

    fun search(query: String): List<Business> {
        return businessList.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.barangay.contains(query, ignoreCase = true)
        }
    }

    fun filterByCategory(category: String): List<Business> {
        if (category == "All") return businessList
        return businessList.filter { it.category == category }
    }

    fun loadNasugbuBusinesses() {
        addBusiness(Resort(1, "Kawayan Cove", "Juan Dela Cruz", "Bucana, Nasugbu, Batangas", "Bucana", "0917-000-0001", "kawayan_cove.jpg", 14.1350, 120.6150, true, 150.0, photos = listOf("kawayan_cove.jpg, cawayan_cove.jpg")))
        addBusiness(Resort(2, "Fortune Island Resort", "Maria Santos", "Fortune Island, Nasugbu, Batangas", "Fortune Island", "0917-000-0002", "fortune_island.jpg", 14.0580, 120.4870, true, 500.0))
        addBusiness(Resort(3, "Calayo Beach Resort", "Pedro Reyes", "Calayo, Nasugbu, Batangas", "Calayo", "0917-000-0003", "calayo.jpg", 14.1100, 120.6200, true, 100.0))
        addBusiness(Resort(4, "Wawa Beach Resort", "Jose Ramos", "Wawa, Nasugbu, Batangas", "Wawa", "0917-000-0004", "wawa.jpg", 14.0800, 120.6400, true, 80.0))
        addBusiness(Resort(5, "Papaya Beach Resort", "Liza Cruz", "Papaya, Nasugbu, Batangas", "Papaya", "0917-000-0005", "papaya.jpg", 14.0900, 120.6000, true, 120.0))
        addBusiness(Restaurant(6, "Jollibee Nasugbu", "Jollibee Corp", "Brgy. 3, Nasugbu, Batangas", "Brgy. 3", "043-123-4567", "jollibee.jpg", 14.0700, 120.6350, "Fast Food"))
        addBusiness(Restaurant(7, "McDonald's Nasugbu", "McDonald's Corp", "Nasugbu Town, Batangas", "Brgy. 3", "043-123-4568", "mcdonalds.jpg", 14.0690, 120.6330, "Fast Food"))
        addBusiness(Restaurant(8, "Lomi House", "Ana Garcia", "Wawa, Nasugbu, Batangas", "Wawa", "0917-000-0008", "lomi.jpg", 14.0810, 120.6410, "Filipino"))
        addBusiness(Shop(9, "Nasugbu Public Market", "Municipality", "Brgy. 4, Nasugbu, Batangas", "Brgy. 4", "043-123-4569", "market.jpg", 14.0725, 120.6345, "5AM - 6PM"))
        addBusiness(Shop(10, "Puregold Nasugbu", "Puregold", "Nasugbu Town, Batangas", "Brgy. 3", "043-123-4570", "puregold.jpg", 14.0680, 120.6360, "8AM - 9PM"))
        addBusiness(Shop(11, "7-Eleven Nasugbu", "7-Eleven", "Brgy. 3, Nasugbu, Batangas", "Brgy. 3", "043-123-4571", "seven_eleven.jpg", 14.0710, 120.6340, "24 Hours"))
        addBusiness(Shop(12, "Nasugbu Municipal Hall", "Municipality", "Brgy. 2, Nasugbu, Batangas", "Brgy. 2", "043-123-4572", "municipal.jpg", 14.0730, 120.6310, "8AM - 5PM"))
    }
}

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val directory = BusinessDirectory()
    private lateinit var adapter: BusinessAdapter
    private var currentList: List<Business> = listOf()
    private val currentGuest = GuestUser("Guest User", "guest@gmail.com", "1234")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        directory.loadNasugbuBusinesses()
        currentList = directory.getAllBusinesses()

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val recycler = findViewById<RecyclerView>(R.id.recyclerBusiness)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = BusinessAdapter(currentList) { business ->
            val pos = LatLng(business.latitude, business.longitude)
            val cameraPosition = com.google.android.gms.maps.model.CameraPosition.Builder()
                .target(pos)
                .zoom(18f)
                .tilt(45f)
                .bearing(30f)
                .build()
            mMap.animateCamera(
                com.google.android.gms.maps.CameraUpdateFactory.newCameraPosition(
                    cameraPosition
                )
            )
            Toast.makeText(this, business.getDisplayInfo(), Toast.LENGTH_LONG).show()
        }
        recycler.adapter = adapter

        findViewById<EditText>(R.id.searchEditText).addTextChangedListener { text ->
            currentList = directory.search(text.toString())
            adapter.updateList(currentList)
            showMarkers()

        }

        findViewById<Button>(R.id.btnAll).setOnClickListener { filterCategory("All") }
        findViewById<Button>(R.id.btnResort).setOnClickListener { filterCategory("Resort") }
        findViewById<Button>(R.id.btnFood).setOnClickListener { filterCategory("Food") }
        findViewById<Button>(R.id.btnMarket).setOnClickListener { filterCategory("Shop") }

        findViewById<Button>(R.id.btnOwnerSignUp).setOnClickListener {
            val owner = BusinessOwner("New Owner", "owner@gmail.com", "1234", "My New Resort")
            Toast.makeText(this, owner.registerBusiness(currentList[0]), Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnGuestComment).setOnClickListener {
            if (currentList.isNotEmpty()) {
                val firstBusiness = currentList[0]
                currentGuest.postComment(firstBusiness, "Beautiful place! I loved it!", 5)
                Toast.makeText(
                    this,
                    "Guest rated ${firstBusiness.name}: ${firstBusiness.getAverageRating()} stars",
                    Toast.LENGTH_SHORT
                ).show()
                adapter.updateList(currentList)
            }
        }
    }

    private fun filterCategory(category: String) {
        currentList = directory.filterByCategory(category)
        adapter.updateList(currentList)
        showMarkers()
    }

    private fun showMarkers() {
        if (!::mMap.isInitialized) return
        mMap.clear()
        for (b in currentList) {
            val marker = mMap.addMarker(
                MarkerOptions().position(LatLng(b.latitude, b.longitude)).title(b.name)
            )
            marker?.tag = b
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMarkerClickListener { marker ->
            val business = marker.tag as? Business
            if (business != null) {
                showBusinessInfo(business)
            }
            true
        }
        mMap.isBuildingsEnabled = true
        mMap.uiSettings.isTiltGesturesEnabled = true
        mMap.uiSettings.isRotateGesturesEnabled = true
        mMap.uiSettings.isZoomGesturesEnabled = true
        showMarkers()
        val nasugbuCenter = LatLng(14.0900, 120.6000)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nasugbuCenter, 11.5f))
    }

    private fun showBusinessInfo(business: Business) {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle(business.name)
        builder.setMessage(business.getDisplayInfo())
        builder.setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    class BusinessAdapter(
        private var list: List<Business>,
        private val onClick: (Business) -> Unit
    ) : RecyclerView.Adapter<BusinessAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(android.R.id.text1)
            val desc: TextView = view.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val b = list[position]
            holder.name.text = "${b.name} - ${String.format("%.1f", b.getAverageRating())} stars"
            holder.desc.text = "${b.category} | ${b.address}"
            holder.itemView.setOnClickListener { onClick(b) }
        }

        override fun getItemCount() = list.size

        fun updateList(newList: List<Business>) {
            list = newList
            notifyDataSetChanged()
        }
    }
}
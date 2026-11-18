package com.example.nasugbubusinesslocator

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.nasugbubusinesslocator.data.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Suppress("unused")
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

    fun setReviews(newReviews: List<Review>) {
        reviews.clear()
        reviews.addAll(newReviews)
    }

    fun addReview(review: Review) {
        reviews.add(review)
    }

    fun getAllPhotos(): List<String> = photos.ifEmpty { listOf(imageName) }

    fun getReviews(): List<Review> = reviews.toList()

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

class Cafe(
    id: Int,
    name: String,
    ownerName: String,
    address: String,
    barangay: String,
    phone: String,
    imageName: String,
    latitude: Double,
    longitude: Double,
    val specialty: String,
    val hasWifi: Boolean,
    photos: List<String> = listOf()
) : Business(id, name, ownerName, address, barangay, "Cafe", phone, imageName, latitude, longitude, photos) {
    override fun getDisplayInfo(): String {
        return "$name - Cafe in $barangay | Specialty: $specialty | WiFi: $hasWifi"
    }
}

class Pharmacy(
    id: Int,
    name: String,
    ownerName: String,
    address: String,
    barangay: String,
    phone: String,
    imageName: String,
    latitude: Double,
    longitude: Double,
    val is24Hours: Boolean,
    photos: List<String> = listOf()
) : Business(id, name, ownerName, address, barangay, "Pharmacy", phone, imageName, latitude, longitude, photos) {
    override fun getDisplayInfo(): String {
        return "$name - Pharmacy in $barangay | 24 Hours: $is24Hours"
    }
}

interface CanComment {
    suspend fun postComment(business: Business, commentText: String, rating: Int, database: AppDatabase)
}

data class Review(
    val guestName: String,
    val commentText: String,
    val rating: Int,
    val date: String
)

@Suppress("unused")
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

@Suppress("unused")
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

    override suspend fun postComment(business: Business, commentText: String, rating: Int, database: AppDatabase) {
        val reviewEntity = ReviewEntity(
            businessId = business.id,
            guestName = name,
            commentText = commentText,
            rating = rating,
            date = "2026-11-15"
        )
        database.reviewDao().insertReview(reviewEntity)
        business.addReview(Review(guestName = name, commentText = commentText, rating = rating, date = "2026-11-15"))
    }
}

class BusinessDirectory {
    private val businessList = mutableListOf<Business>()

    fun setBusinesses(list: List<Business>) {
        businessList.clear()
        businessList.addAll(list)
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

    companion object {
        fun getInitialEntities(): List<BusinessEntity> {
            return listOf(
                BusinessEntity(1, "Kawayan Cove", "Juan Dela Cruz", "Bucana, Nasugbu, Batangas", "Bucana", "Resort", "0917-000-0001", "kawayan_cove.jpg", 14.1350, 120.6150, "kawayan_cove.jpg, cawayan_cove.jpg", hasBeachAccess = true, entranceFee = 150.0),
                BusinessEntity(2, "Fortune Island Resort", "Maria Santos", "Fortune Island, Nasugbu, Batangas", "Fortune Island", "Resort", "0917-000-0002", "fortune_island.jpg", 14.0580, 120.4870, "fortune_island.jpg", hasBeachAccess = true, entranceFee = 500.0),
                BusinessEntity(3, "Calayo Beach Resort", "Pedro Reyes", "Calayo, Nasugbu, Batangas", "Calayo", "Resort", "0917-000-0003", "calayo.jpg", 14.1100, 120.6200, "calayo.jpg", hasBeachAccess = true, entranceFee = 100.0),
                BusinessEntity(4, "Wawa Beach Resort", "Jose Ramos", "Wawa, Nasugbu, Batangas", "Wawa", "Resort", "0917-000-0004", "wawa.jpg", 14.0800, 120.6400, "wawa.jpg", hasBeachAccess = true, entranceFee = 80.0),
                BusinessEntity(5, "Papaya Beach Resort", "Liza Cruz", "Papaya, Nasugbu, Batangas", "Papaya", "Resort", "0917-000-0005", "papaya.jpg", 14.0900, 120.6000, "papaya.jpg", hasBeachAccess = true, entranceFee = 120.0),
                BusinessEntity(6, "Jollibee Nasugbu", "Jollibee Corp", "Brgy. 3, Nasugbu, Batangas", "Brgy. 3", "Food", "043-123-4567", "jollibee.jpg", 14.0700, 120.6350, "jollibee.jpg", cuisineType = "Fast Food"),
                BusinessEntity(7, "McDonald's Nasugbu", "McDonald's Corp", "Nasugbu Town, Batangas", "Brgy. 3", "Food", "043-123-4568", "mcdonalds.jpg", 14.0690, 120.6330, "mcdonalds.jpg", cuisineType = "Fast Food"),
                BusinessEntity(8, "Lomi House", "Ana Garcia", "Wawa, Nasugbu, Batangas", "Wawa", "Food", "0917-000-0008", "lomi.jpg", 14.0810, 120.6410, "lomi.jpg", cuisineType = "Filipino"),
                BusinessEntity(9, "Nasugbu Public Market", "Municipality", "Brgy. 4, Nasugbu, Batangas", "Brgy. 4", "Shop", "043-123-4569", "market.jpg", 14.0725, 120.6345, "market.jpg", openingHours = "5AM - 6PM"),
                BusinessEntity(10, "Puregold Nasugbu", "Puregold", "Nasugbu Town, Batangas", "Brgy. 3", "Shop", "043-123-4570", "puregold.jpg", 14.0680, 120.6360, "puregold.jpg", openingHours = "8AM - 9PM"),
                BusinessEntity(11, "7-Eleven Nasugbu", "7-Eleven", "Brgy. 3, Nasugbu, Batangas", "Brgy. 3", "Shop", "043-123-4571", "seven_eleven.jpg", 14.0710, 120.6340, "seven_eleven.jpg", openingHours = "24 Hours"),
                BusinessEntity(12, "Nasugbu Municipal Hall", "Municipality", "Brgy. 2, Nasugbu, Batangas", "Brgy. 2", "Shop", "043-123-4572", "municipal.jpg", 14.0730, 120.6310, "municipal.jpg", openingHours = "8AM - 5PM"),
                BusinessEntity(13, "Kape Nasugbu", "Juan Santos", "Brgy. 3, Nasugbu, Batangas", "Brgy. 3", "Cafe", "0917-111-2222", "kape.jpg", 14.0715, 120.6348, "kape.jpg", specialty = "Barako Coffee", hasWifi = true),
                BusinessEntity(14, "Seaside Brew", "Carla Reyes", "Wawa, Nasugbu, Batangas", "Wawa", "Cafe", "0917-333-4444", "seaside_brew.jpg", 14.0820, 120.6420, "seaside_brew.jpg", specialty = "Iced Latte", hasWifi = true),
                BusinessEntity(15, "Mercury Drug Nasugbu", "Mercury Corp", "Brgy. 3, Nasugbu, Batangas", "Brgy. 3", "Pharmacy", "043-987-6543", "mercury.jpg", 14.0705, 120.6355, "mercury.jpg", is24Hours = true),
                BusinessEntity(16, "The Generics Pharmacy", "Gen Pharmacy", "Brgy. 2, Nasugbu, Batangas", "Brgy. 2", "Pharmacy", "043-555-1234", "generics.jpg", 14.0735, 120.6315, "generics.jpg", is24Hours = false)
            )
        }

        fun entityToBusiness(entity: BusinessEntity): Business {
            val photosList = if (entity.photosString.isNotBlank()) entity.photosString.split(",").map { it.trim() } else listOf(entity.imageName)
            return when (entity.category) {
                "Resort" -> Resort(entity.id, entity.name, entity.ownerName, entity.address, entity.barangay, entity.phone, entity.imageName, entity.latitude, entity.longitude, entity.hasBeachAccess, entity.entranceFee, photosList)
                "Food" -> Restaurant(entity.id, entity.name, entity.ownerName, entity.address, entity.barangay, entity.phone, entity.imageName, entity.latitude, entity.longitude, entity.cuisineType, photosList)
                "Shop" -> Shop(entity.id, entity.name, entity.ownerName, entity.address, entity.barangay, entity.phone, entity.imageName, entity.latitude, entity.longitude, entity.openingHours, photosList)
                "Cafe" -> Cafe(entity.id, entity.name, entity.ownerName, entity.address, entity.barangay, entity.phone, entity.imageName, entity.latitude, entity.longitude, entity.specialty, entity.hasWifi, photosList)
                "Pharmacy" -> Pharmacy(entity.id, entity.name, entity.ownerName, entity.address, entity.barangay, entity.phone, entity.imageName, entity.latitude, entity.longitude, entity.is24Hours, photosList)
                else -> Shop(entity.id, entity.name, entity.ownerName, entity.address, entity.barangay, entity.phone, entity.imageName, entity.latitude, entity.longitude, "", photosList)
            }
        }
    }
}

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var database: AppDatabase
    private val directory = BusinessDirectory()
    private lateinit var adapter: BusinessAdapter
    private var currentList: List<Business> = listOf()
    private val currentGuest = GuestUser("Guest User", "guest@gmail.com", "1234")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        database = AppDatabase.getDatabase(this)

        lifecycleScope.launch(Dispatchers.IO) {
            val existing = database.businessDao().getAllBusinesses()
            if (existing.isEmpty()) {
                database.businessDao().insertBusinesses(BusinessDirectory.getInitialEntities())
            }
            loadDataAndRefresh()
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val recycler = findViewById<RecyclerView>(R.id.recyclerBusiness)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = BusinessAdapter(emptyList()) { business ->
            zoomToBusiness(business)
            showBusinessInfo(business)
        }
        recycler.adapter = adapter

        findViewById<EditText>(R.id.searchEditText).addTextChangedListener { text ->
            lifecycleScope.launch(Dispatchers.IO) {
                val query = text.toString()
                val entities = database.businessDao().searchBusinesses(query)
                val businesses = entities.map { entity ->
                    val b = BusinessDirectory.entityToBusiness(entity)
                    val revs = database.reviewDao().getReviewsForBusiness(b.id)
                    b.setReviews(revs.map { Review(it.guestName, it.commentText, it.rating, it.date) })
                    b
                }
                withContext(Dispatchers.Main) {
                    currentList = businesses
                    adapter.updateList(currentList)
                    showMarkers()
                }
            }
        }

        findViewById<Button>(R.id.btnAll).setOnClickListener { filterCategory("All") }
        findViewById<Button>(R.id.btnResort).setOnClickListener { filterCategory("Resort") }
        findViewById<Button>(R.id.btnFood).setOnClickListener { filterCategory("Food") }
        findViewById<Button>(R.id.btnMarket).setOnClickListener { filterCategory("Shop") }
        findViewById<Button>(R.id.btnCafe).setOnClickListener { filterCategory("Cafe") }
        findViewById<Button>(R.id.btnPharmacy).setOnClickListener { filterCategory("Pharmacy") }

        findViewById<Button>(R.id.btnOwnerSignUp).setOnClickListener {
            val owner = BusinessOwner("New Owner", "owner@gmail.com", "1234", "My New Resort")
            lifecycleScope.launch(Dispatchers.IO) {
                database.userDao().insertUser(
                    UserEntity(owner.email, owner.name, "1234", owner.getRole(), owner.businessNameOwned)
                )
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, owner.registerBusiness(currentList.firstOrNull() ?: return@withContext), Toast.LENGTH_SHORT).show()
                }
            }
        }

        findViewById<Button>(R.id.btnGuestComment).setOnClickListener {
            if (currentList.isNotEmpty()) {
                val firstBusiness = currentList[0]
                lifecycleScope.launch(Dispatchers.IO) {
                    currentGuest.postComment(firstBusiness, "Beautiful place! I loved it!", 5, database)
                    val updatedRevs = database.reviewDao().getReviewsForBusiness(firstBusiness.id)
                    firstBusiness.setReviews(updatedRevs.map { Review(it.guestName, it.commentText, it.rating, it.date) })
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivity,
                            "Guest rated ${firstBusiness.name}: ${String.format(Locale.getDefault(), "%.1f", firstBusiness.getAverageRating())} stars",
                            Toast.LENGTH_SHORT
                        ).show()
                        adapter.updateList(currentList)
                        showBusinessInfo(firstBusiness)
                    }
                }
            }
        }
    }

    private suspend fun loadDataAndRefresh() {
        val entities = database.businessDao().getAllBusinesses()
        val businesses = entities.map { entity ->
            val b = BusinessDirectory.entityToBusiness(entity)
            val revs = database.reviewDao().getReviewsForBusiness(b.id)
            b.setReviews(revs.map { Review(it.guestName, it.commentText, it.rating, it.date) })
            b
        }
        directory.setBusinesses(businesses)
        currentList = businesses
        withContext(Dispatchers.Main) {
            adapter.updateList(currentList)
            showMarkers()
        }
    }

    private fun filterCategory(category: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val entities = if (category == "All") {
                database.businessDao().getAllBusinesses()
            } else {
                database.businessDao().getBusinessesByCategory(category)
            }
            val businesses = entities.map { entity ->
                val b = BusinessDirectory.entityToBusiness(entity)
                val revs = database.reviewDao().getReviewsForBusiness(b.id)
                b.setReviews(revs.map { Review(it.guestName, it.commentText, it.rating, it.date) })
                b
            }
            withContext(Dispatchers.Main) {
                currentList = businesses
                adapter.updateList(currentList)
                showMarkers()
            }
        }
    }

    private fun zoomToBusiness(business: Business) {
        val pos = LatLng(business.latitude, business.longitude)
        val cameraPosition = com.google.android.gms.maps.model.CameraPosition.Builder()
            .target(pos)
            .zoom(18f)
            .tilt(45f)
            .bearing(30f)
            .build()
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    private fun showMarkers() {
        if (!::mMap.isInitialized) return
        mMap.clear()
        for (b in currentList) {
            val marker = mMap.addMarker(
                MarkerOptions()
                    .position(LatLng(b.latitude, b.longitude))
                    .title(b.name)
                    .snippet("${b.category} - Rating: ${String.format(Locale.getDefault(), "%.1f", b.getAverageRating())}★")
            )
            marker?.tag = b
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMarkerClickListener { marker ->
            val business = marker.tag as? Business
            if (business != null) {
                zoomToBusiness(business)
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
        lifecycleScope.launch(Dispatchers.IO) {
            val revs = database.reviewDao().getReviewsForBusiness(business.id)
            business.setReviews(revs.map { Review(it.guestName, it.commentText, it.rating, it.date) })

            withContext(Dispatchers.Main) {
                val avg = business.getAverageRating()
                val reviewsListStr = if (business.getReviews().isEmpty()) {
                    "No reviews yet."
                } else {
                    business.getReviews().joinToString("\n\n") { r ->
                        "• ${r.guestName} (${r.rating}★, ${r.date}):\n  \"${r.commentText}\""
                    }
                }
                val photosStr = business.getAllPhotos().joinToString(", ")

                val message = buildString {
                    append(business.getDisplayInfo())
                    append("\n\nAddress: ${business.address}")
                    append("\nPhone: ${business.phone}")
                    append("\nOwner: ${business.ownerName}")
                    append("\nAverage Rating: ${String.format(Locale.getDefault(), "%.1f", avg)}★ (${business.getReviews().size} reviews)")
                    append("\n\nPhotos: [$photosStr]")
                    append("\n\n--- Reviews ---\n")
                    append(reviewsListStr)
                }

                AlertDialog.Builder(this@MainActivity)
                    .setTitle(business.name)
                    .setMessage(message)
                    .setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
                    .setNeutralButton("Add Review") { _, _ ->
                        showAddReviewDialog(business)
                    }
                    .show()
            }
        }
    }

    private fun showAddReviewDialog(business: Business) {
        val input = EditText(this)
        input.hint = "Write your review..."
        AlertDialog.Builder(this)
            .setTitle("Add Review for ${business.name}")
            .setView(input)
            .setPositiveButton("Submit") { _, _ ->
                val text = input.text.toString()
                if (text.isNotBlank()) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        currentGuest.postComment(business, text, 5, database)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Review added!", Toast.LENGTH_SHORT).show()
                            showBusinessInfo(business)
                            adapter.updateList(currentList)
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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

        @SuppressLint("DefaultLocale")
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val b = list[position]
            holder.name.text = String.format(Locale.getDefault(), "%s - %.1f★", b.name, b.getAverageRating())
            holder.desc.text = "${b.category} | ${b.address}"
            holder.itemView.setOnClickListener { onClick(b) }
        }

        override fun getItemCount() = list.size

        fun updateList(newList: List<Business>) {
            list = newList
            notifyItemRangeChanged(0, list.size)
        }
    }
}

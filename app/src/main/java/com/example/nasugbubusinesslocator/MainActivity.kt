package com.example.nasugbubusinesslocator

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
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
import java.text.SimpleDateFormat
import java.util.Date
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

    fun getAllPhotos(): List<String> = photos

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
        return "🏖️ $name - Resort in $barangay | Entrance: ₱$entranceFee | Beach Access: $hasBeachAccess"
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
        return "🍽️ $name - $cuisineType Restaurant in $barangay"
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
        return "🛍️ $name - Shop in $barangay | Open: $openingHours"
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
        return "☕ $name - Cafe in $barangay | Specialty: $specialty | WiFi: $hasWifi"
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
        return "💊 $name - Pharmacy in $barangay | 24 Hours: $is24Hours"
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
) {
    fun getStarString(): String {
        val stars = "★★★★★".take(rating) + "☆☆☆☆☆".drop(rating)
        return stars
    }
}

@Suppress("unused")
abstract class User(
    val name: String,
    val email: String,
    private var password: String
) {
    abstract fun getRole(): String
}

class GuestUser(
    name: String,
    email: String,
    password: String
) : User(name, email, password), CanComment {
    override fun getRole(): String = "Guest"

    override suspend fun postComment(business: Business, commentText: String, rating: Int, database: AppDatabase) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        val reviewEntity = ReviewEntity(
            businessId = business.id,
            guestName = name,
            commentText = commentText,
            rating = rating,
            date = currentDate
        )
        database.reviewDao().insertReview(reviewEntity)
        business.addReview(Review(guestName = name, commentText = commentText, rating = rating, date = currentDate))
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
                BusinessEntity(1, "Kawayan Cove", "Juan Dela Cruz", "Bucana, Nasugbu, Batangas", "Bucana", "Resort", "0917-000-0001", "", 14.1350, 120.6150, "", hasBeachAccess = true, entranceFee = 150.0),
                BusinessEntity(2, "Fortune Island Resort", "Maria Santos", "Fortune Island, Nasugbu, Batangas", "Fortune Island", "Resort", "0917-000-0002", "", 14.0580, 120.4870, "", hasBeachAccess = true, entranceFee = 500.0),
                BusinessEntity(3, "Calayo Beach Resort", "Pedro Reyes", "Calayo, Nasugbu, Batangas", "Calayo", "Resort", "0917-000-0003", "", 14.1100, 120.6200, "", hasBeachAccess = true, entranceFee = 100.0),
                BusinessEntity(4, "Canyon Cove Beach Club and Residences", "Jose Ramos", "Wawa, Nasugbu, Batangas", "Wawa", "Resort", "0917-000-0004", "", 14.0800, 120.6400, "", hasBeachAccess = true, entranceFee = 80.0),
                BusinessEntity(5, "Papaya Beach Resort", "Liza Cruz", "Papaya, Nasugbu, Batangas", "Papaya", "Resort", "0917-000-0005", "", 14.0900, 120.6000, "", hasBeachAccess = true, entranceFee = 120.0),
                BusinessEntity(6, "Jollibee Nasugbu", "Jollibee Corp", "Brgy. 3, Nasugbu, Batangas", "Brgy. 3", "Food", "043-123-4567", "", 14.0700, 120.6350, "", cuisineType = "Fast Food"),
                BusinessEntity(7, "McDonald's Nasugbu", "McDonald's Corp", "Nasugbu Town, Batangas", "Brgy. 3", "Food", "043-123-4568", "", 14.0690, 120.6330, "", cuisineType = "Fast Food"),
                BusinessEntity(8, "Lomi House", "Ana Garcia", "Wawa, Nasugbu, Batangas", "Wawa", "Food", "0917-000-0008", "", 14.0810, 120.6410, "", cuisineType = "Filipino"),
                BusinessEntity(9, "Nasugbu Public Market", "Municipality", "Brgy. 4, Nasugbu, Batangas", "Brgy. 4", "Shop", "043-123-4569", "", 14.0725, 120.6345, "", openingHours = "5AM - 6PM"),
                BusinessEntity(10, "Puregold Nasugbu", "Puregold", "Nasugbu Town, Batangas", "Brgy. 3", "Shop", "043-123-4570", "", 14.0680, 120.6360, "", openingHours = "8AM - 9PM"),
                BusinessEntity(11, "7-Eleven Nasugbu", "7-Eleven", "Brgy. 3, Nasugbu, Batangas", "Brgy. 3", "Shop", "043-123-4571", "", 14.0710, 120.6340, "", openingHours = "24 Hours"),
                BusinessEntity(12, "Nasugbu Municipal Hall", "Municipality", "Brgy. 2, Nasugbu, Batangas", "Brgy. 2", "Shop", "043-123-4572", "", 14.0730, 120.6310, "", openingHours = "8AM - 5PM"),
                BusinessEntity(13, "Kape Nasugbu", "Juan Santos", "Brgy. 3, Nasugbu, Batangas", "Brgy. 3", "Cafe", "0917-111-2222", "", 14.0715, 120.6348, "", specialty = "Barako Coffee", hasWifi = true),
                BusinessEntity(14, "Seaside Brew", "Carla Reyes", "Wawa, Nasugbu, Batangas", "Wawa", "Cafe", "0917-333-4444", "", 14.0820, 120.6420, "", specialty = "Iced Latte", hasWifi = true),
                BusinessEntity(15, "Mercury Drug Nasugbu", "Mercury Corp", "Brgy. 3, Nasugbu, Batangas", "Brgy. 3", "Pharmacy", "043-987-6543", "", 14.0705, 120.6355, "", is24Hours = true),
                BusinessEntity(16, "The Generics Pharmacy", "Gen Pharmacy", "Brgy. 2, Nasugbu, Batangas", "Brgy. 2", "Pharmacy", "043-555-1234", "", 14.0735, 120.6315, "", is24Hours = false)
            )
        }

        fun entityToBusiness(entity: BusinessEntity): Business {
            val photosList = if (entity.photosString.isNotBlank()) entity.photosString.split(",").map { it.trim() } else listOf()
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
    private lateinit var currentGuest: GuestUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        database = AppDatabase.getDatabase(this)

        val userName = intent.getStringExtra("USER_NAME") ?: "Visitor"
        val userEmail = intent.getStringExtra("USER_EMAIL") ?: "visitor@gmail.com"
        currentGuest = GuestUser(userName, userEmail, "")

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
                    updateDirectoryHeaderCount()
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
            showBusinessOwnerRegistrationDialog()
        }

        findViewById<Button>(R.id.btnGuestComment).setOnClickListener {
            if (currentList.isNotEmpty()) {
                showAddReviewDialog(currentList[0])
            } else {
                Toast.makeText(this, "No businesses available to review", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showBusinessOwnerRegistrationDialog() {
        val context = this
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val ownerNameInput = EditText(context).apply { hint = "Owner Name"; setText(currentGuest.name) }
        val emailInput = EditText(context).apply { hint = "Email or Contact Details"; setText(currentGuest.email) }
        val bizNameInput = EditText(context).apply { hint = "Business Name" }

        val categoryLabel = TextView(context).apply { text = "Category:"; setPadding(0, 10, 0, 0) }
        val categorySpinner = Spinner(context).apply {
            val categories = arrayOf("Resort", "Food", "Shop", "Cafe", "Pharmacy")
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, categories)
        }

        val addressInput = EditText(context).apply { hint = "Address / Location (e.g. Brgy. Bucana, Nasugbu)" }
        val phoneInput = EditText(context).apply { hint = "Phone Number" }

        layout.addView(ownerNameInput)
        layout.addView(emailInput)
        layout.addView(bizNameInput)
        layout.addView(categoryLabel)
        layout.addView(categorySpinner)
        layout.addView(addressInput)
        layout.addView(phoneInput)

        AlertDialog.Builder(context)
            .setTitle("🏢 Register New Business")
            .setView(layout)
            .setPositiveButton("Register & Save") { _, _ ->
                val ownerName = ownerNameInput.text.toString().trim()
                val email = emailInput.text.toString().trim()
                val bizName = bizNameInput.text.toString().trim()
                val category = categorySpinner.selectedItem.toString()
                val address = addressInput.text.toString().trim()
                val phone = phoneInput.text.toString().trim()

                if (ownerName.isNotEmpty() && email.isNotEmpty() && bizName.isNotEmpty() && address.isNotEmpty()) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        database.userDao().insertUser(
                            UserEntity(email = email, name = ownerName, password = "", role = "Business Owner", businessNameOwned = bizName)
                        )
                        val newId = (System.currentTimeMillis() % 100000).toInt() + 200
                        val entity = BusinessEntity(
                            id = newId,
                            name = bizName,
                            ownerName = ownerName,
                            address = address,
                            barangay = "Nasugbu",
                            category = category,
                            phone = phone.ifEmpty { "N/A" },
                            imageName = "",
                            latitude = 14.0700 + (Math.random() * 0.02 - 0.01),
                            longitude = 120.6300 + (Math.random() * 0.02 - 0.01),
                            photosString = "",
                            hasBeachAccess = category == "Resort",
                            entranceFee = if (category == "Resort") 100.0 else 0.0,
                            cuisineType = if (category == "Food") "Local" else "",
                            openingHours = if (category == "Shop") "8AM - 8PM" else "",
                            specialty = if (category == "Cafe") "Coffee" else "",
                            hasWifi = category == "Cafe",
                            is24Hours = category == "Pharmacy"
                        )
                        database.businessDao().insertBusinesses(listOf(entity))
                        loadDataAndRefresh()

                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "🎉 Business '$bizName' registered successfully!", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateDirectoryHeaderCount() {
        val subtitle = findViewById<TextView>(R.id.textDirectorySubtitle)
        subtitle?.text = "Showing ${currentList.size} businesses in Nasugbu, Batangas"
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
            updateDirectoryHeaderCount()
        }
    }

    private fun filterCategory(category: String) {
        Toast.makeText(this, "Filtering: $category", Toast.LENGTH_SHORT).show()
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
                updateDirectoryHeaderCount()
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
                    "No reviews yet. Be the first to review!"
                } else {
                    business.getReviews().joinToString("\n\n") { r ->
                        "• ${r.guestName} (${r.getStarString()} ${r.rating}★, ${r.date}):\n  \"${r.commentText}\""
                    }
                }

                val message = buildString {
                    append(business.getDisplayInfo())
                    append("\n\n📍 Address: ${business.address}")
                    append("\n📞 Phone: ${business.phone}")
                    append("\n👤 Owner: ${business.ownerName}")
                    append("\n⭐ Average Rating: ${String.format(Locale.getDefault(), "%.1f", avg)}★ (${business.getReviews().size} reviews)")
                    append("\n\n--- Reviews & Ratings ---\n")
                    append(reviewsListStr)
                }

                AlertDialog.Builder(this@MainActivity)
                    .setTitle("🏢 ${business.name}")
                    .setMessage(message)
                    .setPositiveButton("Close", null)
                    .setNeutralButton("✍️ Write Review") { _, _ ->
                        showAddReviewDialog(business)
                    }
                    .setNegativeButton("🧭 Get Directions / Route") { _, _ ->
                        openDirections(business)
                    }
                    .show()
            }
        }
    }

    private fun openDirections(business: Business) {
        val gmmIntentUri = Uri.parse("google.navigation:q=${business.latitude},${business.longitude}")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        try {
            startActivity(mapIntent)
        } catch (e: Exception) {
            val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${business.latitude},${business.longitude}")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri)
            startActivity(webIntent)
        }
    }

    private fun showAddReviewDialog(business: Business) {
        val context = this
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val ratingLabel = TextView(context).apply { text = "Select Star Rating (1 to 5 stars):" }
        val ratingSpinner = Spinner(context).apply {
            val ratings = arrayOf("★★★★★ (5 Stars - Excellent)", "★★★★☆ (4 Stars - Very Good)", "★★★☆☆ (3 Stars - Good)", "★★☆☆☆ (2 Stars - Fair)", "★☆☆☆☆ (1 Star - Poor)")
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, ratings)
            setSelection(0)
        }

        val commentInput = EditText(context).apply {
            hint = "Write your review comment here..."
        }

        layout.addView(ratingLabel)
        layout.addView(ratingSpinner)
        layout.addView(commentInput)

        AlertDialog.Builder(context)
            .setTitle("⭐ Rate & Review ${business.name}")
            .setView(layout)
            .setPositiveButton("Submit Review") { _, _ ->
                val commentText = commentInput.text.toString().trim()
                val selectedIndex = ratingSpinner.selectedItemPosition
                val rating = 5 - selectedIndex

                if (commentText.isNotEmpty()) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        currentGuest.postComment(business, commentText, rating, database)
                        val updatedRevs = database.reviewDao().getReviewsForBusiness(business.id)
                        business.setReviews(updatedRevs.map { Review(it.guestName, it.commentText, it.rating, it.date) })

                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "🎉 Review & $rating-star rating submitted!", Toast.LENGTH_SHORT).show()
                            showBusinessInfo(business)
                            adapter.updateList(currentList)
                            showMarkers()
                        }
                    }
                } else {
                    Toast.makeText(context, "Please write a comment", Toast.LENGTH_SHORT).show()
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
            val name: TextView = view.findViewById(R.id.textBusinessName)
            val rating: TextView = view.findViewById(R.id.textRating)
            val categoryAndAddress: TextView = view.findViewById(R.id.textCategoryAndAddress)
            val phone: TextView = view.findViewById(R.id.textPhone)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_business, parent, false)
            return ViewHolder(view)
        }

        @SuppressLint("DefaultLocale")
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val b = list[position]
            holder.name.text = b.name
            holder.rating.text = String.format(Locale.getDefault(), "⭐ %.1f", b.getAverageRating())
            holder.categoryAndAddress.text = "${b.category} | ${b.address}"
            holder.phone.text = "📞 ${b.phone}"
            holder.itemView.setOnClickListener { onClick(b) }
        }

        override fun getItemCount() = list.size

        fun updateList(newList: List<Business>) {
            list = newList
            notifyItemRangeChanged(0, list.size)
        }
    }
}

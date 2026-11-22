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
import android.widget.ImageView
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

fun ImageView.loadUrl(url: String) {
    if (url.isBlank() || !url.startsWith("http")) return
    setImageResource(android.R.drawable.ic_menu_gallery)
    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
        try {
            var currentUrl = url
            var bitmap: android.graphics.Bitmap? = null
            for (i in 0..3) {
                val connection = java.net.URL(currentUrl).openConnection() as java.net.HttpURLConnection
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                val responseCode = connection.responseCode
                if (responseCode == java.net.HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == java.net.HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == java.net.HttpURLConnection.HTTP_SEE_OTHER) {
                    val nextUrl = connection.getHeaderField("Location")
                    if (nextUrl != null) {
                        currentUrl = nextUrl
                        continue
                    }
                }
                val inputStream = connection.inputStream
                bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                break
            }
            if (bitmap != null) {
                withContext(Dispatchers.Main) {
                    setImageBitmap(bitmap)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

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
        return "$name - Resort in $barangay | Entrance: ₱$entranceFee | Beach Access: $hasBeachAccess"
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
                BusinessEntity(1, "Kawayan Cove", "Jose G. Puyat", "Bucana, Nasugbu, Batangas", "Bucana", "Resort", "0917-000-0001", "", 14.1350, 120.6150, "https://images.unsplash.com/photo-1544644181-1484b3fdfc62?w=800", entranceFee = 150.0),
                BusinessEntity(2, "Fortune Island Resort", "Jose Antonio Leviste", "Fortune Island, Nasugbu, Batangas", "Fortune Island", "Resort", "0917-000-0002", "", 14.0580, 120.4870, "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800", entranceFee = 500.0),
                BusinessEntity(3, "Calayo Beach Resort", "", "Calayo, Nasugbu, Batangas", "Calayo", "Resort", "0917-000-0003", "", 14.1100, 120.6200, "https://images.unsplash.com/photo-1519046904884-53103b34b206?w=800", entranceFee = 100.0),
                BusinessEntity(4, "Canyon Cove Beach Club and Residences", "George Go", "Wawa, Nasugbu, Batangas", "Wawa", "Resort", "0917-000-0004", "", 14.0800, 120.6400, "https://images.unsplash.com/photo-1571896349842-33c89424de2d?w=800", entranceFee = 80.0),
                BusinessEntity(5, "Papaya Beach Resort", "Michael SikSik", "Papaya, Nasugbu, Batangas", "Papaya", "Resort", "0917-000-0005", "", 14.0900, 120.6000, "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=800", entranceFee = 120.0),
                BusinessEntity(6, "Jollibee Nasugbu", "Jollibee Foods Corp", "Brgy. 3, Nasugbu, Batangas", "Brgy. 3", "Food", "043-123-4567", "", 14.0700, 120.6350, "https://images.unsplash.com/photo-1561758033-d89a9ad46330?w=800", cuisineType = "Fast Food"),
                BusinessEntity(7, "McDonald's Nasugbu", "Golden Arches Development Corporation", "Nasugbu Town, Batangas", "Brgy. 3", "Food", "043-123-4568", "", 14.0690, 120.6330, "https://images.unsplash.com/photo-1550547660-d9450f859349?w=800", cuisineType = "Fast Food"),
                BusinessEntity(8, "Bayani’s Lomi House & Restaurant - Nasugbu", "", "Wawa, Nasugbu, Batangas", "Wawa", "Food", "0917-000-0008", "", 14.0810, 120.6410, "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=800", cuisineType = "Filipino"),
                BusinessEntity(9, "Nasugbu Public Market", "Municipality of Nasugbu", "Brgy. 4, Nasugbu, Batangas", "Brgy. 4", "Shop", "043-123-4569", "", 14.0725, 120.6345, "https://images.unsplash.com/photo-1533900298318-6b8da08a523e?w=800", openingHours = "5AM - 6PM"),
                BusinessEntity(10, "Puregold Nasugbu", "Puregold Price Club Inc", "Nasugbu Town, Batangas", "Brgy. 3", "Shop", "043-123-4570", "", 14.0680, 120.6360, "https://images.unsplash.com/photo-1578916171728-46686eac8d58?w=800", openingHours = "8AM - 9PM"),
                BusinessEntity(11, "7-Eleven Nasugbu", "Philippine Seven Corporation", "Brgy. 3, Nasugbu, Batangas", "Brgy. 3", "Shop", "043-123-4571", "", 14.0710, 120.6340, "https://images.unsplash.com/photo-1604719312566-8912e9227c6a?w=800", openingHours = "24 Hours"),
                BusinessEntity(12, "Nasugbu Municipal Hall", "Municipality", "Brgy. 2, Nasugbu, Batangas", "Brgy. 2", "Shop", "043-123-4572", "", 14.0730, 120.6310, "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=800", openingHours = "8AM - 5PM"),
                BusinessEntity(13, "Kape Nasugbu", "", "Brgy. 3, Nasugbu, Batangas", "Brgy. 3", "Cafe", "0917-111-2222", "", 14.0715, 120.6348, "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=800", specialty = "Barako Coffee",),
                BusinessEntity(14, "Seaside Brew", "", "Wawa, Nasugbu, Batangas", "Wawa", "Cafe", "0917-333-4444", "", 14.0820, 120.6420, "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=800", specialty = "Iced Latte"),
                BusinessEntity(15, "Mercury Drug Nasugbu", "Mercury Corp", "Brgy. 3, Nasugbu, Batangas", "Brgy. 3", "Pharmacy", "043-987-6543", "", 14.0705, 120.6355, "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=800"),
                BusinessEntity(16, "The Generics Pharmacy", "LIAHDO Corp", "Brgy. 2, Nasugbu, Batangas", "Brgy. 2", "Pharmacy", "043-555-1234", "", 14.0735, 120.6315, "https://images.unsplash.com/photo-1576091160399-112ba8d25d1d?w=800")
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
            database.businessDao().insertBusinesses(BusinessDirectory.getInitialEntities())
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
                try {
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
                } catch (e: Exception) {
                    e.printStackTrace()
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
            .setTitle("Register New Business")
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
                            Toast.makeText(context, "Business '$bizName' registered successfully!", Toast.LENGTH_SHORT).show()
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
            try {
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
            } catch (e: Exception) {
                e.printStackTrace()
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
        mMap.mapType = GoogleMap.MAP_TYPE_HYBRID // Enables Satellite + Hybrid view showing actual real buildings, roofs, and terrain!
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
                val dialogView = LayoutInflater.from(this@MainActivity).inflate(R.layout.dialog_business_info, null)

                val imgBanner = dialogView.findViewById<ImageView>(R.id.dialogImageBanner)
                val badgeCategory = dialogView.findViewById<TextView>(R.id.dialogBadgeCategory)
                val textName = dialogView.findViewById<TextView>(R.id.dialogTextName)
                val textDisplayInfo = dialogView.findViewById<TextView>(R.id.dialogTextDisplayInfo)
                val textDetails = dialogView.findViewById<TextView>(R.id.dialogTextDetails)
                val textReviews = dialogView.findViewById<TextView>(R.id.dialogTextReviews)
                val btnDirections = dialogView.findViewById<Button>(R.id.dialogBtnDirections)
                val btnReview = dialogView.findViewById<Button>(R.id.dialogBtnReview)
                val btnClose = dialogView.findViewById<Button>(R.id.dialogBtnClose)

                textName.text = business.name
                textDisplayInfo.text = business.getDisplayInfo()
                badgeCategory.text = business.category.uppercase()

                val photoUrl = business.getAllPhotos().firstOrNull() ?: ""
                if (photoUrl.startsWith("http")) {
                    imgBanner.loadUrl(photoUrl)
                }
                
                when (business.category) {
                    "Resort" -> {
                        imgBanner.setBackgroundResource(R.drawable.bg_banner_resort)
                        imgBanner.setImageResource(android.R.drawable.ic_menu_mapmode)
                    }
                    "Food" -> {
                        imgBanner.setBackgroundResource(R.drawable.bg_banner_food)
                        imgBanner.setImageResource(android.R.drawable.ic_menu_day)
                    }
                    "Shop" -> {
                        imgBanner.setBackgroundResource(R.drawable.bg_banner_shop)
                        imgBanner.setImageResource(android.R.drawable.ic_menu_myplaces)
                    }
                    "Cafe" -> {
                        imgBanner.setBackgroundResource(R.drawable.bg_banner_cafe)
                        imgBanner.setImageResource(android.R.drawable.ic_menu_recent_history)
                    }
                    "Pharmacy" -> {
                        imgBanner.setBackgroundResource(R.drawable.bg_banner_shop)
                        imgBanner.setImageResource(android.R.drawable.ic_menu_info_details)
                    }
                    else -> {
                        imgBanner.setBackgroundResource(R.drawable.bg_banner_shop)
                        imgBanner.setImageResource(android.R.drawable.ic_menu_gallery)
                    }
                }

                val avg = business.getAverageRating()
                textDetails.text = buildString {
                    append("Address: ${business.address}\n")
                    append("Phone: ${business.phone}\n")
                    append("Owner: ${business.ownerName}\n")
                    append("Average Rating: ${String.format(Locale.getDefault(), "%.1f", avg)}★ (${business.getReviews().size} reviews)")
                }

                val reviewsListStr = if (business.getReviews().isEmpty()) {
                    "No reviews yet. Be the first to review!"
                } else {
                    business.getReviews().joinToString("\n\n") { r ->
                        "• ${r.guestName} (${r.getStarString()} ${r.rating}★, ${r.date}):\n  \"${r.commentText}\""
                    }
                }
                textReviews.text = reviewsListStr

                val dialog = AlertDialog.Builder(this@MainActivity)
                    .setView(dialogView)
                    .create()

                btnDirections.setOnClickListener {
                    openDirections(business)
                }

                btnReview.setOnClickListener {
                    dialog.dismiss()
                    showAddReviewDialog(business)
                }

                btnClose.setOnClickListener {
                    dialog.dismiss()
                }

                dialog.show()
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
            val ratings = arrayOf("★★★★★ (5 Stars - Excellent)", "★★★★☆ (4 Stats - Very Good)", "★★★☆☆ (3 Stars - Good)", "★★☆☆☆ (2 Stars - Fair)", "★☆☆☆☆ (1 Star - Poor)")
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
            .setTitle("Rate & Review ${business.name}")
            .setView(layout)
            .setPositiveButton("Submit Review") { _, _ ->
                val commentContent = commentInput.text.toString().trim()
                val selectedIndex = ratingSpinner.selectedItemPosition
                val rating = 5 - selectedIndex

                if (commentContent.isNotEmpty()) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        currentGuest.postComment(business, commentContent, rating, database)
                        val updatedRevs = database.reviewDao().getReviewsForBusiness(business.id)
                        business.setReviews(updatedRevs.map { Review(it.guestName, it.commentText, it.rating, it.date) })

                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Review & $rating-star rating submitted!", Toast.LENGTH_SHORT).show()
                            showBusinessInfo(business)
                            adapter.updateList(currentList)
                            showMarkers()
                        }
                    }
                } else {
                    Toast.makeText(context, "Please write a comment", Toast.LENGTH_SHORT).show()
                    showAddReviewDialog(business)
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
            val thumbnail: ImageView = view.findViewById(R.id.imageThumbnail)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_business, parent, false)
            return ViewHolder(view)
        }

        @SuppressLint("ViewHolder")
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val b = list[position]
            holder.name.text = b.name
            holder.rating.text = String.format(Locale.getDefault(), "⭐ %.1f", b.getAverageRating())
            holder.categoryAndAddress.text = "${b.category} | ${b.address}"
            holder.phone.text = "📞 ${b.phone}"

            val photoUrl = b.getAllPhotos().firstOrNull() ?: ""
            if (photoUrl.startsWith("http")) {
                holder.thumbnail.loadUrl(photoUrl)
            } else {
                when (b.category) {
                    "Resort" -> holder.thumbnail.setImageResource(android.R.drawable.ic_menu_mapmode)
                    "Food" -> holder.thumbnail.setImageResource(android.R.drawable.ic_menu_day)
                    "Shop" -> holder.thumbnail.setImageResource(android.R.drawable.ic_menu_myplaces)
                    "Cafe" -> holder.thumbnail.setImageResource(android.R.drawable.ic_menu_recent_history)
                    "Pharmacy" -> holder.thumbnail.setImageResource(android.R.drawable.ic_menu_info_details)
                    else -> holder.thumbnail.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            }

            holder.itemView.setOnClickListener { onClick(b) }
        }

        override fun getItemCount() = list.size

        val updateList: (List<Business>) -> Unit = { newList ->
            list = newList
            notifyDataSetChanged()
        }
    }
}

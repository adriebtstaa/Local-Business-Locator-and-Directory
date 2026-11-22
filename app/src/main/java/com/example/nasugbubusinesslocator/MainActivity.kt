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
    val longitude: Double
) {
    private val reviews = mutableListOf<Review>()

    fun setReviews(newReviews: List<Review>) {
        reviews.clear()
        reviews.addAll(newReviews)
    }

    fun addReview(review: Review) {
        reviews.add(review)
    }

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
    val entranceFee: Double
) : Business(id, name, ownerName, address, barangay, "Resort", phone, imageName, latitude, longitude) {
    override fun getDisplayInfo(): String {
        return "$name - Resort in $barangay | Entrance Fee: ₱$entranceFee"
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
    val cuisineType: String
) : Business(id, name, ownerName, address, barangay, "Food", phone, imageName, latitude, longitude) {
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
    val openingHours: String
) : Business(id, name, ownerName, address, barangay, "Shop", phone, imageName, latitude, longitude) {
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
    val hasWifi: Boolean
) : Business(id, name, ownerName, address, barangay, "Cafe", phone, imageName, latitude, longitude) {
    override fun getDisplayInfo(): String {
        return "$name - Cafe in $barangay | Specialty: $specialty"
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
    val is24Hours: Boolean
) : Business(id, name, ownerName, address, barangay, "Pharmacy", phone, imageName, latitude, longitude) {
    override fun getDisplayInfo(): String {
        return "$name - Pharmacy in $barangay"
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
                BusinessEntity(1, "Kawayan Cove", "Jose G. Puyat", "Bucana, Nasugbu, Batangas", "Bucana", "Resort", "0917-000-0001", "", 14.1350, 120.6150, "", true, 150.0, "", "", "", false, false),
                BusinessEntity(2, "Fortune Island Resort", "Jose Antonio Leviste", "Fortune Island, Nasugbu, Batangas", "Fortune Island", "Resort", "0917-000-0002", "", 14.0580, 120.4870, "", true, 500.0, "", "", "", false, false),
                BusinessEntity(3, "Calayo Beach Resort", "Pedro Reyes", "Calayo, Nasugbu, Batangas", "Calayo", "Resort", "0917-000-0003", "", 14.1100, 120.6200, "", true, 100.0, "", "", "", false, false),
                BusinessEntity(4, "Canyon Cove Beach Club", "George Go", "Wawa, Nasugbu, Batangas", "Wawa", "Resort", "0917-000-0004", "", 14.0800, 120.6400, "", true, 80.0, "", "", "", false, false),
                BusinessEntity(5, "Papaya Beach Resort", "Michael SikSik", "Papaya, Nasugbu, Batangas", "Papaya", "Resort", "0917-000-0005", "", 14.0900, 120.6000, "", true, 120.0, "", "", "", false, false),
                BusinessEntity(6, "Pico de Loro Cove", "SM Prime Holdings", "Hamilo Coast, Papaya, Nasugbu", "Papaya", "Resort", "0917-000-0006", "", 14.2200, 120.6100, "", true, 1000.0, "", "", "", false, false),
                BusinessEntity(7, "Tali Beach Resort", "Tali Estate Owners", "Tali Beach, Balaytigue, Nasugbu", "Balaytigue", "Resort", "0917-000-0007", "", 14.1500, 120.6300, "", true, 300.0, "", "", "", false, false),
                BusinessEntity(8, "Nasugbu Beach Park", "Municipal LGU", "Wawa, Nasugbu, Batangas", "Wawa", "Resort", "0917-000-0008", "", 14.0750, 120.6450, "", true, 50.0, "", "", "", false, false),
                BusinessEntity(9, "Terrazas de Punta Fuego", "Punta Fuego Inc", "Nasugbu, Batangas", "Nasugbu", "Resort", "0917-000-0009", "", 14.1400, 120.6100, "", true, 800.0, "", "", "", false, false),
                BusinessEntity(10, "Club Punta Fuego", "Punta Fuego Inc", "Nasugbu, Batangas", "Nasugbu", "Resort", "0917-000-0010", "", 14.1450, 120.6050, "", true, 1200.0, "", "", "", false, false),
                BusinessEntity(11, "Hamilo Coast Beach Resort", "SM Group", "Hamilo Coast, Papaya, Nasugbu", "Papaya", "Resort", "0917-000-0011", "", 14.2250, 120.6120, "", true, 900.0, "", "", "", false, false),
                BusinessEntity(12, "Calayo Cove Eco-Resort", "Calayo Group", "Calayo, Nasugbu, Batangas", "Calayo", "Resort", "0917-000-0012", "", 14.1120, 120.6180, "", true, 150.0, "", "", "", false, false),
                BusinessEntity(13, "Wawa Seaside Resort", "Wawa Dev", "Wawa, Nasugbu, Batangas", "Wawa", "Resort", "0917-000-0013", "", 14.0820, 120.6430, "", true, 120.0, "", "", "", false, false),
                BusinessEntity(14, "Bucana Sunset Resort", "Bucana Ventures", "Bucana, Nasugbu, Batangas", "Bucana", "Resort", "0917-000-0014", "", 14.1370, 120.6130, "", true, 200.0, "", "", "", false, false),
                BusinessEntity(15, "Natipuan Beach Hideaway", "Natipuan Corp", "Natipuan, Nasugbu, Batangas", "Natipuan", "Resort", "0917-000-0015", "", 14.1200, 120.6100, "", true, 250.0, "", "", "", false, false),
                BusinessEntity(16, "Palangan Coral View Resort", "Palangan Group", "Palangan, Nasugbu, Batangas", "Palangan", "Resort", "0917-000-0016", "", 14.1000, 120.6250, "", true, 180.0, "", "", "", false, false),
                BusinessEntity(17, "Looc Shoreline Resort", "Looc LGU", "Looc, Nasugbu, Batangas", "Looc", "Resort", "0917-000-0017", "", 14.0600, 120.5500, "", true, 100.0, "", "", "", false, false),
                BusinessEntity(18, "Banilad Beach Camp", "Banilad Owners", "Banilad, Nasugbu, Batangas", "Banilad", "Resort", "0917-000-0018", "", 14.0850, 120.5800, "", true, 90.0, "", "", "", false, false),
                BusinessEntity(19, "Talim Point Dive Resort", "Talim Divers", "Talim, Nasugbu, Batangas", "Talim", "Resort", "0917-000-0019", "", 14.0400, 120.5200, "", true, 400.0, "", "", "", false, false),
                BusinessEntity(20, "Utod River Resort", "Utod Group", "Utod, Nasugbu, Batangas", "Utod", "Resort", "0917-000-0020", "", 14.0650, 120.6000, "", false, 75.0, "", "", "", false, false),
                
                BusinessEntity(21, "Jollibee Nasugbu", "Jollibee Foods Corp", "Brgy. 3, Nasugbu, Batangas", "Brgy. 3", "Food", "043-123-4567", "", 14.0700, 120.6350, "", false, 0.0, "Fast Food", "", "", false, false),
                BusinessEntity(22, "McDonald's Nasugbu", "Golden Arches Dev Corp", "Nasugbu Town, Batangas", "Brgy. 3", "Food", "043-123-4568", "", 14.0690, 120.6330, "", false, 0.0, "Fast Food", "", "", false, false),
                BusinessEntity(23, "Bayani’s Lomi House", "Ana Garcia", "Wawa, Nasugbu, Batangas", "Wawa", "Food", "0917-000-0023", "", 14.0810, 120.6410, "", false, 0.0, "Filipino", "", "", false, false),
                BusinessEntity(24, "Nanay Charing Lomi", "Charing Santos", "Brgy. 2, Nasugbu, Batangas", "Brgy. 2", "Food", "0917-000-0024", "", 14.0720, 120.6320, "", false, 0.0, "Filipino", "", "", false, false),
                BusinessEntity(25, "Chowking Nasugbu", "Jollibee Foods Corp", "Brgy. 3, Nasugbu, Batangas", "Brgy. 3", "Food", "043-123-4569", "", 14.0705, 120.6352, "", false, 0.0, "Chinese/Fast Food", "", "", false, false),
                BusinessEntity(26, "Mang Inasal Nasugbu", "Jollibee Foods Corp", "Brgy. 3, Nasugbu, Batangas", "Brgy. 3", "Food", "043-123-4570", "", 14.0695, 120.6340, "", false, 0.0, "Filipino Grill", "", "", false, false),
                BusinessEntity(27, "Greenwich Nasugbu", "Jollibee Foods Corp", "Brgy. 3, Nasugbu, Batangas", "Brgy. 3", "Food", "043-123-4571", "", 14.0702, 120.6345, "", false, 0.0, "Pizza & Pasta", "", "", false, false),
                BusinessEntity(28, "Red Ribbon Nasugbu", "Jollibee Foods Corp", "Brgy. 3, Nasugbu, Batangas", "Brgy. 3", "Food", "043-123-4572", "", 14.0708, 120.6358, "", false, 0.0, "Bakery & Cakes", "", "", false, false),
                BusinessEntity(29, "Shakey's Pizza Parlor", "Century Pacific Group", "Brgy. 3, Nasugbu, Batangas", "Brgy. 3", "Food", "043-123-4573", "", 14.0685, 120.6335, "", false, 0.0, "Pizza & Chicken", "", "", false, false),
                BusinessEntity(30, "Kanto Freestyle Breakfast", "Kanto Corp", "Calayo, Nasugbu, Batangas", "Calayo", "Food", "0917-000-0030", "", 14.1110, 120.6210, "", false, 0.0, "Breakfast", "", "", false, false),
                BusinessEntity(31, "Batangas Bulalohan & Lomi", "Aling Nena", "Brgy. 1, Nasugbu, Batangas", "Brgy. 1", "Food", "0917-000-0031", "", 14.0740, 120.6310, "", false, 0.0, "Filipino", "", "", false, false),
                BusinessEntity(32, "Nasugbu Seaside Grill", "Seafood Group", "Wawa, Nasugbu, Batangas", "Wawa", "Food", "0917-000-0032", "", 14.0830, 120.6440, "", false, 0.0, "Seafood", "", "", false, false),
                BusinessEntity(33, "Aling Tina's Gotohan", "Tina Reyes", "Brgy. 4, Nasugbu, Batangas", "Brgy. 4", "Food", "0917-000-0033", "", 14.0720, 120.6350, "", false, 0.0, "Filipino", "", "", false, false),
                BusinessEntity(34, "Kuya J Nasugbu", "Kuya J Group", "Brgy. 3, Nasugbu, Batangas", "Brgy. 3", "Food", "043-123-4574", "", 14.0698, 120.6342, "", false, 0.0, "Filipino", "", "", false, false),
                BusinessEntity(35, "Max's Restaurant Nasugbu", "Max's Group", "Brgy. 3, Nasugbu, Batangas", "Brgy. 3", "Food", "043-123-4575", "", 14.0703, 120.6348, "", false, 0.0, "Filipino", "", "", false, false),
                BusinessEntity(36, "Goldilocks Nasugbu", "Goldilocks Inc", "Brgy. 3, Nasugbu, Batangas", "Brgy. 3", "Food", "043-123-4576", "", 14.0706, 120.6354, "", false, 0.0, "Bakery & Cakes", "", "", false, false),
                BusinessEntity(37, "Paotsin Nasugbu", "Paotsin Corp", "Brgy. 2, Nasugbu, Batangas", "Brgy. 2", "Food", "0917-000-0037", "", 14.0725, 120.6318, "", false, 0.0, "Asian Fast Food", "", "", false, false),
                BusinessEntity(38, "Master Siomai Nasugbu", "Master Siomai", "Brgy. 3, Nasugbu, Batangas", "Brgy. 3", "Food", "0917-000-0038", "", 14.0692, 120.6338, "", false, 0.0, "Dimsum", "", "", false, false),
                BusinessEntity(39, "Hen Lin Nasugbu", "Hen Lin Group", "Brgy. 3, Nasugbu, Batangas", "Brgy. 3", "Food", "0917-000-0039", "", 14.0701, 120.6341, "", false, 0.0, "Dimsum & Noodles", "", "", false, false),
                BusinessEntity(40, "Goto King Nasugbu", "Goto King", "Brgy. 2, Nasugbu, Batangas", "Brgy. 2", "Food", "0917-000-0040", "", 14.0728, 120.6312, "", false, 0.0, "Filipino", "", "", false, false),
                BusinessEntity(41, "Uncle John's Chicken", "Robinsons", "Brgy. 3, Nasugbu, Batangas", "Brgy. 3", "Food", "0917-000-0041", "", 14.0688, 120.6332, "", false, 0.0, "Fast Food", "", "", false, false),
                BusinessEntity(42, "Baliwag Lechon Manok", "Baliwag", "Brgy. 4, Nasugbu, Batangas", "Brgy. 4", "Food", "0917-000-0042", "", 14.0722, 120.6348, "", false, 0.0, "Lechon Manok", "", "", false, false),
                BusinessEntity(43, "Andok's Litson Manok", "Andok's", "Brgy. 3, Nasugbu, Batangas", "Brgy. 3", "Food", "0917-000-0043", "", 14.0694, 120.6345, "", false, 0.0, "Lechon Manok", "", "", false, false),
                BusinessEntity(44, "Chooks-to-Go", "Bounty Fresh", "Brgy. 2, Nasugbu, Batangas", "Brgy. 2", "Food", "0917-000-0044", "", 14.0731, 120.6316, "", false, 0.0, "Roasted Chicken", "", "", false, false),
                BusinessEntity(45, "Sisig Hooray", "Sisig Hooray", "Brgy. 3, Nasugbu, Batangas", "Brgy. 3", "Food", "0917-000-0045", "", 14.0697, 120.6339, "", false, 0.0, "Filipino", "", "", false, false),
                BusinessEntity(46, "Tapa King Nasugbu", "Tapa King", "Brgy. 3, Nasugbu, Batangas", "Brgy. 3", "Food", "0917-000-0046", "", 14.0704, 120.6346, "", false, 0.0, "Filipino Breakfast", "", "", false, false),
                BusinessEntity(47, "Lugaw Republic", "Lugaw Rep", "Brgy. 1, Nasugbu, Batangas", "Brgy. 1", "Food", "0917-000-0047", "", 14.0742, 120.6302, "", false, 0.0, "Filipino", "", "", false, false),
                BusinessEntity(48, "Goto Tendon", "Goto Tendon", "Brgy. 2, Nasugbu, Batangas", "Brgy. 2", "Food", "0917-000-0048", "", 14.0726, 120.6319, "", false, 0.0, "Filipino", "", "", false, false),
                BusinessEntity(49, "Razon's of Guagua", "Razon's", "Brgy. 3, Nasugbu, Batangas", "Brgy. 3", "Food", "0917-000-0049", "", 14.0699, 120.6343, "", false, 0.0, "Filipino Desserts", "", "", false, false),
                BusinessEntity(50, "Cabalen Express", "Cabalen", "Brgy. 3, Nasugbu, Batangas", "Brgy. 3", "Food", "0917-000-0050", "", 14.0707, 120.6349, "", false, 0.0, "Kapampangan Buffet", "", "", false, false),
                BusinessEntity(51, "Bucana Seaside Eatery", "Bucana Locals", "Bucana, Nasugbu, Batangas", "Bucana", "Food", "0917-000-0051", "", 14.1365, 120.6165, "", false, 0.0, "Seafood", "", "", false, false),
                BusinessEntity(52, "Calayo Sunset Diner", "Calayo Cooks", "Calayo, Nasugbu, Batangas", "Calayo", "Food", "0917-000-0052", "", 14.1115, 120.6215, "", false, 0.0, "Filipino Grill", "", "", false, false),
                BusinessEntity(53, "Wawa Fishermen's Grill", "Wawa Co.", "Wawa, Nasugbu, Batangas", "Wawa", "Food", "0917-000-0053", "", 14.0825, 120.6425, "", false, 0.0, "Fresh Seafood", "", "", false, false),
                BusinessEntity(54, "Papaya Native Kitchen", "Papaya Cooks", "Papaya, Nasugbu, Batangas", "Papaya", "Food", "0917-000-0054", "", 14.0915, 120.6015, "", false, 0.0, "Native", "", "", false, false),
                BusinessEntity(55, "Natipuan Country Kitchen", "Natipuan Locals", "Natipuan, Nasugbu, Batangas", "Natipuan", "Food", "0917-000-0055", "", 14.1215, 120.6115, "", false, 0.0, "Filipino", "", "", false, false),
                BusinessEntity(56, "Palangan Barrio Eatery", "Palangan Group", "Palangan, Nasugbu, Batangas", "Palangan", "Food", "0917-000-0056", "", 14.1015, 120.6265, "", false, 0.0, "Filipino", "", "", false, false),
                BusinessEntity(57, "Looc Bay Grill", "Looc Fishermen", "Looc, Nasugbu, Batangas", "Looc", "Food", "0917-000-0057", "", 14.0615, 120.5515, "", false, 0.0, "Seafood", "", "", false, false),
                BusinessEntity(58, "Banilad Native Haus", "Banilad Cooks", "Banilad, Nasugbu, Batangas", "Banilad", "Food", "0917-000-0058", "", 14.0865, 120.5815, "", false, 0.0, "Filipino", "", "", false, false),
                BusinessEntity(59, "Talim Point Grill", "Talim Locals", "Talim, Nasugbu, Batangas", "Talim", "Food", "0917-000-0059", "", 14.0415, 120.5215, "", false, 0.0, "Grilled Seafood", "", "", false, false),
                BusinessEntity(60, "Utod River Canteen", "Utod Group", "Utod, Nasugbu, Batangas", "Utod", "Food", "0917-000-0060", "", 14.0665, 120.6015, "", false, 0.0, "Filipino", "", "", false, false),

                BusinessEntity(61, "Nasugbu Public Market", "Municipality of Nasugbu", "Brgy. 4, Nasugbu, Batangas", "Brgy. 4", "Shop", "043-123-4574", "", 14.0725, 120.6345, "", false, 0.0, "", "5AM - 6PM", "", false, false),
                BusinessEntity(62, "Puregold Nasugbu", "Puregold Price Club Inc", "Nasugbu Town, Batangas", "Brgy. 3", "Shop", "043-123-4575", "", 14.0680, 120.6360, "", false, 0.0, "", "8AM - 9PM", "", false, false),
                BusinessEntity(63, "7-Eleven Nasugbu Town", "Philippine Seven Corp", "Brgy. 3, Nasugbu, Batangas", "Brgy. 3", "Shop", "043-123-4576", "", 14.0710, 120.6340, "", false, 0.0, "", "24 Hours", "", false, false),
                BusinessEntity(64, "7-Eleven Bucana", "Philippine Seven Corp", "Bucana, Nasugbu, Batangas", "Bucana", "Shop", "043-123-4577", "", 14.1360, 120.6160, "", false, 0.0, "", "24 Hours", "", false, false),
                BusinessEntity(65, "Alfamart Nasugbu", "Monde Nissin", "Brgy. 2, Nasugbu, Batangas", "Brgy. 2", "Shop", "043-123-4578", "", 14.0732, 120.6315, "", false, 0.0, "", "6AM - 10PM", "", false, false),
                BusinessEntity(66, "Savemore Market Nasugbu", "SM Investments", "Brgy. 3, Nasugbu, Batangas", "Brgy. 3", "Shop", "043-123-4579", "", 14.0688, 120.6350, "", false, 0.0, "", "8AM - 8PM", "", false, false),
                BusinessEntity(67, "Nasugbu Municipal Hall", "Local Government", "Brgy. 2, Nasugbu, Batangas", "Brgy. 2", "Shop", "043-123-4580", "", 14.0730, 120.6310, "", false, 0.0, "", "8AM - 5PM", "", false, false),
                BusinessEntity(68, "Nasugbu General Hardware", "Tan Family", "Brgy. 4, Nasugbu, Batangas", "Brgy. 4", "Shop", "0917-000-0068", "", 14.0728, 120.6350, "", false, 0.0, "", "8AM - 6PM", "", false, false),
                BusinessEntity(69, "Nasugbu Central Bookstore", "Local Edu", "Brgy. 1, Nasugbu, Batangas", "Brgy. 1", "Shop", "0917-000-0069", "", 14.0740, 120.6300, "", false, 0.0, "", "8AM - 6PM", "", false, false),
                BusinessEntity(70, "Lungsod Balayan Supermarket", "Balayan Group", "Nasugbu Town, Batangas", "Brgy. 3", "Shop", "0917-000-0070", "", 14.0695, 120.6365, "", false, 0.0, "", "8AM - 7PM", "", false, false),
                BusinessEntity(71, "Nasugbu Agri-Supply Center", "Agri Corp", "Brgy. 4, Nasugbu, Batangas", "Brgy. 4", "Shop", "0917-000-0071", "", 14.0723, 120.6347, "", false, 0.0, "", "7AM - 5PM", "", false, false),
                BusinessEntity(72, "Town Center Dry Goods Store", "Local Merchant", "Brgy. 2, Nasugbu, Batangas", "Brgy. 2", "Shop", "0917-000-0072", "", 14.0734, 120.6312, "", false, 0.0, "", "8AM - 6PM", "", false, false),
                BusinessEntity(73, "Sunrise Sari-Sari Hub", "Bucana Locals", "Bucana, Nasugbu, Batangas", "Bucana", "Shop", "0917-000-0073", "", 14.1355, 120.6155, "", false, 0.0, "", "6AM - 9PM", "", false, false),
                BusinessEntity(74, "Calayo Mini Mart", "Calayo Traders", "Calayo, Nasugbu, Batangas", "Calayo", "Shop", "0917-000-0074", "", 14.1105, 120.6205, "", false, 0.0, "", "7AM - 8PM", "", false, false),
                BusinessEntity(75, "Wawa Fishermen's Co-op Store", "Wawa Co-op", "Wawa, Nasugbu, Batangas", "Wawa", "Shop", "0917-000-0075", "", 14.0805, 120.6405, "", false, 0.0, "", "5AM - 7PM", "", false, false),
                BusinessEntity(76, "Papaya Variety Store", "Papaya Merchants", "Papaya, Nasugbu, Batangas", "Papaya", "Shop", "0917-000-0076", "", 14.0905, 120.6005, "", false, 0.0, "", "7AM - 8PM", "", false, false),
                BusinessEntity(77, "Natipuan General Store", "Natipuan Traders", "Natipuan, Nasugbu, Batangas", "Natipuan", "Shop", "0917-000-0077", "", 14.1195, 120.6095, "", false, 0.0, "", "8AM - 6PM", "", false, false),
                BusinessEntity(78, "Palangan Trading Post", "Palangan Merchants", "Palangan, Nasugbu, Batangas", "Palangan", "Shop", "0917-000-0078", "", 14.0995, 120.6245, "", false, 0.0, "", "8AM - 6PM", "", false, false),
                BusinessEntity(79, "Looc Commercial Supply", "Looc Traders", "Looc, Nasugbu, Batangas", "Looc", "Shop", "0917-000-0079", "", 14.0595, 120.5495, "", false, 0.0, "", "8AM - 6PM", "", false, false),
                BusinessEntity(80, "Banilad Hardware Hub", "Banilad Merchants", "Banilad, Nasugbu, Batangas", "Banilad", "Shop", "0917-000-0080", "", 14.0845, 120.5795, "", false, 0.0, "", "8AM - 6PM", "", false, false),
                BusinessEntity(81, "Kaylaway Commercial Center", "Kaylaway Traders", "Kaylaway, Nasugbu, Batangas", "Kaylaway", "Shop", "0917-000-0081", "", 14.0950, 120.6800, "", false, 0.0, "", "8AM - 7PM", "", false, false),
                BusinessEntity(82, "Tumaway Consumer Store", "Tumaway Locals", "Tumaway, Nasugbu, Batangas", "Tumaway", "Shop", "0917-000-0082", "", 14.0850, 120.6700, "", false, 0.0, "", "8AM - 7PM", "", false, false),
                BusinessEntity(83, "Lumbangan Retail Outlet", "Lumbangan Co", "Lumbangan, Nasugbu, Batangas", "Lumbangan", "Shop", "0917-000-0083", "", 14.0750, 120.6600, "", false, 0.0, "", "8AM - 7PM", "", false, false),
                BusinessEntity(84, "Aga Trading Post", "Aga Merchants", "Aga, Nasugbu, Batangas", "Aga", "Shop", "0917-000-0084", "", 14.0650, 120.6500, "", false, 0.0, "", "8AM - 7PM", "", false, false),
                BusinessEntity(85, "Cogusan Mini-Mart", "Cogusan Locals", "Cogusan, Nasugbu, Batangas", "Cogusan", "Shop", "0917-000-0085", "", 14.0550, 120.6400, "", false, 0.0, "", "8AM - 7PM", "", false, false),
                BusinessEntity(86, "Catandala General Merchandise", "Catandala Co", "Catandala, Nasugbu, Batangas", "Catandala", "Shop", "0917-000-0086", "", 14.0450, 120.6300, "", false, 0.0, "", "8AM - 7PM", "", false, false),
                BusinessEntity(87, "Bilaran Variety Shop", "Bilaran Traders", "Bilaran, Nasugbu, Batangas", "Bilaran", "Shop", "0917-000-0087", "", 14.0350, 120.6200, "", false, 0.0, "", "8AM - 7PM", "", false, false),
                BusinessEntity(88, "Bulihan Retail Hub", "Bulihan Co", "Bulihan, Nasugbu, Batangas", "Bulihan", "Shop", "0917-000-0088", "", 14.0250, 120.6100, "", false, 0.0, "", "8AM - 7PM", "", false, false),
                BusinessEntity(89, "Bunducan Trading Post", "Bunducan Locals", "Bunducan, Nasugbu, Batangas", "Bunducan", "Shop", "0917-000-0089", "", 14.0150, 120.6000, "", false, 0.0, "", "8AM - 7PM", "", false, false),
                BusinessEntity(90, "Butucan Supply Store", "Butucan Traders", "Butucan, Nasugbu, Batangas", "Butucan", "Shop", "0917-000-0090", "", 14.0050, 120.5900, "", false, 0.0, "", "8AM - 7PM", "", false, false),

                BusinessEntity(91, "Kape Nasugbu", "Juan Santos", "Brgy. 3, Nasugbu, Batangas", "Brgy. 3", "Cafe", "0917-111-2222", "", 14.0715, 120.6348, "", false, 0.0, "", "", "Barako Coffee", true, false),
                BusinessEntity(92, "Seaside Brew", "Carla Reyes", "Wawa, Nasugbu, Batangas", "Wawa", "Cafe", "0917-333-4444", "", 14.0820, 120.6420, "", false, 0.0, "", "", "Iced Latte", true, false),
                BusinessEntity(93, "Batangas Barako Coffee House", "Ramil Perez", "Calayo, Nasugbu, Batangas", "Calayo", "Cafe", "0917-000-0093", "", 14.1120, 120.6220, "", false, 0.0, "", "", "Kapeng Barako", true, false),
                BusinessEntity(94, "Sunrise Cafe & Pastries", "Elena Gomez", "Bucana, Nasugbu, Batangas", "Bucana", "Cafe", "0917-000-0094", "", 14.1370, 120.6170, "", false, 0.0, "", "", "Croissants & Brew", true, false),
                BusinessEntity(95, "Ocean Breeze Coffee Spot", "Mark Lee", "Papaya, Nasugbu, Batangas", "Papaya", "Cafe", "0917-000-0095", "", 14.0920, 120.6020, "", false, 0.0, "", "", "Cold Brew", false, false),
                BusinessEntity(96, "Tali Bay Cafe", "Tali Group", "Balaytigue, Nasugbu, Batangas", "Balaytigue", "Cafe", "0917-000-0096", "", 14.1510, 120.6310, "", false, 0.0, "", "", "Espresso", true, false),
                BusinessEntity(97, "Punta Fuego Coffee Lounge", "Fuego Corp", "Nasugbu, Batangas", "Nasugbu", "Cafe", "0917-000-0097", "", 14.1410, 120.6110, "", false, 0.0, "", "", "Cappuccino", true, false),
                BusinessEntity(98, "Pico Brew & Pastry", "SM Group", "Papaya, Nasugbu, Batangas", "Papaya", "Cafe", "0917-000-0098", "", 14.2210, 120.6110, "", false, 0.0, "", "", "Matcha Latte", true, false),
                BusinessEntity(99, "Calayo Cliff Cafe", "Calayo Locals", "Calayo, Nasugbu, Batangas", "Calayo", "Cafe", "0917-000-0099", "", 14.1130, 120.6230, "", false, 0.0, "", "", "Batangas Blend", true, false),
                BusinessEntity(100, "Wawa Sunset Coffee Bar", "Wawa Cooks", "Wawa, Nasugbu, Batangas", "Wawa", "Cafe", "0917-000-0100", "", 14.0840, 120.6440, "", false, 0.0, "", "", "Mocha Frappe", true, false),

                BusinessEntity(101, "Mercury Drug Nasugbu", "Mercury Corp", "Brgy. 3, Nasugbu, Batangas", "Brgy. 3", "Pharmacy", "043-987-6543", "", 14.0705, 120.6355, "", false, 0.0, "", "", "", false, true),
                BusinessEntity(102, "The Generics Pharmacy", "LIAHDO Corp", "Brgy. 2, Nasugbu, Batangas", "Brgy. 2", "Pharmacy", "043-555-1234", "", 14.0735, 120.6315, "", false, 0.0, "", "", "", false, false),
                BusinessEntity(103, "Rose Pharmacy Nasugbu", "Rose Pharma Inc", "Brgy. 3, Nasugbu, Batangas", "Brgy. 3", "Pharmacy", "043-123-4588", "", 14.0692, 120.6342, "", false, 0.0, "", "", "", false, false),
                BusinessEntity(104, "South Star Drug Nasugbu", "Robinsons Retail", "Brgy. 4, Nasugbu, Batangas", "Brgy. 4", "Pharmacy", "043-123-4589", "", 14.0722, 120.6342, "", false, 0.0, "", "", "", false, true),
                BusinessEntity(105, "Nasugbu Medical Clinic & Pharmacy", "Dr. Mendoza", "Brgy. 1, Nasugbu, Batangas", "Brgy. 1", "Pharmacy", "0917-000-0105", "", 14.0745, 120.6305, "", false, 0.0, "", "", "", false, true),
                BusinessEntity(106, "Wawa Community Health Pharmacy", "Wawa LGU", "Wawa, Nasugbu, Batangas", "Wawa", "Pharmacy", "0917-000-0106", "", 14.0815, 120.6415, "", false, 0.0, "", "", "", false, false),
                BusinessEntity(107, "Bucana Health Pharmacy", "Bucana LGU", "Bucana, Nasugbu, Batangas", "Bucana", "Pharmacy", "0917-000-0107", "", 14.1355, 120.6155, "", false, 0.0, "", "", "", false, false),
                BusinessEntity(108, "Calayo Pharmacy & Clinic", "Calayo Health", "Calayo, Nasugbu, Batangas", "Calayo", "Pharmacy", "0917-000-0108", "", 14.1105, 120.6205, "", false, 0.0, "", "", "", false, false),
                BusinessEntity(109, "Papaya Health Depot", "Papaya Health", "Papaya, Nasugbu, Batangas", "Papaya", "Pharmacy", "0917-000-0109", "", 14.0905, 120.6005, "", false, 0.0, "", "", "", false, false),
                BusinessEntity(110, "Tali Medical Pharmacy", "Tali Health", "Balaytigue, Nasugbu, Batangas", "Balaytigue", "Pharmacy", "0917-000-0110", "", 14.1505, 120.6305, "", false, 0.0, "", "", "", false, false)
            )
        }

        fun entityToBusiness(entity: BusinessEntity): Business {
            return when (entity.category) {
                "Resort" -> Resort(entity.id, entity.name, entity.ownerName, entity.address, entity.barangay, entity.phone, entity.imageName, entity.latitude, entity.longitude, entity.hasBeachAccess, entity.entranceFee)
                "Food" -> Restaurant(entity.id, entity.name, entity.ownerName, entity.address, entity.barangay, entity.phone, entity.imageName, entity.latitude, entity.longitude, entity.cuisineType)
                "Shop" -> Shop(entity.id, entity.name, entity.ownerName, entity.address, entity.barangay, entity.phone, entity.imageName, entity.latitude, entity.longitude, entity.openingHours)
                "Cafe" -> Cafe(entity.id, entity.name, entity.ownerName, entity.address, entity.barangay, entity.phone, entity.imageName, entity.latitude, entity.longitude, entity.specialty, entity.hasWifi)
                "Pharmacy" -> Pharmacy(entity.id, entity.name, entity.ownerName, entity.address, entity.barangay, entity.phone, entity.imageName, entity.latitude, entity.longitude, entity.is24Hours)
                else -> Shop(entity.id, entity.name, entity.ownerName, entity.address, entity.barangay, entity.phone, entity.imageName, entity.latitude, entity.longitude, "")
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

                imgBanner.setBackgroundColor(android.graphics.Color.parseColor("#4E6C50"))
                imgBanner.setImageResource(R.mipmap.ic_launcher)
                imgBanner.scaleType = ImageView.ScaleType.CENTER_INSIDE

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

            holder.thumbnail.setBackgroundColor(android.graphics.Color.parseColor("#E8EAF6"))
            holder.thumbnail.setImageResource(R.mipmap.ic_launcher)
            holder.thumbnail.scaleType = ImageView.ScaleType.CENTER_INSIDE

            holder.itemView.setOnClickListener { onClick(b) }
        }

        override fun getItemCount() = list.size

        val updateList: (List<Business>) -> Unit = { newList ->
            list = newList
            notifyDataSetChanged()
        }
    }
}

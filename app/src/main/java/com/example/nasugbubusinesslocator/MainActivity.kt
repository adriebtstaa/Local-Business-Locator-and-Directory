package com.example.nasugbubusinesslocator

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Listahan ng Businesses sa Nasugbu
        val nasugbuTown = LatLng(14.0717, 120.6329)
        val kawayanCove = LatLng(14.1350, 120.6150)
        val fortuneIsland = LatLng(14.0580, 120.4870)
        val publicMarket = LatLng(14.0725, 120.6345)
        val jollibeeNasugbu = LatLng(14.0700, 120.6350)

        mMap.addMarker(MarkerOptions().position(nasugbuTown).title("Nasugbu Town Center"))
        mMap.addMarker(MarkerOptions().position(kawayanCove).title("Kawayan Cove"))
        mMap.addMarker(MarkerOptions().position(fortuneIsland).title("Fortune Island Resort"))
        mMap.addMarker(MarkerOptions().position(publicMarket).title("Nasugbu Public Market"))
        mMap.addMarker(MarkerOptions().position(jollibeeNasugbu).title("Jollibee Nasugbu"))

        // I-zoom sa Nasugbu
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nasugbuTown, 12f))
    }
}
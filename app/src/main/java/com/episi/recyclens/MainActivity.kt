package com.episi.recyclens

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.episi.recyclens.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var navItems: List<NavItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupCustomBottomNavigation()
    }

    private fun setupNavigation() {
        navController = findNavController(R.id.nav_host_fragment_activity_main)

        // Configuración de la AppBar (si la necesitas)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_details, R.id.navigation_camera,
                R.id.navigation_map, R.id.navigation_profile
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    private fun setupCustomBottomNavigation() {
        // Inicializar los elementos del navbar usando ViewBinding
        navItems = listOf(
            NavItem(
                container = binding.navHome,
                icon = binding.iconHome,
                text = binding.textHome,
                destinationId = R.id.navigation_home
            ),
            NavItem(
                container = binding.navDetails,
                icon = binding.iconDetails,
                text = binding.textDetails,
                destinationId = R.id.navigation_details
            ),
            NavItem(
                container = binding.navMap,
                icon = binding.iconMap,
                text = binding.textMap,
                destinationId = R.id.navigation_map
            ),
            NavItem(
                container = binding.navProfile,
                icon = binding.iconProfile,
                text = binding.textProfile,
                destinationId = R.id.navigation_profile
            )
        )

        // Configurar el FAB de la cámara
        binding.navCamera.setOnClickListener {
            navigateToDestination(R.id.navigation_camera)
        }

        // Configurar click listeners para los demás items
        navItems.forEach { navItem ->
            navItem.container.setOnClickListener {
                navigateToDestination(navItem.destinationId)
            }
        }

        // Escuchar cambios de destino para actualizar el estado visual
        navController.addOnDestinationChangedListener { _, destination, _ ->
            updateNavItemsState(destination.id)
        }
    }

    private fun navigateToDestination(destinationId: Int) {
        if (navController.currentDestination?.id != destinationId) {
            navController.navigate(destinationId)
        }
    }

    private fun updateNavItemsState(currentDestinationId: Int) {
        navItems.forEach { navItem ->
            val isSelected = navItem.destinationId == currentDestinationId
            updateNavItemAppearance(navItem, isSelected)
        }

        // Accede al FAB y su texto directamente desde binding
        val isCameraSelected = currentDestinationId == R.id.navigation_camera

        val cameraIconColor = if (isCameraSelected) {
            ContextCompat.getColor(this, R.color.nav_icon_active)
        } else {
            ContextCompat.getColor(this, R.color.nav_icon_inactive)
        }

        val cameraTextColor = if (isCameraSelected) {
            ContextCompat.getColor(this, R.color.nav_text_active)
        } else {
            ContextCompat.getColor(this, R.color.nav_text_inactive)
        }

        // Aplica color al ícono del FAB
        binding.navCamera.imageTintList = ContextCompat.getColorStateList(
            this, if (isCameraSelected)
                R.color.nav_icon_active else R.color.nav_icon_inactive
        )

        // Aplica color al texto debajo del FAB
        binding.textCamera.setTextColor(cameraTextColor)
    }

    private fun updateNavItemAppearance(navItem: NavItem, isSelected: Boolean) {
        val iconColor = if (isSelected) {
            ContextCompat.getColor(this, R.color.nav_icon_active)
        } else {
            ContextCompat.getColor(this, R.color.nav_icon_inactive)
        }

        val textColor = if (isSelected) {
            ContextCompat.getColor(this, R.color.nav_text_active)
        } else {
            ContextCompat.getColor(this, R.color.nav_text_inactive)
        }

        navItem.icon.setColorFilter(iconColor)
        navItem.text.setTextColor(textColor)
    }

    data class NavItem(
        val container: LinearLayout,
        val icon: ImageView,
        val text: TextView,
        val destinationId: Int
    )
}
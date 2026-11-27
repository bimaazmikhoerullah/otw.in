package com.example.otwin

import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.example.otwin.database.FirebaseService
import com.example.otwin.database.UserProfile
import com.example.otwin.databinding.ActivityMainBinding
import com.example.otwin.databinding.DialogBookingBinding
import com.example.otwin.databinding.DialogNameBinding
import com.example.otwin.databinding.ItemOrderBinding
import com.example.otwin.databinding.ItemVehicleBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val startCalendar = Calendar.getInstance()
    private val endCalendar = Calendar.getInstance().apply { add(Calendar.DATE, 2) }
    private val displayDateFormat = SimpleDateFormat("EEE, d MMM", Locale.forLanguageTag("id-ID"))
    private val orderDateFormat = SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("id-ID"))

    private var lepasKunci = true
    private var homeCategory: String? = null
    private var searchCategory: String? = VehicleRepository.CATEGORY_ALL
    private var profile = UserProfile()
    private var vehicles: List<Vehicle> = emptyList()

    private var ordersReference: DatabaseReference? = null
    private var ordersListener: ValueEventListener? = null
    private var vehiclesReference: DatabaseReference? = null
    private var vehiclesListener: ValueEventListener? = null
    private var adminReference: DatabaseReference? = null
    private var adminListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseService.initialize(applicationContext)
        redirectAdminIfNeeded()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        restoreLocation()
        loadProfile()
        updateDateLabels()
        setupListeners()
        highlightHomeCategory(null)
        highlightSearchCategory(VehicleRepository.CATEGORY_ALL)
        showPage(PAGE_HOME)
        observeVehicles()
        observeOrders()
        observeAdmin()
    }

    override fun onDestroy() {
        ordersListener?.let { ordersReference?.removeEventListener(it) }
        vehiclesListener?.let { vehiclesReference?.removeEventListener(it) }
        adminListener?.let { adminReference?.removeEventListener(it) }
        super.onDestroy()
    }

    private fun sessionPreferences() = getSharedPreferences(PREFS_SESSION, MODE_PRIVATE)

    /** Akun admin tidak boleh mendarat di UI customer, termasuk saat sesi dipulihkan. */
    private fun redirectAdminIfNeeded() {
        val user = FirebaseService.auth.currentUser ?: return
        if (!FirebaseService.isDatabaseConfigured) return

        VehicleRepository.isAdmin(user.uid) { isAdmin ->
            if (!isAdmin || isFinishing || isDestroyed) return@isAdmin
            val intent = Intent(this, AdminActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    // ----------------------------------------------------------------------
    // Profile / account
    // ----------------------------------------------------------------------

    /** Greet with the registered RTDB name when available, otherwise the email name. */
    private fun loadProfile() {
        val user = FirebaseService.auth.currentUser ?: return
        val fallback = user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore('@')?.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
            ?: getString(R.string.home_guest)

        profile = profile.copy(name = fallback, email = user.email.orEmpty())
        applyProfile(fallback)

        if (!FirebaseService.isDatabaseConfigured) return
        FirebaseService.usersReference().child(user.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    profile = snapshot.getValue(UserProfile::class.java) ?: profile
                    applyProfile(fallback)
                }

                override fun onCancelled(error: DatabaseError) = Unit
            })
    }

    private fun applyProfile(fallbackName: String) {
        val name = profile.name.trim().ifBlank { fallbackName }
        binding.tvGreeting.text = getString(R.string.home_greeting, name)
        binding.tvAccountName.text = name
        binding.tvAccountEmail.text = profile.email.ifBlank {
            FirebaseService.auth.currentUser?.email.orEmpty()
        }
        binding.tvAccountUsername.text = profile.username
            .takeIf { it.isNotBlank() }
            ?.let { "@$it" }
            ?: "-"
        binding.tvAccountPhone.text = profile.phone.ifBlank { "-" }
    }

    private fun showNameDialog() {
        val dialogBinding = DialogNameBinding.inflate(layoutInflater)
        dialogBinding.txtNewName.setText(
            binding.tvAccountName.text.toString().takeIf { it.isNotBlank() }
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.account_edit_name)
            .setView(dialogBinding.root)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok, null)
            .create()
            .apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val newName = dialogBinding.txtNewName.text.toString().trim()
                        if (newName.isEmpty()) {
                            toast(R.string.account_name_empty)
                            return@setOnClickListener
                        }
                        saveName(newName)
                        dismiss()
                    }
                }
                show()
            }
    }

    private fun saveName(newName: String) {
        val user = FirebaseService.auth.currentUser ?: return
        if (!FirebaseService.isDatabaseConfigured) {
            toast(R.string.auth_not_configured)
            return
        }

        val updated = if (profile.createdAt == 0L) {
            UserProfile(
                name = newName,
                email = user.email.orEmpty(),
                createdAt = System.currentTimeMillis()
            )
        } else {
            profile.copy(name = newName)
        }

        FirebaseService.usersReference().child(user.uid).setValue(updated)
            .addOnSuccessListener {
                profile = updated
                applyProfile(newName)
                toast(R.string.account_name_saved)
            }
            .addOnFailureListener { toast(R.string.auth_generic_error) }
    }

    private fun observeAdmin() {
        val user = FirebaseService.auth.currentUser
        if (user == null || !FirebaseService.isDatabaseConfigured) {
            hideAdminEntry()
            return
        }
        val reference = VehicleRepository.adminsReference().child(user.uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isAdmin = snapshot.getValue(Boolean::class.java) == true
                binding.btnAdminPanel.visibility = if (isAdmin) View.VISIBLE else View.GONE
                binding.adminDivider.visibility = if (isAdmin) View.VISIBLE else View.GONE
            }

            override fun onCancelled(error: DatabaseError) = Unit
        }
        adminReference = reference
        adminListener = listener
        reference.addValueEventListener(listener)
    }

    private fun hideAdminEntry() {
        binding.btnAdminPanel.visibility = View.GONE
        binding.adminDivider.visibility = View.GONE
    }

    // ----------------------------------------------------------------------
    // Navigation and listeners
    // ----------------------------------------------------------------------

    private fun setupListeners() {
        binding.btnLepasKunci.setOnClickListener { setRentalMode(lepasKunci = true) }
        binding.btnDenganSopir.setOnClickListener { setRentalMode(lepasKunci = false) }

        binding.cardStart.setOnClickListener {
            pickDate(startCalendar) { chosen ->
                if (chosen.after(endCalendar)) endCalendar.time = chosen.time
            }
        }
        binding.cardEnd.setOnClickListener {
            pickDate(endCalendar) { chosen ->
                if (chosen.before(startCalendar)) startCalendar.time = chosen.time
            }
        }

        binding.btnCariArmada.setOnClickListener {
            showPage(PAGE_SEARCH)
            binding.txtSearch.requestFocus()
        }
        binding.tvLihatSemua.setOnClickListener { showPage(PAGE_SEARCH) }

        binding.btnLocation.setOnClickListener { showLocationDialog() }
        binding.btnProfile.setOnClickListener { showPage(PAGE_ACCOUNT) }
        binding.btnChatCs.setOnClickListener { openWhatsApp() }

        val homeChips = listOf(
            binding.chipAll to VehicleRepository.CATEGORY_ALL,
            binding.chipMpv to VehicleRepository.CATEGORY_MPV,
            binding.chipCity to VehicleRepository.CATEGORY_CITY,
            binding.chipEv to VehicleRepository.CATEGORY_EV,
            binding.chipSuv to VehicleRepository.CATEGORY_SUV,
            binding.chipSedan to VehicleRepository.CATEGORY_SEDAN
        )
        homeChips.forEach { (chip, category) -> chip.setOnClickListener { setHomeCategory(category) } }

        val searchChips = listOf(
            binding.searchChipAll to VehicleRepository.CATEGORY_ALL,
            binding.searchChipMpv to VehicleRepository.CATEGORY_MPV,
            binding.searchChipCity to VehicleRepository.CATEGORY_CITY,
            binding.searchChipEv to VehicleRepository.CATEGORY_EV,
            binding.searchChipSuv to VehicleRepository.CATEGORY_SUV,
            binding.searchChipSedan to VehicleRepository.CATEGORY_SEDAN
        )
        searchChips.forEach { (chip, category) -> chip.setOnClickListener { setSearchCategory(category) } }

        binding.txtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = renderSearchResults()
        })

        binding.navBeranda.setOnClickListener { showPage(PAGE_HOME) }
        binding.navCari.setOnClickListener { showPage(PAGE_SEARCH) }
        binding.navPesanan.setOnClickListener { showPage(PAGE_ORDERS) }
        binding.navAkun.setOnClickListener { showPage(PAGE_ACCOUNT) }

        binding.btnEditName.setOnClickListener { showNameDialog() }
        binding.btnAccountOrders.setOnClickListener { showPage(PAGE_ORDERS) }
        binding.btnAdminPanel.setOnClickListener {
            startActivity(Intent(this, AdminActivity::class.java))
        }
        binding.btnLogoutAccount.setOnClickListener { logout() }
    }

    private fun showPage(page: Int) {
        binding.homeScroll.visibility = if (page == PAGE_HOME) View.VISIBLE else View.GONE
        binding.searchScroll.visibility = if (page == PAGE_SEARCH) View.VISIBLE else View.GONE
        binding.ordersScroll.visibility = if (page == PAGE_ORDERS) View.VISIBLE else View.GONE
        binding.accountScroll.visibility = if (page == PAGE_ACCOUNT) View.VISIBLE else View.GONE
        selectNav(page)

        if (page == PAGE_ORDERS) observeOrders()
        if (page == PAGE_ACCOUNT) loadProfile()
    }

    private fun selectNav(selectedIndex: Int) {
        val navItems = listOf(binding.navBeranda, binding.navCari, binding.navPesanan, binding.navAkun)
        val navIcons = listOf(
            binding.navBerandaIcon, binding.navCariIcon,
            binding.navPesananIcon, binding.navAkunIcon
        )
        val navLabels = listOf(
            binding.navBerandaLabel, binding.navCariLabel,
            binding.navPesananLabel, binding.navAkunLabel
        )

        val primary = ContextCompat.getColor(this, R.color.home_primary)
        val muted = ContextCompat.getColor(this, R.color.home_on_surface_variant)

        navItems.forEachIndexed { index, item -> item.isSelected = index == selectedIndex }
        navIcons.forEachIndexed { index, icon ->
            icon.setColorFilter(if (index == selectedIndex) primary else muted)
        }
        navLabels.forEachIndexed { index, label ->
            label.setTextColor(if (index == selectedIndex) primary else muted)
            label.setTypeface(
                null,
                if (index == selectedIndex) Typeface.BOLD else Typeface.NORMAL
            )
        }
    }

    // ----------------------------------------------------------------------
    // Beranda: dates, mode, categories, location
    // ----------------------------------------------------------------------

    private fun pickDate(target: Calendar, onPicked: (Calendar) -> Unit) {
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                target.set(year, month, dayOfMonth)
                onPicked(target)
                updateDateLabels()
            },
            target.get(Calendar.YEAR),
            target.get(Calendar.MONTH),
            target.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateLabels() {
        binding.tvStartDate.text = displayDateFormat.format(startCalendar.time)
        binding.tvEndDate.text = displayDateFormat.format(endCalendar.time)
        binding.tvDuration.text = getString(R.string.home_duration, durationLabel())
        refreshVehicleLists()
    }

    private fun durationLabel(): String = getString(R.string.home_duration_days, durationDays())

    private fun durationDays(): Int {
        val diff = startOfDay(endCalendar).timeInMillis - startOfDay(startCalendar).timeInMillis
        return (diff / MILLIS_PER_DAY).toInt().coerceAtLeast(0) + 1
    }

    private fun startOfDay(calendar: Calendar): Calendar =
        Calendar.getInstance().apply {
            set(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
                0, 0, 0
            )
            set(Calendar.MILLISECOND, 0)
        }

    private fun setRentalMode(lepasKunci: Boolean) {
        if (this.lepasKunci == lepasKunci) return
        this.lepasKunci = lepasKunci

        val primary = ContextCompat.getColor(this, R.color.home_primary)
        val muted = ContextCompat.getColor(this, R.color.home_on_surface_variant)

        binding.btnLepasKunci.background = if (lepasKunci) {
            ContextCompat.getDrawable(this, R.drawable.bg_home_toggle_active)
        } else {
            ColorDrawable(Color.TRANSPARENT)
        }
        binding.btnDenganSopir.background = if (lepasKunci) {
            ColorDrawable(Color.TRANSPARENT)
        } else {
            ContextCompat.getDrawable(this, R.drawable.bg_home_toggle_active)
        }

        binding.btnLepasKunci.setTextColor(if (lepasKunci) primary else muted)
        binding.btnDenganSopir.setTextColor(if (lepasKunci) muted else primary)
        tintStartDrawable(binding.btnLepasKunci, if (lepasKunci) primary else muted)
        tintStartDrawable(binding.btnDenganSopir, if (lepasKunci) muted else primary)

        refreshVehicleLists()
        toast(if (lepasKunci) R.string.home_rent_mode_on else R.string.home_driver_mode)
    }

    private fun tintStartDrawable(view: TextView, color: Int) {
        view.compoundDrawablesRelative.getOrNull(0)?.let {
            DrawableCompat.setTint(it.mutate(), color)
        }
    }

    private fun setHomeCategory(category: String) {
        homeCategory = category.takeIf { it != VehicleRepository.CATEGORY_ALL }
        highlightHomeCategory(homeCategory)
        renderHomeVehicles()
    }

    private fun highlightHomeCategory(category: String?) {
        val chips = listOf(
            binding.chipAll to VehicleRepository.CATEGORY_ALL,
            binding.chipMpv to VehicleRepository.CATEGORY_MPV,
            binding.chipCity to VehicleRepository.CATEGORY_CITY,
            binding.chipEv to VehicleRepository.CATEGORY_EV,
            binding.chipSuv to VehicleRepository.CATEGORY_SUV,
            binding.chipSedan to VehicleRepository.CATEGORY_SEDAN
        )
        val activeKey = category ?: VehicleRepository.CATEGORY_ALL
        chips.forEach { (chip, key) ->
            val active = key == activeKey
            chip.setBackgroundResource(
                if (active) R.drawable.bg_home_chip_active else R.drawable.bg_home_chip
            )
            val color = ContextCompat.getColor(
                this,
                if (active) R.color.home_surface else R.color.home_on_surface_variant
            )
            (chip as? ViewGroup)?.let { group ->
                (group.getChildAt(0) as? ImageView)?.setColorFilter(color)
                (group.getChildAt(1) as? TextView)?.setTextColor(color)
            }
        }
    }

    private fun showLocationDialog() {
        val cities = resources.getStringArray(R.array.location_cities)
        AlertDialog.Builder(this)
            .setTitle(R.string.home_location_title)
            .setItems(cities) { _, which ->
                val city = cities[which]
                sessionPreferences().edit().putString(KEY_LOCATION, city).apply()
                binding.tvHomeLocation.text = city
                toast(getString(R.string.home_location_saved, city))
            }
            .show()
    }

    private fun restoreLocation() {
        val city = sessionPreferences().getString(KEY_LOCATION, null)
        if (!city.isNullOrBlank()) binding.tvHomeLocation.text = city
    }

    // ----------------------------------------------------------------------
    // Fleet from Realtime Database
    // ----------------------------------------------------------------------

    private fun observeVehicles() {
        if (!FirebaseService.isDatabaseConfigured) {
            vehicles = emptyList()
            refreshVehicleLists()
            return
        }
        val reference = VehicleRepository.reference()
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                vehicles = snapshot.children.mapNotNull { child ->
                    child.getValue(Vehicle::class.java)?.copy(id = child.key.orEmpty())
                }.filter { it.active }.sortedBy { it.name }
                refreshVehicleLists()
            }

            override fun onCancelled(error: DatabaseError) {
                toast(R.string.vehicles_load_error)
            }
        }
        vehiclesReference = reference
        vehiclesListener = listener
        reference.addValueEventListener(listener)
    }

    private fun refreshVehicleLists() {
        renderHomeVehicles()
        renderSearchResults()
    }

    private fun renderHomeVehicles() {
        val results = VehicleRepository.filter(vehicles, "", homeCategory)
        binding.homeVehicleContainer.removeAllViews()
        binding.tvHomeEmpty.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
        results.forEach { vehicle ->
            bindVehicleCard(binding.homeVehicleContainer, vehicle)
        }
    }

    private fun renderSearchResults() {
        val results = VehicleRepository.filter(
            vehicles,
            binding.txtSearch.text.toString(),
            searchCategory
        )
        binding.tvSearchResultInfo.text = getString(R.string.search_result_count, results.size)
        binding.tvEmptySearch.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
        binding.vehicleListContainer.removeAllViews()
        results.forEach { vehicle ->
            bindVehicleCard(binding.vehicleListContainer, vehicle)
        }
    }

    private fun bindVehicleCard(container: ViewGroup, vehicle: Vehicle) {
        val itemBinding = ItemVehicleBinding.inflate(layoutInflater, container, false)
        VehicleImageLoader.load(itemBinding.imgVehicle, vehicle.imageUrl)
        itemBinding.tvBadge.text = vehicle.badge.ifBlank { vehicle.category.uppercase() }
        itemBinding.tvBadge.setBackgroundResource(
            if (vehicle.badgeType == "green") {
                R.drawable.bg_home_badge_green
            } else {
                R.drawable.bg_home_badge_blue
            }
        )
        itemBinding.tvRating.text = if (vehicle.rating.isBlank()) {
            "-"
        } else {
            getString(R.string.home_rating_format, vehicle.rating, vehicle.reviews)
        }
        itemBinding.tvBrand.text = vehicle.brand
        itemBinding.tvName.text = vehicle.name
        itemBinding.tvSpecs.text = listOf(vehicle.seats, vehicle.transmission, vehicle.feature)
            .filter { it.isNotBlank() }
            .joinToString(" • ")
        itemBinding.tvPrice.text = dailyPriceLabel(vehicle) + " " + getString(R.string.home_per_day)

        itemBinding.root.setOnClickListener { openBooking(vehicle) }
        itemBinding.btnRentItem.setOnClickListener { openBooking(vehicle) }
        container.addView(itemBinding.root)
    }

    private fun setSearchCategory(category: String) {
        searchCategory = category
        highlightSearchCategory(category)
        renderSearchResults()
    }

    private fun highlightSearchCategory(category: String?) {
        val chips = listOf(
            binding.searchChipAll to VehicleRepository.CATEGORY_ALL,
            binding.searchChipMpv to VehicleRepository.CATEGORY_MPV,
            binding.searchChipCity to VehicleRepository.CATEGORY_CITY,
            binding.searchChipEv to VehicleRepository.CATEGORY_EV,
            binding.searchChipSuv to VehicleRepository.CATEGORY_SUV,
            binding.searchChipSedan to VehicleRepository.CATEGORY_SEDAN
        )
        chips.forEach { (chip, key) ->
            val active = key == (category ?: VehicleRepository.CATEGORY_ALL)
            chip.setBackgroundResource(
                if (active) R.drawable.bg_home_chip_active else R.drawable.bg_home_chip
            )
            chip.setTextColor(
                ContextCompat.getColor(
                    this,
                    if (active) R.color.home_surface else R.color.home_on_surface_variant
                )
            )
        }
    }

    // ----------------------------------------------------------------------
    // Booking / Pesanan
    // ----------------------------------------------------------------------

    private fun dailyRate(vehicle: Vehicle): Long =
        vehicle.pricePerDay + if (lepasKunci) 0L else DRIVER_FEE_PER_DAY

    private fun modeLabel(): String = getString(
        if (lepasKunci) R.string.home_mode_rent else R.string.home_mode_driver
    )

    private fun dailyPriceLabel(vehicle: Vehicle): String = formatRupiah(dailyRate(vehicle))

    private fun totalPrice(vehicle: Vehicle): Long {
        val days = durationDays()
        val subtotal = dailyRate(vehicle) * days
        val discount = if (days >= 3) subtotal * 10 / 100 else 0
        return subtotal - discount
    }

    private fun openBooking(vehicle: Vehicle) {
        if (FirebaseService.auth.currentUser == null) {
            toast(R.string.auth_login_required)
            return
        }

        val dialogBinding = DialogBookingBinding.inflate(layoutInflater)
        val days = durationDays()
        dialogBinding.tvBookingVehicle.text = listOf(vehicle.brand, vehicle.name)
            .filter { it.isNotBlank() }
            .joinToString(" ")
        dialogBinding.tvBookingDates.text = getString(
            R.string.orders_date_range,
            orderDateFormat.format(startCalendar.time),
            orderDateFormat.format(endCalendar.time)
        )
        val driverLine = if (lepasKunci) {
            ""
        } else {
            "\n" + getString(R.string.booking_driver_fee, formatRupiah(DRIVER_FEE_PER_DAY))
        }
        dialogBinding.tvBookingMode.text = modeLabel() + driverLine
        dialogBinding.tvBookingBreakdown.text = getString(
            R.string.booking_breakdown,
            formatRupiah(dailyRate(vehicle)),
            days,
            if (days >= 3) getString(R.string.booking_discount) else ""
        )
        dialogBinding.tvBookingTotal.text = getString(R.string.booking_total_label) +
            "\n" + formatRupiah(totalPrice(vehicle))

        AlertDialog.Builder(this)
            .setTitle(R.string.booking_title)
            .setView(dialogBinding.root)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.booking_confirm) { _, _ -> saveOrder(vehicle) }
            .show()
    }

    private fun saveOrder(vehicle: Vehicle) {
        val user = FirebaseService.auth.currentUser ?: return
        if (!FirebaseService.isDatabaseConfigured) {
            toast(R.string.auth_not_configured)
            return
        }

        val orders = FirebaseService.rootReference().child(KEY_ORDERS).child(user.uid)
        val key = orders.push().key
        if (key.isNullOrBlank()) {
            toast(R.string.booking_failed)
            return
        }

        val order = RentalOrder(
            id = key,
            vehicleId = vehicle.id,
            vehicleName = vehicle.name,
            brand = vehicle.brand,
            imageUrl = vehicle.imageUrl,
            customerName = profile.name,
            customerUsername = profile.username,
            startDate = orderDateFormat.format(startCalendar.time),
            endDate = orderDateFormat.format(endCalendar.time),
            durationDays = durationDays(),
            mode = modeLabel(),
            total = totalPrice(vehicle),
            status = getString(R.string.orders_status_processing),
            createdAt = System.currentTimeMillis()
        )

        orders.child(key).setValue(order)
            .addOnSuccessListener {
                toast(R.string.booking_saved)
                showPage(PAGE_ORDERS)
            }
            .addOnFailureListener { toast(R.string.booking_failed) }
    }

    private fun observeOrders() {
        ordersListener?.let { ordersReference?.removeEventListener(it) }
        ordersListener = null
        ordersReference = null

        binding.tvOrdersSummary.text = getString(R.string.orders_summary, 0)
        binding.tvEmptyOrders.visibility = View.VISIBLE
        binding.ordersContainer.removeAllViews()

        val user = FirebaseService.auth.currentUser
        if (user == null || !FirebaseService.isDatabaseConfigured) return

        val reference = FirebaseService.rootReference().child(KEY_ORDERS).child(user.uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val orders = snapshot.children.mapNotNull { child ->
                    child.getValue(RentalOrder::class.java)?.copy(id = child.key.orEmpty())
                }.sortedByDescending { it.createdAt }
                renderOrders(orders)
            }

            override fun onCancelled(error: DatabaseError) {
                toast(R.string.orders_load_error)
            }
        }

        ordersReference = reference
        ordersListener = listener
        reference.addValueEventListener(listener)
    }

    private fun renderOrders(orders: List<RentalOrder>) {
        binding.tvOrdersSummary.text = getString(R.string.orders_summary, orders.size)
        binding.tvEmptyOrders.visibility = if (orders.isEmpty()) View.VISIBLE else View.GONE
        binding.ordersContainer.removeAllViews()

        orders.forEach { order ->
            val itemBinding = ItemOrderBinding.inflate(
                layoutInflater,
                binding.ordersContainer,
                false
            )
            VehicleImageLoader.load(itemBinding.imgOrderVehicle, order.imageUrl)
            itemBinding.tvOrderVehicle.text = order.vehicleName.ifBlank { order.brand }
            itemBinding.tvOrderDates.text = getString(
                R.string.orders_date_range,
                order.startDate,
                order.endDate
            )
            itemBinding.tvOrderMode.text = getString(
                R.string.orders_duration,
                order.durationDays,
                order.mode.ifBlank { modeLabel() }
            )
            itemBinding.tvOrderTotal.text = getString(R.string.orders_total, formatRupiah(order.total))

            val closed = order.status == getString(R.string.orders_status_cancelled) ||
                order.status == getString(R.string.orders_status_completed)
            itemBinding.tvOrderStatus.text = order.status.ifBlank {
                getString(R.string.orders_status_processing)
            }
            itemBinding.tvOrderStatus.setBackgroundResource(
                if (closed) R.drawable.bg_home_badge_white else R.drawable.bg_home_badge_green
            )
            itemBinding.btnCancelOrder.visibility = if (closed) View.GONE else View.VISIBLE
            itemBinding.btnCancelOrder.setOnClickListener { confirmCancelOrder(order) }
            binding.ordersContainer.addView(itemBinding.root)
        }
    }

    private fun confirmCancelOrder(order: RentalOrder) {
        AlertDialog.Builder(this)
            .setTitle(R.string.orders_cancel)
            .setMessage(R.string.orders_cancel_confirm)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.orders_cancel) { _, _ -> cancelOrder(order) }
            .show()
    }

    private fun cancelOrder(order: RentalOrder) {
        val user = FirebaseService.auth.currentUser
        if (user == null || !FirebaseService.isDatabaseConfigured) {
            toast(R.string.auth_not_configured)
            return
        }
        val updated = order.copy(status = getString(R.string.orders_status_cancelled))
        FirebaseService.rootReference()
            .child(KEY_ORDERS)
            .child(user.uid)
            .child(order.id)
            .setValue(updated)
            .addOnSuccessListener { toast(R.string.orders_cancelled) }
            .addOnFailureListener { toast(R.string.auth_generic_error) }
    }

    // ----------------------------------------------------------------------
    // Misc
    // ----------------------------------------------------------------------

    private fun openWhatsApp() {
        val message = Uri.encode("Halo otw.in, saya butuh bantuan sewa mobil.")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/?text=$message"))
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            toast(R.string.home_chat_unavailable)
        }
    }

    private fun logout() {
        FirebaseService.auth.signOut()
        sessionPreferences().edit().putBoolean(KEY_REMEMBER, false).apply()

        val intent = Intent(this, Login::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun toast(message: CharSequence) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun toast(messageRes: Int) {
        Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val PREFS_SESSION = "otwin_session"
        private const val KEY_REMEMBER = "remember_me"
        private const val KEY_LOCATION = "location"
        private const val KEY_ORDERS = "orders"
        private const val DRIVER_FEE_PER_DAY = 250_000L
        private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

        private const val PAGE_HOME = 0
        private const val PAGE_SEARCH = 1
        private const val PAGE_ORDERS = 2
        private const val PAGE_ACCOUNT = 3
    }
}

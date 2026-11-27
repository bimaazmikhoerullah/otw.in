package com.example.otwin

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.otwin.database.FirebaseService
import com.example.otwin.database.UserProfile
import com.example.otwin.databinding.ActivityAdminBinding
import com.example.otwin.databinding.DialogVehicleBinding
import com.example.otwin.databinding.ItemAdminActivityBinding
import com.example.otwin.databinding.ItemAdminOrderBinding
import com.example.otwin.databinding.ItemAdminVehicleBinding
import com.example.otwin.databinding.ItemAdminVerificationBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Panel admin (Stitch: Admin Mobile - Ringkasan Dashboard).
 * Ringkasan, Armada, Pesanan dan Keuangan seluruhnya dihitung dari Realtime Database —
 * tidak ada angka atau nama dummy yang ditanam di aplikasi.
 */
class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private var vehiclesListener: ValueEventListener? = null
    private var vehiclesReference: DatabaseReference? = null
    private var ordersListener: ValueEventListener? = null
    private var ordersReference: DatabaseReference? = null
    private var vehicles: List<Vehicle> = emptyList()
    private var orders: List<AdminOrder> = emptyList()
    private var adminName = "Admin"
    private var selectedCategory = VehicleRepository.CATEGORY_MPV
    private var selectedBadgeType = "blue"

    private val monthFormat = SimpleDateFormat("MMM", Locale.forLanguageTag("id-ID"))
    private val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("id-ID"))

    private data class AdminOrder(val uid: String, val id: String, val order: RentalOrder)

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            val dataUri = withContext(Dispatchers.IO) {
                val bitmap = try {
                    contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                } catch (_: Exception) {
                    null
                }
                bitmap?.let { VehicleImageLoader.encode(it) }
            }
            if (dataUri.isNullOrBlank()) {
                toast(R.string.admin_save_failed)
            } else {
                dialogBinding?.let { form ->
                    form.txtVehicleImageUrl.setText(dataUri)
                    VehicleImageLoader.load(form.imgVehiclePreview, dataUri)
                }
            }
        }
    }

    private var dialogBinding: DialogVehicleBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseService.initialize(applicationContext)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnAddVehicle.setOnClickListener { showVehicleDialog(null) }
        binding.btnAdminAddUnit.setOnClickListener {
            showPage(PAGE_FLEET)
            showVehicleDialog(null)
        }
        binding.btnAdminAllOrders.setOnClickListener { showPage(PAGE_ORDERS) }

        binding.navAdminSummary.setOnClickListener { showPage(PAGE_SUMMARY) }
        binding.navAdminFleet.setOnClickListener { showPage(PAGE_FLEET) }
        binding.navAdminOrders.setOnClickListener { showPage(PAGE_ORDERS) }
        binding.navAdminFinance.setOnClickListener { showPage(PAGE_FINANCE) }

        showPage(PAGE_SUMMARY)
        verifyAdmin()
    }

    override fun onDestroy() {
        vehiclesListener?.let { listener -> vehiclesReference?.removeEventListener(listener) }
        ordersListener?.let { listener -> ordersReference?.removeEventListener(listener) }
        super.onDestroy()
    }

    // ----------------------------------------------------------------------
    // Access + live data
    // ----------------------------------------------------------------------

    private fun verifyAdmin() {
        val user = FirebaseService.auth.currentUser
        if (user == null || !FirebaseService.isDatabaseConfigured) {
            toast(R.string.admin_not_admin)
            finish()
            return
        }
        VehicleRepository.isAdmin(user.uid) { isAdmin ->
            if (!isAdmin || isFinishing || isDestroyed) {
                toast(R.string.admin_not_admin)
                finish()
                return@isAdmin
            }
            loadAdminName(user.uid)
            observeVehicles()
            observeOrders()
        }
    }

    private fun loadAdminName(uid: String) {
        FirebaseService.usersReference().child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val profile = snapshot.getValue(UserProfile::class.java)
                    adminName = profile?.name?.takeIf { it.isNotBlank() }
                        ?: FirebaseService.auth.currentUser?.email?.substringBefore('@')
                        ?: getString(R.string.admin_brand)
                    binding.tvAdminWelcomeName.text = adminName
                }

                override fun onCancelled(error: DatabaseError) = Unit
            })
    }

    private fun observeVehicles() {
        val reference = VehicleRepository.reference()
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                vehicles = snapshot.children.mapNotNull { child ->
                    child.getValue(Vehicle::class.java)?.copy(id = child.key.orEmpty())
                }.sortedBy { it.name }
                renderVehicles()
                renderDashboard()
            }

            override fun onCancelled(error: DatabaseError) {
                toast(R.string.vehicles_load_error)
            }
        }
        vehiclesReference = reference
        vehiclesListener = listener
        reference.addValueEventListener(listener)
    }

    /** Admin may read every uid's orders; rules only allow writing the `status` child. */
    private fun observeOrders() {
        val reference = FirebaseService.rootReference().child("orders")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                orders = snapshot.children.flatMap { userNode ->
                    val uid = userNode.key.orEmpty()
                    userNode.children.mapNotNull { child ->
                        child.getValue(RentalOrder::class.java)?.copy(id = child.key.orEmpty())
                            ?.let { AdminOrder(uid, child.key.orEmpty(), it) }
                    }
                }
                renderAdminOrders()
                renderDashboard()
            }

            override fun onCancelled(error: DatabaseError) {
                toast(R.string.orders_load_error)
            }
        }
        ordersReference = reference
        ordersListener = listener
        reference.addValueEventListener(listener)
    }

    // ----------------------------------------------------------------------
    // Navigation
    // ----------------------------------------------------------------------

    private fun showPage(page: Int) {
        binding.adminPageDashboard.visibility = if (page == PAGE_SUMMARY) View.VISIBLE else View.GONE
        binding.adminPageFleet.visibility = if (page == PAGE_FLEET) View.VISIBLE else View.GONE
        binding.adminPageOrders.visibility = if (page == PAGE_ORDERS) View.VISIBLE else View.GONE
        binding.adminPageFinance.visibility = if (page == PAGE_FINANCE) View.VISIBLE else View.GONE

        val pages = listOf(
            binding.adminPageDashboard,
            binding.adminPageFleet,
            binding.adminPageOrders,
            binding.adminPageFinance
        )
        val items = listOf(
            binding.navAdminSummary,
            binding.navAdminFleet,
            binding.navAdminOrders,
            binding.navAdminFinance
        )
        val icons = listOf(
            binding.navAdminSummaryIcon,
            binding.navAdminFleetIcon,
            binding.navAdminOrdersIcon,
            binding.navAdminFinanceIcon
        )
        val labels = listOf(
            binding.navAdminSummaryLabel,
            binding.navAdminFleetLabel,
            binding.navAdminOrdersLabel,
            binding.navAdminFinanceLabel
        )
        val active = ContextCompat.getColor(this, R.color.home_primary)
        val inactive = ContextCompat.getColor(this, R.color.home_on_surface_variant)

        pages.forEachIndexed { index, view -> view.isSelected = index == page }
        items.forEachIndexed { index, view -> view.isSelected = index == page }
        icons.forEachIndexed { index, icon ->
            icon.setColorFilter(if (index == page) active else inactive)
        }
        labels.forEachIndexed { index, label ->
            label.setTextColor(if (index == page) active else inactive)
            label.setTypeface(null, if (index == page) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        }
    }

    // ----------------------------------------------------------------------
    // Dashboard: all values derived from Firebase
    // ----------------------------------------------------------------------

    private fun isOpen(order: RentalOrder): Boolean {
        val cancelled = getString(R.string.orders_status_cancelled)
        val completed = getString(R.string.orders_status_completed)
        return order.status != cancelled && order.status != completed
    }

    private fun monthBucket(timestamp: Long): Pair<Int, Int> {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        return calendar.get(Calendar.YEAR) to calendar.get(Calendar.MONTH)
    }

    private fun sumRevenue(source: List<AdminOrder>): Long =
        source.filter { isOpen(it.order) || it.order.status == getString(R.string.orders_status_completed) }
            .sumOf { it.order.total }

    private fun shortRupiah(amount: Long): String = if (amount >= 1_000_000L) {
        String.format(
            Locale.forLanguageTag("id-ID"),
            "Rp %.1fjt",
            amount / 1_000_000.0
        )
    } else {
        formatRupiah(amount)
    }

    private fun renderDashboard() {
        val totalVehicles = vehicles.size
        val activeVehicles = vehicles.count { it.active }
        val idleVehicles = totalVehicles - activeVehicles
        val openOrders = orders.filter { isOpen(it.order) }
        val totalRevenue = sumRevenue(orders)

        binding.tvAdminTotalRevenue.text = formatRupiah(totalRevenue)
        binding.tvAdminSystemStatus.text = if (openOrders.isEmpty()) {
            getString(R.string.admin_system_idle)
        } else {
            getString(R.string.admin_system_ok, openOrders.size)
        }
        binding.tvAdminToday.text = getString(
            R.string.admin_pickups,
            orders.count { it.order.startDate == dateFormat.format(Date()) }
        )
        binding.tvAdminIdle.text = getString(R.string.admin_idle_units, idleVehicles)

        binding.tvAdminFleetValue.text = totalVehicles.toString()
        binding.tvAdminFleetDetail.text = getString(
            R.string.admin_fleet_breakdown,
            activeVehicles,
            idleVehicles
        )
        binding.tvAdminActionValue.text = openOrders.size.toString()

        val now = monthBucket(System.currentTimeMillis())
        val calendar = Calendar.getInstance().apply {
            set(now.first, now.second, 1, 0, 0, 0)
        }
        binding.tvAdminRevenueMonthLabel.text = getString(
            R.string.admin_revenue_month,
            monthFormat.format(calendar.time)
        )
        val thisMonth = orders.filter { monthBucket(it.order.createdAt) == now }
        val monthRevenue = sumRevenue(thisMonth)
        binding.tvAdminRevenueValue.text = shortRupiah(monthRevenue)

        val previous = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
        val previousKey = previous.get(Calendar.YEAR) to previous.get(Calendar.MONTH)
        val previousRevenue = sumRevenue(orders.filter { monthBucket(it.order.createdAt) == previousKey })
        binding.tvAdminRevenueTrend.text = if (previousRevenue <= 0L) {
            getString(R.string.admin_revenue_trend, "-")
        } else {
            val change = ((monthRevenue - previousRevenue) * 100) / previousRevenue
            getString(R.string.admin_revenue_trend, if (change >= 0) "+$change%" else "$change%")
        }

        val utilization = if (totalVehicles == 0) 0 else activeVehicles * 100 / totalVehicles
        binding.tvAdminUtilValue.text = getString(R.string.admin_util_value, utilization)
        binding.tvAdminUtilLabel.setText(
            when {
                utilization >= 70 -> R.string.admin_util_high
                utilization >= 40 -> R.string.admin_util_medium
                else -> R.string.admin_util_low
            }
        )
        setWeight(binding.adminUtilFill, utilization.toFloat())

        binding.tvAdminUrgent.text = getString(R.string.admin_urgent, openOrders.size)
        binding.tvAdminUrgent.visibility = if (openOrders.isEmpty()) View.GONE else View.VISIBLE
        binding.btnAdminAllOrders.text = getString(R.string.admin_see_all, orders.size)
        renderVerification(openOrders)

        binding.tvAdminCompositionActive.text = getString(
            R.string.admin_active_of,
            activeVehicles,
            totalVehicles
        )
        renderComposition(totalVehicles)
        renderActivity()
        renderFinance(totalRevenue, monthRevenue)
    }

    private fun setWeight(view: View, weight: Float) {
        val params = view.layoutParams as? LinearLayout.LayoutParams ?: return
        params.weight = weight
        view.layoutParams = params
    }

    private fun renderVerification(openOrders: List<AdminOrder>) {
        binding.adminVerificationContainer.removeAllViews()
        val visible = openOrders.sortedBy { it.order.createdAt }.take(5)
        binding.tvAdminVerificationEmpty.visibility =
            if (visible.isEmpty()) View.VISIBLE else View.GONE

        visible.forEach { entry ->
            val item = ItemAdminVerificationBinding.inflate(layoutInflater, binding.adminVerificationContainer, false)
            val customer = entry.order.customerName.ifBlank {
                entry.order.customerUsername.ifBlank { getString(R.string.home_guest) }
            }
            item.tvVerificationInitial.text = customer.take(1).uppercase(Locale.getDefault())
            item.tvVerificationName.text = customer
            item.tvVerificationCode.text = "#${entry.id.take(8).uppercase(Locale.getDefault())}"
            item.tvVerificationVehicle.text = listOf(
                entry.order.vehicleName.ifBlank { entry.order.brand },
                getString(R.string.orders_date_range, entry.order.startDate, entry.order.endDate)
            ).filter { it.isNotBlank() }.joinToString(" • ")
            item.tvVerificationMode.text = entry.order.mode.ifBlank { getString(R.string.home_mode_rent) }
            item.tvVerificationMode.setBackgroundResource(
                if (entry.order.mode == getString(R.string.home_mode_driver)) {
                    R.drawable.bg_home_badge_blue
                } else {
                    R.drawable.bg_home_badge_green
                }
            )
            item.btnVerificationCheck.setOnClickListener { confirmComplete(entry) }
            binding.adminVerificationContainer.addView(item.root)
        }
    }

    private fun renderComposition(total: Int) {
        val groups = listOf(
            Composition(R.string.admin_category_mpv, VehicleRepository.CATEGORY_MPV,
                binding.tvAdminMpvCount, binding.adminMpvFill, binding.segAdminMpv),
            Composition(R.string.admin_category_city, VehicleRepository.CATEGORY_CITY,
                binding.tvAdminCityCount, binding.adminCityFill, binding.segAdminCity),
            Composition(R.string.admin_category_ev, VehicleRepository.CATEGORY_EV,
                binding.tvAdminEvCount, binding.adminEvFill, binding.segAdminEv),
            Composition(R.string.admin_category_suv, VehicleRepository.CATEGORY_SUV,
                binding.tvAdminSuvCount, binding.adminSuvFill, binding.segAdminSuv),
            Composition(R.string.admin_category_sedan, VehicleRepository.CATEGORY_SEDAN,
                binding.tvAdminSedanCount, binding.adminSedanFill, binding.segAdminSedan)
        )
        groups.forEach { group ->
            val matching = vehicles.filter { it.category.equals(group.key, ignoreCase = true) }
            val activeCount = matching.count { it.active }
            group.label.text = getString(
                R.string.admin_category_ratio,
                activeCount,
                matching.size
            )
            val percentage = if (total == 0) 0f else matching.size * 100f / total
            setWeight(group.fill, percentage)
            setWeight(group.segment, percentage)
        }
    }

    private data class Composition(
        val labelRes: Int,
        val key: String,
        val label: TextView,
        val fill: View,
        val segment: View
    )

    private fun renderActivity() {
        binding.adminActivityContainer.removeAllViews()
        val recent = orders.sortedByDescending { it.order.createdAt }.take(3)
        binding.tvAdminActivityEmpty.visibility = if (recent.isEmpty()) View.VISIBLE else View.GONE

        recent.forEach { entry ->
            val item = ItemAdminActivityBinding.inflate(
                layoutInflater,
                binding.adminActivityContainer,
                false
            )
            val completed = entry.order.status == getString(R.string.orders_status_completed)
            val cancelled = entry.order.status == getString(R.string.orders_status_cancelled)
            item.tvAdminActivityTitle.text = getString(
                when {
                    completed -> R.string.admin_activity_done
                    cancelled -> R.string.orders_status_cancelled
                    else -> R.string.admin_activity_order
                },
                entry.order.vehicleName.ifBlank { entry.order.brand }
            )
            item.tvAdminActivityDetail.text = listOf(
                entry.order.customerName.ifBlank { entry.order.customerUsername },
                entry.order.mode
            ).filter { it.isNotBlank() }.joinToString(" • ")
            item.tvAdminActivityTime.text = relativeTime(entry.order.createdAt)
            item.imgAdminActivity.setImageResource(
                if (completed) R.drawable.ic_check else R.drawable.ic_home_receipt
            )
            binding.adminActivityContainer.addView(item.root)
        }
    }

    private fun relativeTime(timestamp: Long): String {
        if (timestamp <= 0L) return "-"
        val minutes = ((System.currentTimeMillis() - timestamp) / 60_000L).coerceAtLeast(0)
        return when {
            minutes < 1 -> getString(R.string.admin_time_now)
            minutes < 60 -> getString(R.string.admin_time_minutes, minutes)
            minutes < 1440 -> getString(R.string.admin_time_hours, minutes / 60)
            else -> getString(R.string.admin_time_days, minutes / 1440)
        }
    }

    private fun renderFinance(total: Long, month: Long) {
        binding.tvAdminFinanceTotal.text = formatRupiah(total)
        binding.tvAdminFinanceMonth.text = formatRupiah(month)
        binding.tvAdminFinanceOrders.text = orders.count {
            it.order.status == getString(R.string.orders_status_completed)
        }.toString()
        val revenueOrders = orders.filter {
            it.order.status != getString(R.string.orders_status_cancelled)
        }
        binding.tvAdminFinanceAverage.text = formatRupiah(
            if (revenueOrders.isEmpty()) 0L else total / revenueOrders.size
        )
        binding.tvAdminFinanceCancelled.text = orders.count {
            it.order.status == getString(R.string.orders_status_cancelled)
        }.toString()
    }

    // ----------------------------------------------------------------------
    // Pesanan (admin view)
    // ----------------------------------------------------------------------

    private fun renderAdminOrders() {
        binding.tvAdminOrdersSummary.text = getString(R.string.admin_orders_summary, orders.size)
        binding.adminOrdersContainer.removeAllViews()
        binding.tvAdminOrdersEmpty.visibility = if (orders.isEmpty()) View.VISIBLE else View.GONE

        orders.sortedByDescending { it.order.createdAt }.forEach { entry ->
            val item = ItemAdminOrderBinding.inflate(
                layoutInflater,
                binding.adminOrdersContainer,
                false
            )
            item.tvAdminOrderCustomer.text = entry.order.customerName.ifBlank {
                entry.order.customerUsername.ifBlank { getString(R.string.home_guest) }
            }
            item.tvAdminOrderVehicle.text = listOf(
                entry.order.vehicleName.ifBlank { entry.order.brand },
                getString(R.string.orders_date_range, entry.order.startDate, entry.order.endDate)
            ).filter { it.isNotBlank() }.joinToString(" • ")
            item.tvAdminOrderMode.text = getString(
                R.string.orders_duration,
                entry.order.durationDays,
                entry.order.mode.ifBlank { getString(R.string.home_mode_rent) }
            )
            item.tvAdminOrderTotal.text = getString(R.string.orders_total, formatRupiah(entry.order.total))
            item.tvAdminOrderStatus.text = entry.order.status.ifBlank {
                getString(R.string.orders_status_processing)
            }
            item.tvAdminOrderStatus.setBackgroundResource(
                if (isOpen(entry.order)) R.drawable.bg_home_badge_green else R.drawable.bg_home_badge_white
            )
            item.btnAdminOrderDone.visibility = if (isOpen(entry.order)) View.VISIBLE else View.GONE
            item.btnAdminOrderDone.setOnClickListener { confirmComplete(entry) }
            binding.adminOrdersContainer.addView(item.root)
        }
    }

    private fun confirmComplete(entry: AdminOrder) {
        AlertDialog.Builder(this)
            .setTitle(R.string.admin_mark_done)
            .setMessage(
                getString(
                    R.string.orders_date_range,
                    entry.order.startDate,
                    entry.order.endDate
                )
            )
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.admin_mark_done) { _, _ -> completeOrder(entry) }
            .show()
    }

    private fun completeOrder(entry: AdminOrder) {
        FirebaseService.rootReference()
            .child("orders")
            .child(entry.uid)
            .child(entry.id)
            .child("status")
            .setValue(getString(R.string.orders_status_completed))
            .addOnSuccessListener { toast(R.string.admin_order_marked_done) }
            .addOnFailureListener { toast(R.string.admin_order_update_failed) }
    }

    // ----------------------------------------------------------------------
    // Armada CRUD (dipakai halaman Armada)
    // ----------------------------------------------------------------------

    private fun renderVehicles() {
        binding.adminVehicleContainer.removeAllViews()
        binding.tvAdminEmpty.visibility = if (vehicles.isEmpty()) View.VISIBLE else View.GONE

        vehicles.forEach { vehicle ->
            val itemBinding = ItemAdminVehicleBinding.inflate(
                layoutInflater,
                binding.adminVehicleContainer,
                false
            )
            VehicleImageLoader.load(itemBinding.imgAdminVehicle, vehicle.imageUrl)
            itemBinding.tvAdminVehicleName.text = vehicle.name.ifBlank { vehicle.brand }
            itemBinding.tvAdminVehicleSpec.text = listOfNotNull(
                vehicle.brand.takeIf { it.isNotBlank() },
                vehicle.seats.takeIf { it.isNotBlank() },
                vehicle.transmission.takeIf { it.isNotBlank() }
            ).joinToString(" • ")
            itemBinding.tvAdminVehiclePrice.text = formatRupiah(vehicle.pricePerDay) +
                " " + getString(R.string.home_per_day)

            itemBinding.btnAdminEdit.setOnClickListener { showVehicleDialog(vehicle) }
            itemBinding.btnAdminDelete.setOnClickListener { confirmDelete(vehicle) }
            binding.adminVehicleContainer.addView(itemBinding.root)
        }
    }

    private fun confirmDelete(vehicle: Vehicle) {
        AlertDialog.Builder(this)
            .setTitle(R.string.admin_delete)
            .setMessage(R.string.admin_delete_confirm)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.admin_delete) { _, _ ->
                VehicleRepository.delete(vehicle.id) { success ->
                    toast(if (success) R.string.admin_deleted else R.string.admin_delete_failed)
                }
            }
            .show()
    }

    private fun showVehicleDialog(existing: Vehicle?) {
        val form = DialogVehicleBinding.inflate(layoutInflater)
        dialogBinding = form
        selectedCategory = existing?.category?.takeIf {
            VehicleRepository.editableCategories.contains(it)
        } ?: VehicleRepository.CATEGORY_MPV
        selectedBadgeType = existing?.badgeType?.takeIf { it == "green" } ?: "blue"

        val categories = resources.getStringArray(R.array.vehicle_categories)
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        form.spinnerCategory.adapter = categoryAdapter
        form.spinnerCategory.setSelection(
            VehicleRepository.editableCategories.indexOf(selectedCategory).coerceAtLeast(0)
        )
        form.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedCategory = VehicleRepository.editableCategories[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>) = Unit
        }

        val badgeTypes = resources.getStringArray(R.array.admin_badge_types)
        val badgeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, badgeTypes)
        badgeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        form.spinnerBadgeType.adapter = badgeAdapter
        form.spinnerBadgeType.setSelection(if (selectedBadgeType == "green") 1 else 0)
        form.spinnerBadgeType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedBadgeType = if (position == 1) "green" else "blue"
            }

            override fun onNothingSelected(parent: AdapterView<*>) = Unit
        }

        form.txtVehicleBrand.setText(existing?.brand.orEmpty())
        form.txtVehicleName.setText(existing?.name.orEmpty())
        form.txtVehicleSeats.setText(existing?.seats.orEmpty())
        form.txtVehicleTransmission.setText(existing?.transmission.orEmpty())
        form.txtVehicleFeature.setText(existing?.feature.orEmpty())
        form.txtVehiclePrice.setText(existing?.pricePerDay?.takeIf { it > 0 }?.toString().orEmpty())
        form.txtVehicleRating.setText(existing?.rating.orEmpty())
        form.txtVehicleReviews.setText(existing?.reviews.orEmpty())
        form.txtVehicleBadge.setText(existing?.badge.orEmpty())
        form.txtVehicleImageUrl.setText(existing?.imageUrl.orEmpty())
        form.chkVehicleActive.isChecked = existing?.active ?: true
        VehicleImageLoader.load(form.imgVehiclePreview, existing?.imageUrl)

        form.btnPickImage.setOnClickListener { pickImage.launch("image/*") }

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (existing == null) R.string.admin_form_new else R.string.admin_form_edit)
            .setView(form.root)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = form.txtVehicleName.text.toString().trim()
                if (name.isBlank()) {
                    form.txtVehicleName.error = getString(R.string.admin_name_required)
                    return@setOnClickListener
                }
                val price = form.txtVehiclePrice.text.toString().trim().toLongOrNull()
                if (price == null || price <= 0) {
                    form.txtVehiclePrice.error = getString(R.string.admin_price_required)
                    return@setOnClickListener
                }

                val vehicle = Vehicle(
                    id = existing?.id.orEmpty(),
                    brand = form.txtVehicleBrand.text.toString().trim(),
                    name = name,
                    category = selectedCategory,
                    seats = form.txtVehicleSeats.text.toString().trim(),
                    transmission = form.txtVehicleTransmission.text.toString().trim(),
                    feature = form.txtVehicleFeature.text.toString().trim(),
                    pricePerDay = price,
                    rating = form.txtVehicleRating.text.toString().trim(),
                    reviews = form.txtVehicleReviews.text.toString().trim(),
                    imageUrl = form.txtVehicleImageUrl.text.toString().trim(),
                    badge = form.txtVehicleBadge.text.toString().trim(),
                    badgeType = selectedBadgeType,
                    active = form.chkVehicleActive.isChecked,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis()
                )

                VehicleRepository.save(vehicle) { success ->
                    if (success) {
                        toast(R.string.admin_saved)
                        dialog.dismiss()
                    } else {
                        toast(R.string.admin_save_failed)
                    }
                }
            }
        }
        dialog.show()
    }

    private fun toast(resId: Int) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val PAGE_SUMMARY = 0
        private const val PAGE_FLEET = 1
        private const val PAGE_ORDERS = 2
        private const val PAGE_FINANCE = 3
    }
}

package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MotoConnectViewModel(
    application: Application,
    private val repository: MotoConnectRepository
) : AndroidViewModel(application) {

    // --- CURRENT SESSION STATE ---
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // --- ROAD TRIP SIMULATION ---
    private val _isTripActive = MutableStateFlow(false)
    val isTripActive: StateFlow<Boolean> = _isTripActive.asStateFlow()

    private val _bikerLatitude = MutableStateFlow(19.4326)
    val bikerLatitude: StateFlow<Double> = _bikerLatitude.asStateFlow()

    private val _bikerLongitude = MutableStateFlow(-99.1332)
    val bikerLongitude: StateFlow<Double> = _bikerLongitude.asStateFlow()

    // --- PUSH NOTIFICATION SIMULATION ---
    private val _latestPushNotification = MutableStateFlow<SecurityAlertEntity?>(null)
    val latestPushNotification: StateFlow<SecurityAlertEntity?> = _latestPushNotification.asStateFlow()

    // --- ACTIVE SOS EMERGENCY FLOW STATE ---
    // "IDLE", "SEARCHING", "ASSIGNED", "ARRIVED"
    private val _sosState = MutableStateFlow("IDLE")
    val sosState: StateFlow<String> = _sosState.asStateFlow()

    private val _activeSOSBookingId = MutableStateFlow<Int?>(null)
    val activeSOSBookingId: StateFlow<Int?> = _activeSOSBookingId.asStateFlow()

    private val _sosProviderDistance = MutableStateFlow(5.0) // km
    val sosProviderDistance: StateFlow<Double> = _sosProviderDistance.asStateFlow()

    // --- DATA FLOWS FROM REPOSITORY ---
    val allUsers: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allServices: StateFlow<List<ServiceEntity>> = repository.allServices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBookings: StateFlow<List<BookingEntity>> = repository.allBookings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appConfig: StateFlow<AppConfigEntity> = repository.appConfig
        .map { it ?: AppConfigEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppConfigEntity())

    val securityAlerts: StateFlow<List<SecurityAlertEntity>> = repository.allSecurityAlerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Enviar notificaciones push simuladas cuando entra una nueva alerta
        viewModelScope.launch {
            repository.allSecurityAlerts.collect { alerts ->
                if (alerts.isNotEmpty()) {
                    // Notificar sobre la alerta más reciente
                    val newest = alerts.first()
                    // No repetir si ya pasó tiempo o es la misma
                    if (_latestPushNotification.value?.id != newest.id) {
                        _latestPushNotification.value = newest
                    }
                }
            }
        }
    }

    // --- SESSION METHODS ---
    fun loginAsUser(user: UserEntity) {
        _currentUser.value = user
        if (user.role == "BIKER") {
            _bikerLatitude.value = user.latitude ?: 19.4326
            _bikerLongitude.value = user.longitude ?: -99.1332
        }
    }

    fun insertUser(user: UserEntity) {
        viewModelScope.launch {
            repository.insertUser(user)
        }
    }

    fun logout() {
        _currentUser.value = null
        _isTripActive.value = false
        _sosState.value = "IDLE"
        _activeSOSBookingId.value = null
    }

    // --- ROAD TRIP METHOD ---
    private var tripJob: kotlinx.coroutines.Job? = null
    fun toggleRoadTrip() {
        if (_isTripActive.value) {
            _isTripActive.value = false
            tripJob?.cancel()
        } else {
            _isTripActive.value = true
            tripJob = viewModelScope.launch {
                // Simulación de recorrido por carretera (avanzando latitud poco a poco)
                // Vamos del centro de CDMX hacia el sur (Cuernavaca)
                var currentLat = _bikerLatitude.value
                var currentLng = _bikerLongitude.value
                while (_isTripActive.value) {
                    delay(2000)
                    currentLat -= 0.005 // Desplazamiento al sur
                    currentLng += 0.003 // Desplazamiento ligero este
                    _bikerLatitude.value = currentLat
                    _bikerLongitude.value = currentLng

                    // Actualizar ubicación en base de datos
                    currentUser.value?.let { biker ->
                        repository.updateUserLocation(biker.id, currentLat, currentLng)
                    }

                    // Verificar si entramos al área de una alerta de seguridad (distancia < 2km)
                    checkNearbyAlerts(currentLat, currentLng)
                }
            }
        }
    }

    private suspend fun checkNearbyAlerts(bikerLat: Double, bikerLng: Double) {
        val alerts = securityAlerts.value
        for (alert in alerts) {
            val dist = calculateDistance(bikerLat, bikerLng, alert.latitude, alert.longitude)
            if (dist <= 2.0) {
                // Disparar notificación push simulada
                _latestPushNotification.value = alert
                break
            }
        }
    }

    fun clearLatestNotification() {
        _latestPushNotification.value = null
    }

    // --- EMERGENCY SOS FLOW ---
    fun triggerSOS() {
        val biker = _currentUser.value ?: return
        if (biker.role != "BIKER") return

        viewModelScope.launch {
            _sosState.value = "SEARCHING"
            delay(2500) // Simular búsqueda del proveedor más cercano

            // Encontrar proveedor de tipo "Grúa" o "Mecánico" más cercano
            val providers = allUsers.value.filter { it.role == "PROVIDER" }
            val bikerLat = _bikerLatitude.value
            val bikerLng = _bikerLongitude.value

            val nearestProvider = providers.minByOrNull {
                calculateDistance(bikerLat, bikerLng, it.latitude ?: 0.0, it.longitude ?: 0.0)
            } ?: providers.firstOrNull()

            if (nearestProvider != null) {
                val initialDist = calculateDistance(bikerLat, bikerLng, nearestProvider.latitude ?: 0.0, nearestProvider.longitude ?: 0.0)
                _sosProviderDistance.value = if (initialDist > 0) initialDist else 3.5

                // Crear un Booking de emergencia
                val bookingId = repository.createBooking(
                    bikerId = biker.id,
                    providerId = nearestProvider.id,
                    serviceId = null,
                    serviceName = "Auxilio Vial SOS Directo",
                    pricePaid = 800.0, // Tarifa fija de emergencia
                    bikerLat = bikerLat,
                    bikerLng = bikerLng
                )
                _activeSOSBookingId.value = bookingId
                _sosState.value = "ASSIGNED"

                // Simular el trayecto del proveedor llegando al biker
                launch {
                    var remainingDist = _sosProviderDistance.value
                    while (remainingDist > 0.1 && _sosState.value == "ASSIGNED") {
                        delay(2500)
                        remainingDist -= 0.8
                        if (remainingDist < 0.1) remainingDist = 0.0
                        _sosProviderDistance.value = remainingDist
                    }
                    if (_sosState.value == "ASSIGNED") {
                        _sosState.value = "ARRIVED"
                        // Actualizar estatus del booking
                        repository.updateBookingStatus(bookingId, "ACCEPTED")
                    }
                }
            } else {
                _sosState.value = "IDLE"
            }
        }
    }

    fun cancelSOS() {
        val bookingId = _activeSOSBookingId.value
        _sosState.value = "IDLE"
        _activeSOSBookingId.value = null
        if (bookingId != null) {
            viewModelScope.launch {
                repository.updateBookingStatus(bookingId, "CANCELLED")
            }
        }
    }

    fun completeSOSTransaction() {
        val bookingId = _activeSOSBookingId.value ?: return
        viewModelScope.launch {
            repository.updateBookingStatus(bookingId, "COMPLETED")
            _sosState.value = "IDLE"
            _activeSOSBookingId.value = null
        }
    }

    // --- STANDARD SERVICE BOOKING FLOW ---
    fun hireService(service: ServiceEntity, bikerLat: Double, bikerLng: Double) {
        val biker = _currentUser.value ?: return
        viewModelScope.launch {
            // Crear el registro de contratación
            repository.createBooking(
                bikerId = biker.id,
                providerId = service.providerId,
                serviceId = service.id,
                serviceName = service.name,
                pricePaid = service.price,
                bikerLat = bikerLat,
                bikerLng = bikerLng
            )
        }
    }

    fun updateBookingStatus(bookingId: Int, status: String) {
        viewModelScope.launch {
            repository.updateBookingStatus(bookingId, status)
        }
    }

    fun reportDispute(bookingId: Int, message: String) {
        viewModelScope.launch {
            repository.updateBookingStatus(bookingId, "DISPUTED", message)
        }
    }

    fun rateBooking(bookingId: Int, rating: Float, comment: String, isByBiker: Boolean) {
        viewModelScope.launch {
            repository.rateBooking(bookingId, rating, comment, isByBiker)
        }
    }

    // --- ADMIN MASTER CONTROLS ---
    fun updatePlatformConfig(percentage: Double, adText: String) {
        viewModelScope.launch {
            val current = appConfig.value
            repository.updateAppConfig(
                current.copy(
                    commissionPercentage = percentage,
                    adText = adText
                )
            )
        }
    }

    fun resolveDispute(bookingId: Int, resolveInFavorOfBiker: Boolean) {
        viewModelScope.launch {
            val status = if (resolveInFavorOfBiker) "RESOLVED_FAVOR_BIKER" else "RESOLVED_FAVOR_PROVIDER"
            repository.updateBookingStatus(bookingId, status)
        }
    }

    fun deleteUser(userId: Int) {
        viewModelScope.launch {
            // Si el usuario actual es eliminado, cerramos sesión
            if (_currentUser.value?.id == userId) {
                logout()
            }
            repository.deleteUser(userId)
        }
    }

    fun updateUserDetails(user: UserEntity) {
        viewModelScope.launch {
            repository.updateUser(user)
            if (_currentUser.value?.id == user.id) {
                _currentUser.value = user
            }
        }
    }

    // --- PROVIDER CONTROLS ---
    fun addNewService(name: String, desc: String, price: Double, category: String) {
        val provider = _currentUser.value ?: return
        if (provider.role != "PROVIDER") return
        viewModelScope.launch {
            repository.addService(
                ServiceEntity(
                    providerId = provider.id,
                    name = name,
                    description = desc,
                    price = price,
                    category = category
                )
            )
        }
    }

    fun deleteService(serviceId: Int) {
        viewModelScope.launch {
            repository.deleteService(serviceId)
        }
    }

    // --- EMERGENCY ALERTS CREATION ---
    fun reportSecurityAlert(title: String, message: String, type: String) {
        viewModelScope.launch {
            repository.insertSecurityAlert(
                SecurityAlertEntity(
                    title = title,
                    message = message,
                    latitude = _bikerLatitude.value,
                    longitude = _bikerLongitude.value,
                    type = type
                )
            )
        }
    }

    fun deleteSecurityAlert(id: Int) {
        viewModelScope.launch {
            repository.deleteSecurityAlert(id)
        }
    }

    // --- MATH HELPER ---
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371 // Radio de la tierra en km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}

class MotoConnectViewModelFactory(
    private val application: Application,
    private val repository: MotoConnectRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MotoConnectViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MotoConnectViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class MotoConnectRepository(private val database: AppDatabase) {

    private val userDao = database.userDao()
    private val serviceDao = database.serviceDao()
    private val bookingDao = database.bookingDao()
    private val appConfigDao = database.appConfigDao()
    private val securityAlertDao = database.securityAlertDao()

    // --- USER METHODS ---
    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsersFlow()
    fun getUsersByRole(role: String): Flow<List<UserEntity>> = userDao.getUsersByRoleFlow(role)

    suspend fun getUserById(id: Int): UserEntity? = userDao.getUserById(id)

    suspend fun insertUser(user: UserEntity): Int {
        return userDao.insertUser(user).toInt()
    }

    suspend fun updateUser(user: UserEntity) = userDao.updateUser(user)

    suspend fun updateUserLocation(userId: Int, latitude: Double, longitude: Double) {
        userDao.updateUserLocation(userId, latitude, longitude)
    }

    suspend fun deleteUser(userId: Int) = userDao.deleteUserById(userId)


    // --- SERVICE METHODS ---
    val allServices: Flow<List<ServiceEntity>> = serviceDao.getAllServicesFlow()

    fun getServicesByProvider(providerId: Int): Flow<List<ServiceEntity>> =
        serviceDao.getServicesByProviderFlow(providerId)

    fun getServicesByCategory(category: String): Flow<List<ServiceEntity>> =
        serviceDao.getServicesByCategoryFlow(category)

    suspend fun addService(service: ServiceEntity) = serviceDao.insertService(service)

    suspend fun deleteService(id: Int) = serviceDao.deleteServiceById(id)


    // --- BOOKING METHODS ---
    val allBookings: Flow<List<BookingEntity>> = bookingDao.getAllBookingsFlow()

    fun getBookingsByBiker(bikerId: Int): Flow<List<BookingEntity>> =
        bookingDao.getBookingsByBikerFlow(bikerId)

    fun getBookingsByProvider(providerId: Int): Flow<List<BookingEntity>> =
        bookingDao.getBookingsByProviderFlow(providerId)

    suspend fun getBookingById(id: Int): BookingEntity? = bookingDao.getBookingById(id)

    suspend fun createBooking(
        bikerId: Int,
        providerId: Int,
        serviceId: Int?,
        serviceName: String,
        pricePaid: Double,
        bikerLat: Double,
        bikerLng: Double
    ): Int {
        // Obtenemos el porcentaje de comisión actual de AppConfig
        val config = appConfigDao.getConfigFlow().firstOrNull() ?: AppConfigEntity()
        val commissionPaid = (pricePaid * (config.commissionPercentage / 100.0))

        val booking = BookingEntity(
            bikerId = bikerId,
            providerId = providerId,
            serviceId = serviceId,
            serviceName = serviceName,
            status = "PENDING",
            bikerLatitude = bikerLat,
            bikerLongitude = bikerLng,
            pricePaid = pricePaid,
            commissionPaid = commissionPaid
        )
        return bookingDao.insertBooking(booking).toInt()
    }

    suspend fun updateBookingStatus(bookingId: Int, status: String, disputeMessage: String? = null) {
        val booking = bookingDao.getBookingById(bookingId) ?: return
        val updated = booking.copy(
            status = status,
            disputeMessage = disputeMessage ?: booking.disputeMessage
        )
        bookingDao.updateBooking(updated)
    }

    suspend fun rateBooking(
        bookingId: Int,
        rating: Float,
        comment: String,
        isByBiker: Boolean // true if Biker rates Provider, false if Provider rates Biker
    ) {
        val booking = bookingDao.getBookingById(bookingId) ?: return
        val updatedBooking = if (isByBiker) {
            booking.copy(providerRating = rating, providerComment = comment)
        } else {
            booking.copy(bikerRating = rating, bikerComment = comment)
        }
        bookingDao.updateBooking(updatedBooking)

        // Actualizamos la calificación promedio del usuario calificado
        val targetUserId = if (isByBiker) booking.providerId else booking.bikerId
        val targetUser = userDao.getUserById(targetUserId)
        if (targetUser != null) {
            val newCount = targetUser.ratingCount + 1
            val newRating = ((targetUser.rating * targetUser.ratingCount) + rating) / newCount
            val updatedUser = targetUser.copy(rating = newRating, ratingCount = newCount)
            userDao.updateUser(updatedUser)
        }
    }


    // --- APP CONFIG METHODS ---
    val appConfig: Flow<AppConfigEntity?> = appConfigDao.getConfigFlow()

    suspend fun updateAppConfig(config: AppConfigEntity) {
        appConfigDao.insertOrUpdateConfig(config)
    }


    // --- SECURITY ALERT METHODS ---
    val allSecurityAlerts: Flow<List<SecurityAlertEntity>> = securityAlertDao.getAllAlertsFlow()

    suspend fun insertSecurityAlert(alert: SecurityAlertEntity) {
        securityAlertDao.insertAlert(alert)
    }

    suspend fun deleteSecurityAlert(id: Int) {
        securityAlertDao.deleteAlertById(id)
    }
}

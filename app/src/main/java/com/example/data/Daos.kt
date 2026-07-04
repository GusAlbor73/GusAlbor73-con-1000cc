package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: Int): UserEntity?

    @Query("SELECT * FROM users WHERE role = :role")
    fun getUsersByRoleFlow(role: String): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET latitude = :latitude, longitude = :longitude WHERE id = :userId")
    suspend fun updateUserLocation(userId: Int, latitude: Double, longitude: Double)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: Int)
}

@Dao
interface ServiceDao {
    @Query("SELECT * FROM services")
    fun getAllServicesFlow(): Flow<List<ServiceEntity>>

    @Query("SELECT * FROM services WHERE providerId = :providerId")
    fun getServicesByProviderFlow(providerId: Int): Flow<List<ServiceEntity>>

    @Query("SELECT * FROM services WHERE category = :category")
    fun getServicesByCategoryFlow(category: String): Flow<List<ServiceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertService(service: ServiceEntity)

    @Query("DELETE FROM services WHERE id = :id")
    suspend fun deleteServiceById(id: Int)
}

@Dao
interface BookingDao {
    @Query("SELECT * FROM bookings ORDER BY timestamp DESC")
    fun getAllBookingsFlow(): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE bikerId = :bikerId ORDER BY timestamp DESC")
    fun getBookingsByBikerFlow(bikerId: Int): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE providerId = :providerId ORDER BY timestamp DESC")
    fun getBookingsByProviderFlow(providerId: Int): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE id = :id")
    suspend fun getBookingById(id: Int): BookingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: BookingEntity): Long

    @Update
    suspend fun updateBooking(booking: BookingEntity)
}

@Dao
interface AppConfigDao {
    @Query("SELECT * FROM app_config WHERE id = 1")
    fun getConfigFlow(): Flow<AppConfigEntity?>

    @Query("SELECT * FROM app_config WHERE id = 1")
    suspend fun getConfig(): AppConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateConfig(config: AppConfigEntity)
}

@Dao
interface SecurityAlertDao {
    @Query("SELECT * FROM security_alerts ORDER BY timestamp DESC")
    fun getAllAlertsFlow(): Flow<List<SecurityAlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: SecurityAlertEntity)

    @Query("DELETE FROM security_alerts WHERE id = :id")
    suspend fun deleteAlertById(id: Int)
}

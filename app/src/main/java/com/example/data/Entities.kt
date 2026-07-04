package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val email: String,
    val phone: String,
    val role: String, // "BIKER", "PROVIDER", "ADMIN"
    val motorcycleModel: String? = null, // Para motociclistas
    val providerType: String? = null, // Para proveedores (e.g., "Mecánico", "Grúa", "Auxilio")
    val latitude: Double? = null, // Ubicación en tiempo real
    val longitude: Double? = null,
    val rating: Float = 5.0f,
    val ratingCount: Int = 1,
    val avatarUrl: String? = null
)

@Entity(tableName = "services")
data class ServiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val providerId: Int, // Referencia al ID del proveedor
    val name: String,
    val description: String,
    val price: Double,
    val category: String, // "Mecánica", "Grúas", "Repuestos", "Auxilio Vial"
    val imageUrl: String? = null
)

@Entity(tableName = "bookings")
data class BookingEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bikerId: Int,
    val providerId: Int,
    val serviceId: Int?, // Nulo si es auxilio de emergencia directo sin servicio específico
    val serviceName: String, // Nombre guardado para histórico
    val status: String, // "PENDING", "ACCEPTED", "COMPLETED", "CANCELLED", "DISPUTED"
    val bikerLatitude: Double,
    val bikerLongitude: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val pricePaid: Double,
    val commissionPaid: Double,
    // Calificaciones mutuas
    val providerRating: Float? = null, // Calificación dada por el biker al proveedor
    val providerComment: String? = null,
    val bikerRating: Float? = null, // Calificación dada por el proveedor al biker
    val bikerComment: String? = null,
    val disputeMessage: String? = null // Mensaje si está en controversia
)

@Entity(tableName = "app_config")
data class AppConfigEntity(
    @PrimaryKey val id: Int = 1, // Registro único
    val commissionPercentage: Double = 10.0, // Comisión cobrada por la app (%)
    val adText: String = "¡Patrocinado! Neumáticos Pirelli Diablo Rosso IV con 15% de descuento en MotoStore.",
    val adImageUrl: String? = null,
    val adTargetUrl: String? = null
)

@Entity(tableName = "security_alerts")
data class SecurityAlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val message: String,
    val latitude: Double,
    val longitude: Double,
    val type: String, // "ROAD_CLOSED", "ACCIDENT", "WEATHER_HAZARD", "POLICE"
    val timestamp: Long = System.currentTimeMillis()
)

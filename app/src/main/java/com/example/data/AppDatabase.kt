package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        ServiceEntity::class,
        BookingEntity::class,
        AppConfigEntity::class,
        SecurityAlertEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun serviceDao(): ServiceDao
    abstract fun bookingDao(): BookingDao
    abstract fun appConfigDao(): AppConfigDao
    abstract fun securityAlertDao(): SecurityAlertDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "motoconnect_database"
                )
                .addCallback(DatabaseCallback(context))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val context: Context
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Seed the database on creation
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    val userDao = database.userDao()
                    val serviceDao = database.serviceDao()
                    val appConfigDao = database.appConfigDao()
                    val securityAlertDao = database.securityAlertDao()

                    // 1. Initial configuration
                    appConfigDao.insertOrUpdateConfig(
                        AppConfigEntity(
                            id = 1,
                            commissionPercentage = 10.0,
                            adText = "¡Promo del Mes! 15% de descuento en Llantas Michelin Road 6 en Talleres Afiliados.",
                            adImageUrl = null,
                            adTargetUrl = null
                        )
                    )

                    // 2. Insert Users (Admin, Bikers, Providers)
                    userDao.insertUser(
                        UserEntity(
                            id = 1,
                            name = "Admin General",
                            email = "admin@motoconnect.com",
                            phone = "555-0100",
                            role = "ADMIN",
                            avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80"
                        )
                    )

                    // Bikers
                    val bikerId1 = userDao.insertUser(
                        UserEntity(
                            id = 2,
                            name = "Carlos Gómez (Biker)",
                            email = "carlos@gmail.com",
                            phone = "555-0201",
                            role = "BIKER",
                            motorcycleModel = "Yamaha MT-07",
                            latitude = 19.4326,
                            longitude = -99.1332,
                            rating = 4.8f,
                            ratingCount = 5,
                            avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=150&q=80"
                        )
                    ).toInt()

                    val bikerId2 = userDao.insertUser(
                        UserEntity(
                            id = 3,
                            name = "Sofía Martínez (Biker)",
                            email = "sofia@gmail.com",
                            phone = "555-0202",
                            role = "BIKER",
                            motorcycleModel = "BMW R 1250 GS",
                            latitude = 19.4000,
                            longitude = -99.1500,
                            rating = 4.9f,
                            ratingCount = 8,
                            avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=150&q=80"
                        )
                    ).toInt()

                    // Providers
                    val providerId1 = userDao.insertUser(
                        UserEntity(
                            id = 4,
                            name = "MotoSpeed Mecánica S.A.",
                            email = "motospeed@gmail.com",
                            phone = "555-0301",
                            role = "PROVIDER",
                            providerType = "Mecánico",
                            latitude = 19.4284,
                            longitude = -99.1276,
                            rating = 4.7f,
                            ratingCount = 12,
                            avatarUrl = "https://images.unsplash.com/photo-1568605117036-5fe5e7bab0b7?auto=format&fit=crop&w=150&q=80"
                        )
                    ).toInt()

                    val providerId2 = userDao.insertUser(
                        UserEntity(
                            id = 5,
                            name = "Grúas MotoRápido 24/7",
                            email = "motorapido@gmail.com",
                            phone = "555-0302",
                            role = "PROVIDER",
                            providerType = "Grúa",
                            latitude = 19.4450,
                            longitude = -99.1150,
                            rating = 4.5f,
                            ratingCount = 15,
                            avatarUrl = "https://images.unsplash.com/photo-1551524559-8af4e6624178?auto=format&fit=crop&w=150&q=80"
                        )
                    ).toInt()

                    val providerId3 = userDao.insertUser(
                        UserEntity(
                            id = 6,
                            name = "Refacciones y Llantas MotoExpress",
                            email = "motoexpress@gmail.com",
                            phone = "555-0303",
                            role = "PROVIDER",
                            providerType = "Repuestos",
                            latitude = 19.4120,
                            longitude = -99.1700,
                            rating = 4.9f,
                            ratingCount = 20,
                            avatarUrl = "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?auto=format&fit=crop&w=150&q=80"
                        )
                    ).toInt()

                    // 3. Add Services
                    // MotoSpeed Services
                    serviceDao.insertService(
                        ServiceEntity(
                            providerId = providerId1,
                            name = "Ajuste y Lubricación de Cadena",
                            description = "Ajuste de tensión preciso según manual y lubricación completa con grasa de alta calidad.",
                            price = 150.0,
                            category = "Mecánica"
                        )
                    )
                    serviceDao.insertService(
                        ServiceEntity(
                            providerId = providerId1,
                            name = "Reparación de Neumático Ponchado",
                            description = "Reparación rápida con parche de tarugo o vulcanizado para llantas tubeless en sitio.",
                            price = 250.0,
                            category = "Mecánica"
                        )
                    )
                    serviceDao.insertService(
                        ServiceEntity(
                            providerId = providerId1,
                            name = "Revisión Eléctrica Completa",
                            description = "Diagnóstico de batería, alternador, luces, fusibles y cableado con escáner especializado.",
                            price = 450.0,
                            category = "Mecánica"
                        )
                    )

                    // MotoRápido Services
                    serviceDao.insertService(
                        ServiceEntity(
                            providerId = providerId2,
                            name = "Traslado Local en Plataforma Especializada",
                            description = "Transporte seguro de motocicletas de cualquier cilindrada en grúa con rampa y anclajes especiales.",
                            price = 750.0,
                            category = "Grúas"
                        )
                    )
                    serviceDao.insertService(
                        ServiceEntity(
                            providerId = providerId2,
                            name = "Rescate en Carretera (hasta 50km)",
                            description = "Asistencia y traslado desde autopistas o carreteras federales cercanas a la zona metropolitana.",
                            price = 1350.0,
                            category = "Grúas"
                        )
                    )

                    // MotoExpress Services
                    serviceDao.insertService(
                        ServiceEntity(
                            providerId = providerId3,
                            name = "Batería LTH Moto 12V",
                            description = "Batería nueva sellada libre de mantenimiento instalada a domicilio.",
                            price = 1100.0,
                            category = "Repuestos"
                        )
                    )
                    serviceDao.insertService(
                        ServiceEntity(
                            providerId = providerId3,
                            name = "Kit de Cables de Acelerador/Clutch Universal",
                            description = "Cables de emergencia, prisioneros y herramienta básica para cambio en carretera.",
                            price = 180.0,
                            category = "Repuestos"
                        )
                    )

                    // 4. Add initial Security/Safety Alerts
                    securityAlertDao.insertAlert(
                        SecurityAlertEntity(
                            title = "Peligro: Aceite derramado en la vía",
                            message = "Se reporta gran mancha de aceite en la lateral de Periférico Sur, altura San Ángel. Extreme precauciones al tomar la curva.",
                            latitude = 19.3450,
                            longitude = -99.1920,
                            type = "ROAD_CLOSED"
                        )
                    )
                    securityAlertDao.insertAlert(
                        SecurityAlertEntity(
                            title = "Neblina Densa en Carretera México-Cuernavaca",
                            message = "Visibilidad reducida a menos de 15 metros entre el km 35 y el km 50. Encienda intermitentes y reduzca velocidad.",
                            latitude = 19.1800,
                            longitude = -99.1200,
                            type = "WEATHER_HAZARD"
                        )
                    )
                    securityAlertDao.insertAlert(
                        SecurityAlertEntity(
                            title = "Accidente vial en Av. Constituyentes",
                            message = "Choque múltiple obstruye dos carriles centrales con dirección a Santa Fe. Tránsito pesado, busque vías alternas.",
                            latitude = 19.4100,
                            longitude = -99.2100,
                            type = "ACCIDENT"
                        )
                    )
                }
            }
        }
    }
}

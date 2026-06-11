package com.example.database

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.example.dao.CustomerDao
import com.example.dao.CustomerDao_Impl
import com.example.dao.DeliveryDao
import com.example.dao.DeliveryDao_Impl
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AppDatabase_Impl : AppDatabase() {
  private val _customerDao: Lazy<CustomerDao> = lazy {
    CustomerDao_Impl(this)
  }

  private val _deliveryDao: Lazy<DeliveryDao> = lazy {
    DeliveryDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(1,
        "6c181ea60f916a4752422758bde44763", "4e4bb6d40d3cc2bbbc736bdb027546ee") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `customers` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT, `mobile` TEXT, `address` TEXT, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `createdDate` TEXT)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `deliveries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `customerId` INTEGER NOT NULL, `deliveryDate` TEXT, `deliveredTime` TEXT, `status` TEXT, FOREIGN KEY(`customerId`) REFERENCES `customers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_deliveries_customerId` ON `deliveries` (`customerId`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '6c181ea60f916a4752422758bde44763')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `customers`")
        connection.execSQL("DROP TABLE IF EXISTS `deliveries`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        connection.execSQL("PRAGMA foreign_keys = ON")
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection):
          RoomOpenDelegate.ValidationResult {
        val _columnsCustomers: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsCustomers.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCustomers.put("name", TableInfo.Column("name", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCustomers.put("mobile", TableInfo.Column("mobile", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCustomers.put("address", TableInfo.Column("address", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCustomers.put("latitude", TableInfo.Column("latitude", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCustomers.put("longitude", TableInfo.Column("longitude", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCustomers.put("createdDate", TableInfo.Column("createdDate", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysCustomers: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesCustomers: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoCustomers: TableInfo = TableInfo("customers", _columnsCustomers,
            _foreignKeysCustomers, _indicesCustomers)
        val _existingCustomers: TableInfo = read(connection, "customers")
        if (!_infoCustomers.equals(_existingCustomers)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |customers(com.example.models.Customer).
              | Expected:
              |""".trimMargin() + _infoCustomers + """
              |
              | Found:
              |""".trimMargin() + _existingCustomers)
        }
        val _columnsDeliveries: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsDeliveries.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDeliveries.put("customerId", TableInfo.Column("customerId", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDeliveries.put("deliveryDate", TableInfo.Column("deliveryDate", "TEXT", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDeliveries.put("deliveredTime", TableInfo.Column("deliveredTime", "TEXT", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDeliveries.put("status", TableInfo.Column("status", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysDeliveries: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysDeliveries.add(TableInfo.ForeignKey("customers", "CASCADE", "NO ACTION",
            listOf("customerId"), listOf("id")))
        val _indicesDeliveries: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesDeliveries.add(TableInfo.Index("index_deliveries_customerId", false,
            listOf("customerId"), listOf("ASC")))
        val _infoDeliveries: TableInfo = TableInfo("deliveries", _columnsDeliveries,
            _foreignKeysDeliveries, _indicesDeliveries)
        val _existingDeliveries: TableInfo = read(connection, "deliveries")
        if (!_infoDeliveries.equals(_existingDeliveries)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |deliveries(com.example.models.Delivery).
              | Expected:
              |""".trimMargin() + _infoDeliveries + """
              |
              | Found:
              |""".trimMargin() + _existingDeliveries)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "customers", "deliveries")
  }

  public override fun clearAllTables() {
    super.performClear(true, "customers", "deliveries")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(CustomerDao::class, CustomerDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(DeliveryDao::class, DeliveryDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override
      fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun customerDao(): CustomerDao? = _customerDao.value

  public override fun deliveryDao(): DeliveryDao? = _deliveryDao.value
}

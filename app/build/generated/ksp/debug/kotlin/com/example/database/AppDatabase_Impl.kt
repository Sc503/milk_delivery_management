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
import com.example.dao.AttendanceDao
import com.example.dao.AttendanceDao_Impl
import com.example.dao.CustomerDao
import com.example.dao.CustomerDao_Impl
import com.example.dao.DeliveryDao
import com.example.dao.DeliveryDao_Impl
import com.example.dao.PaymentDao
import com.example.dao.PaymentDao_Impl
import com.example.dao.StaffDao
import com.example.dao.StaffDao_Impl
import com.example.dao.UserDao
import com.example.dao.UserDao_Impl
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

  private val _userDao: Lazy<UserDao> = lazy {
    UserDao_Impl(this)
  }

  private val _paymentDao: Lazy<PaymentDao> = lazy {
    PaymentDao_Impl(this)
  }

  private val _staffDao: Lazy<StaffDao> = lazy {
    StaffDao_Impl(this)
  }

  private val _attendanceDao: Lazy<AttendanceDao> = lazy {
    AttendanceDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(10,
        "86a41a81c69b6b3f7fe339a6a698a245", "6ed3cac416366c89dbd171f7a8e8c469") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `customers` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT, `mobile` TEXT, `address` TEXT, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `createdDate` TEXT, `milkQuantity` REAL NOT NULL, `milkRate` REAL NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `deliveries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `customerId` INTEGER NOT NULL, `deliveryDate` TEXT, `deliveredTime` TEXT, `status` TEXT, `staffId` INTEGER NOT NULL, FOREIGN KEY(`customerId`) REFERENCES `customers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_deliveries_customerId_deliveryDate` ON `deliveries` (`customerId`, `deliveryDate`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `payments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `customerId` INTEGER NOT NULL, `month` TEXT, `totalAmount` REAL NOT NULL, `status` TEXT)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `users` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `userType` TEXT, `mobile` TEXT, `password` TEXT)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `staff` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `account_id` INTEGER NOT NULL, `name` TEXT, `usertype` TEXT, `mobile` TEXT, `mobile2` TEXT, `address` TEXT, `documentPath` TEXT, `documentType` TEXT, `isactive` INTEGER NOT NULL, `password` TEXT)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `attendance` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `staffId` INTEGER NOT NULL, `date` TEXT, `present` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '86a41a81c69b6b3f7fe339a6a698a245')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `customers`")
        connection.execSQL("DROP TABLE IF EXISTS `deliveries`")
        connection.execSQL("DROP TABLE IF EXISTS `payments`")
        connection.execSQL("DROP TABLE IF EXISTS `users`")
        connection.execSQL("DROP TABLE IF EXISTS `staff`")
        connection.execSQL("DROP TABLE IF EXISTS `attendance`")
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
        _columnsCustomers.put("milkQuantity", TableInfo.Column("milkQuantity", "REAL", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCustomers.put("milkRate", TableInfo.Column("milkRate", "REAL", true, 0, null,
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
        _columnsDeliveries.put("staffId", TableInfo.Column("staffId", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysDeliveries: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysDeliveries.add(TableInfo.ForeignKey("customers", "CASCADE", "NO ACTION",
            listOf("customerId"), listOf("id")))
        val _indicesDeliveries: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesDeliveries.add(TableInfo.Index("index_deliveries_customerId_deliveryDate", true,
            listOf("customerId", "deliveryDate"), listOf("ASC", "ASC")))
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
        val _columnsPayments: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsPayments.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPayments.put("customerId", TableInfo.Column("customerId", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPayments.put("month", TableInfo.Column("month", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPayments.put("totalAmount", TableInfo.Column("totalAmount", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPayments.put("status", TableInfo.Column("status", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysPayments: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesPayments: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoPayments: TableInfo = TableInfo("payments", _columnsPayments, _foreignKeysPayments,
            _indicesPayments)
        val _existingPayments: TableInfo = read(connection, "payments")
        if (!_infoPayments.equals(_existingPayments)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |payments(com.example.models.Payment).
              | Expected:
              |""".trimMargin() + _infoPayments + """
              |
              | Found:
              |""".trimMargin() + _existingPayments)
        }
        val _columnsUsers: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsUsers.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsUsers.put("userType", TableInfo.Column("userType", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsUsers.put("mobile", TableInfo.Column("mobile", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsUsers.put("password", TableInfo.Column("password", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysUsers: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesUsers: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoUsers: TableInfo = TableInfo("users", _columnsUsers, _foreignKeysUsers,
            _indicesUsers)
        val _existingUsers: TableInfo = read(connection, "users")
        if (!_infoUsers.equals(_existingUsers)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |users(com.example.models.User).
              | Expected:
              |""".trimMargin() + _infoUsers + """
              |
              | Found:
              |""".trimMargin() + _existingUsers)
        }
        val _columnsStaff: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsStaff.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsStaff.put("account_id", TableInfo.Column("account_id", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsStaff.put("name", TableInfo.Column("name", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsStaff.put("usertype", TableInfo.Column("usertype", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsStaff.put("mobile", TableInfo.Column("mobile", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsStaff.put("mobile2", TableInfo.Column("mobile2", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsStaff.put("address", TableInfo.Column("address", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsStaff.put("documentPath", TableInfo.Column("documentPath", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsStaff.put("documentType", TableInfo.Column("documentType", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsStaff.put("isactive", TableInfo.Column("isactive", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsStaff.put("password", TableInfo.Column("password", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysStaff: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesStaff: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoStaff: TableInfo = TableInfo("staff", _columnsStaff, _foreignKeysStaff,
            _indicesStaff)
        val _existingStaff: TableInfo = read(connection, "staff")
        if (!_infoStaff.equals(_existingStaff)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |staff(com.example.models.Staff).
              | Expected:
              |""".trimMargin() + _infoStaff + """
              |
              | Found:
              |""".trimMargin() + _existingStaff)
        }
        val _columnsAttendance: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsAttendance.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAttendance.put("staffId", TableInfo.Column("staffId", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAttendance.put("date", TableInfo.Column("date", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAttendance.put("present", TableInfo.Column("present", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysAttendance: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesAttendance: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoAttendance: TableInfo = TableInfo("attendance", _columnsAttendance,
            _foreignKeysAttendance, _indicesAttendance)
        val _existingAttendance: TableInfo = read(connection, "attendance")
        if (!_infoAttendance.equals(_existingAttendance)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |attendance(com.example.models.Attendance).
              | Expected:
              |""".trimMargin() + _infoAttendance + """
              |
              | Found:
              |""".trimMargin() + _existingAttendance)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "customers", "deliveries",
        "payments", "users", "staff", "attendance")
  }

  public override fun clearAllTables() {
    super.performClear(true, "customers", "deliveries", "payments", "users", "staff", "attendance")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(CustomerDao::class, CustomerDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(DeliveryDao::class, DeliveryDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(UserDao::class, UserDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(PaymentDao::class, PaymentDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(StaffDao::class, StaffDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(AttendanceDao::class, AttendanceDao_Impl.getRequiredConverters())
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

  public override fun userDao(): UserDao? = _userDao.value

  public override fun paymentDao(): PaymentDao? = _paymentDao.value

  public override fun staffDao(): StaffDao? = _staffDao.value

  public override fun attendanceDao(): AttendanceDao? = _attendanceDao.value
}

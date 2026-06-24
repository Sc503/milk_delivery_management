package com.example.dao

import androidx.lifecycle.LiveData
import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performBlocking
import androidx.sqlite.SQLiteStatement
import com.example.models.Customer
import javax.`annotation`.processing.Generated
import kotlin.Double
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class CustomerDao_Impl(
  __db: RoomDatabase,
) : CustomerDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfCustomer: EntityInsertAdapter<Customer>

  private val __insertAdapterOfCustomer_1: EntityInsertAdapter<Customer>

  private val __deleteAdapterOfCustomer: EntityDeleteOrUpdateAdapter<Customer>

  private val __updateAdapterOfCustomer: EntityDeleteOrUpdateAdapter<Customer>
  init {
    this.__db = __db
    this.__insertAdapterOfCustomer = object : EntityInsertAdapter<Customer>() {
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `customers` (`id`,`name`,`mobile`,`address`,`latitude`,`longitude`,`createdDate`,`milkQuantity`,`milkRate`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: Customer) {
        statement.bindLong(1, entity.getId())
        val _tmpName: String? = entity.getName()
        if (_tmpName == null) {
          statement.bindNull(2)
        } else {
          statement.bindText(2, _tmpName)
        }
        val _tmpMobile: String? = entity.getMobile()
        if (_tmpMobile == null) {
          statement.bindNull(3)
        } else {
          statement.bindText(3, _tmpMobile)
        }
        val _tmpAddress: String? = entity.getAddress()
        if (_tmpAddress == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpAddress)
        }
        statement.bindDouble(5, entity.getLatitude())
        statement.bindDouble(6, entity.getLongitude())
        val _tmpCreatedDate: String? = entity.getCreatedDate()
        if (_tmpCreatedDate == null) {
          statement.bindNull(7)
        } else {
          statement.bindText(7, _tmpCreatedDate)
        }
        statement.bindDouble(8, entity.getMilkQuantity())
        statement.bindDouble(9, entity.getMilkRate())
      }
    }
    this.__insertAdapterOfCustomer_1 = object : EntityInsertAdapter<Customer>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `customers` (`id`,`name`,`mobile`,`address`,`latitude`,`longitude`,`createdDate`,`milkQuantity`,`milkRate`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: Customer) {
        statement.bindLong(1, entity.getId())
        val _tmpName: String? = entity.getName()
        if (_tmpName == null) {
          statement.bindNull(2)
        } else {
          statement.bindText(2, _tmpName)
        }
        val _tmpMobile: String? = entity.getMobile()
        if (_tmpMobile == null) {
          statement.bindNull(3)
        } else {
          statement.bindText(3, _tmpMobile)
        }
        val _tmpAddress: String? = entity.getAddress()
        if (_tmpAddress == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpAddress)
        }
        statement.bindDouble(5, entity.getLatitude())
        statement.bindDouble(6, entity.getLongitude())
        val _tmpCreatedDate: String? = entity.getCreatedDate()
        if (_tmpCreatedDate == null) {
          statement.bindNull(7)
        } else {
          statement.bindText(7, _tmpCreatedDate)
        }
        statement.bindDouble(8, entity.getMilkQuantity())
        statement.bindDouble(9, entity.getMilkRate())
      }
    }
    this.__deleteAdapterOfCustomer = object : EntityDeleteOrUpdateAdapter<Customer>() {
      protected override fun createQuery(): String = "DELETE FROM `customers` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: Customer) {
        statement.bindLong(1, entity.getId())
      }
    }
    this.__updateAdapterOfCustomer = object : EntityDeleteOrUpdateAdapter<Customer>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `customers` SET `id` = ?,`name` = ?,`mobile` = ?,`address` = ?,`latitude` = ?,`longitude` = ?,`createdDate` = ?,`milkQuantity` = ?,`milkRate` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: Customer) {
        statement.bindLong(1, entity.getId())
        val _tmpName: String? = entity.getName()
        if (_tmpName == null) {
          statement.bindNull(2)
        } else {
          statement.bindText(2, _tmpName)
        }
        val _tmpMobile: String? = entity.getMobile()
        if (_tmpMobile == null) {
          statement.bindNull(3)
        } else {
          statement.bindText(3, _tmpMobile)
        }
        val _tmpAddress: String? = entity.getAddress()
        if (_tmpAddress == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpAddress)
        }
        statement.bindDouble(5, entity.getLatitude())
        statement.bindDouble(6, entity.getLongitude())
        val _tmpCreatedDate: String? = entity.getCreatedDate()
        if (_tmpCreatedDate == null) {
          statement.bindNull(7)
        } else {
          statement.bindText(7, _tmpCreatedDate)
        }
        statement.bindDouble(8, entity.getMilkQuantity())
        statement.bindDouble(9, entity.getMilkRate())
        statement.bindLong(10, entity.getId())
      }
    }
  }

  public override fun insert(customer: Customer?): Long = performBlocking(__db, false, true) {
      _connection ->
    val _result: Long = __insertAdapterOfCustomer.insertAndReturnId(_connection, customer)
    _result
  }

  public override fun insertAll(customers: MutableList<Customer?>?): Unit = performBlocking(__db,
      false, true) { _connection ->
    __insertAdapterOfCustomer_1.insert(_connection, customers)
  }

  public override fun delete(customer: Customer?): Unit = performBlocking(__db, false, true) {
      _connection ->
    __deleteAdapterOfCustomer.handle(_connection, customer)
  }

  public override fun update(customer: Customer?): Unit = performBlocking(__db, false, true) {
      _connection ->
    __updateAdapterOfCustomer.handle(_connection, customer)
  }

  public override fun getAllCustomers(): LiveData<MutableList<Customer?>?>? {
    val _sql: String = "SELECT * FROM customers ORDER BY name ASC"
    return __db.invalidationTracker.createLiveData(arrayOf("customers"), false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfMobile: Int = getColumnIndexOrThrow(_stmt, "mobile")
        val _columnIndexOfAddress: Int = getColumnIndexOrThrow(_stmt, "address")
        val _columnIndexOfLatitude: Int = getColumnIndexOrThrow(_stmt, "latitude")
        val _columnIndexOfLongitude: Int = getColumnIndexOrThrow(_stmt, "longitude")
        val _columnIndexOfCreatedDate: Int = getColumnIndexOrThrow(_stmt, "createdDate")
        val _columnIndexOfMilkQuantity: Int = getColumnIndexOrThrow(_stmt, "milkQuantity")
        val _columnIndexOfMilkRate: Int = getColumnIndexOrThrow(_stmt, "milkRate")
        val _result: MutableList<Customer?> = mutableListOf()
        while (_stmt.step()) {
          val _item: Customer?
          _item = Customer()
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          _item.setId(_tmpId)
          val _tmpName: String?
          if (_stmt.isNull(_columnIndexOfName)) {
            _tmpName = null
          } else {
            _tmpName = _stmt.getText(_columnIndexOfName)
          }
          _item.setName(_tmpName)
          val _tmpMobile: String?
          if (_stmt.isNull(_columnIndexOfMobile)) {
            _tmpMobile = null
          } else {
            _tmpMobile = _stmt.getText(_columnIndexOfMobile)
          }
          _item.setMobile(_tmpMobile)
          val _tmpAddress: String?
          if (_stmt.isNull(_columnIndexOfAddress)) {
            _tmpAddress = null
          } else {
            _tmpAddress = _stmt.getText(_columnIndexOfAddress)
          }
          _item.setAddress(_tmpAddress)
          val _tmpLatitude: Double
          _tmpLatitude = _stmt.getDouble(_columnIndexOfLatitude)
          _item.setLatitude(_tmpLatitude)
          val _tmpLongitude: Double
          _tmpLongitude = _stmt.getDouble(_columnIndexOfLongitude)
          _item.setLongitude(_tmpLongitude)
          val _tmpCreatedDate: String?
          if (_stmt.isNull(_columnIndexOfCreatedDate)) {
            _tmpCreatedDate = null
          } else {
            _tmpCreatedDate = _stmt.getText(_columnIndexOfCreatedDate)
          }
          _item.setCreatedDate(_tmpCreatedDate)
          val _tmpMilkQuantity: Double
          _tmpMilkQuantity = _stmt.getDouble(_columnIndexOfMilkQuantity)
          _item.setMilkQuantity(_tmpMilkQuantity)
          val _tmpMilkRate: Double
          _tmpMilkRate = _stmt.getDouble(_columnIndexOfMilkRate)
          _item.setMilkRate(_tmpMilkRate)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getCustomerById(id: Long): LiveData<Customer?>? {
    val _sql: String = "SELECT * FROM customers WHERE id = ? LIMIT 1"
    return __db.invalidationTracker.createLiveData(arrayOf("customers"), false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfMobile: Int = getColumnIndexOrThrow(_stmt, "mobile")
        val _columnIndexOfAddress: Int = getColumnIndexOrThrow(_stmt, "address")
        val _columnIndexOfLatitude: Int = getColumnIndexOrThrow(_stmt, "latitude")
        val _columnIndexOfLongitude: Int = getColumnIndexOrThrow(_stmt, "longitude")
        val _columnIndexOfCreatedDate: Int = getColumnIndexOrThrow(_stmt, "createdDate")
        val _columnIndexOfMilkQuantity: Int = getColumnIndexOrThrow(_stmt, "milkQuantity")
        val _columnIndexOfMilkRate: Int = getColumnIndexOrThrow(_stmt, "milkRate")
        val _result: Customer?
        if (_stmt.step()) {
          _result = Customer()
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          _result.setId(_tmpId)
          val _tmpName: String?
          if (_stmt.isNull(_columnIndexOfName)) {
            _tmpName = null
          } else {
            _tmpName = _stmt.getText(_columnIndexOfName)
          }
          _result.setName(_tmpName)
          val _tmpMobile: String?
          if (_stmt.isNull(_columnIndexOfMobile)) {
            _tmpMobile = null
          } else {
            _tmpMobile = _stmt.getText(_columnIndexOfMobile)
          }
          _result.setMobile(_tmpMobile)
          val _tmpAddress: String?
          if (_stmt.isNull(_columnIndexOfAddress)) {
            _tmpAddress = null
          } else {
            _tmpAddress = _stmt.getText(_columnIndexOfAddress)
          }
          _result.setAddress(_tmpAddress)
          val _tmpLatitude: Double
          _tmpLatitude = _stmt.getDouble(_columnIndexOfLatitude)
          _result.setLatitude(_tmpLatitude)
          val _tmpLongitude: Double
          _tmpLongitude = _stmt.getDouble(_columnIndexOfLongitude)
          _result.setLongitude(_tmpLongitude)
          val _tmpCreatedDate: String?
          if (_stmt.isNull(_columnIndexOfCreatedDate)) {
            _tmpCreatedDate = null
          } else {
            _tmpCreatedDate = _stmt.getText(_columnIndexOfCreatedDate)
          }
          _result.setCreatedDate(_tmpCreatedDate)
          val _tmpMilkQuantity: Double
          _tmpMilkQuantity = _stmt.getDouble(_columnIndexOfMilkQuantity)
          _result.setMilkQuantity(_tmpMilkQuantity)
          val _tmpMilkRate: Double
          _tmpMilkRate = _stmt.getDouble(_columnIndexOfMilkRate)
          _result.setMilkRate(_tmpMilkRate)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getCustomerByIdSync(id: Long): Customer? {
    val _sql: String = "SELECT * FROM customers WHERE id = ? LIMIT 1"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfMobile: Int = getColumnIndexOrThrow(_stmt, "mobile")
        val _columnIndexOfAddress: Int = getColumnIndexOrThrow(_stmt, "address")
        val _columnIndexOfLatitude: Int = getColumnIndexOrThrow(_stmt, "latitude")
        val _columnIndexOfLongitude: Int = getColumnIndexOrThrow(_stmt, "longitude")
        val _columnIndexOfCreatedDate: Int = getColumnIndexOrThrow(_stmt, "createdDate")
        val _columnIndexOfMilkQuantity: Int = getColumnIndexOrThrow(_stmt, "milkQuantity")
        val _columnIndexOfMilkRate: Int = getColumnIndexOrThrow(_stmt, "milkRate")
        val _result: Customer?
        if (_stmt.step()) {
          _result = Customer()
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          _result.setId(_tmpId)
          val _tmpName: String?
          if (_stmt.isNull(_columnIndexOfName)) {
            _tmpName = null
          } else {
            _tmpName = _stmt.getText(_columnIndexOfName)
          }
          _result.setName(_tmpName)
          val _tmpMobile: String?
          if (_stmt.isNull(_columnIndexOfMobile)) {
            _tmpMobile = null
          } else {
            _tmpMobile = _stmt.getText(_columnIndexOfMobile)
          }
          _result.setMobile(_tmpMobile)
          val _tmpAddress: String?
          if (_stmt.isNull(_columnIndexOfAddress)) {
            _tmpAddress = null
          } else {
            _tmpAddress = _stmt.getText(_columnIndexOfAddress)
          }
          _result.setAddress(_tmpAddress)
          val _tmpLatitude: Double
          _tmpLatitude = _stmt.getDouble(_columnIndexOfLatitude)
          _result.setLatitude(_tmpLatitude)
          val _tmpLongitude: Double
          _tmpLongitude = _stmt.getDouble(_columnIndexOfLongitude)
          _result.setLongitude(_tmpLongitude)
          val _tmpCreatedDate: String?
          if (_stmt.isNull(_columnIndexOfCreatedDate)) {
            _tmpCreatedDate = null
          } else {
            _tmpCreatedDate = _stmt.getText(_columnIndexOfCreatedDate)
          }
          _result.setCreatedDate(_tmpCreatedDate)
          val _tmpMilkQuantity: Double
          _tmpMilkQuantity = _stmt.getDouble(_columnIndexOfMilkQuantity)
          _result.setMilkQuantity(_tmpMilkQuantity)
          val _tmpMilkRate: Double
          _tmpMilkRate = _stmt.getDouble(_columnIndexOfMilkRate)
          _result.setMilkRate(_tmpMilkRate)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getAllCustomersSync(): MutableList<Customer?>? {
    val _sql: String = "SELECT * FROM customers ORDER BY name ASC"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfMobile: Int = getColumnIndexOrThrow(_stmt, "mobile")
        val _columnIndexOfAddress: Int = getColumnIndexOrThrow(_stmt, "address")
        val _columnIndexOfLatitude: Int = getColumnIndexOrThrow(_stmt, "latitude")
        val _columnIndexOfLongitude: Int = getColumnIndexOrThrow(_stmt, "longitude")
        val _columnIndexOfCreatedDate: Int = getColumnIndexOrThrow(_stmt, "createdDate")
        val _columnIndexOfMilkQuantity: Int = getColumnIndexOrThrow(_stmt, "milkQuantity")
        val _columnIndexOfMilkRate: Int = getColumnIndexOrThrow(_stmt, "milkRate")
        val _result: MutableList<Customer?> = mutableListOf()
        while (_stmt.step()) {
          val _item: Customer?
          _item = Customer()
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          _item.setId(_tmpId)
          val _tmpName: String?
          if (_stmt.isNull(_columnIndexOfName)) {
            _tmpName = null
          } else {
            _tmpName = _stmt.getText(_columnIndexOfName)
          }
          _item.setName(_tmpName)
          val _tmpMobile: String?
          if (_stmt.isNull(_columnIndexOfMobile)) {
            _tmpMobile = null
          } else {
            _tmpMobile = _stmt.getText(_columnIndexOfMobile)
          }
          _item.setMobile(_tmpMobile)
          val _tmpAddress: String?
          if (_stmt.isNull(_columnIndexOfAddress)) {
            _tmpAddress = null
          } else {
            _tmpAddress = _stmt.getText(_columnIndexOfAddress)
          }
          _item.setAddress(_tmpAddress)
          val _tmpLatitude: Double
          _tmpLatitude = _stmt.getDouble(_columnIndexOfLatitude)
          _item.setLatitude(_tmpLatitude)
          val _tmpLongitude: Double
          _tmpLongitude = _stmt.getDouble(_columnIndexOfLongitude)
          _item.setLongitude(_tmpLongitude)
          val _tmpCreatedDate: String?
          if (_stmt.isNull(_columnIndexOfCreatedDate)) {
            _tmpCreatedDate = null
          } else {
            _tmpCreatedDate = _stmt.getText(_columnIndexOfCreatedDate)
          }
          _item.setCreatedDate(_tmpCreatedDate)
          val _tmpMilkQuantity: Double
          _tmpMilkQuantity = _stmt.getDouble(_columnIndexOfMilkQuantity)
          _item.setMilkQuantity(_tmpMilkQuantity)
          val _tmpMilkRate: Double
          _tmpMilkRate = _stmt.getDouble(_columnIndexOfMilkRate)
          _item.setMilkRate(_tmpMilkRate)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getAllCustomersForBackup(): MutableList<Customer?>? {
    val _sql: String = "SELECT * FROM customers"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfMobile: Int = getColumnIndexOrThrow(_stmt, "mobile")
        val _columnIndexOfAddress: Int = getColumnIndexOrThrow(_stmt, "address")
        val _columnIndexOfLatitude: Int = getColumnIndexOrThrow(_stmt, "latitude")
        val _columnIndexOfLongitude: Int = getColumnIndexOrThrow(_stmt, "longitude")
        val _columnIndexOfCreatedDate: Int = getColumnIndexOrThrow(_stmt, "createdDate")
        val _columnIndexOfMilkQuantity: Int = getColumnIndexOrThrow(_stmt, "milkQuantity")
        val _columnIndexOfMilkRate: Int = getColumnIndexOrThrow(_stmt, "milkRate")
        val _result: MutableList<Customer?> = mutableListOf()
        while (_stmt.step()) {
          val _item: Customer?
          _item = Customer()
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          _item.setId(_tmpId)
          val _tmpName: String?
          if (_stmt.isNull(_columnIndexOfName)) {
            _tmpName = null
          } else {
            _tmpName = _stmt.getText(_columnIndexOfName)
          }
          _item.setName(_tmpName)
          val _tmpMobile: String?
          if (_stmt.isNull(_columnIndexOfMobile)) {
            _tmpMobile = null
          } else {
            _tmpMobile = _stmt.getText(_columnIndexOfMobile)
          }
          _item.setMobile(_tmpMobile)
          val _tmpAddress: String?
          if (_stmt.isNull(_columnIndexOfAddress)) {
            _tmpAddress = null
          } else {
            _tmpAddress = _stmt.getText(_columnIndexOfAddress)
          }
          _item.setAddress(_tmpAddress)
          val _tmpLatitude: Double
          _tmpLatitude = _stmt.getDouble(_columnIndexOfLatitude)
          _item.setLatitude(_tmpLatitude)
          val _tmpLongitude: Double
          _tmpLongitude = _stmt.getDouble(_columnIndexOfLongitude)
          _item.setLongitude(_tmpLongitude)
          val _tmpCreatedDate: String?
          if (_stmt.isNull(_columnIndexOfCreatedDate)) {
            _tmpCreatedDate = null
          } else {
            _tmpCreatedDate = _stmt.getText(_columnIndexOfCreatedDate)
          }
          _item.setCreatedDate(_tmpCreatedDate)
          val _tmpMilkQuantity: Double
          _tmpMilkQuantity = _stmt.getDouble(_columnIndexOfMilkQuantity)
          _item.setMilkQuantity(_tmpMilkQuantity)
          val _tmpMilkRate: Double
          _tmpMilkRate = _stmt.getDouble(_columnIndexOfMilkRate)
          _item.setMilkRate(_tmpMilkRate)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getCustomerByMobile(mobile: String?): Customer? {
    val _sql: String = "SELECT * FROM customers WHERE mobile = ? LIMIT 1"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        if (mobile == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, mobile)
        }
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfMobile: Int = getColumnIndexOrThrow(_stmt, "mobile")
        val _columnIndexOfAddress: Int = getColumnIndexOrThrow(_stmt, "address")
        val _columnIndexOfLatitude: Int = getColumnIndexOrThrow(_stmt, "latitude")
        val _columnIndexOfLongitude: Int = getColumnIndexOrThrow(_stmt, "longitude")
        val _columnIndexOfCreatedDate: Int = getColumnIndexOrThrow(_stmt, "createdDate")
        val _result: Customer?
        if (_stmt.step()) {
          _result = Customer()
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          _result.setId(_tmpId)
          val _tmpName: String?
          if (_stmt.isNull(_columnIndexOfName)) {
            _tmpName = null
          } else {
            _tmpName = _stmt.getText(_columnIndexOfName)
          }
          _result.setName(_tmpName)
          val _tmpMobile: String?
          if (_stmt.isNull(_columnIndexOfMobile)) {
            _tmpMobile = null
          } else {
            _tmpMobile = _stmt.getText(_columnIndexOfMobile)
          }
          _result.setMobile(_tmpMobile)
          val _tmpAddress: String?
          if (_stmt.isNull(_columnIndexOfAddress)) {
            _tmpAddress = null
          } else {
            _tmpAddress = _stmt.getText(_columnIndexOfAddress)
          }
          _result.setAddress(_tmpAddress)
          val _tmpLatitude: Double
          _tmpLatitude = _stmt.getDouble(_columnIndexOfLatitude)
          _result.setLatitude(_tmpLatitude)
          val _tmpLongitude: Double
          _tmpLongitude = _stmt.getDouble(_columnIndexOfLongitude)
          _result.setLongitude(_tmpLongitude)
          val _tmpCreatedDate: String?
          if (_stmt.isNull(_columnIndexOfCreatedDate)) {
            _tmpCreatedDate = null
          } else {
            _tmpCreatedDate = _stmt.getText(_columnIndexOfCreatedDate)
          }
          _result.setCreatedDate(_tmpCreatedDate)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getCustomerByMobileSync(mobile: String?): Customer? {
    val _sql: String = "SELECT * FROM customers WHERE mobile = ? LIMIT 1"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        if (mobile == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, mobile)
        }
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfMobile: Int = getColumnIndexOrThrow(_stmt, "mobile")
        val _columnIndexOfAddress: Int = getColumnIndexOrThrow(_stmt, "address")
        val _columnIndexOfLatitude: Int = getColumnIndexOrThrow(_stmt, "latitude")
        val _columnIndexOfLongitude: Int = getColumnIndexOrThrow(_stmt, "longitude")
        val _columnIndexOfCreatedDate: Int = getColumnIndexOrThrow(_stmt, "createdDate")
        val _result: Customer?
        if (_stmt.step()) {
          _result = Customer()
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          _result.setId(_tmpId)
          val _tmpName: String?
          if (_stmt.isNull(_columnIndexOfName)) {
            _tmpName = null
          } else {
            _tmpName = _stmt.getText(_columnIndexOfName)
          }
          _result.setName(_tmpName)
          val _tmpMobile: String?
          if (_stmt.isNull(_columnIndexOfMobile)) {
            _tmpMobile = null
          } else {
            _tmpMobile = _stmt.getText(_columnIndexOfMobile)
          }
          _result.setMobile(_tmpMobile)
          val _tmpAddress: String?
          if (_stmt.isNull(_columnIndexOfAddress)) {
            _tmpAddress = null
          } else {
            _tmpAddress = _stmt.getText(_columnIndexOfAddress)
          }
          _result.setAddress(_tmpAddress)
          val _tmpLatitude: Double
          _tmpLatitude = _stmt.getDouble(_columnIndexOfLatitude)
          _result.setLatitude(_tmpLatitude)
          val _tmpLongitude: Double
          _tmpLongitude = _stmt.getDouble(_columnIndexOfLongitude)
          _result.setLongitude(_tmpLongitude)
          val _tmpCreatedDate: String?
          if (_stmt.isNull(_columnIndexOfCreatedDate)) {
            _tmpCreatedDate = null
          } else {
            _tmpCreatedDate = _stmt.getText(_columnIndexOfCreatedDate)
          }
          _result.setCreatedDate(_tmpCreatedDate)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getPendingCustomersForDate(todayDate: String?):
      LiveData<MutableList<Customer?>?>? {
    val _sql: String =
        "SELECT c.* FROM customers c LEFT JOIN deliveries d ON c.id = d.customerId AND d.deliveryDate = ? WHERE d.id IS NULL OR d.status = 'Pending' ORDER BY c.name ASC"
    return __db.invalidationTracker.createLiveData(arrayOf("customers", "deliveries"), false) {
        _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        if (todayDate == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, todayDate)
        }
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfMobile: Int = getColumnIndexOrThrow(_stmt, "mobile")
        val _columnIndexOfAddress: Int = getColumnIndexOrThrow(_stmt, "address")
        val _columnIndexOfLatitude: Int = getColumnIndexOrThrow(_stmt, "latitude")
        val _columnIndexOfLongitude: Int = getColumnIndexOrThrow(_stmt, "longitude")
        val _columnIndexOfCreatedDate: Int = getColumnIndexOrThrow(_stmt, "createdDate")
        val _columnIndexOfMilkQuantity: Int = getColumnIndexOrThrow(_stmt, "milkQuantity")
        val _columnIndexOfMilkRate: Int = getColumnIndexOrThrow(_stmt, "milkRate")
        val _result: MutableList<Customer?> = mutableListOf()
        while (_stmt.step()) {
          val _item: Customer?
          _item = Customer()
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          _item.setId(_tmpId)
          val _tmpName: String?
          if (_stmt.isNull(_columnIndexOfName)) {
            _tmpName = null
          } else {
            _tmpName = _stmt.getText(_columnIndexOfName)
          }
          _item.setName(_tmpName)
          val _tmpMobile: String?
          if (_stmt.isNull(_columnIndexOfMobile)) {
            _tmpMobile = null
          } else {
            _tmpMobile = _stmt.getText(_columnIndexOfMobile)
          }
          _item.setMobile(_tmpMobile)
          val _tmpAddress: String?
          if (_stmt.isNull(_columnIndexOfAddress)) {
            _tmpAddress = null
          } else {
            _tmpAddress = _stmt.getText(_columnIndexOfAddress)
          }
          _item.setAddress(_tmpAddress)
          val _tmpLatitude: Double
          _tmpLatitude = _stmt.getDouble(_columnIndexOfLatitude)
          _item.setLatitude(_tmpLatitude)
          val _tmpLongitude: Double
          _tmpLongitude = _stmt.getDouble(_columnIndexOfLongitude)
          _item.setLongitude(_tmpLongitude)
          val _tmpCreatedDate: String?
          if (_stmt.isNull(_columnIndexOfCreatedDate)) {
            _tmpCreatedDate = null
          } else {
            _tmpCreatedDate = _stmt.getText(_columnIndexOfCreatedDate)
          }
          _item.setCreatedDate(_tmpCreatedDate)
          val _tmpMilkQuantity: Double
          _tmpMilkQuantity = _stmt.getDouble(_columnIndexOfMilkQuantity)
          _item.setMilkQuantity(_tmpMilkQuantity)
          val _tmpMilkRate: Double
          _tmpMilkRate = _stmt.getDouble(_columnIndexOfMilkRate)
          _item.setMilkRate(_tmpMilkRate)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getCustomerByMobile(mobile: String?): Customer? {
    val _sql: String = "SELECT * FROM customers WHERE mobile=? LIMIT 1"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        if (mobile == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, mobile)
        }
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfMobile: Int = getColumnIndexOrThrow(_stmt, "mobile")
        val _columnIndexOfAddress: Int = getColumnIndexOrThrow(_stmt, "address")
        val _columnIndexOfLatitude: Int = getColumnIndexOrThrow(_stmt, "latitude")
        val _columnIndexOfLongitude: Int = getColumnIndexOrThrow(_stmt, "longitude")
        val _columnIndexOfCreatedDate: Int = getColumnIndexOrThrow(_stmt, "createdDate")
        val _columnIndexOfMilkQuantity: Int = getColumnIndexOrThrow(_stmt, "milkQuantity")
        val _columnIndexOfMilkRate: Int = getColumnIndexOrThrow(_stmt, "milkRate")
        val _result: Customer?
        if (_stmt.step()) {
          _result = Customer()
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          _result.setId(_tmpId)
          val _tmpName: String?
          if (_stmt.isNull(_columnIndexOfName)) {
            _tmpName = null
          } else {
            _tmpName = _stmt.getText(_columnIndexOfName)
          }
          _result.setName(_tmpName)
          val _tmpMobile: String?
          if (_stmt.isNull(_columnIndexOfMobile)) {
            _tmpMobile = null
          } else {
            _tmpMobile = _stmt.getText(_columnIndexOfMobile)
          }
          _result.setMobile(_tmpMobile)
          val _tmpAddress: String?
          if (_stmt.isNull(_columnIndexOfAddress)) {
            _tmpAddress = null
          } else {
            _tmpAddress = _stmt.getText(_columnIndexOfAddress)
          }
          _result.setAddress(_tmpAddress)
          val _tmpLatitude: Double
          _tmpLatitude = _stmt.getDouble(_columnIndexOfLatitude)
          _result.setLatitude(_tmpLatitude)
          val _tmpLongitude: Double
          _tmpLongitude = _stmt.getDouble(_columnIndexOfLongitude)
          _result.setLongitude(_tmpLongitude)
          val _tmpCreatedDate: String?
          if (_stmt.isNull(_columnIndexOfCreatedDate)) {
            _tmpCreatedDate = null
          } else {
            _tmpCreatedDate = _stmt.getText(_columnIndexOfCreatedDate)
          }
          _result.setCreatedDate(_tmpCreatedDate)
          val _tmpMilkQuantity: Double
          _tmpMilkQuantity = _stmt.getDouble(_columnIndexOfMilkQuantity)
          _result.setMilkQuantity(_tmpMilkQuantity)
          val _tmpMilkRate: Double
          _tmpMilkRate = _stmt.getDouble(_columnIndexOfMilkRate)
          _result.setMilkRate(_tmpMilkRate)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun deleteAll() {
    val _sql: String = "DELETE FROM customers"
    return performBlocking(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}

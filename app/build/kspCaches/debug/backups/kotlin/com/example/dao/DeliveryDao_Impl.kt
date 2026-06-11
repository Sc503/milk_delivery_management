package com.example.dao

import androidx.lifecycle.LiveData
import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performBlocking
import androidx.sqlite.SQLiteStatement
import com.example.models.Delivery
import javax.`annotation`.processing.Generated
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
public class DeliveryDao_Impl(
  __db: RoomDatabase,
) : DeliveryDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfDelivery: EntityInsertAdapter<Delivery>

  private val __deleteAdapterOfDelivery: EntityDeleteOrUpdateAdapter<Delivery>

  private val __updateAdapterOfDelivery: EntityDeleteOrUpdateAdapter<Delivery>
  init {
    this.__db = __db
    this.__insertAdapterOfDelivery = object : EntityInsertAdapter<Delivery>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `deliveries` (`id`,`customerId`,`deliveryDate`,`deliveredTime`,`status`) VALUES (nullif(?, 0),?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: Delivery) {
        statement.bindLong(1, entity.getId())
        statement.bindLong(2, entity.getCustomerId())
        val _tmpDeliveryDate: String? = entity.getDeliveryDate()
        if (_tmpDeliveryDate == null) {
          statement.bindNull(3)
        } else {
          statement.bindText(3, _tmpDeliveryDate)
        }
        val _tmpDeliveredTime: String? = entity.getDeliveredTime()
        if (_tmpDeliveredTime == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpDeliveredTime)
        }
        val _tmpStatus: String? = entity.getStatus()
        if (_tmpStatus == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmpStatus)
        }
      }
    }
    this.__deleteAdapterOfDelivery = object : EntityDeleteOrUpdateAdapter<Delivery>() {
      protected override fun createQuery(): String = "DELETE FROM `deliveries` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: Delivery) {
        statement.bindLong(1, entity.getId())
      }
    }
    this.__updateAdapterOfDelivery = object : EntityDeleteOrUpdateAdapter<Delivery>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `deliveries` SET `id` = ?,`customerId` = ?,`deliveryDate` = ?,`deliveredTime` = ?,`status` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: Delivery) {
        statement.bindLong(1, entity.getId())
        statement.bindLong(2, entity.getCustomerId())
        val _tmpDeliveryDate: String? = entity.getDeliveryDate()
        if (_tmpDeliveryDate == null) {
          statement.bindNull(3)
        } else {
          statement.bindText(3, _tmpDeliveryDate)
        }
        val _tmpDeliveredTime: String? = entity.getDeliveredTime()
        if (_tmpDeliveredTime == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpDeliveredTime)
        }
        val _tmpStatus: String? = entity.getStatus()
        if (_tmpStatus == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmpStatus)
        }
        statement.bindLong(6, entity.getId())
      }
    }
  }

  public override fun insert(delivery: Delivery?): Long = performBlocking(__db, false, true) {
      _connection ->
    val _result: Long = __insertAdapterOfDelivery.insertAndReturnId(_connection, delivery)
    _result
  }

  public override fun delete(delivery: Delivery?): Unit = performBlocking(__db, false, true) {
      _connection ->
    __deleteAdapterOfDelivery.handle(_connection, delivery)
  }

  public override fun update(delivery: Delivery?): Unit = performBlocking(__db, false, true) {
      _connection ->
    __updateAdapterOfDelivery.handle(_connection, delivery)
  }

  public override fun getDeliveryForCustomerAndDate(customerId: Long, date: String?): Delivery? {
    val _sql: String = "SELECT * FROM deliveries WHERE customerId = ? AND deliveryDate = ? LIMIT 1"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, customerId)
        _argIndex = 2
        if (date == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, date)
        }
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfCustomerId: Int = getColumnIndexOrThrow(_stmt, "customerId")
        val _columnIndexOfDeliveryDate: Int = getColumnIndexOrThrow(_stmt, "deliveryDate")
        val _columnIndexOfDeliveredTime: Int = getColumnIndexOrThrow(_stmt, "deliveredTime")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _result: Delivery?
        if (_stmt.step()) {
          _result = Delivery()
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          _result.setId(_tmpId)
          val _tmpCustomerId: Long
          _tmpCustomerId = _stmt.getLong(_columnIndexOfCustomerId)
          _result.setCustomerId(_tmpCustomerId)
          val _tmpDeliveryDate: String?
          if (_stmt.isNull(_columnIndexOfDeliveryDate)) {
            _tmpDeliveryDate = null
          } else {
            _tmpDeliveryDate = _stmt.getText(_columnIndexOfDeliveryDate)
          }
          _result.setDeliveryDate(_tmpDeliveryDate)
          val _tmpDeliveredTime: String?
          if (_stmt.isNull(_columnIndexOfDeliveredTime)) {
            _tmpDeliveredTime = null
          } else {
            _tmpDeliveredTime = _stmt.getText(_columnIndexOfDeliveredTime)
          }
          _result.setDeliveredTime(_tmpDeliveredTime)
          val _tmpStatus: String?
          if (_stmt.isNull(_columnIndexOfStatus)) {
            _tmpStatus = null
          } else {
            _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          }
          _result.setStatus(_tmpStatus)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getDeliveriesForCustomer(customerId: Long):
      LiveData<MutableList<Delivery?>?>? {
    val _sql: String = "SELECT * FROM deliveries WHERE customerId = ? ORDER BY deliveryDate DESC"
    return __db.invalidationTracker.createLiveData(arrayOf("deliveries"), false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, customerId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfCustomerId: Int = getColumnIndexOrThrow(_stmt, "customerId")
        val _columnIndexOfDeliveryDate: Int = getColumnIndexOrThrow(_stmt, "deliveryDate")
        val _columnIndexOfDeliveredTime: Int = getColumnIndexOrThrow(_stmt, "deliveredTime")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _result: MutableList<Delivery?> = mutableListOf()
        while (_stmt.step()) {
          val _item: Delivery?
          _item = Delivery()
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          _item.setId(_tmpId)
          val _tmpCustomerId: Long
          _tmpCustomerId = _stmt.getLong(_columnIndexOfCustomerId)
          _item.setCustomerId(_tmpCustomerId)
          val _tmpDeliveryDate: String?
          if (_stmt.isNull(_columnIndexOfDeliveryDate)) {
            _tmpDeliveryDate = null
          } else {
            _tmpDeliveryDate = _stmt.getText(_columnIndexOfDeliveryDate)
          }
          _item.setDeliveryDate(_tmpDeliveryDate)
          val _tmpDeliveredTime: String?
          if (_stmt.isNull(_columnIndexOfDeliveredTime)) {
            _tmpDeliveredTime = null
          } else {
            _tmpDeliveredTime = _stmt.getText(_columnIndexOfDeliveredTime)
          }
          _item.setDeliveredTime(_tmpDeliveredTime)
          val _tmpStatus: String?
          if (_stmt.isNull(_columnIndexOfStatus)) {
            _tmpStatus = null
          } else {
            _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          }
          _item.setStatus(_tmpStatus)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getDeliveriesForCustomerSync(customerId: Long): MutableList<Delivery?>? {
    val _sql: String = "SELECT * FROM deliveries WHERE customerId = ? ORDER BY deliveryDate DESC"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, customerId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfCustomerId: Int = getColumnIndexOrThrow(_stmt, "customerId")
        val _columnIndexOfDeliveryDate: Int = getColumnIndexOrThrow(_stmt, "deliveryDate")
        val _columnIndexOfDeliveredTime: Int = getColumnIndexOrThrow(_stmt, "deliveredTime")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _result: MutableList<Delivery?> = mutableListOf()
        while (_stmt.step()) {
          val _item: Delivery?
          _item = Delivery()
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          _item.setId(_tmpId)
          val _tmpCustomerId: Long
          _tmpCustomerId = _stmt.getLong(_columnIndexOfCustomerId)
          _item.setCustomerId(_tmpCustomerId)
          val _tmpDeliveryDate: String?
          if (_stmt.isNull(_columnIndexOfDeliveryDate)) {
            _tmpDeliveryDate = null
          } else {
            _tmpDeliveryDate = _stmt.getText(_columnIndexOfDeliveryDate)
          }
          _item.setDeliveryDate(_tmpDeliveryDate)
          val _tmpDeliveredTime: String?
          if (_stmt.isNull(_columnIndexOfDeliveredTime)) {
            _tmpDeliveredTime = null
          } else {
            _tmpDeliveredTime = _stmt.getText(_columnIndexOfDeliveredTime)
          }
          _item.setDeliveredTime(_tmpDeliveredTime)
          val _tmpStatus: String?
          if (_stmt.isNull(_columnIndexOfStatus)) {
            _tmpStatus = null
          } else {
            _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          }
          _item.setStatus(_tmpStatus)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getDeliveriesForMonthSync(yearMonthPrefix: String?): MutableList<Delivery?>? {
    val _sql: String =
        "SELECT * FROM deliveries WHERE deliveryDate LIKE ? ORDER BY deliveryDate ASC"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        if (yearMonthPrefix == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, yearMonthPrefix)
        }
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfCustomerId: Int = getColumnIndexOrThrow(_stmt, "customerId")
        val _columnIndexOfDeliveryDate: Int = getColumnIndexOrThrow(_stmt, "deliveryDate")
        val _columnIndexOfDeliveredTime: Int = getColumnIndexOrThrow(_stmt, "deliveredTime")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _result: MutableList<Delivery?> = mutableListOf()
        while (_stmt.step()) {
          val _item: Delivery?
          _item = Delivery()
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          _item.setId(_tmpId)
          val _tmpCustomerId: Long
          _tmpCustomerId = _stmt.getLong(_columnIndexOfCustomerId)
          _item.setCustomerId(_tmpCustomerId)
          val _tmpDeliveryDate: String?
          if (_stmt.isNull(_columnIndexOfDeliveryDate)) {
            _tmpDeliveryDate = null
          } else {
            _tmpDeliveryDate = _stmt.getText(_columnIndexOfDeliveryDate)
          }
          _item.setDeliveryDate(_tmpDeliveryDate)
          val _tmpDeliveredTime: String?
          if (_stmt.isNull(_columnIndexOfDeliveredTime)) {
            _tmpDeliveredTime = null
          } else {
            _tmpDeliveredTime = _stmt.getText(_columnIndexOfDeliveredTime)
          }
          _item.setDeliveredTime(_tmpDeliveredTime)
          val _tmpStatus: String?
          if (_stmt.isNull(_columnIndexOfStatus)) {
            _tmpStatus = null
          } else {
            _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          }
          _item.setStatus(_tmpStatus)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getDeliveriesForDateSync(date: String?): MutableList<Delivery?>? {
    val _sql: String = "SELECT * FROM deliveries WHERE deliveryDate = ?"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        if (date == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, date)
        }
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfCustomerId: Int = getColumnIndexOrThrow(_stmt, "customerId")
        val _columnIndexOfDeliveryDate: Int = getColumnIndexOrThrow(_stmt, "deliveryDate")
        val _columnIndexOfDeliveredTime: Int = getColumnIndexOrThrow(_stmt, "deliveredTime")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _result: MutableList<Delivery?> = mutableListOf()
        while (_stmt.step()) {
          val _item: Delivery?
          _item = Delivery()
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          _item.setId(_tmpId)
          val _tmpCustomerId: Long
          _tmpCustomerId = _stmt.getLong(_columnIndexOfCustomerId)
          _item.setCustomerId(_tmpCustomerId)
          val _tmpDeliveryDate: String?
          if (_stmt.isNull(_columnIndexOfDeliveryDate)) {
            _tmpDeliveryDate = null
          } else {
            _tmpDeliveryDate = _stmt.getText(_columnIndexOfDeliveryDate)
          }
          _item.setDeliveryDate(_tmpDeliveryDate)
          val _tmpDeliveredTime: String?
          if (_stmt.isNull(_columnIndexOfDeliveredTime)) {
            _tmpDeliveredTime = null
          } else {
            _tmpDeliveredTime = _stmt.getText(_columnIndexOfDeliveredTime)
          }
          _item.setDeliveredTime(_tmpDeliveredTime)
          val _tmpStatus: String?
          if (_stmt.isNull(_columnIndexOfStatus)) {
            _tmpStatus = null
          } else {
            _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          }
          _item.setStatus(_tmpStatus)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getDeliveriesForDate(date: String?): LiveData<MutableList<Delivery?>?>? {
    val _sql: String = "SELECT * FROM deliveries WHERE deliveryDate = ?"
    return __db.invalidationTracker.createLiveData(arrayOf("deliveries"), false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        if (date == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, date)
        }
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfCustomerId: Int = getColumnIndexOrThrow(_stmt, "customerId")
        val _columnIndexOfDeliveryDate: Int = getColumnIndexOrThrow(_stmt, "deliveryDate")
        val _columnIndexOfDeliveredTime: Int = getColumnIndexOrThrow(_stmt, "deliveredTime")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _result: MutableList<Delivery?> = mutableListOf()
        while (_stmt.step()) {
          val _item: Delivery?
          _item = Delivery()
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          _item.setId(_tmpId)
          val _tmpCustomerId: Long
          _tmpCustomerId = _stmt.getLong(_columnIndexOfCustomerId)
          _item.setCustomerId(_tmpCustomerId)
          val _tmpDeliveryDate: String?
          if (_stmt.isNull(_columnIndexOfDeliveryDate)) {
            _tmpDeliveryDate = null
          } else {
            _tmpDeliveryDate = _stmt.getText(_columnIndexOfDeliveryDate)
          }
          _item.setDeliveryDate(_tmpDeliveryDate)
          val _tmpDeliveredTime: String?
          if (_stmt.isNull(_columnIndexOfDeliveredTime)) {
            _tmpDeliveredTime = null
          } else {
            _tmpDeliveredTime = _stmt.getText(_columnIndexOfDeliveredTime)
          }
          _item.setDeliveredTime(_tmpDeliveredTime)
          val _tmpStatus: String?
          if (_stmt.isNull(_columnIndexOfStatus)) {
            _tmpStatus = null
          } else {
            _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          }
          _item.setStatus(_tmpStatus)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getAllDeliveries(): LiveData<MutableList<Delivery?>?>? {
    val _sql: String = "SELECT * FROM deliveries ORDER BY deliveryDate DESC"
    return __db.invalidationTracker.createLiveData(arrayOf("deliveries"), false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfCustomerId: Int = getColumnIndexOrThrow(_stmt, "customerId")
        val _columnIndexOfDeliveryDate: Int = getColumnIndexOrThrow(_stmt, "deliveryDate")
        val _columnIndexOfDeliveredTime: Int = getColumnIndexOrThrow(_stmt, "deliveredTime")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _result: MutableList<Delivery?> = mutableListOf()
        while (_stmt.step()) {
          val _item: Delivery?
          _item = Delivery()
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          _item.setId(_tmpId)
          val _tmpCustomerId: Long
          _tmpCustomerId = _stmt.getLong(_columnIndexOfCustomerId)
          _item.setCustomerId(_tmpCustomerId)
          val _tmpDeliveryDate: String?
          if (_stmt.isNull(_columnIndexOfDeliveryDate)) {
            _tmpDeliveryDate = null
          } else {
            _tmpDeliveryDate = _stmt.getText(_columnIndexOfDeliveryDate)
          }
          _item.setDeliveryDate(_tmpDeliveryDate)
          val _tmpDeliveredTime: String?
          if (_stmt.isNull(_columnIndexOfDeliveredTime)) {
            _tmpDeliveredTime = null
          } else {
            _tmpDeliveredTime = _stmt.getText(_columnIndexOfDeliveredTime)
          }
          _item.setDeliveredTime(_tmpDeliveredTime)
          val _tmpStatus: String?
          if (_stmt.isNull(_columnIndexOfStatus)) {
            _tmpStatus = null
          } else {
            _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          }
          _item.setStatus(_tmpStatus)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun findDelivery(customerId: Long, date: String?): Delivery? {
    val _sql: String = "SELECT * FROM deliveries WHERE customerId = ? AND deliveryDate = ? LIMIT 1"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, customerId)
        _argIndex = 2
        if (date == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, date)
        }
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfCustomerId: Int = getColumnIndexOrThrow(_stmt, "customerId")
        val _columnIndexOfDeliveryDate: Int = getColumnIndexOrThrow(_stmt, "deliveryDate")
        val _columnIndexOfDeliveredTime: Int = getColumnIndexOrThrow(_stmt, "deliveredTime")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _result: Delivery?
        if (_stmt.step()) {
          _result = Delivery()
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          _result.setId(_tmpId)
          val _tmpCustomerId: Long
          _tmpCustomerId = _stmt.getLong(_columnIndexOfCustomerId)
          _result.setCustomerId(_tmpCustomerId)
          val _tmpDeliveryDate: String?
          if (_stmt.isNull(_columnIndexOfDeliveryDate)) {
            _tmpDeliveryDate = null
          } else {
            _tmpDeliveryDate = _stmt.getText(_columnIndexOfDeliveryDate)
          }
          _result.setDeliveryDate(_tmpDeliveryDate)
          val _tmpDeliveredTime: String?
          if (_stmt.isNull(_columnIndexOfDeliveredTime)) {
            _tmpDeliveredTime = null
          } else {
            _tmpDeliveredTime = _stmt.getText(_columnIndexOfDeliveredTime)
          }
          _result.setDeliveredTime(_tmpDeliveredTime)
          val _tmpStatus: String?
          if (_stmt.isNull(_columnIndexOfStatus)) {
            _tmpStatus = null
          } else {
            _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          }
          _result.setStatus(_tmpStatus)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
